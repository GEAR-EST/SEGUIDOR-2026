/**
 * @file BluetoothHelper.kt
 * @brief Gerencia a conexão RFCOMM com a ESP32, recebimento e envio de dados seriais.
 *
 * Usa o perfil SPP (Serial Port Profile) com UUID padrão para emular uma porta serial
 * sem fio sobre Bluetooth Classic. Todas as operações de I/O são executadas em threads
 * separadas; os callbacks ao listener ocorrem na UI thread via `runOnUiThread`.
 *
 * Protocolo de mensagens recebidas:
 * - `PARAMS:modo|strat|V|Kp|Ki|Kd|marcas`  → parâmetros PID sincronizados
 * - `BATxx`                                 → nível de bateria (0–100)
 * - `S,pos,s1,...,s8,dirDir,dirEsq`         → leitura dos sensores
 * - `Estado: <valor>`                       → estado do robô
 * - `Modo: <valor>`                         → modo de operação
 * - `Estrategia: <valor>`                   → estratégia de corrida
 * - qualquer outra linha                    → mensagem genérica (log serial)
 *
 * @author Gear Robotics
 * @version 1.0
 */
package com.example.zeguiaapp

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.UUID

/**
 * @class BluetoothHelper
 * @brief Controla o ciclo de vida da conexão Bluetooth e o parser de dados seriais.
 *
 * @param activity        Activity host (para Toasts e `runOnUiThread`).
 * @param btAdapter       Adaptador Bluetooth do sistema.
 * @param uuid            UUID SPP para criar o socket RFCOMM.
 * @param swBluetooth     Switch de controle de conexão (revertido em caso de falha).
 * @param sensoresHelper  Referência ao SensoresHelper para gerenciar parada de streaming.
 * @param listener        Implementação de [BluetoothEventListener] para callbacks de eventos.
 */
