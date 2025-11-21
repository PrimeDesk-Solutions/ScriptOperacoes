import br.com.multitec.utils.http.HttpRequest
import com.fasterxml.jackson.core.type.TypeReference
import com.fazecast.jSerialComm.SerialPort
import groovy.transform.ThreadInterrupt
import multitec.swing.components.autocomplete.MNavigationController
import multitec.swing.components.autocomplete.MNavigation

import multitec.swing.components.spread.MSpread
import multitec.swing.components.textfields.MTextFieldBigDecimal
import multitec.swing.components.textfields.MTextFieldInteger
import multitec.swing.components.textfields.MTextFieldLocalDate
import multitec.swing.components.textfields.MTextFieldString
import multitec.swing.core.MultitecRootPanel
import multitec.swing.request.WorkerRequest
import sam.dto.cgs.CGS7050ImprimirDto
import sam.dto.cgs.CGSGravarEtiquetaDto
import sam.dto.cgs.CentralExisteDocDto
import sam.model.entities.aa.Aah01
import sam.model.entities.aa.Aam05
import sam.model.entities.ab.Abb01
import sam.model.entities.ab.Abe01
import sam.model.entities.ab.Abm01
import sam.model.entities.aa.Aab10
import sam.model.entities.ba.Bab01
import sam.swing.ScriptBase
import sam.swing.tarefas.cas.ClientUtils
import br.com.multitec.utils.collections.TableMap
import javax.print.DocFlavor
import javax.print.PrintService
import javax.print.PrintServiceLookup
import javax.print.attribute.AttributeSet
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.SwingUtilities
import java.awt.event.ActionEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.math.BigDecimal
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.atomic.AtomicBoolean
import java.util.List

class Script extends ScriptBase {
    MultitecRootPanel tarefa;
    private JButton btnPesagem;
    private volatile boolean pesagemIniciada = false;
    private SerialPort porta;
    private InputStream input;
    private OutputStream output;
    private String portaComm = "COM8";
    private int baundRate = 9600;
    private int baundBits = 8;
    private Thread threadPesagem;
    private static final byte STX = 0x02;

    // Variáveis de estado para controle da pesagem
    private volatile int leiturasEstaveisConsecutivas = 0;
    private volatile BigDecimal pesoConsideradoEstavel = null;
    private final int numeroMinimoLeiturasEstaveis = 4;
    private AtomicBoolean pesoProcessadoNestaPesagem = new AtomicBoolean(false);
    private final BigDecimal limitePesoZero = new BigDecimal("0.040");
    private int casasDecimais = 3;




