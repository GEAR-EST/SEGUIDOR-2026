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
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.Locale
import java.util.UUID
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*


class MainActivity : AppCompatActivity() {

    private lateinit var txtSerial: TextView
    private lateinit var txtBatteryPercent: TextView
    private lateinit var txtEstadoRobo: TextView
    private lateinit var txtModoRobo: TextView
    private lateinit var txtEstrategiaRobo: TextView
    private lateinit var txtCronometro: TextView

    private lateinit var txtParamKp: TextView
    private lateinit var txtParamKi: TextView
    private lateinit var txtParamKd: TextView

    private lateinit var btnCalibrar: Button
    private lateinit var btnStartRun: Button
    private lateinit var btnStopRun: Button
    private lateinit var btnModeFollower: Button
    private lateinit var btnModeChase: Button
    private lateinit var btnStrategyConservative: Button
    private lateinit var btnStrategyRisk: Button
    private lateinit var btnAbrirEdicao: RelativeLayout

    private lateinit var btnExportar: Button
    private var continuarEscutando = false
    private var modoSelecionado = false
    private lateinit var btAdapter: BluetoothAdapter
    private var btSocket: BluetoothSocket? = null
    private val address: String = "CC:DB:A7:62:8D:96"
    private val MY_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private lateinit var sharedPreferences: SharedPreferences
    // Modo e estrategia selecionados para namespacing dos parametros
    private var modoAtual: String = ""
    private var estrategiaAtual: String = ""

    // Cronometro sincronizado pela mensagem do robo
    private val cronometroHandler = Handler(Looper.getMainLooper())
    private var cronometroRodando = false
    private var tempoInicioMs: Long = 0L

    private lateinit var containerCards: LinearLayout

    private var lendoSensores = false
    private val sensorHandler = Handler(Looper.getMainLooper())

    private var txtSensoresModal: TextView? = null

    private lateinit var btnLerSensores: Button

    private val atualizarCronometro = object : Runnable {
        override fun run() {
            if (!cronometroRodando) return
            val decorrido = System.currentTimeMillis() - tempoInicioMs
            val minutos = (decorrido / 1000) / 60
            val segundos = (decorrido / 1000) % 60
            val centesimos = (decorrido % 1000) / 10
            txtCronometro.text = String.format(Locale.US, "%02d:%02d:%02d", minutos, segundos, centesimos)
            cronometroHandler.postDelayed(this, 50)
        }
    }

    private val sensorPollRunnable = object : Runnable {
        override fun run() {
            if (!lendoSensores) return
            enviarComando("L")
            sensorHandler.postDelayed(this, 180) // ajuste: 120-250ms
        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPreferences = getSharedPreferences("ZeGuiaPrefs", Context.MODE_PRIVATE)

        txtEstadoRobo = findViewById(R.id.txtEstadoRobo)
        txtModoRobo = findViewById(R.id.txtModoRobo)
        txtEstrategiaRobo = findViewById(R.id.txtEstrategiaRobo)
        txtSerial = findViewById(R.id.txtSerial)
        txtCronometro = findViewById(R.id.txtCronometro)

        txtBatteryPercent = findViewById(R.id.txtBatteryPercent)

        txtParamKp = findViewById(R.id.txtParamKp)
        txtParamKi = findViewById(R.id.txtParamKi)
        txtParamKd = findViewById(R.id.txtParamKd)



        txtParamKp.text     = sharedPreferences.getString("paramKp", "0.0")
        txtParamKi.text     = sharedPreferences.getString("paramKi", "0.0")
        txtParamKd.text     = sharedPreferences.getString("paramKd", "0.0")
        txtCronometro.text  = "00:00:00"

        btnLerSensores = findViewById<Button>(R.id.btnLerSensores)
        btnCalibrar = findViewById(R.id.calibrate)
        btnStartRun = findViewById(R.id.run)
        btnStopRun = findViewById(R.id.stop)
        btnModeFollower = findViewById(R.id.follower)
        btnModeChase = findViewById(R.id.chase)
        btnStrategyConservative = findViewById(R.id.conservative)
        btnStrategyRisk = findViewById(R.id.risk)
        btnAbrirEdicao = findViewById(R.id.btnAbrirEdicao)

        btnExportar = findViewById(R.id.btnExportar)
        btnExportar.setOnClickListener { abrirModalExportar() }

        estadoDesconectado()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestPermissions(
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN),
                1
            )
        }

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        btAdapter = bluetoothManager.adapter

