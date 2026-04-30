#include "globals.h"
#include "sensors.h"
#include "controls.h"
#include "motordriver.h"

QTRSensors qtr;
Preferences preferences;
uint16_t sensorValues[SensorCount];
uint16_t readRight = 0;
uint16_t readLeft = 0;

void setup_qtr(){
    //configuração do QTR-8A
    qtr.setTypeAnalog(); //tipo de sensor é analogico

    // qtr.setSensorPins((const uint8_t[]){14, 27, 26, 25, 33, 32, 35, 34}, SensorCount);
    qtr.setSensorPins((const uint8_t[]){D8_PIN, D7_PIN, D6_PIN, D5_PIN, D4_PIN, D3_PIN, D2_PIN, D1_PIN}, SensorCount);

    pinMode(LED_BUILTIN, OUTPUT);

}

void setup_side_sensors(){
    //configuração dos sensores laterais
    pinMode(RightSensor, INPUT);
    pinMode(LeftSensor, INPUT);
}

void qtr_print(){
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
}

void side_sensors_print(){
    //Sensores laterais
    readRight = digitalRead(RightSensor);
    readLeft = digitalRead(LeftSensor);
    Serial.print("Direito: "); Serial.print(readRight);
    Serial.print('\t');
    Serial.print("Esquerdo: "); Serial.println(readLeft);
}

void doCalibration(){

    digitalWrite(LED_BUILTIN, HIGH);
    vTaskDelay(pdMS_TO_TICKS(50));

    Serial.println("Calibrando");
    //calibrando
    motors_calibrate();
    for (uint16_t i=0; i < 3000; i++){
        qtr.calibrate();
    }
    motors.stop();

    //para depuração, os valores máximo e mínimos na calibração
    uint16_t max_values[8];
    uint16_t min_values[8];

    for (uint8_t i=0; i < 8; i++){
        max_values[i] = EMPTY_VALUE;
        min_values[i] = EMPTY_VALUE;
    }

    Serial.println("Maximum values");
    for(uint16_t i=0; i < SensorCount; i++){
        Serial.print(qtr.calibrationOn.maximum[i]);
        Serial.print(' ');
        max_values[i] = qtr.calibrationOn.maximum[i];
    }

    Serial.println("Minimum values");
    for(uint16_t i=0; i < SensorCount; i++){
        Serial.print(qtr.calibrationOn.minimum[i]);
        Serial.print(' ');
        min_values[i] = qtr.calibrationOn.minimum[i];
    }

    size_t max_values_bytes = sizeof(max_values);
    size_t min_values_bytes = sizeof(min_values);

    preferences.begin("calib", false);
    preferences.putBytes("max_val", max_values, max_values_bytes);
    preferences.putBytes("min_val", min_values, min_values_bytes);
    preferences.end();

    digitalWrite(LED_BUILTIN, LOW);
    Serial.println("Calibração terminou :p");
}

bool readCalibration(){

    uint16_t read_max[SensorCount];
    uint16_t read_min[SensorCount];

    preferences.begin("calib", true);
    
    size_t resMax = preferences.getBytes("max_val", read_max, sizeof(read_max));
    size_t resMin = preferences.getBytes("min_val", read_min, sizeof(read_min));

    preferences.end();

    if (resMax != sizeof(read_max) || resMin != sizeof(read_min)) return false;
    
    for (auto n : read_max){
        if (n == EMPTY_VALUE) {
            Serial.println("Erro: um valor vazio foi encontrado, por favor calibrar novamente");
            return false;
        }
    }

    for (auto n : read_min){
        if (n == EMPTY_VALUE) {
            Serial.println("Erro: um valor vazio foi encontrado, por favor calibrar novamente");
            return false;
        }
    }

    qtr.calibrate();
    for(uint8_t i = 0; i < SensorCount; i++){
        qtr.calibrationOn.maximum[i] = read_max[i];
        qtr.calibrationOn.minimum[i] = read_min[i];
    }

    Serial.println("Calibração carregada com sucesso");

    return true;
  
}