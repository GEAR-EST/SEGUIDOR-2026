/**
 * @file MainActivity.kt
 * @brief Activity principal do ZeGuia — orquestra os helpers e implementa os callbacks Bluetooth.
 *
 * Responsabilidades:
 * - Inicializar e conectar todos os helpers do aplicativo
 * - Gerenciar o estado local de conexão (MAC ativo, MACs salvos, modo selecionado)
 * - Implementar [BluetoothEventListener] para reagir aos eventos da ESP32
 * - Exibir o display de bateria com barras coloridas
 * - Controlar o estado visual do botão de editar parâmetros
 *
 * @author Gear Robotics
 * @version 1.0
 */
package com.example.zeguiaapp

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
import androidx.core.content.edit
import androidx.core.graphics.toColorInt
import java.util.UUID

/**
 * @class MainActivity
 * @brief Entry-point da aplicação; coordena todos os módulos via injeção manual de dependências.
 */
class MainActivity : AppCompatActivity(), BluetoothEventListener {

    // ── Views da tela principal ──
    private lateinit var txtSerial: TextView
    private lateinit var txtEstadoRobo: TextView
    private lateinit var txtModoRobo: TextView
    private lateinit var txtEstrategiaRobo: TextView
    private lateinit var txtCronometro: TextView
    private lateinit var txtBatteryPercent: TextView
    private lateinit var txtEspMac: TextView
    private lateinit var btnAbrirEdicao: LinearLayout
    private lateinit var iconEditarParams: ImageView
    private lateinit var swBluetooth: SwitchCompat

    // ── Helpers ──
    private lateinit var cronometroHelper: CronometroHelper
    private lateinit var uiHelper: UiStateHelper
    private lateinit var iconHelper: BluetoothIconHelper
    private lateinit var parametrosHelper: ParametrosHelper
    private lateinit var btHelper: BluetoothHelper
    private lateinit var sensoresHelper: SensoresHelper
    private lateinit var espConfigHelper: EspConfigHelper
    private lateinit var exportarHelper: ExportarHelper

    // ── Estado de sessão ──
    private lateinit var sharedPreferences: SharedPreferences

    /** @brief Endereço MAC da ESP32 atualmente selecionada. */
    private var address: String = ""

    /** @brief Conjunto de MACs persistidos pelo usuário. */
    private var savedMacs: MutableSet<String> = mutableSetOf()

    /** @brief `true` após o primeiro "Modo:" recebido; habilita estratégias pós-calibração. */
    private var modoSelecionado = false

    private val MY_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    // ──────────────────────────────────────────────────────────────────────────────
    //  Ciclo de vida
    // ──────────────────────────────────────────────────────────────────────────────

    /**
     * @brief Inicializa views, helpers e listeners. Ponto de entrada da Activity.
     *
     * @param savedInstanceState Estado salvo da instância anterior (não utilizado).
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPreferences = getSharedPreferences("ZeGuiaPrefs", Context.MODE_PRIVATE)

        savedMacs = sharedPreferences.getStringSet("savedMacs", mutableSetOf("C0:49:EF:65:16:FE"))
            ?.toMutableSet() ?: mutableSetOf("C0:49:EF:65:16:FE")
        address = sharedPreferences.getString("selectedMac", "C0:49:EF:65:16:FE") ?: "C0:49:EF:65:16:FE"

        inicializarViews()
        inicializarHelpers()
        configurarListeners()

        // Restaura modo/estratégia da sessão anterior
        parametrosHelper.modoAtual      = sharedPreferences.getString("lastModoAtual",      "") ?: ""
        parametrosHelper.estrategiaAtual = sharedPreferences.getString("lastEstrategiaAtual", "") ?: ""
        parametrosHelper.atualizarDisplay()
        atualizarEstadoEdicaoParametros()

        uiHelper.estadoDesconectado()
        iconHelper.setDesconectado()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN),
                1
            )
        }
    }

    /**
     * @brief Libera handlers, fecha o socket e descarta o dialog de "Sobre Nós".
     */
    override fun onDestroy() {
        super.onDestroy()
        SobreNosHelper.onDestroy()
        cronometroHelper.resetar()
        btHelper.fechar()
    }

