#ifndef COMMANDS_H
#define COMMANDS_H

enum RobotCommand
{
    CMD_LED_ON,
    CMD_LED_OFF,
    CMD_NONE,
    CMD_CALIBRATE,
    CMD_START,
    CMD_STOP,
    CMD_MODE_FOLLOWER,
    CMD_MODE_CHASE,
    CMD_STRATEGY_CONSERVATIVE,
    CMD_STRATEGY_RISK,
    CMD_SENSOR_STREAM_ON,
    CMD_SENSOR_STREAM_OFF
};

struct RobotMessage
{
    RobotCommand command;
    bool hasPidTunings;
    float vMax;
    float kp;
    float ki;
    float kd;
    int velEsq;
    int velDir;
    int novasMarcas;
};

#endif