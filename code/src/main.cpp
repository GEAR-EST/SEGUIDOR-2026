#include <Arduino.h>
#include <BluetoothSerial.h>
#include "controls.h"
#include "communication.h"
#include "commands.h"
#include "zeGuia.h"

QueueHandle_t commandsQueue;
ZeGuia zeGuia;

void setup() {

  Serial.begin(115200);
  delay(1500);
  Serial.println("Sistema Iniciando...");

  commandsQueue = xQueueCreate(10, sizeof(RobotMessage));
  if (commandsQueue == NULL) 
  {
    Serial.println("Falha ao criar commandsQueue");
    while (true) 
    {
      delay(1000);
    }
  }

  xTaskCreatePinnedToCore(ControlsTask, "Task_PID", 4096, NULL, 3, NULL, 1);

  xTaskCreatePinnedToCore(CommunicationTask, "Task_BT", 8192, NULL, 1, NULL, 0);
  
}

void loop() {
}
