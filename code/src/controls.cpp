#include "globals.h"
#include "controls.h"
#include "motordriver.h"
#include "sensors.h"
#include "pid.h"
#include "commands.h"
#include "zeGuia.h"

extern QueueHandle_t commandsQueue;
extern ZeGuia zeGuia;

void _setup() {

    //setuping

    setup_qtr();

    setup_side_sensors();

    readCalibration();

    pinModeMotors();

    //Bateria
    pinMode(BATTERY_PIN, INPUT);

}

void ControlsTask(void* pvParameters) {

    zeGuia.setup();

    while(true){
        RobotMessage message;
        if (xQueueReceive(commandsQueue, &message, 0) == pdTRUE) {
            zeGuia.processarMensagem(message);
        }

        zeGuia.loop();
        vTaskDelay(pdMS_TO_TICKS(10));
    }

    vTaskDelete(NULL);

}