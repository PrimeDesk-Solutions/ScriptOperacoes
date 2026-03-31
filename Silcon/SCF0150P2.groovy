package scripts.Silcon

import br.com.multitec.utils.collections.TableMap
import multitec.swing.components.MRadioButton
import multitec.swing.components.autocomplete.MNavigation
import multitec.swing.core.MultitecRootPanel
import sam.model.entities.aa.Aab10
import sam.swing.tarefas.scf.SCF0150

import javax.swing.JButton;

public class SCF0150P2 extends sam.swing.ScriptBase{
    @Override
    public void execute(MultitecRootPanel tarefa) {
        definirValoresDefault();
    }
    private void definirValoresDefault(){
        MRadioButton rdoReceber = getComponente("rdoReceber");

        rdoReceber.setSelected(true);

    }
}