    @Override
    void execute(MultitecRootPanel panel) {
        this.tarefa = panel;
        setarCamposDefault();
        adicionarComponentesPesagem();
    }
    private void setarCamposDefault() {
        MNavigationController ctrAam05 = getComponente("ctrAam05")
        Aab10 aab10 = obterUsuarioLogado();

        ctrAam05.setIdValue(18174787);
    }
    private void adicionarComponentesPesagem(){
        adicionarBotaoIniciarPesagem();
    }
    private void adicionarBotaoIniciarPesagem(){
        btnPesagem = new JButton();
        btnPesagem.setBounds(730, 360, 233, 32);
        btnPesagem.setText("Iniciar Pesagem");
        btnPesagem.addActionListener(e -> btnPesagemActionListener(e));
        this.tarefa.add(btnPesagem);
    }
    private btnPesagemActionListener(ActionEvent e){
        setarCAmposDeAcordoComACentral();
        synchronized (this){
            if(!pesagemIniciada){
                btnPesagem.setText("Parar Pesagem");
                iniciarPesagem();
            }else{
                btnPesagem.setText("Iniciar Pesagem");
                pararPesagem();
            }
        }
    }
    private void setarCAmposDeAcordoComACentral() {
        MTextFieldInteger txtAbb01num = getComponente("txtAbb01num");
        MNavigation nvgAah01codigo = getComponente("nvgAah01codigo");
        MNavigation nvgAbm01codigo = getComponente("nvgAbm01codigo");

        MTextFieldString txtAbm70lote = getComponente("txtAbm70lote")
        MTextFieldString txtAbm70serie = getComponente("txtAbm70serie")
        MTextFieldLocalDate txtAbm70validade = getComponente("txtAbm70validade")
        MTextFieldLocalDate txtAbm70fabric = getComponente("txtAbm70fabric")

        TableMap tm = executarConsulta("SELECT bab01lote AS lote, bab01serie AS serie, CAST(bab01dtE + abm14validDias AS DATE) AS validade, bab01ctDtI AS fabric\n" +
                "FROM bab01\n" +
                "INNER JOIN abb01 ON abb01id = bab01central " +
                "INNER JOIN aah01 ON aah01id = abb01tipo " +
                "INNER JOIN abp20 ON abp20id = bab01comp " +
                "INNER JOIN abm01 ON abm01id = abp20item " +
                "INNER JOIN abm0101 ON abm0101item = abm01id "+
                "INNER JOIN abm14 ON abm14id = abm0101producao "+
                "WHERE abb01num = '" + txtAbb01num.getValue() + "' " +
                "AND aah01codigo = '43' " +
                "AND abm01codigo = '" + nvgAbm01codigo.getValue() + "' ");

        if (tm.size() > 0) {
            txtAbm70lote.setValue(tm.getString("lote"))
            txtAbm70serie.setValue(tm.getString("serie"))
            txtAbm70validade.setValue(tm.getString("validade") != null ? LocalDate.parse(tm.getString("validade"), DateTimeFormatter.ofPattern("yyyyMMdd")) : null);
            txtAbm70fabric.setValue(tm.getString("fabric") != null ? LocalDate.parse(tm.getString("fabric"), DateTimeFormatter.ofPattern("yyyyMMdd")) : null)
        }
    }
    private void iniciarPesagem(){

        try{
            MNavigationController<Abm01> ctrAbm01 = getComponente("ctrAbm01") as MNavigationController<Abm01>;
            MNavigationController<Aam05> ctrAam05 = getComponente("ctrAam05") as MNavigationController<Aam05>;

            if(ctrAbm01.getValue() == null) throw new RuntimeException("Item não selecionado!");
            if(ctrAam05.getValue() == null) throw new RuntimeException("Modelo de etiqueta não informado!");

            resetVariaveisDePesagem();

            // Configuração da porta serial
            porta = SerialPort.getCommPort(portaComm);
            porta.setBaudRate(baundRate);
            porta.setNumDataBits(baundBits);
            porta.setNumStopBits(SerialPort.ONE_STOP_BIT);
            porta.setParity(SerialPort.NO_PARITY);

            if(!porta.openPort()) throw new RuntimeException("Não foi possível abrir a porta: " + portaComm);

            exibirInformacao("Porta aberta com sucesso!")

            // INICIALIZAR STREAMS — obrigatório!!
            input = porta.getInputStream();
            //output = porta.getOutputStream();

            // Marca a pesagem como iniciada após abertura da porta com sucesso
            pesagemIniciada = true;

            def self = this
            threadPesagem = new Thread(() -> {
                try{
                    StringBuilder buffer = new StringBuilder();
                    boolean start = false; // Controla quando o STX foi recebido
                    while (pesagemIniciada){
                        if(input.available() > 0){
                            int dado = input.read();

                            char c = (char) dado;

                            // Inicio de um novo peso
                            if(c == STX ){
                                start = true;
                                buffer.setLength(0) // Reinicia o buffer
                                continue;
                            }

                            if(!start) continue // Se não encontrado STX, ignora tudo

                            // Acumula os bytes dentro do buffer após o STX
                            buffer.append(c);

                            if(buffer.length() >= 6){
                                String pesoStr = buffer.toString().trim();

                                processarMensagemCompleta(pesoStr, self);
                            }
                        }

                        // Pausa curta para não sobrecarregar a CPU
                        Thread.sleep(10);
                    }

                }catch(InterruptedException ei){
                    Thread.currentThread().interrupt()
                }catch(Exception ex){
                    if(pesagemIniciada){
                        exibirAtencao("Erro na Thread de leitura da balança " + ex.getMessage());
                        SwingUtilities.invokeLater(() -> pararPesagem())
                    }
                }
            })
            threadPesagem.start()
        }catch (Exception e){
            pesagemIniciada = false;
            btnPesagem.setText("Iniciar Pesagem");
            interromper("Falha ao iniciar pesagem: " + e.getMessage());
        }

    }
    private synchronized void resetVariaveisDePesagem() {
        leiturasEstaveisConsecutivas = 0;
        pesoConsideradoEstavel = null;
        pesoProcessadoNestaPesagem.set(false);
    }

