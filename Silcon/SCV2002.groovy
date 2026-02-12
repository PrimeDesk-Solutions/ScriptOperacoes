/*
    SCV2001 - Pedidos de Venda
    1. Insere evento no botão ESC para perguntar se deseja realmente sair
    2. Altera a lista de busca (F4) das entidade e dos itens
    3. Insere botão de impressão de documentos
    4. Exibe uma mensagem se deseja recalcular os itens ao trocar a tabela de preço
    5. Remove a coluna abm01na e altera a posição da coluna abm01descr
 */
import br.com.multitec.utils.ValidacaoException
import br.com.multitec.utils.collections.TableMap
import br.com.multitec.utils.http.HttpRequest
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import multitec.swing.components.autocomplete.MNavigation
import multitec.swing.core.MultitecRootPanel
import multitec.swing.core.dialogs.ErrorDialog
import multitec.swing.request.WorkerRunnable
import multitec.swing.request.WorkerSupplier
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.printing.PDFPageable
import sam.model.entities.ab.Abm01
import sam.swing.tarefas.cgs.CGS5001
import sam.swing.tarefas.scv.SCV2002
import javax.print.DocFlavor
import javax.print.PrintService
import javax.print.PrintServiceLookup;
import javax.swing.*;
import javax.swing.JButton
import java.awt.event.ActionListener
import java.awt.event.ActionEvent
import sam.model.entities.ea.Eaa01;
import sam.model.entities.ea.Eaa0103;
import sam.swing.tarefas.srf.SRF1001
import br.com.multitec.utils.UiSqlColumn;
import multitec.swing.components.spread.MSpread

import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.print.PrinterJob


public class Script extends sam.swing.ScriptBase{
    MultitecRootPanel tarefa
    public Runnable windowLoadOriginal;

    @Override
    public void execute(MultitecRootPanel tarefa) {
        this.tarefa = tarefa;
        adicionarEventoTabelaPreco();
        tarefa.getWindow().getJMenuBar().getMnuArquivo().getMniCancelar().addActionListener(mnu -> this.adicionaEventoESC(mnu));
        this.windowLoadOriginal = tarefa.windowLoad ;
        tarefa.windowLoad = {novoWindowLoad()};
        moverColunas();
        ocultarColunaSpread();
        adicionaBotãoImprimirDocumento();
    }
    private void adicionarEventoTabelaPreco(){
        MNavigation nvgAbe40codigo = getComponente("nvgAbe40codigo");
        nvgAbe40codigo.addFocusListener(new FocusListener() {
            @Override
            void focusGained(FocusEvent e) {}

            @Override
            void focusLost(FocusEvent e) {
                recalcularItens();
            }
        })
    }
    private void recalcularItens(){
        MSpread sprEaa0103s = getComponente("sprEaa0103s");
        List<Eaa0103> eaa0103s = sprEaa0103s.getValue();
        if(eaa0103s.size() == 0) return;
        if(exibirQuestao("Deseja recalcular os itens?")){
            JButton btnRecalcularDocumento = getComponente("btnRecalcularDocumento");

            for (eaa0103 in eaa0103s){
                eaa0103.eaa0103unit = new BigDecimal(0);
            }
            sprEaa0103s.refreshAll();
            btnRecalcularDocumento.doClick();
        }

    }
    protected void adicionaEventoESC(ActionEvent evt) {
        if(!exibirQuestao("Deseja realmente sair sem SALVAR?")) interromper("Por favor salvar antes de SAIR.");
    }
    protected void novoWindowLoad(){
        this.windowLoadOriginal.run();

        def ctrAbb01ent = getComponente("ctrAbb01ent");
        def ctrEaa0103item = getComponente("ctrEaa0103item");

        ctrAbb01ent.f4Columns = () -> {
            java.util.List<UiSqlColumn> uiSqlColumn = new ArrayList<>();
            UiSqlColumn abe01codigo = new UiSqlColumn("abe01codigo", "abe01codigo", "Código", 10);
            UiSqlColumn abe01na = new UiSqlColumn("abe01na", "abe01na", "Nome Abreviado", 40);
            UiSqlColumn abe01nome = new UiSqlColumn("abe01nome", "abe01nome", "Nome", 60);
            UiSqlColumn abe01ni = new UiSqlColumn("abe01ni", "abe01ni", "Número da Inscrição", 60);
            uiSqlColumn.addAll(Arrays.asList(abe01codigo, abe01na, abe01nome, abe01ni));
            return uiSqlColumn;
        };

        ctrEaa0103item.f4Columns = () ->{
            java.util.List<UiSqlColumn> uiSqlColumn = new ArrayList<>();
            UiSqlColumn abm01codigo = new UiSqlColumn("abm01codigo", "abm01codigo", "Código", 20);
            UiSqlColumn abm01gtin = new UiSqlColumn("abm01gtin", "abm01gtin", "GTIN", 20);
            UiSqlColumn abm01descr = new UiSqlColumn("abm01descr", "abm01descr", "Descrição", 120);
            UiSqlColumn abm01complem = new UiSqlColumn("abm01complem", "abm01complem", "Fabricante", 255);
            uiSqlColumn.addAll(Arrays.asList(abm01codigo,abm01gtin, abm01descr, abm01complem));
            return uiSqlColumn;
        }

    }
    private void moverColunas() {
        MSpread sprEaa0103s = getComponente("sprEaa0103s")
        //mover coluna
        sprEaa0103s.getColumnIndex("eaa0103descr") != -1 ? sprEaa0103s.moveColumn(sprEaa0103s.getColumnIndex("eaa0103descr"), 5) : null //nome completo do item
    }
    private void ocultarColunaSpread(){
        MSpread sprEaa0103s = getComponente("sprEaa0103s");
        ocultarColunas(sprEaa0103s, 4)
    }
    private void adicionaBotãoImprimirDocumento(){
        JPanel panel7 = getComponente("panel7");

        def btnImprimir = new JButton();
        btnImprimir.setText("Imprimir");

        // X    Y    W  H
        btnImprimir.setBounds(110, 100, 160, 22);
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
            Eaa01 eaa01 = (Eaa01)  ((SCV2002) tarefa).registro;
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