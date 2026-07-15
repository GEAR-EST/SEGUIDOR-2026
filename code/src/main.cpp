/**
 * @file main.cpp
 * @brief Ponto de entrada do firmware do robô ZeGuia na ESP32.
 *
 * Inicializa a fila de comandos FreeRTOS e cria as duas tasks do sistema:
 * - @ref CommunicationTask (núcleo 0, prioridade 1): gerencia Bluetooth e parsing de mensagens.
 * - ControlsTask (núcleo 1, prioridade 3): executa a lógica de controle e PID do robô.
 *
 * A função loop() permanece vazia pois toda a lógica é executada pelas tasks FreeRTOS.
 *
 * @author Gear Robotics
 * @version 1.0
 */

#include <Arduino.h>
#include <BluetoothSerial.h>
#include "controls.h"
#include "communication.h"
#include "commands.h"
#include "zeGuia.h"

/** @brief Fila FreeRTOS que transfere RobotMessage da CommunicationTask para a ControlsTask. */
QueueHandle_t commandsQueue;

/** @brief Instância global única da lógica principal do robô. */
ZeGuia zeGuia;

/**
 * @brief Inicialização do sistema: cria a fila de comandos e lança as tasks FreeRTOS.
 *
 * Executado uma única vez após o boot da ESP32. Em caso de falha na criação da fila,
 * entra em loop infinito com mensagem de erro na serial.
 */
void setup() {
  Serial.begin(115200);
  delay(1500);
  Serial.println("Sistema Iniciando...");

  commandsQueue = xQueueCreate(10, sizeof(RobotMessage));
  if (commandsQueue == NULL)
  {
    Serial.println("Falha ao criar commandsQueue");
    while (true)
    {
      delay(1000);
    }
  }

  xTaskCreatePinnedToCore(ControlsTask, "Task_PID", 4096, NULL, 3, NULL, 1);

  xTaskCreatePinnedToCore(CommunicationTask, "Task_BT", 8192, NULL, 1, NULL, 0);
}

/**
 * @brief Loop principal — não utilizado.
 *
 * Toda a lógica é executada pelas tasks FreeRTOS criadas em setup().
 */
void loop() {
}
