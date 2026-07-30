# Diretório de Código

Este diretório contém o firmware do robô ZeGuia, desenvolvido com o framework **Arduino** sobre **FreeRTOS** utilizando **PlatformIO** como ambiente de build.

O firmware roda em uma **ESP32** e é responsável por toda a lógica de controle do robô: leitura de sensores, controle PID dos motores, comunicação Bluetooth com o ZeGuiaApp e persistência de parâmetros na memória não volátil (NVS).

## Estrutura de Diretórios

```
code/
├── include/          # Cabeçalhos (.h) compartilhados entre os módulos
├── lib/              # Bibliotecas externas (QTR Sensors, L298N)
├── src/              # Código-fonte principal (.cpp)
└── platformio.ini    # Configuração da placa, framework e dependências
```

## Arquitetura do Firmware

O firmware é organizado em dois módulos principais executados como **tasks FreeRTOS** em núcleos separados da ESP32:

| Task | Núcleo | Prioridade | Responsabilidade |
|---|---|---|---|
| `CommunicationTask` | 0 | 1 | Bluetooth, parsing de mensagens, envio de bateria |
| `ControlsTask` | 1 | 3 | Lógica PID, controle de motores, sensores |

As tasks se comunicam exclusivamente via **fila FreeRTOS** (`commandsQueue`) do tipo `RobotMessage`, sem compartilhamento de variáveis direto entre núcleos.

---

## Módulo de Comunicação

### Visão Geral

O módulo de comunicação gerencia toda a troca de dados entre o firmware e o **ZeGuiaApp** via **Bluetooth Classic SPP** (Serial Port Profile), emulando uma porta serial sem fio sobre o protocolo RFCOMM.

É composto por três arquivos:

| Arquivo | Função |
|---|---|
| `include/communication.h` | Declarações públicas: `CommunicationTask`, `SerialBT`, `btMutex`, `send_battery` |
| `src/communication.cpp` | Implementação da task, parsing do protocolo, callback SPP e envio de bateria |
| `include/commands.h` | Enum `RobotCommand` e struct `RobotMessage` — unidade de dado da fila |

### Funcionamento

A `CommunicationTask` roda em loop infinito no núcleo 0. A cada iteração ela lê os bytes disponíveis no `SerialBT` caractere por caractere, acumulando-os numa string até encontrar o `\n` que indica fim de linha. Ao completar uma linha, remove espaços (`trim()`) e inicia o parsing.

Se a linha começa com `PID:`, os campos são extraídos por índice de separador `|` e armazenados nos campos de PID da `RobotMessage`, com `hasPidTunings = true`. Se a linha tem um único caractere, ele é mapeado diretamente para o `RobotCommand` correspondente no `switch`. Em ambos os casos, a mensagem montada é colocada na `commandsQueue` via `xQueueSend`.

Do outro lado, a `ControlsTask` consome essa fila e chama `zeGuia.processarMensagem()`, que aplica os parâmetros PID se presentes e executa o comando. Esse fluxo garante que a leitura do Bluetooth e o controle dos motores nunca disputem o mesmo núcleo.

Além da leitura, a cada iteração da task é chamada `send_battery()`, que verifica se já passou o intervalo de 10 segundos e, se sim, lê o pino de bateria e envia `BAT<percentual>` ao app. Ao fim de cada iteração, a task aguarda 50 ms com `vTaskDelay` para não monopolizar o núcleo.

### Protocolo de Comunicação

#### App → ESP32 (entrada)

| Mensagem | Comando gerado | Ação |
|---|---|---|
| `K` | `CMD_CALIBRATE` | Calibra os sensores |
| `R` | `CMD_START` | Inicia a corrida |
| `F` | `CMD_STOP` | Para a corrida |
| `S` | `CMD_MODE_FOLLOWER` | Seleciona modo Seguidor |
| `P` | `CMD_MODE_CHASE` | Seleciona modo Perseguidor |
| `C` | `CMD_STRATEGY_CONSERVATIVE` | Seleciona estratégia Conservadora |
| `A` | `CMD_STRATEGY_RISK` | Seleciona estratégia Arriscada |
| `L` | `CMD_SENSOR_STREAM_ON` | Liga stream de sensores |
| `l` | `CMD_SENSOR_STREAM_OFF` | Desliga stream de sensores |
| `Q` | `CMD_GET_PARAMS` | Solicita todos os parâmetros PID salvos |
| `PID:V\|Kp\|Ki\|Kd\|Marcas` | `hasPidTunings = true` | Atualiza parâmetros PID |