        val swBluetooth = findViewById<SwitchCompat>(R.id.bluetooth)
        val swLED = findViewById<SwitchCompat>(R.id.LED)


        swBluetooth.setOnCheckedChangeListener { _, isChecked ->
            atualizarCoresSwitch(swBluetooth, isChecked)
            if (isChecked) conectarBluetooth() else desconectarBluetooth()
        }

        swLED.setOnCheckedChangeListener { _, isChecked ->
            atualizarCoresSwitch(swLED, isChecked)
            if (isChecked) enviarComando("1") else enviarComando("0")
        }

        // Cliques so enviam comando; cronometro inicia/para quando chegar mensagem do robo
        btnCalibrar.setOnClickListener { enviarComando("K") }

        btnLerSensores.setOnClickListener {
            abrirModalSensores()
            //simularLeituraSensores()
        }
        btnModeFollower.setOnClickListener {
            modoAtual = "seguidor"
            estrategiaAtual = ""
            enviarComando("S")
        }
        btnModeChase.setOnClickListener {
            modoAtual = "perseguidor"
            estrategiaAtual = ""
            enviarComando("P")
        }
        btnStrategyConservative.setOnClickListener {
            estrategiaAtual = "conservador"
            carregarParametros()
            enviarComando("C")
        }
        btnStrategyRisk.setOnClickListener {
            estrategiaAtual = "arriscado"
            carregarParametros()
            enviarComando("A")
        }
        btnStartRun.setOnClickListener { enviarComando("R") }
        btnStopRun.setOnClickListener { enviarComando("F") }

