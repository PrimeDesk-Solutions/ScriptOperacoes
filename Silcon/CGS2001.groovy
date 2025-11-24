/*
    TELA: CGS2001 - ENTIDADES
    FUNÇÃO:
    1- Desabilita o campo de observações caso um vendedor entrar no cadastro de determinada entidade
 */
package scripts

import multitec.swing.components.textfields.MTextArea
import multitec.swing.core.MultitecRootPanel
import sam.swing.ScriptBase
import br.com.multitec.utils.collections.TableMap


class CGS2001 extends ScriptBase{
    @Override
    void execute(MultitecRootPanel panel) {
        MTextArea txtabe01obs = getComponente("txtabe01obs");
        TableMap camposCustomUser = buscarCamposCustomUser(obterUsuarioLogado().getAab10id());

        if(camposCustomUser.getTableMap("aab10camposcustom") != null && camposCustomUser.getTableMap("aab10camposcustom").getInteger("vendedor") == 1) txtabe01obs.setVisible(false)
    }

    private TableMap buscarCamposCustomUser(Long idUser){
        return executarConsulta("SELECT aab10camposCustom FROM aab10 WHERE aab10id = " + idUser)[0];
    }

    @Override
    void preSalvar(boolean salvo) {
    }

    @Override
    void posSalvar(Long id) {
    }
}
