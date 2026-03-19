import multitec.swing.components.MComboBox
import multitec.swing.components.spread.MSpread
import multitec.swing.core.MultitecRootPanel
import sam.dto.srf.SRF1008DtoItem

import javax.swing.JButton
import javax.swing.JCheckBox;

public class Script extends sam.swing.ScriptBase{
    MultitecRootPanel panel;

    @Override
    public void execute(MultitecRootPanel tarefa) {
        this.panel = tarefa;
        MComboBox cmbFormulaImportar = getComponente("cmbFormulaImportar");
        JCheckBox chkAbrirSRF1001 = getComponente("chkAbrirSRF1001");

        chkAbrirSRF1001.setSelected(true);

        cmbFormulaImportar.setValue("Silcon.formulas.srf.ImportaXmlNFe_Entrada");


        criarBotaoReplicarTipos();

    }
    private void criarBotaoReplicarTipos(){
        JButton btnReplicarTiposItem = new JButton();
        btnReplicarTiposItem.setText("Tipo 1-Prod.");
        btnReplicarTiposItem.setBounds(1000, 264, 150, 30);
        btnReplicarTiposItem.addActionListener(e -> replicarTipoItem());

        panel.add(btnReplicarTiposItem)
    }
    private void replicarTipoItem(){
        MSpread sprItens = getComponente("sprItens");
        List<SRF1008DtoItem> sprValue = sprItens.getValue();

        if(sprValue == null || sprValue.size() == 0) return;

        for(srf1008dto in sprValue){
            srf1008dto.abm01.setAbm01tipo(1);
        }

        sprItens.refreshAll()
    }
}