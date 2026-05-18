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
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.Locale
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var txtSerial: TextView

    private lateinit var icon_Bluetooth: ImageView
    private lateinit var txtBatteryPercent: TextView
    private lateinit var txtEstadoRobo: TextView
    private lateinit var txtModoRobo: TextView
    private lateinit var txtEstrategiaRobo: TextView
    private lateinit var txtCronometro: TextView
    private lateinit var txtEspMac: TextView

    private lateinit var txtParamKp: TextView
    private lateinit var txtParamKi: TextView
    private lateinit var txtParamKd: TextView
    private lateinit var txtParamVR: TextView

    private lateinit var btnCalibrar: Button
    private lateinit var btnStartRun: Button
    private lateinit var btnStopRun: Button
    private lateinit var btnModeFollower: Button
    private lateinit var btnModeChase: Button
    private lateinit var btnStrategyConservative: Button
    private lateinit var btnStrategyRisk: Button
    private lateinit var btnAbrirEdicao: LinearLayout
    private lateinit var iconEditarParams: ImageView

    private lateinit var btnExportar: Button
    private var continuarEscutando = false
    private var modoSelecionado = false
    private lateinit var btAdapter: BluetoothAdapter
    private var btSocket: BluetoothSocket? = null
    private var address: String = " "
    private var savedMacs: MutableSet<String> = mutableSetOf()

    private val MY_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private lateinit var sharedPreferences: SharedPreferences
    // Modo e estrategia selecionados para namespacing dos parametros
    private var modoAtual: String = ""
    private var estrategiaAtual: String = ""

    // Cronometro sincronizado pela mensagem do robo
    private val cronometroHandler = Handler(Looper.getMainLooper())
    private var cronometroRodando = false
    private var tempoInicioMs: Long = 0L

    private var lendoSensores = false
    private val sensorHandler = Handler(Looper.getMainLooper())

    private var txtSensoresModal: TextView? = null

    private lateinit var btnLerSensores: Button

    // ============ CRONÔMETRO: formato SS.cc (segundos com 2 casas) ============
    private val atualizarCronometro = object : Runnable {
        override fun run() {
            if (!cronometroRodando) return
            val decorrido = System.currentTimeMillis() - tempoInicioMs
            val segundos = decorrido / 1000
            val centesimos = (decorrido % 1000) / 10
            // Mostra apenas segundos com 2 casas decimais. Ex: "98.50"
            txtCronometro.text = String.format(Locale.US, "%d.%02d", segundos, centesimos)
            cronometroHandler.postDelayed(this, 50)
        }
    }

    private val sensorPollRunnable = object : Runnable {
        override fun run() {
            if (!lendoSensores) return
            enviarComando("L")
            sensorHandler.postDelayed(this, 180)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<View>(R.id.cardConfigEsp).setOnClickListener { abrirModalConfigEsp32() }

        sharedPreferences = getSharedPreferences("ZeGuiaPrefs", Context.MODE_PRIVATE)

        savedMacs = sharedPreferences.getStringSet("savedMacs", mutableSetOf("C0:49:EF:65:16:FE"))?.toMutableSet() ?: mutableSetOf("C0:49:EF:65:16:FE")

        address = sharedPreferences.getString("selectedMac", "C0:49:EF:65:16:FE") ?: "C0:49:EF:65:16:FE"

        txtEspMac = findViewById(R.id.txtEspMac)
        txtEspMac.text = address

        txtEstadoRobo = findViewById(R.id.txtEstadoRobo)
        txtModoRobo = findViewById(R.id.txtModoRobo)
        txtEstrategiaRobo = findViewById(R.id.txtEstrategiaRobo)
        txtSerial = findViewById(R.id.txtSerial)
        txtCronometro = findViewById(R.id.txtCronometro)

        txtBatteryPercent = findViewById(R.id.txtBatteryPercent)

        txtParamKp = findViewById(R.id.txtParamKp)
        txtParamKi = findViewById(R.id.txtParamKi)
        txtParamKd = findViewById(R.id.txtParamKd)
        txtParamVR = findViewById(R.id.txtParamVR)

        txtCronometro.text = "0.00"

        modoAtual       = sharedPreferences.getString("lastModoAtual", "") ?: ""
        estrategiaAtual = sharedPreferences.getString("lastEstrategiaAtual", "") ?: ""
        atualizarDisplayParametros()

        btnLerSensores = findViewById(R.id.btnLerSensores)
        btnCalibrar = findViewById(R.id.calibrate)
        btnStartRun = findViewById(R.id.run)
        btnStopRun = findViewById(R.id.stop)
        btnModeFollower = findViewById(R.id.follower)
        btnModeChase = findViewById(R.id.chase)
        btnStrategyConservative = findViewById(R.id.conservative)
        btnStrategyRisk = findViewById(R.id.risk)
        btnAbrirEdicao = findViewById(R.id.btnAbrirEdicao)
        iconEditarParams = findViewById(R.id.iconEditarParams)

        btnExportar = findViewById(R.id.btnExportar)
        btnExportar.setOnClickListener { abrirModalExportar() }
        icon_Bluetooth = findViewById(R.id.iconBluetooth)

        // Atualiza o estado inicial dos parâmetros (sem estratégia = desabilitado)
        atualizarEstadoEdicaoParametros()

        estadoDesconectado()
        atualizarIconeBluetooth(false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN),
                1
            )
        }

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        btAdapter = bluetoothManager.adapter

        val swBluetooth = findViewById<SwitchCompat>(R.id.bluetooth)
        swBluetooth.setOnCheckedChangeListener { _, isChecked ->
            atualizarCoresSwitch(swBluetooth, isChecked)
            if (isChecked) {
                conectarBluetooth()
            } else {
                desconectarBluetooth()
            }
        }

        btnCalibrar.setOnClickListener { enviarComando("K") }

        btnLerSensores.setOnClickListener {
            abrirModalSensores()
        }
        btnModeFollower.setOnClickListener {
            modoAtual = "seguidor"
            estrategiaAtual = ""
            atualizarEstadoEdicaoParametros()
            enviarComando("S")
        }
        btnModeChase.setOnClickListener {
            modoAtual = "perseguidor"
            estrategiaAtual = ""
            atualizarEstadoEdicaoParametros()
            enviarComando("P")
        }
        btnStrategyConservative.setOnClickListener {
            estrategiaAtual = "conservador"
            enviarComando("C")
            carregarParametros()
            atualizarEstadoEdicaoParametros()
        }
        btnStrategyRisk.setOnClickListener {
            estrategiaAtual = "arriscado"
            enviarComando("A")
            carregarParametros()
            atualizarEstadoEdicaoParametros()
        }
        btnStartRun.setOnClickListener { enviarComando("R") }
        btnStopRun.setOnClickListener { enviarComando("F") }

        btnAbrirEdicao.setOnClickListener {
            // Só abre se tiver estratégia selecionada (segurança extra além do isEnabled)
            if (estrategiaAtual.isEmpty()) {
                Toast.makeText(this, "Selecione um modo e uma estratégia antes de editar parâmetros", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            mostrarDialogEdicao()
        }
    }

    /**
     * Habilita/desabilita o botão de editar parâmetros (PARÂMETROS card).
     * - Quando NÃO há estratégia selecionada: ícone cinza, alpha reduzido, sem clique
     * - Quando HÁ estratégia: ícone roxo, alpha cheio, clicável
     */
    private fun atualizarEstadoEdicaoParametros() {
        val temEstrategia = estrategiaAtual.isNotEmpty()
        btnAbrirEdicao.isEnabled = temEstrategia
        btnAbrirEdicao.isClickable = temEstrategia
        btnAbrirEdicao.alpha = if (temEstrategia) 1.0f else 0.55f
        iconEditarParams.setColorFilter(
            if (temEstrategia) Color.parseColor("#A066FF") else Color.parseColor("#5A4470")
        )
    }

    private fun abrirModalConfigEsp32() {
        val estadoAtual = txtEstadoRobo.text.toString()
        val podeEditar = (btSocket == null) || (estadoAtual == "Parado")

        if (!podeEditar) {
            Toast.makeText(
                this,
                "Finalize a corrida ou desconecte o Bluetooth antes de alterar a ESP32",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val builder = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.modal_esp32, null)
        builder.setView(dialogView)

        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val spinnerMacs = dialogView.findViewById<Spinner>(R.id.spinnerMacs)
        val editNovoMac = dialogView.findViewById<EditText>(R.id.editNovoMac)
        val btnSalvarEsp = dialogView.findViewById<Button>(R.id.btnSalvarEsp)
        val btnFechar = dialogView.findViewById<ImageView>(R.id.btnFecharModalEsp)

        if (savedMacs.isEmpty()) {
            savedMacs.add("C0:49:EF:65:16:FE")
        }

        val macList = savedMacs.toList()
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, macList)
        spinnerMacs.adapter = adapter

        val indexAtual = macList.indexOf(address)
        if (indexAtual >= 0) {
            spinnerMacs.setSelection(indexAtual)
        }

        editNovoMac.addTextChangedListener(object : android.text.TextWatcher {
            private var isFormatting = false
            private var deletingColon = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                deletingColon = count == 1 && after == 0 && s?.getOrNull(start) == ':'
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: android.text.Editable?) {
                if (isFormatting) return
                isFormatting = true

                val clean = s.toString().replace(":", "").uppercase()
                val texto = if (deletingColon && clean.length >= 2) {
                    clean.substring(0, clean.length - 1)
                } else {
                    clean
                }

                val formatted = StringBuilder()
                texto.forEachIndexed { index, char ->
                    if (index > 0 && index % 2 == 0) {
                        formatted.append(":")
                    }
                    formatted.append(char)
                }

                val result = formatted.toString().take(17)

                if (result != s.toString()) {
                    s?.replace(0, s.length, result)
                    editNovoMac.setSelection(result.length)
                }

                isFormatting = false
                deletingColon = false
            }
        })

        btnFechar.setOnClickListener { dialog.dismiss() }

        btnSalvarEsp.setOnClickListener {
            val macDigitado = editNovoMac.text.toString().trim()

            if (macDigitado.isNotEmpty()) {
                val isValid = macDigitado.matches(Regex("^([0-9A-F]{2}:){5}[0-9A-F]{2}$"))

                if (isValid) {
                    savedMacs.add(macDigitado)
                    address = macDigitado
                    sharedPreferences.edit()
                        .putStringSet("savedMacs", HashSet(savedMacs))
                        .putString("selectedMac", address)
                        .apply()
                    txtEspMac.text = address
                    Toast.makeText(this, "Nova ESP32 cadastrada!", Toast.LENGTH_SHORT).show()
                    if (btSocket != null) {
                        findViewById<SwitchCompat>(R.id.bluetooth).isChecked = false
                    }
                    dialog.dismiss()
                } else {
                    Toast.makeText(this, "MAC incompleto! Digite 12 dígitos (XX:XX:XX:XX:XX:XX)", Toast.LENGTH_LONG).show()
                }
            } else {
                if (spinnerMacs.selectedItem != null) {
                    val macSelecionado = spinnerMacs.selectedItem.toString()

                    if (address != macSelecionado) {
                        address = macSelecionado
                        sharedPreferences.edit().putString("selectedMac", address).apply()
                        txtEspMac.text = address
                        Toast.makeText(this, "ESP32 alterada para $address", Toast.LENGTH_SHORT).show()

                        if (btSocket != null) {
                            findViewById<SwitchCompat>(R.id.bluetooth).isChecked = false
                        }
                    }
                    dialog.dismiss()
                } else {
                    Toast.makeText(this, "Selecione uma ESP32 ou digite um novo MAC", Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialog.show()
    }

    private fun prefKey(): String {
        return if (modoAtual.isNotEmpty() && estrategiaAtual.isNotEmpty()) {
            "${modoAtual}_${estrategiaAtual}_"
        } else {
            ""
        }
    }

    private fun atualizarDisplayParametros() {
        val prefix = prefKey()
        if (prefix.isEmpty()) {
            txtParamKp.text = "--"
            txtParamKi.text = "--"
            txtParamKd.text = "--"
            txtParamVR.text = "--"
            return
        }
        txtParamKp.text = sharedPreferences.getString("${prefix}paramKp", "--") ?: "--"
        txtParamKi.text = sharedPreferences.getString("${prefix}paramKi", "--") ?: "--"
        txtParamKd.text = sharedPreferences.getString("${prefix}paramKd", "--") ?: "--"
        txtParamVR.text = sharedPreferences.getString("${prefix}paramV", "--") ?: "--"
    }

    private fun carregarParametros() {
        val prefix = prefKey()
        val kp     = sharedPreferences.getString("${prefix}paramKp", "0.0") ?: "0.0"
        val ki     = sharedPreferences.getString("${prefix}paramKi", "0.0") ?: "0.0"
        val kd     = sharedPreferences.getString("${prefix}paramKd", "0.0") ?: "0.0"
        val v      = sharedPreferences.getString("${prefix}paramV", "0") ?: "0"
        val marcas = sharedPreferences.getString("${prefix}paramMarcas", "0") ?: "0"

        txtParamKp.text = kp
        txtParamKi.text = ki
        txtParamKd.text = kd
        txtParamVR.text = v

        enviarComando("PID:$v|$kp|$ki|$kd|$marcas")
    }


    private fun mostrarDialogEdicao() {
        if (prefKey().isEmpty()) {
            Toast.makeText(this, "Selecione modo e estratégia antes de editar parâmetros", Toast.LENGTH_SHORT).show()
            return
        }
        val dialogView = LayoutInflater.from(this).inflate(R.layout.editar_parametros, null)
        val builder = AlertDialog.Builder(this)
        builder.setView(dialogView)
        val alertDialog = builder.create()
        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        alertDialog.show()

        val editV      = dialogView.findViewById<EditText>(R.id.editV)
        val editKp     = dialogView.findViewById<EditText>(R.id.editKp)
        val editKi     = dialogView.findViewById<EditText>(R.id.editKi)
        val editKd     = dialogView.findViewById<EditText>(R.id.editKd)
        val editMarcas = dialogView.findViewById<EditText>(R.id.editMarcas)

        val prefix = prefKey()

        val vSalvo = sharedPreferences.getString("${prefix}paramV", "0")
            ?.toFloatOrNull()?.toInt()?.toString() ?: "0"
        editV.setText(vSalvo)

        fun normFloat(key: String): String {
            val raw = sharedPreferences.getString("${prefix}$key", "0.0") ?: "0.0"
            val parsed = raw.replace(",", ".").toFloatOrNull() ?: 0f
            return String.format(Locale.US, "%.2f", parsed)
        }
        editKp.setText(normFloat("paramKp"))
        editKi.setText(normFloat("paramKi"))
        editKd.setText(normFloat("paramKd"))

        val marcasSalvo = sharedPreferences.getString("${prefix}paramMarcas", "0")
            ?.toFloatOrNull()?.toInt()?.toString() ?: "0"
        editMarcas.setText(marcasSalvo)


        fun configurarStepper(btnMenosId: Int, btnMaisId: Int, editText: EditText) {
            dialogView.findViewById<TextView>(btnMaisId).setOnClickListener {
                val v = editText.text.toString().toFloatOrNull() ?: 0f
                editText.setText(String.format(Locale.US, "%.2f", v + 0.01f))
            }
            dialogView.findViewById<TextView>(btnMenosId).setOnClickListener {
                val v = editText.text.toString().toFloatOrNull() ?: 0f
                editText.setText(String.format(Locale.US, "%.2f", v - 0.01f))
            }
        }

        fun configurarStepperInt(btnMenosId: Int, btnMaisId: Int, editText: EditText) {
            dialogView.findViewById<TextView>(btnMaisId).setOnClickListener {
                val v = editText.text.toString().toFloatOrNull()?.toInt() ?: 0
                editText.setText((v + 1).toString())
            }
            dialogView.findViewById<TextView>(btnMenosId).setOnClickListener {
                val v = editText.text.toString().toFloatOrNull()?.toInt() ?: 0
                editText.setText((v - 1).toString())
            }
        }

        configurarStepperInt(R.id.btnDiminuirV, R.id.btnAumentarV, editV)
        configurarStepper(R.id.btnDiminuirKp, R.id.btnAumentarKp, editKp)
        configurarStepper(R.id.btnDiminuirKi, R.id.btnAumentarKi, editKi)
        configurarStepper(R.id.btnDiminuirKd, R.id.btnAumentarKd, editKd)
        configurarStepperInt(R.id.btnDiminuirMarcas, R.id.btnAumentarMarcas, editMarcas)

        dialogView.findViewById<MaterialButton>(R.id.btnCancelar).setOnClickListener {
            alertDialog.dismiss()
        }

        dialogView.findViewById<MaterialButton>(R.id.btnSalvar).setOnClickListener {
            val novoV      = editV.text.toString().ifEmpty { "0" }
            val novoKp     = editKp.text.toString().ifEmpty { "0.0" }
            val novoKi     = editKi.text.toString().ifEmpty { "0.0" }
            val novoKd     = editKd.text.toString().ifEmpty { "0.0" }
            val novoMarcas = editMarcas.text.toString().ifEmpty { "0" }

            txtParamKp.text = novoKp
            txtParamKi.text = novoKi
            txtParamKd.text = novoKd
            txtParamVR.text = novoV

            sharedPreferences.edit().apply {
                putString("${prefix}paramV",      novoV)
                putString("${prefix}paramKp",     novoKp)
                putString("${prefix}paramKi",     novoKi)
                putString("${prefix}paramKd",     novoKd)
                putString("${prefix}paramMarcas", novoMarcas)
                apply()
            }

            enviarComando("PID:$novoV|$novoKp|$novoKi|$novoKd|$novoMarcas")
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
        txtCronometro.text = "0.00"
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
        configurarBotao(btnLerSensores, false)
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

    private fun atualizarIconeBluetooth(conectado: Boolean) {
        if (conectado) {
            icon_Bluetooth.setColorFilter(Color.parseColor("#00FF66"))
        } else {
            icon_Bluetooth.setColorFilter(Color.parseColor("#A066FF"))
        }
    }

    private fun atualizarCoresSwitch(switchCompat: SwitchCompat, isChecked: Boolean) {
        if (isChecked) {
            switchCompat.thumbTintList = ColorStateList.valueOf(Color.parseColor("#00FF66"))
            switchCompat.trackTintList = ColorStateList.valueOf(Color.parseColor("#1F4D2A"))
        } else {
            switchCompat.thumbTintList = ColorStateList.valueOf(Color.parseColor("#A066FF"))
            switchCompat.trackTintList = ColorStateList.valueOf(Color.parseColor("#2E1A47"))
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
                    atualizarIconeBluetooth(true)
                    enviarComando("Q")
                }
            } catch (e: IOException) {
                runOnUiThread {
                    findViewById<SwitchCompat>(R.id.bluetooth).isChecked = false
                    Toast.makeText(this, "Falha na conexão", Toast.LENGTH_SHORT).show()
                    estadoDesconectado()
                    atualizarIconeBluetooth(false)
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
                atualizarIconeBluetooth(false)
                atualizarEstadoEdicaoParametros()
            }
        } catch (e: IOException) {
            e.printStackTrace()
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

                        if (!msgLimpa.startsWith("BAT") && !msgLimpa.startsWith("S,") && !msgLimpa.startsWith("PARAMS:")) {
                            txtSerial.append("$mensagem\n")
                            val scroll = findViewById<ScrollView>(R.id.scrollMonitor)
                            scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
                        }

                        when {
                            msgLimpa.startsWith("PARAMS:") -> {
                                val parts = msgLimpa.removePrefix("PARAMS:").split("|")
                                if (parts.size >= 7) {
                                    val parModo  = parts[0].lowercase()
                                    val parStrat = parts[1].lowercase()
                                    val prefix   = "${parModo}_${parStrat}_"
                                    sharedPreferences.edit().apply {
                                        putString("${prefix}paramV",      parts[2])
                                        putString("${prefix}paramKp",     parts[3])
                                        putString("${prefix}paramKi",     parts[4])
                                        putString("${prefix}paramKd",     parts[5])
                                        putString("${prefix}paramMarcas", parts[6])
                                        apply()
                                    }
                                    if (parModo == modoAtual && parStrat == estrategiaAtual) {
                                        atualizarDisplayParametros()
                                    }
                                }
                            }
                            msgLimpa.startsWith("BAT") -> {
                                val valor = msgLimpa.removePrefix("BAT").trim()
                                val percentual = valor.toIntOrNull()
                                if (percentual != null) {
                                    txtBatteryPercent.text = "$percentual%"
                                    val corAtiva = when {
                                        percentual > 50 -> Color.parseColor("#00E5FF")
                                        percentual > 20 -> Color.parseColor("#FFAA00")
                                        else            -> Color.parseColor("#FF2A55")
                                    }
                                    val corInativa = Color.parseColor("#2E1A47")
                                    txtBatteryPercent.setTextColor(corAtiva)
                                    val barrasAcesas = Math.ceil((percentual / 100.0) * 6).toInt()
                                    val layoutBarras = findViewById<LinearLayout>(R.id.layoutBarrasBateria)
                                    if (layoutBarras != null) {
                                        for (i in 0 until 6) {
                                            layoutBarras.getChildAt(i).setBackgroundColor(
                                                if (i < barrasAcesas) corAtiva else corInativa
                                            )
                                        }
                                    }
                                }
                            }

                            msgLimpa.startsWith("S,") -> {
                                txtSensoresModal?.text = formatarSensoresBonito(mensagem)
                            }

                            msgLimpa.startsWith("Estado: ") -> {
                                val valor = msgLimpa.removePrefix("Estado: ")
                                txtEstadoRobo.text = valor
                                when (valor) {
                                    "Correndo"   -> { txtEstadoRobo.setTextColor(Color.parseColor("#00FF66")); estadoCorrendo(); iniciarCronometro() }
                                    "Parado"     -> { txtEstadoRobo.setTextColor(Color.parseColor("#FF2A55")); estadoFinalizado(); pararCronometro() }
                                    "Calibrando" -> { txtEstadoRobo.setTextColor(Color.parseColor("#FFFF00")); estadoCalibrando() }
                                    "Calibrado"  -> { txtEstadoRobo.setTextColor(Color.parseColor("#9d00ff")); estadoPosCalibracao() }
                                }
                            }

                            msgLimpa.startsWith("Modo: ") -> {
                                val valor = msgLimpa.removePrefix("Modo: ")
                                modoAtual = valor.lowercase()
                                txtModoRobo.text = valor
                                txtModoRobo.setTextColor(if (valor == "Seguidor") Color.parseColor("#00FFFF") else Color.parseColor("#FF007F"))
                                txtEstrategiaRobo.text = "Aguardando..."
                                txtEstrategiaRobo.setTextColor(Color.parseColor("#888888"))
                                estrategiaAtual = ""
                                modoSelecionado = true
                                estadoPosModo()
                                atualizarEstadoEdicaoParametros()
                                sharedPreferences.edit()
                                    .putString("lastModoAtual", modoAtual)
                                    .putString("lastEstrategiaAtual", "")
                                    .apply()
                            }

                            msgLimpa.startsWith("Estrategia: ") -> {
                                val valor = msgLimpa.removePrefix("Estrategia: ")
                                estrategiaAtual = valor.lowercase()
                                txtEstrategiaRobo.text = valor
                                txtEstrategiaRobo.setTextColor(if (valor == "Conservador") Color.parseColor("#3399FF") else Color.parseColor("#FF8800"))
                                atualizarDisplayParametros()
                                estadoPosEstrategia()
                                atualizarEstadoEdicaoParametros()
                                sharedPreferences.edit()
                                    .putString("lastEstrategiaAtual", estrategiaAtual)
                                    .apply()
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

        fun pad(valor: String?): String {
            return (valor ?: "-").padStart(4, ' ')
        }

        val linha1 = "${pad(p.getOrNull(2))} | ${pad(p.getOrNull(3))} | ${pad(p.getOrNull(4))} | ${pad(p.getOrNull(5))}"
        val linha2 = "${pad(p.getOrNull(6))} | ${pad(p.getOrNull(7))} | ${pad(p.getOrNull(8))} | ${pad(p.getOrNull(9))}"
        val dir = pad(p.getOrNull(10))
        val esq = pad(p.getOrNull(11))

        return "POS: $pos\n$linha1\n$linha2\nESQ: $esq | DIR: $dir"
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
        btnFechar.setOnClickListener { dialog.dismiss() }
        dialog.setOnDismissListener {
            enviarComando("l")
            txtSensoresModal = null
        }
        dialog.show()
        enviarComando("L")
    }

    private fun abrirModalExportar() {
        val builder = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.modal_exportar, null)
        builder.setView(dialogView)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val txtResumoTempo = dialogView.findViewById<TextView>(R.id.txtResumoTempo)
        val txtResumoMac = dialogView.findViewById<TextView>(R.id.txtResumoMac)
        val txtResumoModo = dialogView.findViewById<TextView>(R.id.txtResumoModo)
        val txtResumoEstrategia = dialogView.findViewById<TextView>(R.id.txtResumoEstrategia)
        val txtResumoKp = dialogView.findViewById<TextView>(R.id.txtResumoKp)
        val txtResumoKi = dialogView.findViewById<TextView>(R.id.txtResumoKi)
        val txtResumoKd = dialogView.findViewById<TextView>(R.id.txtResumoKd)
        val txtResumoVelMax = dialogView.findViewById<TextView>(R.id.txtResumoVelMax)

        val btnCompleta = dialogView.findViewById<MaterialButton>(R.id.btnStatusCompleta)
        val btnParcial = dialogView.findViewById<MaterialButton>(R.id.btnStatusParcial)
        val editObservacoes = dialogView.findViewById<EditText>(R.id.editObservacoes)
        val btnCopiarResumo = dialogView.findViewById<MaterialButton>(R.id.btnCopiarResumo)
        val btnFechar = dialogView.findViewById<ImageView>(R.id.btnFecharExportar)

        btnCopiarResumo.isEnabled = false
        btnCopiarResumo.alpha = 0.4f

        var statusCorridaSelecionado = ""

        fun configurarSelecaoStatus(botaoAtivo: MaterialButton, botaoInativo: MaterialButton, statusTexto: String) {
            btnCopiarResumo.isEnabled = true
            btnCopiarResumo.alpha = 1.0f
            statusCorridaSelecionado = statusTexto

            // Botão selecionado ativa borda neon e texto lilás
            botaoAtivo.setStrokeColorResource(android.R.color.holo_purple)
            botaoAtivo.setTextColor(Color.parseColor("#EBB3FF"))

            // Botão não selecionado reseta para cinza apagado
            botaoInativo.setStrokeColorResource(android.R.color.transparent)
            botaoInativo.setTextColor(Color.parseColor("#A09DA5"))
        }

        btnCompleta.setOnClickListener {
            configurarSelecaoStatus(btnCompleta, btnParcial, "Completa")
        }

        btnParcial.setOnClickListener {
            configurarSelecaoStatus(btnParcial, btnCompleta, "Parcial")
        }

        val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
        val dataHora = dateFormat.format(java.util.Date())

        val txtTempo = txtCronometro.text.toString()
        val tempoFloat = txtTempo.replace(",", ".").toFloatOrNull() ?: 0f
        val tempoFormatadoParaExportar = String.format(Locale.US, "%.2fs", tempoFloat)

        val modo = txtModoRobo.text.toString()
        val estrategia = txtEstrategiaRobo.text.toString()
        val kp = txtParamKp.text.toString()
        val ki = txtParamKi.text.toString()
        val kd = txtParamKd.text.toString()
        val exportPrefix = prefKey()
        val velMax = sharedPreferences.getString("${exportPrefix}paramV", "0")
        val marcas = sharedPreferences.getString("${exportPrefix}paramMarcas", "0")

        // Alimenta os campos textuais estáticos do topo do seu layout cyberpunk
        txtResumoTempo?.text = tempoFormatadoParaExportar
        txtResumoMac?.text = address
        txtResumoModo?.text = modo
        txtResumoEstrategia?.text = estrategia
        txtResumoKp?.text = kp
        txtResumoKi?.text = ki
        txtResumoKd?.text = kd
        txtResumoVelMax?.text = velMax

        btnCopiarResumo.setOnClickListener {
            val resumoFinal = """
            [FEEDBACK DE CORRIDA - ZÉ-GUIA]
            Data/Hora: $dataHora
            Status da Conclusão: $statusCorridaSelecionado
            Tempo Final: $tempoFormatadoParaExportar
            
            > Configurações:
            Modo: $modo
            Estratégia: $estrategia
            Qtd. Marcas: $marcas
            
            > Parâmetros PID:
            Vel. Máx: $velMax
            Kp: $kp | Ki: $ki | Kd: $kd
        """.trimIndent()

            val obs = editObservacoes.text.toString()
            val textoFinalParaCopiar = if (obs.isNotEmpty()) {
                "$resumoFinal\n\n> Observações:\n$obs"
            } else {
                resumoFinal
            }

            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Feedback do Zé-Guia", textoFinalParaCopiar)
            clipboard.setPrimaryClip(clip)

            Toast.makeText(this, "Feedback copiado com sucesso!", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

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