/*
    FUNÇÃO:
        - Deixa o campo de tipo de documento com o código 10 como default na empresa 001 e código 23 como default na empresa 002
        - Deixa o campo PPV com o código 000 como default na empresa 001 e 002
 */

import multitec.swing.components.autocomplete.MNavigation
import multitec.swing.core.MultitecRootPanel;

public class Script extends sam.swing.ScriptBase{
    @Override
    public void execute(MultitecRootPanel tarefa) {
        MNavigation nvgAah01codigo = getComponente("nvgAah01codigo");
        MNavigation nvgCca01codigo = getComponente("nvgCca01codigo");
        Long idEmpresa = obterEmpresaAtiva().getAac10id();

        if(idEmpresa == 1075797) { // MATRIZ
            nvgAah01codigo.getNavigationController().setIdValue(584524);
            nvgCca01codigo.getNavigationController().setIdValue(35527200);
        }else if(idEmpresa == 2116598 ){ // FILIAL
            nvgAah01codigo.getNavigationController().setIdValue(36248030);
            nvgCca01codigo.getNavigationController().setIdValue(35615205);
        }
    }
}