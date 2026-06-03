#include "commands.h"
#include "communication.h"
#include "zeGuia.h"
#include "controls.h"
#include "globals.h"
#include "motordriver.h"
#include "sensors.h"
#include <Arduino.h>
#include <Preferences.h>

extern QueueHandle_t commandsQueue;

void ZeGuia::setup() 
{
    _setup();
    
}

void ZeGuia::processarMensagem(const RobotMessage& message)
{
    if (message.hasPidTunings)
    {
        atualizarPID(message.vMax, message.kp, message.ki, message.kd,
                     message.novasMarcas);
    }

    if (message.command != CMD_NONE)
    {
        processarComando(message.command);
    }
}

void ZeGuia::atualizarPID(float novaVelMax, float novoKp, float novoKi, float novoKd, int novasMarcas)
{
    velMax            = novaVelMax;
    kp                = novoKp;
    ki                = novoKi;
    kd                = novoKd;
    this->novasMarcas = novasMarcas;
    aplicarParametrosPID();
    salvarParametrosNVS();
}

void ZeGuia::aplicarParametrosPID()
{
    VEL_MAX = static_cast<float>(velMax);
    pid.setTunnings(kp, ki, kd);  
}

static String nvsKey(const char* mAbbr, const char* sAbbr, const char* param)
{
    return String(mAbbr) + "_" + sAbbr + "_" + param;
}

void ZeGuia::salvarParametrosNVS()
{
    if (mode == MODE_NONE || strategy == S_NONE) return;
    const char* mAbbr = (mode == MODE_FOLLOWER) ? "sf" : "ps";
    const char* sAbbr = (strategy == S_CONSERVATIVE) ? "co" : "ar";

    Preferences prefs;
    prefs.begin("params", false);
    prefs.putInt  (nvsKey(mAbbr, sAbbr, "v").c_str(),   (int)velMax);
    prefs.putFloat(nvsKey(mAbbr, sAbbr, "kp").c_str(),  kp);
    prefs.putFloat(nvsKey(mAbbr, sAbbr, "ki").c_str(),  ki);
    prefs.putFloat(nvsKey(mAbbr, sAbbr, "kd").c_str(),  kd);
    prefs.putInt  (nvsKey(mAbbr, sAbbr, "mr").c_str(),  novasMarcas);
    prefs.end();
}

void ZeGuia::enviarTodosParametros()
{
    static const char* mAbbrs[]    = {"sf",          "sf",       "ps",           "ps"};
    static const char* sAbbrs[]    = {"co",          "ar",       "co",           "ar"};
    static const char* mNomes[]    = {"Seguidor",    "Seguidor",    "Perseguidor", "Perseguidor"};
    static const char* sNomes[]    = {"Conservador", "Arriscado",   "Conservador", "Arriscado"};

    Preferences prefs;
    prefs.begin("params", true);

    if (xSemaphoreTake(btMutex, pdMS_TO_TICKS(50)) == pdTRUE) {
        for (int i = 0; i < 4; i++)
        {
            int   v   = prefs.getInt  (nvsKey(mAbbrs[i], sAbbrs[i], "v").c_str(),  0);
            float pkp = prefs.getFloat(nvsKey(mAbbrs[i], sAbbrs[i], "kp").c_str(), 0.0f);
            float pki = prefs.getFloat(nvsKey(mAbbrs[i], sAbbrs[i], "ki").c_str(), 0.0f);
            float pkd = prefs.getFloat(nvsKey(mAbbrs[i], sAbbrs[i], "kd").c_str(), 0.0f);
            int   mr  = prefs.getInt  (nvsKey(mAbbrs[i], sAbbrs[i], "mr").c_str(), 0);
            SerialBT.printf("PARAMS:%s|%s|%d|%.2f|%.2f|%.2f|%d\n",
                mNomes[i], sNomes[i], v, pkp, pki, pkd, mr);
        }
        xSemaphoreGive(btMutex);
    }

    prefs.end();
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
        markCounter(novasMarcas);
    } 
    else if (strategy == S_RISK) 
    {
        lineWhite();
        markCounter(novasMarcas);
    }
}

void ZeGuia::loopPerseguidor()
{
    if (strategy == S_CONSERVATIVE) 
    { 
        lineBlack();
        markCounter(novasMarcas);
    } 
    else if (strategy == S_RISK) 
    {
        lineBlack();
        markCounter(novasMarcas);
    }
}
void ZeGuia::calibrarRobo()
{
    doCalibration();
    SerialBT.println("Estado: Calibrado");
}

void ZeGuia:: iniciarCorrida()
{
    // Reseta contadores de marcas para nova corrida
    rsOn   = 0;
    stateR = 0;
    pos_ant = 0;

    // Descarta mensagens antigas que possam estar acumuladas na fila
    RobotMessage stale;
    while (xQueueReceive(commandsQueue, &stale, 0) == pdTRUE) {}

    running = true;
    rsOn = 0;
}