class BluetoothHelper(
    private val activity: AppCompatActivity,
    private val btAdapter: BluetoothAdapter,
    private val uuid: UUID,
    private val swBluetooth: SwitchCompat,
    private val sensoresHelper: SensoresHelper,
    private val listener: BluetoothEventListener
) {

    /** @brief Socket RFCOMM ativo com a ESP32, ou `null` quando desconectado. */
    var btSocket: BluetoothSocket? = null
        private set

    /** @brief Flag que mantém o loop de leitura em background ativo. */
    private var continuarEscutando = false

    /** @brief Endereço MAC do dispositivo-alvo, definido via [setAddress]. */
    private var address: String = ""

    /**
     * @brief Define o endereço MAC da ESP32 a ser conectada.
     * @param mac Endereço no formato "XX:XX:XX:XX:XX:XX".
     */
    fun setAddress(mac: String) { address = mac }

    /**
     * @brief Verifica se a permissão `BLUETOOTH_CONNECT` foi concedida (obrigatória no Android 12+).
     * @return `true` se a permissão está disponível ou se o SDK é anterior ao Android 12.
     */
    fun temPermissao(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            ContextCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        else
            true

    /**
     * @brief Inicia a conexão RFCOMM com a ESP32 em uma thread de background.
     *
     * Em caso de sucesso, inicia o loop de recebimento de dados e notifica [listener].
     * Em caso de falha, reverte o switch e notifica [listener].
     */
    @SuppressLint("MissingPermission")
    fun conectar() {
        if (!temPermissao()) {
            activity.runOnUiThread {
                Toast.makeText(activity, "Permissão de Bluetooth necessária!", Toast.LENGTH_SHORT).show()
                swBluetooth.isChecked = false
            }
            return
        }
        if (!btAdapter.isEnabled) {
            Toast.makeText(activity, "Ative o Bluetooth!", Toast.LENGTH_SHORT).show()
            swBluetooth.isChecked = false
            return
        }

        Thread {
            try {
                val dispositivo = btAdapter.getRemoteDevice(address)
                btSocket = dispositivo.createRfcommSocketToServiceRecord(uuid)
                btAdapter.cancelDiscovery()
                btSocket?.connect()
                continuarEscutando = true
                receberDados()
                activity.runOnUiThread { listener.onConnected() }
            } catch (e: IOException) {
                activity.runOnUiThread {
                    swBluetooth.isChecked = false
                    listener.onConnectionFailed()
                }
                btSocket = null
            }
        }.start()
    }

    /**
     * @brief Encerra a conexão Bluetooth de forma limpa.
     *
     * Se o modal de sensores estiver aberto, envia o comando de parada de streaming
     * diretamente no socket (sem toast) antes de fechá-lo, e marca o modal como fechado
     * para evitar reenvio duplicado ao dispensar o diálogo.
     */
    fun desconectar() {
        try {
            continuarEscutando = false
            if (sensoresHelper.modalAberto) {
                try { btSocket?.outputStream?.write("l\n".toByteArray()) } catch (_: Exception) {}
                sensoresHelper.modalAberto = false
            }
            btSocket?.close()
            btSocket = null
            activity.runOnUiThread { listener.onDisconnected() }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * @brief Envia um comando serial à ESP32.
     *
     * Acrescenta `\n` automaticamente ao final do comando se necessário.
     * Exibe um Toast se não houver conexão ativa.
     *
     * @param sinal Comando a enviar (ex: "R", "F", "K", "L", "PID:...").
     */
    fun enviarComando(sinal: String) {
        if (btSocket == null) {
            Toast.makeText(activity, "Não está conectado!", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val payload = if (sinal.endsWith("\n")) sinal else "$sinal\n"
            btSocket?.outputStream?.write(payload.toByteArray())
        } catch (e: IOException) {
            Toast.makeText(activity, "Erro ao enviar dados", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * @brief Lê continuamente as linhas do stream serial e as despacha ao listener.
     *
     * Executa em uma thread de background. Termina automaticamente quando
     * [continuarEscutando] é `false` ou o stream é encerrado.
     */
    private fun receberDados() {
        Thread {
            val input  = btSocket?.inputStream ?: return@Thread
            val reader = BufferedReader(InputStreamReader(input))
            while (continuarEscutando) {
                try {
                    val mensagem = reader.readLine() ?: break
                    activity.runOnUiThread { despacharMensagem(mensagem.trim()) }
                } catch (e: IOException) {
                    break
                }
            }
        }.start()
    }

    /**
     * @brief Analisa uma mensagem serial e invoca o callback adequado no listener.
     *
     * Mensagens de bateria e sensores não são adicionadas ao log serial (`onRawMessage`).
     *
     * @param msg Linha recebida, já sem espaços extras e sem o '\n' final.
     */
    private fun despacharMensagem(msg: String) {
        when {
            msg.startsWith("PARAMS:") -> {
                val parts = msg.removePrefix("PARAMS:").split("|")
                if (parts.size >= 7) listener.onParams(parts)
            }
            msg.startsWith("BAT") -> {
                val percentual = msg.removePrefix("BAT").trim().toIntOrNull()
                if (percentual != null) listener.onBateria(percentual)
            }
            msg.startsWith("S,") -> {
                listener.onSensores(msg.split(",").map { it.trim() })
            }
            msg.startsWith("Estado: ") -> {
                listener.onEstado(msg.removePrefix("Estado: "))
            }
            msg.startsWith("Modo: ") -> {
                listener.onModo(msg.removePrefix("Modo: "))
            }
            msg.startsWith("Estrategia: ") -> {
                listener.onEstrategia(msg.removePrefix("Estrategia: "))
            }
            else -> listener.onRawMessage(msg)
        }
    }

    /**
     * @brief Fecha o socket e interrompe o loop de leitura sem notificar o listener.
     *
     * Deve ser chamado no `onDestroy()` da Activity para liberar recursos.
     */
    fun fechar() {
        continuarEscutando = false
        try { btSocket?.close() } catch (_: IOException) {}
        btSocket = null
    }
}
