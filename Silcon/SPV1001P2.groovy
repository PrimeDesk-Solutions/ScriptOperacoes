/*
    SPV1001P2 - CONCLUIR PRÉ-VENDA
    FUNÇÃO:

    1. Verifica títulos vencidos:
       - Se houver, pergunta se deseja continuar.
    2. Validação do limite de crédito:
       - Não valida limite de crédito se a condição de pagamento for Á Vista
       - Verifica data limite (se vencida, interrompe).
       - Calcula saldo devedor:
            - DAA01 (Docs a Receber): abb01quita = 0 AND daa01rp = 0
            - EAA01 (Financeiro 2-Batch): abb10tipoCod = 1 AND eaa01esMov = 1 AND eaa01clasDoc = 1 AND eaa01cancData IS NULL AND eaa01iSCF = 2
            - Saldo devedor = soma(Doc. a Receber + Doc. Batch)
       - Se saldo > limite, pergunta se deseja continuar
     3. Altera o número de vias para 1 quando NFCE e inativa o campo de número de vias
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
import java.time.format.DateTimeFormatter
import java.util.function.Consumer;
import multitec.swing.components.MRadioButton;

public class Script extends sam.swing.ScriptBase{
    public Consumer exibirRegistroPadrao;
    @Override
    public void execute(MultitecRootPanel tarefa) {
        JButton btnConcluirVenda = getComponente("btnConcluirVenda");
        btnConcluirVenda.addActionListener(e -> adicionarEventoBtnConcluir());
        adicionarEventoChkNFCe();
    }
    private void removerCondicaoDePagamento(){
        MNavigation nvgAbe01na = getComponente("nvgAbe01na");
        String nomeEntidade = nvgAbe01na.getValue();
        MNavigation nvgAbe30codigo = getComponente("nvgAbe30codigo");


        if(nomeEntidade.toUpperCase().contains("CONSUMIDOR")) nvgAbe30codigo.getNavigationController().setIdValue(null)
    }
    private void adicionarEventoChkNFCe(){
        MRadioButton rdoNFCe65 = getComponente("rdoNFCe65");

        rdoNFCe65.addFocusListener(new FocusListener() {
            @Override
            void focusGained(FocusEvent e) {
                alterarNumeroDeVias(1, false)
            }

            @Override
            void focusLost(FocusEvent e) {
               alterarNumeroDeVias(2, true)
            }
        })

        //rdoNFCe65.addActionListener(e -> alterarNumeroDeVias(1, false));
    }
    private void alterarNumeroDeVias(Integer qtd, Boolean ativar){
        try{
            def txtVias = getComponente("txtVias");
            txtVias.setValue(qtd);

            txtVias.setEnabled(ativar);
        }catch (Exception e){
            interromper(e.getMessage())
        }
    }
    private void adicionarEventoBtnConcluir(){
        try{
            MNavigation nvgAbe01codigo = getComponente("nvgAbe01codigo");
            String codEntidade = nvgAbe01codigo.getValue();
            Long idEmpresa = obterEmpresaAtiva().getAac10id();
            MNavigation nvgAbe30codigo = getComponente("nvgAbe30codigo");

            buscarTitulosVencidosEntidade(codEntidade, idEmpresa);
            //removerCondicaoDePagamento();

            // Verifica Limite de Crédito da entidade
            TableMap tmAbe01 = buscarInformacoesLimiteCreditoEntidade(codEntidade, idEmpresa);

            if(nvgAbe30codigo.getValue() != "000"){ // Não valida limite de crédito para condição á vista
                if(tmAbe01.size() > 0 && tmAbe01.getTableMap("abe01json").getDate("dt_vcto_lim_credito") != null) verificarLimiteDeCredito(tmAbe01, codEntidade, idEmpresa);
            }
        }catch(Exception e){
            interromper(e.getMessage());
        }
    }
    private void buscarTitulosVencidosEntidade(String codEntidade, Long idEmpresa){
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
            if(!exibirQuestao("Constam títulos vencidos, necessário consultar financeiro. Deseja continuar?")) interromper("Operação Cancelada.")
        }
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
                    // prossegue a venda
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

        return tmValorDocs.getBigDecimal_Zero("totalGeral");
    }
    private Long buscarIdEntidade(String codEntidade, Long idEmpresa){
        String sql = "SELECT abe01id FROM abe01 WHERE abe01codigo = '" + codEntidade + "' AND abe01gc = 1075797 ";
        TableMap tmEntidade = executarConsulta(sql)[0];
        Long idEntidade = tmEntidade.getLong("abe01id");

        return idEntidade;
    }
}