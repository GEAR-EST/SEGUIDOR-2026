# Proteção contra Ruído em Motores + Capacitores de Desacoplamento com base no driver TB6612FNG

Os motores Dc são componentes que quando liga/desliga, muda sua velocidade (PWM) e muda de sentido ele provoca variações bruscas na corrente  gerando ruído elétrico e esses ruídos podem aparecer tanto na parte do GND quanto na alimentação. Assim, esse ruído pode acabar provocando reset do microcontrolador(no caso a nossa esp32), leitura erradas dos sensores, falhas na comunicação e etc, ou seja, ele causa uma bagunça nos restos dos circuitos e como nosso robô vai utilizar sensores para buscar a linha na pista iremos precisar de bastante precisão nas leituras do robô. O nosso driver TB6612FNG (mini ponte H) também pode sofrer com as consequências desse ruído e gerando picos de corrente, queda de tensão e interferências vindo do motor, e no seu datasheet é pedido que haja capacitores externos para solucionar esses problemas. Então assim, podemos perceber que existem dois pontos de ruído diferentes uma pelo motor e outra pelo nosso driver.

## Resumo sobre o que é capacitor

Ele funciona como um pequeno reservatório de energia e libera essa carga muito rápido se for necessário o que pode ajudar a compensar as quedas altas de energia e filtra quando ruídos de alta frequência. A partir disso nasce os capacitores de desacoplamento que vão impedir que os ruído de uma parte do circuito se espalhe para o resto, eles ficam tipo no caminho segurar esse ruído e estabiliza a tensão local. 

## Capacitor deve ficar perto do driver TB6612FNG
 
O nosso driver possui duas alimentações principais, a VM que é a alimentação do motor que vai trabalhar com uma corrente mais alta sendo a principal causa de ruído elétrico e a outra é o VCC que já seria para as aimentações lógicas que trabalha com corrente mais baixas, porém bem sensível a ruído, por isso é necessário que cada uma dessas alimentações precisam de um próprio desacoplamento. E agora a gente entra em um ponto muito importante que é a posição dos capacitores, onde eles devem ficar próximos do driver já que se o capacitor fica muito longe a trilha acaba virando uma pequena indutância, o capacitor chega a perder sua eficiência e o ruído passa mesmo assim. Então a solução seria deixar eles perto do driver. Em relação ao VM é recomendável o uso de dois capacitoresem conjunto sendo um menor, de 100 nF, responsável por filtrar ruídos de alta frequência e um outro com um capacitor maior, como um eletrolítico de 47 µF ou mais, que seria um reservatório de energia ajudando a compensar as variações mais lentas e picos de energia causado pelos motores. Agora no VCC seria necessário apenas um capacitor de 100 nF sendo suficiente para filtrar os ruídos de alta frequência. Com isso, os capacitores devem ser colocados de maneira que um capacitor de 100 nF entre o VCC e o GND e os outros dois entre o VM e o GND (um outro de 100 nF e o outro eletrolítico), assim a gente consegue criar uma espécie de bolha de estabilidade. 

## Capacitores em paralelo com motores para evitar ruído

Agora voltando para as questões de ruído gerado por motores. Segundo dois vídeos que assistir, um que foi recomendado na minha sprint e outro que eu procurei, os profissionais utilizam também capacitores de desacoplamento ligados em paralelo com o motor com o objetivo de suavizar o ruído de alta frequência gerados pelas escovas do motor. Eles até chegam a mostrar como se comporta o ruído quando conectado a um osciloscópio sem e com capacitor onde o ruído é significativamente reduzido com a presença do capacitor. Geralmente é usado um valor de 100 nF cerâmico, como são dois motores vamos precisar de 2 capacitores cerâmicos desse para cada motor, mas também pode-se colocar um pra cada e isso de colocar capacitor no motor é algo praticamente obrigatório em motores DC se a gente quiser obter um melhor desempenho.

Links útei:

- https://youtu.be/SyvMY2W210w?si=iEPsFnJcAv3-gk9u
- https://youtu.be/m8Hokvhiea4?si=6zPDARyGz4C3PwGX
- https://youtu.be/6UIrAPS7gEs?si=fDjqzUWR_8Z1ou1l
