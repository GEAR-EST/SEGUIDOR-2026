# Diretório de Interface

Este diretório contém todos os arquivos relacionados ao desenvolvimento da interface de controle do robô seguidor de linha do projeto *SEGUIDOR-2026*.

Aqui está reunida a aplicação Android **ZéGuia App**, desenvolvida em *Kotlin*, que permite ao operador monitorar e controlar o robô em tempo real por meio de conexão Bluetooth. O diretório reúne o código-fonte do aplicativo, a evolução do design no Figma e os registros de cada versão lançada ao longo do projeto.

O objetivo deste diretório é documentar a arquitetura do aplicativo, registrar a evolução das versões e facilitar a compreensão do funcionamento completo da interface de controle.

---

## Visão Geral

O **ZéGuia App** é um aplicativo Android nativo desenvolvido em **Kotlin** utilizando o padrão de Activity única (`MainActivity`). Toda a lógica de comunicação, controle de estados, atualização de UI e persistência de dados reside em um único arquivo Kotlin, com os layouts definidos em arquivos XML separados por responsabilidade.

A comunicação com o robô é feita via **Bluetooth Classic** (protocolo RFCOMM, SPP), trocando mensagens de texto simples entre o aplicativo e o firmware da ESP32.

### Tecnologias Utilizadas

| Tecnologia | Versão | Descrição |
|---|---|---|
| Kotlin | 1.9+ | Linguagem principal do aplicativo |
| Android SDK | API 24+ | Plataforma alvo |
| AndroidX | - | Componentes de UI modernos |
| Material Design 3 | - | Sistema de design visual |
| Bluetooth Classic (RFCOMM) | - | Comunicação com a ESP32 |
| SharedPreferences | - | Persistência local de parâmetros |
| Coroutines (lifecycleScope) | - | Operações assíncronas |

---

## Ícone do Aplicativo

> Insira aqui a imagem do ícone final do ZéGuia App

```
[ESPAÇO PARA FOTO DO ÍCONE]
```

---

## Tela Principal

A tela principal do aplicativo é composta por seções organizadas verticalmente, cada uma com uma função específica no controle do robô.

<img src="img/tela_v10.jpg" height="700" width="auto">

### Estrutura da Tela

#### Header

Barra superior com fundo escuro `#130822` contendo logo, título e dois botões de ação:

- **Logo do GEAR** — ícone clicável com fundo gradiente roxo que abre o modal "Sobre Nós"
- **Título ZÉ-GUIA** — fonte Orbitron em branco com espaçamento entre letras
- **Botão ESP32** — ícone de chip roxo; ao tocar, abre o modal de configuração de endereços MAC
- **Botão Bluetooth** — ícone de Bluetooth; muda de cor conforme o estado: roxo `#A066FF` quando desconectado, laranja durante a tentativa de conexão e verde `#00FF66` quando conectado; toque liga e desliga a conexão

#### Card Quadro Geral

Card principal com label **QUADRO GERAL** e botão de exportar no canto superior direito (ícone de upload, habilitado somente após uma corrida finalizada). Internamente dividido em dois blocos lado a lado:

- **Bloco esquerdo — Cronômetro**: ícone gráfico de relógio e display digital verde neon no formato `M.SS` (minutos e segundos com centésimos)
- **Bloco direito — Parâmetros**: exibe Kp, Ki, Kd e Vm do modo/estratégia atual com ícone de lápis para abrir edição; abaixo, seção **INFORMAÇÕES** com Estado, Modo e Estratégia do robô em tempo real

#### Card Bateria

Card separado abaixo do Quadro Geral. Exibe o percentual numérico à esquerda e uma barra de progresso contínua à direita. A cor muda dinamicamente: ciano `#00E5FF` acima de 50%, laranja abaixo de 50% e vermelho abaixo de 20%.

#### Seção Controles

Dois botões lado a lado:

- **CALIBRAR** — envia o comando `K` para o firmware; bloqueia todos os demais botões enquanto o estado `Calibrando` está ativo
- **SENSORES** — abre o modal de leitura dos sensores em tempo real

#### Seção Modo

Agrupamento com dois botões de seleção:

- **SEGUIDOR** — define o robô no modo seguidor de linha; envia comando `S`
- **PERSEGUIDOR** — define o robô no modo de perseguição; envia comando `P`

