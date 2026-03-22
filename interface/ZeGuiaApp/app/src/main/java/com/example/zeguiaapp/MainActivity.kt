package com.example.zeguiaapp

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import java.io.IOException
import java.util.Locale
import java.util.UUID

class MainActivity : AppCompatActivity() {

    // Textos do Terminal e do Cartão de Informações
    private lateinit var txtSerial: TextView
    private lateinit var txtEstadoRobo: TextView
    private lateinit var txtModoRobo: TextView
    private lateinit var txtEstrategiaRobo: TextView

    // Textos dos Parâmetros
    private lateinit var txtParamV: TextView
    private lateinit var txtParamKp: TextView
    private lateinit var txtParamKi: TextView
    private lateinit var txtParamKd: TextView

    // Botões de Controle
    private lateinit var btnCalibrar: Button
    private lateinit var btnStartRun: Button
    private lateinit var btnStopRun: Button
    private lateinit var btnModeFollower: Button
    private lateinit var btnModeChase: Button
    private lateinit var btnStrategyConservative: Button
    private lateinit var btnStrategyRisk: Button
    private lateinit var btnAbrirEdicao: MaterialButton

    // Variáveis do Bluetooth, Memória de Estado e Salvamento Local
    private var continuarEscutando = false
    private var modoSelecionado = false
    private lateinit var btAdapter: BluetoothAdapter
    private var btSocket: BluetoothSocket? = null
    private val address: String = "CC:DB:A7:62:8D:96" // MAC da ESP32
    private val MY_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    // Objeto para salvar os dados na memória do celular
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar a Memória do Celular
        sharedPreferences = getSharedPreferences("ZeGuiaPrefs", Context.MODE_PRIVATE)

        // Inicializar os Textos
        txtEstadoRobo = findViewById(R.id.txtEstadoRobo)
        txtModoRobo = findViewById(R.id.txtModoRobo)
        txtEstrategiaRobo = findViewById(R.id.txtEstrategiaRobo)
        txtSerial = findViewById(R.id.txtSerial)

        txtParamV = findViewById(R.id.txtParamV)
        txtParamKp = findViewById(R.id.txtParamKp)
        txtParamKi = findViewById(R.id.txtParamKi)
        txtParamKd = findViewById(R.id.txtParamKd)

        // Carregar os valores salvos (se não tiver nada, coloca "0.0" por padrão)
        txtParamV.text = sharedPreferences.getString("paramV", "0.0")
        txtParamKp.text = sharedPreferences.getString("paramKp", "0.0")
        txtParamKi.text = sharedPreferences.getString("paramKi", "0.0")
        txtParamKd.text = sharedPreferences.getString("paramKd", "0.0")

        // Inicializar os Botões
        btnCalibrar = findViewById(R.id.calibrate)
        btnStartRun = findViewById(R.id.run)
        btnStopRun = findViewById(R.id.stop)
        btnModeFollower = findViewById(R.id.follower)
        btnModeChase = findViewById(R.id.chase)
        btnStrategyConservative = findViewById(R.id.conservative)
        btnStrategyRisk = findViewById(R.id.risk)
        btnAbrirEdicao = findViewById(R.id.btnAbrirEdicao)

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

