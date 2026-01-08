/*
    FUNÇÃO:
    - Valida se já existe um item com o mesmo nome abreviado ao salvar ou editar

 */
import br.com.multitec.utils.ValidacaoException
import multitec.swing.core.MultitecRootPanel;
import br.com.multitec.utils.collections.TableMap;
import sam.model.entities.ab.Abm01;
import sam.swing.tarefas.cgs.CGS5001

import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.util.function.Consumer


public class Script extends sam.swing.ScriptBase{
    public Consumer exibirRegistroPadrao;
    MultitecRootPanel tarefa
    @Override
    public void execute(MultitecRootPanel tarefa) {
        this.tarefa = tarefa;
        def txtAbm01na = getComponente("txtAbm01na");
    }
    @Override
    public void preSalvar(boolean salvo) {
        try{
            Abm01 abm01 = (Abm01)  ((CGS5001) tarefa).registro
            def txtAbm01na = getComponente("txtAbm01na");
            String naItem = txtAbm01na.getValue();

            if(abm01.abm01id == null){
                verificarItemIncluindo(naItem);
            } else{
                verificarItemEditando(naItem);
            }
        }catch(Exception e){
            interromper(e.getMessage())
        }
    }
    private verificarItemEditando(String naItem){
        try{
            Abm01 abm01 = (Abm01)  ((CGS5001) tarefa).registro;
            Long idEmpresa = obterEmpresaAtiva().getAac10id();
            Long idGc = buscarGCPelaEmpresaAtiva(idEmpresa);
            String sql = "SELECT abm01id FROM abm01 WHERE abm01na = '" + naItem + "' AND abm01gc = " + idGc + " AND abm01id <> " + abm01.abm01id + " LIMIT 1";
            TableMap tmItem = executarConsulta(sql);

            if(tmItem.size() > 0) throw new ValidacaoException("Sript: Item com o nome abreviado " + naItem + " já está cadastrado no sistema.");
        }catch(Exception e){
            throw new ValidacaoException(e.getMessage())
        }

    }
    private void verificarItemIncluindo(String naItem){
        try{
            Long idEmpresa = obterEmpresaAtiva().getAac10id();
            Long idGc = buscarGCPelaEmpresaAtiva(idEmpresa);
            String sql = "SELECT abm01id FROM abm01 WHERE abm01na = '" + naItem + "' AND abm01gc = " + idGc + " LIMIT 1";
            TableMap tmItem = executarConsulta(sql);

            if(tmItem.size() > 0) throw new ValidacaoException("Sript: Item com o nome abreviado " + naItem + " já está cadastrado no sistema.");
        }catch(Exception e){
            throw new ValidacaoException("Falha ao verificar registro " + e.getMessage())
        }
    }
    private Long buscarGCPelaEmpresaAtiva(Long idEmpresa){
        try{
            String sql = "SELECT aac1001gc FROM aac1001 WHERE aac1001empresa = " + idEmpresa +" AND aac1001tabela = 'Abm01'";

            TableMap tm = executarConsulta(sql)[0];

            if(tm.size() == 0) interromper("Script: Não foi encontrado Grupo Centralizador para a tabela Abm01 na empresa ativa.")

            return tm.getLong("aac1001gc");
        }catch(Exception e){
            throw new ValidacaoException("Falha ao buscar grupo centralizador a partir da empresa ativa.")
        }
    }

    @Override
    public void posSalvar(Long id) {
    }
}