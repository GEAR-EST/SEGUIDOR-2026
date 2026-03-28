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
    velMax(0),
    kp(0.0),
    ki(0.0),
    kd(0.0)
    {}

    void setup();
    void loop();
    void processarMensagem(const RobotMessage& message);
    void processarComando(RobotCommand cmd);

private:
    RobotMode mode;
    RobotStrategy strategy;
    bool running;
    
    float velMax, kp, ki, kd;

    void calibrarRobo(); 
    void iniciarCorrida();
    void terminarCorrida(); 
    void atualizarPID(float novaVelMax, float novoKp, float novoKi, float novoKd);
    void aplicarParametrosPID();
    void loopSeguidor();
    void loopPerseguidor();


};