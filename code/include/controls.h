#ifndef CONTROLS_H
#define CONTROLS_H
#include "robot_state.h"

#include <Arduino.h>

#define BATTERY_PIN 13

extern RobotState robotState; 

void ControlsTask(void* pvParameters);

void _setup();

void _loop();

#endif