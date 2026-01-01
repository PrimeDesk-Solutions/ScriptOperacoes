package scripts

import br.com.multitec.utils.ValidacaoException
import br.com.multitec.utils.collections.TableMap
import br.com.multitec.utils.http.HttpRequest
import multitec.swing.components.MCheckBox
import multitec.swing.components.MRadioButton
import multitec.swing.components.autocomplete.MNavigation
import multitec.swing.components.spread.MSpread
import multitec.swing.components.spread.columns.MSpreadColumnCheckBox
import multitec.swing.components.spread.columns.MSpreadColumnLocalDate
import multitec.swing.components.textfields.MTextFieldInteger
import multitec.swing.components.textfields.MTextFieldLocalDate
import multitec.swing.core.MultitecRootPanel
import multitec.swing.core.utils.WindowUtils
import org.apache.poi.ss.usermodel.Table
import org.w3c.dom.events.Event
import sam.dto.scf.SCF0221LctoDto
import sam.model.entities.da.Daa01
import sam.swing.ScriptBase
import sam.swing.VariaveisDaSessao
import sam.swing.tarefas.cgs.CGS0150
import sam.swing.tarefas.scf.SCF0102

import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.table.TableColumn
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.time.format.DateTimeFormatter

class CGS0112 extends ScriptBase {
    MultitecRootPanel tarefa;
    Boolean isAcessarTarefaCGS0112 = isAcessarTarefa("CGS0112");