#### Seção Estratégia

Aparece abaixo da seleção de modo:

- **CONSERVADOR** — aplica a estratégia conservadora; envia comando `C` e carrega os parâmetros salvos para a combinação modo+conservador
- **ARRISCADO** — aplica a estratégia arriscada; envia comando `A` e carrega os parâmetros salvos para a combinação modo+arriscado

#### Botões de Corrida

Dois botões de destaque visual:

- **COMEÇAR CORRIDA** — envia comando `R`; disponível somente após modo e estratégia selecionados
- **FINALIZAR CORRIDA** — envia comando `F`; disponível somente durante a corrida

#### Terminal

Monitor de texto com fundo escuro que exibe todas as mensagens recebidas do robô via Bluetooth, com scroll automático para a última linha. Mensagens de bateria (`BAT`), sensores (`S,`) e parâmetros (`PARAMS:`) são tratadas internamente sem aparecer no terminal.

---

## Modais

### Modal: Editar Parâmetros

Acessado pelo ícone de lápis no card de parâmetros. Só pode ser aberto após a seleção de modo e estratégia.

Permite editar os cinco parâmetros da configuração atual:

| Campo | Tipo | Incremento |
|---|---|---|
| VM (Velocidade Máxima) | Inteiro | ±1 |
| Kp | Float | ±0,01 |
| Ki | Float | ±0,01 |
| Kd | Float | ±0,01 |
| Marcas | Inteiro | ±1 |

Cada parâmetro possui botões `+` e `−` (stepper) e campo de edição direta. Ao salvar, os valores são armazenados no `SharedPreferences` com prefixo `modo_estrategia_` e enviados ao firmware no formato:

```
PID:VM|Kp|Ki|Kd|Marcas
```

> Insira aqui a foto do modal de editar parâmetros

```
[ESPAÇO PARA FOTO DO MODAL EDITAR PARÂMETROS]
```

---

### Modal: Sensores

Acessado pelo botão SENSORES. Exibe a leitura em tempo real dos 8 sensores do array QTR-8RC e dos 2 sensores laterais (esquerdo e direito) em uma grade 4×2.

Ao abrir, envia o comando `L` ao firmware para iniciar o stream de dados de sensores. Ao fechar, envia o comando `l` para encerrar o stream.

O formato de exibição é:

```
POS: XXXX
 S1  |  S2  |  S3  |  S4
 S5  |  S6  |  S7  |  S8
ESQ: XX | DIR: XX
```

> Insira aqui a foto do modal de sensores

```
[ESPAÇO PARA FOTO DO MODAL SENSORES]
```

---

### Modal: Exportar

Acessado pelo botão EXPORTAR, disponível somente após o término de uma corrida (estado `Parado`).

Gera um resumo formatado da corrida com os dados:

- Data e hora
- Tempo final em segundos
- Modo e estratégia utilizados
- Quantidade de marcas
- Parâmetros PID (VM, Kp, Ki, Kd)
- Status da corrida (Completa / Parcialmente / Interrompida)
- Campo de observações livres

O botão de copiar só é habilitado após a seleção do status da corrida. O resumo é copiado para a área de transferência do celular para facilitar o registro de performance.

> Insira aqui a foto do modal de exportar

```
[ESPAÇO PARA FOTO DO MODAL EXPORTAR]
```

---

### Modal: Configurar ESP32

Acessado pelo badge de MAC no header. Só pode ser aberto quando o robô está desconectado ou com estado `Parado`.

Permite:

- **Selecionar** uma ESP32 já cadastrada via Spinner (lista dropdown)
- **Cadastrar** um novo endereço MAC digitando no campo de texto (formatação automática com `:`)
- **Editar e excluir** endereços MACs salvos

O campo de texto formata automaticamente o MAC conforme a digitação, inserindo os dois pontos nos lugares corretos. O MAC é validado contra o padrão `XX:XX:XX:XX:XX:XX` antes de ser salvo.

Os endereços MAC são persistidos em `SharedPreferences` como um conjunto (`Set<String>`), permitindo alternar entre diferentes unidades do robô sem redigitar.

> Insira aqui a foto do modal de configurar ESP32

```
[ESPAÇO PARA FOTO DO MODAL CONFIGURAR ESP32]
```

---

### Modal: Sobre Nós

