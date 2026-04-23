import multitec.swing.core.MultitecRootPanel
import javax.swing.JCheckBox;

public class Script extends sam.swing.ScriptBase{
    @Override
    public void execute(MultitecRootPanel tarefa) {
        JCheckBox chkSomenteDocumentoComProtocolo = getComponente("chkSomenteDocumentoComProtocolo");
        chkSomenteDocumentoComProtocolo.setSelected(false);
    }
}