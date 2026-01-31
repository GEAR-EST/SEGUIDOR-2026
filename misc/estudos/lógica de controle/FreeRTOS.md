FreeRTOS é um kernel de sistema operacional de tempo real (RTOS) muito utilizado em sistemas embarcados e especialmente sua integração com o ESP32 já é nativa. Ele tem diversas funções de kernel, mas vamos simplificar e utilizar o FreeRTOS apenas para conseguirmos utilizar os dois núcleos da ESP32 e poder desfrutar de seu total desempenho.

A ideia de usar dois núcleos da ESP é para: enquanto um fica processando apenas a questão da interface com o usuário e comunicação bluetooth (e suas consequências, como o controle da FSM, definição de estratégia e alteração de parâmetros), o outro núcleo ficaria exclusivo para o algoritmo PID e controle dos motores. Dessa forma, não sobrecarregando um núcleo e aproveitando o máximo desempenho da ESP32.

***OBS:** adicionar aqui mais informações de como isso deve ser feito, quais funções e cuidados que se deve tomar e etc*

# O que é Sistema Operacional?
Sistemas operacionais (SO), que tem exemplos conhecidos como Windows e Mac OS, são pedaços do software que rodam no computador. Existem diversas tarefas e aplicações de usuários rodando ao mesmo tempo, então o papel do sistema operacional é decidir quais partes devem ser divididas e por quanto tempo para que tudo rode plenamente. Além disso, o sistemas operacionais lidam com recursos virtuais, como arquivos, pastas para definir o acesso e lida com as entradas e saídas com dispositivos externos, como disco externo, teclado, mouse, monitor. 

Os sistemas operacionais focam na interação humana, sendo assim, prioriza as tarefas de acordo com essa condição, logo, tarefas, que não serão tão notadas pelo usuário, podem ser perdidas ou adiadas. Tudo isso acontece normalmente de forma não-determinística, ou seja, não se tem certeza de qual vai ser a tarefa executada e por quanto tempo. Entretanto, existem situações que dependem que as tarefas sejam executadas plenamente no prazo certo, e é aqui que entra o Sistema Operacional de Tempo Real (RTOS), para garantir que as múltiplas tarefas cumpram esses prazos de forma determinística.


Se, por um lado, o super loop comum do arduino (tarefas executadas de forma cíclica, dentro de loop infinito no `void loop()`, depois da execução de configurações do setup) economiza ciclos de CPU e memória e permite uma depuração mais fácil, por outro, as tarefas são sequenciais, então atrasos podem ser tornar comuns. Por isso, precisa-se de outras estratégias para rodar tarefas de precisão abaixo de 1ms.

Informações retiradas do seguinte vídeo.

# Por que usar RTOS?
O RTOS permite o gerenciamento de tarefas "simultaneas", como ler sensores e processar esses dados ao mesmo tempo, lida muito bem com wi-fi/bluetooth que exigem muito processamento e respostas rápidas e, por último, possibilita a prioridade de tarefas. Em outras palavras, ideial quando se precisa de simultaneidade e precisão de tempo. O RTOS mais popular para IoT é o FreeRTOS.

# Programação Dual-Core no FreeRTOS

A programação dual-core capacita rodar duas tarefas ao mesmo tempo nos dois núcleos físicos da ESP32. Por padrão, as funções `setup()` e ``loop()`` rodam no Core 1. Para usar os núcleos separadamente, usa-se a função do FreeRTOS `xTaskCreatePinnedToCore()` dentro do `setup ()` conforme a [documentação encontrada aqui](https://docs.espressif.com/projects/esp-idf/en/v4.3/esp32/api-reference/system/freertos.html). É possível saber em qual núcleo determinada tarefa está sendo executada pela funcão `xPortGetCoreID()`.

---

```c
xTaskCreatePinnedToCore(
    Task1Code,     // Função da tarefa
    "Tarefa 1",    // Nome da tarefa
    2000,          // Tamanho da stack (bytes)
    NULL,          // Parâmetros de entrada
    2,             // Prioridade
    &Task1,        // Referência para Handle da tarefa
    0              // Núcleo (Core 0)
);
```



# Parâmetros
1) __Função da Tarefa (pvTaskCode)__: Nome da função que contém o código que a tarefa vai executar.

    *Exemplo*: `void Task1Code (void*pvParameters)` que é formato padrão exigido pelo FreeRTOS para definir a função de uma tarefa. 

    *Cuidado*: deve ter um loop infinito, como `for(;;)`, ou terminar com um ``vTaskDelete(NULL)`` para o sistema não travar.

2) __Nome (pcName)__: String de texto para facilitar identificação e debug.

    *Cuidado:* limite padrão de 16 caracteres.

3) __Tamanho da Stack__ (usStackDepth): o quanto de RAM, em bytes, será reservada para as variáveis locais da tarefa.

    *Cuidado:* um valor muito baixo, como >2048 em tarefas complexas, o sistema pode reiniciar por "stack overflow".

4) __Paramêtros de entrada (pvParameters)__: Ponteiro para qualquer dado de entrada, sendo 'NULL' se não precisar.

5) __Prioridade (uxPriority)__: quanta maior o número, maior a prioridade para rodar a tarefa no núcleo.

6) __Handle da Tarefa (pvCreatedTask)__: Ponteiro que guarda o handle, a identidade da tarefa criada. Handle significa basicamente "alça", então seria uma analogia para a tarefa sendo a mala. Então o handle é um identificador para um objeto interno dentro do FreeRTOS.

7) __Núcleo/Core (xCoreID)__: define em qual núcleo a tarefa vai rodar. Na Esp32, pode ser no core 0 ou 1.

    *Observação:* se botar ``tskNO_AFFINITY`` O sistema decide onde a tarefa vai estar, podendo pular de um núcleo para o outro conforme a carga de processamento.

# Código teste com FreeRTOS

Realizei uma simulação no simulador online *Wowki* para testar um código usando FreeRTOS para rodar tarefas independentes nos dois núcleos do ESP32. Baseei-me no [seguinte vídeo](https://youtu.be/V-RGB5yem-Q?si=ChZkxIBFHI3l-eHI), para realizar a simulação do BLINK de dois LEDs, cada um num dos núcleos. O código do teste está [aqui](codes/Blink_Dual_Core_ESP32.cpp). A simulação no Wowki está [aqui](https://wokwi.com/projects/454621177912524801).

![GIF_BLINK_DUAL_CORE](<images/FreeRTOS Dual-core Programming-1.gif>)





