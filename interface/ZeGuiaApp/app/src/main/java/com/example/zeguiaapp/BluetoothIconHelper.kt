/**
 * @file BluetoothIconHelper.kt
 * @brief Atualiza o ícone e o card de Bluetooth conforme o estado da conexão.
 *
 * Aplica cor ao ícone via ColorFilter e ao card via GradientDrawable
 * com borda e fundo semi-transparentes na mesma tonalidade.
 *
 * @author Gear Robotics
 * @version 1.0
 */
package com.example.zeguiaapp

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.ImageView
import androidx.core.graphics.toColorInt

/**
 * @class BluetoothIconHelper
 * @brief Encapsula a lógica de colorização do ícone e card de status Bluetooth.
 *
 * @param iconBluetooth ImageView do ícone Bluetooth na toolbar.
 * @param cardBluetooth View do card que contém o toggle Bluetooth.
 * @param density       Densidade de pixels do display (`resources.displayMetrics.density`).
 */
class BluetoothIconHelper(
    private val iconBluetooth: ImageView,
    private val cardBluetooth: View,
    private val density: Float
) {

    /**
     * @brief Aplica uma cor ao ícone e ao card de Bluetooth.
     *
     * O card recebe borda com 31% de opacidade e fundo com 8% de opacidade
     * da cor fornecida, criando um efeito de glassmorphism leve.
     *
     * @param corHex Cor em formato hexadecimal (ex: "#00FF66").
     */
    private fun aplicarCor(corHex: String) {
        val cor = corHex.toColorInt()
        iconBluetooth.setColorFilter(cor)
        val drawable = GradientDrawable().apply {
            shape         = GradientDrawable.RECTANGLE
            cornerRadius  = 10f * density
            setStroke((1f * density).toInt(), Color.argb(80, Color.red(cor), Color.green(cor), Color.blue(cor)))
            setColor(Color.argb(20, Color.red(cor), Color.green(cor), Color.blue(cor)))
        }
        cardBluetooth.background = drawable
    }

    /**
     * @brief Exibe estado "conectado" com cor verde (#00FF66).
     */
    fun setConectado() = aplicarCor("#00FF66")

    /**
     * @brief Exibe estado "desconectado" com cor roxa (#A066FF).
     */
    fun setDesconectado() = aplicarCor("#A066FF")

    /**
     * @brief Exibe estado "conectando" com cor laranja (#FFA500).
     */
    fun setConectando() = aplicarCor("#FFA500")
}
