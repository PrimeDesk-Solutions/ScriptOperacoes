import multitec.swing.components.MComboBox
import multitec.swing.core.MultitecRootPanel

import javax.swing.JCheckBox;

public class Script extends sam.swing.ScriptBase{
    @Override
    public void execute(MultitecRootPanel tarefa) {
        MComboBox cmbFormulaImportar = getComponente("cmbFormulaImportar");
        JCheckBox chkAbrirSRF1001 = getComponente("chkAbrirSRF1001");

        chkAbrirSRF1001.setSelected(true);

        cmbFormulaImportar.setValue("Silcon.formulas.srf.ImportaXmlNFe_Entrada");


    }
}