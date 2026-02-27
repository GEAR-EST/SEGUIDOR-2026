#ifndef MOTORDRIVER_H
#define MOTORDRIVER_H

#include <Arduino.h>
#include <L298NX2.h>

//pin connections

#define PWMA 21
#define PWMB 15

#define AI1 4
#define AI2 2

#define BI1 18
#define BI2 19

#define STBY 5

// pid controller informations

#define SETPOINT 3500

extern int VEL_MAX;
extern int VEL_MIN; 

void lineBlack();
void lineWhite();

void controlMotors(int left, int right);

#endif