#include "pid.h"

PID::PID(){}

PID::PID(float p, float i, float d): p_value(p), i_value(i), d_value(d), previousError(0), integral(0) {}

float PID::somatory(float setpoint, float mensuredValue){
    float error = setpoint - mensuredValue;
    integral += error;
    float derivative = error - previousError;
    previousError = error;
    return p_value*error + i_value*integral + d_value*derivative;
}

void PID::reset(){
    previousError = 0;
    integral = 0;
}