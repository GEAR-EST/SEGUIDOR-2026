#ifndef COMMUNICATION_H
#define COMMUNICATION_H

#include <BluetoothSerial.h>

extern BluetoothSerial SerialBT;

void CommunicationTask(void* pvParameters);

#endif