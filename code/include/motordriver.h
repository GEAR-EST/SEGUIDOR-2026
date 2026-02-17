#ifndef MOTORDRIVER_H
#define MOTORDRIVER_H

#include <Arduino.h>

//pin connections

#define PWMA 21
#define PWMB 15

#define A11 4
#define A12 2

#define B11 18
#define B12 19

#define STBY 5

extern int VELOCIDADE;

void controlMotors(int left, int right);

#endif