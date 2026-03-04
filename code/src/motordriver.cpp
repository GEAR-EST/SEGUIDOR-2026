#include "globals.h"
#include "motordriver.h"
#include "pid.h"
#include "sensors.h"

int VEL_MAX = 0;
int VEL_MIN = 0;

L298NX2 motors(PWMA, AI1, AI2, PWMB, BI1, BI2);
PID pid(0, 0, 0);

void controlMotors(int speedA, int speedB){
    if (speedA > 0){
        motors.setSpeedA(speedA);
        motors.forward();
    } else if (speedA < 0){
        motors.setSpeedA(abs(speedA));
        motors.backward();
    } else {
        motors.setSpeed(0);
        motors.stop();
    }

    if (speedB > 0){
        motors.setSpeedB(speedB);
        motors.forward();
    } else if (speedA < 0){
        motors.setSpeedB(abs(speedB));
        motors.backward();
    } else {
        motors.setSpeed(0);
        motors.stop();
    }

}

void lineBlack(){
    int pos = qtr.readLineBlack(sensorValues);
    int pid_value = pid.somatory(SETPOINT, pos);

    int vel_m1 = VEL_MAX + pid_value;
    int vel_m2 = VEL_MAX - pid_value;

    controlMotors(vel_m1, vel_m2);

}

void lineWhite(){
    int pos = qtr.readLineWhite(sensorValues);
    int pid_value = pid.somatory(SETPOINT, pos);

    int vel_m1 = VEL_MAX + pid_value;
    int vel_m2 = VEL_MAX - pid_value;

    vel_m1 = constrain(vel_m1, -VEL_MIN, VEL_MAX);
    vel_m2 = constrain(vel_m2, -VEL_MIN, VEL_MAX);

    controlMotors(vel_m1, vel_m2);

}


