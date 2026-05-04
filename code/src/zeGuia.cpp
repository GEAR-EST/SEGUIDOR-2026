#include "commands.h"
#include "communication.h"
#include "zeGuia.h"
#include "controls.h"
#include "globals.h"
#include "motordriver.h"
#include "sensors.h"
#include <Arduino.h>

int speedA = 0; int speedB = 0;

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

    if (sensorStreaming)
    {
        const uint32_t now = millis();
        if (now - lastSensorSendMs >= SENSOR_SEND_INTERVAL_MS)
        {
            lastSensorSendMs = now;
            enviarLeituraSensores();
        }
    }
}

void ZeGuia::loopSeguidor() //logica do seguidor, chamada dentro do loop principal quando o modo é MODE_FOLLOWER
{

    if (strategy == S_CONSERVATIVE) 
    {
        motors_calibrate();
    } 
    else if (strategy == S_RISK) 
    {
        controlMotors(0, -80);
    }

}

void ZeGuia::loopPerseguidor() //logica do perseguidor, chamada dentro do loop principal quando o modo é MODE_CHASE
{

    if (strategy == S_CONSERVATIVE) 
    { 
        controlMotors(80, 0);
    } 
    else if (strategy == S_RISK) 
    {
        controlMotors(-80, 0);
    }

}
void ZeGuia::calibrarRobo()
{
    SerialBT.println("Calibrando");
    doCalibration();
    SerialBT.println("Robo calibrado");
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

void ZeGuia::enviarLeituraSensores()
{
    uint16_t position = qtr.readLineBlack(sensorValues);
    readRight = digitalRead(RightSensor);
    readLeft = digitalRead(LeftSensor);

    // Formato para o app: S,<pos>,<s1>...<s8>,<right>,<left>
    SerialBT.print("S,");
    SerialBT.print(position);
    for (uint8_t i = 0; i < SensorCount; i++)
    {
        SerialBT.print(',');
        SerialBT.print(sensorValues[i]);
    }
    SerialBT.print(',');
    SerialBT.print(readRight);
    SerialBT.print(',');
    SerialBT.println(readLeft);
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
    case CMD_SENSOR_STREAM_ON:
        sensorStreaming = true;
        lastSensorSendMs = 0;
        break;
    case CMD_SENSOR_STREAM_OFF:
        sensorStreaming = false;
        break;

    default:
        break;
    }   
}