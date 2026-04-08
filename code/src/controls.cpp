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

    pinModeMotors();
    digitalWrite(STBY, HIGH);

    //Bateria
    pinMode(BATTERY_PIN, INPUT);

    /*
    Teste dos motores

    controlMotors(200, 200);
    vTaskDelay(pdMS_TO_TICKS(10000));
    controlMotors(0, 0);

    */

    pid.setTunnings(1, 0, 5);

}

void _loop() {
    
    /*
    qtr_print();

    lineBlack();

    side_sensors_print();
    
    vTaskDelay(pdMS_TO_TICKS(2000));
    */

}

int battery_percentage(){
    long sum = 0;
    for (int i = 0; i < 16; i++){
        sum += analogRead(BATTERY_PIN);
    }
    int avg = sum/16;
    return map(avg, 2539, 3325, 0, 100);
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