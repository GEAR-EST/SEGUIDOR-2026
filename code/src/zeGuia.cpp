/**
 * @file zeGuia.cpp
 * @brief Implementação da classe ZeGuia — lógica de controle, PID e estado do robô.
 *
 * Cada método executa uma responsabilidade isolada dentro do ciclo de vida da corrida:
 * calibração, seleção de modo/estratégia, loop de controle, streaming de sensores,
 * persistência de parâmetros na NVS e parada autônoma por contagem de marcas.
 *
 * @author Gear Robotics
 * @version 1.0
 */

#include "commands.h"
#include "communication.h"
#include "zeGuia.h"
#include "controls.h"
#include "globals.h"
#include "motordriver.h"
#include "sensors.h"
#include <Arduino.h>
#include <Preferences.h>

extern QueueHandle_t commandsQueue;

/**
 * @brief Inicializa o hardware do robô chamando _setup() dos módulos externos.
 */
void ZeGuia::setup()
{
    _setup();
}

/**
 * @brief Processa uma mensagem da commandsQueue, aplicando PID e/ou executando o comando.
 *
 * @param message Mensagem recebida, podendo conter parâmetros PID e/ou um comando.
 */
void ZeGuia::processarMensagem(const RobotMessage& message)
{
    if (message.hasPidTunings)
    {
        atualizarPID(message.vMax, message.kp, message.ki, message.kd,
                     message.novasMarcas);
    }

    if (message.command != CMD_NONE)
    {
        processarComando(message.command);
    }
}

/**
 * @brief Atualiza os parâmetros PID em memória, aplica ao controlador e persiste na NVS.
 *
 * @param novaVelMax Nova velocidade máxima.
 * @param novoKp     Novo ganho proporcional.
 * @param novoKi     Novo ganho integral.
 * @param novoKd     Novo ganho derivativo.
 * @param novasMarcas Novo número de marcas para parada autônoma.
 */
void ZeGuia::atualizarPID(float novaVelMax, float novoKp, float novoKi, float novoKd, int novasMarcas)
{
    velMax            = novaVelMax;
    kp                = novoKp;
    ki                = novoKi;
    kd                = novoKd;
    this->novasMarcas = novasMarcas;
    aplicarParametrosPID();
    salvarParametrosNVS();
}

/**
 * @brief Aplica velMax ao VEL_MAX global e atualiza os ganhos do controlador PID.
 */
void ZeGuia::aplicarParametrosPID()
{
    VEL_MAX = static_cast<float>(velMax);
    pid.setTunnings(kp, ki, kd);
}

/**
 * @brief Gera a chave NVS no formato `mAbbr_sAbbr_param`.
 *
 * @param mAbbr  Abreviação do modo  ("sf" para seguidor, "ps" para perseguidor).
 * @param sAbbr  Abreviação da estratégia ("co" para conservador, "ar" para arriscado).
 * @param param  Nome do parâmetro ("v", "kp", "ki", "kd", "mr").
 * @return String com a chave composta.
 */
static String nvsKey(const char* mAbbr, const char* sAbbr, const char* param)
{
    return String(mAbbr) + "_" + sAbbr + "_" + param;
}

/**
 * @brief Salva os parâmetros PID atuais na NVS com chave baseada no modo e estratégia ativos.
 *
 * Não faz nada se o modo ou a estratégia ainda não foram selecionados.
 */
void ZeGuia::salvarParametrosNVS()
{
    if (mode == MODE_NONE || strategy == S_NONE) return;
    const char* mAbbr = (mode == MODE_FOLLOWER) ? "sf" : "ps";
    const char* sAbbr = (strategy == S_CONSERVATIVE) ? "co" : "ar";

    Preferences prefs;
    prefs.begin("params", false);
    prefs.putInt  (nvsKey(mAbbr, sAbbr, "v").c_str(),   (int)velMax);
    prefs.putFloat(nvsKey(mAbbr, sAbbr, "kp").c_str(),  kp);
    prefs.putFloat(nvsKey(mAbbr, sAbbr, "ki").c_str(),  ki);
    prefs.putFloat(nvsKey(mAbbr, sAbbr, "kd").c_str(),  kd);
    prefs.putInt  (nvsKey(mAbbr, sAbbr, "mr").c_str(),  novasMarcas);
    prefs.end();
}

/**
 * @brief Envia via Bluetooth todos os 4 conjuntos de parâmetros PID salvos na NVS.
 *
 * Formato de cada linha: `PARAMS:Modo|Estrategia|V|Kp|Ki|Kd|Marcas`.
 * Utiliza btMutex para acesso thread-safe ao SerialBT.
 */
