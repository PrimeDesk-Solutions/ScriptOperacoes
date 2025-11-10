package scripts

import multitec.swing.components.autocomplete.MNavigation
import multitec.swing.components.textfields.MTextFieldInteger
import multitec.swing.components.textfields.MTextFieldString

import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.print.PrinterJob

import javax.print.DocFlavor
import javax.print.PrintService
import javax.print.PrintServiceLookup
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JOptionPane

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.printing.PDFPageable

import br.com.multitec.utils.ValidacaoException
import br.com.multitec.utils.collections.TableMap
import br.com.multitec.utils.http.HttpRequest
import multitec.swing.components.spread.MSpread
import multitec.swing.core.MultitecRootPanel
import multitec.swing.core.dialogs.ErrorDialog
import multitec.swing.core.dialogs.Messages
import multitec.swing.request.WorkerRunnable
import multitec.swing.request.WorkerSupplier
import sam.swing.ScriptBase
import sam.swing.tarefas.spv.SPV1001
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
class SCF0101 extends sam.swing.ScriptBase {
    private MultitecRootPanel tarefa;

    public void preSalvar(boolean salvo) {
        // Verifica se foi informado departamento ou natureza no documento
        verificarDepartamentosNaturezas();
    }

    @Override
    public void execute(MultitecRootPanel tarefa) {
        this.tarefa = tarefa;
        adicionaBotaoImprimirDoc()
    }

    private void adicionaBotaoImprimirDoc(){
        def tela = tarefa.getWindow();
        tela.setBounds((int) tela.getBounds().x, (int) tela.getBounds().y, (int) tela.getBounds().width, (int) tela.getBounds().height + 40);

        JButton btnImprimir = new JButton();
        btnImprimir.setText("Imprimir Boleto");
        tarefa.add(btnImprimir);
        // X    Y    W  H
        btnImprimir.setBounds(800,635, 175, 36);

        btnImprimir.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                selectionButtonPressed();
            }
        });

    }
    private void verificarDepartamentosNaturezas(){
        MSpread sprDaa0101s = getComponente("sprDaa0101s");
        MSpread sprDaa01011s = getComponente("sprDaa01011s");

        def spreadDepartamento = sprDaa0101s.value();
        def spreadNaturezas = sprDaa01011s.value();

        if(spreadDepartamento.size() == 0 || spreadNaturezas.size() == 0 ) interromper("Não é permitido a inclusão de documentos sem departamentos ou naturezas.")
    }
    private void selectionButtonPressed() {

        try{

            MSpread sprDaa0102s = getComponente("sprDaa0102s");
            def spread = sprDaa0102s.getValue();
            MTextFieldInteger txtAbb01num = getComponente("txtAbb01num");
            MTextFieldString txtAbb01serie = getComponente("txtAbb01serie");
            MNavigation nvgAbe01codigo = getComponente("nvgAbe01codigo");
            MTextFieldString txtAbb01parcela = getComponente("txtAbb01parcela");
            MTextFieldInteger txtAbb01quita = getComponente("txtAbb01quita");
            MNavigation nvgAbf01codigo = getComponente("nvgAbf01codigo");
            Integer numDoc = txtAbb01num.getValue();
            String serie = txtAbb01serie.getValue();
            String codigoEnt =nvgAbe01codigo.getValue();
            String parcela =txtAbb01parcela.getValue();
            Integer quita = txtAbb01quita.getValue();
            String codBanco = nvgAbf01codigo.getValue();

            //Verifica se existe integração bancária no documento
            if(spread.size() == 0){
                interromper("Não foi Possível Gerar Boleto, Documento Sem Integração Bancária");
            }

            //SQL para buscar ID do documento financeiro selecionado em tela
            TableMap documento = executarConsulta( "SELECT daa01id "+
                    "FROM daa01 "+
                    "INNER JOIN abb01 ON abb01id = daa01central "+
                    "INNER JOIN abe01 abe01 ON abe01id = abb01ent "+
                    "WHERE abb01num = '"+numDoc+"' "+
                    "AND abe01codigo = '"+codigoEnt+"' "+
                    "AND abb01serie = '"+serie+"' "+
                    "AND abb01parcela = '"+parcela+"' "+
                    "AND abb01quita = '"+quita+"' ")[0];

            if(documento.size() == 0) interromper("Nenhum documento financeiro encontrado para impressão do boleto.");

            Long idDoc = documento.getLong("daa01id")

            WorkerSupplier.create(this.tarefa.getWindow(), {
                return buscarDadosImpressao(idDoc, codBanco);
            })
                    .initialText("Imprimindo BOLETO")
                    .dialogVisible(true)
                    .success({ bytes ->
                        enviarDadosParaImpressao(bytes, idDoc);
                    })
                    .start();
        } catch (Exception err) {
            ErrorDialog.defaultCatch(this.tarefa.getWindow(), err);
        }
    }

    private byte[] buscarDadosImpressao(Long idDoc, String codBanco) {
        String json = "{\"nome\":\"Silcon.relatorios.scf.SCF_Boleto_Itau\",\"filtros\":{\"daa01id\":"+idDoc+"}}";

        ObjectMapper mapper = new ObjectMapper();
        JsonNode obj = mapper.readTree(json);
        return HttpRequest.create().controllerEndPoint("relatorio").methodEndPoint("gerarRelatorio").parseBody(obj).post().getResponseBody()
    }

    protected void enviarDadosParaImpressao(byte[] bytes, Long idDoc) {
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
                    job.setJobName("Boleto " + idDoc);
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