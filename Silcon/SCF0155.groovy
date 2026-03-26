/*
    TELA: SCF0155 - CAIXA FINANCEIRO
    FUNÇÃO:
    1. Insere um botão para abrir a tela de vincular pré-venda
    2. Altera o view de entidades (F4)
 */
package scripts

import multitec.swing.components.spread.MSpread
import multitec.swing.core.MultitecRootPanel
import multitec.swing.core.utils.WindowUtils
import sam.swing.ScriptBase
import sam.swing.tarefas.spv.SPV1050
import br.com.multitec.utils.UiSqlColumn;
import javax.swing.JButton

class SCF0155 extends ScriptBase{
    MultitecRootPanel tarefa;
    public Runnable windowLoadOriginal;

    @Override
    void execute(MultitecRootPanel panel) {
        this.tarefa = panel;
        inserirBtnAbrirTelaVincularPreVenda();
        this.windowLoadOriginal = tarefa.windowLoad ;
        tarefa.windowLoad = {novoWindowLoad()};
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
    protected void novoWindowLoad() {
        this.windowLoadOriginal.run();

        def ctrAbe01 = getComponente("ctrAbe01");

        ctrAbe01.f4Columns = () -> {
            java.util.List<UiSqlColumn> uiSqlColumn = new ArrayList<>();
            UiSqlColumn abe01codigo = new UiSqlColumn("abe01codigo", "abe01codigo", "Código", 10);
            UiSqlColumn abe01nome = new UiSqlColumn("abe01nome", "abe01nome", "Nome", 60);
            UiSqlColumn abe01complem = new UiSqlColumn("abe01complem", "abe01complem", "Endereço", 60);
            UiSqlColumn abe01na = new UiSqlColumn("abe01na", "abe01na", "Nome Abreviado", 40);
            UiSqlColumn abe01ni = new UiSqlColumn("abe01ni", "abe01ni", "Número da Inscrição", 60);
            uiSqlColumn.addAll(Arrays.asList(abe01codigo, abe01nome, abe01complem, abe01na, abe01ni));
            return uiSqlColumn;
        };
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