/**
 * @file ParametrosHelper.kt
 * @brief Gerencia os parâmetros PID (Kp, Ki, Kd, Vel. Máx., Marcas) e o diálogo de edição.
 *
 * Armazena o modo e a estratégia ativos, calcula o prefixo de chave para
 * o SharedPreferences e mantém o display de parâmetros atualizado.
 * O diálogo de edição inclui steppers de incremento/decremento para cada campo.
 *
 * @author Gear Robotics
 * @version 1.0
 */
package com.example.zeguiaapp

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import java.util.Locale

/**
 * @class ParametrosHelper
 * @brief Controla leitura, salvamento e envio dos parâmetros PID ao robô.
 *
 * @param activity        Activity host para inflar layouts e exibir Toasts.
 * @param sharedPrefs     SharedPreferences onde os parâmetros são persistidos.
 * @param txtParamKp      TextView de exibição do Kp na tela principal.
 * @param txtParamKi      TextView de exibição do Ki na tela principal.
 * @param txtParamKd      TextView de exibição do Kd na tela principal.
 * @param txtParamVR      TextView de exibição da velocidade máxima na tela principal.
 * @param onEnviarComando Callback para transmitir um comando serial à ESP32.
 */
class ParametrosHelper(
    private val activity: AppCompatActivity,
    private val sharedPrefs: SharedPreferences,
    private val txtParamKp: TextView,
    private val txtParamKi: TextView,
    private val txtParamKd: TextView,
    private val txtParamVR: TextView,
    private val onEnviarComando: (String) -> Unit
) {

    /** @brief Modo de operação atual ("seguidor" ou "perseguidor"); vazio se não definido. */
    var modoAtual: String = ""

    /** @brief Estratégia de corrida atual ("conservador" ou "arriscado"); vazio se não definida. */
    var estrategiaAtual: String = ""

    /**
     * @brief Gera o prefixo de chave para o SharedPreferences com base no modo e estratégia.
     *
     * @return String no formato "modo_estrategia_" ou string vazia se algum campo for indefinido.
     */
    fun prefKey(): String =
        if (modoAtual.isNotEmpty() && estrategiaAtual.isNotEmpty())
            "${modoAtual}_${estrategiaAtual}_"
        else
            ""

    /**
     * @brief Atualiza os TextViews de parâmetros com os valores persistidos no SharedPreferences.
     *
     * Exibe "--" quando o prefixo está incompleto (modo ou estratégia não definidos).
     */
    fun atualizarDisplay() {
        val prefix = prefKey()
        if (prefix.isEmpty()) {
            txtParamKp.text = "--"
            txtParamKi.text = "--"
            txtParamKd.text = "--"
            txtParamVR.text = "--"
            return
        }
        txtParamKp.text = sharedPrefs.getString("${prefix}paramKp", "--") ?: "--"
        txtParamKi.text = sharedPrefs.getString("${prefix}paramKi", "--") ?: "--"
        txtParamKd.text = sharedPrefs.getString("${prefix}paramKd", "--") ?: "--"
        txtParamVR.text = sharedPrefs.getString("${prefix}paramV",  "--") ?: "--"
    }

    /**
     * @brief Carrega os parâmetros do modo/estratégia atual e os envia ao robô via comando PID.
     *
     * Formato do comando: `PID:V|Kp|Ki|Kd|Marcas`
     */
    fun carregar() {
        val prefix = prefKey()
        val kp     = sharedPrefs.getString("${prefix}paramKp",     "0.0") ?: "0.0"
        val ki     = sharedPrefs.getString("${prefix}paramKi",     "0.0") ?: "0.0"
        val kd     = sharedPrefs.getString("${prefix}paramKd",     "0.0") ?: "0.0"
        val v      = sharedPrefs.getString("${prefix}paramV",      "0")   ?: "0"
        val marcas = sharedPrefs.getString("${prefix}paramMarcas", "0")   ?: "0"

        txtParamKp.text = kp
        txtParamKi.text = ki
        txtParamKd.text = kd
        txtParamVR.text = v

        onEnviarComando("PID:$v|$kp|$ki|$kd|$marcas")
    }

    /**
     * @brief Retorna o valor de Kp atualmente exibido na tela principal.
     * @return String com o valor de Kp ou "--".
     */
    fun getKp(): String = txtParamKp.text.toString()

    /**
     * @brief Retorna o valor de Ki atualmente exibido na tela principal.
     * @return String com o valor de Ki ou "--".
     */
    fun getKi(): String = txtParamKi.text.toString()

    /**
     * @brief Retorna o valor de Kd atualmente exibido na tela principal.
     * @return String com o valor de Kd ou "--".
     */
    fun getKd(): String = txtParamKd.text.toString()

    /**
     * @brief Exibe o diálogo de edição dos parâmetros PID.
     *
     * Requer que modo e estratégia estejam definidos; caso contrário exibe um Toast de aviso.
     * O diálogo contém steppers de ±0.01 para Kp/Ki/Kd e ±1 para V e Marcas.
     */
    @SuppressLint("SetTextI18n")
    fun mostrarDialogEdicao() {
        if (prefKey().isEmpty()) {
            Toast.makeText(activity, "Selecione modo e estratégia antes de editar parâmetros", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = android.view.LayoutInflater.from(activity).inflate(R.layout.editar_parametros, null)
        val alertDialog = AlertDialog.Builder(activity)
            .setView(dialogView)
            .create()
            .also {
                it.window?.setBackgroundDrawableResource(android.R.color.transparent)
                it.show()
            }

        val editV      = dialogView.findViewById<EditText>(R.id.editV)
        val editKp     = dialogView.findViewById<EditText>(R.id.editKp)
        val editKi     = dialogView.findViewById<EditText>(R.id.editKi)
        val editKd     = dialogView.findViewById<EditText>(R.id.editKd)
        val editMarcas = dialogView.findViewById<EditText>(R.id.editMarcas)
        val prefix     = prefKey()

        editV.setText(sharedPrefs.getString("${prefix}paramV", "0")?.toFloatOrNull()?.toInt()?.toString() ?: "0")

        fun normFloat(key: String): String {
            val raw = sharedPrefs.getString("${prefix}$key", "0.0") ?: "0.0"
            return String.format(Locale.US, "%.2f", raw.replace(",", ".").toFloatOrNull() ?: 0f)
        }
        editKp.setText(normFloat("paramKp"))
        editKi.setText(normFloat("paramKi"))
        editKd.setText(normFloat("paramKd"))
        editMarcas.setText(sharedPrefs.getString("${prefix}paramMarcas", "0")?.toFloatOrNull()?.toInt()?.toString() ?: "0")

        /**
         * @brief Configura botões stepper de ponto flutuante com passo de 0.01.
         *
         * @param menosId ID do botão de decremento.
         * @param maisId  ID do botão de incremento.
         * @param edit    EditText alvo.
         */
        fun stepperFloat(menosId: Int, maisId: Int, edit: EditText) {
            dialogView.findViewById<TextView>(maisId).setOnClickListener {
                edit.setText(String.format(Locale.US, "%.2f", (edit.text.toString().toFloatOrNull() ?: 0f) + 0.01f))
            }
            dialogView.findViewById<TextView>(menosId).setOnClickListener {
                edit.setText(String.format(Locale.US, "%.2f", (edit.text.toString().toFloatOrNull() ?: 0f) - 0.01f))
            }
        }

        /**
         * @brief Configura botões stepper inteiro com passo de 1.
         *
         * @param menosId ID do botão de decremento.
         * @param maisId  ID do botão de incremento.
         * @param edit    EditText alvo.
         */
        fun stepperInt(menosId: Int, maisId: Int, edit: EditText) {
            dialogView.findViewById<TextView>(maisId).setOnClickListener {
                edit.setText(String.format(Locale.US, "%d", (edit.text.toString().toFloatOrNull()?.toInt() ?: 0) + 1))
            }
            dialogView.findViewById<TextView>(menosId).setOnClickListener {
                edit.setText(String.format(Locale.US, "%d", (edit.text.toString().toFloatOrNull()?.toInt() ?: 0) - 1))
            }
        }

        stepperInt(R.id.btnDiminuirV,      R.id.btnAumentarV,      editV)
        stepperFloat(R.id.btnDiminuirKp,   R.id.btnAumentarKp,     editKp)
        stepperFloat(R.id.btnDiminuirKi,   R.id.btnAumentarKi,     editKi)
        stepperFloat(R.id.btnDiminuirKd,   R.id.btnAumentarKd,     editKd)
        stepperInt(R.id.btnDiminuirMarcas, R.id.btnAumentarMarcas, editMarcas)

        dialogView.findViewById<ImageButton>(R.id.btnFechar).setOnClickListener { alertDialog.dismiss() }
        dialogView.findViewById<MaterialButton>(R.id.btnCancelar).setOnClickListener { alertDialog.dismiss() }

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

            sharedPrefs.edit().apply {
                putString("${prefix}paramV",      novoV)
                putString("${prefix}paramKp",     novoKp)
                putString("${prefix}paramKi",     novoKi)
                putString("${prefix}paramKd",     novoKd)
                putString("${prefix}paramMarcas", novoMarcas)
                apply()
            }

            onEnviarComando("PID:$novoV|$novoKp|$novoKi|$novoKd|$novoMarcas")
            alertDialog.dismiss()
        }
    }
}
