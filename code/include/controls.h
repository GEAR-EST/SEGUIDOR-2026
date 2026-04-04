#ifndef CONTROLS_H
#define CONTROLS_H

#include <Arduino.h>

<<<<<<< HEAD
#define BATTERY_PIN 13

extern RobotState robotState; 
=======
>>>>>>> b6aa64eb879197a0bec372377663e9bcb0de9ddf

void ControlsTask(void* pvParameters);

void _setup();

void _loop();

#endif