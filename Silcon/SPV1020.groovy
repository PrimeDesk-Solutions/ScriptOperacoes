import br.com.multitec.utils.UiSqlColumn
import multitec.swing.core.MultitecRootPanel;
import multitec.swing.components.MCheckBox;
import multitec.swing.components.MRadioButton;
import multitec.swing.components.autocomplete.MNavigation
import javax.swing.JSpinner;

import javax.swing.JButton;


public class Script extends sam.swing.ScriptBase{
    public Runnable windowLoadOriginal;
    @Override
    public void execute(MultitecRootPanel tarefa) {
        this.windowLoadOriginal = tarefa.windowLoad ;
        tarefa.windowLoad = {novoWindowLoad()};
        definirValoresDefault();
        adicionarEventoBtnGravar();
        alterarNumeroDeVias(2);
    }
    protected void novoWindowLoad(){
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
    private void adicionarEventoBtnGravar(){
        JButton btnGravar = getComponente("btnGravar");
        btnGravar.addActionListener(e -> verificarChkValeAoSalvar());
    }
    private void definirValoresDefault(){
        MCheckBox chkTipoDoc = getComponente("chkTipoDoc");
        MRadioButton rdoCashback = getComponente("rdoCashback");
        MNavigation nvgAbd01codigo = getComponente("nvgAbd01codigo");
        MNavigation nvgAae10codigo = getComponente("nvgAae10codigo");
        MRadioButton rdoVale = getComponente("rdoVale");
        Long idEmpresa = obterEmpresaAtiva().getAac10id();

        //chkTipoDoc.setValue(1);
        rdoCashback.setValue(0);

        if(idEmpresa == 1075797){
            nvgAbd01codigo.getNavigationController().setIdValue(584530);
            nvgAae10codigo.getNavigationController().setIdValue(68338);
        }

        if(idEmpresa == 2116598){
            nvgAbd01codigo.getNavigationController().setIdValue(836082);
            nvgAae10codigo.getNavigationController().setIdValue(68338);
        }

    }
    private void verificarChkValeAoSalvar(){
        MNavigation nvgAbe01codigo = getComponente("nvgAbe01codigo");
        MRadioButton rdoVale = getComponente("rdoVale");
        MRadioButton rdoDinheiro = getComponente("rdoDinheiro");

        if((rdoVale.isSelected() || rdoDinheiro.isSelected()) && nvgAbe01codigo.getValue() != "9999999100") interromper("Para clientes com cadastro é necessário utilizar a opção de cashback.");
    }
    private void alterarNumeroDeVias(Integer numVias){
        JSpinner txtVias = getComponente("txtVias");
        txtVias.setValue(numVias);
    }
}