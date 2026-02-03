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
    6. Cria botão de imprimir documento, ao selecionar o botão considerar:
        6.1 Tipo de documento 04 ou 16, abrir tela de impressão de documento (SRF1009);
        6.2 Demais tipos de documentos enviar diretamente para impressora
 */

package scripts

import com.fasterxml.jackson.core.type.TypeReference
import multitec.swing.components.autocomplete.MNavigation
import multitec.swing.components.textfields.MTextArea
import multitec.swing.core.MultitecRootPanel
import multitec.swing.core.utils.WindowUtils
import java.awt.event.FocusListener;
import javax.swing.SwingUtilities
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException
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
import multitec.swing.core.dialogs.ErrorDialog
import multitec.swing.core.dialogs.Messages
import multitec.swing.request.WorkerRunnable
import multitec.swing.request.WorkerSupplier
import sam.swing.ScriptBase
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import multitec.swing.core.MultitecRootPanel;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import multitec.swing.core.dialogs.Messages;
import multitec.swing.request.WorkerRequest;
import multitec.swing.request.WorkerRunnable;
import sam.swing.tarefas.srf.SRF1009;
import br.com.multitec.utils.UiSqlColumn;
import multitec.swing.components.textfields.MTextFieldInteger;
import multitec.swing.components.textfields.MTextFieldString;


public class SRF1002 extends sam.swing.ScriptBase{
    def strTexto = "";
    String obs = "";
    MultitecRootPanel tarefa;
    public Runnable windowLoadOriginal;

