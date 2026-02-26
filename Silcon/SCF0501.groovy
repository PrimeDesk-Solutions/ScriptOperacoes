import br.com.multitec.utils.ValidacaoException
import multitec.swing.core.MultitecRootPanel;
import br.com.multitec.utils.collections.TableMap
import com.amazonaws.protocol.json.internal.JsonMarshaller
import multitec.swing.core.MultitecRootPanel
import multitec.swing.core.utils.WindowUtils
import multitec.swing.request.WorkerRunnable
import multitec.swing.request.WorkerSupplier
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.printing.PDFPageable
import sam.model.entities.da.Daa10
import sam.swing.tarefas.scf.SCF0101
import sam.swing.tarefas.scf.SCF0150

import javax.print.DocFlavor
import javax.print.PrintService
import javax.print.PrintServiceLookup
import javax.swing.JButton
import javax.swing.JComboBox;
import javax.swing.JLabel
import javax.swing.JOptionPane;
import java.awt.Rectangle;
import java.awt.Point;
import multitec.swing.components.textfields.MTextFieldBigDecimal;
import javax.swing.JPanel
import java.awt.event.ActionEvent
import java.awt.event.ActionListener;
import multitec.swing.components.spread.MSpread;
import multitec.swing.components.spread.MSpread
import javax.swing.event.TableModelEvent
import javax.swing.event.TableModelListener
import java.awt.event.ActionListener
import sam.dto.cgs.CGS2050DocumentoSCFDto
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.print.PrinterJob
import java.time.temporal.ChronoUnit
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import multitec.swing.core.dialogs.ErrorDialog;
import java.nio.file.Files
import java.nio.file.Paths
import java.awt.Desktop
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import br.com.multitec.utils.http.HttpRequest
import multitec.swing.components.textfields.MTextFieldLocalDate;
import multitec.swing.components.MCheckBox;
import multitec.swing.components.autocomplete.MNavigation;






