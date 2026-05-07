#ifndef COMMUNICATION_H
#define COMMUNICATION_H

#include <BluetoothSerial.h>

extern BluetoothSerial SerialBT;

uint8_t battery_percentage();

void send_battery();

extern unsigned long past_time;

const long bat_interval = 10000;

void CommunicationTask(void* pvParameters);

#endif