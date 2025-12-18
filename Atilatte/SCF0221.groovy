package scripts

import br.com.multitec.utils.ValidacaoException
import br.com.multitec.utils.collections.TableMap
import multitec.swing.components.MCheckBox
import multitec.swing.components.MRadioButton
import multitec.swing.components.autocomplete.MNavigation
import multitec.swing.components.spread.MSpread
import multitec.swing.components.spread.columns.MSpreadColumnCheckBox
import multitec.swing.components.textfields.MTextFieldFile
import multitec.swing.core.MultitecRootPanel
import org.apache.poi.ss.usermodel.Table
import sam.dto.scf.SCF0221DtoConciliar
import sam.dto.scf.SCF0221LctoDto
import sam.dto.scf.SCF0221LctoExtratoDto
import multitec.swing.components.textfields.MTextFieldLocalDate;

import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JPanel
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.sql.ResultSet
import java.sql.SQLException
import java.time.LocalDate;
import multitec.swing.core.dialogs.Messages;
import multitec.swing.request.WorkerRequest;
import multitec.swing.request.WorkerRunnable;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import sam.dto.scf.SCF0221DtoConciliar;


public class SCF0221 extends sam.swing.ScriptBase{
    private MultitecRootPanel tarefa;
    private conciliado = false;
    Connection connection = null;
    JButton btnBuscarDocRepositorio = new JButton();
    JButton btnGravarDocsRepositorio = new JButton();

