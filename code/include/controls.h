#ifndef CONTROLS_H
#define CONTROLS_H
#include "robot_state.h"

#include <Arduino.h>

#define LED_BUILTIN 2

extern RobotState robotState; 

void ControlsTask(void* pvParameters);

void _setup();

void _loop();

#endif