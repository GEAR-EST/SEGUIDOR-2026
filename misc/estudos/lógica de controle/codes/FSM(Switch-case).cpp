#include "BluetoothSerial.h"
#include <ctype.h> 

 // Dicionario das letras:
 // 'O' (ocioso) - parado esperando comando
 // 'K' (calibrar) - calibrando os sensore
 // 'M' (modo) - escolhendo entre 'S'(seguidor) e 'P' (perseguidor)
 // 'E'(estrategia) - escolhendo entre a estrategia 'C' (conservador) e 'A' (arriscado)
 // 'R'(Run/Correr) - executar o PID
 // 'F (fim) - Estado final (concluido)
 // 'I'(informação) - informação do estado atual 

BluetoothSerial SerialBT;

static char estado_atual = 'O';
int modo_atual = 0; //1 - seguidor; 2 - perseguidor

float kp = 0, ki = 0, kd = 0;
int velMax = 0;

void pararMotores() {
    // Serial.println("HARDWARE: Motores Parados");
}

void Calibrar() {
    Serial.println("HARDWARE: Calibrando sensores... (Simulado)");
    delay(1000); // Simula o tempo da calibração
}

void executarPID(float p, float i, float d, int vel) {
    // Imprime apenas de vez em quando para não travar o bluetooth
    static unsigned long lastPrint = 0;
    if (millis() - lastPrint > 2000) {
        Serial.printf("HARDWARE: PID Rodando | Kp: %.1f | Vel: %d\n", p, vel);
        lastPrint = millis();
    }
}

// Simula o sensor de chegada
bool detectouFaixaDeParada() {
    // Retorne 'true' aqui se quiser testar a parada automática do Seguidor
    return false; 
}

void ExecutarRobo()
{
    char msg_comando = '\0';

    if(SerialBT.available())
    {
        msg_comando = SerialBT.read();
        msg_comando = toupper(msg_comando);

        Serial.print("Recebido: "); 
        Serial.println(msg_comando);
    }

    if (msg_comando == 'I')
    {
        SerialBT.print("Estado: ");
        SerialBT.println(estado_atual);
        SerialBT.print("Modo: ");
        SerialBT.println(modo_atual);
        return;
    }

    switch(estado_atual)
    {
        case 'O': 
            pararMotores();
            if(msg_comando == 'K')
            {
                SerialBT.println("Calibrando...");
                estado_atual = 'K';
            }
            break;

        case 'K': 
            Calibrar();
            SerialBT.println("Calibrado. S ou P?");
            estado_atual = 'M';
            break;

        case 'M':
            pararMotores();
            if (msg_comando == 'S')
            { 
                modo_atual = 1;
                SerialBT.println("Modo: SEGUIDOR definido.");
                SerialBT.println("Seguidor. C ou A?");
                estado_atual = 'E';
            }
            else if (msg_comando == 'P') 
            {
                modo_atual = 2;
                SerialBT.println("Modo: PERSEGUIDOR definido.");
                SerialBT.println("Perseguidor. C ou A?");
                estado_atual = 'E';
            }
            break;

        case 'E':
            pararMotores();
            if (msg_comando == 'C')
            {
                kp = 1.0;
                velMax = 150;
                SerialBT.println("Estrategia: CONSERVADOR.");
                SerialBT.println("INICIANDO CORRIDA (Estado R)!");
                estado_atual = 'R';
            }
            else if (msg_comando == 'A')
            {
                kp = 1.0;
                velMax = 255;
                SerialBT.println("Estrategia: ARRISCADO.");
                SerialBT.println("INICIANDO CORRIDA!");
                estado_atual = 'R';
            }
            break;

        case 'R': 
            executarPID(kp, ki, kd, velMax);

            if (modo_atual == 1) 
            {
                if (detectouFaixaDeParada()) 
                {
                    pararMotores();
                    estado_atual = 'F'; 
                    SerialBT.println("Faixa detectada! Parando...");
                    pararMotores();
                    estado_atual = 'F';
                }
            }
            else if (modo_atual == 2) 
            {
                if (msg_comando == 'F')
                {
                    SerialBT.println("Comando de Parada recebido!");
                    pararMotores();
                    estado_atual = 'F';
                }
            }
            break;
        
        case 'F':
            pararMotores();
            if (msg_comando == 'S') 
            {
                modo_atual = 1; 
                SerialBT.println("Seguidor. C ou A?");
                estado_atual = 'E'; 
            }
            else if (msg_comando == 'P') 
            {
                modo_atual = 2; 
                SerialBT.println("Perseguidor. C ou A?");
                estado_atual = 'E'; 
            }
            else if (msg_comando == 'O') 
            {
                SerialBT.println("Ocioso.");
                estado_atual = 'O'; 
            }
            break;

        default:
            estado_atual = 'O';
            break;
    }
}

void setup() {
    Serial.begin(115200);
    
    SerialBT.begin("ROBO_TESTE"); 
    
    Serial.println("O sistema iniciou! Conecte o Bluetooth agora.");
}

void loop() {
    ExecutarRobo();
    delay(20); 
}