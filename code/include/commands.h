/**
 * @file commands.h
 * @brief Definição dos comandos e mensagens trocados entre a tarefa de comunicação e a lógica do robô.
 *
 * O enum @ref RobotCommand representa cada ação que o app pode solicitar ao robô.
 * A struct @ref RobotMessage encapsula um comando junto com os parâmetros PID opcionais,
 * sendo a unidade de dado enfileirada na commandsQueue entre as tasks FreeRTOS.
 *
 * @author Gear Robotics
 * @version 1.0
 */

#ifndef COMMANDS_H
#define COMMANDS_H

/**
 * @enum RobotCommand
 * @brief Comandos de controle enviados pelo app via Bluetooth.
 *
 * Cada valor mapeia diretamente a um caractere ou prefixo do protocolo serial:
 * - `1` / `0`  → CMD_LED_ON / CMD_LED_OFF
 * - `K`        → CMD_CALIBRATE
 * - `R`        → CMD_START
 * - `F`        → CMD_STOP
 * - `S`        → CMD_MODE_FOLLOWER
 * - `P`        → CMD_MODE_CHASE
 * - `C`        → CMD_STRATEGY_CONSERVATIVE
 * - `A`        → CMD_STRATEGY_RISK
 * - `L` / `l`  → CMD_SENSOR_STREAM_ON / CMD_SENSOR_STREAM_OFF
 * - `Q`        → CMD_GET_PARAMS
 */
enum RobotCommand
{
    CMD_LED_ON,               /**< Liga o LED interno da ESP32. */
    CMD_LED_OFF,              /**< Desliga o LED interno da ESP32. */
    CMD_NONE,                 /**< Nenhum comando; usado para inicializar RobotMessage sem ação. */
    CMD_CALIBRATE,            /**< Inicia a calibração dos sensores de linha. */
    CMD_START,                /**< Inicia a corrida. */
    CMD_STOP,                 /**< Para a corrida. */
    CMD_MODE_FOLLOWER,        /**< Seleciona o modo Seguidor de Linha. */
    CMD_MODE_CHASE,           /**< Seleciona o modo Perseguidor. */
    CMD_STRATEGY_CONSERVATIVE,/**< Seleciona a estratégia Conservadora. */
    CMD_STRATEGY_RISK,        /**< Seleciona a estratégia Arriscada. */
    CMD_SENSOR_STREAM_ON,     /**< Liga o stream periódico de leitura dos sensores. */
    CMD_SENSOR_STREAM_OFF,    /**< Desliga o stream de leitura dos sensores. */
    CMD_GET_PARAMS            /**< Solicita o envio de todos os parâmetros PID salvos. */
};

/**
 * @struct RobotMessage
 * @brief Mensagem enfileirada na commandsQueue entre CommunicationTask e ControlsTask.
 *
 * Pode conter um comando simples, parâmetros PID ou ambos simultaneamente.
 * O campo @p hasPidTunings indica se os campos de PID devem ser processados.
 */
struct RobotMessage
{
    RobotCommand command;  /**< Comando a ser executado pela lógica do robô. */
    bool hasPidTunings;    /**< `true` se os campos de PID abaixo são válidos. */
    float vMax;            /**< Velocidade máxima recebida via protocolo PID. */
    float kp;              /**< Ganho proporcional do controlador PID. */
    float ki;              /**< Ganho integral do controlador PID. */
    float kd;              /**< Ganho derivativo do controlador PID. */
    int novasMarcas;       /**< Número de marcas laterais para parada autônoma. */
};

#endif
