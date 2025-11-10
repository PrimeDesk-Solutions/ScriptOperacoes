package scripts.Silcon

import br.com.multitec.utils.collections.TableMap
import multitec.swing.components.autocomplete.MNavigation
import multitec.swing.core.MultitecRootPanel
import sam.model.entities.aa.Aab10
import sam.swing.tarefas.scf.SCF0150

import javax.swing.JButton;

public class SCF0150P2 extends sam.swing.ScriptBase{
    @Override
    public void execute(MultitecRootPanel tarefa) {
        Aab10 user = obterUsuarioLogado();
        JButton btnImportar = getComponente("btnImportar");

        btnImportar.addActionListener(e -> verificarContasUsuario(user))
    }
    private void verificarContasUsuario(Aab10 user){
        String usuario = user.getAab10user();
        MNavigation nvgDab01codigo = getComponente("nvgDab01codigo");
        String codConta = nvgDab01codigo.getValue();
        String Valquiria = "RECEBIMENTOS"; // "0004"
        String Gemil = "PAGAMENTOS"; // "0003"
        String Arlete = "FILIAL 01"; // "0005"
        String Cris = "MATRIZ 02"; // "0002"
        String Giovanna = "MATRIZ 01"; // "0001"
        String Gabriela = "FILIAL 02"; // "0021"
        String Andressa = "CAIXA RAPIDO"; // "0019"

        try{
            if(codConta != null){
                TableMap dadosConta = executarConsulta("SELECT dab01nome FROM dab01 WHERE dab01codigo = '" + codConta + "'")[0];

                if(usuario == "VAL" && Valquiria != dadosConta.getString("dab01nome")){
                    throw new RuntimeException("Script Operações: A conta "+ codConta +" utilizada não é deste usuario.")
                }

                if(usuario == "GEMIL" && Gemil != dadosConta.getString("dab01nome")){
                    throw new RuntimeException("Script Operações: A conta "+ codConta +" utilizada não é deste usuario.")
                }

                if(usuario == "ARLETE" && Arlete != dadosConta.getString("dab01nome")){
                    throw new RuntimeException("Script Operações: A conta "+ codConta +" utilizada não é deste usuario.")
                }

                if(usuario == "ARLETE1" && Arlete != dadosConta.getString("dab01nome")){
                    throw new RuntimeException("Script Operações: A conta "+ codConta +" utilizada não é deste usuario.")
                }

                if(usuario == "CRIS" && Cris != dadosConta.getString("dab01nome")){
                    throw new RuntimeException("Script Operações: A conta "+ codConta +" utilizada não é deste usuario.")
                }

                if(usuario == "GIOVANNA" && Giovanna != dadosConta.getString("dab01nome")){
                    throw new RuntimeException("Script Operações: A conta "+ codConta +" utilizada não é deste usuario.")
                }

                if(usuario == "GABRIELA" && Gabriela != dadosConta.getString("dab01nome")){
                    throw new RuntimeException("Script Operações: A conta "+ codConta +" utilizada não é deste usuario.")
                }

                if(usuario == "ANDRESSA" && Andressa != dadosConta.getString("dab01nome")){
                    throw new RuntimeException("Script Operações: A conta "+ codConta +" utilizada não é deste usuario.")
                }

                if(usuario != "VAL" && usuario != "VALL" && usuario != "GEMIL" && usuario != "ARLETE" && usuario != "ARLETE1" && usuario != "CRIS" && usuario != "GIOVANNA" && usuario != "MASTER" && usuario != "MASTER1" && usuario != "MASTER2" && usuario != "GABRIELA"  && usuario != "ANDRESSA"){
                    if((dadosConta.getString("dab01nome") == '0001')  || (dadosConta.getString("dab01nome") == '0002') || (dadosConta.getString("dab01nome") == '0003') || (dadosConta.getString("dab01nome") == '0004') || (dadosConta.getString("dab01nome") == '0005') || (dadosConta.getString("dab01nome") == '0021')){
                        throw new RuntimeException("Script Operações: Só é permitido utilizar conta referente a Banco")
                    }
                }
            }
        }catch(Exception e){
            interromper(e.getMessage())
        }
    }
}