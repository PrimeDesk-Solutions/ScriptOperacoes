/*
    SRF1001 - Recebimentos
    1. Altera a ordem das colunas
    2. Adiciona botão de imprimir documento
 */
import br.com.multitec.utils.ValidacaoException
import br.com.multitec.utils.collections.TableMap
import br.com.multitec.utils.http.HttpRequest
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import multitec.swing.components.autocomplete.MNavigation
import multitec.swing.components.spread.MSpread
import multitec.swing.core.MultitecRootPanel
import multitec.swing.core.dialogs.ErrorDialog
import multitec.swing.request.WorkerRunnable
import multitec.swing.request.WorkerSupplier
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.printing.PDFPageable
import sam.model.entities.ab.Abm01
import sam.swing.tarefas.cgs.CGS5001

import javax.print.DocFlavor
import javax.print.PrintService
import javax.print.PrintServiceLookup;
import javax.swing.*;
import javax.swing.JButton
import java.awt.event.ActionListener
import java.awt.event.ActionEvent
import sam.model.entities.ea.Eaa01;
import sam.swing.tarefas.srf.SRF1001

import java.awt.print.PrinterJob


public class Script extends sam.swing.ScriptBase{
    MultitecRootPanel tarefa
    @Override
    public void execute(MultitecRootPanel tarefa) {
        this.tarefa = tarefa;
        reordenarColunas();
        adicionaBotãoImprimirDocumento();
    }
    private void reordenarColunas(){
        MSpread sprEaa0103s = getComponente("sprEaa0103s")
        sprEaa0103s.getColumnIndex("eaa0103descr") != -1 ? sprEaa0103s.moveColumn(sprEaa0103s.getColumnIndex("eaa0103descr"), 3) : null;
        sprEaa0103s.getColumnIndex("eaa0103umu.aam06codigo") != -1 ? sprEaa0103s.moveColumn(sprEaa0103s.getColumnIndex("eaa0103umu.aam06codigo"), 4) : null;
        sprEaa0103s.getColumnIndex("eaa0103umComl.aam06codigo") != -1 ? sprEaa0103s.moveColumn(sprEaa0103s.getColumnIndex("eaa0103umComl.aam06codigo"), 5) : null;
        sprEaa0103s.getColumnIndex("eaa0103qtComl") != -1 ? sprEaa0103s.moveColumn(sprEaa0103s.getColumnIndex("eaa0103qtComl"), 6) : null;
        sprEaa0103s.getColumnIndex("eaa0103unit") != -1 ? sprEaa0103s.moveColumn(sprEaa0103s.getColumnIndex("eaa0103unit"), 7) : null;
        sprEaa0103s.getColumnIndex("eaa0103total") != -1 ? sprEaa0103s.moveColumn(sprEaa0103s.getColumnIndex("eaa0103total"), 8) : null;
        sprEaa0103s.getColumnIndex("eaa0103totDoc") != -1 ? sprEaa0103s.moveColumn(sprEaa0103s.getColumnIndex("eaa0103totDoc"), 9) : null;
        sprEaa0103s.getColumnIndex("eaa0103totFinanc") != -1 ? sprEaa0103s.moveColumn(sprEaa0103s.getColumnIndex("eaa0103totFinanc"), 10) : null;
        sprEaa0103s.getColumnIndex("eaa0103entrega") != -1 ? sprEaa0103s.moveColumn(sprEaa0103s.getColumnIndex("eaa0103entrega"), 11) : null;
        sprEaa0103s.getColumnIndex("eaa0103ncm.abg01codigo") != -1 ? sprEaa0103s.moveColumn(sprEaa0103s.getColumnIndex("eaa0103ncm.abg01codigo"), 12) : null;
        sprEaa0103s.getColumnIndex("eaa0103ncm.abg01descr") != -1 ? sprEaa0103s.moveColumn(sprEaa0103s.getColumnIndex("eaa0103ncm.abg01descr"), 13) : null;
    }
    private void adicionaBotãoImprimirDocumento(){
        JPanel panel7 = getComponente("panel7");

        def btnImprimir = new JButton();
        btnImprimir.setText("Imprimir");

        // X    Y    W  H
        btnImprimir.setBounds(110, 100, 160, 25);
        panel7.add(btnImprimir);

        panel7.setLayout(null);

        panel7.revalidate();
        panel7.repaint();

        btnImprimir.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                btnImprimirPressed();
            }
        });
    }
    private void btnImprimirPressed() {
        try {
            Eaa01 eaa01 = (Eaa01)  ((SRF1001) tarefa).registro;
            MNavigation nvgAah01codigo = getComponente("nvgAah01codigo");
            String codTipoDoc = nvgAah01codigo.getValue();

            if(eaa01 == null || eaa01.eaa01id == null) interromper("Antes de imprimir é necessário salvar o documento.");

            Long idDocumento = eaa01.eaa01id;

            WorkerSupplier.create(this.tarefa.getWindow(), {
                return buscarDadosImpressao(idDocumento, codTipoDoc);
            })
                    .initialText("Imprimindo Documento")
                    .dialogVisible(true)
                    .success({ bytes ->
                        enviarDadosParaImpressao(bytes);
                    })
                    .start();
        } catch (Exception err) {
            ErrorDialog.defaultCatch(this.tarefa.getWindow(), err);
        }
    }
    private byte[] buscarDadosImpressao(Long idDocumento, String codTipoDoc) {
        String caminhoRelatorio = buscarCaminhoRelatorio(codTipoDoc);
        String json = "{\"nome\":\""+caminhoRelatorio+"\",\"filtros\":{\"eaa01id\":"+idDocumento+"}}"

        ObjectMapper mapper = new ObjectMapper();
        JsonNode obj = mapper.readTree(json);
        return HttpRequest.create().controllerEndPoint("relatorio").methodEndPoint("gerarRelatorio").parseBody(obj).post().getResponseBody()
    }
    private String buscarCaminhoRelatorio(String codTipoDoc){
        String sql = "SELECT aah01formRelDoc FROM aah01 WHERE aah01codigo = '" + codTipoDoc + "'";

        TableMap tmTipoDoc = executarConsulta(sql)[0];

        if(tmTipoDoc == null || tmTipoDoc.size() == 0) throw new ValidacaoException("Não foi encontrado relatório de impressão no tipo de documento " + codTipoDoc);

        return tmTipoDoc.getString("aah01formRelDoc");
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
                    job.setJobName("DANFE");
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

    @Override
    public void preSalvar(boolean salvo) {
    }

    @Override
    public void posSalvar(Long id) {
    }
}