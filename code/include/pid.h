#pragma once

class PID{
private:
    float p_value;
    float i_value;
    float d_value;
    float previousError;
    float integral;

public:

    PID();
    PID(float p, float i, float d);
    float somatory(float setpoint, float mensuredValue);
    void reset();
    void setTunnings(float _p, float _i, float _d);

};
