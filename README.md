# *Project Template V2*

*Este repositório serve como guia para organização dos projetos desenvolvidos no GEAR. Tudo o que estiver em itálico são informações para como preencher o template e deverá ser excluído na entrega final do projeto. Alguns arquivos README podem ter a observação de que eles devem ser apagados, quando isso ocorrer, mesmo que um texto não esteja em itálico, o arquivo como um todo (README.md) deve ser apagado*

> *OBS: deve ser eleita uma pessoa para ser a responsável pelo GitHub de sua modalidade (preferencialmente o Líder da equipe, ou quem já tem mais experiência com a plataforma), essa pessoa deve auxiliar os membros a como preecnher as informações neste Template, o processo de configuração de ambiente local, ler e saber todas as regras deste Template e modificar/apagar os aquivos que tem a observação em itálico.*

> *OBS 2: é de extrema importância que todos leiam e apliquem os Padrões de Desenvolvimento, que está na Área de Membros (View as: Member) da Organização: https://github.com/GEAR-EST. Caso não esteja conseguindo acessar, entre em contato com o Líder de Embarcados.*

## *Estrutura do Template*

*Este template fornece uma estrutura organizada para desenvolvimento de robôs no GEAR, com diretorios específicos para cada aspecto do projeto. Lembrete: tudo o que estiver em itálico é para ajudar você, membro do GEAR a preecher corretamente o Template e deverá ser excluído futuramente.*

# *Insira aqui o nome do repositório. Ex: ARTBOT-2026*

## Resumo Geral

*Insira aqui um resumo geral sobre o robô, a modalidade e a competição. Resumo de 100 a 150 palavras.*

## Organização dos Diretórios

*Cada diretório contém um README.md com instruções de uso, dependências e explicações específicas. Ainda seguem o padrão de se estiver em itálico, deve ser apagado. O snippet de código abaixo deve permanecer no texto final com as alterações no NOME_DO_REPOSITÓRIO e em OPCIONAL.*

*Idealmente, a divisão da equipe para ficar responsável pelo seu diretório vai de cada cargo do membro. Fica assim a divisão:* 
- *`/code`: membro de **Embarcados** (programação);* 
- *`/docs`: **todos**, cada um com sua área técnica específica*
- *`/electronics`: membro de **Embarcados** (eletrônica);* 
- *`/mechanics`: membro de **Mecânica**;*
- *`/misc`: **qualquer um** que precisar;*
- *`/simu`: membro de **Embarcados** (programação), se for necessário;*

> *OBS: os Gestores de Equipe precisam apenas acompanhar o time para saber se o GitHub está sendo preenchido no decorrer das Sprints, não sendo necessário preencher informações técnicas. Se necessário, pode contribuir na descrição do robô e modalidade.*

```
*NOME_DO_REPOSITÓRIO*/
├── code/                       # Toda a implementação de algoritmos
│
├── docs/                       # Documentação técnica do projeto
│
├── electronics/                # Toda a parte eletrônica do robô
│
├── mechanics/                  # Toda a parte mecânica do robô
│
├── misc/                       # Miscelânea, arquivos sem categoria definida (*OPCIONAL, se não for usado, deve ser apagado*)
│
├── simu/                       # Arquivos de simulação (*OPCIONAL, se não for usado, deve ser apagado*)
│
└── README.md --> ESTE ARQUIVO  # Resumo geral e organização dos diretórios
```

Para saber mais sobre o projeto, acesse a documentação em [/docs](/docs/).