void ZeGuia:: terminarCorrida()
{
    running = false;
    
    const uint32_t time_now = millis();
    if (time_now - lastStopMs >= TIME_BACK_STOP){
        controlMotors(-120, -120);
    }
    digitalWrite(AI1, HIGH);
    digitalWrite(AI2, HIGH);
    digitalWrite(BI1, HIGH);
    digitalWrite(BI2, HIGH);
    SerialBT.println("Estado: Parado");
    
      
}

void ZeGuia::enviarLeituraSensores()
{
    uint16_t position = qtr.readLineBlack(sensorValues);
    readRight = digitalRead(RightSensor);
    readLeft = digitalRead(LeftSensor);

    if (xSemaphoreTake(btMutex, pdMS_TO_TICKS(10)) == pdTRUE) {
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
        xSemaphoreGive(btMutex);
    }
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
    case CMD_GET_PARAMS:
        enviarTodosParametros();
        break;

    default:
        break;
    }   
}

void ZeGuia::lineWhite() {
    int pos = qtr.readLineWhite(sensorValues);

    bool lineLost = ((pos == 0) || (pos == 7000));

    static uint32_t timeStartDashed = 0;
    static bool inDashed = false;

    if (lineLost) {
        if (!inDashed) {
            timeStartDashed = millis();
            inDashed = true;
            SerialBT.println("Em tracejado!");
        }
            
        uint32_t tempo = millis() - timeStartDashed;
        if (tempo < 80) {
            SerialBT.println("Frentee!");
            controlMotors(0.7*VEL_MAX, 0.7*VEL_MAX);
            return; 
        } else {
            if (pos == 0)    controlMotors(-1.2*VEL_MAX, 1.2*VEL_MAX);
            if (pos == 7000) controlMotors(1.2*VEL_MAX, -1.2*VEL_MAX);
        }
        
    } else {
        inDashed = false;
    }

    pos_ant = pos; 
    
    int pid_value = pid.somatory(SETPOINT, pos);

    int vel_m1 = VEL_MAX - pid_value;
    int vel_m2 = VEL_MAX + pid_value;

    vel_m1 = constrain(vel_m1, -VEL_MAX, VEL_MAX);
    vel_m2 = constrain(vel_m2, -VEL_MAX, VEL_MAX);

    if (vel_m1 >= 0) vel_m1 = map(vel_m1, 0, VEL_MAX, VEL_MIN, VEL_MAX);
    else if (vel_m1 < 0) vel_m1 = map(vel_m1, -VEL_MAX, 0, -VEL_MAX, -VEL_MIN);

    controlMotors(vel_m1, vel_m2);
}

void ZeGuia::lineBlack(){
    int pos = qtr.readLineBlack(sensorValues);

    bool lineLost = ((pos == 0) || (pos == 7000));

    static uint32_t timeStartDashed = 0;
    static bool inDashed = false;

    if (lineLost) {
        if (!inDashed) {
            timeStartDashed = millis();
            inDashed = true;
            SerialBT.println("Em tracejado!");
        }
            
        uint32_t tempo = millis() - timeStartDashed;
        if (tempo < 80) {
            SerialBT.println("Frentee!");
            controlMotors(0.7*VEL_MAX, 0.7*VEL_MAX);
            return; 
        } else {
            if (pos == 0)    controlMotors(-1.2*VEL_MAX, 1.2*VEL_MAX);
            if (pos == 7000) controlMotors(1.2*VEL_MAX, -1.2*VEL_MAX);
        }
        
    } else {
        inDashed = false;
    }

    pos_ant = pos; 
    
    int pid_value = pid.somatory(SETPOINT, pos);

    int vel_m1 = VEL_MAX + pid_value;
    int vel_m2 = VEL_MAX - pid_value;

    vel_m1 = constrain(vel_m1, -VEL_MAX, VEL_MAX);
    vel_m2 = constrain(vel_m2, -VEL_MAX, VEL_MAX);

    if (vel_m1 >= 0) vel_m1 = map(vel_m1, 0, VEL_MAX, VEL_MIN, VEL_MAX);
    else if (vel_m1 < 0) vel_m1 = map(vel_m1, -VEL_MAX, 0, -VEL_MAX, -VEL_MIN);

    controlMotors(vel_m2, vel_m1);
}

void ZeGuia::markCounter(uint8_t n){
    if (n > 0){
        if (!digitalRead(RightSensor) && stateR == 0){
            rsOn++;
            stateR = 1;
           SerialBT.print("Contador de marcas: "); SerialBT.println(rsOn);
        } else if (digitalRead(RightSensor) && stateR == 1){
            stateR = 0;
        }
        if (rsOn == n){
            SerialBT.println("Parada Ativa Ativada!");
            processarComando(RobotCommand::CMD_STOP);
      }
    }
}

