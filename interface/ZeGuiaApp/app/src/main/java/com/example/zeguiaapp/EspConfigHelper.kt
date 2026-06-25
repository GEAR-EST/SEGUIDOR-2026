/**
 * @file EspConfigHelper.kt
 * @brief Gerencia o modal de configuração da ESP32: seleção e cadastro de endereços MAC.
 *
 * Permite ao usuário escolher um MAC salvo em um dropdown ou cadastrar um novo.
 * O campo de entrada formata automaticamente o MAC com dois-pontos (XX:XX:XX:XX:XX:XX).
 * A abertura é bloqueada durante corrida ativa ("Correndo") e a confirmação de troca
 * é bloqueada enquanto o socket Bluetooth estiver aberto em estado diferente de "Parado".
 *
 * @author Gear Robotics
 * @version 1.0
 */
package com.example.zeguiaapp

import android.content.SharedPreferences
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.graphics.toColorInt
import androidx.core.view.isVisible
import com.google.android.material.button.MaterialButton

/**
 * @class EspConfigHelper
 * @brief Exibe e gerencia o modal de configuração do endereço MAC da ESP32.
 *
 * @param activity      Activity host para inflação de layout e exibição de Toasts.
 * @param sharedPrefs   SharedPreferences usado para persistir endereços MAC salvos.
 * @param getAddress    Lambda que retorna o MAC atualmente ativo.
 * @param setAddress    Lambda que define o novo MAC ativo.
 * @param getSavedMacs  Lambda que retorna o conjunto mutável de MACs salvos.
 * @param isSocketOpen  Lambda que retorna `true` se há conexão Bluetooth ativa.
 * @param txtEspMac     TextView da tela principal que exibe o MAC ativo.
 * @param onDesconectar Lambda chamado para acionar desconexão quando o MAC é alterado com socket aberto.
 */
