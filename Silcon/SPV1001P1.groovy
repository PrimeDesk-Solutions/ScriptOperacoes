/*
    SPV1001P1 - INICIAR PRÉ-VENDA
    FUNÇÃO:

    1. Exibe observação de uso interno (Abe02obsUsoInt) da entidade (se existir).
    2. Verifica títulos vencidos:
       - Se houver, pergunta se deseja continuar.
    3. Validação do limite de crédito:
       - Verifica data limite (se vencida, interrompe).
       - Calcula saldo devedor:
            - DAA01 (Docs a Receber): abb01quita = 0 AND daa01rp = 0
            - EAA01 (Financeiro 2-Batch): abb10tipoCod = 1 AND eaa01esMov = 1 AND eaa01clasDoc = 1 AND eaa01cancData IS NULL AND eaa01iSCF = 2
            - Saldo devedor = soma(Doc. a Receber + Doc. Batch)
       - Se saldo > limite, pergunta se deseja continuar
 */
package scripts

import br.com.multitec.utils.collections.TableMap
import multitec.swing.components.autocomplete.MNavigation
import multitec.swing.core.MultitecRootPanel

import javax.swing.JButton;

import br.com.multitec.utils.collections.TableMap
import multitec.swing.components.autocomplete.MNavigation
import multitec.swing.components.textfields.MTextArea
import multitec.swing.core.MultitecRootPanel

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

public class Script extends sam.swing.ScriptBase{

    @Override
    public void execute(MultitecRootPanel tarefa) {
        JButton btnIniciarVenda = getComponente("btnIniciarVenda");
        //JButton btnAtivarVendedor = getComponente("btnAtivarVendedor");

        btnIniciarVenda.addActionListener(e -> adicionarEventoBtnIniciar())
        //btnAtivarVendedor.addActionListener(e -> buscarTitulosVencidosEntidade())
    }

    private void adicionarEventoBtnIniciar(){
        try{
            MNavigation nvgAbe01codigo = getComponente("nvgAbe01codigo");
            String codEntidade = nvgAbe01codigo.getValue();
            Long idEmpresa = obterEmpresaAtiva().getAac10id();
            String msgEnt = buscarMensagemEntidade(codEntidade, idEmpresa);
            MTextArea txtCcb01obs = getComponente("txtCcb01obs");
            MNavigation nvgAbe30codigo = getComponente("nvgAbe30codigo");

            // Busca os titulos vencidos da entidade
            buscarTitulosVencidosEntidade(codEntidade, idEmpresa);

            // Exibe caixa de dialogo na tela com a mensagem do cadastro da entidade
            if (msgEnt != null) exibirTelaDeAtencaoComMensagemEntidade(msgEnt);

            if(nvgAbe30codigo.getValue() != "000"){ // Não valida limite de crédito para condição á vista
                // Verifica Limite de Crédito da entidade
                TableMap tmAbe01 = buscarInformacoesLimiteCreditoEntidade(codEntidade, idEmpresa);

                if(tmAbe01.size() > 0 && tmAbe01.getTableMap("abe01json").getDate("dt_vcto_lim_credito") != null) verificarLimiteDeCredito(tmAbe01, codEntidade, idEmpresa);
            }

            //txtCcb01obs.setValue("strTexto")
        }catch(Exception e){
            interromper(e.getMessage())
        }
    }

    private void buscarTitulosVencidosEntidade(codEntidade, idEmpresa){
        try{
            Long idEntidade = buscarIdEntidade(codEntidade, idEmpresa);

            String sql = "SELECT SUM(daa01valor) AS totDoc " +
                    "FROM daa01 " +
                    "INNER JOIN abb01 ON abb01id = daa01central " +
                    "WHERE daa01rp = 0 " +
                    "AND abb01quita = 0 " +
                    "AND daa01dtVctoR < current_date " +
                    "AND abb01ent = " + idEntidade;
            TableMap totalTituloVencido = executarConsulta(sql)[0];

            if(totalTituloVencido.getBigDecimal_Zero("totDoc") > 0 ){
                if(!exibirQuestao("Constam títulos vencidos para esse cliente, necessário consultar financeiro. Deseja continuar?")) interromper("Operação Cancelada.")
            }
        }catch(Exception e){
            interromper(e.getMessage());
        }
    }

    private String buscarMensagemEntidade(String codEntidade, Long idEmpresa){
        Long idEntidade = buscarIdEntidade(codEntidade, idEmpresa);
        String sql = "SELECT abe02obsUsoInt FROM abe02 WHERE abe02ent = " + idEntidade.toString();

        TableMap tmEntidade =  executarConsulta(sql)[0]

        return tmEntidade.getString("abe02obsUsoInt");
    }

    private void exibirTelaDeAtencaoComMensagemEntidade(String msg){

        String caminhoImagem = "C:/Users/leona/Desktop/atencao.png";

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

    private void verificarLimiteDeCredito(TableMap jsonAbe01, String codEntidade, Long idEmpresa){
        BigDecimal vlrLimiteCredito = jsonAbe01.getTableMap("abe01json").getBigDecimal_Zero("vlr_lim_credito");
        LocalDate dataAtual = LocalDate.now();
        LocalDate dtVencLimCredito = jsonAbe01.getTableMap("abe01json").getDate("dt_vcto_lim_credito");

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
                    exibirInformacao("Pré-venda liberada com limite de crédito ultrapassado. Informe uma observação caso necessário.")
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

    private Long buscarIdEntidade(String codEntidade, Long idEmpresa){
        String sql = "SELECT abe01id FROM abe01 WHERE abe01codigo = '" + codEntidade + "' AND abe01gc = 1075797";
        TableMap tmEntidade = executarConsulta(sql)[0];
        if(tmEntidade == null) interromper("Entidade não encontrada na empresa ativa.")
        Long idEntidade = tmEntidade.getLong("abe01id");

        return idEntidade;
    }
}