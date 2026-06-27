/**
 * @file ExportarHelper.kt
 * @brief Gerencia o modal de exportação de feedback de corrida.
 *
 * Exibe um resumo da última corrida (tempo, modo, estratégia, PID) e permite
 * ao usuário selecionar o status de conclusão, adicionar observações e copiar
 * o relatório completo para a área de transferência.
 *
 * @author Gear Robotics
 * @version 1.0
 */
package com.example.zeguiaapp

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * @class ExportarHelper
 * @brief Infla e controla o modal de exportação de resultado de corrida.
 *
 * @param activity         Activity host para inflação de layout, Toasts e clipboard.
 * @param cronometroHelper Referência ao CronometroHelper para obter o tempo final.
 * @param parametrosHelper Referência ao ParametrosHelper para obter Kp, Ki, Kd e prefKey.
 * @param sharedPrefs      SharedPreferences para leitura de velocidade máxima e marcas.
 * @param txtModoRobo      TextView que exibe o modo de operação ativo.
 * @param txtEstrategiaRobo TextView que exibe a estratégia ativa.
 * @param getAddress       Lambda que retorna o MAC da ESP32 conectada.
 */
class ExportarHelper(
    private val activity: AppCompatActivity,
    private val cronometroHelper: CronometroHelper,
    private val parametrosHelper: ParametrosHelper,
    private val sharedPrefs: SharedPreferences,
    private val txtModoRobo: TextView,
    private val txtEstrategiaRobo: TextView,
    private val getAddress: () -> String
) {

    /**
     * @brief Infla e exibe o modal de exportação com os dados da corrida atual.
     *
     * O botão "Copiar Resumo" fica desabilitado até que o usuário selecione
     * o status de conclusão ("Completa" ou "Parcial").
     */
    @SuppressLint("SetTextI18n")
    fun abrir() {
        val dialogView = activity.layoutInflater.inflate(R.layout.modal_exportar, null)
        val dialog = AlertDialog.Builder(activity)
            .setView(dialogView)
            .create()
            .also { it.window?.setBackgroundDrawableResource(android.R.color.transparent) }

        val txtResumoData       = dialogView.findViewById<TextView>(R.id.txtResumoData)
        val txtResumoRobo       = dialogView.findViewById<TextView>(R.id.txtResumoRobo)
        val txtResumoTempo      = dialogView.findViewById<TextView>(R.id.txtResumoTempo)
        val txtResumoMac        = dialogView.findViewById<TextView>(R.id.txtResumoMac)
        val txtResumoModo       = dialogView.findViewById<TextView>(R.id.txtResumoModo)
        val txtResumoEstrategia = dialogView.findViewById<TextView>(R.id.txtResumoEstrategia)
        val txtResumoKp         = dialogView.findViewById<TextView>(R.id.txtResumoKp)
        val txtResumoKi         = dialogView.findViewById<TextView>(R.id.txtResumoKi)
        val txtResumoKd         = dialogView.findViewById<TextView>(R.id.txtResumoKd)
        val txtResumoVelMax     = dialogView.findViewById<TextView>(R.id.txtResumoVelMax)

        val btnCompleta      = dialogView.findViewById<MaterialButton>(R.id.btnStatusCompleta)
        val btnParcial       = dialogView.findViewById<MaterialButton>(R.id.btnStatusParcial)
        val editObservacoes  = dialogView.findViewById<EditText>(R.id.editObservacoes)
        val btnCopiarResumo  = dialogView.findViewById<MaterialButton>(R.id.btnCopiarResumo)
        val btnFechar        = dialogView.findViewById<ImageButton>(R.id.btnFecharExportar)

        btnCopiarResumo.isEnabled = false
        btnCopiarResumo.alpha     = 0.4f

        var statusCorridaSelecionado = ""

        /**
         * @brief Aplica o visual de seleção em um dos dois botões de status de corrida.
         *
         * @param botaoAtivo   Botão que recebe a cor de destaque.
         * @param botaoInativo Botão que retorna ao visual neutro.
         * @param statusTexto  Texto do status ("Completa" ou "Parcial").
         * @param corHex       Cor de destaque em hexadecimal.
         */
        fun configurarSelecaoStatus(
            botaoAtivo: MaterialButton,
            botaoInativo: MaterialButton,
            statusTexto: String,
            corHex: String
        ) {
            btnCopiarResumo.isEnabled = true
            btnCopiarResumo.alpha     = 1.0f
            statusCorridaSelecionado  = statusTexto

            val corAtiva = corHex.toColorInt()
            botaoAtivo.strokeColor   = ColorStateList.valueOf(corAtiva)
            botaoAtivo.setTextColor(corAtiva)
            botaoInativo.strokeColor = ColorStateList.valueOf("#2C164D".toColorInt())
            botaoInativo.setTextColor("#A09DA5".toColorInt())
        }

        btnCompleta.setOnClickListener { configurarSelecaoStatus(btnCompleta, btnParcial, "Completa", "#00FF66") }
        btnParcial.setOnClickListener  { configurarSelecaoStatus(btnParcial, btnCompleta, "Parcial",  "#FF8800") }

        // ── Coleta de dados da corrida ──
        val dataHora         = SimpleDateFormat("dd/MM/yyyy\nHH:mm", Locale.getDefault()).format(Date())
        val dataHoraExport   = dataHora.replace("\n", " ")
        val tempoTexto       = cronometroHelper.getTexto()
        val tempoFloat       = tempoTexto.replace(",", ".").toFloatOrNull() ?: 0f
        val tempoFormatado   = String.format(Locale.US, "%.2fs", tempoFloat)
        val modo             = txtModoRobo.text.toString()
        val estrategia       = txtEstrategiaRobo.text.toString()
        val kp               = parametrosHelper.getKp()
        val ki               = parametrosHelper.getKi()
        val kd               = parametrosHelper.getKd()
        val exportPrefix     = parametrosHelper.prefKey()
        val velMax           = sharedPrefs.getString("${exportPrefix}paramV",      "0")
        val marcas           = sharedPrefs.getString("${exportPrefix}paramMarcas", "0")

        txtResumoData?.text       = dataHora
        txtResumoRobo?.text       = "Zé-Guia"
        txtResumoTempo?.text      = tempoFormatado
        txtResumoMac?.text        = getAddress()
        txtResumoModo?.text       = modo
        txtResumoEstrategia?.text = estrategia
        txtResumoKp?.text         = kp
        txtResumoKi?.text         = ki
        txtResumoKd?.text         = kd
        txtResumoVelMax?.text     = velMax

        btnCopiarResumo.setOnClickListener {
            val resumo = """
                [FEEDBACK DE CORRIDA - ZÉ-GUIA]
                Data/Hora: $dataHoraExport
                Status da Conclusão: $statusCorridaSelecionado
                Tempo Final: $tempoFormatado

                > Configurações:
                ESP32 MAC: ${getAddress()}
                Modo: $modo
                Estratégia: $estrategia
                Qtd. Marcas: $marcas

                > Parâmetros PID:
                Vel. Máx: $velMax
                Kp: $kp | Ki: $ki | Kd: $kd
            """.trimIndent()

            val obs = editObservacoes.text.toString()
            val textoFinal = if (obs.isNotEmpty()) "$resumo\n\n> Observações:\n$obs" else resumo

            val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Feedback do Zé-Guia", textoFinal))

            Toast.makeText(activity, "Feedback copiado com sucesso!", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        btnFechar.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }
}