Acessado pelo logo do GEAR no header.

Exibe informações sobre a equipe criadora do robô, incluindo nomes, funções, logo do GEAR, logo da instituição e QR Code para o Instagram do GEAR.

> Insira aqui a foto do modal sobre nós

```
[ESPAÇO PARA FOTO DO MODAL SOBRE NÓS]
```

---

## Arquivos Kotlin

A partir da versão 8, o código foi **modularizado**: a lógica que antes estava inteiramente em `MainActivity.kt` foi distribuída em 10 arquivos helpers especializados, seguindo o princípio de responsabilidade única. A `MainActivity` passou a atuar apenas como orquestradora, conectando os helpers entre si.

### `BluetoothEventListener.kt`

Interface Kotlin que define os callbacks de eventos de conexão e dados recebidos da ESP32. Implementada pela `MainActivity`, desacopla completamente o `BluetoothHelper` da Activity.

| Método | Disparado quando |
|---|---|
| `onConnected()` | Conexão RFCOMM estabelecida com sucesso |
| `onDisconnected()` | Conexão encerrada (deliberada ou inesperada) |
| `onConnectionFailed()` | Tentativa de conexão falhou |
| `onRawMessage(msg)` | Mensagem serial genérica recebida |
| `onEstado(valor)` | Recebeu `Estado: X` |
| `onModo(valor)` | Recebeu `Modo: X` |
| `onEstrategia(valor)` | Recebeu `Estrategia: X` |
| `onBateria(percentual)` | Recebeu `BATxx` |
| `onSensores(parts)` | Recebeu linha `S,...` |
| `onParams(parts)` | Recebeu `PARAMS:...` |

---

### `BluetoothHelper.kt`

Gerencia a conexão RFCOMM com a ESP32 em threads separadas. Parseia as mensagens recebidas e dispara os callbacks da `BluetoothEventListener`. Detecta desconexões inesperadas (ex: ESP32 desligada durante corrida) e notifica via `onDisconnected()`.

---

### `BluetoothIconHelper.kt`

Atualiza o ícone e o card de Bluetooth conforme o estado da conexão. Aplica `ColorFilter` ao ícone e modifica o `GradientDrawable` do card (borda + fundo semi-transparente) para três estados:

| Estado | Cor do ícone e borda |
|---|---|
| Desconectado | Roxo `#A066FF` |
| Conectando | Laranja `#FF8800` |
| Conectado | Verde `#00FF66` |

---

### `CronometroHelper.kt`

Controla o cronômetro de corrida. Exibe o tempo no formato `SS.cc` (segundos e centésimos). Atualiza o `TextView` a cada 50 ms via `Handler`. Expõe os métodos `iniciar()`, `parar()` e `resetar()`.

---

### `UiStateHelper.kt`

Máquina de estados de UI. Cada método `estadoXxx()` habilita ou desabilita os 9 botões de controle conforme o estado atual do robô, garantindo que o operador só interaja com ações válidas no momento.

---

### `ParametrosHelper.kt`

Gerencia os parâmetros PID (Kp, Ki, Kd, Vm, Marcas). Armazena o modo e estratégia ativos, calcula o prefixo de chave `modo_estrategia_` para o `SharedPreferences` e mantém o display atualizado. Exibe e controla o modal de edição com steppers de incremento/decremento.

---

### `SensoresHelper.kt`

Gerencia o modal de leitura em tempo real dos sensores. Exibe os 8 sensores ópticos em grade 4×2, a posição ponderada e as velocidades dos motores. Ativa o stream com `L` ao abrir e desativa com `l` ao fechar. As referências de view são nulas quando o modal está fechado, evitando memory leaks.

---

### `EspConfigHelper.kt`

Gerencia o modal de configuração da ESP32. Exibe a lista de MACs cadastrados e permite adicionar, editar e excluir entradas. Formata automaticamente o MAC com dois-pontos durante a digitação. Bloqueia a abertura durante corrida ativa (`Estado: Correndo`).

---

### `ExportarHelper.kt`

Gerencia o modal de exportação de feedback de corrida. Monta o resumo com data/hora, tempo em segundos, modo, estratégia e parâmetros PID. O botão de copiar só é habilitado após a seleção do status da corrida (Completa / Parcialmente / Interrompida). Copia o relatório final para a área de transferência.

---

