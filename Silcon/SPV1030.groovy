import br.com.multitec.utils.ValidacaoException
import br.com.multitec.utils.http.HttpRequest
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import multitec.swing.components.spread.MSpread
import multitec.swing.core.MultitecRootPanel
import br.com.multitec.utils.collections.TableMap
import multitec.swing.core.dialogs.ErrorDialog
import multitec.swing.request.WorkerRunnable
import multitec.swing.request.WorkerSupplier
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.printing.PDFPageable
import javax.print.DocFlavor
import javax.print.PrintService
import javax.print.PrintServiceLookup
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JOptionPane
import java.awt.print.PrinterJob;
import java.awt.print.PageFormat;
import java.awt.print.Book;
import org.apache.pdfbox.printing.PDFPrintable
import multitec.swing.components.textfields.MTextFieldLocalDate;

import java.time.LocalDate;



public class Script extends sam.swing.ScriptBase{
MultitecRootPanel tarefa;
@Override
public void execute(MultitecRootPanel tarefa) {
    this.tarefa = tarefa;
    criarBotaoImprimir();
}
private void criarBotaoImprimir(){
    JButton btnImprimir = new JButton();
    btnImprimir.setText("Imprimir Lançamentos");
    btnImprimir.setBounds(400, 227, 172, 37);
    btnImprimir.addActionListener(e -> btnImprimirPressed());

    tarefa.add(btnImprimir)
}
private void btnImprimirPressed(){
    try{
        MSpread sprCaixas = getComponente("sprCaixas");
        MSpread sprLctos = getComponente("sprLctos");
        MTextFieldLocalDate txtDataInicial = getComponente("txtDataInicial");
        Integer rowSprCaixasSelected = sprCaixas.getSelectedRow();

        if(rowSprCaixasSelected == -1) return;

        TableMap tmCaixa = sprCaixas.get(rowSprCaixasSelected);
        List<TableMap> listLctos = sprLctos.getValue();


        List<Long> idsLcto = new ArrayList();
        if(listLctos != null && listLctos.size() > 0){
            for(lcto in listLctos){
                Long idLcto = lcto.getLong("dab10id") == null ? 0L : lcto.getLong("dab10id");
                idsLcto.add(idLcto);
            }
        }

        String codConta = tmCaixa.getString("dab01codigo");
        String nomeConta = tmCaixa.getString("dab01nome");
        String usuario = tmCaixa.getString("usuario");
        BigDecimal saldoAtual = tmCaixa.getBigDecimal("saldo");
        Long idConta = tmCaixa.getLong("dab01id");
        String dataInicial = txtDataInicial.getValue();

        if(idsLcto != null && idsLcto.size() > 0){

            WorkerSupplier.create(this.tarefa.getWindow(), {
                return buscarDadosImpressao(idsLcto, codConta,nomeConta, usuario, saldoAtual,idConta, dataInicial.toString());
            })
            .initialText("Imprimindo/Salvando Documento")
            .dialogVisible(true)
            .success({ bytes ->
                enviarDadosParaImpressao(bytes);
            })
            .start();
        }
    } catch(Exception ex){
        interromper(ex.getMessage())
    }

}
private byte[] buscarDadosImpressao(List<Long> idsLcto, String codConta, String nomeConta, String usuario, BigDecimal saldoAtual,Long idConta, String data) {
    String json = "{\"nome\":\"Silcon.relatorios.spv.SPV_Impressao_Monitoramento_Caixa\",\"filtros\":{\"codConta\":\""+codConta+"\",\"lancamentos\":"+idsLcto+",\"nomeConta\":\""+nomeConta+"\",\"usuario\":\""+usuario+"\",\"saldoAtual\":"+saldoAtual+",\"idConta\":"+idConta+",\"data\":\""+data.toString()+"\"}}";

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
                job.setJobName("Documento");
                job.print();
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
}