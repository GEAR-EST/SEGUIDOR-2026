/**
 * @file communication.cpp
 * @brief Gerenciamento da comunicação Bluetooth e envio de bateria da ESP32.
 *
 * Implementa a task FreeRTOS de comunicação, o parsing de mensagens recebidas
 * via Bluetooth Serial, o callback de conexão/desconexão e o envio periódico
 * do nível de bateria.
 *
 * Protocolo de entrada (app -> ESP32):
 * - `PID:V|Kp|Ki|Kd|Marcas`  -> parâmetros PID
 * - `K` -> calibrar
 * - `R` -> iniciar corrida
 * - `F` -> parar
 * - `S` -> modo seguidor
 * - `P` -> modo perseguidor
 * - `C` -> estratégia conservadora
 * - `A` -> estratégia arriscada
 * - `L` / `l` -> stream de sensores ligado / desligado
 * - `Q` -> solicitar parâmetros atuais
 * - `1` / `0` -> LED aceso / apagado
 */

#include "communication.h"
#include "commands.h"

extern QueueHandle_t commandsQueue;

BluetoothSerial SerialBT;
SemaphoreHandle_t btMutex;
String bt_device = "Ze-Guia_BT";

void BluetoothConnection();
void SerialMonitorChecked(RobotCommand cmd);

/**
 * @brief Task FreeRTOS principal de comunicação Bluetooth.
 *
 * Inicializa a conexão Bluetooth, lê linhas do stream serial caractere a
 * caractere, parseia o protocolo de entrada e envia as mensagens para a fila
 * de comandos. Também aciona o envio periódico da bateria.
 *
 * @param pvParameters Parâmetro padrão de task FreeRTOS (não utilizado).
 */