### `SobreNosHelper.kt`

Singleton que gerencia o modal "Sobre Nós" e as animações associadas. O modal possui uma animação de luzes inspirada no grid de largada da Fórmula 1: as luzes acendem sequencialmente e apagam de uma vez simulando a largada. O ponto verde no terminal principal pisca a cada 900 ms enquanto a Activity está ativa.

---

### `MainActivity.kt`

Orquestradora da aplicação. Instancia e conecta todos os helpers via injeção manual de dependências, implementa a `BluetoothEventListener` e mantém o estado de sessão (MAC ativo, MACs salvos, flag `modoSelecionado`).

| Método | Responsabilidade |
|---|---|
| `onCreate()` | Inicializa views, helpers, listeners, permissões e restaura sessão anterior |
| `onDestroy()` | Destrói helpers com handlers e fecha o socket |
| `inicializarViews()` | Localiza e atribui todas as views do layout |
| `inicializarHelpers()` | Instancia todos os helpers e registra listeners de botões |
| `configurarListeners()` | Registra listeners do switch Bluetooth e dos cards do header |
| `animarClique()` | Animação de "aperto" (scale 78%→100%) nos botões do header |
| `atualizarEstadoEdicaoParametros()` | Habilita/desabilita o botão de edição conforme estratégia ativa |
| `atualizarBateria()` | Atualiza percentual e 10 barras de cor dinâmica |
| `onConnected()` / `onDisconnected()` / `onConnectionFailed()` | Callbacks Bluetooth → atualiza ícone e estado da UI |
| `onEstado()` / `onModo()` / `onEstrategia()` | Callbacks de estado do robô → aciona máquina de estados |
| `onBateria()` / `onSensores()` / `onParams()` | Callbacks de dados → delega ao helper responsável |

---

## Protocolo de Comunicação Bluetooth

### Comandos enviados pelo App → ESP32

| Comando | Ação |
|---|---|
| `K` | Iniciar calibração |
| `S` | Selecionar modo Seguidor |
| `P` | Selecionar modo Perseguidor |
| `C` | Selecionar estratégia Conservador |
| `A` | Selecionar estratégia Arriscado |
| `R` | Começar corrida |
| `F` | Finalizar corrida |
| `L` | Ligar stream de leitura de sensores |
| `l` | Desligar stream de leitura de sensores |
| `Q` | Solicitar parâmetros salvos no firmware |
| `PID:VM\|Kp\|Ki\|Kd\|Marcas` | Enviar parâmetros ao firmware |

### Mensagens recebidas pela ESP32 → App

| Prefixo | Conteúdo | Ação no App |
|---|---|---|
| `Estado: X` | `Correndo`, `Parado`, `Calibrando`, `Calibrado` | Atualiza label, cor e estado da máquina de estados |
| `Modo: X` | `Seguidor`, `Perseguidor` | Atualiza label e habilita botões de estratégia |
| `Estrategia: X` | `Conservador`, `Arriscado` | Atualiza label e habilita botão de iniciar corrida |
| `BATXX` | Número inteiro 0–100 | Atualiza barras e percentual de bateria |
| `PARAMS:modo\|estrat\|V\|Kp\|Ki\|Kd\|Marcas` | 7 campos separados por `\|` | Sincroniza parâmetros do firmware com o SharedPreferences |
| `S,pos,s1..s8,dir,esq` | 12 campos separados por `,` | Atualiza o modal de sensores |

---

## Máquina de Estados

O aplicativo implementa uma máquina de estados baseada nas mensagens recebidas do robô. Cada estado habilita ou desabilita um conjunto específico de botões da interface.

```
[ESPAÇO PARA DIAGRAMA DA MÁQUINA DE ESTADOS]
```

| Estado | Gatilho | Botões habilitados |
|---|---|---|
| **Desconectado** | App iniciado / Switch BT desligado | Nenhum |
| **Conectado** | Conexão Bluetooth estabelecida | Calibrar, Seguidor, Perseguidor, Sensores |
| **Calibrando** | Recebeu `Estado: Calibrando` | Nenhum (todos bloqueados) |
| **Calibrado** | Recebeu `Estado: Calibrado` | Calibrar, Seguidor, Perseguidor, Sensores |
| **Pós-Modo** | Recebeu `Modo: X` | Seguidor, Perseguidor, Conservador, Arriscado, Sensores |
| **Pós-Estratégia** | Recebeu `Estrategia: X` | Conservador, Arriscado, Começar Corrida, Sensores |
| **Correndo** | Recebeu `Estado: Correndo` | Finalizar Corrida |
| **Finalizado** | Recebeu `Estado: Parado` | Todos (incluindo Exportar) |

