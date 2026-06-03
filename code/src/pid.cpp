// Implementação do controlador PID utilizado no ZeGuia.
// Calcula a saída de controle com base no erro entre setpoint e valor medido, 
// acumulando o termo integral e derivando o erro a cada chamada.

#include "pid.h"

// Método construtor padrão
PID::PID(){}

// Método construtor que inicializa os atributos e zera o estado interno
PID::PID(float p, float i, float d): p_value(p), i_value(i), d_value(d), previousError(0), integral(0) {}

// Método que retorna o valor PID dado o setpoing e o valor mensurável (medido)
float PID::somatory(float setpoint, float mensuredValue){
    float error = setpoint - mensuredValue;
    integral += error;
    float derivative = error - previousError;
    previousError = error;
    return p_value*error + i_value*integral + d_value*derivative;
}

// Método que zera o estado interno e o atributo integral
void PID::reset(){
    previousError = 0;
    integral = 0;
}

// Método que muda os valores das constantes PID
void PID::setTunnings(float _p, float _i, float _d){
    p_value = _p;
    i_value = _i;
    d_value = _d;
}