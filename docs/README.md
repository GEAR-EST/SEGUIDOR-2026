# SEGUIDOR-2026

## Introdução

O projeto se trata de um robô Seguidor de Linha de alto desempenho utilizado para a competição Robocore Experience. *Adicionar mais informações sobre o robô. Não ultrapassar 100 palavras.*

<!-- ![Nome do robô](img/nome_do_arquivo.png)  -->
<!-- **Figura 1:** *(nome do robô)* -->

## 1. Bill of Materials (BOM)

Na tabela a seguir será possível observar a lista com todos os componentes utilizados para a construção do robô:

|               | **Bill of Materials - BOM**               |               |
|:--:           |:--:                                       |:--:	        |
| Nome          | Explicação                                | Quantidade    |
|               | **Baterias e componentes relacionados**   |               |
| Bateria X     | Bateria 3S de 11.1V                       | qtd     	    |
|               |                                           |               |
|               | **Componentes eletrônicos**               |               |
| Sensor X      | Sensor para X coisa                       | qtd     	    |
| Componente Y  | Componente para Y coisa                   | qtd     	    |
|               |                                           |               |
|               | **Estrutura mecânica**                    |               |
| Engrenagem A  | Engrenagem para o GEARbox                 | qtd     	    |
|               |                                           |               |
|               | **Outros componentes**                    |               |
| Botão         | Botão para apertar                        | qtd     	    |

## 2. Modelo 3D

*Inserir imagens, explicações e evolução do [Modelo 3D](../mechanics) do robô*

## 3. Esquemático eletrônico

*Inserir imagens, explicações e evolução do [Circuito Eletrônico](../electronics)*

## 4. Lógica do robô

*Inserir explicações e evolução da lógica do robô e da [Programação](../code). Se achar necessário, pode adicionar diagramas para explicar a lógica de funcionamento, como um fluxograma ou máquina de estados*

## 5. Interface de Controle

O robô é controlado remotamente pelo **ZeGuiaApp**, um aplicativo Android nativo desenvolvido em **Kotlin**, que se comunica com a ESP32 via **Bluetooth Classic (SPP/RFCOMM)**. O app permite ao operador calibrar os sensores, selecionar modo e estratégia, ajustar os parâmetros PID, iniciar e finalizar corridas, monitorar a leitura dos sensores em tempo real e exportar um resumo de performance ao final de cada corrida. A documentação completa da interface está em [interface/README.md](../interface/README.md).

## 5. Resultados

*Inserir imagens e vídeos (link do youtube) do robô funcionando, em competição e foto da equipe completa*
