/*
    TELA: SPV1001 - PRÉ-VENDA
    FUNÇÃO:

    1- Verifica se foi inserido quantidade e unitário nos itens da pré-venda, caso não inserido, o processo é interrompido
    2- Cria menu customizado para impressão das etiquetas a partir da pré-venda
    3- Valida se foi preenchido o contato do cliente, caso consumidor final
    4- Cria um check de "Com Nota" na tela
    5- Altera a observação na pré-venda de acordo com a seleção do check "Com Nota".
        5.1 Quando selecionado para marcar o check "Com Nota", é colocado nas observações o texto "Com Nota."
        5.2 Quando selecionado para desmarcar o check "Com Nota" é retirado o texto "COM NOTA." das observações.
    6- Ao clicar no botão concluir, é verificado se foi preenchido o campo comprador, e se consumido, não permite gravar com o texto "CONSUMIDOR"
 */
package scripts

import multitec.swing.components.textfields.MTextArea
import multitec.swing.components.textfields.MTextFieldString

import javax.swing.event.ListSelectionEvent
import javax.swing.event.ListSelectionListener
import java.awt.Rectangle;
import java.awt.Point;
import br.com.multitec.utils.ValidacaoException
import br.com.multitec.utils.http.HttpRequest
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import multitec.swing.components.MCheckBox
import multitec.swing.components.spread.MSpread
import multitec.swing.core.MultitecRootPanel
import multitec.swing.core.dialogs.ErrorDialog
import multitec.swing.request.WorkerRunnable
import multitec.swing.request.WorkerSupplier
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.printing.PDFPageable
import sam.model.entities.ab.Abm01
import sam.model.entities.cc.Ccb01
import sam.model.entities.cc.Ccb0101

import javax.mail.Session
import javax.print.DocFlavor
import javax.print.PrintService
import javax.print.PrintServiceLookup
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JOptionPane
import javax.swing.JPanel
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.print.PrinterJob;

