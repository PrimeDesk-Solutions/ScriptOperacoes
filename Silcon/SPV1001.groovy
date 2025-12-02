/*
    TELA: SPV1001 - PRÉ-VENDA
    FUNÇÃO:

    1- Verifica se foi inserido quantidade e unitário nos itens da pré-venda, caso não inserido, o processo é interrompido
    2- Cria menu customizado para impressão das etiquetas a partir da pré-venda
 */
package scripts

import br.com.multitec.utils.ValidacaoException
import br.com.multitec.utils.http.HttpRequest
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
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
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.print.PrinterJob;

public class Script extends sam.swing.ScriptBase{
    MultitecRootPanel panel;
    @Override
    public void execute(MultitecRootPanel tarefa) {
        this.panel = tarefa;
        JButton btnConcluir = getComponente("btnConcluir");

        btnConcluir.addActionListener(new ActionListener() {
            @Override
            void actionPerformed(ActionEvent e) {
                verificarUnitariosQuantidadesItem();
            }
        } )

        criarMenu("Impressão Etiqueta", "Imprimir", e -> imprimirEtiqueta(), null)

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
        MSpread sprCcb01s = getComponente("sprCcb01s");
        List<Ccb01> ccb01s = sprCcb01s.getValue();
        Integer row = sprCcb01s.getSelectedRow();
        Ccb01 ccb01 = sprCcb01s.get(row);

        Integer numPreVenda = ccb01.ccb01num;

        return numPreVenda;

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
}