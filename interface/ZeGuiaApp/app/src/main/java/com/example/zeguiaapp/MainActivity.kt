package com.example.zeguiaapp

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import java.io.IOException
import java.util.UUID

class MainActivity : AppCompatActivity() {

    // Textos do Terminal e do Cartão de Informações
    private lateinit var txtSerial: TextView
    private lateinit var txtEstadoRobo: TextView
    private lateinit var txtModoRobo: TextView
    private lateinit var txtEstrategiaRobo: TextView

    // Botões de Controle
    private lateinit var btnCalibrar: Button
    private lateinit var btnStartRun: Button
    private lateinit var btnStopRun: Button
    private lateinit var btnModeFollower: Button
    private lateinit var btnModeChase: Button
    private lateinit var btnStrategyConservative: Button
    private lateinit var btnStrategyRisk: Button

    // Variáveis do Bluetooth e Memória de Estado
    private var continuarEscutando = false
    private var modoSelecionado = false // Lembra se o modo já foi escolhido alguma vez
    private lateinit var btAdapter: BluetoothAdapter
    private var btSocket: BluetoothSocket? = null
    private val address: String = "CC:DB:A7:62:8D:96" // MAC da ESP32
    private val MY_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar os Textos
        txtEstadoRobo = findViewById(R.id.txtEstadoRobo)
        txtModoRobo = findViewById(R.id.txtModoRobo)
        txtEstrategiaRobo = findViewById(R.id.txtEstrategiaRobo)
        txtSerial = findViewById(R.id.txtSerial)

        // Inicializar os Botões
        btnCalibrar = findViewById(R.id.calibrate)
        btnStartRun = findViewById(R.id.run)
        btnStopRun = findViewById(R.id.stop)
        btnModeFollower = findViewById(R.id.follower)
        btnModeChase = findViewById(R.id.chase)
        btnStrategyConservative = findViewById(R.id.conservative)
        btnStrategyRisk = findViewById(R.id.risk)
        estadoDesconectado()