public class Script extends sam.swing.ScriptBase{
    MTextFieldBigDecimal txtTotalDesconto = new MTextFieldBigDecimal();
    MTextFieldBigDecimal txtTotalMulta = new MTextFieldBigDecimal();
    MTextFieldBigDecimal txtTotalJuros = new MTextFieldBigDecimal();
    MTextFieldBigDecimal txtTotalEncargos = new MTextFieldBigDecimal();
    MTextFieldBigDecimal txtTotalGeral = new MTextFieldBigDecimal();
    JLabel lblTotalGeral = new JLabel();
    private boolean preenchendoSpread = false;
    MultitecRootPanel tarefa;
    @Override
    public void execute(MultitecRootPanel tarefa) {
        this.tarefa = tarefa;
        criarBotaoImprimirRelatorio();
        adicionarEventoSprEntidadeRealizarAReceber();
        adicionarEventoBtnMostrar();
        reordenarColunas();
        alterarPosicoesComponentes();
        adicionarEventoSpread();
        criarComponentes();
    }
    private void criarBotaoImprimirRelatorio(){
        JPanel pnlRealizarEntidade = getComponente("pnlRealizarEntidade");

        JButton btnImprimir = new JButton();
        btnImprimir.setText("Imprimir Fechamento Cliente");
        btnImprimir.setBounds(800, 495, 200, 30);

        btnImprimir.addActionListener(new ActionListener() {
            @Override
            void actionPerformed(ActionEvent e) {
                try{
                    btnImprimirPressed();
                }catch(Exception ex){
                    interromper("Falha ao salvar PDF: " + ex.getMessage())
                }
            }
        })

        pnlRealizarEntidade.add(btnImprimir);

    }
    private void btnImprimirPressed() {
        try {
            MCheckBox chkEmissaoRealizar = getComponente("chkEmissaoRealizar");
            MTextFieldLocalDate txtDataInicialEntEmissaoRealizar = getComponente("txtDataInicialEntEmissaoRealizar");
            MTextFieldLocalDate txtDataFinalEntEmissaoRealizar = getComponente("txtDataFinalEntEmissaoRealizar");
            MCheckBox chkVencimento = getComponente("chkVencimento");
            MTextFieldLocalDate txtDataInicialEntVencimento = getComponente("txtDataInicialEntVencimento");
            MTextFieldLocalDate txtDataFinallEntVencimento = getComponente("txtDataFinalEntVencimento");
            MNavigation nvgAbe01codigoEntidade = getComponente("nvgAbe01codigoEntidade");

            if(nvgAbe01codigoEntidade.getValue() == null) throw new ValidacaoException("Informe a entidade para realizar a impressão.")

            Long idEntidade = buscarIdEntidade(nvgAbe01codigoEntidade.getValue());
            LocalDate dtEmisIni = null;
            LocalDate dtEmisFin = null;
            LocalDate dtVctoIni = null;
            LocalDate dtVctoFin = null;

            if(chkEmissaoRealizar.isSelected()){
                dtEmisIni = txtDataInicialEntEmissaoRealizar.getValue();
                dtEmisFin = txtDataFinalEntEmissaoRealizar.getValue();
            }

            if(chkVencimento.isSelected()){
                dtVctoIni = txtDataInicialEntVencimento.getValue();
                dtVctoFin = txtDataFinallEntVencimento.getValue();
            }

            WorkerSupplier.create(this.tarefa.getWindow(), {
                return buscarDadosImpressao(idEntidade, dtEmisIni, dtEmisFin, dtVctoIni, dtVctoFin);
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
    private byte[] buscarDadosImpressao(Long idEntidade, LocalDate dtEmisIni, LocalDate dtEmisFin,  LocalDate dtVctoIni, LocalDate dtVctoFin ) {
        String json = "{\"nome\":\"Silcon.relatorios.customizados.CST_Fechamento_Cliente\",\"filtros\":{\"abe01id\":\""+idEntidade+"\",\"dtEmisIni\":\""+dtEmisIni.toString()+"\",\"dtEmisFin\":\""+dtEmisFin.toString()+"\",\"dtVctoIni\":\""+dtVctoIni.toString()+"\",\"dtVctoFin\":\""+dtVctoFin.toString()+"\"}}";

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
                    job.setJobName("Movimento");
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

    private Long buscarIdEntidade(String codEntidade){
        String sql = "SELECT abe01id FROM abe01 WHERE abe01codigo = '" + codEntidade + "'";

        TableMap tmEntidade = executarConsulta(sql)[0];

        if(tmEntidade == null || tmEntidade.size() == 0) throw new ValidacaoException("Não foi encontrada a entidade com o código " + codEntidade);

        return tmEntidade.getLong("abe01id");
    }

    private void adicionarEventoBtnMostrar(){
        JButton btnMostrarEntRealizar = getComponente("btnMostrarEntRealizar");
        btnMostrarEntRealizar.addActionListener(new ActionListener() {
            @Override
            void actionPerformed(ActionEvent e) {
                zerarCamposCustomizados();
                preenchendoSpread = true;
            }
        })
    }
    private void adicionarEventoSprEntidadeRealizarAReceber() {
        MSpread sprEntidadeRealizarAReceber = getComponente("sprEntidadeRealizarAReceber");
        sprEntidadeRealizarAReceber.addMouseListener(new MouseAdapter() {
            @Override
            void mouseClicked(MouseEvent e) {
                sprEntidadeRealizarAReceberMouseClicked(e);
            }
        })
    }
    private void  sprEntidadeRealizarAReceberMouseClicked(MouseEvent e){
        MSpread sprEntidadeRealizarAReceber = getComponente("sprEntidadeRealizarAReceber");
        try{
            if(e.getClickCount() == 2 && sprEntidadeRealizarAReceber.getSelectedRow() >= 0){
                int row = sprEntidadeRealizarAReceber.getSelectedRow();
                if(row < 0) return;
                Long idDocFinanc = ((TableMap)sprEntidadeRealizarAReceber.get(row)).getLong("daa01id");

                SCF0101 scf0101 = new SCF0101();
                WindowUtils.createJDialog(scf0101.getWindow(), scf0101);
                scf0101.cancelar = () -> scf0101.getWindow().dispose();
                scf0101.exibirPanelListaCadastro = () -> scf0101.getWindow().dispose();
                scf0101.editar(idDocFinanc);
                scf0101.getWindow().setVisible(true);
            }
        }catch(Exception err){
            ErrorDialog.defaultCatch(tarefa.getWindow(), err);
        }
    }
    private void reordenarColunas(){
        MSpread sprEntidadeRealizarAReceber = getComponente("sprEntidadeRealizarAReceber");
        sprEntidadeRealizarAReceber.getColumnIndex("daa01json.desconto") != -1 ? sprEntidadeRealizarAReceber.moveColumn(sprEntidadeRealizarAReceber.getColumnIndex("daa01json.desconto"), 9) : null;
        sprEntidadeRealizarAReceber.getColumnIndex("daa01json.multa") != -1 ? sprEntidadeRealizarAReceber.moveColumn(sprEntidadeRealizarAReceber.getColumnIndex("daa01json.multa"), 10) : null;
        sprEntidadeRealizarAReceber.getColumnIndex("daa01json.juros") != -1 ? sprEntidadeRealizarAReceber.moveColumn(sprEntidadeRealizarAReceber.getColumnIndex("daa01json.juros"), 11) : null;
        sprEntidadeRealizarAReceber.getColumnIndex("daa01json.encargos") != -1 ? sprEntidadeRealizarAReceber.moveColumn(sprEntidadeRealizarAReceber.getColumnIndex("daa01json.encargos"), 12) : null;
    }
    private void alterarPosicoesComponentes(){
        MTextFieldBigDecimal txtTotalEntidadeRealizarAReceber = getComponente("txtTotalEntidadeRealizarAReceber");        JLabel lblTotalAReceber = getComponente("lblTotalAReceber");

        txtTotalEntidadeRealizarAReceber.setBounds(160, 275, 137, 25);
        lblTotalAReceber.setBounds(100, 275, 137, 25);
    }
    private void criarComponentes(){
        JPanel pnlRealizarEntidade = getComponente("pnlRealizarEntidade");

        // Lbl Descontos
        txtTotalDesconto.setBounds(300, 275, 137, 25);
        txtTotalDesconto.setMaxValue(new BigDecimal("99999999999999.99"));
        txtTotalDesconto.setEditable(false);
        txtTotalDesconto.setEnabled(false);

        // Lbl Multa
        txtTotalMulta.setBounds(435, 275, 137, 25);
        txtTotalMulta.setMaxValue(new BigDecimal("99999999999999.99"));
        txtTotalMulta.setEditable(false);
        txtTotalMulta.setEnabled(false);
//
        // Lbl Juros
        txtTotalJuros.setBounds(570, 275, 137, 25);
        txtTotalJuros.setMaxValue(new BigDecimal("99999999999999.99"));
        txtTotalJuros.setEditable(false);
        txtTotalJuros.setEnabled(false);

        // Lbl Encargos
        txtTotalEncargos.setBounds(705, 275, 137, 25);
        txtTotalEncargos.setMaxValue(new BigDecimal("99999999999999.99"));
        txtTotalEncargos.setEditable(false);
        txtTotalEncargos.setEnabled(false);

        // Lbl Txt Total Geral
        lblTotalGeral.setText("Total Geral");
        lblTotalGeral.setBounds(850, 275, 137, 25);

        // Txt Total Geral
        txtTotalGeral.setBounds(920, 275, 137, 25);
        txtTotalGeral.setMaxValue(new BigDecimal("99999999999999.99"));
        txtTotalGeral.setEditable(false);
        txtTotalGeral.setEnabled(false);

        pnlRealizarEntidade.add(txtTotalDesconto);
        pnlRealizarEntidade.add(txtTotalMulta);
        pnlRealizarEntidade.add(txtTotalJuros);
        pnlRealizarEntidade.add(txtTotalEncargos);
        pnlRealizarEntidade.add(lblTotalGeral);
        pnlRealizarEntidade.add(txtTotalGeral);
    }
    private void adicionarEventoSpread(){
        MSpread sprEntidadeRealizarAReceber = getComponente("sprEntidadeRealizarAReceber");

        sprEntidadeRealizarAReceber.getModel().addTableModelListener(e -> {

            if(e.getType() != TableModelEvent.UPDATE) return;

            // Não executa quando ordena a spread
            if(!preenchendoSpread) return;

            callback();
        });
    }
    private void callback(){
        MSpread sprEntidadeRealizarAReceber = getComponente("sprEntidadeRealizarAReceber");
        int size = sprEntidadeRealizarAReceber.getValue().size();
        if(size > 0) somarCamposLivresSpread(sprEntidadeRealizarAReceber.getValue())
    }
    private void somarCamposLivresSpread(List<TableMap> vlrSpread){

        BigDecimal totDesconto = BigDecimal.ZERO;
        BigDecimal totMulta = BigDecimal.ZERO;
        BigDecimal totJuros = BigDecimal.ZERO;
        BigDecimal totEncargos = BigDecimal.ZERO;
        BigDecimal totDocs = BigDecimal.ZERO;

        for(registro in vlrSpread ){
            Integer diasAtraso = registro.getInteger("dias");
            diasAtraso < 0 ? registro.put("daa01json.juros", registro.getBigDecimal_Zero("daa01json.juros") * diasAtraso.abs()) : registro.put("daa01json.juros", BigDecimal.ZERO)
            totDesconto += registro.getBigDecimal_Zero("daa01json.desconto");
            totMulta += registro.getBigDecimal_Zero("daa01json.multa");
            totJuros += registro.getBigDecimal_Zero("daa01json.juros");
            totEncargos += registro.getBigDecimal_Zero("daa01json.encargos");
            totDocs += registro.getBigDecimal("valor");
        }

        BigDecimal totGeral = totDocs + totMulta + totJuros + totEncargos - totDesconto.abs();

        txtTotalDesconto.setValue(totDesconto.abs().round(2));
        txtTotalMulta.setValue(totMulta.round(2));
        txtTotalJuros.setValue(totJuros.round(2));
        txtTotalEncargos.setValue(totEncargos.round(2));
        txtTotalGeral.setValue(totGeral.round(2));

        preenchendoSpread = false;

    }
    private void zerarCamposCustomizados(){
        txtTotalDesconto.setValue(BigDecimal.ZERO)
        txtTotalMulta.setValue(BigDecimal.ZERO)
        txtTotalJuros.setValue(BigDecimal.ZERO)
        txtTotalEncargos.setValue(BigDecimal.ZERO)
        txtTotalGeral.setValue(BigDecimal.ZERO)
    }
}