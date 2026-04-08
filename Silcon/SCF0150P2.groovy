    package scripts.Silcon

    import br.com.multitec.utils.collections.TableMap
    import multitec.swing.components.MRadioButton
    import multitec.swing.components.autocomplete.MNavigation
    import multitec.swing.core.MultitecRootPanel
    import sam.model.entities.aa.Aab10
    import sam.model.entities.aa.Aab1001
    import sam.swing.tarefas.scf.SCF0150

    import javax.swing.JButton
    import java.awt.event.ActionEvent
    import java.awt.event.ActionListener
    import java.awt.event.FocusEvent
    import java.awt.event.FocusListener;

    public class SCF0150P2 extends sam.swing.ScriptBase{
        @Override
        public void execute(MultitecRootPanel tarefa) {
            definirValoresDefault();
            //adicionarEventoBtnImportar();
        }
        private void definirValoresDefault(){
            MRadioButton rdoReceber = getComponente("rdoReceber");

            rdoReceber.setSelected(true);

        }
        private void adicionarEventoBtnImportar(){
            JButton btnImportar = getComponente("btnImportar");

            btnImportar.addActionListener(e -> verificarContaCorrente())
        }
        private void verificarContaCorrente(){
            Aab10 user = obterUsuarioLogado();
            MNavigation nvgDab01codigo = getComponente("nvgDab01codigo");

            TableMap tmAab1001 = executarConsulta("SELECT aab1001conteudo FROM aab1001 WHERE aab1001aplic = 'CC' AND aab1001param = 'CONTACORRENTE' AND aab1001user = " + user.aab10id)[0];
            if(tmAab1001 == null || tmAab1001.size() == 0 || tmAab1001.getString("aab1001conteudo") == null) return
            if(nvgDab01codigo.getValue() == null) interromper("Script: Necessário informar a conta corrente.");

            String codConta = tmAab1001.getString("aab1001conteudo");

            if(nvgDab01codigo.getValue() != codConta) interromper("A conta " + nvgDab01codigo.getValue() + " não é do usuário " + user.aab10nome)

        }
    }