void ZeGuia::enviarTodosParametros()
{
    static const char* mAbbrs[]    = {"sf",          "sf",       "ps",           "ps"};
    static const char* sAbbrs[]    = {"co",          "ar",       "co",           "ar"};
    static const char* mNomes[]    = {"Seguidor",    "Seguidor",    "Perseguidor", "Perseguidor"};
    static const char* sNomes[]    = {"Conservador", "Arriscado",   "Conservador", "Arriscado"};

    Preferences prefs;
    prefs.begin("params", true);

    if (xSemaphoreTake(btMutex, pdMS_TO_TICKS(50)) == pdTRUE) {
        for (int i = 0; i < 4; i++)
        {
            int   v   = prefs.getInt  (nvsKey(mAbbrs[i], sAbbrs[i], "v").c_str(),  0);
            float pkp = prefs.getFloat(nvsKey(mAbbrs[i], sAbbrs[i], "kp").c_str(), 0.0f);
            float pki = prefs.getFloat(nvsKey(mAbbrs[i], sAbbrs[i], "ki").c_str(), 0.0f);
            float pkd = prefs.getFloat(nvsKey(mAbbrs[i], sAbbrs[i], "kd").c_str(), 0.0f);
            int   mr  = prefs.getInt  (nvsKey(mAbbrs[i], sAbbrs[i], "mr").c_str(), 0);
            SerialBT.printf("PARAMS:%s|%s|%d|%.2f|%.2f|%.2f|%d\n",
                mNomes[i], sNomes[i], v, pkp, pki, pkd, mr);
        }
        xSemaphoreGive(btMutex);
    }

    prefs.end();
}

/**
 * @brief Executa a lógica cíclica do robô: controle de corrida e streaming de sensores.
 *
 * Chamado repetidamente pela ControlsTask. Despacha para loopSeguidor() ou
 * loopPerseguidor() conforme o modo ativo, e envia leituras de sensores no
 * intervalo definido por SENSOR_SEND_INTERVAL_MS quando sensorStreaming está ativo.
 */
void ZeGuia::loop()
{
    if (running)
    {
        switch (mode)
        {
            case MODE_FOLLOWER:
                loopSeguidor();
                break;
            case MODE_CHASE:
                loopPerseguidor();
                break;
            default:
                break;
        }
    }

    if (sensorStreaming)
    {
        const uint32_t now = millis();
        if (now - lastSensorSendMs >= SENSOR_SEND_INTERVAL_MS)
        {
            lastSensorSendMs = now;
            enviarLeituraSensores();
        }
    }
}

/**
 * @brief Loop do modo Seguidor: executa o controle de linha e a parada autônoma por marcas.
 *
 * Na estratégia Conservadora, ativa também o contador de marcas laterais.
 * Na estratégia Arriscada, apenas segue a linha sem verificar marcas.
 */
void ZeGuia::loopSeguidor()
{
    if (strategy == S_CONSERVATIVE)
    {
        lineWhite();
        markCounter(novasMarcas);
    }
    else if (strategy == S_RISK)
    {
        lineWhite();
    }
}

/**
 * @brief Loop do modo Perseguidor: aciona os motores na velocidade máxima em linha reta.
 */
void ZeGuia::loopPerseguidor()
{
    if (strategy == S_CONSERVATIVE)
    {
        controlMotors((int)velMax, (int)velMax);
    }
    else if (strategy == S_RISK)
    {
        controlMotors((int)velMax, (int)velMax);
    }
}

/**
 * @brief Executa a calibração dos sensores e notifica o app com `Estado: Calibrado`.
 */
void ZeGuia::calibrarRobo()
{
    doCalibration();
    SerialBT.println("Estado: Calibrado");
}

/**
 * @brief Inicia a corrida: reseta contadores de marca, limpa a fila e seta running = true.
 *
 * Descarta mensagens acumuladas na fila para evitar comandos antigos afetando a nova corrida.
 */
void ZeGuia::iniciarCorrida()
{
    rsOn   = 0;
    stateR = 0;

    RobotMessage stale;
    while (xQueueReceive(commandsQueue, &stale, 0) == pdTRUE) {}

    running = true;
    rsOn = 0;
}

/**
 * @brief Para a corrida: aplica ré breve se dentro do intervalo, trava motores e notifica `Estado: Parado`.
 */
