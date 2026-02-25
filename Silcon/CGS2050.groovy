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
import multitec.swing.core.MultitecRootPanel
import javax.swing.event.TableModelEvent
import javax.swing.event.TableModelListener
import java.awt.event.ActionListener
import sam.dto.cgs.CGS2050DocumentoSCFDto
import java.time.LocalDate
import java.time.temporal.ChronoUnit

public class Script extends sam.swing.ScriptBase{
    MTextFieldBigDecimal txtTotalDesconto = new MTextFieldBigDecimal();
    MTextFieldBigDecimal txtTotalMulta = new MTextFieldBigDecimal();
    MTextFieldBigDecimal txtTotalJuros = new MTextFieldBigDecimal();
    MTextFieldBigDecimal txtTotalEncargos = new MTextFieldBigDecimal();
    MTextFieldBigDecimal txtTotalGeral = new MTextFieldBigDecimal();
    JLabel lblTotalGeral = new JLabel();
    private boolean preenchendoSpread = false;


    private ActionListener[] actionEventOriginal;

    @Override
    public void execute(MultitecRootPanel tarefa) {
        reordenarColunas();
        adicionarEventoBtnAReceber();
        adicionarEventoBtnAPagar();
        adicionarEventoBtnRecebidos()
        adicionarEventoBtnPagos();
        adicionarEventoBtnPrevisaoAPagar();
        adicionarEventoBtnPrevisaoAReceber();
        alterarPosicoesComponentes();
        adicionarEventoSpread();
        criarComponentes();
    }
    private void reordenarColunas(){
        MSpread sprDocsFin = getComponente("sprDocsFin");
        sprDocsFin.getColumnIndex("daa01json.dias") != -1 ? sprDocsFin.moveColumn(sprDocsFin.getColumnIndex("daa01json.dias"), 7) : null;
        sprDocsFin.getColumnIndex("daa01json.desconto") != -1 ? sprDocsFin.moveColumn(sprDocsFin.getColumnIndex("daa01json.desconto"), 8) : null;
        sprDocsFin.getColumnIndex("daa01json.multa") != -1 ? sprDocsFin.moveColumn(sprDocsFin.getColumnIndex("daa01json.multa"), 9) : null;
        sprDocsFin.getColumnIndex("daa01json.juros") != -1 ? sprDocsFin.moveColumn(sprDocsFin.getColumnIndex("daa01json.juros"), 10) : null;
        sprDocsFin.getColumnIndex("daa01json.encargos") != -1 ? sprDocsFin.moveColumn(sprDocsFin.getColumnIndex("daa01json.encargos"), 11) : null;
    }
    private void adicionarEventoBtnAReceber(){
        JButton btnAReceber = getComponente("btnAReceber");
        btnAReceber.addActionListener(new ActionListener() {
            @Override
            void actionPerformed(ActionEvent e) {
                preenchendoSpread = true;
                zerarCamposCustomizados()
            }
        });
    }
    private void adicionarEventoBtnAPagar(){
        JButton btnAPagar = getComponente("btnAPagar");
        btnAPagar.addActionListener(new ActionListener() {
            @Override
            void actionPerformed(ActionEvent e) {
                preenchendoSpread = true;
                zerarCamposCustomizados()
            }
        });
    }
    private void adicionarEventoBtnRecebidos(){
        JButton btnRecebidos = getComponente("btnRecebidos");
        btnRecebidos.addActionListener(new ActionListener() {
            @Override
            void actionPerformed(ActionEvent e) {
                preenchendoSpread = true;
                zerarCamposCustomizados()
            }
        });
    }
    private void adicionarEventoBtnPagos(){
        JButton btnPagos = getComponente("btnPagos");
        btnPagos.addActionListener(new ActionListener() {
            @Override
            void actionPerformed(ActionEvent e) {
                preenchendoSpread = true;
                zerarCamposCustomizados()
            }
        });
    }
    private void adicionarEventoBtnPrevisaoAPagar(){
        JButton btnPrevisaoPagar = getComponente("btnPrevisaoPagar");
        btnPrevisaoPagar.addActionListener(new ActionListener() {
            @Override
            void actionPerformed(ActionEvent e) {
                preenchendoSpread = true;
                zerarCamposCustomizados()
            }
        });
    }
    private void adicionarEventoBtnPrevisaoAReceber(){
        JButton btnPrevisaoReceber = getComponente("btnPrevisaoReceber");
        btnPrevisaoReceber.addActionListener(new ActionListener() {
            @Override
            void actionPerformed(ActionEvent e) {
                preenchendoSpread = true;
                zerarCamposCustomizados()
            }
        });
    }
    private void alterarPosicoesComponentes(){
        JLabel lblTotalFinanceiro = getComponente("lblTotalFinanceiro");
        JLabel lblTotalValorFinanceiro = getComponente("lblTotalValorFinanceiro");
        MTextFieldBigDecimal txtTotalValorFinanceiro = getComponente("txtTotalValorFinanceiro");
        MTextFieldBigDecimal txtTotalValorPagoFinanceiro = getComponente("txtTotalValorPagoFinanceiro");
        JLabel lblTotalValorPagoFinanceiro = getComponente("lblTotalValorPagoFinanceiro");

        lblTotalFinanceiro.setBounds(new Rectangle(new Point(250, 536), lblTotalFinanceiro.getPreferredSize()));
        lblTotalValorFinanceiro.setBounds(300, 536, (lblTotalValorFinanceiro.getPreferredSize()).width as int, 14);
        txtTotalValorFinanceiro.setBounds(345, 525, 124, (txtTotalValorFinanceiro.getPreferredSize()).height as int);
        txtTotalValorPagoFinanceiro.setBounds(1200, 525, 124, (txtTotalValorPagoFinanceiro.getPreferredSize()).height as int);
        lblTotalValorPagoFinanceiro.setBounds(1140, 536, (lblTotalValorPagoFinanceiro.getPreferredSize()).width as int, 14);
        lblTotalFinanceiro.setVisible(false)
    }