        // Permissões de Bluetooth
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN), 1)
        }

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        btAdapter = bluetoothManager.adapter

        val swBluetooth = findViewById<SwitchCompat>(R.id.bluetooth)
        val swLED = findViewById<SwitchCompat>(R.id.LED)
        val txtStatusBluetooth = findViewById<TextView>(R.id.txtStatusBluetooth)
        val txtStatusLED = findViewById<TextView>(R.id.txtStatusLED)

        atualizarCoresSwitch(swBluetooth, txtStatusBluetooth, swBluetooth.isChecked, "CONECTADO")
        atualizarCoresSwitch(swLED, txtStatusLED, swLED.isChecked, "LIGADO")

        swBluetooth.setOnCheckedChangeListener { _, isChecked ->
            atualizarCoresSwitch(swBluetooth, txtStatusBluetooth, isChecked, "CONECTADO")
            if (isChecked) conectarBluetooth() else desconectarBluetooth()
        }

        swLED.setOnCheckedChangeListener { _, isChecked ->
            atualizarCoresSwitch(swLED, txtStatusLED, isChecked, "LIGADO")
            if (isChecked) enviarComando("1") else enviarComando("0")
        }

        // Cliques dos Botões
        btnCalibrar.setOnClickListener { enviarComando("K") }
        btnModeFollower.setOnClickListener { enviarComando("S") }
        btnModeChase.setOnClickListener { enviarComando("P") }
        btnStrategyConservative.setOnClickListener { enviarComando("C") }
        btnStrategyRisk.setOnClickListener { enviarComando("A") }
        btnStartRun.setOnClickListener { enviarComando("R") }
        btnStopRun.setOnClickListener { enviarComando("F") }
    }


    private fun configurarBotao(botao: Button, habilitado: Boolean) {
        botao.isEnabled = habilitado
        botao.alpha = if (habilitado) 1.0f else 0.3f
    }

    private fun estadoDesconectado() {
        modoSelecionado = false // Esquece o modo ao desconectar
        configurarBotao(btnCalibrar, false)
        configurarBotao(btnModeFollower, false)
        configurarBotao(btnModeChase, false)
        configurarBotao(btnStrategyConservative, false)
        configurarBotao(btnStrategyRisk, false)
        configurarBotao(btnStartRun, false)
        configurarBotao(btnStopRun, false)
    }

    private fun estadoConectadoInicial() {
        // Só disponível: calibrar e modos
        configurarBotao(btnCalibrar, true)
        configurarBotao(btnModeFollower, true)
        configurarBotao(btnModeChase, true)
        configurarBotao(btnStrategyConservative, false)
        configurarBotao(btnStrategyRisk, false)
        configurarBotao(btnStartRun, false)
        configurarBotao(btnStopRun, false)
    }

    private fun estadoPosCalibracao() {
        configurarBotao(btnCalibrar, true)
        configurarBotao(btnModeFollower, true)
        configurarBotao(btnModeChase, true)

        configurarBotao(btnStrategyConservative, modoSelecionado)
        configurarBotao(btnStrategyRisk, modoSelecionado)

        configurarBotao(btnStartRun, false)
        configurarBotao(btnStopRun, false)
    }

    private fun estadoPosModo() {
        configurarBotao(btnCalibrar, false)

        configurarBotao(btnModeFollower, true)
        configurarBotao(btnModeChase, true)

        // Estratégias ficam libertas
        configurarBotao(btnStrategyConservative, true)
        configurarBotao(btnStrategyRisk, true)

        configurarBotao(btnStartRun, false)
        configurarBotao(btnStopRun, false)
    }

    private fun estadoPosEstrategia() {
        configurarBotao(btnCalibrar, false)

        configurarBotao(btnModeFollower, false)
        configurarBotao(btnModeChase, false)

        configurarBotao(btnStrategyConservative, true)
        configurarBotao(btnStrategyRisk, true)

        configurarBotao(btnStartRun, true)
        configurarBotao(btnStopRun, false)
    }

    private fun estadoCorrendo() {
        configurarBotao(btnCalibrar, false)
        configurarBotao(btnModeFollower, false)
        configurarBotao(btnModeChase, false)
        configurarBotao(btnStrategyConservative, false)
        configurarBotao(btnStrategyRisk, false)
        configurarBotao(btnStartRun, false)
        configurarBotao(btnStopRun, true)
    }

    private fun estadoFinalizado() {

        configurarBotao(btnCalibrar, true)
        configurarBotao(btnModeFollower, true) 
        configurarBotao(btnModeChase, true)
        configurarBotao(btnStrategyConservative, true)
        configurarBotao(btnStrategyRisk, true)
        configurarBotao(btnStartRun, true)
        configurarBotao(btnStopRun, false)
    }

    private fun permissaoBluetooth(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
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
                    estadoConectadoInicial()
                }

            } catch (e: IOException) {
                runOnUiThread {
                    findViewById<SwitchCompat>(R.id.bluetooth).isChecked = false
                    Toast.makeText(this, "Falha na conexão", Toast.LENGTH_SHORT).show()
                    estadoDesconectado()
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
            runOnUiThread {
                Toast.makeText(this, "Desconectado", Toast.LENGTH_SHORT).show()
                estadoDesconectado()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun atualizarCoresSwitch(switchCompat: SwitchCompat, textView: TextView, isChecked: Boolean, textoLigado: String) {
        if (isChecked) {
            textView.text = textoLigado
            textView.setTextColor(Color.parseColor("#00FF66"))
            switchCompat.thumbTintList = ColorStateList.valueOf(Color.parseColor("#FFFFFF"))
            switchCompat.trackTintList = ColorStateList.valueOf(Color.parseColor("#A066FF"))
        } else {
            textView.text = "DESLIGADO"
            textView.setTextColor(Color.parseColor("#3D285B"))
            switchCompat.thumbTintList = ColorStateList.valueOf(Color.parseColor("#A09DA5"))
            switchCompat.trackTintList = ColorStateList.valueOf(Color.parseColor("#2E1A47"))
        }
    }

    private fun receberDados() {
        val buffer = ByteArray(1024)

        Thread {
            val input = btSocket?.inputStream ?: return@Thread

            while (continuarEscutando) {
                try {
                    val bytes = input.read(buffer)

                    if (bytes > 0) {
                        val mensagem = String(buffer, 0, bytes)

                        runOnUiThread {
                            // Atualiza o Terminal e rola para baixo
                            txtSerial.append(mensagem)
                            val scroll = findViewById<ScrollView>(R.id.scrollMonitor)
                            scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }

                            val msgMinuscula = mensagem.lowercase()

                            // --- 1. VERIFICA O ESTADO ---
                            if (msgMinuscula.contains("comecando") || msgMinuscula.contains("correndo")) {
                                txtEstadoRobo.text = "Correndo"
                                txtEstadoRobo.setTextColor(Color.parseColor("#00FF66"))
                                estadoCorrendo()
                            }
                            else if (msgMinuscula.contains("calibrando") || msgMinuscula.contains("calibrado")) {
                                txtEstadoRobo.text = "Calibrando"
                                txtEstadoRobo.setTextColor(Color.parseColor("#FFFF00"))
                                estadoPosCalibracao()
                            }
                            else if (msgMinuscula.contains("finalizou") || msgMinuscula.contains("parado")) {
                                txtEstadoRobo.text = "Parado"
                                txtEstadoRobo.setTextColor(Color.parseColor("#FF2A55"))
                                estadoFinalizado()
                            }

                            // --- 2. VERIFICA O MODO ---
                            if (msgMinuscula.contains("perseguidor")) {
                                txtModoRobo.text = "Perseguidor"
                                txtModoRobo.setTextColor(Color.parseColor("#FF007F"))

                                // RESET DA ESTRATÉGIA
                                txtEstrategiaRobo.text = "Aguardando..."
                                txtEstrategiaRobo.setTextColor(Color.parseColor("#888888"))

                                modoSelecionado = true // Lembra que um modo já foi escolhido
                                estadoPosModo()
                            }
                            else if (msgMinuscula.contains("seguidor")) {
                                txtModoRobo.text = "Seguidor"
                                txtModoRobo.setTextColor(Color.parseColor("#00FFFF"))

                                // RESET DA ESTRATÉGIA
                                txtEstrategiaRobo.text = "Aguardando..."
                                txtEstrategiaRobo.setTextColor(Color.parseColor("#888888"))

                                modoSelecionado = true // Lembra que um modo já foi escolhido
                                estadoPosModo()
                            }

                            // --- 3. VERIFICA A ESTRATÉGIA ---
                            if (msgMinuscula.contains("arriscado") || msgMinuscula.contains("kamikaze")) {
                                txtEstrategiaRobo.text = "Arriscado"
                                txtEstrategiaRobo.setTextColor(Color.parseColor("#FF8800"))
                                estadoPosEstrategia()
                            }
                            else if (msgMinuscula.contains("conservador")) {
                                txtEstrategiaRobo.text = "Conservador"
                                txtEstrategiaRobo.setTextColor(Color.parseColor("#3399FF"))
                                estadoPosEstrategia()
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