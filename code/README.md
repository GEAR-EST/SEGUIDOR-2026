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

*A ser documentado.*

---

## Módulo de Sensores

*A ser documentado.*

---

## Módulo de Motores

*A ser documentado.*
