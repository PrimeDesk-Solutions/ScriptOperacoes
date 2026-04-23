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
        btnTransferir.addActionListener(e -> validarContasCorrente());
    }
    private void validarContasCorrente(){
        MNavigation nvgDab01codigoSaida = getComponente("nvgDab01codigoSaida");
        MNavigation nvgDab01codigoEntrada = getComponente("nvgDab01codigoEntrada");

        validarContaCorrente(nvgDab01codigoSaida);
        validarContaCorrente(nvgDab01codigoEntrada);
    }
    private void validarContaCorrente(MNavigation nvgConta){
        try{
            Dab01 dab01 = nvgConta.getNavigationController().getValue();

            if(dab01 == null) throw new ValidacaoException("Necessário informar a conta corrente de entrada ou saída.");

            Integer requerAbertura = buscarCampoCustomCC(dab01.dab01id);
            if(requerAbertura == 0) return;

            Boolean isOpen = verificarAberturaConta(dab01.dab01id);

            if(!isOpen) throw new ValidacaoException("Não há abertura de conta para a conta " + dab01.dab01codigo + ". Necessário realizar abertura de conta antes de prosseguir.");
        }catch (Exception e){
            interromper(e.getMessage());
        }
    }
    private Integer buscarCampoCustomCC(Long idConta){
        String sql = "SELECT dab01camposCustom AS custom FROM dab01 WHERE dab01id = " + idConta;

        TableMap tmCamposCustom = executarConsulta(sql)[0] == null ? new TableMap() : executarConsulta(sql)[0];

        return tmCamposCustom.getTableMap("custom").getInteger("requer_abertura");
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