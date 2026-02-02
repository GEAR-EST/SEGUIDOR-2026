#include <stdio.h>
#include <stdlib.h>

#define ESTADO_OCIOSO          0
#define ESTADO_CALIBRADO       1
#define MODO_SEGUIDOR          2
#define MODO_PERSEGUIDOR       3
#define ESTRATEGIA_CONSERVADOR 4
#define ESTRATEGIA_ARRISCADO   5
#define CORRIDA_SEGUIDOR       6
#define CORRIDA_PERSEGUIDOR    7
#define ESTADO_CONCLUIDO       8

volatile char E_Atual = ESTADO_OCIOSO;
int modoAtivo = 0;

//parametros
float kp, ki, kd;
int velMax;

void ExecutarRobo()
{
    switch(E_Atual)
    {
        case ESTADO_OCIOSO:
            pararMotores();
            if(btn_calibrar())
            {
                Calibrar();
                E_Atual = ESTADO_CALIBRADO;
            }
            break;

        case ESTADO_CALIBRADO:
            if (selSeguidor())
            {
                 modoAtivo = 1;
                 E_Atual = MODO_SEGUIDOR;
            }
            else if (selPerseguidor())
            { 
                modoAtivo = 2;
                E_Atual = MODO_PERSEGUIDOR;
            }
            break;
        
        case MODO_SEGUIDOR:
        case MODO_PERSEGUIDOR:
        if (selConservador()) E_Atual = ESTRATEGIA_CONSERVADOR;
        if (selArriscado()) E_Atual = ESTRATEGIA_ARRISCADO;
        break;

        case ESTRATEGIA_CONSERVADOR;
            kp = 1.0;
            velMax = 150;
            E_Atual = (modoAtivo == 1) ? CORRIDA_SEGUIDOR : CORRIDA_PERSEGUIDOR;
            break;

        case ESTRATEGIA_ARRISCADO:
            kp = 1.0;
            velMax = 255;
            E_Atual = (modoAtivo == 1) ? CORRIDA_SEGUIDOR : CORRIDA_PERSEGUIDOR;
            break;

        case CORRIDA_SEGUIDOR:
            executarPID(kp, ki, kd, velMax);
            if (detectouFaixaDeParada())
            {
                E_Atual = ESTADO_CONCLUIDO;
            }
            break;

        case CORRIDA_PERSEGUIDOR:
            executarPID(kp, ki, kd, velMax);
            if (comandoParada())
            {
                E_Atual = ESTADO_CONCLUIDO;
            }
            break;
        
        case ESTADO_CONCLUIDO:
            pararMotores();
            esperar(10000); 
            if (selSeguidor()) {
                modoAtivo = 1;
                E_Atual = MODO_SEGUIDOR;
            } else if (selPerseguidor()) {
                modoAtivo = 2;
                E_Atual = MODO_PERSEGUIDOR;
            } else {
                E_Atual = ESTADO_OCIOSO;
            }
            break;

        default:
            estadoAtual = ESTADO_OCIOSO;
            break;

    }
}