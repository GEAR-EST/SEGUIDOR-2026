#include "globals.h"
#include "controls.h"
#include "motordriver.h"
#include "sensors.h"
#include "pid.h"

uint16_t readRight = 0;
uint16_t readLeft = 0;
uint16_t sensorValues[SensorCount];

void _setup() {

    //configuração do QTR-8A
    qtr.setTypeAnalog(); //tipo de sensor é analogico
    // qtr.setSensorPins((const uint8_t[]){14, 27, 26, 25, 33, 32, 35, 34}, SensorCount);
    qtr.setSensorPins((const uint8_t[]){D8_PIN, D7_PIN, D6_PIN, D5_PIN, D4_PIN, D3_PIN, D2_PIN, D1_PIN}, SensorCount);

    pinMode(LED_BUILTIN, OUTPUT);

    //verificar se já há valores calibrados
    //if (readCalibration() == false) 
    doCalibration();

    //configuração dos sensores laterais
    pinMode(RightSensor, INPUT);
    pinMode(LeftSensor, INPUT);

    //motor driver pin mode

    pinMode(PWMA, OUTPUT); pinMode(PWMB, OUTPUT);
    pinMode(AI1, OUTPUT); pinMode(AI2, OUTPUT);
    pinMode(BI1, OUTPUT); pinMode(BI2, OUTPUT);
    pinMode(STBY, OUTPUT);

}

void _loop() {

    uint16_t position = qtr.readLineBlack(sensorValues);

    Serial.print("frente:");
    for (uint8_t i = 0; i < SensorCount; i++) {
        Serial.print(sensorValues[i]);
        Serial.print('\t');
    }
    Serial.println();
    Serial.print("pos:");
    Serial.println(position);

    //Sensores laterais
    readRight = digitalRead(RightSensor);
    readLeft = digitalRead(LeftSensor);
    Serial.print("Direito: "); Serial.println(readRight);
    Serial.println();
    Serial.print("Esquerdo: "); Serial.println(readLeft);

    vTaskDelay(pdMS_TO_TICKS(2000));

}

void ControlsTask(void* pvParameters) {

    _setup();

    while(true){
        _loop();
        vTaskDelay(pdMS_TO_TICKS(10));
    }

    vTaskDelete(NULL);

}