A interface é essencial para que não tenhamos que ficar reprogramando o robô para ter que mudar parâmetros, estratégias ou outras coisas assim. A ideia é usar o bluetooth da ESP32 conectada no celular para fazer essas alterações.

O projeto de 2025 utilizou o aplicativo Serial Bluetooth Monitor, para android, para essa finalidade e seu uso foi satisfatório, mas caso queira (e ache mais interessante), é possível usar outro aplicativo para fazer esse controle (ou até fazer um app para isso👀).

A interface humano computador deve:
- Alternar entre os modos Seguidor ou Perseguidor
- Alterar a FSM: parado, correndo, calibrando, etc
- Alterar os parâmetros PID: kp, ki e kd
- Alterar a estratégia de corrida (conservador, arriscado, etc

# Máquina de Estados Finita (FSM)

O robô precisa seguir determinada sequência de comandos e ações para o devido funcionamento, nesse aspecto, podemos representar isso por meio de uma máquina de Estados Finita. Cada estado define uma situação relevante do sistema, sendo possível avançar, recuar ou permanecer no estado conforme o fluxo da máquina. Consegue-se implementar uma FSM na linguagem de programação C, via `switch-case`, em que há uma variável de controle para armazenar o estado atual e cada ``case`` determina cada estado diferente. As principais informações foram retiradas desse site: [Máquinas de Estado - Embarcados](https://embarcados.com.br/maquina-de-estado/).

Foi feito o diagrama de Máquina de Estados Finita (FSM) no draw.io baseado, principalmente, nos projetos de robô seguidor de linha e perseguidor de linha da [equipe Raiju](https://raiju.team/projects/raijin) da USP e o projeto de 2025 do [G.E.A.R](https://github.com/GEAR-projects/SEGUIDOR_PRO_2025/).

![FSM_Seguidor_e_Perseguidor_de_Linha (v.2)](<images/FSM-Seguidor ePerseguidordeLinha(v3).jpg>)
- *Desligado* - estado inicial e final, em que o robô está inativo eletronicamente.
- *Parado* - estado ao ligar, parado esperado o próximo comando humano.
- *Parado Calibrado* - estado após a calibração feita.
- *Com Modo (Seguidor ou Perseguidor)* - Estado que representa a escolha entre os dois modos do robô.
- *Com Estratégia (Conservador e Arriscado)* - Estado que  define a estratégia escolhida para o futuro momento do funcionamento do robô, que possuem paramêtros diferentes.
- *Corrida* - Estado do robô em movimento durante a volta.
- *Parado Concluído* - Termino da volta, seja por detecção da faixa de parada ou comando de parada.
- *Recalibrado* - Possibilidade de recalibração após parar.

O diagrama de FSM pode ser encontrado no draw.io, [aqui](https://drive.google.com/file/d/1oaSej4jX4JBlGdejw2AE11unm4lenKVx/view?usp=sharing).

O esqueleto do código da FSM pode ser encontrado [aqui](codes/FSM(Switch-case).cpp).

# Bluetooth no ESP32
O Bluetooth é um tecnologia robusta, de baixo consumo e custom, que opera de forma sem fio (wireless) para troca de dados em curtas distâncias. O Esp32 é Bluetooth dual-mode, podendo operar tanto na categoria Bluetooth Clássico e Bluetooth Low Energy (BLE). 

O Bluetooth Clássico foi projeto para conexões contínuas e fluxos de dados pesados, por exemplo, áudio e arquivos grandes. Já o BLE foi projetado para ser duradouro de consumo baixissimo, enviando pequenos pacotes de dados.

Baseado nas informações dessa
[versão da ESP32](https://www.robocore.net/wifi/esp32-wifi-bluetooth?srsltid=AfmBOop8iqMsq-R0HSUzLCX_tMgmTOFL_HXxRb80_gBCo7PlyySzLXxj) vendida pela Robocore, com Wifi + Bluethooth.

- UART HCI Interface: até 4 Mbps

    Comunicação rápida entre o núcleo que opera o Bluetooth e o rádio, quase instantânea.

- Segue a versão 4.2 do padrão Bluetooth.

Arquivos utéis:
- [GEAR: Capacitação Embarcados - ESP32](<files/Capacitação Embarcados - ESP32.docx.pdf>)
- [Datasheet ESP32](files/esp32_datasheet_en.pdf)

# Serial Bluetooth Monitor

A seguir, está o exemplo padrão e em domínio público da biblioteca BluetoothSerial, encontrado no Arduino IDE, feito por Evandro Copercini (2018), para analisarmos e saber como conectar o ESP32 com o aplicativo para android Serial Bluetooth Monitor.

```c

#include "BluetoothSerial.h"

String device_name = "ESP32-BT-Slave";

// Check if Bluetooth is available
#if !defined(CONFIG_BT_ENABLED) || !defined(CONFIG_BLUEDROID_ENABLED)
#error Bluetooth is not enabled! Please run `make menuconfig` to and enable it
#endif

// Check Serial Port Profile
#if !defined(CONFIG_BT_SPP_ENABLED)
#error Serial Port Profile for Bluetooth is not available or not enabled. It is only available for the ESP32 chip.
#endif

BluetoothSerial SerialBT;

void setup() {
  Serial.begin(115200);
  SerialBT.begin(device_name);  //Bluetooth device name
  //SerialBT.deleteAllBondedDevices(); // Uncomment this to delete paired devices; Must be called after begin
  Serial.printf("The device with name \"%s\" is started.\nNow you can pair it with Bluetooth!\n", device_name.c_str());
}

void loop() {
  if (Serial.available()) {
    SerialBT.write(Serial.read());
  }
  if (SerialBT.available()) {
    Serial.write(SerialBT.read());
  }
  delay(20);
}

```
Analisando parte por parte o código:

```c

#include "BluetoothSerial.h" // importa a biblioteca responsável pela conexão Bluetooth Classic no ESP32

String device_name = "ESP32-BT-Slave"; //Nome do dispositivo que vai aparecer quando conectar dispositivos bluetooth
```
```c

#if !defined(CONFIG_BT_ENABLED) || !defined(CONFIG_BLUEDROID_ENABLED)
#error Bluetooth is not enabled! Please run `make menuconfig` to and enable it
#endif 
//Verificação de segurança do chip interno do esp32 para permitir o bluetooth de funcionar, se der erro, interrompe a compilação.

// Check Serial Port Profile
#if !defined(CONFIG_BT_SPP_ENABLED)
#error Serial Port Profile for Bluetooth is not available or not enabled. It is only available for the ESP32 chip.
#endif 
//Funciona de forma análoga ao anterior,  mas aqui é focado em permitir que o bluetooth se comporte como uma porta serial comum.

```

```c
BluetoothSerial SerialBT; //Instancia o objeto

void setup() {
  Serial.begin(115200); // Inicializa a comunicação serial via USB a 115200 bits por segundo.
  SerialBT.begin(device_name);  
  //Inicia o rádio Bluetooth com o nome do inicio, ou seja, nesse caso, ESP32-BT-Slave
  Serial.printf("The device with name \"%s\" is started.\nNow you can pair it with Bluetooth!\n", device_name.c_str()); //Imprime mensagem no monitor serial.
}
```
```c
void loop() {
  if (Serial.available()) {
    SerialBT.write(Serial.read());
  } 
  //Dados enviado do USB (Serial) para o dispositivo conectado pelo Bluetooth (SerialBT), escreve no celular o que leu no monitor do computador
  if (SerialBT.available()) {
    Serial.write(SerialBT.read());
  } 
  //Dados enviados do dispositivo conectado pelo Bluetooth (SerialBT) para o USB (Serial), escreve no computador o que leu no celular.
  delay(20);
}
```

Upe o código no ESP32 físico e ajuste para baud rate de 115200 no Monitor Serial do computador. Liga bluetooth no aparelho android. Após isso, encontrar em devices (dispositivos) o nome do bluetooth do ESP32 pra se conectar, nesse exemplo, ESP32-BT-Slave. Depois de se conectar, o terminal do aplicativo e o monitor serial podem se comunicar.

# Aplicativo com Kotlin 
## Informações utéis
- [Conectividade Bluetooth](https://developer.android.com/develop/connectivity/bluetooth?hl=pt-br)