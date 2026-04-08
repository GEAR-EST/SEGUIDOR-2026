#ifndef CONTROLS_H
#define CONTROLS_H

#include <Arduino.h>

#define BATTERY_PIN 13

void ControlsTask(void* pvParameters);

void _setup();

void _loop();

int battery_percentage();

#endif