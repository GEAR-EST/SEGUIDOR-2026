#include "communication.h"

BluetoothSerial SerialBT;
String bt_device = "Ze-Guia_BT";

void CommunicationTask(void* pvParameters) {
    pinMode(2, OUTPUT);
    Serial.println("Passo 1: Iniciando hardware BT...");
    
    if (SerialBT.begin("Ze-Guia_BT")) {
        Serial.println("Passo 2: BT iniciado com sucesso!");
    } else {
        Serial.println("Passo 2: FALHA ao iniciar BT!");
    }
    vTaskDelay(pdMS_TO_TICKS(1000));
    
    Serial.print("Passo 3: Endereco MAC: ");
    Serial.println(SerialBT.getBtAddressString());
    
    for (;;) {
        if (SerialBT.available()) {
            char msg_comando = SerialBT.read();
            Serial.print("Comando recebido: ");
            Serial.println(msg_comando);
            
            if (msg_comando == '1') 
            {
                digitalWrite(2, HIGH);
            } 
            else if (msg_comando == '0') 
            {
                digitalWrite(2, LOW);
            }
        }
        vTaskDelay(pdMS_TO_TICKS(100));
    }
}