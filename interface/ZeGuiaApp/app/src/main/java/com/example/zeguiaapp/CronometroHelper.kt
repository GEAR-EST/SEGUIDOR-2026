/**
 * @file CronometroHelper.kt
 * @brief Gerencia o cronômetro de corrida exibido na tela principal.
 *
 * O display usa o formato SS.cc (segundos inteiros com dois centésimos).
 * Exemplo: "98.50" → 98 segundos e 50 centésimos.
 *
 * @author Gear Robotics
 * @version 1.0
 */
package com.example.zeguiaapp

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import java.util.Locale

/**
 * @class CronometroHelper
 * @brief Controla start, stop e reset do cronômetro com atualização a cada 50 ms.
 *
 * @param txtCronometro TextView onde o tempo é exibido.
 */
class CronometroHelper(private val txtCronometro: TextView) {

    private val handler = Handler(Looper.getMainLooper())

    /** @brief `true` enquanto o cronômetro estiver contando. */
    private var rodando = false

    /** @brief Timestamp em milissegundos do instante em que o cronômetro foi iniciado. */
    private var tempoInicioMs = 0L

    /**
     * @brief Runnable que recalcula e exibe o tempo decorrido a cada 50 ms.
     */
    private val atualizarRunnable = object : Runnable {
        override fun run() {
            if (!rodando) return
            val decorrido   = System.currentTimeMillis() - tempoInicioMs
            val segundos    = decorrido / 1000
            val centesimos  = (decorrido % 1000) / 10
            txtCronometro.text = String.format(Locale.US, "%d.%02d", segundos, centesimos)
            handler.postDelayed(this, 50)
        }
    }

    /**
     * @brief Inicia o cronômetro a partir do instante atual.
     *
     * Não faz nada se o cronômetro já estiver rodando.
     */
    fun iniciar() {
        if (rodando) return
        rodando = true
        tempoInicioMs = System.currentTimeMillis()
        handler.post(atualizarRunnable)
    }

    /**
     * @brief Para o cronômetro sem alterar o valor exibido no display.
     */
    fun parar() {
        rodando = false
        handler.removeCallbacks(atualizarRunnable)
    }

    /**
     * @brief Para o cronômetro e redefine o display para "0.00".
     */
    @SuppressLint("SetTextI18n")
    fun resetar() {
        parar()
        txtCronometro.text = "0.00"
    }

    /**
     * @brief Retorna o texto atualmente exibido no cronômetro.
     * @return String no formato "SS.cc" ou "0.00" se resetado.
     */
    fun getTexto(): String = txtCronometro.text.toString()
}
