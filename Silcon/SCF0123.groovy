package scripts

import br.com.multitec.utils.collections.TableMap
import br.com.multitec.utils.criteria.client.ClientCriterions;
import br.com.multitec.utils.criteria.client.ClientCriterion
import com.fasterxml.jackson.core.type.TypeReference;
import multitec.swing.components.MRadioButton;
import multitec.swing.components.MCheckBox
import multitec.swing.components.autocomplete.MButtonList;
import multitec.swing.components.autocomplete.MComboBoxData
import multitec.swing.components.autocomplete.MNavigation
import multitec.swing.components.spread.MSpread
import multitec.swing.components.textfields.MTextFieldInteger;
import multitec.swing.components.textfields.MTextFieldLocalDate;
import multitec.swing.core.MultitecRootPanel
import org.w3c.dom.events.Event
import sam.model.entities.da.Daa01;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.ButtonGroup;
import javax.swing.border.TitledBorder;
import java.awt.Font
import java.awt.Point
import java.awt.Rectangle
import java.awt.event.ActionEvent
import java.awt.event.ActionListener;
import sam.dto.scf.SCF0123MostrarDto
import multitec.swing.core.dialogs.Messages;
import multitec.swing.request.WorkerRequest;
import multitec.swing.request.WorkerRunnable;
import java.time.LocalDate;
import com.fasterxml.jackson.core.type.TypeReference;




public class SCF0123 extends sam.swing.ScriptBase {

    JPanel pnlStatus = new JPanel();
    MRadioButton rdoRecebidos = new MRadioButton();
    MRadioButton rdoPagos = new MRadioButton();
    MRadioButton rdoReceber2 = new MRadioButton();
    MRadioButton rdoPagar2 = new MRadioButton();
    ButtonGroup buttonGroup1 = new ButtonGroup();
    JPanel pnlDtRecebimento = new JPanel();
    MCheckBox chkRecebimento = new MCheckBox();
    MComboBoxData cmbDtRecebimento = new MComboBoxData();
    MTextFieldLocalDate txtDataInicialRecebi = new MTextFieldLocalDate();
    MTextFieldLocalDate txtDataFinalRecebi = new MTextFieldLocalDate();
    JPanel pnlDtPagamento = new JPanel();
    MCheckBox chkPagamento = new MCheckBox();
    MComboBoxData cmbDtPagamento = new MComboBoxData();
    MTextFieldLocalDate txtDataInicialPgto = new MTextFieldLocalDate();
    MTextFieldLocalDate txtDataFinalPgto = new MTextFieldLocalDate();
    MultitecRootPanel tarefa