void ZeGuia::terminarCorrida()
{
    running = false;

    const uint32_t time_now = millis();
    if (time_now - lastStopMs >= TIME_BACK_STOP){
        controlMotors(-120, -120);
    }
    digitalWrite(AI1, HIGH);
    digitalWrite(AI2, HIGH);
    digitalWrite(BI1, HIGH);
    digitalWrite(BI2, HIGH);
    SerialBT.println("Estado: Parado");
}

/**
 * @brief Lê os sensores e envia a leitura via Bluetooth no formato `S,pos,s1..s8,dir,esq`.
 *
 * Utiliza btMutex para acesso thread-safe ao SerialBT.
 * Os valores dos sensores são invertidos (1000 - valor) para representar reflexão sobre fundo branco.
 */
void ZeGuia::enviarLeituraSensores()
{
    uint16_t position = qtr.readLineWhite(sensorValues);
    readRight = digitalRead(RightSensor);
    readLeft = digitalRead(LeftSensor);

    if (xSemaphoreTake(btMutex, pdMS_TO_TICKS(10)) == pdTRUE) {
        SerialBT.print("S,");
        SerialBT.print(position);
        for (uint8_t i = 0; i < SensorCount; i++)
        {
            SerialBT.print(',');
            SerialBT.print(1000 - sensorValues[i]);
        }
        SerialBT.print(',');
        SerialBT.print(readRight);
        SerialBT.print(',');
        SerialBT.println(readLeft);
        xSemaphoreGive(btMutex);
    }
}

/**
 * @brief Despacha o comando recebido para a ação correspondente no robô.
 *
 * @param cmd Comando a ser executado.
 */
void ZeGuia::processarComando(RobotCommand cmd)
{
    switch (cmd)
    {
    case CMD_CALIBRATE:
        calibrarRobo();
        break;
    case CMD_START:
        iniciarCorrida();
        break;
    case CMD_STOP:
        terminarCorrida();
        break;
    case CMD_MODE_FOLLOWER:
        mode = MODE_FOLLOWER;
        break;
    case CMD_MODE_CHASE:
        mode = MODE_CHASE;
        break;
    case CMD_STRATEGY_CONSERVATIVE:
        strategy = S_CONSERVATIVE;
        break;
    case CMD_STRATEGY_RISK:
        strategy = S_RISK;
        break;
    case CMD_SENSOR_STREAM_ON:
        sensorStreaming = true;
        lastSensorSendMs = 0;
        break;
    case CMD_SENSOR_STREAM_OFF:
        sensorStreaming = false;
        break;
    case CMD_GET_PARAMS:
        enviarTodosParametros();
        break;
    default:
        break;
    }
}

/**
 * @brief Lê a posição da linha branca, calcula a saída PID e aplica aos motores.
 *
 * A saída do PID é somada/subtraída da velocidade máxima para gerar as velocidades
 * individuais de cada motor. Os valores são mapeados para o intervalo [VEL_MIN, VEL_MAX]
 * antes de serem enviados ao driver de motores.
 */
void ZeGuia::lineWhite()
{
    int pos = qtr.readLineWhite(sensorValues);

    int pid_value = pid.somatory(SETPOINT, pos);

    int vel_m1 = VEL_MAX - pid_value;
    int vel_m2 = VEL_MAX + pid_value;

    vel_m1 = constrain(vel_m1, -VEL_MAX, VEL_MAX);
    vel_m2 = constrain(vel_m2, -VEL_MAX, VEL_MAX);

    if (vel_m1 >= 0) vel_m1 = map(vel_m1, 0, VEL_MAX, VEL_MIN, VEL_MAX);
    else if (vel_m1 < 0) vel_m1 = map(vel_m1, -VEL_MAX, 0, -VEL_MAX, -VEL_MIN);

    controlMotors(vel_m1, vel_m2);
}

/**
 * @brief Conta marcas laterais detectadas pelo sensor direito e para o robô ao atingir o alvo.
 *
 * Utiliza uma máquina de estados de dois bits (rsOn, stateR) para detectar a borda
 * de subida do sensor e evitar contagens múltiplas por marca.
 *
 * @param n Número de marcas que disparam a parada autônoma (0 = desativado).
 */
void ZeGuia::markCounter(uint8_t n)
{
    if (n > 0){
        if (!digitalRead(RightSensor) && stateR == 0){
            rsOn++;
            stateR = 1;
            SerialBT.print("Contador de marcas: "); SerialBT.println(rsOn);
        } else if (digitalRead(RightSensor) && stateR == 1){
            stateR = 0;
        }
        if (rsOn == n){
            SerialBT.println("Parada Ativa Ativada!");
            processarComando(RobotCommand::CMD_STOP);
        }
    }
}
