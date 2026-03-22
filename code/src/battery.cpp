#include "battery.h"

float r2 = 10000;
float r3 = 4700;
const float vin = 7.4;
float divisionFactor = (r2+r3)/r3; //Invertido porque eu quero saber o valor da bateria a partir da leitura do pino
float full_value = 8.4;
float empty_value = 6.4;

float voutCalculation(int battery_read){
    return (battery_read/4095)*3.3*divisionFactor;
}

float percentageCalculation(float vout){
    return (vout - vin)/(full_value - empty_value) * 100;
}