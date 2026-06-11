/**
 * @file controls.cpp
 * @brief Task de controle principal do robô ZeGuia.
 *
 * Gerencia o loop de controle do robô: processa mensagens da fila de comandos
 * e executa o ciclo principal de operação via FreeRTOS.
 */

#include "controls.h"
#include "motordriver.h"
#include "sensors.h"
#include "commands.h"
#include "zeGuia.h"

extern QueueHandle_t commandsQueue; // Fila de mensagens de comando (definida em main.cpp).
extern ZeGuia zeGuia;               // Instância global do robô (definida em main.cpp).

/**
 * @brief Task FreeRTOS responsável pelo controle do ZeGuia.
 *
 * Inicializa o robô e entra em loop contínuo, consumindo mensagens da fila
 * @c commandsQueue e executando o ciclo de controle a cada 10 ms.
 *
 * O fluxo por iteração é:
 * 1. Tenta receber uma mensagem da fila (sem bloqueio).
 * 2. Se houver mensagem, repassa ao robô via @c processarMensagem().
 * 3. Executa @c zeGuia.loop() independentemente de ter recebido mensagem.
 * 4. Aguarda 10 ms antes da próxima iteração.
 *
 * @param pvParameters Parâmetro padrão de tasks FreeRTOS (não utilizado).
 *
 * @note O @c vTaskDelete(NULL) ao final é inalcançável, mas mantido por
 *       boa prática para o caso de o @c while ser quebrado futuramente.
 */

void ControlsTask(void* pvParameters) {

    zeGuia.setup();

    while(true){
        RobotMessage message;

        // Polling sem bloqueio: não trava a task se a fila estiver vazia.
        if (xQueueReceive(commandsQueue, &message, 0) == pdTRUE) {
            zeGuia.processarMensagem(message);
        }

        zeGuia.loop();
        vTaskDelay(pdMS_TO_TICKS(10)); // Cede CPU e mantém período de 10 ms. 
    }

    vTaskDelete(NULL); // Segurança: encerra a task caso o loop seja interrompido.
}