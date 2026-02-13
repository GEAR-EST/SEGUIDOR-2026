#include "communication.h"

BluetoothSerial SerialBT;
String bt_device = "Ze-Guia_BT";

void CommunicationTask(void* pvParameters) {
    SerialBT.begin(bt_device);
    
    for (;;) {
        if (SerialBT.available()) {
            char msg_comando = SerialBT.read();
            // Lógica da interface e FSM
        }
        vTaskDelay(pdMS_TO_TICKS(50));
    }
}