    @Override
    public void execute(MultitecRootPanel tarefa) {
        this.tarefa = tarefa;
        tarefa.getWindow().getJMenuBar().getMnuArquivo().getMniCancelar().addActionListener(mnu -> this.adicionaEventoESC(mnu));
        this.windowLoadOriginal = tarefa.windowLoad ;
        tarefa.windowLoad = {novoWindowLoad()};
        adicionaEventoPCD();
        adicionaBotaoImprimirDanfe();
    }
    protected void adicionaEventoESC(ActionEvent evt) {
        if(!exibirQuestao("Deseja realmente sair sem SALVAR?")) interromper("Por favor salvar antes de SAIR.");
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
    private void adicionaEventoPCD(){
        MNavigation nvgAbd01codigo = getComponente("nvgAbd01codigo");

        nvgAbd01codigo.addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent e) {};

            public void focusLost(FocusEvent e) {
                MNavigation nvgAbe01codigo = getComponente("nvgAbe01codigo");
                String codEntidade = nvgAbe01codigo.getValue();

                if(codEntidade != null) {
                    Long idEntidade = buscarIdEntidade(codEntidade);
                    String msgEnt = buscarMensagemEntidade(idEntidade);

                    // Exibe caixa de dialogo na tela com a mensagem do cadastro da entidade
                    if (msgEnt != null && msgEnt != "") exibirTelaDeAtencaoComMensagemEntidade(msgEnt);

                    // Busca os titulos vencidos da entidade
                    buscarTitulosVencidosEntidade(idEntidade);

                }
            }
        });
    }
    private String buscarMensagemEntidade(Long idEntidade){
        String sql = "SELECT abe02obsUsoInt FROM abe02 WHERE abe02ent = " + idEntidade.toString();

        TableMap tmEntidade =  executarConsulta(sql)[0]

        return tmEntidade.getString("abe02obsUsoInt");
    }

    private void exibirTelaDeAtencaoComMensagemEntidade(String msg){

        String caminhoImagem = "H:/Sam4/imagens/atencao.png";

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
        titulo.setFont(new Font("Arial", Font.BOLD, 25));
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
//        String imagemPath = caminhoImagem;
//        JLabel imagemLabel = new JLabel();
//        imagemLabel.setAlignmentX(Component.CENTER_ALIGNMENT); // centralizar horizontalmente
//        try {
//            BufferedImage img = ImageIO.read(new File(imagemPath));
//            // opcional: redimensionar mantendo proporção para largura fixa
//            int targetWidth = 220;
//            int w = img.getWidth();
//            int h = img.getHeight();
//            int targetHeight = (targetWidth * h) / w;
//            Image scaled = img.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
//            imagemLabel.setIcon(new ImageIcon(scaled));
//        } catch (IOException e) {
//            // se não encontrar, mostra texto substituto
//            imagemLabel.setText("Imagem não encontrada: " + imagemPath);
//            imagemLabel.setFont(new Font("Arial", Font.ITALIC, 12));
//        }
//        south.add(imagemLabel);
//        south.add(Box.createRigidArea(new Dimension(0, 8)));

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
    private Long buscarIdEntidade(String codEntidade){
        String sql = "SELECT abe01id FROM abe01 WHERE abe01codigo = '" + codEntidade + "' AND abe01gc = 1075797" //+ idEmpresa.toString();
        TableMap tmEntidade = executarConsulta(sql)[0];
        Long idEntidade = tmEntidade.getLong("abe01id");

        return idEntidade;
    }
    private void buscarTitulosVencidosEntidade(Long idEntidade){
        try{
            TableMap body = new TableMap()
            body.put("abe01id",idEntidade)
            WorkerRequest.create(tarefa.getWindow())
                    .initialText("Buscando Limite de Crédito")
                    .dialogVisible(false)
                    .controllerEndPoint("servlet")
                    .methodEndPoint("run")
                    .param("name", "Silcon.servlet.Buscar_Titulos_Vencidos_Entidade")
                    .header("ignore-body-decrypt", "true")
                    .parseBody(body)
                    .success((response) -> {
                        Boolean contemTituloVencido = response.parseResponse(new TypeReference<Boolean>(){});
                        if(contemTituloVencido && !exibirQuestao("Constam títulos vencidos para esse cliente, necessário consultar financeiro. Deseja continuar?")){
                            throw new ValidacaoException("Operação Cancelada.")
                        }else{
                            if(contemTituloVencido) exibirInformacao("Insira uma observação, caso necessário.");
                            verificarLimiteDeCredito(idEntidade)
                        }
                    })
                    .post();
        }catch(Exception err){
            throw new ValidacaoException(err.getMessage());
        }
    }
    private void verificarLimiteDeCredito(Long idEntidade){
        TableMap body = new TableMap()
        body.put("abe01id",idEntidade)

        WorkerRequest.create(tarefa.getWindow())
                .initialText("Verificando Limite de Crédito")
                .dialogVisible(false)
                .controllerEndPoint("servlet")
                .methodEndPoint("run")
                .param("name", "Silcon.servlet.Verificar_Limite_Credito_Entidade")
                .header("ignore-body-decrypt", "true")
                .parseBody(body)
                .success((response) -> {
                    Boolean limiteCreditoExcedido = response.parseResponse(new TypeReference<Boolean>(){});
                    if(limiteCreditoExcedido && !exibirQuestao("Limite de crédito ultrapassado, necessario consultar o financeiro. Deseja continuar?")){
                        throw new ValidacaoException("Operação Cancelada.")
                    }else{
                        if(limiteCreditoExcedido) exibirInformacao("Insira uma observação, caso necessário.")
                    }
                })
                .post();
    }
    private void adicionaBotaoImprimirDanfe(){
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
    private void btnImprimirPressed() {
        try {
            TableMap documento = verificarDocumentoSalvo();
            MNavigation nvgAah01codigo = getComponente("nvgAah01codigo");
            String codTipoDoc = nvgAah01codigo.getValue();

            if(documento.isEmpty()) interromper("Necessário salvar o documento antes de imprimir.");

            Long idDocumento = documento.getLong("eaa01id");

            if(!codTipoDoc.equals("04") && !codTipoDoc.equals("16")){
                WorkerSupplier.create(this.tarefa.getWindow(), {
                    return buscarDadosImpressao(idDocumento, codTipoDoc);
                })
                        .initialText("Imprimindo Documento")
                        .dialogVisible(true)
                        .success({ bytes ->
                            enviarDadosParaImpressao(bytes);
                        })
                        .start();
            }else{
                abrirTelaImpressaoDocs(codTipoDoc, idDocumento);
            }
        } catch (Exception err) {
            ErrorDialog.defaultCatch(this.tarefa.getWindow(), err);
        }
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
    private abrirTelaImpressaoDocs(String codTipoDoc, Long idDocumento){
        try{
            TableMap tmTipoDoc = buscarInformacoesTipoDoc(codTipoDoc);
            Long idTipoDoc = tmTipoDoc.getLong("aah01id");
            MTextFieldInteger txtAbb01num = getComponente("txtAbb01num");
            MTextFieldString txtEaa01nfeChave = getComponente("txtEaa01nfeChave");
            Integer numDoc = txtAbb01num.getValue();
            String protocolo = txtEaa01nfeChave.getValue();
            Boolean somenteComProtocolo = !(protocolo == null || protocolo.isEmpty());
            Integer statusImpressao = buscarStatusImpressao(idDocumento);
            Boolean rdoReimpressao = statusImpressao == 2;

            SRF1009 srf1009 = new SRF1009();
            WindowUtils.createJDialog(srf1009.getWindow(), srf1009);
            srf1009.cancelar = () -> srf1009.dispose();
            srf1009.chkDocumento.setValue(1);
            srf1009.ctrAah01.setIdValue(idTipoDoc);
            srf1009.txtNumInicialDoc.setValue(numDoc);
            srf1009.txtNumFinalDoc.setValue(numDoc);
            srf1009.chkSomenteDocumentoComProtocolo.setSelected(somenteComProtocolo);
            srf1009.rdoReimpressao.setSelected(rdoReimpressao);
            srf1009.btnMostrar.doClick()
            srf1009.getWindow().setVisible(true);
        }catch(Exception e) {
            throw new ValidacaoException("Falha ao abrir tarefa de imprimir documentos: " + e.getMessage());
        }
    }
    private byte[] buscarDadosImpressao(Long idDocumento, String codTipoDoc) {
        TableMap tmTipoDoc =  buscarInformacoesTipoDoc(codTipoDoc);
        String caminhoRelatorio = tmTipoDoc.getString("aah01formRelDoc");
        if(caminhoRelatorio == null || caminhoRelatorio.isEmpty()) throw new ValidacaoException("Não foi encontrado relatório de impressão no tipo de documento " + codTipoDoc);

        String json = "{\"nome\":\""+caminhoRelatorio+"\",\"filtros\":{\"eaa01id\":"+idDocumento+"}}"

        ObjectMapper mapper = new ObjectMapper();
        JsonNode obj = mapper.readTree(json);
        return HttpRequest.create().controllerEndPoint("relatorio").methodEndPoint("gerarRelatorio").parseBody(obj).post().getResponseBody()
    }
    private TableMap buscarInformacoesTipoDoc(String codTipoDoc){
        String sql = "SELECT aah01id, aah01formRelDoc FROM aah01 WHERE aah01codigo = '" + codTipoDoc + "'";

        TableMap tmTipoDoc = executarConsulta(sql)[0];

        if(tmTipoDoc.size() == 0) throw new ValidacaoException("Não foi encontrado registros para o tipo de documento " + codTipoDoc);

        return tmTipoDoc;
    }
    private buscarStatusImpressao(Long idDocumento){
        String sql = "SELECT eaa01statusImpr FROM eaa01 WHERE eaa01id = " + idDocumento;
        TableMap tmDoc = executarConsulta(sql)[0];
        Integer statusImpressao = tmDoc.getInteger("eaa01statusImpr");

        return statusImpressao;
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