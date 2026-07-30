/**
 * @file BluetoothEventListener.kt
 * @brief Interface de callbacks para eventos de conexão Bluetooth e mensagens seriais.
 *
 * Implementada pela Activity principal para reagir a eventos da ESP32
 * sem acoplamento direto ao BluetoothHelper.
 *
 * @author Gear Robotics
 * @version 1.0
 */
package com.example.zeguiaapp

/**
 * @interface BluetoothEventListener
 * @brief Callbacks disparados pelo BluetoothHelper conforme eventos de conexão e dados.
 */
interface BluetoothEventListener {

    /** @brief Conexão RFCOMM estabelecida com sucesso. */
    fun onConnected()

    /** @brief Conexão encerrada (por desconexão deliberada ou falha). */
    fun onDisconnected()

    /** @brief Tentativa de conexão falhou (ex: dispositivo fora de alcance). */
    fun onConnectionFailed()

    /**
     * @brief Mensagem serial genérica recebida (não pertence a nenhum prefixo conhecido).
     * @param msg Linha recebida, sem o '\n' final.
     */
    fun onRawMessage(msg: String)

    /**
     * @brief Estado do robô atualizado pela ESP32.
     * @param valor Ex: "Correndo", "Parado", "Calibrando", "Calibrado".
     */
    fun onEstado(valor: String)

    /**
     * @brief Modo de operação confirmado pela ESP32.
     * @param valor Ex: "Seguidor", "Perseguidor".
     */
    fun onModo(valor: String)

    /**
     * @brief Estratégia de corrida confirmada pela ESP32.
     * @param valor Ex: "Conservador", "Arriscado".
     */
    fun onEstrategia(valor: String)

    /**
     * @brief Nível de bateria recebido.
     * @param percentual Valor entre 0 e 100.
     */
    fun onBateria(percentual: Int)

    /**
     * @brief Leitura dos sensores de linha recebida (mensagem prefixada com "S,").
     * @param parts Tokens da linha: [0]="S", [1]=posição, [2..9]=s1..s8, [10]=dirDir, [11]=dirEsq.
     */
    fun onSensores(parts: List<String>)

    /**
     * @brief Parâmetros PID sincronizados pela ESP32 (mensagem prefixada com "PARAMS:").
     * @param parts Tokens após "PARAMS:": [0]=modo, [1]=estrategia, [2]=V, [3]=Kp, [4]=Ki, [5]=Kd, [6]=marcas.
     */
    fun onParams(parts: List<String>)
}
