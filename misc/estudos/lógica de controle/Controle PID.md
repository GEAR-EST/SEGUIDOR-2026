Falar sobre o controlador PID é bem complexo, pois ele é todo uma área de estudo da Engenharia de Controle e Automação, então vamos focar apenas nesse controle voltado à amostragem discreta, que é o caso do seguidor de linha.

Muito resumidamente ele usa constantes de proporcionalidade para fazer cálculos em cima do valor lido pelo sensor e retorna um valor de velocidade de cada motor para indicar se o robô deve ir para frente, para esquerda ou para a direita.

***OBS:** colocar mais informações da pesquisa sobre o controle PID, especialmente aplicado ao seguidor de linha. Seria interessante dar uma olhada no código do seguidor de 2025 (https://github.com/GEAR-projects/SEGUIDOR_PRO_2025/tree/main/line_follower_code)*

Além do controle PID, também é importante definir uma estratégia de [Mapeamento](./Mapeamento) de pista, para que o robô saiba quando ele pode acelerar, quando ele deve "freiar" para fazer curvas e etc.