# Corrente e tensão do motor coreless 8523 (7.4 V)

## O que seria um motor coreless?

Um motor coreless é um tipo de motor DC que não possui núcleo de ferro interno e osso faz com que ele seja mais leve, tenha uma menor inércia e consiga atingir rotações muito altas rapidamente. Esse tipo de motor é muito utilizado em asas de drones, em mini ventoinhas e sistemas de sucção. No caso do nosso robô ele será utilizado como ventoinha para o sistema de sucção do robô. As especificações dos valores típicos do motor coreless 8523 podem variar por fabricante, mas ao pesquisar vi que os valores normalmente são:

- Tensão nominal: 7.4 V
- Corrente em vazio: 72 mA a 80 mA
- Corrente nominal: 800 mA em operação eficiente
- Corrente de pico/ stall: 2 A até 6 A dependendo do modelo

Como o motor puxa muita corrente e possui um alto valor de corrente de pico é exigido que em nosso circuito tenha um transistor Mosfet, diodo flyback, trilha largas e até mesmo o uso de  capacitores.

# Como usar o MOSFET IRLZ44N com o motor coreless

Primeiramente, a gente nunca deve ligar o motor direto na esp32 pois como ele puxa muita corrente gera picos muito alto, Se ligarmos direto a esp32 pode ficar resetando ou até mesmo queimar a placa toda e até o motor nem funcionar direito então por isso que usamos o mosfet

## O que é o Mosfet IRLZ44N?

Ele é basicamente um transistor eletrônico que funciona como um interruptor controlado por sina que permite usar um sinal fraco da ESP32 para controlar uma carga forte do motor. No nosso caso a nossa esp32 manda um comando e o Mosfet pode liga ou desliga o motor e o motor puxa corrente direto da bateria. A sua estrutura básica da ligação consiste em 3 terminais:

- Gate --> Ele é como se fosse o “botão” do Mosfet onde ele recebe um sinal da ESP32 e decide se o MOSFET liga ou desliga. Quando a ESP32 envia tensão no Gate o Mosfet liga e permite a passagem de corrente entre os outros dois terminais Drain e Source, porém quando nada é enviado o mosfet desliga/bloqueia corrente. Então resumindo ele é o interruptor eletrônico.
  
- Drain --> Ele é ligado ao motor e quando mosfet liga a corrente do motor passa por ele sendo ela a corrente mais pesada e assim ele se torna um ponto crítico no circuito exigindo um certo cuidado onde a trilha que se liga ao pad dele precisa se larga para evitar um aquecimento e ela se desfazer

- Source --> Ele já seria um terminal que é ligado ao GND do circuito onde ele fecha o caminho da corrente e permite que o motor funcione. Sem o nosso Source conectado nada funciona

OBS: O Gate é um terminal muito sensível então ele precisa de um resistor de proteção (220 ohm) e outro de pull-down (10k ohm) sem isso ele pode acabar se ligando sozinho, captar ruído e causar um comportamento estranho.

"Gate controla, Drain conduz e Source fecha o circuito"

## Onde isso entra em nosso robô?

Basicamente ele é o intermediador entre a esp32 e o motor e também o responsável por controlar a ventoinha de sucção e sem ele não conseguimos alimentar o robô, nem controlar a velocidade e nem proteger o sistema. Alguns pontos imporantes inclui deixar o GND bem conectado, deixar o Mosfet próximo do motor e ter uma dissipação térmica bem considerada

# Diodo de flyback (proteção do mosfet e do circuito)

Primero, os motores não são cargas normais onde dentre deles existem bobinas, campo magnético e energia armazenada. Quando o motor fica ligado a corrente passa pela bobina e cria um campo magnético e quando você desliga ele o campo magnético colapsa e com isso a bobina tenta manter a corrente, porém ao fazer isso é gerado uma tensão reversa muito alta o que pode acabar queimando o mosfet, travar a esp32, gera ruído no circuito (nosso arqui inimigo) e reduzir a vida útil dos componentes e isso acontece toda vez que o motor desliga principalmente com PWM.

