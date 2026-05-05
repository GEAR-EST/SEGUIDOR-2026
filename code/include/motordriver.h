#ifndef MOTORDRIVER_H
#define MOTORDRIVER_H

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

#define SETPOINT 2000

// fail safe

const unsigned long failtime = 500;
extern unsigned long past_fail;

extern int VEL_MAX;
extern int VEL_MAX_BACK; 
extern L298NX2 motors;

void lineBlack();
void lineWhite();

void controlMotors(int speedA, int speedB);

void pinModeMotors();

bool fail_safe();

#endif