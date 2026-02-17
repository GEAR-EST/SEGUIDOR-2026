#include <Arduino.h>
#include <BluetoothSerial.h>
#include "controls.h"
#include "communication.h"

void setup() {
  Serial.begin(115200);

  xTaskCreatePinnedToCore(ControlsTask, "Task_PID", 4096, NULL, 3, NULL, 1);

  xTaskCreatePinnedToCore(
    CommunicationTask,
    "Task_BT",
    4096,
    NULL,
    1,
    NULL,
    0
  );
  
}

void loop() {
  
}