    @Override
    void execute(MultitecRootPanel panel) {
        this.tarefa = panel;
        criarBotaoAbrirTarefa();

        JButton btnMostrar = getComponente("btnMostrar");
        MSpread sprDocs = getComponente("sprDocs");

//        for(ActionListener event : btnMostrar.getActionListeners()){
//            btnMostrar.removeActionListener(event);
//        }
//
//        btnMostrar.addActionListener(e -> preencherSpread(e))
        adicionaEventoCheckMarcar();
        adicionarEventoBtnGravar();

    }
    private void adicionaEventoCheckMarcar(){
        MSpread sprDocs = getComponente("sprDocs");

        TableMap tmCamposCustomUser = buscarCamposCustomUser();
        MSpreadColumnCheckBox colunaMarcar = (MSpreadColumnCheckBox)sprDocs.getColumnByName("marcar");

        colunaMarcar.getCellEditorComponent().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    Integer linha = sprDocs.getSelectedRow();
                    TableMap tmLinhaSpread  = sprDocs.getSpreadModel().getItens().get(linha);
                    Boolean marcar = ((MCheckBox)e.getSource());
                    if(tmCamposCustomUser != null || tmCamposCustomUser.size() > 0){
                        //if(marcar) verificarLinhaSelecionada(tmLinhaSpread);
                        marcarDocumentosIguais(tmLinhaSpread, marcar, linha); // Aprova automaticamente as demais linhas do mesmo documento que foram criadas devido a politica de segurança
                    }
                } catch (Exception err) {
                    if(((MCheckBox)e.getSource()).isSelected()) ((MCheckBox)e.getSource()).setValue(0)
                    interromper(err.getMessage())
                }
            }
        });
    }
    private void marcarDocumentosIguais(TableMap tmLinhaSpread, Boolean marcar, Integer linha){
        MSpread sprDocs = getComponente("sprDocs");
        Long idCentralSelecionado = tmLinhaSpread.getLong("abb01id");
        TableMap tmCamposCustomUser = buscarCamposCustomUser();
        BigDecimal vlrLimiteCompraUser = tmCamposCustomUser.getBigDecimal_Zero("vlrLimCompra");
        BigDecimal vlrDoc = tmLinhaSpread.getBigDecimal_Zero("abb01valor");

        List<TableMap> documentos = sprDocs.getValue();

        Integer count = 0;
        for(documento in documentos){
            Long idCentral = documento.getLong("abb01id")
            
            if(tmCamposCustomUser.getInteger("userMaster") == 0){ // Usuário não é master, aplica validações
                if(idCentral == idCentralSelecionado && documento.getInteger("marcar") == 1 && vlrDoc > vlrLimiteCompraUser && marcar && count != linha) throw new RuntimeException("Documentos acima de R\$ " + String.format("%.2f", vlrLimiteCompraUser) + " podem ser aprovados uma unica vez por usuário." )

                if(idCentral == idCentralSelecionado && vlrDoc <= vlrLimiteCompraUser ){
                    if(documento.getInteger("marcar") == 0){
                        documento.put("marcar", 1);
                    }else{
                        documento.put("marcar", 0);
                    }
                }
            }else{
                if(idCentral == idCentralSelecionado){
                    if(documento.getInteger("marcar") == 0){
                        documento.put("marcar", 1);
                    }else{
                        documento.put("marcar", 0);
                    }
                }
            }

            count++
        }

        sprDocs.refreshAll()
    }
    private TableMap buscarCamposCustomUser(){
        Long idUser = obterUsuarioLogado().getAab10id();

        return executarConsulta("SELECT cast(aab10camposcustom ->> 'valor_lim_compra' as numeric(18,6)) as vlrLimCompra, CAST(aab10camposCustom -> 'user_master' AS INTEGER) AS userMaster from aab10 where aab10id = " + idUser)[0];

    }
    private void adicionarEventoBtnGravar(){
        JButton btnGravar = getComponente("btnGravar");
        btnGravar.addActionListener(e -> validarDocumentosAoGravar() )
    }
    private void validarDocumentosAoGravar(){
        MRadioButton rdoAprovar = getComponente("rdoAprovar");
        if(!rdoAprovar.isSelected()) return

        try{
            MSpread sprDocs = getComponente("sprDocs");
            List<TableMap> documentos = sprDocs.getValue();
            Map<String, Integer> hashCountItens = new HashMap<>();
            Map<String, BigDecimal> hashDocValor = new HashMap<>();

            TableMap tmCamposCustomUser = buscarCamposCustomUser();
            BigDecimal limiteUsuario = tmCamposCustomUser.getBigDecimal_Zero("vlrLimCompra");

            if(tmCamposCustomUser.getInteger("userMaster") == 0){ // Usuário não é master, aplica validações
                for(documento in documentos){
                    Long idCentral = documento.getLong("abb01id");
                    Integer numDoc = documento.getInteger("abb01num");
                    String codTipoDoc = documento.getString("aah01codigo");
                    String parcela = documento.getString("abb01parcela");
                    BigDecimal valor = documento.getBigDecimal_Zero("abb01valor");
                    Integer marcar = documento.getInteger("marcar");
                    String key = codTipoDoc + " " + parcela + " " + numDoc.toString();

                    if(marcar == 1){
                        // Verifica se o documento selecionado já foi aprovado pelo usuário
                        def tmAprovacoes = executarConsulta("SELECT abb0103user "+
                                "FROM abb01 "+
                                "INNER JOIN abb0103 ON abb0103central = abb01id "+
                                "WHERE abb01num = " + numDoc + " " +
                                "AND abb01parcela = '" + parcela + "' "+
                                "AND abb0103user = " + obterUsuarioLogado().getAab10id())

                        if(tmAprovacoes.size() > 0) throw new RuntimeException("O documento " + numDoc + " parcela " + parcela + " já foi aprovado pelo usuário " + obterUsuarioLogado().getAab10user() + ".")

                        if(hashCountItens.containsKey(key)){
                            hashCountItens.put(key, hashCountItens.get(key) + 1);
                            hashDocValor.put(key, valor);
                        }else{
                            hashCountItens.put(key, 1)
                        }
                    }
                }

                // Verifica se foi marcado o mesmo documento mais de uma vez
                for(String doc : hashCountItens.keySet()){
                    if(hashCountItens.get(doc) >= 2 && limiteUsuario < hashDocValor.get(doc)) throw new RuntimeException("Não é permitido aprovar o mesmo documento mais de uma vez. Documento: " + doc.substring(2))
                }
            }
        }catch(Exception e){
            interromper(e.getMessage())
        }
    }

    private void criarBotaoAbrirTarefa() {
        JPanel pnlDocumentos = getComponente("pnlDocumentos");
        JButton btnAbrirTarefa = new JButton();
        btnAbrirTarefa.setText("Abrir Doc. Selecionado");
        btnAbrirTarefa.addActionListener(e -> btnAbrirPressed(e))
        btnAbrirTarefa.setBounds(1029, 20, 150, 32);

        pnlDocumentos.add(btnAbrirTarefa);

    }

    private void btnAbrirPressed(ActionEvent e) {
        MSpread sprDocs = getComponente("sprDocs");
        Integer row = sprDocs.getSelectedRow();
        String codTipoDoc = ((TableMap) sprDocs.get(row)).getString("aah01codigo");
        Integer numDoc = ((TableMap) sprDocs.get(row)).getInteger("abb01num");
        String serie = ((TableMap) sprDocs.get(row)).getString("abb01serie");
        String parcela = ((TableMap) sprDocs.get(row)).getString("abb01parcela");

        if (sprDocs.getValue().size() == 0) interromper("Spread de documentos vazia, importe os documentos.");
        if (row < 0) interromper("Nenhuma linha selecionada.");
        if (!isAcessarTarefaCGS0112) interromper("O usuário logado não tem permissão para acessar essa tarefa.")

        TableMap tmDocFinanc = buscarDocumentoFinanceiro(codTipoDoc, numDoc, serie, parcela);
        if(tmDocFinanc.size() == 0) interromper("Falha ao buscar registro.");
        Long idDocFinanc = tmDocFinanc.getLong("daa01id");

        SCF0102 scf0102 = new SCF0102();
        WindowUtils.createJDialog(scf0102.getWindow(), scf0102);
        scf0102.cancelar = () -> scf0102.getWindow().dispose();
        scf0102.exibirPanelListaCadastro = () -> scf0102.getWindow().dispose();
        scf0102.editar(idDocFinanc);
        scf0102.getWindow().setVisible(true);
    }
    protected boolean isAcessarTarefa(String tarefa) {
        return (Boolean) HttpRequest.create().controllerEndPoint("cas0103").methodEndPoint("verificarSeUsuarioTemAcessoTarefa").param("aab10id", VariaveisDaSessao.getInstance().getAab10().getIdValue()).param("tarefa", tarefa).get().parseResponse(Boolean.class);
    }
    private TableMap buscarDocumentoFinanceiro(String codTipoDoc, Integer numDoc, String serie, String parcela) {
        String whereNumDoc = " WHERE abb01num = " + numDoc.toString();
        String whereTipoDoc = " AND aah01codigo = '" + codTipoDoc + "' ";
        String whereSerie = serie != null ? " AND abb01serie = '" + serie + "' " : "";
        String whereParcela = " AND abb01parcela = '" + parcela + "'";

        String sql = " SELECT daa01id " +
                    " FROM daa01 " +
                    " INNER JOIN abb01 ON abb01id = daa01central "+
                    " INNER JOIN aah01 ON aah01id = abb01tipo "+
                    whereNumDoc +
                    whereTipoDoc +
                    whereSerie +
                    whereParcela +
                    "LIMIT 1"

        return executarConsulta(sql)[0];
    }
    private void preencherSpread(ActionEvent e) {
        List<TableMap> listDocs = buscarDocumentos();
        MSpread sprDocs = getComponente("sprDocs");

        if (listDocs == null || listDocs.size() == 0) interromper("Não há registros para exibir com os filtros informados.");

        try {
            sprDocs.setValue(listDocs);
        } catch (Error erro) {
            interromper(erro.toString())
        }

    }
    private List<TableMap> buscarDocumentos() {
        MRadioButton rdoAprovar = getComponente("rdoAprovar");
        MRadioButton rdoDesaprovar = getComponente("rdoDesaprovar");
        MNavigation nvgAah01codigo = getComponente("nvgAah01codigo");
        MNavigation nvgAbe01codigo = getComponente("nvgAbe01codigo");
        MCheckBox chkNumero = getComponente("chkNumero");
        MTextFieldInteger txtNumInicial = getComponente("txtNumInicial");
        MTextFieldInteger txtNumFinal = getComponente("txtNumFinal");
        MCheckBox chkPeriodo = getComponente("chkPeriodo");
        MTextFieldLocalDate txtDataInicial = getComponente("txtDataInicial");
        MTextFieldLocalDate txtDataFinal = getComponente("txtDataFinal");

        def chkAprovar = rdoAprovar.isSelected();
        def chkDesaprovar = rdoDesaprovar.isSelected();
        def codTipoDoc = nvgAah01codigo.getValue();
        def codEntidade = nvgAbe01codigo.getValue();
        def chkNumeroValue = chkNumero.getValue();
        def numInicial = txtNumInicial.getValue();
        def numFinal = txtNumFinal.getValue();
        def chkPeriodoValue = chkPeriodo.getValue();
        def dtInicial = txtDataInicial.getValue()
        def dtFinal = txtDataFinal.getValue();

        String whereAprovados = chkAprovar ? "WHERE abb0103data IS NULL " : "WHERE abb0103data IS NOT NULL";
        String whereTipoDoc = codTipoDoc != null ? " AND aah01codigo = '" + codTipoDoc + "'" : "";
        String whereEntidade = codEntidade != null ? " AND abe01codigo = '" + codEntidade + "'" : "";

        String whereNumero = ""
        if (chkNumeroValue == 1) {
            whereNumero = numInicial != null && numFinal != null ? " AND abb01num BETWEEN " + numInicial.toString() + " AND " + numFinal.toString() :
                    numInicial != null && numFinal == null ? " AND abb01num >= " + numInicial.toString() :
                            numInicial == null && numFinal != null ? " AND abb01num <= " + numFinal.toString() : ""
        }

        String whereData = "";
        if (chkPeriodoValue == 1) {
            whereData = dtInicial != null && dtFinal != null ? " AND daa01dtVctoR BETWEEN '" + dtInicial.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")).toString() + "' AND '" + dtFinal.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")).toString() + "' " :
                    dtInicial != null && dtFinal == null ? " AND daa01dtVctoR >= '" + dtInicial.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")).toString() + "' " :
                            dtInicial == null && dtFinal != null ? " AND daa01dtVctoR <= '" + dtFinal.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")).toString() + "' " : ""
        }

        String campoUser = chkDesaprovar ? ", abb0103user " : "";

        String sql = "SELECT daa01dtVctoR AS abb01data, abb01ent, abb01tipo, abe01na, abb10descr, aab03nome AS nomeDasPoliticas,  " +
                "abb01parcela, abb0103central, abb01valor, abb10codigo, abb01id, aah01codigo, abb01num, aah01nome, abb0103id, " +
                "abb01opercod, abe01codigo, 0 AS marcar, abb01serie, abb0103ps, daa01dtvctor " + campoUser +
                "FROM daa01 " +
                "INNER JOIN abb01 ON abb01id = daa01central " +
                "LEFT JOIN aah01 ON aah01id = abb01tipo " +
                "INNER JOIN abe01 ON abe01id = abb01ent " +
                "INNER JOIN abb10 ON abb10id = abb01operCod " +
                "INNER JOIN abb0103 ON abb0103central = abb01id " +
                "LEFT JOIN aab10 ON aab10id = abb0103user " +
                "LEFT JOIN aab03 ON aab03id::text = abb0103ps " +
                whereAprovados +
                whereTipoDoc +
                whereEntidade +
                whereNumero +
                whereData;

        List<TableMap> listDocs = executarConsulta(sql);

        return listDocs;
    }
}