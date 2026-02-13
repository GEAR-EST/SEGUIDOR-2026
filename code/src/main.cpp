#include <BluetoothSerial.h>

void setup() {
  Serial.begin(115200);


  xTaskCreatePinnedToCore(
    ControlsTask,
    "Task_PID",
    4096,
    NULL,
    3,
    &Task1,
    1
  );

  xTaskCreatePinnedToCore(
    CommunicationTask,
    "Task_BT",
    4096,
    NULL,
    1,
    &Task2,
    0
  );
  
}

void loop() 
{
  vTaskDelete(NULL);
}
