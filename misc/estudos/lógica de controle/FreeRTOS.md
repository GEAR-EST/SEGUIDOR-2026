FreeRTOS é um kernel de sistema operacional de tempo real (RTOS) muito utilizado em sistemas embarcados e especialmente sua integração com o ESP32 já é nativa. Ele tem diversas funções de kernel, mas vamos simplificar e utilizar o FreeRTOS apenas para conseguirmos utilizar os dois núcleos da ESP32 e poder desfrutar de seu total desempenho.

A ideia de usar dois núcleos da ESP é para: enquanto um fica processando apenas a questão da interface com o usuário e comunicação bluetooth (e suas consequências, como o controle da FSM, definição de estratégia e alteração de parâmetros), o outro núcleo ficaria exclusivo para o algoritmo PID e controle dos motores. Dessa forma, não sobrecarregando um núcleo e aproveitando o máximo desempenho da ESP32.

***OBS:** adicionar aqui mais informações de como isso deve ser feito, quais funções e cuidados que se deve tomar e etc*
