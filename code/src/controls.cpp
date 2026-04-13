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

    /*
    Teste dos motores

    controlMotors(200, 200);
    vTaskDelay(pdMS_TO_TICKS(10000));
    controlMotors(0, 0);

    */
}

void _loop() {
    
    /*
    qtr_print();

    side_sensors_print();
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

void send_battery(){
    unsigned long current_time = millis();
    if (current_time - past_time >= bat_interval){
        past_time = current_time;
        //Pega essa função leandra
    }
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