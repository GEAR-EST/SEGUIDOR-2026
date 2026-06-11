#pragma once

#include <BluetoothSerial.h>
#include <freertos/semphr.h>

#define BATTERY_PIN 13

extern BluetoothSerial SerialBT;
extern SemaphoreHandle_t btMutex;

uint8_t battery_percentage();

void send_battery();

extern unsigned long past_time;

const long bat_interval = 10000;

void CommunicationTask(void* pvParameters);
