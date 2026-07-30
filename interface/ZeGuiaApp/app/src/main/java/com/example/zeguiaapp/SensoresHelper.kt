/**
 * @file SensoresHelper.kt
 * @brief Gerencia o modal de leitura em tempo real dos sensores de linha.
 *
 * O modal exibe os valores dos 8 sensores ópticos, a posição calculada
 * (ponderada) e as velocidades dos motores esquerdo e direito.
 * O streaming é ativado com o comando "L" e desativado com "l".
 *
 * @author Gear Robotics
 * @version 1.0
 */
package com.example.zeguiaapp

import android.annotation.SuppressLint
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

/**
 * @class SensoresHelper
 * @brief Abre o modal de sensores, recebe atualizações e gerencia as referências de views.
 *
 * As referências de view são nulas enquanto o modal está fechado e
 * inicializadas enquanto está aberto, evitando memory leaks.
 *
 * @param activity        Activity host usada para inflar o layout e criar o diálogo.
 * @param onEnviarComando Callback para transmitir comandos seriais à ESP32.
 */
class SensoresHelper(
    private val activity: AppCompatActivity,
    private val onEnviarComando: (String) -> Unit
) {

    /**
     * @brief `true` enquanto o modal de sensores estiver visível na tela.
     *
     * Utilizado pelo BluetoothHelper para enviar o comando de parada de streaming
     * diretamente no socket antes de fechar a conexão.
     */
    var modalAberto: Boolean = false

    // ── Referências aos views internos do modal (null quando fechado) ──
    private var txtPosicao: TextView? = null
    private var progressPosicao: ProgressBar? = null
    private var txtS1: TextView? = null;  private var progressS1: ProgressBar? = null
    private var txtS2: TextView? = null;  private var progressS2: ProgressBar? = null
    private var txtS3: TextView? = null;  private var progressS3: ProgressBar? = null
    private var txtS4: TextView? = null;  private var progressS4: ProgressBar? = null
    private var txtS5: TextView? = null;  private var progressS5: ProgressBar? = null
    private var txtS6: TextView? = null;  private var progressS6: ProgressBar? = null
    private var txtS7: TextView? = null;  private var progressS7: ProgressBar? = null
    private var txtS8: TextView? = null;  private var progressS8: ProgressBar? = null
    private var txtDirEsq: TextView? = null
    private var txtDirDir: TextView? = null

    /**
     * @brief Infla e exibe o modal de sensores, iniciando o streaming via comando "L".
     */
    fun abrir() {
        val dialogView = activity.layoutInflater.inflate(R.layout.modal_sensores, null)
        val dialog = AlertDialog.Builder(activity)
            .setView(dialogView)
            .create()
            .also { it.window?.setBackgroundDrawableResource(android.R.color.transparent) }

        txtPosicao      = dialogView.findViewById(R.id.txtPosicaoValor)
        progressPosicao = dialogView.findViewById(R.id.progressPosicao)
        txtS1  = dialogView.findViewById(R.id.txtS1);  progressS1 = dialogView.findViewById(R.id.progressS1)
        txtS2  = dialogView.findViewById(R.id.txtS2);  progressS2 = dialogView.findViewById(R.id.progressS2)
        txtS3  = dialogView.findViewById(R.id.txtS3);  progressS3 = dialogView.findViewById(R.id.progressS3)
        txtS4  = dialogView.findViewById(R.id.txtS4);  progressS4 = dialogView.findViewById(R.id.progressS4)
        txtS5  = dialogView.findViewById(R.id.txtS5);  progressS5 = dialogView.findViewById(R.id.progressS5)
        txtS6  = dialogView.findViewById(R.id.txtS6);  progressS6 = dialogView.findViewById(R.id.progressS6)
        txtS7  = dialogView.findViewById(R.id.txtS7);  progressS7 = dialogView.findViewById(R.id.progressS7)
        txtS8  = dialogView.findViewById(R.id.txtS8);  progressS8 = dialogView.findViewById(R.id.progressS8)
        txtDirEsq = dialogView.findViewById(R.id.txtDirecaoEsquerdaValor)
        txtDirDir = dialogView.findViewById(R.id.txtDirecaoDireitaValor)

        dialogView.findViewById<ImageButton>(R.id.btnFecharSensores).setOnClickListener { dialog.dismiss() }

        dialog.setOnDismissListener { limparReferencias() }

        modalAberto = true
        onEnviarComando("L")
        dialog.show()
    }

    /**
     * @brief Atualiza os views do modal com a leitura mais recente dos sensores.
     *
     * Deve ser chamado somente quando [modalAberto] for `true`.
     * O formato esperado da lista é: [0]="S", [1]=posição, [2..9]=s1..s8, [10]=dirDir, [11]=dirEsq.
     *
     * @param parts Tokens extraídos da mensagem serial "S,..." (já divididos por vírgula).
     */
    @SuppressLint("SetTextI18n")
    fun atualizarSensores(parts: List<String>) {
        if (!modalAberto) return

        val posVal = parts.getOrNull(1)?.toIntOrNull() ?: 0
        txtPosicao?.text          = String.format(Locale.US, "%d", posVal)
        progressPosicao?.progress = posVal

        val sVals = (2..9).map { parts.getOrNull(it)?.toIntOrNull() ?: 0 }
        val txts  = listOf(txtS1, txtS2, txtS3, txtS4, txtS5, txtS6, txtS7, txtS8)
        val progs = listOf(progressS1, progressS2, progressS3, progressS4,
                           progressS5, progressS6, progressS7, progressS8)
        sVals.forEachIndexed { i, v ->
            txts[i]?.text      = String.format(Locale.US, "%d", v)
            progs[i]?.progress = v
        }

        txtDirEsq?.text = parts.getOrNull(11) ?: "0"
        txtDirDir?.text = parts.getOrNull(10) ?: "0"
    }

    /**
     * @brief Nula todas as referências de view e envia o comando de parada de streaming ("l").
     *
     * Chamado automaticamente pelo `setOnDismissListener` do diálogo.
     * Se [modalAberto] já for `false` (desconexão Bluetooth anterior), o comando não é reenviado.
     */
    private fun limparReferencias() {
        val eraNecessarioParar = modalAberto
        modalAberto = false
        if (eraNecessarioParar) onEnviarComando("l")

        txtPosicao = null;  progressPosicao = null
        txtS1 = null;  progressS1 = null;  txtS2 = null;  progressS2 = null
        txtS3 = null;  progressS3 = null;  txtS4 = null;  progressS4 = null
        txtS5 = null;  progressS5 = null;  txtS6 = null;  progressS6 = null
        txtS7 = null;  progressS7 = null;  txtS8 = null;  progressS8 = null
        txtDirEsq = null;  txtDirDir = null
    }
}
