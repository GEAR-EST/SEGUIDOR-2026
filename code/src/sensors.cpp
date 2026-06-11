/**
 * @file sensors.cpp
 * @brief Configuração e leitura dos sensores do ZeGuia.
 *
 * Gerencia o array QTR-8A (linha), os sensores laterais digitais e a
 * calibração persistente via NVS (Preferences).
 */

#include "sensors.h"
#include "communication.h"

QTRSensors qtr;               ///< Instância do array de sensores QTR-8A.
Preferences preferences;      ///< Interface com a NVS para persistência da calibração.
uint16_t sensorValues[SensorCount]; ///< Leituras brutas do QTR a cada ciclo.
uint16_t readRight = 0;       ///< Última leitura do sensor lateral direito.
uint16_t readLeft  = 0;       ///< Última leitura do sensor lateral esquerdo.

/**
 * @brief Inicializa o array de sensores QTR-8A.
 *
 * Define o tipo analógico, mapeia os pinos D1–D8 e configura o LED
 * embutido como saída (usado durante a calibração pelo QTRSensors).
 */
void setup_qtr() {
    qtr.setTypeAnalog();

    // qtr.setSensorPins((const uint8_t[]){14, 27, 26, 25, 33, 32, 35, 34}, SensorCount);
    qtr.setSensorPins((const uint8_t[]){D8_PIN, D7_PIN, D6_PIN, D5_PIN,
                                        D4_PIN, D3_PIN, D2_PIN, D1_PIN}, SensorCount);

    pinMode(LED_BUILTIN, OUTPUT);
}

/**
 * @brief Inicializa os sensores laterais digitais.
 *
 * Configura @c RightSensor e @c LeftSensor como entradas digitais.
 * Esses sensores detectam desvios acentuados da linha (ex.: cruzamentos).
 */
void setup_side_sensors() {
    pinMode(RightSensor, INPUT);
    pinMode(LeftSensor, INPUT);
}

/**
 * @brief Lê e imprime via Bluetooth o estado dos sensores laterais.
 *
 * Atualiza as variáveis globais @c readRight e @c readLeft e envia
 * os valores formatados pelo @c SerialBT para depuração em tempo real.
 */
void side_sensors_print() {
    readRight = digitalRead(RightSensor);
    readLeft  = digitalRead(LeftSensor);
    SerialBT.print("Direito: ");  SerialBT.print(readRight);
    SerialBT.print('\t');
    SerialBT.print("Esquerdo: "); SerialBT.println(readLeft);
}

/**
 * @brief Executa a calibração do QTR-8A e salva os resultados na NVS.
 *
 * Realiza 200 leituras de calibração (movimentar o robô sobre a linha
 * durante esse período para cobrir preto e branco). Ao final, persiste
 * os valores máximo e mínimo de cada sensor no namespace @c "calib" da
 * NVS, sobrescrevendo qualquer calibração anterior.
 *
 * @note Bloqueia a task por ~10 s (200 × calibrate() + delay inicial).
 *       Deve ser chamada apenas no modo de calibração, nunca no loop
 *       de corrida.
 *
 * @see readCalibration()
 */
void doCalibration() {
    vTaskDelay(pdMS_TO_TICKS(50));

    for (uint16_t i = 0; i < 200; i++) {
        qtr.calibrate();
    }

    /* Coleta os extremos para depuração e persistência. */
    uint16_t max_values[8];
    uint16_t min_values[8];

    for (uint8_t i = 0; i < 8; i++) {
        max_values[i] = EMPTY_VALUE;
        min_values[i] = EMPTY_VALUE;
    }

    SerialBT.println("Maximum values");
    for (uint16_t i = 0; i < SensorCount; i++) {
        SerialBT.print(qtr.calibrationOn.maximum[i]);
        SerialBT.print(' ');
        max_values[i] = qtr.calibrationOn.maximum[i];
    }

    SerialBT.println("Minimum values");
    for (uint16_t i = 0; i < SensorCount; i++) {
        SerialBT.print(qtr.calibrationOn.minimum[i]);
        SerialBT.print(' ');
        min_values[i] = qtr.calibrationOn.minimum[i];
    }

    preferences.begin("calib", false);
    preferences.putBytes("max_val", max_values, sizeof(max_values));
    preferences.putBytes("min_val", min_values, sizeof(min_values));
    preferences.end();

    SerialBT.println("Calibração terminou :p");
}

/**
 * @brief Carrega a calibração salva na NVS e a aplica ao QTR-8A.
 *
 * Lê os arrays @c max_val e @c min_val do namespace @c "calib". Valida
 * o tamanho dos dados lidos e rejeita entradas com @c EMPTY_VALUE, que
 * indicam que a calibração nunca foi realizada ou foi corrompida.
 *
 * Se válida, injeta os extremos diretamente em @c qtr.calibrationOn,
 * dispensando uma nova calibração física.
 *
 * @return @c true  se a calibração foi carregada e aplicada com sucesso.
 * @return @c false se os dados estiverem ausentes, incompletos ou inválidos.
 *
 * @see doCalibration()
 */
bool readCalibration() {
    uint16_t read_max[SensorCount];
    uint16_t read_min[SensorCount];

    preferences.begin("calib", true);
    size_t resMax = preferences.getBytes("max_val", read_max, sizeof(read_max));
    size_t resMin = preferences.getBytes("min_val", read_min, sizeof(read_min));
    preferences.end();

    /* Verifica se a NVS retornou o número esperado de bytes. */
    if (resMax != sizeof(read_max) || resMin != sizeof(read_min)) return false;

    for (auto n : read_max) {
        if (n == EMPTY_VALUE) {
            SerialBT.println("Erro: um valor vazio foi encontrado, por favor calibrar novamente");
            return false;
        }
    }

    for (auto n : read_min) {
        if (n == EMPTY_VALUE) {
            SerialBT.println("Erro: um valor vazio foi encontrado, por favor calibrar novamente");
            return false;
        }
    }

    /* Injeta os extremos calibrados diretamente na estrutura interna do QTR. */
    qtr.calibrate();
    for (uint8_t i = 0; i < SensorCount; i++) {
        qtr.calibrationOn.maximum[i] = read_max[i];
        qtr.calibrationOn.minimum[i] = read_min[i];
    }

    SerialBT.println("Calibração carregada com sucesso");
    return true;
}