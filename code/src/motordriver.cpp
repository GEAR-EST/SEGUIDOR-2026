#include "motordriver.h"
#include "sensors.h"

int VEL_MAX = 0; 
uint32_t past_fail = 0; 

L298NX2 motors(PWMA, AI1, AI2, PWMB, BI1, BI2);

void controlMotors(int speedA, int speedB){
    if (speedA > 0){
        motors.setSpeedA(speedA);
        motors.forwardA();
    } else if (speedA < 0){
        motors.setSpeedA(abs(speedA));
        motors.backwardA();
    } else {
        motors.setSpeedA(0);
        motors.stopA();
    }

    if (speedB > 0){
        motors.setSpeedB(speedB);
        motors.forwardB();
    } else if (speedB < 0){
        motors.setSpeedB(abs(speedB));
        motors.backwardB();
    } else {
        motors.setSpeedB(0);
        motors.stopB();
    }

}

void pinModeMotors(){
    analogWriteFrequency(20000);
    pinMode(PWMA, OUTPUT); pinMode(PWMB, OUTPUT);
    pinMode(AI1, OUTPUT); pinMode(AI2, OUTPUT);
    pinMode(BI1, OUTPUT); pinMode(BI2, OUTPUT);
    pinMode(STBY, OUTPUT);

    digitalWrite(STBY, HIGH);
}

bool fail_safe(){
    const uint32_t current_time = millis();
    while (qtr.readLineWhite(sensorValues) == 0){
        if (current_time - past_fail >= failtime) return true;
    }
    return false;
}