#ifndef CONTROLS_H
#define CONTROLS_H

#include <QTRSensors.h>
#include <Preferences.h>

#define RightSensor 9
#define LeftSensor 8

#define LED_BUILTIN 2

#define EMPTY_VALUE 65535

extern Preferences preferences;
extern QTRSensors qtr;

void ControlsTask(void* pvParameters);

void doCalibration();

bool readCalibration();

void _setup();

void _loop();

#endif