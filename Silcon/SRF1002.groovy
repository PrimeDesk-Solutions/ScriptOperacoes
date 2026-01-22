/*
    SRF1002 - DOCUMENTOS DE VENDAS (NOTAS)
    FUNÇÃO:

    1. Exibe observação de uso interno (Abe02obsUsoInt) da entidade (se existir).
    2. Verifica títulos vencidos:
       - Se houver, pergunta se deseja continuar.
       - Se sim, solicita observação para registrar no documento.
    3. Validação do limite de crédito:
       - Verifica data limite (se vencida, interrompe).
       - Calcula saldo devedor:
            - DAA01 (Docs a Receber): abb01quita = 0 AND daa01rp = 0
            - EAA01 (Financeiro 2-Batch): abb10tipoCod = 1 AND eaa01esMov = 1 AND eaa01clasDoc = 1 AND eaa01cancData IS NULL AND eaa01iSCF = 2
            - Saldo devedor = soma(Doc. a Receber + Doc. Batch)
       - Se saldo > limite, pergunta se deseja continuar e solicita observação.
    4. Ao salvar, valida novamente o limite (sem solicitar nova observação).
    5. Adiciona evento ao pressionar a tecla ESC, perguntando se realmente deseja sair
 */
package scripts

import br.com.multitec.utils.UiSqlColumn
import br.com.multitec.utils.collections.TableMap
import multitec.swing.components.autocomplete.MNavigation
import multitec.swing.components.textfields.MTextArea
import multitec.swing.core.MultitecRootPanel
import multitec.swing.core.utils.WindowUtils

import java.awt.event.FocusEvent
import java.awt.event.FocusListener;
import javax.swing.SwingUtilities
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter;
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
import multitec.swing.core.MultitecRootPanel;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import sam.model.entities.ea.Eaa0103;
import sam.model.entities.ea.Eaa01;
import sam.model.entities.ab.Abm01;
import java.util.Comparator;
import multitec.swing.core.dialogs.Messages;
import multitec.swing.request.WorkerRequest;
import multitec.swing.request.WorkerRunnable;
import sam.swing.tarefas.srf.SRF1002;
import br.com.multitec.utils.UiSqlColumn;


public class SRF1002 extends sam.swing.ScriptBase{
    def strTexto = "";
    String obs = "";
    Integer cancelou = 0;
    MultitecRootPanel tarefa;
    public Runnable  windowLoadOriginal;

