/*
    TELA: SCF0155 - CAIXA FINANCEIRO
    FUNÇÃO:
    1. Insere um botão para abrir a tela de vincular pré-venda
 */
package scripts

import multitec.swing.core.MultitecRootPanel
import multitec.swing.core.utils.WindowUtils
import sam.swing.ScriptBase
import sam.swing.tarefas.spv.SPV1050

import javax.swing.JButton

class SCF0155 extends ScriptBase{
    MultitecRootPanel tarefa;
    @Override
    void execute(MultitecRootPanel panel) {
        this.tarefa = panel;
        inserirBtnAbrirTelaVincularPreVenda();
    }

    private void inserirBtnAbrirTelaVincularPreVenda(){
        JButton btnAbrirPreVenda = new JButton();
        btnAbrirPreVenda.setBounds(1210, 0, 150, 40);
        btnAbrirPreVenda.setText("Abrir Vinc. Pré-Venda");
        btnAbrirPreVenda.addActionListener(e -> abrirTelaVincularPreVenda());
        tarefa.add(btnAbrirPreVenda);
    }
    private void abrirTelaVincularPreVenda(){
        try{
            SPV1050 spv1050 = new SPV1050();
            WindowUtils.createJDialog(spv1050.getWindow(), spv1050);
            spv1050.getWindow().setVisible(true);
        }catch(Exception e){
            exibirInformacao("Falha ao abrir tarefa " + e.getMessage())
        }
    }

    @Override
    void preSalvar(boolean salvo) {
    }

    @Override
    void posSalvar(Long id) {
    }
}