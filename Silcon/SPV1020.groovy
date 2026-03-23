import multitec.swing.core.MultitecRootPanel;
import multitec.swing.components.MCheckBox;
import multitec.swing.components.MRadioButton;
import multitec.swing.components.autocomplete.MNavigation
import javax.swing.JSpinner;

import javax.swing.JButton;


public class Script extends sam.swing.ScriptBase{
    @Override
    public void execute(MultitecRootPanel tarefa) {
        definirValoresDefault();
        adicionarEventoBtnGravar();
        alterarNumeroDeVias(2);
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