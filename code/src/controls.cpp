#include "controls.h"
#include "motordriver.h"
#include "sensors.h"
#include "pid.h"

uint16_t readRight;
uint16_t readLeft;
uint16_t sensorValues[SensorCount];

PID pid;

void _setup() {

    Serial.begin(9600);

    //configuração do QTR-8RC
    qtr.setTypeRC(); //tipo de sensor é RC pq é o QTR-8RC duuuuh
    qtr.setSensorPins((const uint8_t[]){3, 4, 5, 6, 7, 8, 9, 10}, SensorCount);
    qtr.setEmitterPin(2);

    pinMode(LED_BUILTIN, OUTPUT);

    //verificar se já há valores calibrados
    if (readCalibration() == false) doCalibration();

    //configuração dos sensores laterais
    pinMode(RightSensor, INPUT);
    pinMode(LeftSensor, INPUT);

    //motor driver pin mode

    pinMode(PWMA, OUTPUT); pinMode(PWMB, OUTPUT);
    pinMode(A11, OUTPUT); pinMode(A12, OUTPUT);
    pinMode(B11, OUTPUT); pinMode(B12, OUTPUT);
    pinMode(STBY, OUTPUT);

}

void _loop() {

    uint16_t pos = qtr.readLineBlack(sensorValues);

    float output = pid.somatory(3500, pos);

    

    //0 = máxima reflectância e 1000 = mínima reflectância
    for (uint8_t i = 0; i < SensorCount; i++){
        Serial.print(sensorValues[i]);
        Serial.print('\t');
    }
    Serial.println(pos);

    //Sensores laterais
    readRight = digitalRead(RightSensor);
    readLeft = digitalRead(LeftSensor);
    Serial.print("Direito: "); Serial.println(readRight);
    Serial.print("Esquerdo: "); Serial.println(readLeft);

}

void ControlsTask(void* pvParameters) {

    _setup();

    while(true){
        _loop();
        vTaskDelay(pdMS_TO_TICKS(10));
    }

    vTaskDelete(NULL);

}