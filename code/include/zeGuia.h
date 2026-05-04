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
    velEsq(0),
    velDir(0),
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
    static const uint32_t SENSOR_SEND_INTERVAL_MS = 120;
    
    float velMax, kp, ki, kd;
    int velEsq, velDir, novasMarcas;

    void calibrarRobo(); 
    void iniciarCorrida();
    void terminarCorrida(); 
    void atualizarPID(float novaVelMax, float novoKp, float novoKi, float novoKd, int novoVelEsq, int novoVelDir, int novasMarcas);
    void aplicarParametrosPID();
    void loopSeguidor();
    void loopPerseguidor();
    void enviarLeituraSensores();


};