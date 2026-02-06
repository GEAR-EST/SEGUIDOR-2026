#include <iostream>

class PID{
private:
    float p_value;
    float i_value;
    float d_value;
    float previousError;
    float integral;

public:

    PID(float p, float i, float d): p_value(p), i_value(i), d_value(d), previousError(0), integral(0) {}
    float somatory(float setpoint, float mensuredValue){
        float error = setpoint - mensuredValue;
        integral += error;
        float derivative = error - previousError;
        previousError = error;
        return p_value*error + i_value*integral + d_value*derivative;
    }
    void reset(){
        previousError = 0;
        integral = 0;
    }

};