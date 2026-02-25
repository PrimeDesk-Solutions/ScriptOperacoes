import multitec.swing.core.MultitecRootPanel;
import br.com.multitec.utils.collections.TableMap
import com.amazonaws.protocol.json.internal.JsonMarshaller
import multitec.swing.core.MultitecRootPanel
import javax.swing.JButton;
import javax.swing.JLabel;
import java.awt.Rectangle;
import java.awt.Point;
import multitec.swing.components.textfields.MTextFieldBigDecimal;
import javax.swing.JPanel
import java.awt.event.ActionEvent
import java.awt.event.ActionListener;
import multitec.swing.components.spread.MSpread;
import multitec.swing.components.spread.MSpread
import javax.swing.event.TableModelEvent
import javax.swing.event.TableModelListener
import java.awt.event.ActionListener
import sam.dto.cgs.CGS2050DocumentoSCFDto
import java.time.temporal.ChronoUnit
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class Script extends sam.swing.ScriptBase{
    MTextFieldBigDecimal txtTotalDesconto = new MTextFieldBigDecimal();
    MTextFieldBigDecimal txtTotalMulta = new MTextFieldBigDecimal();
    MTextFieldBigDecimal txtTotalJuros = new MTextFieldBigDecimal();
    MTextFieldBigDecimal txtTotalEncargos = new MTextFieldBigDecimal();
    MTextFieldBigDecimal txtTotalGeral = new MTextFieldBigDecimal();
    JLabel lblTotalGeral = new JLabel();
    private boolean preenchendoSpread = false;
    @Override
    public void execute(MultitecRootPanel tarefa) {
        adicionarEventoBtnMostrar();
        reordenarColunas();
        alterarPosicoesComponentes();
        adicionarEventoSpread();
        criarComponentes();
    }
    private void adicionarEventoBtnMostrar(){
        JButton btnMostrarEntRealizar = getComponente("btnMostrarEntRealizar");
        btnMostrarEntRealizar.addActionListener(new ActionListener() {
            @Override
            void actionPerformed(ActionEvent e) {
                preenchendoSpread = true;
            }
        })
    }
    private void reordenarColunas(){
        MSpread sprEntidadeRealizarAReceber = getComponente("sprEntidadeRealizarAReceber");
        sprEntidadeRealizarAReceber.getColumnIndex("daa01json.desconto") != -1 ? sprEntidadeRealizarAReceber.moveColumn(sprEntidadeRealizarAReceber.getColumnIndex("daa01json.desconto"), 9) : null;
        sprEntidadeRealizarAReceber.getColumnIndex("daa01json.multa") != -1 ? sprEntidadeRealizarAReceber.moveColumn(sprEntidadeRealizarAReceber.getColumnIndex("daa01json.multa"), 10) : null;
        sprEntidadeRealizarAReceber.getColumnIndex("daa01json.juros") != -1 ? sprEntidadeRealizarAReceber.moveColumn(sprEntidadeRealizarAReceber.getColumnIndex("daa01json.juros"), 11) : null;
        sprEntidadeRealizarAReceber.getColumnIndex("daa01json.encargos") != -1 ? sprEntidadeRealizarAReceber.moveColumn(sprEntidadeRealizarAReceber.getColumnIndex("daa01json.encargos"), 12) : null;
    }
    private void alterarPosicoesComponentes(){
        MTextFieldBigDecimal txtTotalEntidadeRealizarAReceber = getComponente("txtTotalEntidadeRealizarAReceber");        JLabel lblTotalAReceber = getComponente("lblTotalAReceber");

        txtTotalEntidadeRealizarAReceber.setBounds(160, 275, 137, 25);
        lblTotalAReceber.setBounds(100, 275, 137, 25);
    }
    private void criarComponentes(){
        JPanel pnlRealizarEntidade = getComponente("pnlRealizarEntidade");

        // Lbl Descontos
        txtTotalDesconto.setBounds(300, 275, 137, 25);
        txtTotalDesconto.setMaxValue(new BigDecimal("99999999999999.99"));
        txtTotalDesconto.setEditable(false);
        txtTotalDesconto.setEnabled(false);

        // Lbl Multa
        txtTotalMulta.setBounds(435, 275, 137, 25);
        txtTotalMulta.setMaxValue(new BigDecimal("99999999999999.99"));
        txtTotalMulta.setEditable(false);
        txtTotalMulta.setEnabled(false);
//
        // Lbl Juros
        txtTotalJuros.setBounds(570, 275, 137, 25);
        txtTotalJuros.setMaxValue(new BigDecimal("99999999999999.99"));
        txtTotalJuros.setEditable(false);
        txtTotalJuros.setEnabled(false);

        // Lbl Encargos
        txtTotalEncargos.setBounds(705, 275, 137, 25);
        txtTotalEncargos.setMaxValue(new BigDecimal("99999999999999.99"));
        txtTotalEncargos.setEditable(false);
        txtTotalEncargos.setEnabled(false);

        // Lbl Txt Total Geral
        lblTotalGeral.setText("Total Geral");
        lblTotalGeral.setBounds(850, 275, 137, 25);

        // Txt Total Geral
        txtTotalGeral.setBounds(920, 275, 137, 25);
        txtTotalGeral.setMaxValue(new BigDecimal("99999999999999.99"));
        txtTotalGeral.setEditable(false);
        txtTotalGeral.setEnabled(false);

        pnlRealizarEntidade.add(txtTotalDesconto);
        pnlRealizarEntidade.add(txtTotalMulta);
        pnlRealizarEntidade.add(txtTotalJuros);
        pnlRealizarEntidade.add(txtTotalEncargos);
        pnlRealizarEntidade.add(lblTotalGeral);
        pnlRealizarEntidade.add(txtTotalGeral);
    }
    private void adicionarEventoSpread(){
        MSpread sprEntidadeRealizarAReceber = getComponente("sprEntidadeRealizarAReceber");

        sprEntidadeRealizarAReceber.getModel().addTableModelListener(e -> {

            if(e.getType() != TableModelEvent.UPDATE) return;

            // Não executa quando ordena a spread
            if(!preenchendoSpread) return;

            callback();
        });
    }
    private void callback(){
        MSpread sprEntidadeRealizarAReceber = getComponente("sprEntidadeRealizarAReceber");
        int size = sprEntidadeRealizarAReceber.getValue().size();
        if(size > 0) somarCamposLivresSpread(sprEntidadeRealizarAReceber.getValue())
    }
    private void somarCamposLivresSpread(List<TableMap> vlrSpread){

        BigDecimal totDesconto = BigDecimal.ZERO;
        BigDecimal totMulta = BigDecimal.ZERO;
        BigDecimal totJuros = BigDecimal.ZERO;
        BigDecimal totEncargos = BigDecimal.ZERO;
        BigDecimal totDocs = BigDecimal.ZERO;

        for(registro in vlrSpread ){
            interromper(registro.toString())
            Integer diasAtraso = registro.getInteger("dias");
            diasAtraso < 0 ? registro.put("daa01json.juros", registro.getBigDecimal_Zero("daa01json.juros") * diasAtraso.abs()) : registro.put("daa01json.juros", BigDecimal.ZERO)
            totDesconto += registro.getBigDecimal_Zero("daa01json.desconto");
            totMulta += registro.getBigDecimal_Zero("daa01json.multa");
            totJuros += registro.getBigDecimal_Zero("daa01json.juros");
            totEncargos += registro.getBigDecimal_Zero("daa01json.encargos");
            totDocs += registro.getBigDecimal("valor");
        }

        BigDecimal totGeral = totDocs + totMulta + totJuros + totEncargos - totDesconto.abs();

        txtTotalDesconto.setValue(totDesconto.abs().round(2));
        txtTotalMulta.setValue(totMulta.round(2));
        txtTotalJuros.setValue(totJuros.round(2));
        txtTotalEncargos.setValue(totEncargos.round(2));
        txtTotalGeral.setValue(totGeral.round(2));

        preenchendoSpread = false;

    }
    private void zerarCamposCustomizados(){
        txtTotalDesconto.setValue(BigDecimal.ZERO)
        txtTotalMulta.setValue(BigDecimal.ZERO)
        txtTotalJuros.setValue(BigDecimal.ZERO)
        txtTotalEncargos.setValue(BigDecimal.ZERO)
        txtTotalGeral.setValue(BigDecimal.ZERO)
    }
}