void CommunicationTask(void* pvParameters)
{
    pinMode(BATTERY_PIN, INPUT);
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

                RobotMessage message = {CMD_NONE, false, 0.0f, 0.0f, 0.0f, 0.0f, 0};

                if (inputLine.startsWith("PID:"))
                {
                    // Formato: PID:V|Kp|Ki|Kd|NovasMarcas
                    String payload = inputLine.substring(4);
                    int sep1 = payload.indexOf('|');
                    int sep2 = payload.indexOf('|', sep1 + 1);
                    int sep3 = payload.indexOf('|', sep2 + 1);
                    int sep4 = payload.indexOf('|', sep3 + 1);

                    if (sep1 > 0 && sep2 > 0 && sep3 > 0 && sep4 > 0)
                    {
                        message.hasPidTunings = true;
                        message.vMax        = payload.substring(0, sep1).toFloat();
                        message.kp          = payload.substring(sep1 + 1, sep2).toFloat();
                        message.ki          = payload.substring(sep2 + 1, sep3).toFloat();
                        message.kd          = payload.substring(sep3 + 1, sep4).toFloat();
                        message.novasMarcas = payload.substring(sep4 + 1).toInt();
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
                            message.command = CMD_SENSOR_STREAM_ON;
                            sensorStreamRequested = true;
                            SerialMonitorChecked(message.command);
                            break;
                        case 'l':
                            message.command = CMD_SENSOR_STREAM_OFF;
                            sensorStreamRequested = false;
                            SerialMonitorChecked(message.command);
                            break;
                        case 'Q':
                            message.command = CMD_GET_PARAMS;
                            break;
                        default:
                            break;
                    }
                }

                if (message.hasPidTunings)
                {
                    Serial.printf("-> Parametros: V=%.2f Kp=%.2f Ki=%.2f Kd=%.2f Marcas=%d\n",
                        message.vMax, message.kp, message.ki, message.kd,
                        message.novasMarcas);
                    SerialBT.printf("-> Parametros: V=%.2f Kp=%.2f Ki=%.2f Kd=%.2f Marcas=%d\n",
                        message.vMax, message.kp, message.ki, message.kd,
                        message.novasMarcas);
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

/**
 * @brief Callback de eventos SPP Bluetooth (conexão e desconexão).
 *
 * Ao conectar, envia mensagem de boas-vindas, reseta o stream de sensores e
 * solicita os parâmetros atuais. Ao desconectar, garante que o stream de
 * sensores seja interrompido.
 *
 * @param event Tipo do evento SPP.
 * @param param Parâmetros do evento (não utilizados diretamente).
 */
void callback(esp_spp_cb_event_t event, esp_spp_cb_param_t *param) {
    if (event == ESP_SPP_SRV_OPEN_EVT) {
        Serial.println(">>> Celular CONECTADO!");
        SerialBT.println("Conexão Estabelecida com Zé-Guia");
        RobotMessage stopStream = {CMD_SENSOR_STREAM_OFF, false, 0.0f, 0.0f, 0.0f, 0.0f, 0};
        xQueueSend(commandsQueue, &stopStream, 0);
        RobotMessage getParams = {CMD_GET_PARAMS, false, 0.0f, 0.0f, 0.0f, 0.0f, 0};
        xQueueSend(commandsQueue, &getParams, 0);
    }

    if (event == ESP_SPP_CLOSE_EVT) {
        Serial.println(">>> Celular DESCONECTADO!");
        RobotMessage stopStream = {CMD_SENSOR_STREAM_OFF, false, 0.0f, 0.0f, 0.0f, 0.0f, 0};
        xQueueSend(commandsQueue, &stopStream, 0);
    }
}

/**
 * @brief Inicializa a conexão Bluetooth Serial e registra o callback SPP.
 *
 * Cria o mutex de acesso ao SerialBT, registra o callback de eventos,
 * inicia o Bluetooth com o nome do dispositivo e imprime o endereço MAC.
 */
void BluetoothConnection()
{
    btMutex = xSemaphoreCreateMutex();

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

/**
 * @brief Envia feedback serial (USB e Bluetooth) para cada comando recebido.
 *
 * Cada comando envia duas mensagens via SerialBT quando necessário:
 * - Mensagem sem prefixo (`Estado:`, `Modo:`, `Estrategia:`) -> parseada pelo
 *   app para atualizar os campos da UI.
 * - Mensagem com prefixo `->` -> exibida no terminal do app.
 *
 * @param cmd Comando recebido, do tipo RobotCommand.
 */
void SerialMonitorChecked(RobotCommand cmd)
{
    switch(cmd)
    {
        case CMD_LED_ON:
            Serial.println("Led do esp32 aceso");
            SerialBT.println("-> Led aceso");
            break;

        case CMD_LED_OFF:
            Serial.println("Led do esp32 apagado");
            SerialBT.println("-> Led apagado");
            break;

        case CMD_CALIBRATE:
            Serial.println("Robo calibrando");
            SerialBT.println("Estado: Calibrando");
            SerialBT.println("-> Calibrando");
            break;

        case CMD_START:
            Serial.println("Robo comecando a corrida");
            SerialBT.println("Estado: Correndo");
            SerialBT.println("-> Iniciando corrida");
            break;

        case CMD_STOP:
            Serial.println("Robo finalizou a corrida");
            SerialBT.println("-> Corrida finalizada");
            break;

        case CMD_MODE_FOLLOWER:
            Serial.println("Modo selecionado: SEGUIDOR");
            SerialBT.println("Modo: Seguidor");
            SerialBT.println("-> Modo: Seguidor");
            break;

        case CMD_MODE_CHASE:
            Serial.println("Modo selecionado: PERSEGUIDOR");
            SerialBT.println("Modo: Perseguidor");
            SerialBT.println("-> Modo: Perseguidor");
            break;

        case CMD_STRATEGY_CONSERVATIVE:
            Serial.println("Estrategia selecionada: CONSERVADOR");
            SerialBT.println("Estrategia: Conservador");
            break;

        case CMD_STRATEGY_RISK:
            Serial.println("Estrategia selecionada: ARRISCADO");
            SerialBT.println("Estrategia: Arriscado");
            break;

        case CMD_SENSOR_STREAM_ON:
            Serial.println("Stream de sensores ligado");
            SerialBT.println("-> Stream de sensores ligado");
            break;

        case CMD_SENSOR_STREAM_OFF:
            Serial.println("Stream de sensores desligado");
            SerialBT.println("-> Stream de sensores desligado");
            break;

        default:
            break;
    }
}

/**
 * @brief Calcula o percentual de bateria a partir da leitura analógica.
 *
 * Realiza 16 leituras do pino de bateria, calcula a média e mapeia para
 * 0–100%. O valor é limitado ao intervalo válido com `constrain`.
 *
 * @return Percentual de bateria entre 0 e 100.
 */
uint8_t battery_percentage(){
    long sum = 0;
    for (int i = 0; i < 16; i++){
        sum += analogRead(BATTERY_PIN);
    }
    int avg = sum/16;
    int perc = map(avg, 2419, 3024, 0, 100);
    perc = constrain(perc, 0, 100);
    return (uint8_t) perc;
}

/**
 * @brief Envia o percentual de bateria via Bluetooth a cada intervalo definido.
 *
 * Utiliza um mutex para acesso seguro ao SerialBT em ambiente multithread.
 * Formato enviado: `BAT<percentual>` (ex: `BAT85`).
 */
void send_battery(){
    static unsigned long past_time = 0;
    unsigned long current_time = millis();
    if (current_time - past_time >= bat_interval){
        past_time = current_time;
        if (xSemaphoreTake(btMutex, pdMS_TO_TICKS(10)) == pdTRUE) {
            SerialBT.print("BAT");
            SerialBT.println(battery_percentage());
            xSemaphoreGive(btMutex);
        }
    }
}