#include "globals.h"
#include "motordriver.h"
#include "pid.h"
#include "sensors.h"
#include "communication.h"

int VEL_MAX = 0;
int VEL_MAX_BACK = 0;
unsigned long past_fail = 0; 

L298NX2 motors(PWMA, AI1, AI2, PWMB, BI1, BI2);
PID pid(0, 0, 0);

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

void lineBlack(){
    int pos = qtr.readLineBlack(sensorValues);
    int pid_value = pid.somatory(SETPOINT, pos);

    if (pos == 0){
        if (fail_safe() == true){
            controlMotors(0, 0);
            digitalWrite(STBY, LOW);
        }
    }

    int vel_m1 = VEL_MAX + pid_value;
    int vel_m2 = VEL_MAX - pid_value;

    vel_m1 = constrain(vel_m1, -220, VEL_MAX);
    vel_m2 = constrain(vel_m2, -220, VEL_MAX);

    controlMotors(vel_m1, vel_m2);

}

void lineWhite(){
    int pos = qtr.readLineWhite(sensorValues);

    if (pos == 0){
        if (fail_safe() == true){
            // turbina e tals
            controlMotors(0, 0);
            digitalWrite(STBY, LOW);
            SerialBT.println("FAIL SAFE FOI ATIVADO!!!!!");
        }
    }
    int pid_value = pid.somatory(SETPOINT, pos);

    int vel_m1 = VEL_MAX + pid_value;
    int vel_m2 = VEL_MAX - pid_value;

    vel_m1 = constrain(vel_m1, -VEL_MAX_BACK, VEL_MAX);
    vel_m2 = constrain(vel_m2, -VEL_MAX_BACK, VEL_MAX);

    controlMotors(vel_m1, vel_m2);

}

void pinModeMotors(){
    pinMode(PWMA, OUTPUT); pinMode(PWMB, OUTPUT);
    pinMode(AI1, OUTPUT); pinMode(AI2, OUTPUT);
    pinMode(BI1, OUTPUT); pinMode(BI2, OUTPUT);
    pinMode(STBY, OUTPUT);
}

void motors_calibrate(){
    motors.setSpeed(120);
    motors.forwardA();
    motors.backwardB();
}

bool fail_safe(){
    unsigned long current_time = millis();
    while (qtr.readLineWhite(sensorValues) == 0){
        if (current_time - past_fail >= failtime) return true;
    }
    return false;
}