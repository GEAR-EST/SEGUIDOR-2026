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
        atualizarPID(message.vMax, message.kp, message.ki, message.kd,
                     message.velEsq, message.velDir, message.novasMarcas);
    }

    if (message.command != CMD_NONE)
    {
        processarComando(message.command);
    }
}

void ZeGuia::atualizarPID(float novaVelMax, float novoKp, float novoKi, float novoKd, int novoVelEsq, int novoVelDir, int novasMarcas)
{
    velMax     = novaVelMax;
    kp         = novoKp;
    ki         = novoKi;
    kd         = novoKd;
    velEsq     = novoVelEsq;
    velDir     = novoVelDir;
    this->novasMarcas = novasMarcas;
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
        lineWhite();
    } 
    else if (strategy == S_RISK) 
    {
        lineWhite();
    }
}

void ZeGuia::loopPerseguidor()
{
    if (strategy == S_CONSERVATIVE) 
    { 
        controlMotors(velEsq, velDir);
    } 
    else if (strategy == S_RISK) 
    {
        controlMotors(velEsq, velDir);
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
}

void ZeGuia:: terminarCorrida()
{
    running = false;
    if (strategy == S_CONSERVATIVE){
        const uint32_t time_now = millis();
        if (time_now - lastStopMs >= TIME_BACK_STOP){
            controlMotors(-120, -120);
        }
        digitalWrite(AI1, HIGH);
        digitalWrite(AI2, HIGH);
        digitalWrite(BI1, HIGH);
        digitalWrite(BI2, HIGH);
        SerialBT.println("PARADA ATIVA ATIVADAAAAAAA");
    } else if (strategy == S_RISK) {
        digitalWrite(AI1, LOW);
        digitalWrite(AI2, LOW);
        digitalWrite(BI1, LOW);
        digitalWrite(BI2, LOW);
        SerialBT.println("PARADA PASSIVA ATIVADAAAAAAA");
    }
    // digitalWrite(STBY, LOW);
    //logica terminar corrida 
}

void ZeGuia::enviarLeituraSensores()
{
    uint16_t position = qtr.readLineWhite(sensorValues);
    readRight = digitalRead(RightSensor);
    readLeft = digitalRead(LeftSensor);

    // Formato para o app: S,<pos>,<s1>...<s8>,<right>,<left>
    SerialBT.print("S,");
    SerialBT.print(position);
    for (uint8_t i = 0; i < SensorCount; i++)
    {
        SerialBT.print(',');
        SerialBT.print(1000 - sensorValues[i]);
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

void ZeGuia::lineWhite(){
    int pos = qtr.readLineWhite(sensorValues);
    /*
    if (pos == 0 || pos == 7000){
        if (fail_safe() == true){
            terminarCorrida();
            SerialBT.println("FAIL SAFE FOI ATIVADO!!!!!");
        }
    }
    */
   
    int pid_value = pid.somatory(SETPOINT, pos);

    int vel_m1 = VEL_MAX - pid_value;
    int vel_m2 = VEL_MAX + pid_value;

    vel_m1 = constrain(vel_m1, -VEL_MAX, VEL_MAX);
    vel_m2 = constrain(vel_m2, -VEL_MAX, VEL_MAX);

    controlMotors(vel_m1, vel_m2);

}