#pragma once

#include <Arduino.h>
#include <L298NX2.h>

//pin connections

#define PWMA 21
#define PWMB 15

#define AI1 19
#define AI2 18

#define BI1 2
#define BI2 4

#define STBY 5

// pid controller informations

#define SETPOINT 3500

// fail safe

const uint32_t failtime = 500;
extern uint32_t past_fail;

extern int VEL_MAX;
extern L298NX2 motors;

void lineBlack();

void controlMotors(int speedA, int speedB);

void pinModeMotors();

bool fail_safe();