    @Override
    public void execute(MultitecRootPanel tarefa) {
        this.tarefa = tarefa;
        tarefa.getWindow().getJMenuBar().getMnuArquivo().getMniCancelar().addActionListener(mnu -> this.adicionaEventoESC(mnu));
        criarMenu("Impressão", "Imprimir Documento", { btnImprimirPressed() }, null);
        this.windowLoadOriginal = tarefa.windowLoad ;
        tarefa.windowLoad = {novoWindowLoad()};
        adicionaEventoPCD();
        adicionaBotãoImprimirDanfe();
    }
    protected void novoWindowLoad(){
        this.windowLoadOriginal.run();

        def ctrAbb01ent = getComponente("ctrAbb01ent");

        ctrAbb01ent.f4Columns = () -> {
            java.util.List<UiSqlColumn> uiSqlColumn = new ArrayList<>();
            UiSqlColumn abe01codigo = new UiSqlColumn("abe01codigo", "abe01codigo", "Código", 10);
            UiSqlColumn abe01na = new UiSqlColumn("abe01na", "abe01na", "Nome Abreviado", 40);
            UiSqlColumn abe01nome = new UiSqlColumn("abe01nome", "abe01nome", "Nome", 60);
            UiSqlColumn abe01ni = new UiSqlColumn("abe01ni", "abe01ni", "Número da Inscrição", 60);
            uiSqlColumn.addAll(Arrays.asList(abe01codigo, abe01na, abe01nome, abe01ni));
            return uiSqlColumn;
        };
    }
    private void adicionaBotãoImprimirDanfe(){
        JPanel panel7 = getComponente("panel7");
        def tela = tarefa.getWindow();
        tela.setBounds((int) tela.getBounds().x, (int) tela.getBounds().y, (int) tela.getBounds().width, (int) tela.getBounds().height + 40);

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
    private void adicionaEventoPCD(){
        MNavigation nvgAbd01codigo = getComponente("nvgAbd01codigo");
        Long idEmpresa = obterEmpresaAtiva().getAac10id();
        MTextArea txtEaa01obsUsoInt = getComponente("txtEaa01obsUsoInt");

        nvgAbd01codigo.addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent e) {};

            public void focusLost(FocusEvent e) {
                MNavigation nvgAbe01codigo = getComponente("nvgAbe01codigo");
                String codEntidade = nvgAbe01codigo.getValue();

                if(codEntidade != null) {
                    String msgEnt = buscarMensagemEntidade(codEntidade, idEmpresa);
                    if (msgEnt != null) exibirTelaDeAtencaoComMensagemEntidade(msgEnt);

                    // Soma de Titulos Vencidos
                    TableMap totalTituloVencido = buscarTitulosVencidosEntidade(codEntidade, idEmpresa);
                    if(totalTituloVencido.getBigDecimal_Zero("totDoc") > 0) exibirCaixaDeDialogoTituloVencido(tarefa); // exibe a caixa de dialogo para observações

                    // Verifica Limite de Crédito da entidade
                    TableMap tmAbe01 = buscarInformacoesLimiteCreditoEntidade(codEntidade, idEmpresa);

                    if(tmAbe01.size() > 0 && tmAbe01.getTableMap("abe01json").getDate("dt_vcto_lim_credito") != null) verificarLimiteDeCreditoIniciar(tmAbe01, codEntidade, idEmpresa, tarefa);

                    txtEaa01obsUsoInt.setValue(strTexto);
                }
            }
        });
    }
    protected void adicionaEventoESC(ActionEvent evt) {
        if(!exibirQuestao("Deseja realmente sair sem SALVAR?")) interromper("Por favor salvar antes de SAIR.");
    }
    private TableMap buscarTitulosVencidosEntidade(String codEntidade, Long idEmpresa){
        Long idEntidade = buscarIdEntidade(codEntidade, idEmpresa);

        String sql = " SELECT SUM(daa01valor) AS totDoc " +
                " FROM daa01 " +
                " INNER JOIN abb01 ON abb01id = daa01central " +
                " WHERE daa01rp = 0 " +
                " AND abb01quita = 0 " +
                " AND daa01dtVctoR < current_date " +
                " AND abb01ent = " + idEntidade.toString() +
                " AND daa01gc = " + idEmpresa.toString();

        return executarConsulta(sql)[0]
    }


    private Long buscarIdEntidade(String codEntidade, Long idEmpresa){
        String sql = "SELECT abe01id FROM abe01 WHERE abe01codigo = '" + codEntidade + "' AND abe01gc = " + idEmpresa.toString();
        TableMap tmEntidade = executarConsulta(sql)[0];
        Long idEntidade = tmEntidade.getLong("abe01id");

        return idEntidade;
    }

    private void exibirCaixaDeDialogoTituloVencido(MultitecRootPanel tarefa){
        def swing = new groovy.swing.SwingBuilder();
        def lblTexto;
        def txtArea;
        if(exibirQuestao("Constam títulos vencidos para esse cliente, necessário consultar financeiro. Deseja continuar?")){
            swing.edt {
                dialog(title:"Observação de Aprovação", size:[500,250], defaultCloseOperation:javax.swing.JFrame.DISPOSE_ON_CLOSE, show:true, modal:true, locationRelativeTo:null) {
                    borderLayout()
                    lblTexto = label(text:"Consta títulos vencidos. Informe Autorizador.", constraints: java.awt.BorderLayout.NORTH)
                    scrollPane(){
                        txtArea = textArea(text:"", constraints:java.awt.BorderLayout.CENTER, rows:50, columns:65)
                    }
                    button(text:'Ok', actionPerformed: {strTexto = txtArea.text; dispose()}, constraints:java.awt.BorderLayout.SOUTH)
                }
            }
        }else{
            exibirInformacao("Processo Interrompido");
            tarefa.dispose(); // fecha a tarefa
        }

    }

    private String buscarMensagemEntidade(String codEntidade, Long idEmpresa){
        Long idEntidade = buscarIdEntidade(codEntidade, idEmpresa);
        String sql = "SELECT abe02obsUsoInt FROM abe02 WHERE abe02ent = " + idEntidade.toString();

        TableMap tmEntidade =  executarConsulta(sql)[0]

        return tmEntidade.getString("abe02obsUsoInt");
    }

    private void exibirTelaDeAtencaoComMensagemEntidade(String msg){

        String caminhoImagem = "H:/Server/homes/Sam4/imagens/atencao.png";

        // Caixa de Mensagem
        JDialog dialog = new JDialog((Frame) null, "Mensagem de Alerta", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setResizable(false);

        // Painel principal
        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setBorder(new EmptyBorder(8, 8, 8, 8)); // margem externa
        dialog.setContentPane(content);

        // --- Top: título centralizado em vermelho
        JLabel titulo = new JLabel("*** A T E N Ç Ã O ***", SwingConstants.CENTER);
        titulo.setFont(new Font("Arial", Font.BOLD, 18));
        titulo.setForeground(Color.RED);

        // Painel para manter título com pequena margem
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(titulo, BorderLayout.CENTER);
        content.add(topPanel, BorderLayout.NORTH);

        // --- Centro: área de texto não-editável dentro de um JScrollPane
        JTextArea txt = new JTextArea();
        txt.setText(msg); // texto inicial
        txt.setEditable(false);
        txt.setLineWrap(true);
        txt.setWrapStyleWord(true);
        txt.setFont(new Font("Arial", Font.PLAIN, 14));
        txt.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        JScrollPane scroll = new JScrollPane(txt);
        scroll.setPreferredSize(new Dimension(620, 260));
        content.add(scroll, BorderLayout.CENTER);

        // --- Sul: imagem + botão OK (imagem acima do botão)
        JPanel south = new JPanel();
        south.setLayout(new BoxLayout(south, BoxLayout.Y_AXIS));
        south.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Carrega imagem
        String imagemPath = caminhoImagem;
        JLabel imagemLabel = new JLabel();
        imagemLabel.setAlignmentX(Component.CENTER_ALIGNMENT); // centralizar horizontalmente
        try {
            BufferedImage img = ImageIO.read(new File(imagemPath));
            // opcional: redimensionar mantendo proporção para largura fixa
            int targetWidth = 220;
            int w = img.getWidth();
            int h = img.getHeight();
            int targetHeight = (targetWidth * h) / w;
            Image scaled = img.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
            imagemLabel.setIcon(new ImageIcon(scaled));
        } catch (IOException e) {
            // se não encontrar, mostra texto substituto
            imagemLabel.setText("Imagem não encontrada: " + imagemPath);
            imagemLabel.setFont(new Font("Arial", Font.ITALIC, 12));
        }
        south.add(imagemLabel);
        south.add(Box.createRigidArea(new Dimension(0, 8)));

        // Botão OK centralizado
        JButton btnOk = new JButton("OK");
        btnOk.setFont(new Font("Arial", Font.BOLD, 16));
        btnOk.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnOk.setPreferredSize(new Dimension(120, 28));
        btnOk.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.dispose();
            }
        });

        south.add(btnOk);

        content.add(south, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setLocationRelativeTo(null); // centraliza na tela
        dialog.setVisible(true);
    }

    private TableMap buscarInformacoesLimiteCreditoEntidade(String codEntidade, Long idEmpresa) {
        Long idEntidade = buscarIdEntidade(codEntidade, idEmpresa);

        String sql = "SELECT abe01json FROM abe01 WHERE abe01id = " + idEntidade.toString();

        return executarConsulta(sql)[0];
    }

    private void verificarLimiteDeCreditoIniciar(TableMap jsonAbe01, String codEntidade, Long idEmpresa, MultitecRootPanel tarefa){


        BigDecimal vlrLimiteCredito = jsonAbe01.getTableMap("abe01json").getBigDecimal_Zero("vlr_lim_credito");
        LocalDate dataAtual = LocalDate.now();
        LocalDate dtVencLimCredito = jsonAbe01.getTableMap("abe01json").getDate("dt_vcto_lim_credito");
        def swing = new groovy.swing.SwingBuilder();
        def lblTexto;
        def txtArea;

        if(vlrLimiteCredito >= 0){
            if(dtVencLimCredito < dataAtual){ // Data de vencimento de crédito menor que data atual, significa expirou
                interromper("Data de vencimento do limite de crédito do cliente venceu em " + dtVencLimCredito.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")).toString() + ".")
            }

            BigDecimal vlrDocumentosReceber = somarDocsAReceber(codEntidade, idEmpresa);

            // Soma todos os documentos emitidos para a entidade com SCF = 2-Batch
            BigDecimal valorDocumentosEmitidos = buscarSomaDocumentosEmitidos(codEntidade, idEmpresa);

            // Calcula o valor total devedor
            BigDecimal valorTotalDevedor = vlrDocumentosReceber + valorDocumentosEmitidos;

            // Se o total devedor do cliente for maior que o limite de crédito, significa que houve inconsistências e precisa se analisada
            if(valorTotalDevedor > vlrLimiteCredito){
                if(exibirQuestao("Limite de crédito ultrapassado, necessario consultar o financeiro. Deseja continuar?")){
                    swing.edt {
                        dialog(title:"Observação de Aprovação", size:[500,250], defaultCloseOperation:javax.swing.JFrame.DISPOSE_ON_CLOSE, show:true, modal:true, locationRelativeTo:null) {
                            borderLayout()
                            lblTexto = label(text:"Limite de crédito ultrapassado.", constraints: java.awt.BorderLayout.NORTH)
                            scrollPane(){
                                txtArea = textArea(text:"", constraints:java.awt.BorderLayout.CENTER, rows:50, columns:65)
                            }
                            button(text:'Ok', actionPerformed: {strTexto = strTexto + "\n" + txtArea.text; dispose()}, constraints:java.awt.BorderLayout.SOUTH)
                        }
                    }
                }else{
                    exibirInformacao("Processo Interrompido");
                    tarefa.dispose(); // fecha a tarefa

                }
            }
        }
    }
    private void verificarLimiteDeCreditoFechar(TableMap jsonAbe01, String codEntidade, Long idEmpresa){

        BigDecimal vlrLimiteCredito = jsonAbe01.getTableMap("abe01json").getBigDecimal_Zero("vlr_lim_credito");
        LocalDate dataAtual = LocalDate.now();
        LocalDate dtVencLimCredito = jsonAbe01.getTableMap("abe01json").getDate("dt_vcto_lim_credito");
        def swing = new groovy.swing.SwingBuilder();
        def lblTexto;
        def txtArea;

        if(vlrLimiteCredito >= 0){
            if(dtVencLimCredito < dataAtual){ // Data de vencimento de crédito menor que data atual, significa expirou
                interromper("Data de vencimento do limite de crédito do cliente venceu em " + dtVencLimCredito.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")).toString() + ".")
            }

            BigDecimal vlrDocumentosReceber = somarDocsAReceber(codEntidade, idEmpresa);

            // Soma todos os documentos emitidos para a entidade com SCF = 2-Batch
            BigDecimal valorDocumentosEmitidos = buscarSomaDocumentosEmitidos(codEntidade, idEmpresa);

            // Calcula o valor total devedor
            BigDecimal valorTotalDevedor = vlrDocumentosReceber + valorDocumentosEmitidos;

            // Se o total devedor do cliente for maior que o limite de crédito, significa que houve inconsistências e precisa se analisada
            if(valorTotalDevedor > vlrLimiteCredito){
                if(exibirQuestao("Limite de crédito ultrapassado, necessario consultar o financeiro. Deseja continuar?")){
                    swing.edt {
                        dialog(title:"Observação de Aprovação", size:[500,250], defaultCloseOperation:javax.swing.JFrame.DISPOSE_ON_CLOSE, show:true, modal:true, locationRelativeTo:null) {
                            borderLayout()
                            lblTexto = label(text:"Limite de crédito ultrapassado.", constraints: java.awt.BorderLayout.NORTH)
                            scrollPane(){
                                txtArea = textArea(text:"", constraints:java.awt.BorderLayout.CENTER, rows:50, columns:65)
                            }
                            button(text:'Ok', actionPerformed: {strTexto = strTexto + "\n" + txtArea.text; dispose()}, constraints:java.awt.BorderLayout.SOUTH)
                        }
                    }
                }else{
                    interromper("Processo Interrompido");
                }
            }
        }
    }

    private BigDecimal somarDocsAReceber(String codEntidade, Long idEmpresa){
        Long idEntidade = buscarIdEntidade(codEntidade, idEmpresa);

        String sql = " SELECT SUM(daa01valor) AS valor" +
                " FROM daa01 " +
                " INNER JOIN abb01 ON abb01id = daa01central " +
                " WHERE abb01quita = 0 " +
                " AND daa01rp = 0 " +
                " AND abb01ent = " + idEntidade.toString() +
                " AND daa01gc = " + idEmpresa;

        TableMap tmValor = executarConsulta(sql)[0];

        return tmValor.getBigDecimal_Zero("valor");
    }

    private BigDecimal buscarSomaDocumentosEmitidos(String codEntidade, Long idEmpresa){
        Long idEntidade = buscarIdEntidade(codEntidade, idEmpresa);

        String sql = " SELECT SUM(eaa01totDoc) AS totalGeral " +
                " FROM eaa01 " +
                " INNER JOIN abb01 ON abb01id = eaa01central " +
                " INNER JOIN abd01 ON abd01id = eaa01pcd " +
                " LEFT JOIN abb10 ON abb10id = abd01opercod " +
                " WHERE abb10tipoCod = 1 " +
                " AND eaa01esMov = 1 " +
                " AND eaa01clasDoc = 1 " +
                " AND eaa01cancData IS NULL " +
                " AND eaa01iSCF = 2 " +
                " AND abb01ent = " + idEntidade.toString() +
                " AND eaa01gc = " + idEmpresa.toString();

        TableMap tmValorDocs = executarConsulta(sql)[0];

        return tmValorDocs.getBigDecimal_Zero("totalGeral")
    }
    private TableMap verificarDocumentoSalvo(){
        def txtAbb01num = getComponente("txtAbb01num");
        def nvgAah01codigo = getComponente("nvgAah01codigo");

        Integer numDoc = txtAbb01num.getValue();
        String tipoDoc = nvgAah01codigo.getValue();
        Long idEmpresa = obterEmpresaAtiva().getAac10id();

        return executarConsulta("SELECT eaa01id "+
                                " FROM eaa01 "+
                                "INNER JOIN abb01 on abb01id = eaa01central "+
                                "INNER JOIN abd01 on abd01id = eaa01pcd "+
                                "INNER JOIN aah01 ON aah01id = abb01tipo "+
                                "WHERE abd01es = 1 AND abd01aplic = 1 "+
                                "AND aah01codigo = '"+ tipoDoc + "' "+
                                "AND abb01num = "+numDoc+ " "+
                                "AND eaa01eg = "+idEmpresa);
    }
    private void btnImprimirPressed() {
        try {
            TableMap documento = verificarDocumentoSalvo();
            MNavigation nvgAah01codigo = getComponente("nvgAah01codigo");
            String codTipoDoc = nvgAah01codigo.getValue();

            if(documento.isEmpty()) interromper("Necessário salvar o documento antes de imprimir.");

            Long idDocumento = documento.getLong("eaa01id");


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

        if(tmTipoDoc.size() == 0) throw new ValidacaoException("Não foi encontrado relatório de impressão no tipo de documento " + codTipoDoc);

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
        MNavigation nvgAbd01codigo = getComponente("nvgAbd01codigo");
        Long idEmpresa = obterEmpresaAtiva().getAac10id();
        MTextArea txtEaa01obsUsoInt = getComponente("txtEaa01obsUsoInt");
        MNavigation nvgAbe01codigo = getComponente("nvgAbe01codigo");
        String codEntidade = nvgAbe01codigo.getValue();

        // Verifica Limite de Crédito da entidade
        TableMap tmAbe01 = buscarInformacoesLimiteCreditoEntidade(codEntidade, idEmpresa);

        if(tmAbe01.size() > 0 && tmAbe01.getTableMap("abe01json").getDate("dt_vcto_lim_credito") != null) verificarLimiteDeCreditoFechar(tmAbe01, codEntidade, idEmpresa);

        txtEaa01obsUsoInt.setValue(strTexto);
    }

    @Override
    public void posSalvar(Long id) {
    }
}