    // ──────────────────────────────────────────────────────────────────────────────
    //  Inicialização
    // ──────────────────────────────────────────────────────────────────────────────

    /**
     * @brief Localiza e atribui todos os views do layout às propriedades da classe.
     */
    private fun inicializarViews() {
        txtSerial           = findViewById(R.id.txtSerial)
        txtEstadoRobo       = findViewById(R.id.txtEstadoRobo)
        txtModoRobo         = findViewById(R.id.txtModoRobo)
        txtEstrategiaRobo   = findViewById(R.id.txtEstrategiaRobo)
        txtCronometro       = findViewById(R.id.txtCronometro)
        txtBatteryPercent   = findViewById(R.id.txtBatteryPercent)
        txtEspMac           = findViewById(R.id.txtEspMac)
        btnAbrirEdicao      = findViewById(R.id.btnAbrirEdicao)
        iconEditarParams    = findViewById(R.id.iconEditarParams)
        swBluetooth         = findViewById(R.id.bluetooth)

        txtEspMac.text       = address
        @Suppress("SetTextI18n")
        txtCronometro.text   = "0.00"
    }

    /**
     * @brief Instancia todos os helpers injetando as dependências necessárias.
     */
    private fun inicializarHelpers() {
        cronometroHelper = CronometroHelper(txtCronometro)

        val btnCalibrar             = findViewById<Button>(R.id.calibrate)
        val btnStartRun             = findViewById<Button>(R.id.run)
        val btnStopRun              = findViewById<Button>(R.id.stop)
        val btnModeFollower         = findViewById<Button>(R.id.follower)
        val btnModeChase            = findViewById<Button>(R.id.chase)
        val btnStrategyConservative = findViewById<Button>(R.id.conservative)
        val btnStrategyRisk         = findViewById<Button>(R.id.risk)
        val btnLerSensores          = findViewById<Button>(R.id.btnLerSensores)
        val btnExportar             = findViewById<Button>(R.id.btnExportar)

        uiHelper = UiStateHelper(
            btnCalibrar, btnStartRun, btnStopRun,
            btnModeFollower, btnModeChase,
            btnStrategyConservative, btnStrategyRisk,
            btnLerSensores, btnExportar
        )

        iconHelper = BluetoothIconHelper(
            iconBluetooth = findViewById(R.id.iconBluetooth),
            cardBluetooth = findViewById(R.id.cardBluetoothToggle),
            density       = resources.displayMetrics.density
        )

        val txtParamKp = findViewById<TextView>(R.id.txtParamKp)
        val txtParamKi = findViewById<TextView>(R.id.txtParamKi)
        val txtParamKd = findViewById<TextView>(R.id.txtParamKd)
        val txtParamVR = findViewById<TextView>(R.id.txtParamVR)

        parametrosHelper = ParametrosHelper(
            activity        = this,
            sharedPrefs     = sharedPreferences,
            txtParamKp      = txtParamKp,
            txtParamKi      = txtParamKi,
            txtParamKd      = txtParamKd,
            txtParamVR      = txtParamVR,
            onEnviarComando = { btHelper.enviarComando(it) }
        )

        sensoresHelper = SensoresHelper(
            activity        = this,
            onEnviarComando = { btHelper.enviarComando(it) }
        )

        val btAdapter = (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter

        btHelper = BluetoothHelper(
            activity       = this,
            btAdapter      = btAdapter,
            uuid           = MY_UUID,
            swBluetooth    = swBluetooth,
            sensoresHelper = sensoresHelper,
            listener       = this
        ).also { it.setAddress(address) }

        espConfigHelper = EspConfigHelper(
            activity      = this,
            sharedPrefs   = sharedPreferences,
            getAddress    = { address },
            setAddress    = { mac -> address = mac; btHelper.setAddress(mac) },
            getSavedMacs  = { savedMacs },
            isSocketOpen  = { btHelper.btSocket != null },
            txtEspMac     = txtEspMac,
            onDesconectar = { swBluetooth.isChecked = false }
        )

        exportarHelper = ExportarHelper(
            activity         = this,
            cronometroHelper = cronometroHelper,
            parametrosHelper = parametrosHelper,
            sharedPrefs      = sharedPreferences,
            txtModoRobo      = txtModoRobo,
            txtEstrategiaRobo = txtEstrategiaRobo,
            getAddress       = { address }
        )

        SobreNosHelper.setup(this)

        // Registra os listeners de clique em cada botão de controle
        btnCalibrar.setOnClickListener             { btHelper.enviarComando("K") }
        btnStartRun.setOnClickListener             { btHelper.enviarComando("R") }
        btnStopRun.setOnClickListener              { btHelper.enviarComando("F") }
        btnLerSensores.setOnClickListener          { sensoresHelper.abrir() }
        btnExportar.setOnClickListener             { exportarHelper.abrir() }

        btnModeFollower.setOnClickListener {
            parametrosHelper.modoAtual       = "seguidor"
            parametrosHelper.estrategiaAtual = ""
            atualizarEstadoEdicaoParametros()
            btHelper.enviarComando("S")
        }
        btnModeChase.setOnClickListener {
            parametrosHelper.modoAtual       = "perseguidor"
            parametrosHelper.estrategiaAtual = ""
            atualizarEstadoEdicaoParametros()
            btHelper.enviarComando("P")
        }
        btnStrategyConservative.setOnClickListener {
            parametrosHelper.estrategiaAtual = "conservador"
            btHelper.enviarComando("C")
            parametrosHelper.carregar()
            atualizarEstadoEdicaoParametros()
            onRawMessage("→ Estratégia: Conservador")
        }
        btnStrategyRisk.setOnClickListener {
            parametrosHelper.estrategiaAtual = "arriscado"
            btHelper.enviarComando("A")
            parametrosHelper.carregar()
            atualizarEstadoEdicaoParametros()
            onRawMessage("→ Estratégia: Arriscado")
        }

        btnAbrirEdicao.setOnClickListener { parametrosHelper.mostrarDialogEdicao() }
    }

    /**
     * @brief Registra os listeners de clique dos cards e do switch Bluetooth.
     */
    private fun configurarListeners() {
        swBluetooth.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                iconHelper.setConectando()
                btHelper.conectar()
            } else {
                btHelper.desconectar()
            }
        }

