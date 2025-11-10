

package scripts

import br.com.multitec.utils.ValidacaoException
import br.com.multitec.utils.http.HttpRequest
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import groovy.swing.table.TableMap
import multitec.swing.components.spread.MSpread
import multitec.swing.core.MultitecRootPanel
import multitec.swing.core.dialogs.ErrorDialog
import multitec.swing.request.WorkerRunnable
import multitec.swing.request.WorkerSupplier
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.printing.PDFPageable
import sam.swing.ScriptBase

import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JFrame;
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.border.TitledBorder
import java.awt.LayoutManager;
import javax.print.PrintServiceLookup
import javax.print.DocFlavor
import javax.print.PrintService
import javax.print.attribute.AttributeSet
import java.awt.print.PrinterJob


class SCF1001 extends ScriptBase{
    MultitecRootPanel tarefa;
    JPanel pnlImpressora = new JPanel();
    JComboBox<String> combo;
    JButton btnImprimir = new JButton()
    @Override
    void execute(MultitecRootPanel panel) {
        this.tarefa = panel;
        criarComponentesIniciais();

        tarefa.add(pnlImpressora);
        tarefa.add(btnImprimir);
    }
    private void criarComponentesIniciais(){
        // Painel Impressora
        pnlImpressora.setBorder(new TitledBorder("Impressora"));
        pnlImpressora.setLayout((LayoutManager)null);
        pnlImpressora.setBounds(700,583,200,40);

        // Cria o combo de impressoras
        List<String> impressoras = listarImpressoras();
        String[] opcoes = impressoras;
        combo = new JComboBox<>(opcoes);
        combo.setBounds(10,15, 180, 20);
        pnlImpressora.add(combo);

        // Botão de impressão
        btnImprimir.setText("Imprimir Boletos");
        btnImprimir.setBounds(400,583,200,40);
        btnImprimir.addActionListener(e -> buscarImprimirDocumento())

    }
    private void buscarImprimirDocumento(){
        try{
            MSpread sprDocumentos = getComponente("sprDocumentos");
            List<TableMap> listDocs = sprDocumentos.getValue();
            List<Long> ids = new ArrayList<>();

            if(listDocs == null || listDocs.size() == 0) interromper("Nenhum documento encontrado na spread para impressão dos boletos.");

            Integer linha = 0;
            for(docs in listDocs){
                linha++
                Long idDoc = docs.getLong("daa01id");
                Integer numDoc = docs.getInteger("numero");
                List<TableMap> daa0102s = buscarIntegracaoBancaria(idDoc);

                if(daa0102s == null || daa0102s.size() == 0) interromper("Documento " + numDoc + " sem integração bancária na linha " + linha + ".");

                ids.add(idDoc);
            }

            WorkerSupplier.create(tarefa.getWindow(), {
                return buscarDadosImpressao(ids);
            })
                    .initialText("Imprimindo Boletos")
                    .dialogVisible(true)
                    .success({ bytes ->
                        enviarDadosParaImpressao(bytes);
                    })
                    .start();
        }catch (Exception err){
            ErrorDialog.defaultCatch(this.tarefa.getWindow(), err);
        }
    }
    private buscarIntegracaoBancaria(idDoc){
        return executarConsulta("SELECT daa0102id FROM daa0102 WHERE daa0102doc = " + idDoc.toString());
    }
    private byte[] buscarDadosImpressao(List<Long> ids) {
        String json = "{\"nome\":\"Silcon.relatorios.scf.SCF_Boleto_Itau\",\"filtros\":{\"ids\":"+ids+"}}";

        ObjectMapper mapper = new ObjectMapper();
        JsonNode obj = mapper.readTree(json);
        return HttpRequest.create().controllerEndPoint("relatorio").methodEndPoint("gerarRelatorio").parseBody(obj).post().getResponseBody()
    }
    protected void enviarDadosParaImpressao(byte[] bytes) {
        try {
            if(bytes == null || bytes.length == 0) {
                interromper("Não foi encontrado o relatório ou parametrizações para a impressão.");
            }

            PrintService myService = escolherImpressora();

            WorkerRunnable load = WorkerRunnable.create(this.tarefa.getWindow());
            load.dialogVisible(true);
            load.initialText("Enviando Documento para impressão");
            load.runnable({
                try {
                    PDDocument document = PDDocument.load(bytes);
                    PrinterJob job = PrinterJob.getPrinterJob();
                    job.setPageable(new PDFPageable(document));
                    job.setPrintService(myService);
                    job.setCopies(1);
                    job.setJobName("Boleto");
                    job.print();
                    //document.save("C:/Users/Leonardo/Desktop/teste.pdf")
                    document.close();
                }catch (Exception err) {
                    interromper("Erro ao imprimir Documento. Verifique a impressora utilizada.");
                }
            });
            load.start();
        }catch (Exception err) {
            ErrorDialog.defaultCatch(this.tarefa.getWindow(), err, "Erro ao enviar dados para impressão.");
        }
    }
    protected PrintService escolherImpressora() {
        PrintService myService = null;

        PrintService[] ps = PrintServiceLookup.lookupPrintServices(DocFlavor.SERVICE_FORMATTED.PAGEABLE, null);

        if (ps.length == 0) {
            throw new ValidacaoException("Não foram encontradas impressoras.");
        }else {
            String nomeImpressoraComum = combo.getSelectedItem();

            for (PrintService printService : ps) {
                if (printService.getName().equalsIgnoreCase(nomeImpressoraComum)) {
                    myService = printService;
                    break;
                }
            }

            if (myService == null) {
                throw new ValidacaoException("Nenhuma impressora encontrada.");
            }
        }
        return myService;
    }
    private List<String> listarImpressoras(){
        PrintService[] ps = PrintServiceLookup.lookupPrintServices(DocFlavor.SERVICE_FORMATTED.PAGEABLE, (AttributeSet)null);
        List<String> impressoras = new ArrayList<>()
        for(PrintService p : ps){
            impressoras.add(p.getName())
        }
        return impressoras;
    }
    @Override
    void preSalvar(boolean salvo) {
    }

    @Override
    void posSalvar(Long id) {
    }
}