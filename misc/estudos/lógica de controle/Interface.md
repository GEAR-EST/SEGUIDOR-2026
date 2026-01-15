A interface é essencial para que não tenhamos que ficar reprogramando o robô para ter que mudar parâmetros, estratégias ou outras coisas assim. A ideia é usar o bluetooth da ESP32 conectada no celular para fazer essas alterações.

O projeto de 2025 utilizou o aplicativo Serial Bluetooth Monitor, para android, para essa finalidade e seu uso foi satisfatório, mas caso queira (e ache mais interessante), é possível usar outro aplicativo para fazer esse controle (ou até fazer um app para isso👀).

A interface humano computador deve:
- Alternar entre os modos Seguidor ou Perseguidor
- Alterar a FSM: parado, correndo, calibrando, etc
- Alterar os parâmetros PID: kp, ki e kd
- Alterar a estratégia de corrida (conservador, arriscado, etc
