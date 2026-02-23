#include <Arduino.h>
#include <BluetoothSerial.h>
#include "controls.h"
#include "communication.h"
#include "commands.h"

QueueHandle_t commandsQueue;

void setup() {
  Serial.begin(115200);
  delay(1500);
  Serial.println("Sistema Iniciando...");

  commandsQueue = xQueueCreate(10, sizeof(RobotCommand));

  xTaskCreatePinnedToCore(
    ControlsTask,
    "Task_PID",
    4096,
    NULL,
    1,
    NULL,
    1
  );

  xTaskCreatePinnedToCore(
    CommunicationTask,
    "Task_BT",
    8192,
    NULL,
    1,
    NULL,
    0
  );
  
}

void loop() 
{
  delay(1000);
}
