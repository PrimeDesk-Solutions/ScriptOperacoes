/*
    1. Busca a lista de feriado na API BrasilAPI
    2. Preenche o repositorio de dados de feriado
 */

import br.com.multitec.utils.ValidacaoException
import br.com.multitec.utils.collections.TableMap
import com.fasterxml.jackson.core.type.TypeReference
import multitec.swing.components.spread.MSpread
import multitec.swing.components.textfields.MTextFieldString
import multitec.swing.core.MultitecRootPanel
import multitec.swing.request.WorkerRequest
import sam.model.entities.ab.Aba2001
import java.time.LocalDate


import javax.swing.JButton
import javax.swing.JPanel;

public class Script extends sam.swing.ScriptBase{
    MultitecRootPanel tarefa;
    @Override
    public void execute(MultitecRootPanel tarefa) {
        this.tarefa = tarefa;
        MTextFieldString txtAba20codigo = getComponente("txtAba20codigo");
        adicionarBotaoPreencherFeriados();
    }
    private void adicionarBotaoPreencherFeriados(){
        JPanel panel2 = getComponente("panel2");

        JButton btnPreencherFeriados = new JButton();
        btnPreencherFeriados.setText("Preencher Repositorio");
        btnPreencherFeriados.setBounds(280,15,154, 30);
        btnPreencherFeriados.addActionListener(e -> btnPreencherPressed())

        panel2.add(btnPreencherFeriados);
    }
    private void btnPreencherPressed(){
        MTextFieldString txtAba20codigo = getComponente("txtAba20codigo");
        MSpread sprAba2001s = getComponente("sprAba2001s");

        sprAba2001s.clear();
        sprAba2001s.refreshAll();

        if(!"002".equals(txtAba20codigo.getValue().toString())) interromper("Funcionalidade apenas para o repositório de FERIADOS.");

        try{
            TableMap body = new TableMap();
            List<TableMap> feriados = new ArrayList();
            WorkerRequest.create(tarefa.getWindow())
                    .initialText("Compondo lista de feriados")
                    .dialogVisible(true)
                    .controllerEndPoint("servlet")
                    .methodEndPoint("run")
                    .param("name", "Silcon.servlet.CGS_Compor_Feriados_Repositorio")
                    .header("ignore-body-decrypt", "true")
                    .parseBody(body)
                    .success((response) -> {
                        List<TableMap> listFeriados = response.parseResponse(new TypeReference<List<TableMap>>(){});

                        if(listFeriados != null && listFeriados.size() > 0){
                            for(feriado in listFeriados){
                                if(feriado.getString("type").toUpperCase() == "NATIONAL"){
                                    TableMap tmFeriado = new TableMap();
                                    String data = feriado.getString("date");
                                    String descrFeriado = feriado.getString("name");

                                    tmFeriado.put("data", data.replace("-",""));
                                    tmFeriado.put("feriado", descrFeriado);

                                    preencherSpread(tmFeriado);
                                }
                            }
                        }
                    })
                    .post();

        }catch(Exception err){
            throw new ValidacaoException(err.getMessage());
        }
    }
    private void preencherSpread(TableMap tmFeriado){
        MSpread sprAba2001s = getComponente("sprAba2001s");

        Aba2001 aba2001 = new Aba2001();
        aba2001.setAba2001json(tmFeriado);
        sprAba2001s.addRow(aba2001);
        sprAba2001s.refreshAll();
    }

    @Override
    public void preSalvar(boolean salvo) {
    }

    @Override
    public void posSalvar(Long id) {
    }
}