        // Clique para abrir o Pop-up de Edição
        btnAbrirEdicao.setOnClickListener { mostrarDialogEdicao() }
    }

    private fun mostrarDialogEdicao() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.editar_parametros, null)
        val builder = AlertDialog.Builder(this)
        builder.setView(dialogView)
        val alertDialog = builder.create()
        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        alertDialog.show()

        val editV = dialogView.findViewById<EditText>(R.id.editV)
        val editKp = dialogView.findViewById<EditText>(R.id.editKp)
        val editKi = dialogView.findViewById<EditText>(R.id.editKi)
        val editKd = dialogView.findViewById<EditText>(R.id.editKd)

        // Puxa os valores atuais da tela principal para as caixas de texto
        editV.setText(txtParamV.text.toString())
        editKp.setText(txtParamKp.text.toString())
        editKi.setText(txtParamKi.text.toString())
        editKd.setText(txtParamKd.text.toString())

        // Função auxiliar para os botões de + e -
        fun configurarStepper(btnMenosId: Int, btnMaisId: Int, editText: EditText) {
            dialogView.findViewById<TextView>(btnMaisId).setOnClickListener {
                val valorAtual = editText.text.toString().toFloatOrNull() ?: 0f
                // Usa Locale.US para garantir que saia com ponto (1.5) e não vírgula (1,5)
                editText.setText(String.format(Locale.US, "%.1f", valorAtual + 0.1f))
            }
            dialogView.findViewById<TextView>(btnMenosId).setOnClickListener {
                val valorAtual = editText.text.toString().toFloatOrNull() ?: 0f
                editText.setText(String.format(Locale.US, "%.1f", valorAtual - 0.1f))
            }
        }

        // Aplica o + e - em cada linha
        configurarStepper(R.id.btnDiminuirV, R.id.btnAumentarV, editV)
        configurarStepper(R.id.btnDiminuirKp, R.id.btnAumentarKp, editKp)
        configurarStepper(R.id.btnDiminuirKi, R.id.btnAumentarKi, editKi)
        configurarStepper(R.id.btnDiminuirKd, R.id.btnAumentarKd, editKd)

        dialogView.findViewById<MaterialButton>(R.id.btnCancelar).setOnClickListener {
            alertDialog.dismiss()
        }

        dialogView.findViewById<MaterialButton>(R.id.btnSalvar).setOnClickListener {
            val novoV = editV.text.toString().ifEmpty { "0.0" }
            val novoKp = editKp.text.toString().ifEmpty { "0.0" }
            val novoKi = editKi.text.toString().ifEmpty { "0.0" }
            val novoKd = editKd.text.toString().ifEmpty { "0.0" }

            // 1. Atualiza na tela
            txtParamV.text = novoV
            txtParamKp.text = novoKp
            txtParamKi.text = novoKi
            txtParamKd.text = novoKd

            // 2. Salva permanentemente na memória do celular
            sharedPreferences.edit().apply {
                putString("paramV", novoV)
                putString("paramKp", novoKp)
                putString("paramKi", novoKi)
                putString("paramKd", novoKd)
                apply() // Salva em segundo plano
            }

            // 3. Envia os novos parâmetros para o robô via Bluetooth.
            // Exemplo: O robô recebe a string "PID:1.5|2.0|0.0|0.5\n"
            enviarComando("PID:$novoV|$novoKp|$novoKi|$novoKd\n")

            alertDialog.dismiss()
        }
    }

    private fun configurarBotao(botao: Button, habilitado: Boolean) {
        botao.isEnabled = habilitado
        botao.alpha = if (habilitado) 1.0f else 0.3f
    }

    private fun estadoDesconectado() {
        modoSelecionado = false
        configurarBotao(btnCalibrar, false)
        configurarBotao(btnModeFollower, false)
        configurarBotao(btnModeChase, false)
        configurarBotao(btnStrategyConservative, false)
        configurarBotao(btnStrategyRisk, false)
        configurarBotao(btnStartRun, false)
        configurarBotao(btnStopRun, false)
    }

    private fun estadoConectadoInicial() {
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
                            txtSerial.append(mensagem)
                            val scroll = findViewById<ScrollView>(R.id.scrollMonitor)
                            scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }

                            val msgMinuscula = mensagem.lowercase()

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

                            if (msgMinuscula.contains("perseguidor")) {
                                txtModoRobo.text = "Perseguidor"
                                txtModoRobo.setTextColor(Color.parseColor("#FF007F"))
                                txtEstrategiaRobo.text = "Aguardando..."
                                txtEstrategiaRobo.setTextColor(Color.parseColor("#888888"))
                                modoSelecionado = true
                                estadoPosModo()
                            }
                            else if (msgMinuscula.contains("seguidor")) {
                                txtModoRobo.text = "Seguidor"
                                txtModoRobo.setTextColor(Color.parseColor("#00FFFF"))
                                txtEstrategiaRobo.text = "Aguardando..."
                                txtEstrategiaRobo.setTextColor(Color.parseColor("#888888"))
                                modoSelecionado = true
                                estadoPosModo()
                            }

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