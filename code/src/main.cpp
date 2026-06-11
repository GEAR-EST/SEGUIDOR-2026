/**
 * @file main.cpp
 * @brief Ponto de entrada do firmware do ZeGuia.
 *
 * Cria a fila de comandos e inicializa as duas tasks FreeRTOS:
 * - @c ControlsTask  (core 1, prioridade 3): loop de controle PID.
 * - @c CommunicationTask (core 0, prioridade 1): recepção Bluetooth.
 */

#include <Arduino.h>
#include <BluetoothSerial.h>
#include "controls.h"
#include "communication.h"
#include "commands.h"
#include "zeGuia.h"

QueueHandle_t commandsQueue; ///< Fila de mensagens entre CommunicationTask e ControlsTask.
ZeGuia zeGuia;               ///< Instância global do robô.

/**
 * @brief Inicialização do sistema.
 *
 * Sequência de boot:
 * 1. Abre Serial para depuração a 115200 baud.
 * 2. Cria @c commandsQueue (capacidade: 10 mensagens).
 * 3. Trava em loop infinito se a fila não puder ser alocada.
 * 4. Sobe as tasks de controle e comunicação em cores separados.
 */
void setup() {
    Serial.begin(115200);
    delay(1500);
    Serial.println("Sistema Iniciando...");

    commandsQueue = xQueueCreate(10, sizeof(RobotMessage));
    if (commandsQueue == NULL) {
        Serial.println("Falha ao criar commandsQueue");
        while (true) { delay(1000); } /* Trava: sem fila não há operação segura. */
    }

    /* Core 1 — controle PID (maior prioridade para tempo-real). */
    xTaskCreatePinnedToCore(ControlsTask,      "Task_PID", 4096, NULL, 3, NULL, 1);

    /* Core 0 — comunicação Bluetooth (menor prioridade, tolerante a latência). */
    xTaskCreatePinnedToCore(CommunicationTask, "Task_BT",  8192, NULL, 1, NULL, 0);
}

/**
 * @brief Loop principal do Arduino (não utilizado).
 *
 * Toda a lógica roda em tasks FreeRTOS; este loop fica vazio
 * e a task @c loopTask do Arduino-ESP32 simplesmente cede CPU.
 */
void loop() {}