---

## Fluxo de Uso do Aplicativo

```
[ESPAÇO PARA DIAGRAMA DE FLUXO / DIAGRAMA DE ATIVIDADES]
```

O fluxo completo para realizar uma corrida é:

1. Abrir o aplicativo → estado Desconectado, ícone Bluetooth roxo
2. (Opcional) Tocar no botão de chip ESP32 no header → configurar ou selecionar endereço MAC
3. Tocar no botão Bluetooth → ícone fica laranja (conectando); ao conectar, fica verde e o app envia `Q` para sincronizar parâmetros
4. Selecionar **Modo** (Seguidor ou Perseguidor)
5. Selecionar **Estratégia** (Conservador ou Arriscado) → app envia os parâmetros salvos ao firmware e libera o botão de edição
6. (Opcional) Tocar no ícone de lápis para editar parâmetros
7. Pressionar **CALIBRAR** → aguardar estado Calibrado (ícone lápis bloqueado durante calibração)
8. Pressionar **COMEÇAR CORRIDA** → cronômetro inicia ao receber `Estado: Correndo`
9. Robô finaliza autonomamente ou operador pressiona **FINALIZAR CORRIDA** → cronômetro para
10. Pressionar **EXPORTAR** → selecionar status → adicionar observações → copiar resumo

---

## Como Executar o Aplicativo

### Pré-requisitos

- Android Studio Hedgehog (2023.1.1) ou superior
- JDK 17
- Android físico com API 24 (Android 7.0) ou superior
- Bluetooth Classic habilitado no celular

### Passos

