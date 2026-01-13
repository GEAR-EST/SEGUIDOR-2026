> *OBS: Este arquivo (code/README.md) deve ser excluído, ele serve apenas como guia para entender como é organizado o este diretório (/code). Então mesmo que não tenha a seguir textos em itálico, este arquivo por completo deve ser excluído futuramente*

---

Este é um template de projeto padrão para desenvolvimento com [PlatformIO](https://platformio.org/). Ele fornece uma estrutura de pastas limpa e organizada, ideal para iniciar seus projetos de sistemas embarcados.

O objetivo deste template é promover as melhores práticas de organização de código, separando a lógica da aplicação, configurações, bibliotecas e testes.

## Estrutura de Diretórios

O projeto está organizado da seguinte forma:

```
code/
├── .pio/                       # Arquivos de compilação e firmware (gerado automaticamente, não está no git)
├── .vscode/                    # Configurações do editor Visual Studio Code (gerado automaticamente, não está no git)
├── include/                    # Arquivos de cabeçalho (.h) globais do projeto
├── lib/                        # Bibliotecas privadas (específicas do projeto). Para bibliotecas externas, use o arquivo `platformio.ini`
├── src/                        # Arquivos de código-fonte (.c, .cpp) da aplicação
├── platformio.ini              # Arquivo de configuração principal do PlatformIO
└── README.md --> ESTE ARQUIVO  # Organização do template
```

### Descrição

  * `├── .pio/`

      * Este diretório é gerenciado pelo PlatformIO e contém os arquivos de compilação, dependências baixadas e o firmware final (`.bin` ou `.hex`). **Ele deve ser ignorado pelo git, em [.gitignore](.gitignore).**

  * `├── .vscode/`

      * Contém as configurações específicas para o editor Visual Studio Code, como definições do IntelliSense. Também é gerenciado automaticamente.

  * `├── include/`

      * Use este diretório para arquivos de cabeçalho (`.h`, `.hpp`) que precisam ser compartilhados entre diferentes partes do seu código-fonte em `src/`. Um exemplo clássico é um arquivo `config.h` com definições de pinos e constantes globais.

  * `├── lib/`

      * Local ideal para bibliotecas internas e específicas do projeto. Cada biblioteca deve estar em sua própria subpasta (ex: `lib/MySensorLib/`). O PlatformIO automaticamente compila e vincula essas bibliotecas ao projeto. É perfeito para encapsular e reutilizar código. Se for uma biblioteca externa, usar o [`lib_deps`](https://docs.platformio.org/en/latest/projectconf/sections/env/options/library/lib_deps.html) dentro de [`platformio.ini`](platformio.ini)

  * `├── src/`

      * O coração da sua aplicação. O código-fonte principal (`.c`, `.cpp`) reside aqui. O arquivo `main.cpp` (ou `main.c`), que contém as funções `setup()` e `loop()`, é o ponto de entrada do programa.

  * `└── platformio.ini`
      * Este é o arquivo de configuração mais importante do projeto. Nele você define a placa em que está desenvolvendo (`board`), o framework (`framework`), as bibliotecas de que o projeto depende (`lib_deps`) e outras opções de configuração do projeto PlatformIO. Para saber quais configurações podem ser feitas, acesse: [https://docs.platformio.org/en/stable/projectconf/index.html](https://docs.platformio.org/en/stable/projectconf/index.html)