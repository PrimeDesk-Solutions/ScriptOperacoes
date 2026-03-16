import multitec.swing.components.spread.MSpread
import multitec.swing.core.MultitecRootPanel;

public class Script extends sam.swing.ScriptBase{
    @Override
    public void execute(MultitecRootPanel tarefa) {
        alterarPosicoesSpread();
    }
    private void alterarPosicoesSpread(){
        MSpread sprDocumentos = getComponente("sprDocumentos");
        sprDocumentos.getColumnIndex("daa01.daa01central.abb01num") != -1 ? sprDocumentos.moveColumn(sprDocumentos.getColumnIndex("daa01.daa01central.abb01num"), 1) : null;
        sprDocumentos.getColumnIndex("daa01.daa01central.abb01parcela") != -1 ? sprDocumentos.moveColumn(sprDocumentos.getColumnIndex("daa01.daa01central.abb01parcela"), 2) : null;
        sprDocumentos.getColumnIndex("daa01.daa01central.abb01ent.abe01na") != -1 ? sprDocumentos.moveColumn(sprDocumentos.getColumnIndex("daa01.daa01central.abb01ent.abe01na"), 3) : null;
        sprDocumentos.getColumnIndex("daa01.daa01central.abb01data") != -1 ? sprDocumentos.moveColumn(sprDocumentos.getColumnIndex("daa01.daa01central.abb01data"), 4) : null;
        sprDocumentos.getColumnIndex("daa01.daa01dtVctoN") != -1 ? sprDocumentos.moveColumn(sprDocumentos.getColumnIndex("daa01.daa01dtVctoN"), 5) : null;
        sprDocumentos.getColumnIndex("daa01.daa01valor") != -1 ? sprDocumentos.moveColumn(sprDocumentos.getColumnIndex("daa01.daa01valor"), 6) : null;
        sprDocumentos.getColumnIndex("ocorrencia") != -1 ? sprDocumentos.moveColumn(sprDocumentos.getColumnIndex("ocorrencia"), 7) : null;
    }
}