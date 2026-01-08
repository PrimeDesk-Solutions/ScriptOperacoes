/*
    FUNÇÃO:
        - Deixa o check de item flegado como default
        - Deixa o check de condição de pagamento flegado como default
        - Preenche a condição de pagamento com 000 inicial e final
 */
import multitec.swing.components.MCheckBox
import multitec.swing.core.MultitecRootPanel

import java.util.function.Consumer;

public class Script extends sam.swing.ScriptBase{
    public Consumer exibirRegistroPadrao;

    @Override
    public void execute(MultitecRootPanel tarefa) {
        this.exibirRegistroPadrao = tarefa.exibirRegistro
        tarefa.exibirRegistro = {definirCamposDefault(tarefa)}
    }

    @Override
    public void preSalvar(boolean salvo) {
    }

    @Override
    public void posSalvar(Long id) {
    }

    private void definirCamposDefault(MultitecRootPanel tarefa) {
        this.exibirRegistroPadrao.accept(tarefa.registro)
        MCheckBox chkItem = getComponente("chkItem");
        MCheckBox chkCondPgto = getComponente("chkCondPgto");
        def nvgAbe30codigoInicial = getComponente("nvgAbe30codigoInicial");
        def nvgAbe30codigoFinal = getComponente("nvgAbe30codigoFinal");

        chkItem.setValue(1);
        chkCondPgto.setValue(1);
        nvgAbe30codigoInicial.getNavigationController().setIdValue(420586);
        nvgAbe30codigoFinal.getNavigationController().setIdValue(420586);
    }
}