#### ESP32 → App (saída)

| Prefixo | Conteúdo | Uso no app |
|---|---|---|
| `Estado: X` | `Calibrando`, `Calibrado`, `Correndo`, `Parado` | Atualiza máquina de estados da UI |
| `Modo: X` | `Seguidor`, `Perseguidor` | Atualiza label de modo |
| `Estrategia: X` | `Conservador`, `Arriscado` | Atualiza label de estratégia |
| `PARAMS:Modo\|Estrat\|V\|Kp\|Ki\|Kd\|Marcas` | 7 campos por `\|` | Sincroniza parâmetros com SharedPreferences |
| `BAT<valor>` | Inteiro 0–100 | Atualiza indicador de bateria |
| `S,pos,s1..s8,dir,esq` | 12 campos por `,` | Atualiza modal de sensores |
| `-> mensagem` | Texto livre | Exibido no terminal do app |

### Thread Safety

O `SerialBT` é acessado por duas tasks simultâneas (`CommunicationTask` que lê, e `ZeGuia::enviarLeituraSensores` / `enviarTodosParametros` que escrevem). O acesso de escrita é protegido pelo **btMutex** (`SemaphoreHandle_t`), garantindo que apenas uma task utilize o SerialBT por vez.

### Callback SPP

Ao **conectar**, o callback envia uma mensagem de boas-vindas, interrompe qualquer stream de sensores residual e solicita automaticamente os parâmetros PID via `CMD_GET_PARAMS`. Ao **desconectar**, encerra o stream de sensores para evitar envios sem receptor.

### Bateria

A função `send_battery()` é chamada dentro do loop da `CommunicationTask` a cada **10 segundos** (`bat_interval`). Realiza 16 leituras analógicas do pino de bateria, calcula a média e mapeia o resultado para 0–100%, enviando no formato `BAT<percentual>`.

---

## Módulo Principal — ZeGuia

O módulo ZeGuia define o esqueleto da lógica principal do robô. A classe `ZeGuia` foi estruturada com a **máquina de estados interna** e as assinaturas de todos os métodos necessários, servindo de base para que os demais contribuidores implementem a lógica de controle e PID.

| Arquivo | Função |
|---|---|
| `include/zeGuia.h` | Declaração da classe, enums de modo/estratégia e assinaturas dos métodos |
| `src/zeGuia.cpp` | Esqueleto de implementação com a máquina de estados e estrutura de chamadas |

### Máquina de Estados Interna

O estado do robô é representado por quatro atributos da classe, que determinam o que o `loop()` executa a cada ciclo:

| Atributo | Tipo | Descrição |
|---|---|---|
| `mode` | `RobotMode` | `MODE_NONE`, `MODE_FOLLOWER` ou `MODE_CHASE` |
| `strategy` | `RobotStrategy` | `S_NONE`, `S_CONSERVATIVE` ou `S_RISK` |
| `running` | `bool` | `true` enquanto a corrida está em andamento |
| `sensorStreaming` | `bool` | `true` enquanto o stream de sensores está ativo |

As transições de estado são disparadas por `processarComando()`, que recebe comandos da `commandsQueue` e atualiza os atributos acima. A lógica de cada estado (controle PID, streaming, parada autônoma) está estruturada nos métodos privados e deve ser completada pelo contribuidor responsável pela parte de controle.

---

## Módulo de Controle e PID

O módulo de controle é responsável pelo ciclo principal de operação do robô. É composto por quatro arquivos:

| Arquivo | Função |
|---|---|
| `include/controls.h` | Declaração de `ControlsTask` |
| `src/controls.cpp` | Implementação da task de controle: consumo da fila de comandos e loop do robô |
| `include/pid.h` | Declaração da classe `PID` |
| `src/pid.cpp` | Implementação do cálculo P, I e D e dos métodos de tuning/reset |

A `ControlsTask` roda no núcleo 1 e chama `zeGuia.setup()` uma única vez antes de entrar em loop infinito. A cada iteração, tenta receber uma mensagem da `commandsQueue` sem bloquear (`xQueueReceive(..., 0)`); se houver mensagem, repassa para `zeGuia.processarMensagem()`. Em seguida, executa `zeGuia.loop()` independentemente de ter recebido mensagem, garantindo que o controle do robô continue rodando mesmo sem novos comandos. Ao fim de cada iteração, aguarda 10 ms (`vTaskDelay`), fixando a frequência do laço de controle em ~100 Hz. Esse desenho garante que o consumo de comandos via Bluetooth nunca bloqueie o ciclo de controle PID, já que a leitura da fila é não bloqueante.

