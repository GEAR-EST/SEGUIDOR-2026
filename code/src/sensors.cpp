#include "sensors.h"
#include "controls.h"

QTRSensors qtr;
Preferences preferences;

uint16_t readRight;
uint16_t readLeft;
uint16_t sensorValues[SensorCount];

void doCalibration(){

    digitalWrite(LED_BUILTIN, HIGH);

    //calibrando
    for (uint16_t i=0; i < 400; i++){
        qtr.calibrate();
        vTaskDelay(pdMS_TO_TICKS(1));
    }
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

    preferences.begin("calibration_values", false);
    preferences.putBytes("max_values_array", max_values, max_values_bytes);
    preferences.putBytes("min_values_array", min_values, min_values_bytes);
    preferences.end();
}

bool readCalibration(){

    uint16_t read_max[8];
    uint16_t read_min[8];

    preferences.begin("calibration_values", true);
    size_t read_size_max = preferences.getBytesLength("max_values_array");
    size_t read_size_min = preferences.getBytesLength("min_values_array");
    
    if(read_size_max == sizeof(read_max)) preferences.getBytes("max_values_array", read_max, sizeof(read_max));
    if(read_size_min == sizeof(read_min)) preferences.getBytes("min_values_array", read_min, sizeof(read_min));

    preferences.end();

    for (auto n : read_max){
        if (n == EMPTY_VALUE) return false;
    }

    return true;
  
}