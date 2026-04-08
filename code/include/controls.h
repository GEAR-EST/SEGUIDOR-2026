#ifndef CONTROLS_H
#define CONTROLS_H

#define BATTERY_PIN 13

void ControlsTask(void* pvParameters);

void _setup();

void _loop();

int battery_percentage();

void send_battery();

extern unsigned long past_time;

const long bat_interval = 10000;

#endif