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

    //Bateria

    int percentage = map(analogRead(BATTERY_PIN), 2539, 3325, 0, 100);
    Serial.print("Bateria: ");
    Serial.println(percentage);
    Serial.print("Analog read: ");
    Serial.println(analogRead(BATTERY_PIN));
    delay(3000);
}

void ControlsTask(void* pvParameters) {

    _setup();

    while(true){
        _loop();
        vTaskDelay(pdMS_TO_TICKS(10));
    }

    vTaskDelete(NULL);

}