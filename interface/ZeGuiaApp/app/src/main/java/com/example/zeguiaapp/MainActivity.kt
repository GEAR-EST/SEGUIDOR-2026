package com.example.zeguiaapp

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.os.Bundle
import androidx.appcompat.widget.SwitchCompat
import android.widget.Switch
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.IOException
import java.util.UUID
import kotlin.io.outputStream
import android.Manifest
import android.os.Build
import android.content.Context
import android.bluetooth.BluetoothManager


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

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager
        btAdapter = bluetoothManager.adapter

        val swBluetooth = findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.bluetooth)
        val swLED = findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.LED)
        val btnCalibrar = findViewById<android.widget.Button>(R.id.calibrate)
        val btnStartRun = findViewById<android.widget.Button>(R.id.run)
        val btnStopRun = findViewById<android.widget.Button>(R.id.stop)


        swBluetooth.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                conectarBluetooth()

            } else {
                desconectarBluetooth()

            }
        }

        swLED.setOnCheckedChangeListener { _, isChecked ->
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


    }
    private fun conectarBluetooth() {

        if (!btAdapter.isEnabled) {
            Toast.makeText(this, "Ative o Bluetooth!", Toast.LENGTH_SHORT).show()
            findViewById<SwitchCompat>(R.id.bluetooth).isChecked = false
            return
        }

        Thread {
            try {
                val dispositivo = btAdapter.getRemoteDevice(address)
                btSocket = dispositivo.createRfcommSocketToServiceRecord(MY_UUID)

                btAdapter.cancelDiscovery()
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

