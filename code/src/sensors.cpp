#include "globals.h"
#include "sensors.h"
#include "controls.h"
#include "motordriver.h"
#include "communication.h"

QTRSensors qtr;
Preferences preferences;
uint16_t sensorValues[SensorCount];
uint16_t readRight = 0;
uint16_t readLeft = 0;

void setup_qtr(){
    // Configuração do QTR-8A
    qtr.setTypeAnalog(); // Tipo de sensor é analógico

    // qtr.setSensorPins((const uint8_t[]){14, 27, 26, 25, 33, 32, 35, 34}, SensorCount);
    qtr.setSensorPins((const uint8_t[]){D8_PIN, D7_PIN, D6_PIN, D5_PIN, D4_PIN, D3_PIN, D2_PIN, D1_PIN}, SensorCount);

    pinMode(LED_BUILTIN, OUTPUT);

}

void setup_side_sensors(){
    // Configuração dos sensores laterais
    pinMode(RightSensor, INPUT);
    pinMode(LeftSensor, INPUT);
}

void side_sensors_print(){
    // Sensores laterais
    readRight = digitalRead(RightSensor);
    readLeft = digitalRead(LeftSensor);
    SerialBT.print("Direito: "); SerialBT.print(readRight);
    SerialBT.print('\t');
    SerialBT.print("Esquerdo: "); SerialBT.println(readLeft);
}

void doCalibration(){

    vTaskDelay(pdMS_TO_TICKS(50));

    // Calibrando
    
    for (uint16_t i=0; i < 200; i++){
        qtr.calibrate();
    }

    // Para depuração, os valores máximo e mínimos na calibração
    uint16_t max_values[8];
    uint16_t min_values[8];

    for (uint8_t i=0; i < 8; i++){
        max_values[i] = EMPTY_VALUE;
        min_values[i] = EMPTY_VALUE;
    }

    SerialBT.println("Maximum values");
    for(uint16_t i=0; i < SensorCount; i++){
        SerialBT.print(qtr.calibrationOn.maximum[i]);
        SerialBT.print(' ');
        max_values[i] = qtr.calibrationOn.maximum[i];
    }

    SerialBT.println("Minimum values");
    for(uint16_t i=0; i < SensorCount; i++){
        SerialBT.print(qtr.calibrationOn.minimum[i]);
        SerialBT.print(' ');
        min_values[i] = qtr.calibrationOn.minimum[i];
    }

    size_t max_values_bytes = sizeof(max_values);
    size_t min_values_bytes = sizeof(min_values);

    preferences.begin("calib", false);
    preferences.putBytes("max_val", max_values, max_values_bytes);
    preferences.putBytes("min_val", min_values, min_values_bytes);
    preferences.end();

    digitalWrite(LED_BUILTIN, LOW);
    SerialBT.println("Calibração terminou :p");
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
            SerialBT.println("Erro: um valor vazio foi encontrado, por favor calibrar novamente");
            return false;
        }
    }

    for (auto n : read_min){
        if (n == EMPTY_VALUE) {
            SerialBT.println("Erro: um valor vazio foi encontrado, por favor calibrar novamente");
            return false;
        }
    }

    qtr.calibrate();
    for(uint8_t i = 0; i < SensorCount; i++){
        qtr.calibrationOn.maximum[i] = read_max[i];
        qtr.calibrationOn.minimum[i] = read_min[i];
    }

    SerialBT.println("Calibração carregada com sucesso");

    return true;
  
}