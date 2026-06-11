/**
 * @file pid.cpp
 * @brief Implementação do controlador PID do ZeGuia.
 *
 * Calcula a saída de controle com base no erro entre setpoint e valor medido,
 * acumulando o termo integral e derivando o erro a cada chamada.
 */

#include "pid.h"

/**
 * @brief Construtor padrão. Atributos ficam não inicializados.
 * @see PID(float, float, float)
 */
PID::PID() {}

/**
 * @brief Construtor que inicializa as constantes e zera o estado interno.
 *
 * @param p Ganho proporcional (Kp).
 * @param i Ganho integral (Ki).
 * @param d Ganho derivativo (Kd).
 */
PID::PID(float p, float i, float d)
    : p_value(p), i_value(i), d_value(d), previousError(0), integral(0) {}

/**
 * @brief Calcula a saída do controlador PID para uma iteração.
 *
 * Computa os três termos (P, I, D) com base no erro atual e retorna
 * a soma ponderada pelas constantes configuradas. Atualiza o estado
 * interno (@c integral e @c previousError) a cada chamada.
 *
 * @param setpoint      Valor desejado (referência).
 * @param mensuredValue Valor atual medido pelo sensor.
 * @return Saída de controle u(t) = Kp·e + Ki·∫e + Kd·Δe.
 */
float PID::somatory(float setpoint, float mensuredValue) {
    float error      = setpoint - mensuredValue;
    integral        += error;
    float derivative = error - previousError;
    previousError    = error;
    return p_value * error + i_value * integral + d_value * derivative;
}

/**
 * @brief Zera o estado interno do controlador.
 *
 * Deve ser chamado ao retomar o controle após uma pausa para evitar
 * acúmulo indevido no termo integral (*integral windup*).
 */
void PID::reset() {
    previousError = 0;
    integral      = 0;
}

/**
 * @brief Atualiza as constantes do controlador em tempo de execução.
 *
 * Permite ajuste fino (*tuning*) sem reiniciar o estado interno.
 *
 * @param _p Novo ganho proporcional (Kp).
 * @param _i Novo ganho integral (Ki).
 * @param _d Novo ganho derivativo (Kd).
 */
void PID::setTunnings(float _p, float _i, float _d) {
    p_value = _p;
    i_value = _i;
    d_value = _d;
}