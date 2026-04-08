#include "commands.h"
#include "communication.h"
#include "zeGuia.h"
#include "controls.h"
#include "globals.h"
#include "motordriver.h"
#include "sensors.h"
#include <Arduino.h>

void ZeGuia::setup() 
{
    _setup();
}

void ZeGuia::processarMensagem(const RobotMessage& message)
{
    if (message.hasPidTunings)
    {
        atualizarPID(message.vMax, message.kp, message.ki, message.kd);
    }

    if (message.command != CMD_NONE)
    {
        processarComando(message.command);
    }
}

void ZeGuia::atualizarPID(float novaVelMax, float novoKp, float novoKi, float novoKd)
{
    velMax = novaVelMax;
    kp = novoKp;
    ki = novoKi;
    kd = novoKd;
    aplicarParametrosPID();
}

void ZeGuia::aplicarParametrosPID()
{
    VEL_MAX = static_cast<float>(velMax);
    pid.setTunnings(kp, ki, kd);
}

void ZeGuia::loop() 
{
    // Lógica principal do robô, chamada repetidamente
    if (running) 
    {
        switch (mode) 
        {
            case MODE_FOLLOWER:
                loopSeguidor();
                break;
            case MODE_CHASE:
                loopPerseguidor();
                break;
            default:
                break;
        }
    }
}

void ZeGuia::loopSeguidor() //logica do seguidor, chamada dentro do loop principal quando o modo é MODE_FOLLOWER
{

    if (strategy == S_CONSERVATIVE) 
    {
        // Lógica para estratégia conservadora do seguidor  

    } 
    else if (strategy == S_RISK) 
    {
        // Lógica para estratégia arriscada do seguidor
    }

}

void ZeGuia::loopPerseguidor() //logica do perseguidor, chamada dentro do loop principal quando o modo é MODE_CHASE
{

    if (strategy == S_CONSERVATIVE) 
    {
        pid.setTunnings(0.5, 1, 10);
        lineWhite();  

    } 
    else if (strategy == S_RISK) 
    {
        pid.setTunnings(1, 1, 5);
        lineWhite(); 
    }

}
void ZeGuia::calibrarRobo()
{
    doCalibration();
}

void ZeGuia:: iniciarCorrida()
{
    //logica iniciar corrida
    running = true;
    digitalWrite(STBY, HIGH);
}

void ZeGuia:: terminarCorrida()
{
    running = false;
    controlMotors(0, 0);
    digitalWrite(STBY, LOW);
    //logica terminar corrida 
}


void ZeGuia::processarComando(RobotCommand cmd)
{
    switch (cmd)
    {
    case CMD_CALIBRATE:
        calibrarRobo();
        break;
    case CMD_START:
        iniciarCorrida();
        break;
    case CMD_STOP:
        terminarCorrida();
        break;
    case CMD_MODE_FOLLOWER:
        mode = MODE_FOLLOWER;
        break;
    case CMD_MODE_CHASE:
        mode = MODE_CHASE;
        break;
    case CMD_STRATEGY_CONSERVATIVE:
        strategy = S_CONSERVATIVE;
        break;
    case CMD_STRATEGY_RISK:
        strategy = S_RISK;
        break;

    default:
        break;
    }   
}