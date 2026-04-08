#include "globals.h"
#include "controls.h"
#include "motordriver.h"
#include "sensors.h"
#include "pid.h"
#include "commands.h"
#include "zeGuia.h"

extern QueueHandle_t commandsQueue;
extern ZeGuia zeGuia;

uint16_t readRight = 0;
uint16_t readLeft = 0;
uint16_t sensorValues[SensorCount];


void _setup() {

    //configuração do QTR-8A
    qtr.setTypeAnalog(); //tipo de sensor é analogico

    // qtr.setSensorPins((const uint8_t[]){14, 27, 26, 25, 33, 32, 35, 34}, SensorCount);
    qtr.setSensorPins((const uint8_t[]){D8_PIN, D7_PIN, D6_PIN, D5_PIN, D4_PIN, D3_PIN, D2_PIN, D1_PIN}, SensorCount);

    //verificar se já há valores calibrados
    //if (readCalibration() == false)  

    //configuração dos sensores laterais
    pinMode(RightSensor, INPUT);
    pinMode(LeftSensor, INPUT);

    //motor driver pin mode
    doCalibration();

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
    uint16_t position = qtr.readLineBlack(sensorValues);

    //Sensor frontal
    Serial.print("Frontal: ");
    for (uint8_t i = 0; i < SensorCount; i++) {
        Serial.print(sensorValues[i]);
        Serial.print('\t');
    }
    Serial.println();
    Serial.print("Pos: ");
    Serial.println(position);

    lineBlack();

    //Sensores laterais
    readRight = digitalRead(RightSensor);
    readLeft = digitalRead(LeftSensor);
    Serial.print("Direito: "); Serial.print(readRight);
    Serial.print('\t');
    Serial.print("Esquerdo: "); Serial.println(readLeft);

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