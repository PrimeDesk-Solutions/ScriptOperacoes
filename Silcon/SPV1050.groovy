/*
    FUNÇÃO:
        1. Deixa o campo de tipo de documento com o código 10 como default na empresa 001 e código 23 como default na empresa 002
        2. Deixa o campo PPV com o código 000 como default na empresa 001 e 002
        3. Altera a view de busca das entidades (F4)
 */


import br.com.multitec.utils.UiSqlColumn
import multitec.swing.components.autocomplete.MNavigation
import multitec.swing.core.MultitecRootPanel;

public class Script extends sam.swing.ScriptBase{
    public Runnable windowLoadOriginal;
    @Override
    public void execute(MultitecRootPanel tarefa) {
        this.windowLoadOriginal = tarefa.windowLoad ;
        tarefa.windowLoad = {novoWindowLoad()};
       definirCamposDefault();
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
    private void definirCamposDefault(){
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