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

![FSM_Seguidor_e_Perseguidor_de_Linha](<images/FSM-Seguidor e Perseguidor de Linha.drawio.png>)
- *Desligado* - estado inicial e final, em que o robô está inativo eletronicamente.
- *Parado Ocioso* - estado ao ligar, parado esperado o próximo comando humano, sendo possível mudar parâmetros.
- *Parado Calibrado* - estado após a calibração feita.
- *Seguidor de Linha e Perseguidor* - Estados que representam os dois modos do robô.
- *Conservado e Arriscado* - Estados que definem a estratégia escolhida para o futuro momento do funcionamento do robô, que possuem paramêtros diferentes.
- *Corrida* - Estado do robô em movimento durante a volta.
- *Parado Concluído* - Termino da volta, seja por detecção da faixa de parada ou comando de parada.

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
- [GEAR: Capacitação Embarcados - ESP32 ](files/esp32_datasheet_en.pdf)
- [Datasheet ESP32](files/esp32_datasheet_en.pdf)
# Serial Bluetooth Monitor