    @Override
    public void execute(MultitecRootPanel panel) {
        this.tarefa = panel;
        JButton btnConciliarExtrato = getComponente("btnConciliarExtrato");
        JButton btnImportarArquivo = getComponente("btnImportarArquivo");

        btnConciliarExtrato.addActionListener(e -> conciliado = true);
        btnImportarArquivo.addActionListener(e -> conciliado = false);

        realizarConexaoBD();
        adicionarBotaoBuscarDocRepositorio();
        adicionarBotaoGravarDocRepositorio();
        //verificarCheckExtratoSpreadLancamentos();
        adicionarEventoChkConciliarLancamento();
        renomearCampos();

    }
    private void renomearCampos(){
        JButton btnImportarArquivo = getComponente("btnImportarArquivo");
        JButton btnConciliarExtrato = getComponente("btnConciliarExtrato");

        btnImportarArquivo.setText("1 - Importar");
        btnConciliarExtrato.setText("2 - Conciliar");
        btnBuscarDocRepositorio.setText("3 - Buscar Docs Repositório");
        btnGravarDocsRepositorio.setText("4 - Gravar Docs Repositório");
    }
    private void adicionarBotaoBuscarDocRepositorio(){
        JPanel pnlExtrato = getComponente("pnlExtrato");
        btnBuscarDocRepositorio.setBounds(600, 5, 150, 40);
        btnBuscarDocRepositorio.setText("Buscar Docs Repositório");
        btnBuscarDocRepositorio.addActionListener(e -> btnBuscarDocRepositorioSelected(e));
        pnlExtrato.add(btnBuscarDocRepositorio);
    }
    private void adicionarBotaoGravarDocRepositorio(){
        JPanel pnlExtrato = getComponente("pnlExtrato");
        btnGravarDocsRepositorio.setBounds(770, 5, 150, 40);
        btnGravarDocsRepositorio.setText("Gravar Docs Repositório");
        btnGravarDocsRepositorio.addActionListener(e -> btnGravarDocRepositorioSelected(e));
        pnlExtrato.add(btnGravarDocsRepositorio);
    }
    private void btnBuscarDocRepositorioSelected(ActionEvent e){
        try{
            MSpread sprLctosExtrato = getComponente("sprLctosExtrato");

            realizarValidacoesIniciais();

            Long idRepositorio = buscarIdRepositorio();

            List<TableMap> listDocs = executarConsulta("SELECT aba2001id, aba2001json FROM aba2001 WHERE aba2001rd = " + idRepositorio);

            if(listDocs.size() > 0){
                for(documento in listDocs){
                    SCF0221LctoExtratoDto extratoDto = new SCF0221LctoExtratoDto();
                    LocalDate data = documento.getTableMap("aba2001json").getDate("data")
                    BigDecimal valor = documento.getTableMap("aba2001json").getBigDecimal_Zero("valor");
                    String dc = documento.getTableMap("aba2001json").getString("debito_credito");
                    String historico = documento.getTableMap("aba2001json").getString("historico");
                    String dados1 = documento.getTableMap("aba2001json").getString("dados1");
                    String dados2 = documento.getTableMap("aba2001json").getString("id_pgto");


                    extratoDto.data = data;
                    extratoDto.valor = valor;
                    extratoDto.dc = dc
                    extratoDto.historico = historico
                    extratoDto.ni = null;
                    extratoDto.dados1 = null;
                    extratoDto.dados2 = dados2;

                    sprLctosExtrato.addRow(extratoDto)
                }
                exibirInformacao("Registros importados com sucesso!");

                executarSalvarOuExcluir("DELETE FROM aba2001 WHERE aba2001rd = " + idRepositorio);
            }else {
                exibirAtencao("Nenhum documento encontrado no repositório de dados para exibição.");
            }
        }catch(Exception err){
            interromper("Falha ao tentar buscar dados do repositório: " + err.getMessage());
        }
    }
    private void btnGravarDocRepositorioSelected(ActionEvent e) {
        if(!conciliado) interromper("Antes de gravar os documentos, clique em conciliar para separar os documentos que serão gravados.");

        try {
            realizarConexaoBD();
            realizarValidacoesIniciais();

            MSpread sprLctosExtrato = getComponente("sprLctosExtrato");
            List<SCF0221LctoExtratoDto> listExtratoDto = sprLctosExtrato.getValue();

            if (listExtratoDto.isEmpty()) throw new ValidacaoException("Não há registros para serem gravados no repositório.");

            Long aba2001rd = buscarIdRepositorio();
            Long nextSequence = buscarProximaSequenciaRepositorio(aba2001rd);
            Integer docsGravados = gravarDocumentosRepositorio(listExtratoDto, aba2001rd, nextSequence);

            if(docsGravados > 0){
                connection.commit();
                exibirInformacao("Registros gravados com sucesso!");

                sprLctosExtrato.clear();
                sprLctosExtrato.refreshAll()
            }else{
                exibirAtencao("Nenhum registro gravado no repositório de dados.")
            }

            conciliado = false;

        }catch (SQLException err){
            rollback();
            interromper("Falha ao tentar gravar registros: " + err.getMessage());
        }catch (Exception ex) {
            interromper("Validação ao tentar gravar registros: " + ex.getMessage());
        } finally {
            fecharConexao();
        }
    }
    private void realizarValidacoesIniciais(){
        // --- Validações iniciais ---
        MNavigation nvgDab01codigo = getComponente("nvgDab01codigo");
        MRadioButton rdoNaoConciliados = getComponente("rdoNaoConciliados");
        String codConta = nvgDab01codigo.getValue();

        if (codConta == null) throw new ValidacaoException("Necessário informar a conta corrente.");
        if (!rdoNaoConciliados.isSelected()) throw new ValidacaoException("Somente é permitido a gravação de registros não conciliados no repositório de dados.");
    }
    private void realizarConexaoBD(){
        String url = "jdbc:postgresql://192.168.1.12:5432/atibainha_sam4";
//        String url = "jdbc:postgresql://localhost:5432/atilatte_reforma";
        String user = "postgres";
        String password = "postgres";
        connection = DriverManager.getConnection(url, user, password);
        connection.setAutoCommit(false);
    }
    private Long buscarIdRepositorio(){
        Long idEmpresa = obterEmpresaAtiva().getAac10id();

        TableMap repositorio = executarConsulta("SELECT aba20id, aba20codigo FROM aba20 WHERE aba20codigo = '998' AND aba20gc = " + idEmpresa)[0];

        if (repositorio.length == 0) throw new ValidacaoException("Não foi encontrado repositório de dados com o código 998 para gravar os documentos.");

        return repositorio.getLong("aba20id");
    }
    private Long buscarProximaSequenciaRepositorio(Long aba2001rd ){
        String sqlMaxSeq = "SELECT COALESCE(MAX(aba2001lcto), 0) FROM aba2001 WHERE aba2001rd = ?";
        PreparedStatement psMax = connection.prepareStatement(sqlMaxSeq);
        psMax.setLong(1, aba2001rd);

        ResultSet rs = psMax.executeQuery();
        rs.next();
        Long ultimaSeq = rs.getLong(1);
        psMax.close();
        rs.close();

        return ultimaSeq;
    }
    private List<String> buscarIdsExtratosASeremGravados(List<SCF0221LctoExtratoDto> listExtratoDto){
        List<String> idsLcto = new ArrayList<>();
        for(SCF0221LctoExtratoDto extratoDto in listExtratoDto){
            idsLcto.add("'" + extratoDto.dados2 + "'");
        }

        return idsLcto;
    }
    private Integer gravarDocumentosRepositorio(List<SCF0221LctoExtratoDto> listExtratoDto, Long aba2001rd, Long nextSequence){

        List<String> idsLcto = buscarIdsExtratosASeremGravados(listExtratoDto); // Lista de todos os registros que serão gravados no repositório;

        // Prepara o insert dos documentos no repositório
        String sqlInsert = "INSERT INTO aba2001 (aba2001id, aba2001rd, aba2001lcto, aba2001json) " +
                            "VALUES (nextval('default_sequence'), ?, ?, ?::jsonb)";

        PreparedStatement psInsert = connection.prepareStatement(sqlInsert);

        Integer docsGravados = 0;
        List<TableMap> idsDocJaGravado = buscarIdsDocRepositorio(idsLcto); // Lista todos os documentos já gravados no repositório

        for (SCF0221LctoExtratoDto extratoDto : listExtratoDto) {
            String dados2 = extratoDto.dados2;

            if(idsDocJaGravado != null && idsDocJaGravado.size() > 0){
                def encontrou = idsDocJaGravado.stream().filter(tm -> tm.getString("idLcto") == dados2).findFirst(); // Verifica se o documento já está salvo no repositório

                if (encontrou) continue
            }

            docsGravados++;
            nextSequence++; // incrementa manualmente

            psInsert.setLong(1, aba2001rd);
            psInsert.setLong(2, nextSequence);
            psInsert.setString(3, montarJsonExtrato(extratoDto));
            psInsert.addBatch(); // adiciona ao lote

        }

        if(docsGravados > 0) psInsert.executeBatch()

        return docsGravados;
    }
    private List<TableMap> buscarIdsDocRepositorio(List<String> idsLcto){
        Long idRepositorio = buscarIdRepositorio();
        String filtroIds = idsLcto.toString().replace("[", "");
        filtroIds = filtroIds.replace("]", '');

        String sql = "SELECT CAST(aba2001json ->> 'id_pgto' AS TEXT) AS idLcto FROM aba2001 WHERE CAST(aba2001json ->> 'id_pgto' AS text) IN (" + filtroIds + ") AND aba2001rd = " + idRepositorio;

        List<TableMap> tmDocGravado = executarConsulta(sql);

        return tmDocGravado;
    }
    private String montarJsonExtrato(SCF0221LctoExtratoDto extratoDto) {
        return String.format(
                "{\"data\":\"%s\",\"valor\":%s,\"debito_credito\":\"%s\",\"historico\":\"%s\",\"id_pgto\":\"%s\"}",
                extratoDto.data.toString().replace("-", ""),
                extratoDto.valor,
                extratoDto.dc,
                extratoDto.historico.replace("\"", "\\\""),
                extratoDto.dados2
        );
    }
    private void verificarCheckExtratoSpreadLancamentos(){
        MSpread sprLctos = getComponente("sprLctos");
        MSpreadColumnCheckBox colunaExtrato = (MSpreadColumnCheckBox)sprLctos.getColumnByName("extrato");
        colunaExtrato.getCellEditorComponent().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    Integer linha = sprLctos.getSelectedRow();
                    Boolean extrato = ((MCheckBox)e.getSource()).isSelected();
                    Long dab1002id = ((SCF0221LctoDto)sprLctos.getSpreadModel().getItens().get(linha)).getDab1002id();
                    gravarDocumentoRepositorio(dab1002id);
                } catch (Exception err) {
                    interromper(err.getMessage())
                }
            }
        });
    }
    private void adicionarEventoChkConciliarLancamento(){
        MRadioButton rdoNaoConciliados = getComponente("rdoNaoConciliados");
        MSpread sprLctos = getComponente("sprLctos");
        MSpread sprLctosExtrato = getComponente("sprLctosExtrato");
        MTextFieldLocalDate txtDataConc = getComponente("txtDataConc");
        MSpreadColumnCheckBox colunaConciliado = (MSpreadColumnCheckBox)sprLctos.getColumnByName("conc");
        colunaConciliado.getCellEditorComponent().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    if(!rdoNaoConciliados.isSelected()) return;
                    Integer linha = sprLctos.getSelectedRow();
                    SCF0221LctoDto scf0221LctoDto = (SCF0221LctoDto)sprLctos.getSpreadModel().getItens().get(linha);
                    Boolean chkExtrato = scf0221LctoDto.extrato;
                    Long dab1002id = ((SCF0221LctoDto)sprLctos.getSpreadModel().getItens().get(linha)).getDab1002id();
                    Boolean conciliado = ((MCheckBox)e.getSource()).isSelected();
                    LocalDate dtAtual = LocalDate.now();

                    if(chkExtrato){
                        SCF0221LctoExtratoDto extratoDtoLcto = buscarSCF0221LctoExtratoDtoPeloLancamento(dab1002id);
                        if(extratoDtoLcto != null){
                            LocalDate dtExtrato = extratoDtoLcto.data;
                            txtDataConc.setValue(dtExtrato);
                        }
                    }else{
                        txtDataConc.setValue(dtAtual);
                    }

                    if(sprLctosExtrato.getValue().size() > 0 && chkExtrato){
                        for(int i = 0; i < sprLctosExtrato.getValue().size(); i++ ){
                            SCF0221LctoExtratoDto extratoDto = sprLctosExtrato.getSpreadModel().getItens().get(i);
                            Long dab1002Extrato = extratoDto.dab1002id;
                            if(dab1002Extrato == dab1002id) sprLctosExtrato.removeRow(i); // Remove a linha da spread de extrato que foi conciliada ao clicar no check conciliar
                        }
                        sprLctosExtrato.refreshAll();
                    }
                } catch (Exception err) {
                    interromper(err.getMessage())
                }
            }
        });
    }
    private SCF0221LctoExtratoDto buscarSCF0221LctoExtratoDtoPeloLancamento(Long dab1002id){
        MSpread sprLctosExtrato = getComponente("sprLctosExtrato");
        Optional<SCF0221LctoExtratoDto> optional1 = ((List<SCF0221LctoDto>)sprLctosExtrato.getValue()).stream().filter(extratoDto -> extratoDto.dab1002id == dab1002id).findFirst();

        if(optional1.isPresent()){ // Documento encontrado na spread de extrato
            try{
                 return optional1.get();
            }catch(SQLException e){
                rollback();
                interromper("Erro: " + e.getMessage());
            }
        }
    }
    private void gravarDocumentoRepositorio(Long dab1002id){
        SCF0221LctoExtratoDto extratoDto = buscarSCF0221LctoExtratoDtoPeloLancamento(dab1002id);
        gravarDocumentoRepositorio(extratoDto);
    }
    private void gravarDocumentoRepositorio(SCF0221LctoExtratoDto extratoDto){

        if (connection == null || connection.isClosed()) realizarConexaoBD();

        Long idRepositorio = buscarIdRepositorio();
        Long nextSequence = buscarProximaSequenciaRepositorio(idRepositorio);
        
        // Prepara o insert dos documentos no repositório
        String sqlInsert = "INSERT INTO aba2001 (aba2001id, aba2001rd, aba2001lcto, aba2001json) " +
                "VALUES (nextval('default_sequence'), ?, ?, ?::jsonb)";

        PreparedStatement psInsert = connection.prepareStatement(sqlInsert);

        nextSequence++; // incrementa manualmente

        psInsert.setLong(1, aba2001rd);
        psInsert.setLong(2, nextSequence);
        psInsert.setString(3, montarJsonExtrato(extratoDto));
        psInsert.addBatch(); // adiciona ao lote

        psInsert.executeBatch();

        connection.commit();
    }
    private void rollback() {
        try {
            if (connection != null) connection.rollback();
        } catch (SQLException ex) {
            interromper(ex.getMessage())
        }
    }
    private void fecharConexao() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}