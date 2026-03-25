#include "communication.h"
#include "commands.h"
#include "robot_state.h"
#include "battery.h"
#include "globals.h"

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
    const uint32_t BATTERY_SEND_INTERVAL_MS = 1000;
    uint32_t lastBatterySendMs = 0;

    BluetoothConnection();
    
    for (;;) {
        uint32_t nowMs = millis();
        if (SerialBT.hasClient() && (nowMs - lastBatterySendMs >= BATTERY_SEND_INTERVAL_MS))
        {
            float batteryVoltage = voutCalculation(analogRead(BATTERY_PIN));
            float batteryPercentage = percentageCalculation(batteryVoltage);

            String batteryValue = "BAT," + String(batteryVoltage, 2) + "," + String(batteryPercentage, 2) + "\n";
            SerialBT.print(batteryValue);
            lastBatterySendMs = nowMs;
        }

        static String inputLine = "";
        while (SerialBT.available())
        {
            char c = SerialBT.read();
            if (c == '\n')
            {
                inputLine.trim();
                Serial.print("Linha recebida: ");
                Serial.println(inputLine);

                RobotCommand cmd = CMD_NONE;
                RobotMode md = MODE_NONE;
                RobotStrategy stg = S_NONE;

                if (inputLine.startsWith("PID:"))
                {
                    // Formato: PID:V|Kp|Ki|Kd
                    String payload = inputLine.substring(4);
                    int sep1 = payload.indexOf('|');
                    int sep2 = payload.indexOf('|', sep1 + 1);
                    int sep3 = payload.indexOf('|', sep2 + 1);

                    if (sep1 > 0 && sep2 > 0 && sep3 > 0)
                    {
                        float novoKp = payload.substring(sep1 + 1, sep2).toFloat();
                        float novoKi = payload.substring(sep2 + 1, sep3).toFloat();
                        float novoKd = payload.substring(sep3 + 1).toFloat();
                        pid.setTunnings(novoKp, novoKi, novoKd);
                        Serial.printf("PID atualizado: Kp=%.2f Ki=%.2f Kd=%.2f\n", novoKp, novoKi, novoKd);
                        SerialBT.printf("PID atualizado: Kp=%.2f Ki=%.2f Kd=%.2f\n", novoKp, novoKi, novoKd);
                    }
                }
                else if (inputLine.length() == 1)
                {
                    char msg = inputLine.charAt(0);
                    switch(msg)
                    {
                        case '1':
                            cmd = CMD_LED_ON;
                            digitalWrite(2, HIGH);
                            SerialMonitorChecked(cmd);
                            break;
                        case '0':
                            cmd = CMD_LED_OFF;
                            digitalWrite(2, LOW);
                            SerialMonitorChecked(cmd);
                            break;
                        case 'K':
                            cmd = CMD_CALIBRATE;
                            SerialMonitorChecked(cmd);
                            break;
                        case 'R':
                            cmd = CMD_START;
                            SerialMonitorChecked(cmd);
                            break;
                        case 'F':
                            cmd = CMD_STOP;
                            SerialMonitorChecked(cmd);
                            break;
                        case 'S':
                            md = MODE_FOLLOWER;
                            SerialMonitorCheckedMode(md);
                            break;
                        case 'P':
                            md = MODE_CHASE;
                            SerialMonitorCheckedMode(md);
                            break;
                        case 'C':
                            stg = S_CONSERVATIVE;
                            SerialMonitorCheckedStg(stg);
                            break;
                        case 'A':
                            stg = S_RISK;
                            SerialMonitorCheckedStg(stg);
                            break;
                        default:
                            break;
                    }
                }

                if (cmd != CMD_NONE)
                {
                    xQueueSend(commandsQueue, &cmd, 0);
                }

                inputLine = "";
            }
            else if (c != '\r')
            {
                inputLine += c;
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