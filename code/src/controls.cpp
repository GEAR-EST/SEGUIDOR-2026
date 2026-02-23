#include "controls.h"
#include "commands.h"
#include <Arduino.h>

extern QueueHandle_t commandsQueue;

void ControlsTask(void* pvParameters) {
    // Configuração dos pinos de motores e sensores aqui (pinMode)
    
    for (;;) 
    {

        vTaskDelay(pdMS_TO_TICKS(10)); // para testes do bluetooth
        
        // Lógica do algoritmo de PID e controle de motores no Core 1
    }
}