    @Override
    public void execute(MultitecRootPanel tarefa) {
        // componentes já existentes (exemplo: o script que você já tinha)
        JButton btnMostrar = getComponente("btnMostrar");
        JButton btnSetaParaBaixo = getComponente("btnSetaParaBaixo");
        MRadioButton rdoReceber = getComponente("rdoReceber");
        MRadioButton rdoPagar = getComponente("rdoPagar");
        JPanel pnlRP = getComponente("pnlRP");
        JPanel pnlTipoDoc = getComponente("pnlTipoDoc");
        MButtonList btlAbe01 = getComponente("btlAbe01");
        MButtonList btlAah01 = getComponente("btlAah01");
        this.tarefa = tarefa;

        pnlTipoDoc.setBounds(2, 3, 470, 89) // Altera a posição do painel de tipo de documento
        pnlRP.setVisible(false) // Inativa painel R/P
        rdoReceber.setEnabled(false) // Inativa os componentes de R/P


        for(ActionListener event : btnMostrar.getActionListeners()){ // Retira os eventos defaut do botão
            btnMostrar.removeActionListener(event);
        }

        btnMostrar.setBounds(1100, 140, 100, 40);
        btnSetaParaBaixo.setBounds(1200, 158, 28, 22);

        // Painel com layout para arazenar status recebidos e pagos
        pnlStatus.setName("pnlStatus");
        pnlStatus.setBorder(new TitledBorder("Status"));
        pnlStatus.setBounds(345, 93, 120, 89);
        pnlStatus.setLayout(null);


        // Opção de recebidos
        rdoRecebidos.setName("rdoRecebidos");
        rdoRecebidos.setText("Recebidos");
        rdoRecebidos.setBounds(new Rectangle(new Point(10,17), rdoRecebidos.getPreferredSize()));
        pnlStatus.add(rdoRecebidos);

        // Opção de pagos
        rdoPagos.setName("rdoPagos");
        rdoPagos.setText("Pagos");
        rdoPagos.setSelected(true);
        rdoPagos.setBounds(new Rectangle(new Point(10,34), rdoPagos.getPreferredSize()));
        pnlStatus.add(rdoPagos);

        // Opção de receber
        rdoReceber2.setName("rdoReceber2");
        rdoReceber2.setText("Receber");
        rdoReceber2.setSelected(true);
        rdoReceber2.setBounds(new Rectangle(new Point(10,50), rdoReceber2.getPreferredSize()));
        pnlStatus.add(rdoReceber2);

        // Opção de pagar
        rdoPagar2.setName("rdoPagar2");
        rdoPagar2.setText("Pagar");
        rdoPagar2.setSelected(true);
        rdoPagar2.setBounds(new Rectangle(new Point(10,68), rdoPagar2.getPreferredSize()));
        pnlStatus.add(rdoPagar2);

        // Grupo Opções Status
        buttonGroup1.add(rdoRecebidos);
        buttonGroup1.add(rdoPagos);
        buttonGroup1.add(rdoReceber2);
        buttonGroup1.add(rdoPagar2);

        // Painel Data Recebimento
        pnlDtRecebimento.setName("pnlDtRecebimento");
        pnlDtRecebimento.setBorder(new TitledBorder(" "));
        pnlDtRecebimento.setLayout(null);
        pnlDtRecebimento.setBounds(473, 93, 238, 89);

        // Check Controller Recebimento
        chkRecebimento.setName("chkRecebimento");
        chkRecebimento.setText("Data Recebimento/Pagamento");
        chkRecebimento.setControladorDePanel(true);
        chkRecebimento.setFont(new Font("Arial", 1, 13));
        chkRecebimento.setBounds(7,1,230, (int)(chkRecebimento.getPreferredSize()).height);
        pnlDtRecebimento.add(chkRecebimento);

        // Combo Data Recebimento
        cmbDtRecebimento.setName("cmbDtRecebimento");
        cmbDtRecebimento.setBounds(8, 27, 222, (int)(cmbDtRecebimento.getPreferredSize()).height);
        pnlDtRecebimento.add(cmbDtRecebimento);


        // Campo Data Recebimento Inicial
        txtDataInicialRecebi.setName("txtDataInicialRecebi");
        txtDataInicialRecebi.setBounds(8, 56, 103, (int)(txtDataInicialRecebi.getPreferredSize()).height);
        pnlDtRecebimento.add(txtDataInicialRecebi);

        // Campo Data Recebimento Final
        txtDataFinalRecebi.setName("txtDataFinalRecebi");
        txtDataFinalRecebi.setBounds(120, 55, 103, (int)(txtDataFinalRecebi.getPreferredSize()).height);
        pnlDtRecebimento.add(txtDataFinalRecebi);

        // Painel Data Pagamento
        pnlDtPagamento.setName("pnlDtPagamento");
        pnlDtPagamento.setBorder(new TitledBorder(" "));
        pnlDtPagamento.setLayout(null);
        pnlDtPagamento.setBounds(720, 93, 238, 89);

//        // Check Controller Pagamento
////        MCheckBox chkPagamento = new MCheckBox();
//        chkPagamento.setName("chkPagamento");
//        chkPagamento.setText("Data Pagamento");
//        chkPagamento.setControladorDePanel(true);
//        chkPagamento.setFont(new Font("Arial", 1, 13));
//        chkPagamento.setBounds(7,1,150, (int)(chkPagamento.getPreferredSize()).height);
//        pnlDtPagamento.add(chkPagamento);

//        // Combo Data Pagamento
//        cmbDtPagamento.setName("cmbDtPagamento");
//        cmbDtPagamento.setBounds(8, 27, 222, (int)(cmbDtPagamento.getPreferredSize()).height);
//        pnlDtPagamento.add(cmbDtPagamento);


//        // Campo Data Pagamento Inicial
////        MTextFieldLocalDate txtDataInicialPgto = new MTextFieldLocalDate();
//        txtDataInicialPgto.setName("txtDataInicialPgto");
//        txtDataInicialPgto.setBounds(8, 56, 103, (int)(txtDataInicialPgto.getPreferredSize()).height);
//        pnlDtPagamento.add(txtDataInicialPgto);

//        // Campo Data Pagamento Final
////        MTextFieldLocalDate txtDataFinalPgto = new MTextFieldLocalDate();
//        txtDataFinalPgto.setName("txtDataFinalPgto");
//        txtDataFinalPgto.setBounds(120, 55, 103, (int)(txtDataFinalPgto.getPreferredSize()).height);
//        pnlDtPagamento.add(txtDataFinalPgto);

        // Data Inicial e Final Recebimento
        cmbDtRecebimento.init(txtDataInicialRecebi, txtDataFinalRecebi);

//        // Data Inicial e Final Pagamento
//        cmbDtPagamento.init(txtDataInicialPgto, txtDataFinalPgto);

        tarefa.add(pnlStatus);
        tarefa.add(pnlDtRecebimento);
//        tarefa.add(pnlDtPagamento);

        btnMostrar.addActionListener(e -> btnMostrarPressed(e));

    }
    private btnMostrarPressed(ActionEvent e){
       buscarDocumentosPreencherSpread();
    }

