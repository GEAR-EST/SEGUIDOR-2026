FreeRTOS é um kernel de sistema operacional de tempo real (RTOS) muito utilizado em sistemas embarcados e especialmente sua integração com o ESP32 já é nativa. Ele tem diversas funções de kernel, mas vamos simplificar e utilizar o FreeRTOS apenas para conseguirmos utilizar os dois núcleos da ESP32 e poder desfrutar de seu total desempenho.

A ideia de usar dois núcleos da ESP é para: enquanto um fica processando apenas a questão da interface com o usuário e comunicação bluetooth (e suas consequências, como o controle da FSM, definição de estratégia e alteração de parâmetros), o outro núcleo ficaria exclusivo para o algoritmo PID e controle dos motores. Dessa forma, não sobrecarregando um núcleo e aproveitando o máximo desempenho da ESP32.

***OBS:** adicionar aqui mais informações de como isso deve ser feito, quais funções e cuidados que se deve tomar e etc*

# O que é Sistema Operacional?
Sistemas operacionais (SO), que tem exemplos conhecidos como Windows e Mac OS, são pedaços do software que rodam no computador. Existem diversas tarefas e aplicações de usuários rodando ao mesmo tempo, então o papel do sistema operacional é decidir quais partes devem ser divididas e por quanto tempo para que tudo rode plenamente. Além disso, o sistemas operacionais lidam com recursos virtuais, como arquivos, pastas para definir o acesso e lida com as entradas e saídas com dispositivos externos, como disco externo, teclado, mouse, monitor. 

Os sistemas operacionais focam na interação humana, sendo assim, prioriza as tarefas de acordo com essa condição, logo, tarefas, que não serão tão notadas pelo usuário, podem ser perdidas ou adiadas. Tudo isso acontece normalmente de forma não-determinística, ou seja, não se tem certeza de qual vai ser a tarefa executada e por quanto tempo. Entretanto, existem situações que dependem que as tarefas sejam executadas plenamente no prazo certo, e é aqui que entra o Sistema Operacional de Tempo Real (RTOS), para garantir que as múltiplas tarefas cumpram esses prazos de forma determinística.


Se, por um lado, o super loop comum do arduino (tarefas executadas de forma cíclica, dentro de loop infinito no `void loop()`, depois da execução de configurações do setup) economiza ciclos de CPU e memória e permite uma depuração mais fácil, por outro, as tarefas são sequenciais, então atrasos podem ser tornar comuns. Por isso, precisa-se de outras estratégias para rodar tarefas de precisão abaixo de 1ms.

# Por que usar RTOS?
O RTOS permite o gerenciamento de tarefas "simultaneas", como ler sensores e processar esses dados ao mesmo tempo, lida muito bem com wi-fi/bluetooth que exigem muito processamento e respostas rápidas e, por último, possibilita a prioridade de tarefas. Em outras palavras, ideial quando se precisa de simultaneidade e precisão de tempo. O RTOS mais popular para IoT é o FreeRTOS.

# Programação Dual-Core no FreeRTOS

A programação dual-core capacita rodar duas tarefas ao mesmo tempo nos dois núcleos físicos da ESP32. Por padrão, as funções `setup()` e ``loop()`` rodam no Core 1. Para usar o segundo núcleo, usa-se a função do FreeRTOS `xTaskCreatePinnedToCore()`