    private void processarMensagemCompleta(String pesoStr, Script self){
        try{
            // Extrair apenas os dígitos numéricos
            String pesoNumericoStr = pesoStr.replaceAll("[^0-9]", "");
            if (pesoNumericoStr.isEmpty()) {
                return; // Ignorar mensagens sem dígitos
            }

            // Formatar o peso com casas decimais
            String tempPesoFormatado;
            if (pesoNumericoStr.length() <= casasDecimais) {
                tempPesoFormatado = "0." + pesoNumericoStr.padLeft(casasDecimais, '0');
            } else {
                String parteInteira = pesoNumericoStr.substring(0, pesoNumericoStr.length() - casasDecimais);
                String parteDecimal = pesoNumericoStr.substring(pesoNumericoStr.length() - casasDecimais);
                tempPesoFormatado = parteInteira + "." + parteDecimal;
            }

            BigDecimal pesoLidoAtual = new BigDecimal(pesoNumericoStr);

            // Lógica de estabilidade
            synchronized (self){
                if(pesoProcessadoNestaPesagem.get()){
                    if(pesoLidoAtual.compareTo(limitePesoZero) <= 0){
                        resetVariaveisDePesagem();
                    }
                }else{
                    // Lógica de detecção de estabilidade
                    if(pesoConsideradoEstavel == null || pesoLidoAtual.compareTo(pesoConsideradoEstavel) != 0){
                        // Peso mudou ou é a primeira leitura
                        pesoConsideradoEstavel = pesoLidoAtual;
                        leiturasEstaveisConsecutivas = 1;
                    }else{
                        // Peso igual ao anterior, incrementa contador
                        leiturasEstaveisConsecutivas++;
                        if(leiturasEstaveisConsecutivas >= numeroMinimoLeiturasEstaveis){
                            // Peso considerável estável
                            processaPesoRecebido(tempPesoFormatado);
                            pesoProcessadoNestaPesagem.set(true);

                        }
                    }
                }
            }
        }catch(NumberFormatException e){
            //ignora os erros de formatos de números e continua
        }
    }
    private void  processaPesoRecebido(String pesoRecebido){
        try{
            BigDecimal pesoComTara = new BigDecimal(pesoRecebido);

            if(pesoComTara.compareTo(BigDecimal.ZERO) <= 0){
                return;
            }

            criarEtiqueta(pesoComTara);
        }catch(Exception e){
            exibirAtencao("Erro ao processar peso recebido " + e.getMessage())
        }
    }

    private void criarEtiqueta(BigDecimal peso){
        MSpread sprEtiquetas = getComponente("sprEtiquetas");

        CGSGravarEtiquetaDto etiqueta = new CGSGravarEtiquetaDto(
                null, 377311, 18174787,
                LocalDate.now(), LocalDate.now(), LocalDate.now(),
                peso, "1234", null, null, null
        );

        sprEtiquetas.addRow(etiqueta);
    }
    private pararPesagem(){
        try{
            if(input != null) input.close();
            if(output != null) output.close();
            if(porta != null && porta.isOpen()) porta.closePort();

        }catch(Exception e){
            // Ignora erro ao fechar os recursos
        }finally {
            input = null;
            output = null;
            porta = null;
        }

        resetVariaveisDePesagem();
        btnPesagem.setText("Iniciar Pesagem");
        pesagemIniciada = false;
        exibirInformacao("Porta fechada corretamente!")
    }


}