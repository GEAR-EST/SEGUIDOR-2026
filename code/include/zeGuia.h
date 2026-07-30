/**
 * @file zeGuia.h
 * @brief Declaração da classe ZeGuia, núcleo da lógica de controle do robô.
 *
 * A classe ZeGuia encapsula a máquina de estados do robô, os parâmetros PID,
 * o controle de corrida e o envio de dados de sensores. É instanciada uma única
 * vez em main.cpp e operada pela ControlsTask via fila de comandos.
 *
 * @author Gear Robotics
 * @version 1.0
 */

#pragma once
#include "commands.h"
#include "pid.h"

/**
 * @class ZeGuia
 * @brief Máquina de estados e lógica principal do robô seguidor/perseguidor de linha.
 *
 * Gerencia o modo de operação (Seguidor ou Perseguidor), a estratégia (Conservador
 * ou Arriscado), o ciclo de corrida e o streaming de sensores. Os parâmetros PID
 * são persistidos na NVS da ESP32 por combinação de modo e estratégia.
 */
class ZeGuia
{

public:

    /**
     * @enum RobotMode
     * @brief Modo de operação do robô.
     */
    enum RobotMode
    {
        MODE_NONE,      /**< Nenhum modo selecionado. */
        MODE_FOLLOWER,  /**< Modo Seguidor: segue a linha e para de forma autônoma. */
        MODE_CHASE      /**< Modo Perseguidor: tenta alcançar o robô adversário. */
    };

    /**
     * @enum RobotStrategy
     * @brief Estratégia de corrida selecionada pelo operador.
     */
    enum RobotStrategy
    {
        S_NONE,          /**< Nenhuma estratégia selecionada. */
        S_CONSERVATIVE,  /**< Conservador: prioriza estabilidade e conclusão da volta. */
        S_RISK           /**< Arriscado: maior velocidade, risco de perder a linha. */
    };

    /**
     * @brief Construtor padrão; inicializa todos os atributos com valores neutros.
     */
    ZeGuia() :
    mode(MODE_NONE),
    strategy(S_NONE),
    running(false),
    sensorStreaming(false),
    lastSensorSendMs(0),
    velMax(0),
    kp(0.0),
    ki(0.0),
    kd(0.0),
    novasMarcas(0)
    {}

    /** @brief Inicializa hardware e sensores chamando _setup() dos módulos externos. */
    void setup();

    /** @brief Executa a lógica cíclica do robô: controle de corrida e streaming de sensores. */
    void loop();

    /**
     * @brief Processa uma mensagem recebida da fila de comandos.
     *
     * Aplica os parâmetros PID se @p message.hasPidTunings for verdadeiro,
     * e despacha o comando para @ref processarComando.
     *
     * @param message Mensagem recebida da commandsQueue.
     */
    void processarMensagem(const RobotMessage& message);

    /**
     * @brief Executa a ação correspondente ao comando recebido.
     * @param cmd Comando do tipo @ref RobotCommand a ser executado.
     */
    void processarComando(RobotCommand cmd);

private:
    RobotMode mode;           /**< Modo de operação atual. */
    RobotStrategy strategy;   /**< Estratégia de corrida atual. */
    bool running;             /**< `true` enquanto a corrida está em andamento. */
    bool sensorStreaming;     /**< `true` enquanto o stream de sensores está ativo. */
    uint32_t lastSensorSendMs;/**< Timestamp do último envio de sensores (ms). */
    uint32_t lastStopMs;      /**< Timestamp da última parada (ms). */
    int pos_ant = 0;

    static const uint32_t SENSOR_SEND_INTERVAL_MS = 120; /**< Intervalo entre envios de sensores (ms). */
    static const uint32_t TIME_BACK_STOP = 500;          /**< Tempo de ré ao parar (ms). */
    static const uint8_t VEL_MIN = 70;                   /**< Velocidade mínima aplicada após mapeamento. */

    uint8_t rsOn = 0;  /**< Contador de marcas laterais detectadas na corrida atual. */
    bool stateR = 0;   /**< Estado do sensor lateral para detecção de borda de marca. */

    float velMax, kp, ki, kd; /**< Parâmetros PID ativos. */
    int novasMarcas;           /**< Número de marcas para parada autônoma (0 = desativado). */

    /** @brief Executa a calibração dos sensores e notifica o app com `Estado: Calibrado`. */
    void calibrarRobo();

    /** @brief Inicia a corrida: reseta contadores, limpa a fila e seta running = true. */
    void iniciarCorrida();

    /** @brief Para a corrida: aplica ré breve, trava motores e notifica `Estado: Parado`. */
    void terminarCorrida();

    /**
     * @brief Atualiza os parâmetros PID em memória, aplica ao controlador e persiste na NVS.
     * @param novaVelMax Nova velocidade máxima.
     * @param novoKp Novo ganho proporcional.
     * @param novoKi Novo ganho integral.
     * @param novoKd Novo ganho derivativo.
     * @param novasMarcas Novo número de marcas para parada autônoma.
     */
    void atualizarPID(float novaVelMax, float novoKp, float novoKi, float novoKd, int novasMarcas);

    /** @brief Aplica velMax ao VEL_MAX global e atualiza os ganhos do controlador PID. */
    void aplicarParametrosPID();

    /** @brief Salva os parâmetros atuais na NVS com chave baseada no modo e estratégia. */
    void salvarParametrosNVS();

    /** @brief Envia via Bluetooth todos os 4 conjuntos de parâmetros salvos na NVS. */
    void enviarTodosParametros();

    /** @brief Loop do modo Seguidor: executa lineWhite e, se conservador, markCounter. */
    void loopSeguidor();

    /** @brief Loop do modo Perseguidor: aciona motores na velocidade máxima. */
    void loopPerseguidor();

    /** @brief Envia a leitura atual dos sensores via Bluetooth no formato `S,pos,s1..s8,dir,esq`. */
    void enviarLeituraSensores();

    /** @brief Lê a linha branca com PID e aplica a saída aos motores com mapeamento de velocidade. */
    void lineWhite();
    void lineBlack();

    /**
     * @brief Conta marcas laterais detectadas pelo sensor direito e para o robô ao atingir o alvo.
     * @param n Número de marcas que disparam a parada autônoma.
     */
    void markCounter(uint8_t n);
    bool handleDashed(int pos);

};
