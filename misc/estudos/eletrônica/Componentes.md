Aqui estará destacado os componentes eletrônicos do robô e uma breve descrição de como ele será usado:

## Lista preliminar de componentes – Projeto PCB (versão atualizada)

| Componente | Qtd | Descrição |
|----------|-----|-----------|
| ESP32 Dev Kit | 1 | É o microcontrolador principal, ele será usado pois tem rápido processamento, é de baixo custo, possui conectividade bluetooth e (principalmente) possui 2 núcleos para provessamento em paralelo.   |
| TB6612FNG | 1 | Ponte H para controle dos dois motores N20. Ela tem uma boa precisão dos comando PWM, uma corrente nominal copatível com os motores que estão sendo usados, boa integração com a ESP32 e também é de baixo custo.   |
| Array sensor de linha QTR-8RC | 1 |  É o sensor principal do projeto, ele é um array (conjunto) de 8 sensores de refletância infravermelho de alta precisão ideal para seguidores de linha. Além disso, já possui uma biblioteca própria, o que facilita o desenvolvimento do algoritmo de controle. |
| Módulo sensor infravermelho TCRT5000 | 2 | Sensores de refletância infravermelho utilizados para detecção de linhas laterais e marcações de início/fim de percurso. |
| Bateria LiPo 2S 7,4 V 20C 250 mAh | 1 | Baterias de LiPo são ótimas para sempre conseguir entregar a corrente que o circuito pede. O ideal é que tenha uma bateria para testes (geralmente maior que 500mAh) e outra para a competição (menor que 500mAh). |
| Motor N20 3000RPM | 2 | Motor principal de locomoção. É um micro motor com alto RPM e relativo alto torque, ideal para a locomoção do seguidor. |
| Motor coreless 8523 (7,4 V) | 1 | Motor para o sistema de sucção. É o mesmo tipo de motor usado em drones, eles são sem núcleo (coreless), assim conseguem atingir maiores RPM sem muitas dificuldades. É possível aplicar a técnica de overvoltage para aumentar ainda mais o RPM. |
| Transistor MOSFET IRLZ44N | 1 |Transistor de efeito de campo (MOSFET) para controle da turbina de sucção. Ela já possui boa integração com a ESP32 e com o motor Coreless.   |
| Diodo 1N4007 | 1 |Diodo de Flyback essencial para proteger o MOSFET contra picos de tensão reversos. Deve ser colocado em paralelo com o motor da turbina. |
| Regulador de tensão Mini 360 | 1 | Conversor DC-DC do tipo buck utilizado para reduzir a tensão da bateria LiPo 2S para níveis adequados de alimentação para a esp32. |
| Switch 3 vias | 1 | Chave utilizada para ligar e desligar o sistema. |
| Capacitor cerâmico 100 nF | 4 | Capacitores utilizados para desacoplamento e filtragem de ruído de alta frequência. Aplicações incluem os três motores e um para o driver. |
| Capacitor eletrolítico 47 µF | 1 | Capacitor utilizado segurar picos de correntes e evitar queda de tensão. |
| Resistor 220 Ω | 1 | Resistor utilizado entre a porta lógica e a porta do transistor. |
| Resistor 10 kΩ | 1 | Resistor utilizado para medirmos a quantidade de bateria restante . |
| Resistor 4,7 kΩ | 1 | Resistor utilizado para medirmos a quantidade de bateria restante. |
| Barra de pinos fêmea 1x40 vias | 1 | Barra de pinos utilizada para conexão da ponte h e a esp32. |
| Barra de pinos macho 1x40 vias | 1 | Barra de pinos utilizada para sensores e motores. |

***OBS**: neste arquivo você pode complementar com mais informações técnicas acerca das características dos componentes, como corrente máxima, instruções de como usar no circuito e etc.* 
