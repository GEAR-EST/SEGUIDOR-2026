#ifndef CONTROLS_H
#define CONTROLS_H

#include <Arduino.h>

#define LED_BUILTIN 2

void ControlsTask(void* pvParameters);

void _setup();

void _loop();

#endif