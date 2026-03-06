#include "communication.h"
#include "commands.h"
#include "robot_state.h"

extern QueueHandle_t commandsQueue;

BluetoothSerial SerialBT;
String bt_device = "Ze-Guia_BT";

void BluetoothConnection();
void SerialMonitorChecked(RobotCommand cmd);
void SerialMonitorCheckedMode(RobotMode md);
void SerialMonitorCheckedStg(RobotStrategy stg);

void CommunicationTask(void* pvParameters) 
{
    pinMode(2, OUTPUT);

    BluetoothConnection();
    
    for (;;) {
        if (SerialBT.available()) 
        {
            char msg = SerialBT.read();
            Serial.print("Comando recebido: ");
            Serial.println(msg);

            RobotCommand cmd = CMD_NONE;
            RobotMode md = MODE_NONE;
            RobotStrategy stg = S_NONE;
            
            
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
                    cmd = CMD_START;
                    SerialMonitorChecked(cmd);
                    break;
                case 'F': //finalizar corrida
                    cmd = CMD_STOP;
                    SerialMonitorChecked(cmd);
                    break;
                case 'M': //escolher o modo
                     cmd = CMD_SET_MODE;
                     SerialMonitorChecked(cmd);
                     break;
                case 'S': // modo seguidor
                     md = MODE_FOLLOWER;
                     SerialMonitorCheckedMode(md);
                     break;
                case 'P': // modo perseguidor
                     md = MODE_CHASE; 
                     SerialMonitorCheckedMode(md);
                     break;
                case 'E': //escolher estrategia
                     cmd = CMD_SET_STRATEGY;
                     SerialMonitorChecked(cmd);
                     break;
                case 'C': // estrategia conservador
                     stg = S_CONSERVATIVE;
                     SerialMonitorCheckedStg(stg);
                     break;
                case 'A': //estrategia arriscado
                     stg = S_RISK;
                     SerialMonitorCheckedStg(stg);
                     break;

                default:
                    cmd = CMD_NONE;
                    md = MODE_NONE;
                    stg = S_NONE;
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

void callback(esp_spp_cb_event_t event, esp_spp_cb_param_t *param) {
    if (event == ESP_SPP_SRV_OPEN_EVT) {
        Serial.println(">>> Celular CONECTADO!");
        SerialBT.println("Conexão Estabelecida com Zé-Guia");
    }

    if (event == ESP_SPP_CLOSE_EVT) {
        Serial.println(">>> Celular DESCONECTADO!");
    }
}

void BluetoothConnection()
{
    Serial.println("Passo 1: Iniciando Bluetooth...");
     SerialBT.println("Passo 1: Iniciando Bluetooth...");

    SerialBT.register_callback(callback);
   
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

        case CMD_START:
            Serial.println("Robo comecando a corrida");
            SerialBT.println("Robo comecando a corrida");
            break;

        case CMD_STOP:
            Serial.println("Robo finalizou a corrida");
            SerialBT.println("Robo finalizou a corrida");
            break;

        case CMD_SET_MODE:
            Serial.println("Escolhendo um modo...");
            SerialBT.println("Escolhendo um modo...");
            break;
        
        case CMD_SET_STRATEGY:
            Serial.println("Escolhendo uma estratégia...");
            SerialBT.println("Escolhendo uma estratégia...");
            break;

        default:
            break;
    }
}

void SerialMonitorCheckedMode(RobotMode md)
{
    switch(md)
    {
        case MODE_FOLLOWER:
            Serial.println("Modo selecionado: SEGUIDOR");
            SerialBT.println("Modo selecionado: SEGUIDOR");
            break;

        case MODE_CHASE:
            Serial.println("Modo selecionado: PERSEGUIDOR");
            SerialBT.println("Modo selecionado: PERSEGUIDOR");
            break;

        default:
            break;
    }
}

void SerialMonitorCheckedStg(RobotStrategy stg)
{
    switch(stg)
    {
        case S_CONSERVATIVE:
            Serial.println("Estrategia selecionada: CONSERVADOR");
            SerialBT.println("Estrategia selecionada: CONSERVADOR");
            break;

        case S_RISK:
            Serial.println("Estrategia selecionada: ARRISCADO");
            SerialBT.println("Estrategia selecionada: ARRISCADO");
            break;

        default:
            break;
    }
}