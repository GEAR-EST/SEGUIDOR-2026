/**
 * @file SobreNosHelper.kt
 * @brief Gerencia o modal "Sobre Nós" e a animação do ponto piscante no terminal.
 *
 * O modal exibe informações da equipe com animação de luzes inspirada no grid de
 * largada da Fórmula 1: as luzes acendem sequencialmente e apagam de uma vez ("largada!").
 * O ponto verde no terminal principal pisca a cada 900 ms enquanto a Activity estiver ativa.
 *
 * @author Gear Robotics
 * @version 1.0
 */
package com.example.zeguiaapp

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

/**
 * @object SobreNosHelper
 * @brief Singleton que gerencia o ciclo de vida do modal "Sobre Nós" e das animações associadas.
 *
 * Deve ser inicializado via [setup] no `onCreate()` e destruído via [onDestroy] no `onDestroy()`
 * da Activity para evitar memory leaks nos Handlers.
 */
object SobreNosHelper {

    private val handler   = Handler(Looper.getMainLooper())

    /** @brief Runnable responsável pela animação das luzes no modal. */
    private var luzRunnable: Runnable? = null

    /** @brief Runnable responsável pelo piscar do ponto verde no terminal. */
    private var dotRunnable: Runnable? = null

    /** @brief Referência ao AlertDialog atual para poder descartá-lo no onDestroy. */
    private var dialog: AlertDialog? = null

    /**
     * @brief Inicia o piscar do ponto verde no label do terminal.
     *
     * O ponto alterna entre alpha 1.0 e 0.3 a cada 900 ms.
     * Busca o primeiro filho de `terminalLabelContainer` como o view do ponto.
     *
     * @param activity Activity onde o view `terminalLabelContainer` está inflado.
     */
    fun startDotBlink(activity: AppCompatActivity) {
        val dot = activity.findViewById<View>(R.id.terminalLabelContainer)
            ?.let { (it as? android.view.ViewGroup)?.getChildAt(0) }
            ?: return

        dot.visibility = View.VISIBLE
        dot.alpha      = 1.0f

        var aceso = true
        dotRunnable = object : Runnable {
            override fun run() {
                aceso   = !aceso
                dot.alpha = if (aceso) 1.0f else 0.3f
                handler.postDelayed(this, 900)
            }
        }
        handler.post(dotRunnable!!)
    }

    /**
     * @brief Configura o helper: inicia o piscar do ponto e registra o clique no logo.
     *
     * Deve ser chamado no `onCreate()` da Activity, após `setContentView()`.
     *
     * @param activity Activity host que contém os views `terminalLabelContainer` e `btnLogo`.
     */
    fun setup(activity: AppCompatActivity) {
        startDotBlink(activity)
        activity.findViewById<View>(R.id.btnLogo)?.setOnClickListener {
            it.animate()
                .scaleX(0.78f).scaleY(0.78f)
                .setDuration(90)
                .withEndAction { it.animate().scaleX(1f).scaleY(1f).setDuration(110).start() }
                .start()
            showModal(activity)
        }
    }

    /**
     * @brief Infla e exibe o modal "Sobre Nós" com animação de luzes.
     *
     * @param context Context usado para inflar o layout e criar o AlertDialog.
     */
    fun showModal(context: Context) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_sobre_nos, null)
        val dlg = AlertDialog.Builder(context)
            .setView(dialogView)
            .create()
        dialog = dlg

        dlg.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<View>(R.id.btnFecharModal)?.setOnClickListener {
            stopLuzAnimation()
            dlg.dismiss()
        }
        dlg.setOnDismissListener { stopLuzAnimation() }
        dlg.show()

        val luzes = listOf(
            dialogView.findViewById<View>(R.id.luz1),
            dialogView.findViewById<View>(R.id.luz2),
            dialogView.findViewById<View>(R.id.luz3),
            dialogView.findViewById<View>(R.id.luz4),
            dialogView.findViewById<View>(R.id.luz5)
        )
        startLuzAnimation(luzes)
    }

    /**
     * @brief Executa a animação de luzes estilo grid de largada F1 em loop.
     *
     * Sequência de um ciclo completo:
     * - Passo 0: todas apagadas (1200 ms de pausa — "largada")
     * - Passos 1–4: acende uma luz por vez, a cada 600 ms
     * - Passo 5: todas acesas (800 ms)
     * - Repete a partir do passo 0
     *
     * @param luzes Lista ordenada de Views representando as 5 luzes.
     */
    private fun startLuzAnimation(luzes: List<View>) {
        val apagada = R.drawable.bg_luz_apagada
        val acesa   = R.drawable.bg_luz_acesa
        val step    = 600L
        val hold    = 800L
        val gap     = 1200L

        fun setLuzes(quantas: Int) {
            luzes.forEachIndexed { i, v ->
                v.setBackgroundResource(if (i < quantas) acesa else apagada)
            }
        }

        var currentStep = 0
        luzRunnable = object : Runnable {
            override fun run() {
                when (currentStep) {
                    0 -> { setLuzes(0); handler.postDelayed(this, gap)  }
                    1 -> { setLuzes(1); handler.postDelayed(this, step) }
                    2 -> { setLuzes(2); handler.postDelayed(this, step) }
                    3 -> { setLuzes(3); handler.postDelayed(this, step) }
                    4 -> { setLuzes(4); handler.postDelayed(this, step) }
                    5 -> { setLuzes(5); handler.postDelayed(this, hold) }
                }
                currentStep = (currentStep + 1) % 6
            }
        }
        handler.post(luzRunnable!!)
    }

    /**
     * @brief Para a animação das luzes, removendo o Runnable da fila do Handler.
     */
    private fun stopLuzAnimation() {
        luzRunnable?.let { handler.removeCallbacks(it) }
        luzRunnable = null
    }

    /**
     * @brief Libera todos os recursos: para animações, remove callbacks e descarta o diálogo.
     *
     * Deve ser chamado no `onDestroy()` da Activity para evitar vazamentos de memória.
     */
    fun onDestroy() {
        luzRunnable?.let { handler.removeCallbacks(it) }
        dotRunnable?.let { handler.removeCallbacks(it) }
        dialog?.dismiss()
        luzRunnable = null
        dotRunnable = null
        dialog      = null
    }
}