    private List<Daa01> buscarDocumentosPreencherSpread(){

        // Componentes padrão
        MRadioButton rdoReceber = getComponente("rdoReceber");
        MRadioButton rdoPagar = getComponente("rdoPagar");
        MCheckBox chkTipoDoc = getComponente("chkTipoDoc");
        MNavigation nvgAah01codigoIni = getComponente("nvgAah01codigoIni");
        MNavigation nvgAah01codigoFim = getComponente("nvgAah01codigoFim");
        MCheckBox chkNumero = getComponente("chkNumero");
        MTextFieldInteger txtNumInicial = getComponente("txtNumInicial");
        MTextFieldInteger txtNumFinal = getComponente("txtNumFinal");
        MCheckBox chkEmissao = getComponente("chkEmissao");
        MComboBoxData cmbEmissao = getComponente("cmbEmissao");
        MTextFieldLocalDate txtDataInicialEmi = getComponente("txtDataInicialEmi");
        MTextFieldLocalDate txtDataFinalEmi = getComponente("txtDataFinalEmi");
        MCheckBox chkEntidade = getComponente("chkEntidade");
        MNavigation nvgAbe01codigoIni = getComponente("nvgAbe01codigoIni");
        MNavigation nvgAbe01codigoFim = getComponente("nvgAbe01codigoFim");
        MComboBoxData cmbVencimento = getComponente("cmbVencimento");
        MCheckBox chkVencimento = getComponente("chkVencimento");
        MRadioButton rdoVencimentoReal = getComponente("rdoVencimentoReal");
        MTextFieldLocalDate txtDataInicialVcto = getComponente("txtDataInicialVcto");
        MTextFieldLocalDate txtDataFinalVcto = getComponente("txtDataFinalVcto");
        MRadioButton rdoReal = getComponente("rdoReal");
        MRadioButton rdoPrevisao = getComponente("rdoPrevisao");
        MButtonList btlAbe01 = getComponente("btlAbe01");
        MButtonList btlAah01 = getComponente("btlAah01");
        MSpread sprDaa01s = getComponente("sprDaa01s");

        Integer valueRecebidos = rdoRecebidos.isSelected() ? 1 : 0;
        Integer valuePagos = rdoPagos.isSelected() ? 1 : 0;
        Integer valueChkRecebimento = chkRecebimento.getValue();
        String dtRecebimentoPgtoIni = txtDataInicialRecebi.getValue()
        String dtRecebimentoPgtoFin = txtDataFinalRecebi.getValue()
        Integer valueReceber = rdoReceber2.isSelected() ? 1 : 0;
        Integer valuePagar = rdoPagar2.isSelected() ? 1 : 0;
        Integer valueChkTipoDoc = chkTipoDoc.getValue();
        String codTipoDocIni = nvgAah01codigoIni.getValue();
        String codTipoDocFin = nvgAah01codigoFim.getValue();
        Integer valueChkNumero = chkNumero.getValue();
        String numeroInicial = txtNumInicial.getValue();
        String numeroFinal = txtNumFinal.getValue();
        Integer valueChkEmissao = chkEmissao.getValue();
        String dataEmissaoIni = txtDataInicialEmi.getValue();
        String dataEmissaoFin = txtDataFinalEmi.getValue();
        Integer valueChkEntidade = chkEntidade.getValue();
        String codEntidadeIni = nvgAbe01codigoIni.getValue();
        String codEntidadeFin = nvgAbe01codigoFim.getValue();
        Integer valueChkVencimento = chkVencimento.getValue();
        Integer valueRdoVencimentoReal = rdoVencimentoReal.isSelected() ? 1 : 0;
        String dataInicialVcto = txtDataInicialVcto.getValue();
        String dataFinalVcto = txtDataFinalVcto.getValue();
        Integer valueRdoReal = rdoReal.isSelected() ? 1 : 0;
        Integer valueRdoPrevisao = rdoPrevisao.isSelected() ? 1 : 0;
        String idsAbe01 = btlAbe01.getValue().getValor1();
        String idsAah01 = btlAah01.getValue().getValor1();

        try{
            sprDaa01s.clear()
            WorkerRequest.create(tarefa.getWindow())
                    .controllerEndPoint("servlet/run")
                    .showServerMessages()
                    .param("name", "Silcon.servlets.SCF0123_Servlet")
                    .param("valueRecebidos", valueRecebidos)
                    .param("valuePagos", valuePagos)
                    .param("valueChkRecebimento", valueChkRecebimento)
                    .param("dtRecebimentoPgtoIni", dtRecebimentoPgtoIni)
                    .param("dtRecebimentoPgtoFin", dtRecebimentoPgtoFin)
                    .param("valueReceber", valueReceber)
                    .param("valuePagar", valuePagar)
                    .param("valueChkTipoDoc", valueChkTipoDoc)
                    .param("codTipoDocIni", codTipoDocIni)
                    .param("codTipoDocFin", codTipoDocFin)
                    .param("valueChkNumero", valueChkNumero)
                    .param("numeroInicial", numeroInicial)
                    .param("numeroFinal", numeroFinal)
                    .param("valueChkEmissao", valueChkEmissao)
                    .param("dataEmissaoIni", dataEmissaoIni)
                    .param("dataEmissaoFin", dataEmissaoFin)
                    .param("valueChkEntidade", valueChkEntidade)
                    .param("codEntidadeIni", codEntidadeIni)
                    .param("codEntidadeFin", codEntidadeFin)
                    .param("valueChkVencimento", valueChkVencimento)
                    .param("valueRdoVencimentoReal", valueRdoVencimentoReal)
                    .param("dataInicialVcto", dataInicialVcto)
                    .param("dataFinalVcto", dataFinalVcto)
                    .param("valueRdoReal", valueRdoReal)
                    .param("valueRdoPrevisao", valueRdoPrevisao)
                    .param("idsAbe01", idsAbe01)
                    .param("idsAah01", idsAah01)
                    .initialText("Buscando os documentos financeiros")
                    .success(response -> {
                        List<Daa01> daa01s = (List<Daa01>)response.parseResponse(new TypeReference<List<Daa01>>() {

                        });

                        if (daa01s.size() == 0) interromper("Não foram encontrados documentos financeiros para os filtros informados.");
                        sprDaa01s.setValue(daa01s);
                    })
                    .post();
        }catch(Exception ex){
            interromper(ex.getMessage())
        }

    }
}
