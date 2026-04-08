import br.com.multitec.utils.ValidacaoException
import multitec.swing.components.autocomplete.MNavigation
import multitec.swing.core.MultitecRootPanel
import sam.model.entities.da.Dab01
import br.com.multitec.utils.collections.TableMap



import javax.swing.JButton;

public class Script extends sam.swing.ScriptBase{
    @Override
    public void execute(MultitecRootPanel tarefa) {
        JButton btnTransferir = getComponente("btnTransferir");
        btnTransferir.addActionListener(e -> validarContaCorrente());
    }
    private void validarContaCorrente(){
        try{
            MNavigation nvgDab01codigoEntrada = getComponente("nvgDab01codigoEntrada");
            Dab01 dab01 = nvgDab01codigoEntrada.getNavigationController().getValue();

            if(dab01 == null) throw new ValidacaoException("Necessário informar a conta corrente de entrada.");

            Boolean isOpen = verificarAberturaConta(dab01.dab01id);

            if(!isOpen) throw new ValidacaoException("Não há abertura de conta para a conta " + dab01.dab01codigo + ". Necessário realizar abertura de conta para a conta de entrada selecionada.");
        }catch (Exception e){
            interromper(e.getMessage());
        }
    }
    private Boolean verificarAberturaConta(Long idConta){
        try{
            String sql = "SELECT cca10id FROM cca10 WHERE cca10abertData IS NOT NULL AND cca10fechamdata IS NULL AND cca10cc = "  + idConta + " LIMIT 1";

            TableMap tmAbertura = executarConsulta(sql)[0];

            return tmAbertura != null && tmAbertura.getLong("cca10id") != null
        }catch(Exception e){
            throw new ValidacaoException("Erro ao buscar dados da conta de Entrada.")
        }
    }
}