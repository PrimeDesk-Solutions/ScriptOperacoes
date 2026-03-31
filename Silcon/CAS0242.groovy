import multitec.swing.components.MCheckBox
import multitec.swing.core.MultitecRootPanel;

public class Script extends sam.swing.ScriptBase{
    @Override
    public void execute(MultitecRootPanel tarefa) {
        MCheckBox chkGTIN = getComponente("chkGTIN");
        chkGTIN.setValue(1);
    }
}