/*
    1. Replica a entidade da aba origem para a aba destino
    2. Deixa fixo os dados da baixa do documento origem
 */
import br.com.multitec.utils.ValidacaoException
import br.com.multitec.utils.collections.TableMap
import multitec.swing.core.MultitecRootPanel;
import multitec.swing.components.autocomplete.MNavigation
import multitec.swing.components.textfields.MTextFieldLocalDate;
import java.awt.event.FocusEvent
import java.awt.event.FocusListener;
import javax.swing.JTabbedPane
import java.time.LocalDate;
import multitec.swing.components.MCheckBox;

public class Script extends sam.swing.ScriptBase{
    @Override
    public void execute(MultitecRootPanel tarefa) {
        try{
            MNavigation nvgAbe01codigoIni = getComponente("nvgAbe01codigoIni");
            MNavigation nvgAbe30codigo = getComponente("nvgAbe30codigo");
            MNavigation nvgAbe01codigo = getComponente("nvgAbe01codigo");
            Long idEmpresa = obterEmpresaAtiva().getAac10id();
            JTabbedPane tabbedPane1 = getComponente("tabbedPane1");
            MNavigation nvgAbf20codigoBaixa = getComponente("nvgAbf20codigoBaixa");
            MNavigation nvgAbf15codigoBaixa = getComponente("nvgAbf15codigoBaixa");
            MNavigation nvgAbf16codigoBaixa = getComponente("nvgAbf16codigoBaixa");
            MTextFieldLocalDate txtDataBaixa = getComponente("txtDataBaixa");
            MCheckBox chkDaa01aceite = getComponente("chkDaa01aceite");

            tabbedPane1.addChangeListener(e ->{ // Trocando de aba
                int index = tabbedPane1.getSelectedIndex();
                String titulo = tabbedPane1.getTitleAt(index);

                if(titulo.toUpperCase().equals("DESTINO")){
                    String codEntidadeOrigem = nvgAbe01codigoIni.getValue();
                    if(codEntidadeOrigem == null) return;
                    Long idEntidade = buscarIdEntidade(codEntidadeOrigem, idEmpresa);
                    Long idPLF = buscarIdPLF('004', idEmpresa);
                    Long idPortador = buscarIdPortador("0001", idEmpresa);
                    Long idOperacao = buscarIdOperacao("01", idEmpresa);

                    nvgAbe01codigo.getNavigationController().setIdValue(idEntidade);
                    nvgAbf20codigoBaixa.getNavigationController().setIdValue(idPLF);
                    nvgAbf15codigoBaixa.getNavigationController().setIdValue(idPortador);
                    nvgAbf16codigoBaixa.getNavigationController().setIdValue(idOperacao);
                    txtDataBaixa.setValue(LocalDate.now());
                    chkDaa01aceite.setValue(1);
                }
            });
        } catch (Exception ex){
            interromper(ex.getMessage())
        }
    }
    private Long buscarIdEntidade(String codEntidadeOrigem, Long idEmpresa){
        try{
            Long idGc = buscarGCPelaEmpresaAtiva(idEmpresa, "Abe01");

            String sql = "SELECT abe01id FROM abe01 WHERE abe01codigo = '" + codEntidadeOrigem + "' AND abe01gc = " + idGc;

            TableMap tmEntidade = executarConsulta(sql)[0];

            if(tmEntidade == null || tmEntidade.size() == 0) throw new ValidacaoException("Não foi encontrada entidade com o código " + codEntidadeOrigem + " na empresa ativa")

            return tmEntidade.getLong("abe01id");
        } catch(Exception ex){
           throw new ValidacaoException("Falha ao buscar entidade: " + ex.getMessage());
        }
    }
    private Long buscarIdPLF(String codPLF, Long idEmpresa){
        try{
            Long idGc = buscarGCPelaEmpresaAtiva(idEmpresa, "Abf20");
            String sql = "SELECT abf20id FROM abf20 WHERE abf20codigo = '" + codPLF + "' AND abf20gc = " + idGc;
            TableMap tmPLF = executarConsulta(sql)[0];

            if(tmPLF == null || tmPLF.size() == 0) throw new ValidacaoException("Não foi encontrado PLF com o código " + codPLF + " na empresa ativa.");

            return tmPLF.getLong("abf20id");
        } catch (Exception ex){
            throw new ValidacaoException(ex.getMessage());
        }
    }
    private Long buscarIdPortador(String codPortador, Long idEmpresa){
        try{
            Long idGc = buscarGCPelaEmpresaAtiva(idEmpresa, "Abf15");
            String sql = "SELECT abf15id FROM abf15 WHERE abf15codigo = '" + codPortador + "' AND abf15gc = " + idGc;

            TableMap tmPortador = executarConsulta(sql)[0];
            if(tmPortador == null || tmPortador.size() == 0) throw new ValidacaoException("Não foi encontrado portador com o código " + codPortador + " na empresa ativa.");

            return tmPortador.getLong("abf15id");

        } catch(Exception ex){
            throw new ValidacaoException(ex.getMessage());
        }
    }
    private Long buscarIdOperacao(String codOper, Long idEmpresa){
        try{
            Long idGc = buscarGCPelaEmpresaAtiva(idEmpresa, "Abf16");
            String sql = "SELECT abf16id FROM abf16 WHERE abf16codigo = '" + codOper + "' AND abf16gc = " + idGc;

            TableMap tmOper = executarConsulta(sql)[0];
            if(tmOper == null || tmOper.size() == 0) throw new ValidacaoException("Não foi encontrado operação com o código " + codOper + " na empresa ativa.");

            return tmOper.getLong("abf16id");

        } catch(Exception ex){
            throw new ValidacaoException(ex.getMessage());
        }
    }
    private Long buscarGCPelaEmpresaAtiva(Long idEmpresa, String tabela){
        try{
            String sql = "SELECT aac1001gc FROM aac1001 WHERE aac1001empresa = " + idEmpresa +" AND aac1001tabela = '" + tabela + "'";

            TableMap tm = executarConsulta(sql)[0];

            if(tm.size() == 0) throw new ValidacaoException("Script: Não foi encontrado Grupo Centralizador para a tabela " + tabela +" na empresa ativa.")

            return tm.getLong("aac1001gc");
        }catch(Exception e){
            throw new ValidacaoException("Falha ao buscar grupo centralizador a partir da empresa ativa." + e.getMessage())
        }
    }

}