        findViewById<View>(R.id.cardBluetoothToggle).setOnClickListener {
            animarClique(it) { swBluetooth.isChecked = !swBluetooth.isChecked }
        }

        findViewById<View>(R.id.cardConfigEsp).setOnClickListener {
            animarClique(it) { espConfigHelper.abrir(txtEstadoRobo.text.toString()) }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────────
    //  Helpers de UI locais
    // ──────────────────────────────────────────────────────────────────────────────

    /**
     * @brief Executa uma animação de "aperto" (scale 78% → 100%) em um view e chama a ação no final.
     *
     * @param view  View que recebe a animação.
     * @param acao  Lambda executado ao fim da animação de retorno.
     */
    private fun animarClique(view: View, acao: () -> Unit) {
        view.animate()
            .scaleX(0.78f).scaleY(0.78f)
            .setDuration(90)
            .withEndAction {
                view.animate().scaleX(1f).scaleY(1f).setDuration(110)
                    .withEndAction(acao).start()
            }
            .start()
    }

    /**
     * @brief Habilita ou desabilita o botão de editar parâmetros conforme a estratégia selecionada.
     *
     * O botão (LinearLayout) fica com alpha reduzido e ícone acinzentado quando nenhuma
     * estratégia está ativa; ícone roxo e alpha cheio quando uma estratégia está selecionada.
     */
    private fun atualizarEstadoEdicaoParametros() {
        val temEstrategia          = parametrosHelper.estrategiaAtual.isNotEmpty()
        btnAbrirEdicao.isEnabled   = temEstrategia
        btnAbrirEdicao.isClickable = temEstrategia
        btnAbrirEdicao.alpha       = if (temEstrategia) 1.0f else 0.55f
        iconEditarParams.setColorFilter(
            if (temEstrategia) "#A066FF".toColorInt() else "#5A4470".toColorInt()
        )
    }

    /**
     * @brief Atualiza o percentual e as barras visuais de bateria na tela principal.
     *
     * @param percentual Nível de bateria de 0 a 100.
     */
    @SuppressLint("SetTextI18n")
    private fun atualizarBateria(percentual: Int) {
        val corAtiva = when {
            percentual > 50 -> "#00E5FF".toColorInt()
            percentual > 20 -> "#FFAA00".toColorInt()
            else            -> "#FF2A55".toColorInt()
        }
        val corInativa   = "#2E1A47".toColorInt()
        val barrasAcesas = (percentual / 10.0 + 0.5).toInt().coerceIn(0, 10)

        txtBatteryPercent.text = "$percentual%"
        txtBatteryPercent.setTextColor(corAtiva)

        val layoutBarras = findViewById<LinearLayout>(R.id.layoutBarrasBateria)
        for (i in 0 until 10) {
            layoutBarras?.getChildAt(i)?.setBackgroundColor(
                if (i < barrasAcesas) corAtiva else corInativa
            )
        }
    }

    // ──────────────────────────────────────────────────────────────────────────────
    //  BluetoothEventListener
    // ──────────────────────────────────────────────────────────────────────────────

    /**
     * @brief Conexão estabelecida: atualiza ícone, libera botões e solicita estado atual.
     */
    override fun onConnected() {
        Toast.makeText(this, "Conectado ao ZeGuia!", Toast.LENGTH_SHORT).show()
        iconHelper.setConectado()
        uiHelper.estadoConectadoInicial()
        btHelper.enviarComando("Q")
    }

    /**
     * @brief Conexão encerrada: restaura UI para estado desconectado e zera o cronômetro.
     */
    override fun onDisconnected() {
        Toast.makeText(this, "Desconectado", Toast.LENGTH_SHORT).show()
        iconHelper.setDesconectado()
        uiHelper.estadoDesconectado()
        cronometroHelper.resetar()
        parametrosHelper.modoAtual       = ""
        parametrosHelper.estrategiaAtual = ""
        atualizarEstadoEdicaoParametros()
    }

    /**
     * @brief Falha de conexão: restaura o switch e a UI para estado desconectado.
     */
    override fun onConnectionFailed() {
        Toast.makeText(this, "Falha na conexão", Toast.LENGTH_SHORT).show()
        iconHelper.setDesconectado()
        uiHelper.estadoDesconectado()
    }

    /**
     * @brief Mensagem serial genérica: appenda ao monitor serial e faz scroll automático.
     * @param msg Linha de texto recebida.
     */
    override fun onRawMessage(msg: String) {
        txtSerial.append("$msg\n")
        val scroll = findViewById<ScrollView>(R.id.scrollMonitor)
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
    }

    /**
     * @brief Atualiza o estado do robô na tela e aciona as transições de UI correspondentes.
     *
     * @param valor Estado recebido: "Correndo", "Parado", "Calibrando" ou "Calibrado".
     */
    override fun onEstado(valor: String) {
        txtEstadoRobo.text = valor
        when (valor) {
            "Correndo"   -> {
                txtEstadoRobo.setTextColor("#00FF66".toColorInt())
                uiHelper.estadoCorrendo()
                cronometroHelper.iniciar()
            }
            "Parado"     -> {
                txtEstadoRobo.setTextColor("#FF2A55".toColorInt())
                uiHelper.estadoFinalizado()
                cronometroHelper.parar()
            }
            "Calibrando" -> {
                txtEstadoRobo.setTextColor("#FFFF00".toColorInt())
                uiHelper.estadoCalibrando()
            }
            "Calibrado"  -> {
                txtEstadoRobo.setTextColor("#9d00ff".toColorInt())
                uiHelper.estadoPosCalibracao(modoSelecionado)
            }
        }
    }

    /**
     * @brief Confirma o modo de operação selecionado e libera as estratégias.
     *
     * @param valor Modo recebido: "Seguidor" ou "Perseguidor".
     */
    override fun onModo(valor: String) {
        parametrosHelper.modoAtual       = valor.lowercase()
        parametrosHelper.estrategiaAtual = ""
        txtModoRobo.text = valor
        txtModoRobo.setTextColor(
            if (valor == "Seguidor") "#00FFFF".toColorInt() else "#FF007F".toColorInt()
        )
        @Suppress("SetTextI18n")
        txtEstrategiaRobo.text = "Aguardando..."
        txtEstrategiaRobo.setTextColor("#888888".toColorInt())
        modoSelecionado = true
        uiHelper.estadoPosModo()
        atualizarEstadoEdicaoParametros()
        sharedPreferences.edit {
            putString("lastModoAtual",       parametrosHelper.modoAtual)
            putString("lastEstrategiaAtual", "")
        }
    }

    /**
     * @brief Confirma a estratégia de corrida selecionada e libera o botão de iniciar.
     *
     * @param valor Estratégia recebida: "Conservador" ou "Arriscado".
     */
    override fun onEstrategia(valor: String) {
        parametrosHelper.estrategiaAtual = valor.lowercase()
        txtEstrategiaRobo.text = valor
        txtEstrategiaRobo.setTextColor(
            if (valor == "Conservador") "#3399FF".toColorInt() else "#FF8800".toColorInt()
        )
        parametrosHelper.atualizarDisplay()
        uiHelper.estadoPosEstrategia()
        atualizarEstadoEdicaoParametros()
        sharedPreferences.edit {
            putString("lastEstrategiaAtual", parametrosHelper.estrategiaAtual)
        }
    }

    /**
     * @brief Atualiza o display de bateria com o percentual recebido.
     * @param percentual Nível de bateria de 0 a 100.
     */
    override fun onBateria(percentual: Int) {
        atualizarBateria(percentual)
    }

    /**
     * @brief Repassa os dados de sensores ao SensoresHelper para atualizar o modal.
     * @param parts Tokens da mensagem "S,...".
     */
    override fun onSensores(parts: List<String>) {
        sensoresHelper.atualizarSensores(parts)
    }

    /**
     * @brief Persiste os parâmetros PID recebidos da ESP32 e atualiza o display se pertinentes.
     *
     * @param parts Tokens após "PARAMS:": [0]=modo, [1]=estrategia, [2]=V, [3]=Kp, [4]=Ki, [5]=Kd, [6]=marcas.
     */
    override fun onParams(parts: List<String>) {
        val parModo  = parts[0].lowercase()
        val parStrat = parts[1].lowercase()
        val prefix   = "${parModo}_${parStrat}_"
        sharedPreferences.edit {
            putString("${prefix}paramV",      parts[2])
            putString("${prefix}paramKp",     parts[3])
            putString("${prefix}paramKi",     parts[4])
            putString("${prefix}paramKd",     parts[5])
            putString("${prefix}paramMarcas", parts[6])
        }
        if (parModo == parametrosHelper.modoAtual && parStrat == parametrosHelper.estrategiaAtual) {
            parametrosHelper.atualizarDisplay()
        }
    }
}
