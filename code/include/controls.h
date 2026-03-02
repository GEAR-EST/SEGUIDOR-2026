#ifndef CONTROLS_H
#define CONTROLS_H
#include "robot_state.h"

extern RobotState robotState;   

void ControlsTask(void* pvParameters);

#endif