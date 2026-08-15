/**
 * @file motordriver.cpp
 * @brief Controle de baixo nível dos motores via ponte TB6612FNG.
 *
 * Abstrai a direção e velocidade dos dois motores (A e B), e provê
 * o fail-safe de linha perdida.
 */

#include "motordriver.h"
#include "sensors.h"

int      VEL_MAX   = 0; ///< Velocidade máxima atual (atualizada via PID tuning).
uint32_t past_fail = 0; ///< Timestamp da última verificação de fail-safe (ms).

L298NX2 motors(PWMA, AI1, AI2, PWMB, BI1, BI2); ///< Instância da ponte H L298N dual.

/**
 * @brief Define a velocidade e direção de ambos os motores.
 *
 * Valores positivos movem o motor para frente, negativos para trás e
 * zero aplica freio ativo. O valor absoluto é usado como velocidade.
 *
 * @param speedA Velocidade do motor A (negativo = ré).
 * @param speedB Velocidade do motor B (negativo = ré).
 */
void controlMotors(int speedA, int speedB) {
    if      (speedA > 0) { motors.setSpeedA(speedA);       motors.forwardA();  }
    else if (speedA < 0) { motors.setSpeedA(abs(speedA));  motors.backwardA(); }
    else                 { motors.setSpeedA(0);             motors.stopA();     }

    if      (speedB > 0) { motors.setSpeedB(speedB);       motors.forwardB();  }
    else if (speedB < 0) { motors.setSpeedB(abs(speedB));  motors.backwardB(); }
    else                 { motors.setSpeedB(0);             motors.stopB();     }
}

/**
 * @brief Configura os pinos da ponte H e habilita o standby.
 *
 * Define PWM a 20 kHz (acima da faixa audível) e coloca o pino
 * @c STBY em HIGH para habilitar a ponte H.
 */
void pinModeMotors() {
    analogWriteFrequency(20000);
    pinMode(PWMA, OUTPUT); pinMode(PWMB, OUTPUT);
    pinMode(AI1,  OUTPUT); pinMode(AI2,  OUTPUT);
    pinMode(BI1,  OUTPUT); pinMode(BI2,  OUTPUT);
    pinMode(STBY, OUTPUT);
    digitalWrite(STBY, HIGH);
}

/**
 * @brief Verifica se o robô perdeu a linha por tempo superior ao limite.
 *
 * Lê @c readLineWhite() em loop e compara o tempo decorrido desde
 * @c past_fail com @c failtime. Se exceder, sinaliza falha para que a
 * camada superior decida (parar, recuar etc.).
 *
 * @warning @c current_time é capturado antes do @c while; se a leitura
 *          demorar muito, o tempo efetivo pode ultrapassar @c failtime
 *          antes da primeira comparação.
 *
 * @return @c true  se a linha ficou perdida por mais de @c failtime ms.
 * @return @c false se a linha foi reencontrada dentro do prazo.
 */
bool fail_safe() {
    const uint32_t current_time = millis();
    while (qtr.readLineWhite(sensorValues) == 0) {
        if (current_time - past_fail >= failtime) return true;
    }
    return false;
}

