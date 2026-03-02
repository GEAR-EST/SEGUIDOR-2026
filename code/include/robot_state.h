#ifndef ROBOT_STATE_H
#define ROBOT_STATE_H

enum RobotMode
{
    MODE_NONE,
    MODE_FOLLOWER,
    MODE_CHASE
};

enum RobotStrategy
{
    S_NONE,
    S_RISK,
    S_CONSERVATIVE
};

struct RobotState
{
    RobotMode mode;
    RobotStrategy strategy;
    bool running;
};

#endif