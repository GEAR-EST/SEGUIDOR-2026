/**
 * @file UiStateHelper.kt
 * @brief Gerencia os estados visuais dos botões de controle conforme o estado do robô.
 *
 * Cada método `estadoXxx()` habilita ou desabilita os botões relevantes para
 * aquele estado, garantindo que o usuário só interaja com ações válidas no momento.
 *
 * @author Gear Robotics
 * @version 1.0
 */
package com.example.zeguiaapp

import android.widget.Button

/**
 * @class UiStateHelper
 * @brief Máquina de estados de UI: mapeia estado do robô → configuração dos botões.
 *
 * @param btnCalibrar            Botão de calibração dos sensores.
 * @param btnStartRun            Botão de início de corrida.
 * @param btnStopRun             Botão de parada de corrida.
 * @param btnModeFollower        Botão de seleção do modo Seguidor.
 * @param btnModeChase           Botão de seleção do modo Perseguidor.
 * @param btnStrategyConservative Botão de seleção da estratégia Conservadora.
 * @param btnStrategyRisk        Botão de seleção da estratégia Arriscada.
 * @param btnLerSensores         Botão para abrir o modal de leitura de sensores.
 * @param btnExportar            Botão para abrir o modal de exportação de resultados.
 */
class UiStateHelper(
    private val btnCalibrar: Button,
    private val btnStartRun: Button,
    private val btnStopRun: Button,
    private val btnModeFollower: Button,
    private val btnModeChase: Button,
    private val btnStrategyConservative: Button,
    private val btnStrategyRisk: Button,
    private val btnLerSensores: Button,
    private val btnExportar: Button
) {

    /**
     * @brief Habilita ou desabilita um botão com feedback visual via transparência.
     *
     * Botão desabilitado fica com alpha 0.3; habilitado com alpha 1.0.
     *
     * @param botao      Botão a ser configurado.
     * @param habilitado `true` para habilitar, `false` para desabilitar.
     */
    fun configurarBotao(botao: Button, habilitado: Boolean) {
        botao.isEnabled = habilitado
        botao.alpha     = if (habilitado) 1.0f else 0.3f
    }

    /**
     * @brief Estado: Bluetooth desconectado.
     *
     * Todos os botões ficam desabilitados para impedir comandos sem conexão.
     */
    fun estadoDesconectado() {
        configurarBotao(btnCalibrar,             false)
        configurarBotao(btnModeFollower,         false)
        configurarBotao(btnModeChase,            false)
        configurarBotao(btnStrategyConservative, false)
        configurarBotao(btnStrategyRisk,         false)
        configurarBotao(btnStartRun,             false)
        configurarBotao(btnStopRun,              false)
        configurarBotao(btnLerSensores,          false)
        configurarBotao(btnExportar,             false)
    }

    /**
     * @brief Estado: Bluetooth conectado, aguardando calibração ou seleção de modo.
     */
    fun estadoConectadoInicial() {
        configurarBotao(btnCalibrar,             true)
        configurarBotao(btnModeFollower,         true)
        configurarBotao(btnModeChase,            true)
        configurarBotao(btnLerSensores,          true)
        configurarBotao(btnStrategyConservative, false)
        configurarBotao(btnStrategyRisk,         false)
        configurarBotao(btnStartRun,             false)
        configurarBotao(btnStopRun,              false)
        configurarBotao(btnExportar,             false)
    }

    /**
     * @brief Estado: robô executando calibração dos sensores.
     *
     * Todos os botões de controle ficam bloqueados durante o processo.
     */
    fun estadoCalibrando() {
        configurarBotao(btnCalibrar,             false)
        configurarBotao(btnModeFollower,         false)
        configurarBotao(btnModeChase,            false)
        configurarBotao(btnLerSensores,          false)
        configurarBotao(btnStrategyConservative, false)
        configurarBotao(btnStrategyRisk,         false)
        configurarBotao(btnStartRun,             false)
        configurarBotao(btnStopRun,              false)
        configurarBotao(btnExportar,             false)
    }

    /**
     * @brief Estado: calibração concluída, pronto para seleção de modo.
     *
     * @param modoSelecionado Se `true`, os botões de estratégia também são habilitados.
     */
    fun estadoPosCalibracao(modoSelecionado: Boolean) {
        configurarBotao(btnCalibrar,             true)
        configurarBotao(btnModeFollower,         true)
        configurarBotao(btnModeChase,            true)
        configurarBotao(btnLerSensores,          true)
        configurarBotao(btnStrategyConservative, modoSelecionado)
        configurarBotao(btnStrategyRisk,         modoSelecionado)
        configurarBotao(btnStartRun,             false)
        configurarBotao(btnStopRun,              false)
        configurarBotao(btnExportar,             false)
    }

    /**
     * @brief Estado: modo de operação selecionado, aguardando escolha de estratégia.
     */
    fun estadoPosModo() {
        configurarBotao(btnCalibrar,             false)
        configurarBotao(btnModeFollower,         true)
        configurarBotao(btnModeChase,            true)
        configurarBotao(btnLerSensores,          true)
        configurarBotao(btnStrategyConservative, true)
        configurarBotao(btnStrategyRisk,         true)
        configurarBotao(btnStartRun,             false)
        configurarBotao(btnStopRun,              false)
        configurarBotao(btnExportar,             false)
    }

    /**
     * @brief Estado: estratégia selecionada, pronto para iniciar a corrida.
     */
    fun estadoPosEstrategia() {
        configurarBotao(btnCalibrar,             false)
        configurarBotao(btnModeFollower,         false)
        configurarBotao(btnModeChase,            false)
        configurarBotao(btnLerSensores,          true)
        configurarBotao(btnStrategyConservative, true)
        configurarBotao(btnStrategyRisk,         true)
        configurarBotao(btnStartRun,             true)
        configurarBotao(btnStopRun,              false)
        configurarBotao(btnExportar,             false)
    }

    /**
     * @brief Estado: corrida em andamento.
     *
     * Apenas o botão de parada fica disponível.
     */
    fun estadoCorrendo() {
        configurarBotao(btnCalibrar,             false)
        configurarBotao(btnModeFollower,         false)
        configurarBotao(btnModeChase,            false)
        configurarBotao(btnStrategyConservative, false)
        configurarBotao(btnStrategyRisk,         false)
        configurarBotao(btnStartRun,             false)
        configurarBotao(btnLerSensores,          false)
        configurarBotao(btnStopRun,              true)
        configurarBotao(btnExportar,             false)
    }

    /**
     * @brief Estado: corrida finalizada, resultados disponíveis para exportação.
     */
    fun estadoFinalizado() {
        configurarBotao(btnCalibrar,             true)
        configurarBotao(btnModeFollower,         true)
        configurarBotao(btnModeChase,            true)
        configurarBotao(btnStrategyConservative, true)
        configurarBotao(btnStrategyRisk,         true)
        configurarBotao(btnStartRun,             true)
        configurarBotao(btnStopRun,              false)
        configurarBotao(btnLerSensores,          true)
        configurarBotao(btnExportar,             true)
    }
}