    private void criarComponentes(){
        JPanel pnlDocsFinanceiros = getComponente("pnlDocsFinanceiros");

        // Lbl Descontos
        txtTotalDesconto.setBounds(470, 525, 100, (txtTotalDesconto.getPreferredSize()).height as int);
        txtTotalDesconto.setMaxValue(new BigDecimal("99999999999999.99"));
        txtTotalDesconto.setEditable(false);
        txtTotalDesconto.setEnabled(false);

        // Lbl Multa
        txtTotalMulta.setBounds(570, 525, 100, (txtTotalMulta.getPreferredSize()).height as int);
        txtTotalMulta.setMaxValue(new BigDecimal("99999999999999.99"));
        txtTotalMulta.setEditable(false);
        txtTotalMulta.setEnabled(false);

        // Lbl Juros
        txtTotalJuros.setBounds(670, 525, 100, (txtTotalJuros.getPreferredSize()).height as int);
        txtTotalJuros.setMaxValue(new BigDecimal("99999999999999.99"));
        txtTotalJuros.setEditable(false);
        txtTotalJuros.setEnabled(false);

        // Lbl Encargos
        txtTotalEncargos.setBounds(770, 525, 100, (txtTotalEncargos.getPreferredSize()).height as int);
        txtTotalEncargos.setMaxValue(new BigDecimal("99999999999999.99"));
        txtTotalEncargos.setEditable(false);
        txtTotalEncargos.setEnabled(false);

        // Lbl Txt Total Geral
        lblTotalGeral.setText("Total Geral");
        lblTotalGeral.setBounds(870, 536, (lblTotalGeral.getPreferredSize()).width as int, 14);

        // Txt Total Geral
        txtTotalGeral.setBounds(930, 525, 100, (txtTotalGeral.getPreferredSize()).height as int);
        txtTotalGeral.setMaxValue(new BigDecimal("99999999999999.99"));
        txtTotalGeral.setEditable(false);
        txtTotalGeral.setEnabled(false);

        pnlDocsFinanceiros.add(txtTotalDesconto);
        pnlDocsFinanceiros.add(txtTotalMulta);
        pnlDocsFinanceiros.add(txtTotalJuros);
        pnlDocsFinanceiros.add(txtTotalEncargos);
        pnlDocsFinanceiros.add(lblTotalGeral);
        pnlDocsFinanceiros.add(txtTotalGeral);
    }
    private void adicionarEventoSpread(){
        MSpread sprDocsFin = getComponente("sprDocsFin");

        sprDocsFin.getModel().addTableModelListener(e -> {

            if(e.getType() != TableModelEvent.UPDATE) return;

            // Não executa quando ordena a spread
            if(!preenchendoSpread) return;

            callback();
        });
    }
    private void callback(){
        MSpread sprDocsFin = getComponente("sprDocsFin");
        int size = sprDocsFin.getValue().size();
        if(size > 0) somarCamposLivresSpread(sprDocsFin.getValue())
    }
    private void somarCamposLivresSpread(List<CGS2050DocumentoSCFDto> vlrSpread){

        LocalDate dtAtual = LocalDate.now();
        BigDecimal totDesconto = BigDecimal.ZERO;
        BigDecimal totMulta = BigDecimal.ZERO;
        BigDecimal totJuros = BigDecimal.ZERO;
        BigDecimal totEncargos = BigDecimal.ZERO;
        BigDecimal totDocs = BigDecimal.ZERO;

        for(int i = 0; i < vlrSpread.size(); i++ ){
            TableMap jsonDaa01 = vlrSpread.get(i).getDaa01json() != null ? vlrSpread.get(i).getDaa01json() : new TableMap();
            LocalDate dtVctoN =  vlrSpread.get(i).getVencimento();
            Integer diasAtraso = ChronoUnit.DAYS.between(dtAtual, dtVctoN);
            diasAtraso < 0 ? jsonDaa01.put("juros", jsonDaa01.getBigDecimal_Zero("juros") * diasAtraso.abs()) : jsonDaa01.put("juros", BigDecimal.ZERO)
            totDesconto += jsonDaa01.getBigDecimal_Zero("desconto");
            totMulta += jsonDaa01.getBigDecimal_Zero("multa");
            totJuros += jsonDaa01.getBigDecimal_Zero("juros");
            totEncargos += jsonDaa01.getBigDecimal_Zero("encargos");
            totDocs += vlrSpread.get(i).getValor();

            jsonDaa01.put("dias", diasAtraso);
            vlrSpread.get(i).setDaa01json(jsonDaa01);
        }

        BigDecimal totGeral = totDocs + totMulta + totJuros + totEncargos - totDesconto.abs();

        txtTotalDesconto.setValue(totDesconto.abs().round(2));
        txtTotalMulta.setValue(totMulta.round(2));
        txtTotalJuros.setValue(totJuros.round(2));
        txtTotalEncargos.setValue(totEncargos.round(2));
        txtTotalGeral.setValue(totGeral.round(2));

        preenchendoSpread = false
    }
    private void zerarCamposCustomizados(){
        txtTotalDesconto.setValue(BigDecimal.ZERO)
        txtTotalMulta.setValue(BigDecimal.ZERO)
        txtTotalJuros.setValue(BigDecimal.ZERO)
        txtTotalEncargos.setValue(BigDecimal.ZERO)
        txtTotalGeral.setValue(BigDecimal.ZERO)
    }

}