```bash
# 1. Clone o repositório
git clone <url-do-repositorio>

# 2. Abra o Android Studio
# File > Open > selecione a pasta interface/ZeGuiaApp

# 3. Aguarde o Gradle sincronizar as dependências

# 4. Conecte um dispositivo Android via USB com depuração USB ativada

# 5. Execute pelo botão Run (▶) ou via terminal:
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Permissões Necessárias

O `AndroidManifest.xml` declara as permissões de Bluetooth. Em dispositivos Android 12+ (API 31+), o aplicativo solicita em tempo de execução:

- `BLUETOOTH_CONNECT`
- `BLUETOOTH_SCAN`

### Pareamento da ESP32

Antes de usar o aplicativo, é necessário parear a ESP32 com o celular pelo menu de configurações de Bluetooth do Android. O aplicativo não realiza o pareamento, apenas a conexão com dispositivos já pareados.

---

## Diagramas UML

### Diagrama de Casos de Uso

Representa as principais interações do operador com o sistema.

```
[ESPAÇO PARA DIAGRAMA DE CASOS DE USO]
```

---

### Diagrama de Sequência: Fluxo de Conexão e Corrida

Representa a troca de mensagens entre o App e a ESP32 durante uma corrida completa.

```
[ESPAÇO PARA DIAGRAMA DE SEQUÊNCIA]
```

---

### Diagrama de Máquina de Estados

Representa os estados da interface e as transições causadas pelas mensagens recebidas do robô.

```
[ESPAÇO PARA DIAGRAMA DE MÁQUINA DE ESTADOS UML]
```

---

### Diagrama de Atividades: Fluxo de Uso

Representa o fluxo de ações do operador desde a abertura do app até o fim de uma corrida.

```
[ESPAÇO PARA DIAGRAMA DE ATIVIDADES]
```

---

### Diagrama de Componentes

Representa a arquitetura de componentes do sistema, incluindo App, Bluetooth, ESP32 e Firmware.

```
[ESPAÇO PARA DIAGRAMA DE COMPONENTES]
```

---

## Evolução do Design — Figma

O design do ZéGuia App foi desenvolvido iterativamente no **Figma**, acompanhando as versões do aplicativo. Abaixo estão os registros de cada versão do protótipo.

### Versão 1

```
[ESPAÇO PARA IMAGEM DO FIGMA V1]
```

### Versão 2

```
[ESPAÇO PARA IMAGEM DO FIGMA V2]
```

### Versão 3

```
[ESPAÇO PARA IMAGEM DO FIGMA V3]
```

### Versão 4

```
[ESPAÇO PARA IMAGEM DO FIGMA V4]
```

### Versão 5

```
[ESPAÇO PARA IMAGEM DO FIGMA V5]
```

### Versão 6

```
[ESPAÇO PARA IMAGEM DO FIGMA V6]
```

### Versão 7

```
[ESPAÇO PARA IMAGEM DO FIGMA V7]
```

### Versão 8

```
[ESPAÇO PARA IMAGEM DO FIGMA V8]
```

### Versão 9

```
[ESPAÇO PARA IMAGEM DO FIGMA V9]
```

### Versão 10 (Final)

```
[ESPAÇO PARA IMAGEM DO FIGMA V10]
```

---

## Histórico de Versões

### Versão 1

Primeira versão funcional do ZéGuia App. Implementação base da comunicação Bluetooth com a ESP32 e tela de controle inicial.

> Insira aqui a foto da tela da versão 1

```
[ESPAÇO PARA FOTO DA VERSÃO 1]
```

---

### Versão 2 — ESP32 `88:57:21:7A:C6:1E`

- Edição de parâmetros expandida: `velEsq`, `velDir` e `Contador de Marcas`
- Componente visual de bateria dinâmico com atualização conforme percentual recebido da ESP32
- Botão **Exportar** para registro de informações e performance após finalizar corrida

> Insira aqui a foto da tela da versão 2

```
[ESPAÇO PARA FOTO DA VERSÃO 2]
```

---

### Versão 3 — ESP32 `88:57:21:7A:C6:1E`

- Parâmetros vinculados a modo e estratégia: 4 conjuntos independentes salvos em memória
  - (1) Seguidor + Conservador
  - (2) Seguidor + Arriscado
  - (3) Perseguidor + Conservador
  - (4) Perseguidor + Arriscado
- Ao selecionar modo e estratégia, o card de parâmetros exibe automaticamente os valores salvos para aquela combinação
- Novo estado **Calibrado** após conclusão da calibração; demais botões bloqueados durante calibração
- Cronômetro com fonte maior para melhor visualização
- Orientação do aplicativo bloqueada em retrato

> Insira aqui a foto da tela da versão 3

```
[ESPAÇO PARA FOTO DA VERSÃO 3]
```

---

### Versão 4 — ESP32 `88:57:21:7A:C6:1E`

- Correção da lógica de parada autônoma sincronizada com a máquina de estados do app
- Ajustes de tamanho no layout

> Insira aqui a foto da tela da versão 4

```
[ESPAÇO PARA FOTO DA VERSÃO 4]
```

---

### Versão 5 — ESP32 `88:57:21:7A:C6:1E`

- Atualização do número de dígitos do cronômetro (remoção de um dígito à esquerda)
- Conversão de tempo para segundos no feedback da corrida
- Layout da leitura de sensores reorganizado em grade 4×2 para os 8 sensores

> Insira aqui a foto da tela da versão 5

```
[ESPAÇO PARA FOTO DA VERSÃO 5]
```

---

### Versão 6 — ESP32 `C0:49:EF:65:16:FE`

- **Troca de ESP32**: novo endereço MAC `C0:49:EF:65:16:FE`
- Parâmetros passam a ser obtidos diretamente da ESP32 (não mais armazenados apenas localmente)
- Velocidade convertida de float para inteiro
- Remoção de `velEsq` e `velDir` no modal de edição de parâmetros
- Remoção do botão de controle de LED

> Insira aqui a foto da tela da versão 6

```
[ESPAÇO PARA FOTO DA VERSÃO 6]
```

---

### Versão 7 — ESP32 `C0:49:EF:65:16:FE`

- Correção no modal de editar parâmetros: ao reabrir o modal, os valores salvos são exibidos corretamente
- Incremento dos steppers de Kp, Ki e Kd reduzido de 0,1 para **0,01**
- Edição de parâmetros bloqueada até que modo e estratégia sejam selecionados
- Remoção do tempo em minutos no feedback da corrida

> Insira aqui a foto da tela da versão 7

```
[ESPAÇO PARA FOTO DA VERSÃO 7]
```

---

### Versão 8 — ESP32 `C0:49:EF:65:16:FE`

Grande atualização de layout para sincronização com o design do Figma.

- Redesign completo do quadro geral e de todos os modais (sensores, editar parâmetros e exportar)
- Nova funcionalidade: **Configurar ESP32** — possibilidade de adicionar e selecionar endereços MAC
- Adição do modal **Sobre Nós** com nomes e funções da equipe, logo do GEAR, logo da instituição e QR Code do Instagram
- Cronômetro exibindo segundos (mudança de minutos para segundos)
- VM reincluído na tela principal

> Insira aqui a foto da tela da versão 8

```
[ESPAÇO PARA FOTO DA VERSÃO 8]
```

---

### Versão 9 — ESP32 `C0:49:EF:65:16:FE`

- Melhorias de layout em relação à versão 8, principalmente no modal Sobre Nós
- Ícone Bluetooth do header: switch removido; ícone muda de cor (laranja ao conectar, verde quando conectado)
- Mini animação de clique nos botões do header (contração e expansão)
- Correções de fontes, cores e padronização visual

> Insira aqui a foto da tela da versão 9

```
[ESPAÇO PARA FOTO DA VERSÃO 9]
```

---

### Versão 10 — Final

- Sincronização total com a versão final do Figma
- Logo atualizada
- Configuração da ESP32: **editar e excluir** endereços MACs cadastrados com confirmação de exclusão via dialog customizado
- Campo "Adicionar novo MAC" oculto por padrão; ícone da lixeira com mesma cor do lápis
- **Modularização completa**: toda a lógica extraída da `MainActivity` para 10 helpers especializados (`BluetoothHelper`, `BluetoothIconHelper`, `CronometroHelper`, `UiStateHelper`, `ParametrosHelper`, `SensoresHelper`, `EspConfigHelper`, `ExportarHelper`, `SobreNosHelper`, `BluetoothEventListener`)
- Detecção de **desconexão inesperada** (ESP32 desligada durante corrida); `SharedPreferences` padronizado entre todos os helpers
- Animação de largada da Fórmula 1 no modal Sobre Nós
- Ponto verde piscante no terminal

<img src="img/tela_v10.jpg" height="700" width="auto">

---

## Estrutura de Diretórios

```
interface/
├── img/                           # Fotos das versões do aplicativo
└── ZeGuiaApp/
    └── app/
        └── src/
            └── main/
                ├── java/com/example/zeguiaapp/
                │   ├── MainActivity.kt            # Orquestradora; implementa BluetoothEventListener
                │   ├── BluetoothEventListener.kt  # Interface de callbacks de eventos
                │   ├── BluetoothHelper.kt         # Conexão RFCOMM, I/O serial, parse de mensagens
                │   ├── BluetoothIconHelper.kt     # Colorização do ícone e card Bluetooth
                │   ├── CronometroHelper.kt        # Cronômetro no formato SS.cc
                │   ├── UiStateHelper.kt           # Máquina de estados de botões
                │   ├── ParametrosHelper.kt        # PID, display e modal de edição
                │   ├── SensoresHelper.kt          # Modal de leitura de sensores em tempo real
                │   ├── EspConfigHelper.kt         # Modal de configuração de endereços MAC
                │   ├── ExportarHelper.kt          # Modal de exportação de feedback de corrida
                │   └── SobreNosHelper.kt          # Modal "Sobre Nós" e animação F1
                ├── res/
                │   ├── layout/
                │   │   ├── activity_main.xml      # Tela principal
                │   │   ├── editar_parametros.xml  # Modal de edição de parâmetros
                │   │   ├── modal_sensores.xml     # Modal de leitura de sensores
                │   │   ├── modal_exportar.xml     # Modal de exportar feedback
                │   │   └── modal_esp32.xml        # Modal de configuração da ESP32
                │   ├── drawable/                  # Ícones e backgrounds XML vetoriais
                │   ├── font/                      # Fontes Orbitron e Source Code Pro
                │   ├── mipmap-*/                  # Ícone do app em múltiplas densidades
                │   └── values/
                │       ├── colors.xml             # Paleta de cores do projeto
                │       ├── strings.xml            # Strings do aplicativo
                │       └── themes.xml             # Tema visual
                └── AndroidManifest.xml            # Permissões e configuração do app
```
