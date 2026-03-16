/*
    TELA: SCF0155 - CAIXA FINANCEIRO
    FUNÇÃO:
    1. Insere um botão para abrir a tela de vincular pré-venda
 */
package scripts

import multitec.swing.components.spread.MSpread
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
        //alterarPosicoesSpread()
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
    private void alterarPosicoesSpread(){
        MSpread sprDocumentos = getComponente("sprDocumentos");
        sprDocumentos.getColumnIndex("daa01json.jurosq") != -1 ? sprDocumentos.moveColumn(sprDocumentos.getColumnIndex("daa01json.jurosq"), 7) : null;
        sprDocumentos.getColumnIndex("daa01json.descontoq") != -1 ? sprDocumentos.moveColumn(sprDocumentos.getColumnIndex("daa01json.descontoq"), 8) : null;
        sprDocumentos.getColumnIndex("daa01json.multaq") != -1 ? sprDocumentos.moveColumn(sprDocumentos.getColumnIndex("daa01json.multaq"), 9) : null;
        sprDocumentos.getColumnIndex("daa01json.encargosq") != -1 ? sprDocumentos.moveColumn(sprDocumentos.getColumnIndex("daa01json.encargosq"), 10) : null;
        sprDocumentos.getColumnIndex("daa01json.desconto") != -1 ? sprDocumentos.moveColumn(sprDocumentos.getColumnIndex("daa01json.desconto"), 18) : null;
    }

    @Override
    void preSalvar(boolean salvo) {
    }

    @Override
    void posSalvar(Long id) {
    }
}