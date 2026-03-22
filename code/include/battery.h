#pragma once

#define BATTERY_PIN 13

extern float r2;
extern float r3;
extern const float vin;

float voutCalculation(int);
float percentageCalculation(float);