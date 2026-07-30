#pragma once
/**
 * @file communication.h
 * @brief Declarações públicas do módulo de comunicação Bluetooth da ESP32.
 *
 * Expõe a task FreeRTOS @ref CommunicationTask, as funções de bateria e as
 * variáveis globais compartilhadas @ref SerialBT e @ref btMutex, utilizados
 * por outros módulos para enviar dados ao app de forma thread-safe.
 *
 * @author Gear Robotics
 * @version 1.0
 */

#include <BluetoothSerial.h>
#include <freertos/semphr.h>

#define BATTERY_PIN 13

/** @brief Instância global do Bluetooth Serial (SPP). Usada por outros módulos para enviar dados ao app. */
extern BluetoothSerial SerialBT;

/** @brief Mutex FreeRTOS que garante acesso exclusivo ao @ref SerialBT em ambiente multithread. */
extern SemaphoreHandle_t btMutex;

/**
 * @brief Calcula o percentual de bateria a partir da leitura analógica do pino de bateria.
 * @return Percentual de bateria entre 0 e 100.
 */
uint8_t battery_percentage();

/**
 * @brief Envia o percentual de bateria via Bluetooth no formato `BAT<valor>` a cada intervalo definido.
 */
void send_battery();

/** @brief Tempo da última leitura de bateria enviada (ms). */
extern unsigned long past_time;

/** @brief Intervalo em ms entre envios de bateria (10 segundos). */
const long bat_interval = 10000;

/**
 * @brief Task FreeRTOS principal de comunicação Bluetooth.
 *
 * Deve ser criada com @c xTaskCreatePinnedToCore no núcleo 0.
 * Inicializa o Bluetooth, lê comandos do app e encaminha mensagens para a fila.
 *
 * @param pvParameters Parâmetro padrão de task FreeRTOS (não utilizado).
 */
void CommunicationTask(void* pvParameters);
