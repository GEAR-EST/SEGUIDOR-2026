#ifndef SENSORS_H
#define SENSORS_H

#include <QTRSensors.h>
#include <Preferences.h>
#include <Arduino.h>

#define RightSensor 9
#define LeftSensor 8

#define EMPTY_VALUE 65535

extern uint16_t readRight;
extern uint16_t readLeft;

const uint8_t SensorCount = 8;
extern uint16_t sensorValues[SensorCount];

extern Preferences preferences;
extern QTRSensors qtr;

void doCalibration();

bool readCalibration();

#endif