A classe `PID` implementa o controlador clássico. `somatory(setpoint, mensuredValue)` calcula o erro entre o valor desejado e o medido, acumula o termo integral, deriva o erro em relação à chamada anterior e retorna `Kp·e + Ki·∫e + Kd·Δe`, atualizando o estado interno (`integral`, `previousError`) a cada chamada. `reset()` zera esse estado e deve ser chamado ao retomar o controle após uma pausa, evitando *integral windup*. `setTunnings(_p, _i, _d)` atualiza os ganhos em tempo de execução — usado pelo protocolo `PID:` vindo do app — sem resetar o estado acumulado.

---

## Módulo de Sensores

O módulo de sensores gerencia a leitura da linha via array QTR-8A, os sensores laterais digitais e a calibração persistente via NVS (Preferences). É composto por dois arquivos:

| Arquivo | Função |
|---|---|
| `include/sensors.h` | Pinout, constantes (`SensorCount`, `EMPTY_VALUE`) e declarações públicas |
| `src/sensors.cpp` | Setup dos sensores, leitura dos laterais, calibração e persistência |

`setup_qtr()` configura o array QTR-8A em modo analógico, mapeando os 8 pinos (`D1_PIN`–`D8_PIN`) em ordem invertida (`D8`→`D1`) e habilita o `LED_BUILTIN` como saída, usado pela biblioteca `QTRSensors` durante a calibração. `setup_side_sensors()` configura `RightSensor` e `LeftSensor` como entradas digitais simples, usadas para detectar desvios acentuados da linha (ex.: cruzamentos).

A calibração segue duas etapas. `doCalibration()` executa 200 chamadas a `qtr.calibrate()` — o robô deve ser passado manualmente sobre a linha durante esse período para cobrir preto e branco — e ao final grava os vetores de máximo e mínimo de cada sensor no namespace `"calib"` da NVS (`max_val` / `min_val`), sobrescrevendo qualquer calibração anterior. É uma chamada bloqueante (~10 s) e só deve rodar no modo de calibração, nunca durante uma corrida. `readCalibration()` lê os bytes salvos na NVS, valida o tamanho retornado e rejeita valores marcados como `EMPTY_VALUE` (indicando calibração nunca feita ou corrompida); se os dados forem válidos, injeta os extremos diretamente em `qtr.calibrationOn`, dispensando uma nova calibração física a cada boot.

`side_sensors_print()` lê `RightSensor`/`LeftSensor`, atualiza as variáveis globais `readRight`/`readLeft` e envia os valores formatados pelo `SerialBT` para depuração em tempo real.

---

## Módulo de Motores

O módulo de motores abstrai o controle de baixo nível dos dois motores via ponte H L298N (biblioteca `L298NX2`) e expõe o fail-safe de linha perdida. É composto por dois arquivos:

| Arquivo | Função |
|---|---|
| `include/motordriver.h` | Pinout da ponte H, constantes de controle e fail-safe, declarações públicas |
| `src/motordriver.cpp` | Implementação de `controlMotors`, `pinModeMotors` e `fail_safe` |

`controlMotors(speedA, speedB)` abstrai direção e velocidade dos dois motores a partir do sinal do valor recebido: positivo aciona `forward`, negativo aciona `backward` (usando o valor absoluto como velocidade), e zero aplica freio ativo (`stop`) — de forma independente para os motores A e B.

`pinModeMotors()` configura o PWM em 20 kHz (acima da faixa audível, evitando o chiado característico de PWM em frequências baixas), define os pinos de direção e PWM como saída, e habilita a ponte H colocando `STBY` em `HIGH`.

`fail_safe()` monitora se o robô perdeu a linha por tempo prolongado: captura o timestamp atual e, enquanto `qtr.readLineWhite()` retornar 0 (nenhuma linha detectada), compara o tempo decorrido contra `failtime` (500 ms fixos). Se exceder, retorna `true`, sinalizando para a camada superior decidir a ação (parar, recuar etc.).

> **Nota:** no momento não estamos utilizando o fail safe