public class Script extends sam.swing.ScriptBase{
    MultitecRootPanel panel;
    MCheckBox chkNota = new MCheckBox();
    @Override
    public void execute(MultitecRootPanel tarefa) {
        this.panel = tarefa;
        chkNota.setEnabled(false);
        criarMenu("Impressão Etiqueta", "Imprimir", e -> imprimirEtiqueta(), null);
        adicionarEventoBotaoConcluir();
        adicionarCheckComNota();
    }
    private void adicionarCheckComNota(){
        MSpread sprCcb01 = getComponente("sprCcb01s");
        JPanel pnlItens = getComponente("pnlItens");
        chkNota.setText("Com Nota");
        chkNota.setBounds(new Rectangle(new Point(1250, 0), chkNota.getPreferredSize()));
        pnlItens.add(chkNota);
        chkNota.addActionListener(new ActionListener() {
            @Override
            void actionPerformed(ActionEvent e) {
                if(chkNota.isSelected()){
                    inserirMensagemComNota();
                } else{
                    apagarMensagemComNota();
                }

            }
        });

        sprCcb01.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            void valueChanged(ListSelectionEvent e) {
                chkNota.setEnabled(true);
            }
        })
    }
    private void inserirMensagemComNota(){
        MTextArea txtCcb01obs = getComponente("txtCcb01obs");
        JButton btnGravarObservacao = getComponente("btnGravarObservacao");
        String obs = txtCcb01obs.getValue() == null ? "" : txtCcb01obs.getValue();

        if(!obs.toUpperCase().contains("COM NOTA.")){
            String novaObs = "COM NOTA. " + obs;

            txtCcb01obs.setValue(novaObs);

            btnGravarObservacao.doClick();
        }
    }
    private void apagarMensagemComNota(){
        MTextArea txtCcb01obs = getComponente("txtCcb01obs");
        JButton btnGravarObservacao = getComponente("btnGravarObservacao");
        String obs = txtCcb01obs.getValue();

        if(obs.toUpperCase().contains("COM NOTA.")){
            obs = obs.replace("COM NOTA.", "");

            txtCcb01obs.setValue(obs);

            btnGravarObservacao.doClick()
        }
    }
    private void adicionarEventoBotaoConcluir(){
        JButton btnConcluir = getComponente("btnConcluir");

        btnConcluir.addActionListener(new ActionListener() {
            @Override
            void actionPerformed(ActionEvent e) {
                verificarUnitariosQuantidadesItem();
                verificarContatosConsumidorFinal();
                verificarCampoComprador();
            }
        } )
    }
    private void imprimirEtiqueta(){
        MSpread sprCcb01s = getComponente("sprCcb01s");

        if(sprCcb01s.getValue().size() == 0) return;
        Integer numPreVenda = buscarNumeroPreVenda();

        try{
            WorkerSupplier.create(this.panel.getWindow(), {
                return buscarDadosImpressao(numPreVenda);
            })
            .initialText("Imprimindo Etiqueta")
            .dialogVisible(true)
            .success({ bytes ->
                enviarDadosParaImpressao(bytes, numPreVenda);
            })
            .start();
        }catch(Exception err){
            ErrorDialog.defaultCatch(this.panel.getWindow(), err);
        }
    }
    private Integer buscarNumeroPreVenda(){

        Ccb01 ccb01 = buscarLinhaPreVendaSelecionada();

        Integer numPreVenda = ccb01.ccb01num;

        return numPreVenda;

    }
    private Ccb01 buscarLinhaPreVendaSelecionada(){
        MSpread sprCcb01s = getComponente("sprCcb01s");
        List<Ccb01> ccb01s = sprCcb01s.getValue();
        Integer row = sprCcb01s.getSelectedRow();

        return sprCcb01s.get(row);
    }
    private byte[] buscarDadosImpressao(Integer numPreVenda) {
        String json = "{\"nome\":\"Silcon.relatorios.spv.SPV_Etiquetas\",\"filtros\":{\"numero\":"+numPreVenda+"}}";

        ObjectMapper mapper = new ObjectMapper();
        JsonNode obj = mapper.readTree(json);
        return HttpRequest.create().controllerEndPoint("relatorio").methodEndPoint("gerarRelatorio").parseBody(obj).post().getResponseBody()
    }
    protected void enviarDadosParaImpressao(byte[] bytes, Integer idDoc) {
        try {
            if(bytes == null || bytes.length == 0) {
                interromper("Não foi encontrado o relatório ou parametrizações para a impressão.");
            }

            PrintService myService = escolherImpressora();

            WorkerRunnable load = WorkerRunnable.create(this.panel.getWindow());
            load.dialogVisible(true);
            load.initialText("Enviando Documento para impressão");
            load.runnable({
                try {
                    PDDocument document = PDDocument.load(bytes);
                    PrinterJob job = PrinterJob.getPrinterJob();
                    job.setPageable(new PDFPageable(document));
                    job.setPrintService(myService);
                    job.setCopies(1);
                    job.print();
                    document.close();
                }catch (Exception err) {
                    interromper("Erro ao imprimir Etiqueta. Verifique a impressora utilizada.");
                }
            });
            load.start();
        }catch (Exception err) {
            ErrorDialog.defaultCatch(this.panel.getWindow(), err, "Erro ao enviar dados para impressão.");
        }
    }

    protected PrintService escolherImpressora() {
        PrintService myService = null;

        PrintService[] ps = PrintServiceLookup.lookupPrintServices(DocFlavor.SERVICE_FORMATTED.PAGEABLE, null);
        if (ps.length == 0) {
            throw new ValidacaoException("Não foram encontradas impressoras.");
        }else {
            String nomeImpressoraComum = null;

            if(ps.length == 1) {
                nomeImpressoraComum = ps[0].getName();
            }else {
                JComboBox<String> jcb = new JComboBox<>();

                for (PrintService printService : ps) {
                    jcb.addItem(printService.getName());
                }

                JOptionPane.showMessageDialog(null, jcb, "Selecione a impressora", JOptionPane.QUESTION_MESSAGE);

                if (jcb.getSelectedItem() == null) {
                    throw new ValidacaoException("Nenhuma impressora selecionada.");
                }

                nomeImpressoraComum = (String)jcb.getSelectedItem();
            }

            for (PrintService printService : ps) {
                if (printService.getName().equalsIgnoreCase(nomeImpressoraComum)) {
                    myService = printService;
                    break;
                }
            }

            if (myService == null) {
                throw new ValidacaoException("Nenhuma impressora selecionada.");
            }
        }

        return myService;
    }
    private void verificarUnitariosQuantidadesItem(){
        MSpread sprCcb0101s = getComponente("sprCcb0101s");
        def spreadValue = sprCcb0101s.getValue()

        if(spreadValue != null && spreadValue.size() > 0){
            for(Ccb0101 ccb0101 : spreadValue ){
                if(ccb0101.ccb0101qtComl_Zero == 0 || ccb0101.ccb0101unit_Zero == 0) interromper("O item seq. " + ccb0101.ccb0101seq + " - " + ccb0101.ccb0101item.abm01descr + " está com quantidade/unitário zero. Processo interrompido!");
            }
        }
    }
    private void verificarContatosConsumidorFinal(){
        MTextFieldString txtCcb01eeDdd1 = getComponente("txtCcb01eeDdd1");
        MTextFieldString txtCcb01eeFone1 = getComponente("txtCcb01eeFone1");
        MTextFieldString txtCcb01eeDdd2 = getComponente("txtCcb01eeDdd2");
        MTextFieldString txtCcb01eeFone2 = getComponente("txtCcb01eeFone2");

        String ddd1 = txtCcb01eeDdd1.getValue();
        String ddd2 = txtCcb01eeDdd2.getValue();
        String fone1 = txtCcb01eeFone1.getValue();
        String fone2 = txtCcb01eeFone2.getValue();
        Ccb01 ccb01 = buscarLinhaPreVendaSelecionada();
        String nomeEntidade = ccb01.ccb01ent.abe01nome;

        if((ddd1 == null || fone1 == null) && (ddd2 == null || fone2 == null) && nomeEntidade.toUpperCase() == "CONSUMIDOR"){
            interromper("Para consumidor final é necessário preencher o contato.")
        }
    }
    private void verificarCampoComprador(){
        try{
            MTextFieldString txtCcb01comprador = getComponente("txtCcb01comprador");
            Ccb01 ccb01 = buscarLinhaPreVendaSelecionada();
            String nomeEntidade = ccb01.ccb01ent.abe01nome;
            String nomeComprador = txtCcb01comprador.getValue();

            if(nomeEntidade.toUpperCase() == "CONSUMIDOR" && nomeComprador == null) throw new ValidacaoException("Para consumidor final é necessário preencher o campo comprador.");
            if(nomeEntidade.toUpperCase() == "CONSUMIDOR" && nomeComprador.toUpperCase().contains("CONSUMIDOR")) throw new ValidacaoException("O nome do comprador não pode ser consumidor.")

        }catch(Exception e){
            interromper(e.getMessage())
        }
    }
}