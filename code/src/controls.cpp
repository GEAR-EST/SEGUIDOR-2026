#include "globals.h"
#include "controls.h"
#include "motordriver.h"
#include "sensors.h"
#include "pid.h"
#include "battery.h"

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

    /*
    Teste dos motores

    controlMotors(200, 200);
    vTaskDelay(pdMS_TO_TICKS(10000));
    controlMotors(0, 0);

    */

    pid.setTunnings(1, 0, 5);

    //Bateria
    analogSetAttenuation(ADC_11db); // Atenuação para 1.1 V
}

void _loop() {

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

    //Bateria

    int battery_read = analogRead(BATTERY_PIN);
    float vout = voutCalculation(battery_read);
    float percentage = percentageCalculation(vout);

}

void ControlsTask(void* pvParameters) {

    _setup();

    while(true){
        _loop();
        vTaskDelay(pdMS_TO_TICKS(10));
    }

    vTaskDelete(NULL);

}