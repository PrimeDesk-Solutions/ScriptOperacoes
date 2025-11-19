/*
    TELA: SPV1001 - PRÉ-VENDA
    FUNÇÃO:

    1- Verifica se foi inserido quantidade e unitário nos itens da pré-venda, caso não inserido, o processo é interrompido



 */
package scripts

import multitec.swing.core.MultitecRootPanel
import sam.model.entities.ab.Abm01
import sam.model.entities.cc.Ccb0101

import javax.mail.Session
import javax.swing.JButton
import java.awt.event.ActionEvent
import java.awt.event.ActionListener;

public class Script extends sam.swing.ScriptBase{
    @Override
    public void execute(MultitecRootPanel tarefa) {
        JButton btnConcluir = getComponente("btnConcluir");

        btnConcluir.addActionListener(new ActionListener() {
            @Override
            void actionPerformed(ActionEvent e) {
                verificarUnitariosQuantidadesItem();
            }
        } )

    }

    private void verificarUnitariosQuantidadesItem(){
        def sprCcb0101s = getComponente("sprCcb0101s");
        def spreadValue = sprCcb0101s.getValue()

        if(spreadValue != null && spreadValue.size() > 0){
            for(Ccb0101 ccb0101 : spreadValue ){
                if(ccb0101.ccb0101qtComl_Zero == 0 || ccb0101.ccb0101unit_Zero == 0) interromper("O item seq. " + ccb0101.ccb0101seq + " - " + ccb0101.ccb0101item.abm01descr + " está com quantidade/unitário zero. Processo interrompido!");
            }
        }
    }
}