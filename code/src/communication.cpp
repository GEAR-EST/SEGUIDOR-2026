#include "communication.h"
#include "commands.h"
#include "globals.h"

extern QueueHandle_t commandsQueue;

BluetoothSerial SerialBT;
String bt_device = "Ze-Guia_BT";

void BluetoothConnection();
void SerialMonitorChecked(RobotCommand cmd);

void CommunicationTask(void* pvParameters) 
{
    const uint32_t BATTERY_SEND_INTERVAL_MS = 1000;
    uint32_t lastBatterySendMs = 0;
    bool sensorStreamRequested = false;

    BluetoothConnection();
    
    for (;;) {

        static String inputLine = "";
        while (SerialBT.available())
        {
            char c = SerialBT.read();
            if (c == '\n')
            {
                inputLine.trim();
                Serial.print("Linha recebida: ");
                Serial.println(inputLine);

                RobotMessage message = {CMD_NONE, false, 0.0f, 0.0f, 0.0f, 0.0f, 0, 0, 0};

                if (inputLine.startsWith("PID:"))
                {
                    // Formato: PID:V|Kp|Ki|Kd|VelEsq|VelDir|NovasMarcas
                    String payload = inputLine.substring(4);
                    int sep1 = payload.indexOf('|');
                    int sep2 = payload.indexOf('|', sep1 + 1);
                    int sep3 = payload.indexOf('|', sep2 + 1);
                    int sep4 = payload.indexOf('|', sep3 + 1);
                    int sep5 = payload.indexOf('|', sep4 + 1);
                    int sep6 = payload.indexOf('|', sep5 + 1);

                    if (sep1 > 0 && sep2 > 0 && sep3 > 0)
                    {
                        message.hasPidTunings = true;
                        message.vMax = payload.substring(0, sep1).toFloat();
                        message.kp = payload.substring(sep1 + 1, sep2).toFloat();
                        message.ki = payload.substring(sep2 + 1, sep3).toFloat();
                        if (sep4 > sep3 && sep5 > sep4 && sep6 > sep5)
                        {
                            message.kd          = payload.substring(sep3 + 1, sep4).toFloat();
                            message.velEsq      = payload.substring(sep4 + 1, sep5).toInt();
                            message.velDir      = payload.substring(sep5 + 1, sep6).toInt();
                            message.novasMarcas = payload.substring(sep6 + 1).toInt();
                        }
                        else
                        {
                            message.kd = payload.substring(sep3 + 1).toFloat();
                        }
                    }
                }
                else if (inputLine.length() == 1)
                {
                    char msg = inputLine.charAt(0);
                    switch(msg)
                    {
                        case '1':
                            message.command = CMD_LED_ON;
                            digitalWrite(2, HIGH);
                            SerialMonitorChecked(message.command);
                            break;
                        case '0':
                            message.command = CMD_LED_OFF;
                            digitalWrite(2, LOW);
                            SerialMonitorChecked(message.command);
                            break;
                        case 'K':
                            message.command = CMD_CALIBRATE;
                            SerialMonitorChecked(message.command);
                            break;
                        case 'R':
                            message.command = CMD_START;
                            SerialMonitorChecked(message.command);
                            break;
                        case 'F':
                            message.command = CMD_STOP;
                            SerialMonitorChecked(message.command);
                            break;
                        case 'S':
                            message.command = CMD_MODE_FOLLOWER;
                            SerialMonitorChecked(message.command);
                            break;
                        case 'P':
                            message.command = CMD_MODE_CHASE;
                            SerialMonitorChecked(message.command);
                            break;
                        case 'C':
                            message.command = CMD_STRATEGY_CONSERVATIVE;
                            SerialMonitorChecked(message.command);
                            break;
                        case 'A':
                            message.command = CMD_STRATEGY_RISK;
                            SerialMonitorChecked(message.command);
                            break;
                        case 'L':
                            if (!sensorStreamRequested)
                            {
                                message.command = CMD_SENSOR_STREAM_ON;
                                sensorStreamRequested = true;
                                SerialMonitorChecked(message.command);
                            }
                            break;
                        case 'l':
                            if (sensorStreamRequested)
                            {
                                message.command = CMD_SENSOR_STREAM_OFF;
                                sensorStreamRequested = false;
                                SerialMonitorChecked(message.command);
                            }
                            break;
                        default:
                            break;
                    }
                }

                if (message.hasPidTunings)
                {
                    Serial.printf("Controle recebido: V=%.2f Kp=%.2f Ki=%.2f Kd=%.2f VelEsq=%d VelDir=%d Marcas=%d\n",
                        message.vMax, message.kp, message.ki, message.kd,
                        message.velEsq, message.velDir, message.novasMarcas);
                    SerialBT.printf("Controle recebido: V=%.2f Kp=%.2f Ki=%.2f Kd=%.2f VelEsq=%d VelDir=%d Marcas=%d\n",
                        message.vMax, message.kp, message.ki, message.kd,
                        message.velEsq, message.velDir, message.novasMarcas);
                }

                if (message.command != CMD_NONE || message.hasPidTunings)
                {
                    xQueueSend(commandsQueue, &message, 0);
                }

                inputLine = "";
            }
            else if (c != '\r')
            {
                inputLine += c;
            }
        }

        send_battery();

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

        case CMD_MODE_FOLLOWER:
            Serial.println("Modo selecionado: SEGUIDOR");
            SerialBT.println("Modo selecionado: SEGUIDOR");
            break;

        case CMD_MODE_CHASE:
            Serial.println("Modo selecionado: PERSEGUIDOR");
            SerialBT.println("Modo selecionado: PERSEGUIDOR");
            break;

        case CMD_STRATEGY_CONSERVATIVE:
            Serial.println("Estrategia selecionada: CONSERVADOR");
            SerialBT.println("Estrategia selecionada: CONSERVADOR");
            break;

        case CMD_STRATEGY_RISK:
            Serial.println("Estrategia selecionada: ARRISCADO");
            SerialBT.println("Estrategia selecionada: ARRISCADO");
            break;


        case CMD_SENSOR_STREAM_ON:
            Serial.println("Stream de sensores ligado");
            SerialBT.println("Stream de sensores ligado");
                
            break;

        case CMD_SENSOR_STREAM_OFF:
            Serial.println("Stream de sensores desligado");
            SerialBT.println("Stream de sensores desligado");
            break;

        default:
            break;
    }
}

uint8_t battery_percentage(){
    long sum = 0;
    for (int i = 0; i < 16; i++){
        sum += analogRead(BATTERY_PIN);
    }
    int avg = sum/16;
    int perc = map(avg, 2539, 3325, 0, 100);
    perc = constrain(perc, 0, 100);
    return (uint8_t) perc;
}

void send_battery(){
    static unsigned long past_time = 0;
    unsigned long current_time = millis();
    if (current_time - past_time >= bat_interval){
        past_time = current_time;
        SerialBT.print("BAT"); 
        SerialBT.println(battery_percentage());
        
    }
}