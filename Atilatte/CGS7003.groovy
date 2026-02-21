import br.com.multitec.utils.http.HttpRequest
import com.fasterxml.jackson.core.type.TypeReference
import com.fazecast.jSerialComm.SerialPort
import groovy.transform.ThreadInterrupt
import multitec.swing.components.autocomplete.MNavigationController
import multitec.swing.components.autocomplete.MNavigation
import multitec.swing.components.MComboBox;
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
    private String portaComm = "COM4";
    private int baundRate = 9600;
    private int baundBits = 8;
    private Thread threadPesagem;
    private static final byte STX = 0x02;
    private static final byte CR = 0x0D;
    private String impressoraDefault = "ZEBRA QUEIJO 2";

    // Variáveis de estado para controle da pesagem
    private volatile int leiturasEstaveisConsecutivas = 0;
    private volatile BigDecimal pesoConsideradoEstavel = null;
    private final int numeroMinimoLeiturasEstaveis = 3;
    private AtomicBoolean pesoProcessadoNestaPesagem = new AtomicBoolean(false);
    private final BigDecimal limitePesoZero = new BigDecimal("0.200");
    private int casasDecimais = 3;


    @Override
    void execute(MultitecRootPanel panel) {
        this.tarefa = panel;
        adicionarEventoBtnGravar();
        setarCamposDefault();
        adicionarComponentesPesagem();
        onClosed();

        criarMenu("Balança", "Listar impressoras", e -> listarImpressoras(), null)
        criarMenu("Balança", "Listar portas", e -> listarPortas(), null)
        criarMenu("Balança", "Trazer lote", e-> setarCamposDeAcordoComACentral(), null)
    }
    private void adicionarEventoBtnGravar(){
        JButton btnGravar = getComponente("btnGravar");

        btnGravar.addActionListener(e -> preencherCampoLivre());
    }

    private void preencherCampoLivre(){
        MSpread sprEtiquetas = getComponente("sprEtiquetas");
        def etiquetas = sprEtiquetas.getValue();
        MNavigation nvgAam05codigo = getComponente("nvgAam05codigo");
        String codModelo = nvgAam05codigo.getValue();


        for(int i = 0; i < sprEtiquetas.getRowCount(); i++) {
            TableMap tmEtiqueta = new TableMap();
            tmEtiqueta.put("modelo_etiqueta", codModelo)
            sprEtiquetas.get(i).setJson(tmEtiqueta);

            sprEtiquetas.refreshRow(i);
        }
    }

    private void listarPortas(){
        SerialPort[] portas = SerialPort.getCommPorts();
        StringBuilder portasStr = new StringBuilder();
        for(SerialPort p : portas){
            portasStr.append("" + p.getSystemPortName() + "\n");
        }
        exibirInformacao(portasStr.toString());
    }

    private void listarImpressoras(){
        PrintService[] ps = PrintServiceLookup.lookupPrintServices(DocFlavor.SERVICE_FORMATTED.PAGEABLE, (AttributeSet)null);
        StringBuilder impressoras = new StringBuilder();
        for(PrintService p : ps){
            impressoras.append("" + p.getName() + "\n");
        }
        exibirInformacao(impressoras.toString());
    }
    private void setarCamposDefault() {
        MNavigationController ctrAam05 = getComponente("ctrAam05");
        MComboBox cmbAbm01tipo = getComponente("cmbAbm01tipo");
        MNavigation nvgAah01codigo = getComponente("nvgAah01codigo");

        Aab10 aab10 = obterUsuarioLogado();
        cmbAbm01tipo.setValue(1);
        ctrAam05.setIdValue(18174787);
        nvgAah01codigo.getNavigationController().setIdValue(69766);
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

            // MODIFICAÇÃO: Usar TIMEOUT_READ_SEMI_BLOCKING com timeout curto
            porta.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 500, 0);

            if(!porta.openPort()) throw new RuntimeException("Não foi possível abrir a porta: " + portaComm);

            // INICIALIZAR STREAMS — obrigatório!!
            input = porta.getInputStream();
            //output = porta.getOutputStream();

            // Marca a pesagem como iniciada após abertura da porta com sucesso
            pesagemIniciada = true;

            def self = this
            threadPesagem = new Thread(() -> {
                try{
                    boolean start = false; // Controla quando o STX foi recebido
                    // Buffer para leitura em bloco
                    byte[] buffer = new byte[128];
                    int bytesRead;
                    StringBuilder messageBuilder = new StringBuilder();
                    while (pesagemIniciada){
                        if(Thread.currentThread().isInterrupted()){
                            break;
                        }
                        try{
                            // Ler em bloco - com SEMI_BLOCKING não lança exceção de timeout
                            bytesRead = input.read(buffer);

                            // Verificar se temos dados válidos
                            if (bytesRead > 0) {
                                // Processar todos os bytes lidos
                                for (int i = 0; i < bytesRead; i++) {
                                    byte currentByte = buffer[i];

                                    // Inicio de um novo peso
                                    if(currentByte == STX ){
                                        start = true;
                                        messageBuilder.setLength(0) // Reinicia o buffer
                                        continue;
                                    }

                                    if(!start) continue // Se não encontrado STX, ignora tudo

                                    if(currentByte == CR){
                                        String pesoStr = messageBuilder.toString().trim();
                                        processarMensagemCompleta(pesoStr, self);

                                        messageBuilder.setLength(0);
                                        start = false;
                                        continue
                                    }

                                    // Acumula os bytes dentro do buffer após o STX
                                    messageBuilder.append((char) currentByte);
                                }
                            }
                        }catch(Exception e){
                            // Ignora erro de leitura e continua tentando
                            // Não exibe mensagem para não carregar a interface
                        }

                        // Pausa curta para não sobrecarregar a CPU
                        Thread.sleep(50);
                    }

                }catch(InterruptedException ie){
                    Thread.currentThread().interrupt()
                }catch(Exception ex){
                    if(pesagemIniciada){
                        exibirAtencao("Erro na Thread de leitura da balança " + ex.getMessage());
                        SwingUtilities.invokeLater(() -> pararPesagem())
                    }
                }
            });
            threadPesagem.setName("ThreadPesagemBalanca");
            threadPesagem.start();
            exibirInformacao("A pesagem foi iniciada.");

        }catch (Exception e){
            pesagemIniciada = false;
            if(threadPesagem != null && threadPesagem.isAlive()){
                threadPesagem.interrupt();
            }
            threadPesagem = null;
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
                tempPesoFormatado = "0." + pesoNumericoStr.padLeft(casasDecimais + 1, '0');
            } else {
                String parteInteira = pesoNumericoStr.substring(0, pesoNumericoStr.length() - casasDecimais);
                String parteDecimal = pesoNumericoStr.substring(pesoNumericoStr.length() - casasDecimais);
                tempPesoFormatado = parteInteira + "." + parteDecimal;
            }

            BigDecimal pesoLidoAtual = new BigDecimal(tempPesoFormatado);
            BigDecimal tolerancia = new BigDecimal("0.002")

            // Lógica de estabilidade
            synchronized (self){
                if(pesoProcessadoNestaPesagem.get()){
                    if(pesoLidoAtual.compareTo(limitePesoZero) <= 0){
                        resetVariaveisDePesagem();
                    }
                }else{
                    // Lógica de detecção de estabilidade
                    if(pesoConsideradoEstavel == null ||  pesoLidoAtual.subtract(pesoConsideradoEstavel).abs().compareTo(tolerancia) > 0){
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
        MNavigationController<Abm01> ctrAbm01 = getComponente("ctrAbm01") as MNavigationController<Abm01>;
        MNavigationController<Aam05> ctrAam05 = getComponente("ctrAam05") as MNavigationController<Aam05>;
        MNavigationController<Aah01> ctrAbb01tipo = getComponente("ctrAbb01tipo") as MNavigationController<Aah01>;
        MNavigationController<Abe01> ctrAbb01ent = getComponente("ctrAbb01ent") as MNavigationController<Abe01>;
        MTextFieldLocalDate txtAbm70fabric = getComponente("txtAbm70fabric") as MTextFieldLocalDate;
        MTextFieldLocalDate txtAbm70validade = getComponente("txtAbm70validade") as MTextFieldLocalDate;
        MTextFieldLocalDate txtAbb01data = getComponente("txtAbb01data") as MTextFieldLocalDate;
        MTextFieldString txtAbm70serie = getComponente("txtAbm70serie") as MTextFieldString;
        MTextFieldString txtAbm70lote = getComponente("txtAbm70lote") as MTextFieldString;
        MTextFieldInteger txtAbb01num = getComponente("txtAbb01num") as MTextFieldInteger;
        MSpread sprEtiquetas = getComponente("sprEtiquetas");

        Abm01 abm01 = ctrAbm01.getValue();
        Long aam05id = ctrAam05.getValue().getAam05id();
        LocalDate dataFabricacao = txtAbm70fabric.getValue();
        LocalDate dataValidade = txtAbm70validade.getValue();
        LocalDate dataCriacao = LocalDate.now();
        String lote = txtAbm70lote.getValue();
        String serie = txtAbm70serie.getValue();

        String whereAbb01ent = ctrAbb01ent.getValue() == null ? "" : " AND abb01ent = " + ctrAbb01ent.getValue().getAbe01id() + " "
        String whereAbb01tipo = ctrAbb01tipo.getValue() == null ? "" : " AND abb01tipo = " + ctrAbb01tipo.getValue().getAah01id() + " "
        String whereAbb01num = " AND abb01num = " + txtAbb01num.getValue()
        String whereAbb01data = " AND abb01data = '" + txtAbb01data.getValue() + "'"


        String sqlBuscarCentral = "SELECT abb01id FROM abb01 WHERE TRUE " + whereAbb01ent + whereAbb01tipo + whereAbb01num + whereAbb01data
        List<TableMap> abb01s = executarConsulta(sqlBuscarCentral)

        Long abb01id = null
        if(abb01s != null && abb01s.size() > 0){
            TableMap abb01 = abb01s.get(0)
            abb01id = abb01.getLong("abb01id")
        }

        CGSGravarEtiquetaDto etiqueta = new CGSGravarEtiquetaDto(
                abb01id, abm01.abm01id, aam05id,
                dataValidade, dataFabricacao, dataCriacao,
                peso, lote, serie, null, null
        );


        try{
            WorkerRequest.create(this.tarefa.getWindow())
                    .initialText("Gravando etiqueta")
                    .dialogVisible(true)
                    .controllerEndPoint("cgs")
                    .methodEndPoint("gravarEtiqueta")
                    .parseBody(etiqueta)
                    .success((response) -> {
                        sprEtiquetas.addRow(etiqueta);
                        Long abm70id = response.parseResponse(new TypeReference<Long>(){});
                        byte[] bytes = buscarDadosImpressaoEtiquetas(ctrAam05.getValue().getAam05id(), List.of(abm70id));
                        PrintService printService = ClientUtils.escolherImpressora(impressoraDefault);
                        ClientUtils.enviarDadosParaImpressao(bytes, printService, null, "Etiqueta");
                    })
                    .post();
        }catch(Exception ex){
            exibirAtencao("Etiqueta: " + ex.message)
        }
    }
    private void setarCamposDeAcordoComACentral() {
        def txtAbb01num = getComponente("txtAbb01num");
        def nvgAah01codigo = getComponente("nvgAah01codigo");
        def nvgAbm01codigo = getComponente("nvgAbm01codigo");

        def txtAbm70lote = getComponente("txtAbm70lote");
        def txtAbm70serie = getComponente("txtAbm70serie");
        def txtAbm70validade = getComponente("txtAbm70validade");
        def txtAbm70fabric = getComponente("txtAbm70fabric");

        TableMap tmDiasValidadeItem = executarConsulta("SELECT abm14validDias " +
                " FROM abm01 " +
                " INNER JOIN abm0101 ON abm0101item = abm01id" +
                " INNER JOIN abm14 ON abm14id = abm0101producao" +
                " WHERE abm01codigo = '" + nvgAbm01codigo.getValue() +
                "' AND abm01tipo = 1")[0];

        if(tmDiasValidadeItem.size() == 0) interromper("O item " + nvgAah01codigo.getValue() + " encontra-se sem dias para cálculo da date de validade no parametro de produção.");

        Integer diasValid = tmDiasValidadeItem.getInteger("abm14validDias");

        TableMap tm = executarConsulta("SELECT bab01lote AS lote, bab01serie AS serie, CAST(bab01dtE + INTERVAL '"+ diasValid +" days' AS DATE) AS validade, bab01ctDtI AS fabric " +
                "FROM bab01 " +
                "INNER JOIN abb01 ON abb01id = bab01central " +
                "INNER JOIN aah01 ON aah01id = abb01tipo " +
                "INNER JOIN abp20 ON abp20id = bab01comp " +
                "INNER JOIN abm01 ON abm01id = abp20item " +
                "WHERE abb01num = '" + txtAbb01num.getValue() + "' " +
                "AND aah01codigo = '43' " +
                "AND abm01codigo = '" + nvgAbm01codigo.getValue() + "' ");


        if (tm.size() > 0) {
            txtAbm70lote.setValue(tm.getString("lote"))
            txtAbm70serie.setValue(tm.getString("serie"))
            //txtAbm70validade.setValue(tm.getString("validade") != null ? LocalDate.parse(tm.getString("validade"), DateTimeFormatter.ofPattern("yyyyMMdd")) : null);
            txtAbm70fabric.setValue(tm.getString("fabric") != null ? LocalDate.parse(tm.getString("fabric"), DateTimeFormatter.ofPattern("yyyyMMdd")) : null)
        }
    }

    private Bab01 getProducao(){
        MNavigationController<Bab01> ctrBab01 = getComponente("ctrBab01") as MNavigationController<Bab01>;
        return ctrBab01.getValue();
    }
    private void onClosed(){
        this.tarefa.getWindow().addWindowListener(new WindowAdapter() {
            @Override
            void windowClosed(WindowEvent e) {
                super.windowClosed(e);
                pararPesagem(false);
            }

            @Override
            void windowClosing(WindowEvent e) {
                super.windowClosing(e)
                pararPesagem(false);
            }
        });
    }

    private byte[] buscarDadosImpressaoEtiquetas(Long aam05id, List<Long> abm70ids) {
        CGS7050ImprimirDto dto = new CGS7050ImprimirDto(aam05id, abm70ids);
        return HttpRequest.create()
                .controllerEndPoint("cgs7050")
                .methodEndPoint("buscarDadosImpressaoEtiquetas")
                .parseBody(dto)
                .post()
                .getResponseBody();

    }

    private synchronized void pararPesagem(){
        pararPesagem(true);
    }

    private pararPesagem(boolean exibirMensagem){

        pesagemIniciada = false;

        if(threadPesagem != null){
            threadPesagem.interrupt();
            try{
                threadPesagem.join(1000);
            }catch(InterruptedException e){
                Thread.currentThread().interrupt();
            }
            threadPesagem = null;
        }

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

        if(exibirMensagem) exibirInformacao("Pesagem parada!")
    }


}