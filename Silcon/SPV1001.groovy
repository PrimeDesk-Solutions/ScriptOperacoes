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
    7- Altera a exibição da lista de itens no F4
 */
package scripts

import multitec.swing.components.textfields.MTextArea
import multitec.swing.components.textfields.MTextFieldString
import org.springframework.asm.TypeReference

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
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.print.PrinterJob;
import br.com.multitec.utils.UiSqlColumn;
import br.com.multitec.utils.collections.TableMap
import javax.swing.*;
import multitec.swing.components.autocomplete.MNavigation
import multitec.swing.components.textfields.MTextArea
import javax.swing.border.EmptyBorder;
import java.awt.image.BufferedImage;
import java.time.LocalDate
import java.util.List
import java.util.ArrayList
import java.awt.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import multitec.swing.request.WorkerRequest;
import multitec.swing.request.WorkerRunnable;
import com.fasterxml.jackson.core.type.TypeReference;

public class Script extends sam.swing.ScriptBase{
    MultitecRootPanel panel;
    MCheckBox chkNota = new MCheckBox();
    public Runnable  windowLoadOriginal;
    private ActionListener[] actionEventOriginal;
    @Override
    public void execute(MultitecRootPanel tarefa) {
        this.panel = tarefa;
        criarMenu("Impressão Etiqueta", "Imprimir", e -> imprimirEtiqueta(), null);
        this.windowLoadOriginal = tarefa.windowLoad ;
        tarefa.windowLoad = {novoWindowLoad()};
        adicionarEventoSpreadPreVenda()
        adicionarEventoBotaoConcluir();
        adicionarEventoBtnIniciar();
        adicionarCheckComNota();
    }
    protected void novoWindowLoad(){
        this.windowLoadOriginal.run();

        def ctrAbm01 = getComponente("ctrAbm01");
        ctrAbm01.f4Columns = () -> {
            List<UiSqlColumn> uiSqlColumn = new ArrayList<>();
            UiSqlColumn abm01codigo = new UiSqlColumn("abm01codigo", "abm01codigo", "Código", 20);
            UiSqlColumn abm01gtin = new UiSqlColumn("abm01gtin", "abm01gtin", "GTIN", 20);
            UiSqlColumn abm01descr = new UiSqlColumn("abm01descr", "abm01descr", "Descrição", 120);
            UiSqlColumn abm01complem = new UiSqlColumn("abm01complem", "abm01complem", "Fabricante", 255);
            uiSqlColumn.addAll(Arrays.asList(abm01codigo,abm01descr, abm01complem, abm01gtin));
            return uiSqlColumn;
        };

    }
    private adicionarEventoSpreadPreVenda(){
        MSpread sprCcb01s = getComponente("sprCcb01s");

        sprCcb01s.addMouseListener(new MouseAdapter() {
            @Override
            void mouseClicked(MouseEvent e) {
                if(sprCcb01s.getSelectedRow() >= 0){
                    chkNota.setEnabled(true);
                    chkNota.setValue(0);
                }
            }
        });

        sprCcb01s.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            void valueChanged(ListSelectionEvent e) {
                if(sprCcb01s.getValue().size() > 0){
                    chkNota.setEnabled(true);
                }else{
                    chkNota.setValue(0);
                    chkNota.setEnabled(false);
                }

            }
        })
    }
    private void adicionarEventoBotaoConcluir(){
        try{
            JButton btnConcluir = getComponente("btnConcluir");
            actionEventOriginal = btnConcluir.getActionListeners(); // Armazena os eventos default

            for(evento in actionEventOriginal){
                btnConcluir.removeActionListener(evento); // Remove os evento default do botão
            }

            btnConcluir.addActionListener(new ActionListener() {
                @Override
                void actionPerformed(ActionEvent e) {
                    verificarUnitariosQuantidadesItem();
                    verificarCampoComprador();
                    verificarContatosConsumidorFinal();
                    verificarEntidade(e);
                }
            } )
        }catch(Exception err){
            interromper(err.getMessage())
        }
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
    private verificarEntidade(ActionEvent e){
        try{
            //String msgEnt = buscarMensagemEntidade(codEntidade, idEmpresa);
            MSpread sprCcb01s = getComponente("sprCcb01s");
            Integer row = sprCcb01s.getSelectedRow();

            if(row == -1) return;

            Ccb01 ccb01 = sprCcb01s.get(row);
            Long idEntidade = ccb01.ccb01ent.abe01id;
            String nomeEntidade = ccb01.ccb01ent.abe01nome;

            if(nomeEntidade != null && nomeEntidade.toUpperCase() != "CONSUMIDOR"){
                String msgEnt = buscarMensagemEntidade(idEntidade);

                // Exibe caixa de dialogo na tela com a mensagem do cadastro da entidade
                if (msgEnt != null && msgEnt != "") exibirTelaDeAtencaoComMensagemEntidade(msgEnt);

                // Busca os titulos vencidos da entidade
                buscarTitulosVencidosEntidade(idEntidade, e);
            }else{
                if(actionEventOriginal != null && actionEventOriginal.size() > 0){
                    for(evento in actionEventOriginal){
                        evento.actionPerformed(e) // Executa os eventos originais novamente
                    }
                }
            }
        }catch(Exception err){
            interromper(err.getMessage());
        }
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
    private void buscarTitulosVencidosEntidade(Long idEntidade, ActionEvent e){
        try{
            TableMap body = new TableMap()
            body.put("abe01id",idEntidade)
            WorkerRequest.create(panel.getWindow())
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
                            verificarLimiteDeCredito(idEntidade, e);
                        }
                    })
                    .post();

        }catch(Exception err){
            throw new ValidacaoException(err.getMessage());
        }
    }
    private void verificarLimiteDeCredito(Long idEntidade, ActionEvent e){
        TableMap body = new TableMap()
        body.put("abe01id",idEntidade)
        WorkerRequest.create(panel.getWindow())
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
                    }else {
                        if(actionEventOriginal != null && actionEventOriginal.size() > 0){
                            for(evento in actionEventOriginal){
                                evento.actionPerformed(e) // Executa os eventos originais novamente
                            }
                        }
                    }
                })
                .post();
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
    private adicionarEventoBtnIniciar(){
        JButton btnIniciar = getComponente("btnIniciar");
        btnIniciar.addActionListener(e -> btnIniciarClicked());
    }
    private void btnIniciarClicked(){
        chkNota.setValue(0);
    }
    private void adicionarCheckComNota(){
        JPanel pnlItens = getComponente("pnlItens");
        chkNota.setText("Com Nota");
        chkNota.setBounds(new Rectangle(new Point(1250, 0), chkNota.getPreferredSize()));
        pnlItens.add(chkNota);
        chkNota.setValue(0);
        chkNota.setEnabled(false);

        adicionarEventoChkComNota();
    }
    private adicionarEventoChkComNota(){
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
                        enviarDadosParaImpressao(bytes);
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
    protected void enviarDadosParaImpressao(byte[] bytes) {
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
}