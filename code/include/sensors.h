#ifndef SENSORS_H
#define SENSORS_H

#include <QTRSensors.h>
#include <Preferences.h>

#define RightSensor 16
#define LeftSensor 17

#define EMPTY_VALUE 65535

#define LED_BUILTIN 2

#define D1_PIN 34
#define D2_PIN 35
#define D3_PIN 32
#define D4_PIN 33
#define D5_PIN 25
#define D6_PIN 26
#define D7_PIN 27
#define D8_PIN 14

//14, 27, 26, 25, 33, 32, 35, 34

extern Preferences preferences;
extern QTRSensors qtr;

void setup_qtr();

void setup_side_sensors();

void qtr_print();

void side_sensors_print();

void doCalibration();

bool readCalibration();

const long qtr_interval = 10000;

#endif