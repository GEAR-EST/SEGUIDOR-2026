// ============================================================
// SobreNosHelper.kt
// Cole este arquivo em:  app/src/main/java/com/example/zeguiaapp/
//
// No seu MainActivity.kt, no onCreate(), chame:
//   SobreNosHelper.setup(this)
// ============================================================

package com.example.zeguiaapp

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.ScrollView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

object SobreNosHelper {

    // ── Referências para poder cancelar quando a Activity destruir ──
    private val handler = Handler(Looper.getMainLooper())
    private var luzRunnable: Runnable? = null
    private var dotRunnable: Runnable? = null
    private var dialog: AlertDialog? = null

    // ════════════════════════════════════════════════════════════════
    //  Ponto verde do Terminal piscando
    // ════════════════════════════════════════════════════════════════
    fun startDotBlink(activity: AppCompatActivity) {
        // O dot fica dentro de terminalLabelContainer (primeiro filho, um View)
        val dot = activity.findViewById<View>(R.id.terminalLabelContainer)
            ?.let { (it as? android.view.ViewGroup)?.getChildAt(0) }
            ?: return

        dot.visibility = View.VISIBLE
        dot.alpha = 1.0f

        var aceso = true
        dotRunnable = object : Runnable {
            override fun run() {
                aceso = !aceso
                dot.alpha = if (aceso) 1.0f else 0.3f
                handler.postDelayed(this, 900)
            }
        }
        handler.post(dotRunnable!!)
    }

    // ════════════════════════════════════════════════════════════════
    //  Setup geral — chame no onCreate()
    // ════════════════════════════════════════════════════════════════
    fun setup(activity: AppCompatActivity) {
        // Dot piscando
        startDotBlink(activity)

        // Clique no logo abre o modal
        activity.findViewById<View>(R.id.btnLogo)?.setOnClickListener {
            it.animate()
                .scaleX(0.78f).scaleY(0.78f)
                .setDuration(90)
                .withEndAction {
                    it.animate().scaleX(1f).scaleY(1f).setDuration(110).start()
                }
                .start()
            showModal(activity)
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  Modal Sobre Nós
    // ════════════════════════════════════════════════════════════════
    fun showModal(context: Context) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_sobre_nos, null)
        val dlg = AlertDialog.Builder(context)
            .setView(dialogView)
            .create()
        dialog = dlg

        dlg.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Fechar
        dialogView.findViewById<View>(R.id.btnFecharModal)?.setOnClickListener {
            stopLuzAnimation()
            dlg.dismiss()
        }
        dlg.setOnDismissListener { stopLuzAnimation() }

        dlg.show()

        // Inicia animação das luzes após o dialog estar visível
        val luzes = listOf(
            dialogView.findViewById<View>(R.id.luz1),
            dialogView.findViewById<View>(R.id.luz2),
            dialogView.findViewById<View>(R.id.luz3),
            dialogView.findViewById<View>(R.id.luz4),
            dialogView.findViewById<View>(R.id.luz5)
        )
        startLuzAnimation(luzes)
    }

    // ════════════════════════════════════════════════════════════════
    //  Animação das luzes — estilo grid de largada F1
    //
    //  Sequência (loop):
    //   0. todas apagadas            — 600 ms
    //   1. luz 1 acende              — 600 ms
    //   2. luz 1+2 acendem           — 600 ms
    //   3. luz 1+2+3 acendem         — 600 ms
    //   4. luz 1+2+3+4 acendem       — 600 ms
    //   5. todas 5 acendem           — 800 ms
    //   6. todas APAGAM (largada!)   — 1200 ms de pausa
    //   → repete
    // ════════════════════════════════════════════════════════════════
    private fun startLuzAnimation(luzes: List<View>) {
        val apagada = R.drawable.bg_luz_apagada
        val acesa   = R.drawable.bg_luz_acesa

        fun setLuzes(quantas: Int) {
            luzes.forEachIndexed { i, v ->
                v.setBackgroundResource(if (i < quantas) acesa else apagada)
            }
        }

        val step = 600L   // ms entre cada luz
        val hold = 800L   // ms com todas acesas
        val gap  = 1200L  // ms de pausa após apagar (largada)

        var currentStep = 0

        luzRunnable = object : Runnable {
            override fun run() {
                when (currentStep) {
                    0 -> { setLuzes(0); handler.postDelayed(this, gap) }
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

    private fun stopLuzAnimation() {
        luzRunnable?.let { handler.removeCallbacks(it) }
        luzRunnable = null
    }

    // Chame no onDestroy() da Activity para limpar tudo
    fun onDestroy() {
        luzRunnable?.let { handler.removeCallbacks(it) }
        dotRunnable?.let { handler.removeCallbacks(it) }
        dialog?.dismiss()
        luzRunnable = null
        dotRunnable = null
        dialog = null
    }
}