class EspConfigHelper(
    private val activity: AppCompatActivity,
    private val sharedPrefs: SharedPreferences,
    private val getAddress: () -> String,
    private val setAddress: (String) -> Unit,
    private val getSavedMacs: () -> MutableSet<String>,
    private val isSocketOpen: () -> Boolean,
    private val txtEspMac: TextView,
    private val onDesconectar: () -> Unit
) {

    /**
     * @brief Abre o modal de configuração da ESP32.
     *
     * @param estadoAtual Texto atual do estado do robô. Se "Correndo", o modal é bloqueado.
     */
    fun abrir(estadoAtual: String) {
        if (estadoAtual == "Correndo") {
            Toast.makeText(activity, "Não é possível alterar durante a corrida", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = activity.layoutInflater.inflate(R.layout.modal_esp32, null)
        val dialog = AlertDialog.Builder(activity)
            .setView(dialogView)
            .create()
            .also { it.window?.setBackgroundDrawableResource(android.R.color.transparent) }

        val savedMacs = getSavedMacs()
        if (savedMacs.isEmpty()) savedMacs.add("C0:49:EF:65:16:FE")

        val dropdownSelectedMac   = dialogView.findViewById<TextView>(R.id.dropdownSelectedMac)
        val dropdownHeader        = dialogView.findViewById<View>(R.id.dropdownHeader)
        val dropdownListCard      = dialogView.findViewById<View>(R.id.dropdownListCard)
        val dropdownListContainer = dialogView.findViewById<LinearLayout>(R.id.dropdownListContainer)
        val editNovoMac           = dialogView.findViewById<EditText>(R.id.editNovoMac)
        val btnSalvarEsp          = dialogView.findViewById<MaterialButton>(R.id.btnSalvarEsp)
        val btnConfirmarSelecao   = dialogView.findViewById<MaterialButton>(R.id.btnConfirmarSelecao)
        val btnFechar             = dialogView.findViewById<View>(R.id.btnFecharModalEsp)
        val btnEditarEsp          = dialogView.findViewById<View>(R.id.btnEditarEsp)
        val btnExcluirEsp         = dialogView.findViewById<View>(R.id.btnExcluirEsp)
        val layoutEditarMac       = dialogView.findViewById<LinearLayout>(R.id.layoutEditarMac)
        val editMacAtivo          = dialogView.findViewById<EditText>(R.id.editMacAtivo)
        val btnSalvarEdicao       = dialogView.findViewById<MaterialButton>(R.id.btnSalvarEdicao)

        var macSelecionadoAtual = getAddress()
        dropdownSelectedMac.text = macSelecionadoAtual

        /**
         * @brief Reconstrói a lista de MACs no dropdown, destacando o MAC selecionado em verde.
         */
        fun atualizarLista() {
            dropdownListContainer.removeAllViews()
            savedMacs.toList().forEach { mac ->
                val item = TextView(activity).apply {
                    text      = mac
                    textSize  = 13f
                    typeface  = android.graphics.Typeface.MONOSPACE
                    setPadding(40, 28, 40, 28)
                    setTextColor(
                        if (mac == macSelecionadoAtual) "#00FF66".toColorInt()
                        else "#CFC9D8".toColorInt()
                    )
                    setOnClickListener {
                        macSelecionadoAtual = mac
                        dropdownSelectedMac.text = mac
                        dropdownListCard.isVisible = false
                        atualizarLista()
                    }
                }
                dropdownListContainer.addView(item)
            }
        }
        atualizarLista()

        val toggleDropdown = View.OnClickListener {
            dropdownListCard.isVisible = !dropdownListCard.isVisible
        }
        dropdownHeader.setOnClickListener(toggleDropdown)

        editNovoMac.addTextChangedListener(criarMacWatcher(editNovoMac))

        btnFechar.setOnClickListener { dialog.dismiss() }

        editMacAtivo.addTextChangedListener(criarMacWatcher(editMacAtivo))

        btnEditarEsp.setOnClickListener {
            val mostrar = !layoutEditarMac.isVisible
            layoutEditarMac.isVisible = mostrar
            if (mostrar) {
                dropdownListCard.isVisible = false
                editMacAtivo.setText(macSelecionadoAtual)
                editMacAtivo.setSelection(editMacAtivo.text.length)
                editMacAtivo.requestFocus()
            }
        }

        btnSalvarEdicao.setOnClickListener {
            val novoMac = editMacAtivo.text.toString().trim()
            if (novoMac.matches(Regex("^([0-9A-F]{2}:){5}[0-9A-F]{2}$"))) {
                savedMacs.remove(macSelecionadoAtual)
                savedMacs.add(novoMac)
                macSelecionadoAtual = novoMac
                sharedPrefs.edit { putStringSet("savedMacs", HashSet(savedMacs)) }
                dropdownSelectedMac.text = novoMac
                layoutEditarMac.isVisible = false
                atualizarLista()
                Toast.makeText(activity, "MAC atualizado!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(activity, "MAC inválido! Use o formato XX:XX:XX:XX:XX:XX", Toast.LENGTH_SHORT).show()
            }
        }

        btnExcluirEsp.setOnClickListener {
            if (savedMacs.size <= 1) {
                Toast.makeText(activity, "Não é possível excluir o único dispositivo salvo", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            AlertDialog.Builder(activity)
                .setTitle("Excluir dispositivo")
                .setMessage("Tem certeza que deseja excluir o dispositivo?\n\n$macSelecionadoAtual")
                .setPositiveButton("Excluir") { _, _ ->
                    savedMacs.remove(macSelecionadoAtual)
                    sharedPrefs.edit { putStringSet("savedMacs", HashSet(savedMacs)) }
                    macSelecionadoAtual = savedMacs.first()
                    dropdownSelectedMac.text = macSelecionadoAtual
                    layoutEditarMac.isVisible = false
                    atualizarLista()
                    Toast.makeText(activity, "Dispositivo removido", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        val podeConfirmar = !isSocketOpen() || estadoAtual == "Parado"
        btnConfirmarSelecao.isEnabled = podeConfirmar
        btnConfirmarSelecao.alpha     = if (podeConfirmar) 1.0f else 0.4f

        btnSalvarEsp.setOnClickListener {
            val mac = editNovoMac.text.toString().trim()
            if (mac.isEmpty()) {
                Toast.makeText(activity, "Digite um endereço MAC para adicionar", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (mac.matches(Regex("^([0-9A-F]{2}:){5}[0-9A-F]{2}$"))) {
                savedMacs.add(mac)
                macSelecionadoAtual = mac
                sharedPrefs.edit { putStringSet("savedMacs", HashSet(savedMacs)) }
                dropdownSelectedMac.text = mac
                editNovoMac.setText("")
                atualizarLista()
                Toast.makeText(activity, "Nova ESP32 cadastrada!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(activity, "MAC inválido! Use o formato XX:XX:XX:XX:XX:XX", Toast.LENGTH_SHORT).show()
            }
        }

        btnConfirmarSelecao.setOnClickListener {
            val currentAddress = getAddress()
            if (currentAddress != macSelecionadoAtual) {
                setAddress(macSelecionadoAtual)
                sharedPrefs.edit { putString("selectedMac", macSelecionadoAtual) }
                txtEspMac.text = macSelecionadoAtual
                Toast.makeText(activity, "ESP32 selecionada: $macSelecionadoAtual", Toast.LENGTH_SHORT).show()
                if (isSocketOpen()) onDesconectar()
            } else {
                Toast.makeText(activity, "ESP32 já ativa: $currentAddress", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        dialog.show()
    }

    /**
     * @brief Cria um TextWatcher que formata automaticamente o campo de MAC com dois-pontos.
     *
     * Remove todos os ':' existentes, converte para maiúsculas e reinsere os separadores
     * a cada dois caracteres hexadecimais, até o limite de 17 caracteres (XX:XX:XX:XX:XX:XX).
     *
     * @param editText Campo de texto ao qual o watcher será vinculado.
     * @return TextWatcher configurado para formatação de endereço MAC.
     */
    private fun criarMacWatcher(editText: EditText): android.text.TextWatcher {
        return object : android.text.TextWatcher {
            private var isFormatting  = false
            private var deletingColon = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                deletingColon = count == 1 && after == 0 && s?.getOrNull(start) == ':'
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: android.text.Editable?) {
                if (isFormatting) return
                isFormatting = true

                val clean  = s.toString().replace(":", "").uppercase()
                val texto  = if (deletingColon && clean.length >= 2) clean.substring(0, clean.length - 1) else clean
                val sb     = StringBuilder()
                texto.forEachIndexed { i, c -> if (i > 0 && i % 2 == 0) sb.append(':'); sb.append(c) }
                val result = sb.toString().take(17)
                if (result != s.toString()) {
                    s?.replace(0, s.length, result)
                    editText.setSelection(result.length)
                }

                isFormatting  = false
                deletingColon = false
            }
        }
    }
}
