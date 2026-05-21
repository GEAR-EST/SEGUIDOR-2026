#pragma once
#include "commands.h"

class ZeGuia
{

public:

    enum RobotMode 
    { MODE_NONE, 
      MODE_FOLLOWER, 
      MODE_CHASE 
    };

    enum RobotStrategy 
    { S_NONE, 
      S_CONSERVATIVE, 
      S_RISK 
    };

    ZeGuia() : 
    mode(MODE_NONE), 
    strategy(S_NONE), 
    running(false),
    sensorStreaming(false),
    lastSensorSendMs(0),
    velMax(0),
    kp(0.0),
    ki(0.0),
    kd(0.0),
    novasMarcas(0)
    {}

    void setup();
    void loop();
    void processarMensagem(const RobotMessage& message);
    void processarComando(RobotCommand cmd);

private:
    RobotMode mode;
    RobotStrategy strategy;
    bool running;
    bool sensorStreaming;
    uint32_t lastSensorSendMs;
    uint32_t lastStopMs;
    static const uint32_t SENSOR_SEND_INTERVAL_MS = 120;
    static const uint32_t TIME_BACK_STOP = 500;
    static const uint8_t VEL_MIN = 70;
    int pos_ant = 0;
    
    unsigned long tempoPerda = 0;
    bool emGap = false;
    const unsigned long GAP_TIMEOUT = 100;
    uint8_t ultimaVel_m1 = 0;
    uint8_t ultimaVel_m2 = 0;
    

    uint8_t rsOn = 0;
    bool stateR = 0;
    
    float velMax, kp, ki, kd;
    int novasMarcas;

    void calibrarRobo(); 
    void iniciarCorrida();
    void terminarCorrida(); 
    void atualizarPID(float novaVelMax, float novoKp, float novoKi, float novoKd, int novasMarcas);
    void aplicarParametrosPID();
    void salvarParametrosNVS();
    void enviarTodosParametros();
    void loopSeguidor();
    void loopPerseguidor();
    void enviarLeituraSensores();
    void lineWhite();
    void lineBlack();
    void markCounter(uint8_t n);

};