#ifndef CONTROLS_H
#define CONTROLS_H
#include "robot_state.h"

#include <Arduino.h>

extern RobotState robotState; 

void ControlsTask(void* pvParameters);

void _setup();

void _loop();

#endif