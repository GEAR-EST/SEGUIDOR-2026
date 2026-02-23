#include "communication.h"
#include "commands.h"

extern QueueHandle_t commandsQueue;

BluetoothSerial SerialBT;
String bt_device = "Ze-Guia_BT";

void BluetoothConnection();
void SerialMonitorChecked(RobotCommand cmd);

void CommunicationTask(void* pvParameters) {
    pinMode(2, OUTPUT);

    BluetoothConnection();
    
    for (;;) {
        if (SerialBT.available()) 
        {
            char msg = SerialBT.read();
            Serial.print("Comando recebido: ");
            Serial.println(msg);

            RobotCommand cmd = CMD_NONE;
            
            switch(msg)
            {
                case '1': //ligar led
                    cmd = CMD_LED_ON;
                    digitalWrite(2, HIGH);
                    SerialMonitorChecked(cmd);
                    break;
                case '0': //desligar led
                    cmd = CMD_LED_OFF;
                    digitalWrite(2, LOW);
                    SerialMonitorChecked(cmd);
                    break;
                case 'K': //calibrar
                    cmd = CMD_CALIBRATE;
                    SerialMonitorChecked(cmd);
                    break;
                case 'R': //começar corrida
                    cmd = CMD_RUN;
                    SerialMonitorChecked(cmd);
                    break;
                case 'F': //finalizar corrida
                    cmd = CMD_END;
                    SerialMonitorChecked(cmd);
                    break;
                
                default:
                    cmd = CMD_NONE;
                    break;
                
            }

            if (cmd != CMD_NONE)
            {
                xQueueSend(commandsQueue, &cmd, 0); //envia comando para a fila de comandos (mudar de 0 para quando o controls estiver implementado)

            }
        }
        vTaskDelay(pdMS_TO_TICKS(50));
        
    }
}

void BluetoothConnection()
{
    Serial.println("Passo 1: Iniciando Bluetooth...");
     SerialBT.println("Passo 1: Iniciando Bluetooth...");
   
    if (SerialBT.begin(bt_device)) 
    {
        Serial.println("Passo 2: BT iniciado com sucesso!");
        SerialBT.println("Passo 2: BT iniciado com sucesso!");
        
    } 
    else 
    {
        Serial.println("Passo 2: FALHA ao iniciar BT!");
        SerialBT.println("Passo 2: FALHA ao iniciar BT!");
    }

    vTaskDelay(pdMS_TO_TICKS(1000));
    
    Serial.print("Passo 3: Endereco MAC: ");
    Serial.println(SerialBT.getBtAddressString());

}

void SerialMonitorChecked(RobotCommand cmd)
{
    switch(cmd)
    {
        case CMD_LED_ON:
            Serial.println("Led do esp32 aceso");
            SerialBT.println("Led do esp32 aceso");
            break;

        case CMD_LED_OFF:
            Serial.println("Led do esp32 apagado");
            SerialBT.println("Led do esp32 apagado");
            break;

        case CMD_CALIBRATE:
            Serial.println("Robo calibrando");
            SerialBT.println("Robo calibrando");
            break;

        case CMD_RUN:
            Serial.println("Robo comecando a corrida");
            SerialBT.println("Robo comecando a corrida");
            break;

        case CMD_END:
            Serial.println("Robo finalizou a corrida");
            SerialBT.println("Robo finalizou a corrida");
            break;

        default:
            break;
    }
}
