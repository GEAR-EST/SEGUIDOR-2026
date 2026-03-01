package com.example.zeguiaapp

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.os.Bundle
import androidx.appcompat.widget.SwitchCompat
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.IOException
import java.util.UUID
import android.Manifest
import android.os.Build
import android.content.Context
import android.content.res.ColorStateList
import androidx.core.content.ContextCompat


class MainActivity : AppCompatActivity() {

    private lateinit var txtSerial: android.widget.TextView

    private var continuarEscutando = false
    private lateinit var btAdapter: BluetoothAdapter
    private var btSocket: BluetoothSocket? = null
    private val address: String = "CC:DB:A7:62:8D:96" // MAC da sua ESP32
    private val MY_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN), 1)
        }

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager
        btAdapter = bluetoothManager.adapter

        val swBluetooth = findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.bluetooth)
        val swLED = findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.LED)

        val txtStatusBluetooth = findViewById<android.widget.TextView>(R.id.txtStatusBluetooth)
        val txtStatusLED = findViewById<android.widget.TextView>(R.id.txtStatusLED)

        atualizarCoresSwitch(swBluetooth, txtStatusBluetooth, swBluetooth.isChecked, "CONECTADO")
        atualizarCoresSwitch(swLED, txtStatusLED, swLED.isChecked, "LIGADO")

        val btnCalibrar = findViewById<android.widget.Button>(R.id.calibrate)
        val btnStartRun = findViewById<android.widget.Button>(R.id.run)
        val btnStopRun = findViewById<android.widget.Button>(R.id.stop)
        val btnChooseMode = findViewById<android.widget.Button>(R.id.mode)
        val btnModeFollower = findViewById<android.widget.Button>(R.id.follower)
        val btnModeChase = findViewById<android.widget.Button>(R.id.chase)
        val btnChooseStrategy = findViewById<android.widget.Button>(R.id.strategy)
        val btnStrategyConservative = findViewById<android.widget.Button>(R.id.conservative)
        val btnStrategyRisk = findViewById<android.widget.Button>(R.id.risk)


        swBluetooth.setOnCheckedChangeListener { _, isChecked ->
            atualizarCoresSwitch(swBluetooth, txtStatusBluetooth, isChecked, "CONECTADO")
            if (isChecked) {
                conectarBluetooth()

            } else {
                desconectarBluetooth()

            }
        }

        swLED.setOnCheckedChangeListener { _, isChecked ->
            atualizarCoresSwitch(swLED, txtStatusLED, isChecked, "LIGADO")
            if (isChecked) {
                enviarComando("1")
            } else {
                enviarComando("0")
            }
        }

        btnCalibrar.setOnClickListener {
            enviarComando("K")
        }

        btnStartRun.setOnClickListener {
            enviarComando("R")
        }

        btnStopRun.setOnClickListener {
            enviarComando("F")
        }

        btnChooseMode.setOnClickListener {
            enviarComando("M")
        }

        btnModeFollower.setOnClickListener {
            enviarComando("S")
        }

        btnModeChase.setOnClickListener {
            enviarComando("P")
        }

        btnChooseStrategy.setOnClickListener {
            enviarComando("E")
        }

        btnStrategyConservative.setOnClickListener {
            enviarComando("C")
        }

        btnStrategyRisk.setOnClickListener {
            enviarComando("A")
        }

    }

    private fun permissaoBluetooth(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            androidx.core.content.ContextCompat.checkSelfPermission(
                this, Manifest.permission.BLUETOOTH_CONNECT
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun conectarBluetooth() {

        if (!permissaoBluetooth()) {
            runOnUiThread {
                Toast.makeText(this, "Permissão de Bluetooth necessária!", Toast.LENGTH_SHORT).show()
                findViewById<SwitchCompat>(R.id.bluetooth).isChecked = false
            }
            return
        }


        if (!btAdapter.isEnabled) {
            Toast.makeText(this, "Ative o Bluetooth!", Toast.LENGTH_SHORT).show()
            findViewById<SwitchCompat>(R.id.bluetooth).isChecked = false
            return
        }

        Thread {
            try {
                val dispositivo = btAdapter.getRemoteDevice(address)
                btSocket = dispositivo.createRfcommSocketToServiceRecord(MY_UUID)

                btSocket?.connect()

                continuarEscutando = true
                receberDados()

                runOnUiThread {
                    Toast.makeText(this, "Conectado ao ZeGuia!", Toast.LENGTH_SHORT).show()

                }

            } catch (e: IOException) {

                runOnUiThread {
                    findViewById<SwitchCompat>(R.id.bluetooth).isChecked = false
                    Toast.makeText(this, "Falha na conexão", Toast.LENGTH_SHORT).show()
                }

                btSocket = null
            }
        }.start()
    }

    private fun desconectarBluetooth() {
        try {
            continuarEscutando = false
            btSocket?.close()
            btSocket = null

            Toast.makeText(this, "Desconectado", Toast.LENGTH_SHORT).show()

        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun atualizarCoresSwitch(switchCompat: androidx.appcompat.widget.SwitchCompat, textView: android.widget.TextView, isChecked: Boolean, textoLigado: String) {
        if (isChecked) {
            textView.text = textoLigado
            textView.setTextColor(android.graphics.Color.parseColor("#00FF66"))
            switchCompat.thumbTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FFFFFF"))
            switchCompat.trackTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#A066FF"))
        } else {
            // DESLIGADO (Roxo escuro)
            textView.text = "DESLIGADO"
            textView.setTextColor(android.graphics.Color.parseColor("#3D285B"))
            switchCompat.thumbTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#A09DA5"))
            switchCompat.trackTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#2E1A47"))
        }
    }
    private fun receberDados() {
        val buffer = ByteArray(1024)
        txtSerial = findViewById(R.id.txtSerial)

        Thread {
            val input = btSocket?.inputStream ?: return@Thread

            while (continuarEscutando) {
                try {
                    val bytes = input.read(buffer)

                    if (bytes > 0) {
                        val mensagem = String(buffer, 0, bytes)

                        runOnUiThread {
                            txtSerial.append(mensagem)

                            val scroll = findViewById<android.widget.ScrollView>(R.id.scrollMonitor)
                            scroll.post {
                                scroll.fullScroll(android.view.View.FOCUS_DOWN)
                            }
                        }
                    }

                } catch (e: IOException) {
                    break
                }
            }
        }.start()
    }


    private fun enviarComando(sinal: String) {

        if (btSocket == null) {
            Toast.makeText(this, "Não está conectado!", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            btSocket?.outputStream?.write(sinal.toByteArray())
        } catch (e: IOException) {
            Toast.makeText(this, "Erro ao enviar dados", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        continuarEscutando = false
        btSocket?.close()
    }
}

