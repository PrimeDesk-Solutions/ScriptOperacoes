import multitec.swing.components.spread.MSpread
import multitec.swing.components.textfields.MTextFieldString
import multitec.swing.core.MultitecRootPanel
import sam.model.entities.cc.Ccb01
import javax.swing.JButton
import javax.swing.border.TitledBorder
import java.awt.event.ActionEvent
import java.awt.event.ActionListener;
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
import br.com.multitec.utils.http.HttpRequest
import multitec.swing.components.spread.MSpread
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import multitec.swing.core.MultitecRootPanel;
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.Path;
import java.awt.Desktop



public class Script extends sam.swing.ScriptBase{
    MultitecRootPanel panel;
    MTextFieldString txtCaminho = new MTextFieldString();
    @Override
    public void execute(MultitecRootPanel tarefa) {
        this.panel = tarefa;
        criarComponentesIniciais();
        definirCamposDefault();
    }
    private void definirCamposDefault(){
        Long idUser = obterUsuarioLogado().getAab10id();
        TableMap camposCustomUser = buscarCampoCustomizadosUsuario(idUser);

        if(camposCustomUser != null && camposCustomUser.size() > 0){
            String caminhoArquivo = camposCustomUser.getTableMap("aab10camposcustom").getString("caminho_pedidos");

            if(caminhoArquivo != null && !caminhoArquivo.isEmpty()) txtCaminho.setValue(caminhoArquivo);

        }
    }
    private void criarComponentesIniciais(){
        criarLabelCaminho();
        adicionarBotaoSalvarDocumento();
    }
    private void criarLabelCaminho(){
        JPanel panelCaminho = new JPanel();
        panelCaminho.setBorder(new TitledBorder("Caminho Arquivo"));
        panelCaminho.setLayout(null);
        panelCaminho.setBounds(1035, 8, 258, 45);

        txtCaminho.setBounds(10, 15, 245, 25);
        panelCaminho.add(txtCaminho);

        panel.add(panelCaminho)
    }
    private void adicionarBotaoSalvarDocumento(){
        JButton btnImprimir = new JButton();
        btnImprimir.setText("Salvar Documento");
        btnImprimir.setBounds(900, 8, 130, 40);
        btnImprimir.addActionListener(new ActionListener() {
            @Override
            void actionPerformed(ActionEvent e) {
                try{
                    salvarPDF();
                } catch(Exception ex){
                    interromper(ex.getMessage())
                }
            }
        })

        panel.add(btnImprimir)

    }
    private void salvarPDF() {
        try {
            TableMap jsonCcb01 = buscarVendaSelecionada();
            if (jsonCcb01 == null || jsonCcb01.size() == 0) return;

            Long idVenda = jsonCcb01.getLong("ccb01id");
            Integer numVenda = jsonCcb01.getInteger("ccb01num");
            String nomeEntidade = jsonCcb01.getString("abe01nome");

            byte[] pdfBytes = buscarDadosPDF(idVenda);
            String caminhoArquivo = txtCaminho.getValue();

            if (caminhoArquivo == null || caminhoArquivo.isEmpty())
                throw new ValidacaoException("Necessário preencher o caminho aonde será salvo o documento.");

            Path diretorio = Paths.get(caminhoArquivo);
            if (!Files.exists(diretorio) || !Files.isDirectory(diretorio))
                throw new ValidacaoException("O caminho informado não é um diretório válido.");

            String nomeArquivo = "Orçamento-" + numVenda + "-" + nomeEntidade + ".pdf";
            Path caminhoPdf = diretorio.resolve(nomeArquivo);

            Files.write(caminhoPdf, pdfBytes);

            exibirInformacao("Documento salvo com sucesso!")

            //abrirPastaArquivo(caminhoPdf.toFile());

        } catch (Exception e) {
           throw new ValidacaoException(e.getMessage())
        }
    }
    private TableMap buscarCampoCustomizadosUsuario(Long idUser){
        String sql = "SELECT aab10camposCustom FROM aab10 WHERE aab10id = " + idUser;

        return executarConsulta(sql)[0];
    }
    private TableMap buscarVendaSelecionada(){
        MSpread sprVendas = getComponente("sprVendas");

        if(sprVendas.getRowCount() == 0) interromper("Não há vendas a serem impressas.");
        if(sprVendas.getSelectedRow() < 0) interromper("Nenhuma venda selecionada para impressão.");

        Integer row = sprVendas.getSelectedRow();

        return sprVendas.get(row);
    }

    private byte[] buscarDadosPDF(Long idVenda) {
        try{
            String json = "{\"nome\":\"Silcon.relatorios.spv.SPV_Impressao_Pre_Venda\",\"filtros\":{\"ccb01id\":"+idVenda+"}}";

            ObjectMapper mapper = new ObjectMapper();
            JsonNode obj = mapper.readTree(json);
            def result = HttpRequest.create().controllerEndPoint("relatorio").methodEndPoint("gerarRelatorio").parseBody(obj).post().getResponseBody();

            return result;
        }catch (Exception e){
            throw new ValidacaoException(e.getMessage())
        }

    }
    private static void abrirPastaArquivo(File pdfFile) {
        try {
            Desktop.getDesktop().open(pdfFile.getParentFile());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}