        btnAbrirEdicao.setOnClickListener { mostrarDialogEdicao() }
    }

    // Retorna o prefixo de chave para o modo+estrategia atual, ex: "seguidor_conservador_"
    // Retorna o prefixo de chave para o modo+estrategia atual, ex: "seguidor_conservador_"
    private fun prefKey(): String {
        return if (modoAtual.isNotEmpty() && estrategiaAtual.isNotEmpty()) {
            "${modoAtual}_${estrategiaAtual}_"
        } else {
            ""
        }
    }

    private fun atualizarDisplayParametros() {
        val prefix = prefKey()
        txtParamKp.text = sharedPreferences.getString("${prefix}paramKp", "0.0") ?: "0.0"
        txtParamKi.text = sharedPreferences.getString("${prefix}paramKi", "0.0") ?: "0.0"
        txtParamKd.text = sharedPreferences.getString("${prefix}paramKd", "0.0") ?: "0.0"
    }

    // Carrega os parametros do modo+estrategia atual, atualiza a tela e envia para o ESP32
    private fun carregarParametros() {
        val prefix = prefKey()
        val kp     = sharedPreferences.getString("${prefix}paramKp", "0.0") ?: "0.0"
        val ki     = sharedPreferences.getString("${prefix}paramKi", "0.0") ?: "0.0"
        val kd     = sharedPreferences.getString("${prefix}paramKd", "0.0") ?: "0.0"
        val v      = sharedPreferences.getString("${prefix}paramV", "0.0") ?: "0.0"
        val velEsq = sharedPreferences.getString("${prefix}paramVelEsq", "0") ?: "0"
        val velDir = sharedPreferences.getString("${prefix}paramVelDir", "0") ?: "0"
        val marcas = sharedPreferences.getString("${prefix}paramMarcas", "0") ?: "0"

        txtParamKp.text = kp
        txtParamKi.text = ki
        txtParamKd.text = kd

        // Envia os parametros salvos desta estrategia direto para o ESP32
        enviarComando("PID:$v|$kp|$ki|$kd|$velEsq|$velDir|$marcas")
    }


    private fun mostrarDialogEdicao() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.editar_parametros, null)
        val builder = AlertDialog.Builder(this)
        builder.setView(dialogView)
        val alertDialog = builder.create()
        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        alertDialog.show()

        val editV      = dialogView.findViewById<EditText>(R.id.editV)
        val editVelEsq = dialogView.findViewById<EditText>(R.id.editVelEsq)
        val editVelDir = dialogView.findViewById<EditText>(R.id.editVelDir)
        val editKp     = dialogView.findViewById<EditText>(R.id.editKp)
        val editKi     = dialogView.findViewById<EditText>(R.id.editKi)
        val editKd     = dialogView.findViewById<EditText>(R.id.editKd)
        val editMarcas = dialogView.findViewById<EditText>(R.id.editMarcas)

        val prefix = prefKey()
        editV.setText(sharedPreferences.getString("${prefix}paramV", "0.0"))
        editVelEsq.setText(sharedPreferences.getString("${prefix}paramVelEsq", "0"))
        editVelDir.setText(sharedPreferences.getString("${prefix}paramVelDir", "0"))
        editKp.setText(sharedPreferences.getString("${prefix}paramKp", "0.0"))
        editKi.setText(sharedPreferences.getString("${prefix}paramKi", "0.0"))
        editKd.setText(sharedPreferences.getString("${prefix}paramKd", "0.0"))
        editMarcas.setText(sharedPreferences.getString("${prefix}paramMarcas", "0"))

        // Stepper para floats (passo 0.1)
        fun configurarStepper(btnMenosId: Int, btnMaisId: Int, editText: EditText) {
            dialogView.findViewById<TextView>(btnMaisId).setOnClickListener {
                val v = editText.text.toString().toFloatOrNull() ?: 0f
                editText.setText(String.format(Locale.US, "%.1f", v + 0.1f))
            }
            dialogView.findViewById<TextView>(btnMenosId).setOnClickListener {
                val v = editText.text.toString().toFloatOrNull() ?: 0f
                editText.setText(String.format(Locale.US, "%.1f", v - 0.1f))
            }
        }

        // Stepper para inteiros (passo 1)
        fun configurarStepperInt(btnMenosId: Int, btnMaisId: Int, editText: EditText) {
            dialogView.findViewById<TextView>(btnMaisId).setOnClickListener {
                val v = editText.text.toString().toIntOrNull() ?: 0
                editText.setText((v + 1).toString())
            }
            dialogView.findViewById<TextView>(btnMenosId).setOnClickListener {
                val v = editText.text.toString().toIntOrNull() ?: 0
                editText.setText((v - 1).toString())
            }
        }

        configurarStepper(R.id.btnDiminuirV, R.id.btnAumentarV, editV)
        configurarStepperInt(R.id.btnDiminuirVelEsq, R.id.btnAumentarVelEsq, editVelEsq)
        configurarStepperInt(R.id.btnDiminuirVelDir, R.id.btnAumentarVelDir, editVelDir)
        configurarStepper(R.id.btnDiminuirKp, R.id.btnAumentarKp, editKp)
        configurarStepper(R.id.btnDiminuirKi, R.id.btnAumentarKi, editKi)
        configurarStepper(R.id.btnDiminuirKd, R.id.btnAumentarKd, editKd)
        configurarStepperInt(R.id.btnDiminuirMarcas, R.id.btnAumentarMarcas, editMarcas)

        dialogView.findViewById<MaterialButton>(R.id.btnCancelar).setOnClickListener {
            alertDialog.dismiss()
        }

        dialogView.findViewById<MaterialButton>(R.id.btnSalvar).setOnClickListener {
            val novoV      = editV.text.toString().ifEmpty { "0.0" }
            val novoVelEsq = editVelEsq.text.toString().ifEmpty { "0" }
            val novoVelDir = editVelDir.text.toString().ifEmpty { "0" }
            val novoKp     = editKp.text.toString().ifEmpty { "0.0" }
            val novoKi     = editKi.text.toString().ifEmpty { "0.0" }
            val novoKd     = editKd.text.toString().ifEmpty { "0.0" }
            val novoMarcas = editMarcas.text.toString().ifEmpty { "0" }

            txtParamKp.text     = novoKp
            txtParamKi.text     = novoKi
            txtParamKd.text     = novoKd


            sharedPreferences.edit().apply {
                putString("${prefix}paramV",      novoV)
                putString("${prefix}paramVelEsq", novoVelEsq)
                putString("${prefix}paramVelDir", novoVelDir)
                putString("${prefix}paramKp",     novoKp)
                putString("${prefix}paramKi",     novoKi)
                putString("${prefix}paramKd",     novoKd)
                putString("${prefix}paramMarcas", novoMarcas)
                apply()
            }

            // Formato firmware: PID:V|Kp|Ki|Kd|VelEsq|VelDir|NovasMarcas
            enviarComando("PID:$novoV|$novoKp|$novoKi|$novoKd|$novoVelEsq|$novoVelDir|$novoMarcas")
            alertDialog.dismiss()
        }
    }

    private fun iniciarCronometro() {
        if (cronometroRodando) return
        cronometroRodando = true
        tempoInicioMs = System.currentTimeMillis()
        cronometroHandler.post(atualizarCronometro)
    }

    private fun pararCronometro() {
        cronometroRodando = false
        cronometroHandler.removeCallbacks(atualizarCronometro)
    }

    private fun resetarCronometro() {
        pararCronometro()
        txtCronometro.text = "00:00:00"
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
        configurarBotao(btnLerSensores, false) // sem conexão, desabilitado
        configurarBotao(btnExportar, false)
    }

    private fun estadoConectadoInicial() {
        configurarBotao(btnCalibrar, true)
        configurarBotao(btnModeFollower, true)
        configurarBotao(btnModeChase, true)
        configurarBotao(btnLerSensores, true)
        configurarBotao(btnStrategyConservative, false)
        configurarBotao(btnStrategyRisk, false)
        configurarBotao(btnStartRun, false)
        configurarBotao(btnStopRun, false)
        configurarBotao(btnExportar, false)
    }

    private fun estadoCalibrando() {
        configurarBotao(btnCalibrar, false)
        configurarBotao(btnModeFollower, false)
        configurarBotao(btnModeChase, false)
        configurarBotao(btnLerSensores, false)
        configurarBotao(btnStrategyConservative, false)
        configurarBotao(btnStrategyRisk, false)
        configurarBotao(btnStartRun, false)
        configurarBotao(btnStopRun, false)
        configurarBotao(btnExportar, false)
    }

    private fun estadoPosCalibracao() {
        configurarBotao(btnCalibrar, true)
        configurarBotao(btnModeFollower, true)
        configurarBotao(btnModeChase, true)
        configurarBotao(btnLerSensores, true)
        configurarBotao(btnStrategyConservative, modoSelecionado)
        configurarBotao(btnStrategyRisk, modoSelecionado)
        configurarBotao(btnStartRun, false)
        configurarBotao(btnStopRun, false)
        configurarBotao(btnExportar, false)
    }

    private fun estadoPosModo() {
        configurarBotao(btnCalibrar, false)
        configurarBotao(btnModeFollower, true)
        configurarBotao(btnModeChase, true)
        configurarBotao(btnLerSensores, true)
        configurarBotao(btnStrategyConservative, true)
        configurarBotao(btnStrategyRisk, true)
        configurarBotao(btnStartRun, false)
        configurarBotao(btnStopRun, false)
        configurarBotao(btnExportar, false)
    }

    private fun estadoPosEstrategia() {
        configurarBotao(btnCalibrar, false)
        configurarBotao(btnModeFollower, false)
        configurarBotao(btnModeChase, false)
        configurarBotao(btnLerSensores, true)
        configurarBotao(btnStrategyConservative, true)
        configurarBotao(btnStrategyRisk, true)
        configurarBotao(btnStartRun, true)
        configurarBotao(btnStopRun, false)
        configurarBotao(btnExportar, false)
    }

    private fun estadoCorrendo() {
        configurarBotao(btnCalibrar, false)
        configurarBotao(btnModeFollower, false)
        configurarBotao(btnModeChase, false)
        configurarBotao(btnStrategyConservative, false)
        configurarBotao(btnStrategyRisk, false)
        configurarBotao(btnStartRun, false)
        configurarBotao(btnLerSensores, false)
        configurarBotao(btnStopRun, true)
        configurarBotao(btnExportar, false)
    }

    private fun estadoFinalizado() {
        configurarBotao(btnCalibrar, true)
        configurarBotao(btnModeFollower, true)
        configurarBotao(btnModeChase, true)
        configurarBotao(btnStrategyConservative, true)
        configurarBotao(btnStrategyRisk, true)
        configurarBotao(btnStartRun, true)
        configurarBotao(btnStopRun, false)
        configurarBotao(btnLerSensores, true)
        configurarBotao(btnExportar, true)
    }

    private fun permissaoBluetooth(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
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
                btAdapter.cancelDiscovery()
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
            modoAtual = ""
            estrategiaAtual = ""
            runOnUiThread {
                Toast.makeText(this, "Desconectado", Toast.LENGTH_SHORT).show()
                estadoDesconectado()
                resetarCronometro()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun atualizarCoresSwitch(
        switchCompat: SwitchCompat,
        isChecked: Boolean
    ) {
        if (isChecked) {
            switchCompat.thumbTintList = ColorStateList.valueOf(Color.parseColor("#FFFFFF"))
            switchCompat.trackTintList = ColorStateList.valueOf(Color.parseColor("#A066FF"))
        } else {
            switchCompat.thumbTintList = ColorStateList.valueOf(Color.parseColor("#A09DA5"))
            switchCompat.trackTintList = ColorStateList.valueOf(Color.parseColor("#2E1A47"))
        }
    }

    private fun receberDados() {
        Thread {
            val input = btSocket?.inputStream ?: return@Thread
            val reader = BufferedReader(InputStreamReader(input))

            while (continuarEscutando) {
                try {
                    val mensagem = reader.readLine() ?: break

                    runOnUiThread {
                        val msgLimpa  = mensagem.trim()
                        when {
                            msgLimpa.startsWith("BAT") -> {
                                val valor = msgLimpa.removePrefix("BAT").trim()
                                val percentual = valor.toIntOrNull()

                                if (percentual != null) {
                                    txtBatteryPercent.text = "$percentual%"

                                    val corAtiva = when {
                                        percentual > 50 -> Color.parseColor("#00E5FF") // Azul
                                        percentual > 20 -> Color.parseColor("#FFAA00") // Laranja
                                        else -> Color.parseColor("#FF2A55") // Vermelho
                                    }

                                    val corInativa = Color.parseColor("#2E1A47")


                                    txtBatteryPercent.setTextColor(corAtiva)

                                    val barrasAcesas = Math.ceil((percentual / 100.0) * 6).toInt()


                                    val layoutBarras = findViewById<LinearLayout>(R.id.layoutBarrasBateria)

                                    for (i in 0 until 6) {
                                        val barra = layoutBarras.getChildAt(i)
                                        if (i < barrasAcesas) {

                                            barra.setBackgroundColor(corAtiva)
                                        } else {

                                            barra.setBackgroundColor(corInativa)
                                        }
                                    }
                                }
                            }

                            mensagem.trim().startsWith("S,")-> {
                                // Sensores: atualiza SOMENTE o modal
                                txtSensoresModal?.text = formatarSensoresBonito(mensagem)
                            }

                            else -> {
                                txtSerial.append("$mensagem\n")
                                val scroll = findViewById<ScrollView>(R.id.scrollMonitor)
                                scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }

                                val msgMinuscula = mensagem.lowercase()

                                if (msgMinuscula.contains("comecando") || msgMinuscula.contains("correndo")) {
                                    txtEstadoRobo.text = "Correndo"
                                    txtEstadoRobo.setTextColor(Color.parseColor("#00FF66"))
                                    estadoCorrendo()
                                    iniciarCronometro()
                                } else if (msgMinuscula.contains("calibrado")) {
                                    if (txtEstadoRobo.text != "Calibrado") {
                                        txtEstadoRobo.text = "Calibrado"
                                        txtEstadoRobo.setTextColor(Color.parseColor("#9d00ff"))
                                        estadoPosCalibracao()
                                    }
                                } else if (msgMinuscula.contains("calibrando")) {
                                    if (txtEstadoRobo.text != "Calibrando") {
                                        txtEstadoRobo.text = "Calibrando"
                                        txtEstadoRobo.setTextColor(Color.parseColor("#FFFF00"))
                                        estadoCalibrando()
                                    }
                                } else if (msgMinuscula.contains("finalizou") || msgMinuscula.contains("parado")) {
                                    txtEstadoRobo.text = "Parado"
                                    txtEstadoRobo.setTextColor(Color.parseColor("#FF2A55"))
                                    estadoFinalizado()
                                    pararCronometro()
                                }

                                if (msgMinuscula.contains("perseguidor")) {
                                    txtModoRobo.text = "Perseguidor"
                                    txtModoRobo.setTextColor(Color.parseColor("#FF007F"))
                                    txtEstrategiaRobo.text = "Aguardando..."
                                    txtEstrategiaRobo.setTextColor(Color.parseColor("#888888"))
                                    modoAtual = "perseguidor"
                                    estrategiaAtual = ""
                                    modoSelecionado = true
                                    estadoPosModo()
                                } else if (msgMinuscula.contains("seguidor")) {
                                    txtModoRobo.text = "Seguidor"
                                    txtModoRobo.setTextColor(Color.parseColor("#00FFFF"))
                                    txtEstrategiaRobo.text = "Aguardando..."
                                    txtEstrategiaRobo.setTextColor(Color.parseColor("#888888"))
                                    modoAtual = "seguidor"
                                    estrategiaAtual = ""
                                    modoSelecionado = true
                                    estadoPosModo()
                                }

                                if (msgMinuscula.contains("arriscado") || msgMinuscula.contains("kamikaze")) {
                                    estrategiaAtual = "arriscado"
                                    txtEstrategiaRobo.text = "Arriscado"
                                    txtEstrategiaRobo.setTextColor(Color.parseColor("#FF8800"))
                                    atualizarDisplayParametros()
                                    estadoPosEstrategia()
                                } else if (msgMinuscula.contains("conservador")) {
                                    estrategiaAtual = "conservador"
                                    txtEstrategiaRobo.text = "Conservador"
                                    txtEstrategiaRobo.setTextColor(Color.parseColor("#3399FF"))
                                    atualizarDisplayParametros()
                                    estadoPosEstrategia()
                                }
                            }
                        }
                    }
                } catch (e: IOException) {
                    break
                }
            }
        }.start()
    }

    private fun formatarSensoresBonito(mensagem: String): String {
        val p = mensagem.trim().split(",").map { it.trim() }
        if (p.size < 10 || p[0] != "S") return "Aguardando sensores..."

        val pos = p.getOrNull(1) ?: "-"
        val frontais = (2..9).map { idx -> p.getOrNull(idx) ?: "-" }.joinToString(" | ")
        val direito = p.getOrNull(10) ?: "-"
        val esquerdo = p.getOrNull(11) ?: "-"

        return "POS: $pos\n$frontais\nDIR: $direito | ESQ: $esquerdo"
    }



    private fun enviarComando(sinal: String) {
        if (btSocket == null) {
            Toast.makeText(this, "Não está conectado!", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val payload = if (sinal.endsWith("\n")) sinal else "$sinal\n"
            btSocket?.outputStream?.write(payload.toByteArray())
        } catch (e: IOException) {
            Toast.makeText(this, "Erro ao enviar dados", Toast.LENGTH_SHORT).show()
        }
    }



    private fun abrirModalSensores() {
        val builder = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.modal_sensores, null)
        builder.setView(dialogView)

        txtSensoresModal = dialogView.findViewById(R.id.txtValoresSensores)

        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        txtSensoresModal?.text = "Aguardando sensores..."

        val btnFechar = dialogView.findViewById<ImageView>(R.id.btnFecharSensores)
        btnFechar.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()

        enviarComando("L") // liga stream no firmware

        dialog.setOnDismissListener {
            enviarComando("l") // desliga stream no firmware
            txtSensoresModal = null
        }
    }

    private fun abrirModalExportar() {
        val builder = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.modal_exportar, null)
        builder.setView(dialogView)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val txtResumoDados = dialogView.findViewById<TextView>(R.id.txtResumoDados)
        val editObservacoes = dialogView.findViewById<EditText>(R.id.editObservacoes)
        val btnCopiarResumo = dialogView.findViewById<Button>(R.id.btnCopiarResumo)
        val btnFechar = dialogView.findViewById<ImageView>(R.id.btnFecharExportar)
        val rgStatusCorrida = dialogView.findViewById<android.widget.RadioGroup>(R.id.rgStatusCorrida)

        // 1. Iniciar o botão "Copiar" como DESABILITADO
        btnCopiarResumo.isEnabled = false
        btnCopiarResumo.alpha = 0.4f // Deixa o botão meio transparente

        var statusCorridaSelecionado = ""

        // 2. Ouvinte para quando o usuário escolher uma opção (Sim, Parcial, Não)
        rgStatusCorrida.setOnCheckedChangeListener { _, checkedId ->
            // Habilita o botão "Copiar"
            btnCopiarResumo.isEnabled = true
            btnCopiarResumo.alpha = 1.0f

            // Salva a resposta de acordo com a caixa marcada
            statusCorridaSelecionado = when (checkedId) {
                R.id.rbSim -> "Sim (Completa)"
                R.id.rbParcial -> "Parcialmente"
                R.id.rbNao -> "Não (Interrompida)"
                else -> "Indefinido"
            }
        }

        // Formatar Data e Hora atual
        val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
        val dataHora = dateFormat.format(java.util.Date())

        // Pegar informações da tela e memória
        val tempo = txtCronometro.text.toString()
        val modo = txtModoRobo.text.toString()
        val estrategia = txtEstrategiaRobo.text.toString()
        val kp = txtParamKp.text.toString()
        val ki = txtParamKi.text.toString()
        val kd = txtParamKd.text.toString()
        val exportPrefix = prefKey()
        val velMax = sharedPreferences.getString("${exportPrefix}paramV", "0.0")
        val velEsq = sharedPreferences.getString("${exportPrefix}paramVelEsq", "0")
        val velDir = sharedPreferences.getString("${exportPrefix}paramVelDir", "0")
        val marcas = sharedPreferences.getString("${exportPrefix}paramMarcas", "0")


        // Montar a String formatada apenas visual (sem o status, pois ele será escolhido)
        val resumoVisual = """
        [FEEDBACK DE CORRIDA - ZÉ-GUIA]
        Data/Hora: $dataHora
        Tempo Final: $tempo
        
        > Configurações:
        Modo: $modo
        Estratégia: $estrategia
        Qtd. Marcas: $marcas
        
        > Parâmetros PID:
        Vel. Máx: $velMax
        Vel. Esq: $velEsq | Vel. Dir: $velDir
        Kp: $kp | Ki: $ki | Kd: $kd
    """.trimIndent()

        txtResumoDados.text = resumoVisual

        // 3. Ação do Botão Copiar (quando habilitado)
        btnCopiarResumo.setOnClickListener {

            // Agora nós reconstruímos o texto final inserindo a resposta do Status:
            val resumoFinal = """
            [FEEDBACK DE CORRIDA - ZÉ-GUIA]
            Data/Hora: $dataHora
            Status: $statusCorridaSelecionado
            Tempo Final: $tempo
            
            > Configurações:
            Modo: $modo
            Estratégia: $estrategia
            Qtd. Marcas: $marcas
            
            > Parâmetros PID:
            Vel. Máx: $velMax
            Vel. Esq: $velEsq | Vel. Dir: $velDir
            Kp: $kp | Ki: $ki | Kd: $kd
        """.trimIndent()

            val obs = editObservacoes.text.toString()
            val textoFinalParaCopiar = if (obs.isNotEmpty()) {
                "$resumoFinal\n\n> Observações:\n$obs"
            } else {
                resumoFinal
            }

            // Chamar o serviço do Android que copia o texto
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Feedback do Zé-Guia", textoFinalParaCopiar)
            clipboard.setPrimaryClip(clip)

            Toast.makeText(this, "Feedback copiado com sucesso!", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        // Ação de fechar no X
        btnFechar.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        continuarEscutando = false
        pararCronometro()
        btSocket?.close()
    }
}