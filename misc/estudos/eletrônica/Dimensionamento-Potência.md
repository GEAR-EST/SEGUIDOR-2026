# Dimensionamento de Potência em PCB
Para esse estudo, primeiramente eu busquei primeiro entender mais sobre o assunto pois quando eu fui assistir um vídeo no youtube eu percebi que não conseguia entender certos termos e nomes que eram mencionados, então para esse estudo vou começar desde o básico e assim vou avançando no asssunto até chegar na parte que é desejada para o estudo

## Largura de Trilhas para Alta Corrente

Primeiramente dentro das placas PCB existem trilhas de cobre que seria como fios desenhados na placa e a diferença é que os fios são redondos e as trilhas já são achatadas e finas. Quando uma corrente elétrica passa por qualquer condutor (trilha) o cobre acaba oferecendo mais resistência e quando temos Resistência + Corrente = calor, assim quanto mais corrente mais calor temos e assim podemos ter as trilhas finas e as trilhas largas. Por exemplo, se fosse ligado um motor que puxa 5 amperes e essa corrente passa por uma trilha muito fina pode acabar acontecendo de a trilha esquentar muito e assim descolando o cobre da placa, queimar o verniz e causar falha intermitente. Por isso é necessário não apenas desenhar a trilha, mas também dimensiona-la. Ou seja, primeiro escolhemos o quanto de corrente vai passar e quanto de aquecimento é aceitável e depois definimos a largura da trilha.

## 4 coisas necessárias para fazer o cálculo

A primeira é a corrente, que ja é bem intuitivo onde por exemplo um LED que recebe uma corrente de 20mA requer apenas uma trilha fininha, mas já para um motor que que requer uma corrente de 2A a 10A já requer uma trilha bem mais larga. A segunda já seria a espessura do cobre (parte muito importante) onde normalmente ele possui valores como 1 oz (ounce) onde o cobre é fino sendo esse o valor padrão, 2 oz onde o cobre é mais grosso e 3 oz onde o cobre é bem grosso, também pesquisei que a partir de 2 oz no site da JLC a placa ja sairia por quase R$100,00 enquanto a de 1 oz sairia por R$10,00, assim quanto mais grosso o cobre mais corrente aguenta. A terceira já leva em consideração onde a trilha está, se está na parte externa onde o calor é mais dissipado ou se está na parte interna onde o calor fica mais preso e consequentemente esquenta mais, então para as trilhas internas é necessário que elas sejam mais largas e enquanto as trilhas externas podem ser um pouco mais estreitas. E Por último o quarto que diz a respeito ao quanto de calor a placa aguenta onde normalmente se usa 10° C ou 20° C e se por acaso o ambiente está a 25 ° C a trilha pode chegar a 35° C ou 45° C, uma informação útil é que a placa que usariamos se pedirmos da JLC que suporta até uma temperatura de 135° C . 

## Como fazer o cálculo?

Fazemos o cálculo baseado numa norma chamada IPC-221 que é uma norma internacional que define os parâmetros que foram indicados anteriomente. Segundo um vídeo que eu assistir pra realizar o cálculo podemos utilizar sites e aplicativos que nos dão os resultados que  direto. Primeiramente foi mostrado o site Sierra Circuit que basta você colocar os parâmetros pedidos e ele ja te retorna os valores da largura da trilha que devemos usar e a capacidade de corrente máxima da trilha, além disso podemos calcular a perda de corrente devido ao comprimento que a trilha pode ter então basta colocar o tamanho da trilha que ele já vai retornar o valor da perda, assim é bom que a trilha seja o mais curto possível. O aplicativo apresentado foi o electrodoc que já é bem mais simples e intuitivo que também faz o mesmo cálculo, mas os resultados são válidos até uma corrente de 35A. Uma vantagem desse aplicativo é que podemos fazer o cálculo para as trilhas tanto da parte interna e externa de maneira separadas, assim facilitando a visualização em relação à calculadora do site.

## Técnica para diminuir a trilha e aumentar a capacidade de corrente 

A téncica vista no vídeo consiste o seguinte, temos uma trilha externa (Top) na camada de cima indo da fonte até o componente, agora fazemos uma outra trilha só que na camada de baixo (Bottom) seguindo o mesmo caminho da trilha de cima conectando por fim nos mesmos pontos, o importante é que ela fica embaixo, paralela à trilha de cima. Agora, colocamos várias vias ao longo do percusso da trilha ligando as duas trilhas. Com isso, temos que a corrente se divide entre a trilha superior e trilha inferior, ou seja, as duas trilhas passam a funcionar como uma trilha muito mais grossa.

links úteis: 
- https://youtu.be/B2n9fwoOa6U?si=KViPhgb7k_YMhNGQ




