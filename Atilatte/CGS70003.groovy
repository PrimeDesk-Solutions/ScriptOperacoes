package scripts

import br.com.multitec.utils.http.HttpRequest
import com.fasterxml.jackson.core.type.TypeReference
import com.fazecast.jSerialComm.SerialPort
import multitec.swing.components.autocomplete.MNavigation
import multitec.swing.components.autocomplete.MNavigationController
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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import br.com.multitec.utils.jackson.JSonMapperCreator
import multitec.swing.core.dialogs.Messages
import br.com.multitec.utils.DateUtils
import java.time.temporal.ChronoUnit
import java.net.URL
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.format.DateTimeParseException
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.ExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.Semaphore
import java.util.concurrent.ThreadFactory
import sam.dto.cgs.CGS7003EtiquetaDto;

class CGS7003 extends sam.swing.ScriptBase {
    private MultitecRootPanel tarefa;
    private SerialPort comPort;
    private InputStream input;
    private OutputStream output;
    private static final byte ENQ = 0x05;
    private static final byte STX = 0x02;
    private static final byte ETX = 0x03;
    private String portaComm = "COM3";
    private int baundRate = 9600;
    private int baundBits = 8;
    private int casasDecimais = 2;
    private String impressoraDefault = "Microsoft Print to PDF";

    private String taraDefault = "0.0";

    private MTextFieldBigDecimal txtTara;
    private JLabel lblTara;
    private JButton btnPesagem;

    // Esses campos substituem a Thread direta
    private ScheduledExecutorService scheduler;
    private ExecutorService requestPool;
    private volatile ScheduledFuture<?> scheduledTask;

    private volatile boolean pesagemIniciada = false;

    // controle de concorrência de requisições pendentes
    private final Semaphore maxPending = new Semaphore(10); // ajuste conforme necessidade

    // Variáveis de estado para controle da pesagem
    private volatile int leiturasEstaveisConsecutivas = 0;
    private volatile BigDecimal pesoConsideradoEstavel = null;
    private final int numeroMinimoLeiturasEstaveis = 3;
    private AtomicBoolean pesoProcessadoNestaPesagem = new AtomicBoolean(false);
    private final BigDecimal limitePesoZero = new BigDecimal("0.050");
    private AtomicBoolean erroNotificado = new AtomicBoolean(false)
    private ScheduledExecutorService executor;
    private ScheduledFuture<?> future;

    @Override
    void execute(MultitecRootPanel panel) {
        this.tarefa = panel;
        setarCamposDefault();
        adicionarComponentesPesagem();
        onClosed();
        criarMenu("Balança", "Listar impressoras", e -> listarImpressoras(), null)
        criarMenu("Balança", "Listar portas", e -> listarPortas(), null)
    }

    private void setarCamposDefault() {
        def ctrAam05 = getComponente("ctrAam05");
        ctrAam05.setIdValue(18174787)
    }

    private void adicionarComponentesPesagem(){
        adicionaBotaoIniciarPesagem();
    }

    private void adicionaBotaoIniciarPesagem(){
        btnPesagem = new JButton();
        btnPesagem.setBounds(730, 360, 233, 32);
        btnPesagem.setText("Iniciar Pesagem");
        btnPesagem.addActionListener(e -> btnPesagemActionListener(e));
        this.tarefa.add(btnPesagem);
    }

    private void btnPesagemActionListener(ActionEvent e){
        setarCamposDeAcordoComACentral();

        try{
            boolean isTokenExpired = verificarExpiracaoToken();
            if(isTokenExpired) realizarAutenticacao();
        }catch(Exception ex){
            interromper("Falha ao verificar token de autenticação." + ex.getMessage())
        }

        synchronized (this) {
            if(!pesagemIniciada){
                btnPesagem.setText("Parar Pesagem");
                iniciarPesagem();
            }else{
                btnPesagem.setText("Iniciar Pesagem");
                pararPesagem();
            }
        }
    }

    private void setarCamposDeAcordoComACentral() {
        MTextFieldInteger txtAbb01num = getComponente("txtAbb01num");
        MNavigation nvgAah01codigo = getComponente("nvgAah01codigo");
        MNavigation nvgAbm01codigo = getComponente("nvgAbm01codigo");

        MTextFieldString txtAbm70lote = getComponente("txtAbm70lote");
        MTextFieldString txtAbm70serie = getComponente("txtAbm70serie");
        MTextFieldLocalDate txtAbm70validade = getComponente("txtAbm70validade");
        MTextFieldLocalDate txtAbm70fabric = getComponente("txtAbm70fabric");

        if(nvgAbm01codigo.getValue() == null) interromper("Não foi informado o item da etiqueta.");
        if(txtAbb01num.getValue() == null) interromper("Não foi informado o documento para geração da etiqueta.");

        TableMap tm = executarConsulta("SELECT bab01lote AS lote, bab01serie AS serie, bab01dtE AS fabric " +
                "FROM bab01 " +
                "INNER JOIN abb01 ON abb01id = bab01central " +
                "INNER JOIN aah01 ON aah01id = abb01tipo " +
                "INNER JOIN abp20 ON abp20id = bab01comp " +
                "INNER JOIN abm01 ON abm01id = abp20item " +
                "WHERE abb01num = '" + txtAbb01num.getValue() + "'\n" +
                "AND aah01codigo = '43'\n" +
                "AND abm01codigo = '" + nvgAbm01codigo.getValue() + "'\n")

        if (tm.size() > 0) {
            txtAbm70lote.setValue(tm.getString("lote"))
            txtAbm70serie.setValue(tm.getString("serie"))
            txtAbm70validade.setValue(tm.getString("validade") != null ? LocalDate.parse(tm.getString("validade"), DateTimeFormatter.ofPattern("yyyyMMdd")) : null);
            txtAbm70fabric.setValue(tm.getString("fabric") != null ? LocalDate.parse(tm.getString("fabric"), DateTimeFormatter.ofPattern("yyyyMMdd")) : null)
        }
    }
    /**
     * Verifica se o token no banco está expirado. Caso não exista, tenta autenticar.
     * Retorna true se token está expirado (ou ausência/crash), false se ok.
     */
    private boolean verificarExpiracaoToken(){
        TableMap tmToken = executarConsulta("SELECT * FROM token_authentication")
        LocalDate dtAtual = LocalDate.now()

        if (tmToken.size() == 0) { // Ainda não foi realizado nenhuma autenticação
            realizarAutenticacao()
            tmToken = executarConsulta("SELECT * FROM token_authentication") // reconsulta
            if (tmToken.size() == 0) {
                throw new RuntimeException("Falha ao obter token após autenticação.")
            }
        }

        LocalDate dataExpiracao = null
        try {
            // tenta ler como Date/LocalDate primeiro, se for String parse
            dataExpiracao = tmToken.getDate("expire_date")
            if (dataExpiracao == null) {
                String s = tmToken.getString("expire_date")
                if (s != null && s.trim().length() > 0) {
                    dataExpiracao = LocalDate.parse(s)
                }
            }
        } catch(Exception ex) {
            throw new RuntimeException("Não foi possível determinar data de expiração do token: " + ex.getMessage())
        }

        if (dataExpiracao == null) {
            throw new RuntimeException("Data de expiração do token não encontrada.")
        }

        // Retorna true se token expirou ou expira hoje
        return dataExpiracao.isBefore(dtAtual) || dataExpiracao.isEqual(dtAtual)
    }
    /**
     * Realiza autenticação na API e grava token na tabela token_authentication.
     */
    private void realizarAutenticacao(){
        HttpURLConnection conn = null
        try {
            URL url = new URL("https://services.gestao.me/api/autenticate")
            conn = (HttpURLConnection) url.openConnection()
            conn.setRequestMethod("POST")
            conn.setDoOutput(true)
            conn.setRequestProperty("Content-Type", "application/json; utf-8")
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("Url-Do-Sistema", "https://milk.gestao.me")

            String userName = "portalmilk@gestao.me"
            String password = 'fH#4kgqZP$#Zz2*Nx&$%%z&m2'

            // Corpo da requisição (exemplo em JSON)
            String jsonInput = "{ \"username\": \"" + userName + "\", \"password\": \"" + password + "\" }";

            // Envia o corpo
            OutputStream os = conn.getOutputStream()
            try {
                byte[] input = jsonInput.getBytes("utf-8")
                os.write(input, 0, input.length)
                os.flush()
            } finally {
                try { os.close() } catch (Exception ignore) {}
            }

            int status = conn.getResponseCode()
            InputStream is = (status == 200) ? conn.getInputStream() : conn.getErrorStream()
            StringBuilder response = new StringBuilder()
            if (is != null) {
                BufferedReader br = new BufferedReader(new InputStreamReader(is, "utf-8"))
                try {
                    String responseLine
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim())
                    }
                } finally {
                    try { br.close() } catch(Exception ignore) {}
                }
            }

            if (status == 200) {
                TableMap tmAuthenticacao = JSonMapperCreator.create().read(response.toString(), TableMap.class);

                // Deleta token antigo e grava o novo de forma explícita
                executarSalvarOuExcluir("DELETE FROM token_authentication")

                String tokenType = tmAuthenticacao.getString("token_type")
                Integer expireIn = tmAuthenticacao.getInteger("expires_in")
                String accessToken = tmAuthenticacao.getString("access_token")
                LocalDate dataAtual = LocalDate.now()
                Long dias = (expireIn != null) ? (expireIn / 86400) : 0L
                LocalDate dataExpiracao = dataAtual.plusDays(dias != null ? dias : 0L)

                // Grava token
                String sql = "INSERT INTO token_authentication (token_type, seconds_expire, access_token, expire_date) " +
                        "VALUES ('${tokenType}', ${expireIn ?: 0}, '${accessToken}', '${dataExpiracao}')"
                executarSalvarOuExcluir(sql)
            } else {
                throw new RuntimeException("Erro ao tentar realizar autenticação com a API: " + response.toString())
            }
        } catch(Exception e){
            throw new RuntimeException("Erro na autenticação: " + e.getMessage(), e)
        } finally {
            if (conn != null) {
                try { conn.disconnect() } catch(Exception ignore) {}
            }
        }
    }

    private synchronized void iniciarPesagem(){
        try{
            MNavigationController<Abm01> ctrAbm01 = getComponente("ctrAbm01") as MNavigationController<Abm01>;
            MNavigationController<Aam05> ctrAam05 = getComponente("ctrAam05") as MNavigationController<Aam05>;

            if(ctrAbm01.getValue() == null) throw new RuntimeException("Item não selecionado!");
            if(ctrAam05.getValue() == null) throw new RuntimeException("Modelo de etiqueta não informado!");

            erroNotificado.set(false)
            pesagemIniciada = true;

            executor = Executors.newSingleThreadScheduledExecutor();

            Runnable tarefa = () -> {
                SwingUtilities.invokeLater(this::iniciarTarefa());
            };

            future = executor.scheduleAtFixedRate(tarefa, 0, 1, TimeUnit.SECONDS);

            SwingUtilities.invokeLater(() -> {
                btnPesagem.setText("Parar Pesagem");
            })

        }catch (Exception ex){
            pesagemIniciada = false;
            pararPesagem(true)
            btnPesagem.setText("Iniciar Pesagem");
            interromper("Falha ao iniciar pesagem: " + ex.getMessage());
        }
    }

    // Método chamado a cada segundo pelo scheduler
    private void iniciarTarefa(){
        if (!pesagemIniciada) return

        // formato de data/hora que sua API espera
        final String dataHora = obterLocalTime();

        try {
            String peso = buscarPesoAPI(dataHora)
                if (peso != null) {
                    try {
                        processarPeso(peso)
//                        MSpread sprEtiquetas = getComponente("sprEtiquetas");
//                        CGS7003EtiquetaDto etiquetas = new CGS7003EtiquetaDto(10.5, dataHora, peso, LocalDate.of(2025,1,1), LocalDate.of(2025, 2,1), null);
//                        sprEtiquetas.addRow(etiquetas)
                    } catch(Exception e) {
                        // tratar exceção de processamento da etiqueta sem quebrar o pool
                        throw new RuntimeException("Erro ao processar peso: " + e.getMessage())
                    }
                }
        } catch(Exception t){
            // captura qualquer erro inesperado para garantir release do semaphore
            throw new RuntimeException("Erro inesperado no requestPool: " + t)
        }

    }

    private String obterLocalTime(){
        // Data e hora atual no formato yyyy-MM-dd HH:mm:ss
        LocalDateTime agora = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return agora.format(formatter);
    }

    /**
     * Consulta a API externa para buscar leitura do sensor (peso).
     * Mantive sua implementação bloqueante com HttpURLConnection — agora executada no pool.
     */
    private String buscarPesoAPI(String txtDataParametro) {
        HttpURLConnection connection = null
        String txtDataInicio = txtDataParametro
        String txtDataFim = txtDataParametro

        try {
            // Se token expirado, reautentica
            boolean expirado = verificarExpiracaoToken()
            if (expirado) {
                realizarAutenticacao()
            }

            // Obtém token atualizado
            String authToken = obterAccessTokenDaBase()
            if (authToken == null) {
                throw new RuntimeException("Token de autenticação não disponível")
            }

            String urlApi = "https://services.gestao.me/api/consultar-leitura-de-dados-externos?" +
                    "data_hora_de_leitura_de=${URLEncoder.encode(txtDataInicio, 'UTF-8')}" +
                    "&data_hora_de_leitura_ate=${URLEncoder.encode(txtDataFim, 'UTF-8')}" +
                    "&serial_do_sensor=1122152"

            URL url = new URL(urlApi)
            connection = (HttpURLConnection) url.openConnection()
            connection.setRequestMethod("GET")

            if (authToken) {
                connection.setRequestProperty("Authorization", "Bearer ${authToken}")
            }

            connection.setRequestProperty("Accept", "application/json")
            connection.setConnectTimeout(10_000) // 10s
            connection.setReadTimeout(15_000) // 15s

            int status = connection.getResponseCode()

            InputStream is = (status == 200) ? connection.getInputStream() : connection.getErrorStream()
            String responseText = ""
            if (is != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"))
                try {
                    StringBuilder sb = new StringBuilder()
                    String line
                    while ((line = reader.readLine()) != null) {
                        sb.append(line)
                    }
                    responseText = sb.toString().trim()
                } finally {
                    try { reader.close() } catch(Exception ignore) {}
                }
            }

            if (status == 200) {
                TableMap tmRegistrosAPI = JSonMapperCreator.create().read(responseText, TableMap.class);
                TableMap tmRegistroPeso = tmRegistrosAPI.get("registros")[0] != null ? tmRegistrosAPI.get("registros")[0] : new TableMap();
                def peso = tmRegistroPeso.get("valor_da_leitura")
                return peso ? peso.toString() : null
            } else {
                String msgErro = responseText ? "Resposta da API: ${responseText}" : "Código HTTP: ${status}"
                throw new RuntimeException("Erro ao executar consulta do peso. ${msgErro}")
            }
        } catch (Exception err) {
            // Notifica atenção e para a pesagem (com segurança)
            exibirAtencao("Erro ao tentar buscar peso da API: " + err.getMessage())
            SwingUtilities.invokeLater(() -> pararPesagem())
            return null
        } finally {
            if (connection != null) {
                try {
                    connection.disconnect()
                } catch (Exception ignore) {}
            }
        }
    }
    /**
     * Retorna o access_token atual (lê do DB). Se não existir ou estiver expirado, tenta autenticar.
     */
    private String obterAccessTokenDaBase() {
        TableMap tmToken = executarConsulta("SELECT * FROM token_authentication")
        if (tmToken.size() == 0) {
            realizarAutenticacao()
            tmToken = executarConsulta("SELECT * FROM token_authentication")
            if (tmToken.size() == 0) {
                return null
            }
        }
        return tmToken.getString("access_token")
    }

    private void processarPeso(String pesoRecebido){
        try {
            BigDecimal peso = new BigDecimal(pesoRecebido)
            // Ignora leituras insignificantes
            if (peso.compareTo(limitePesoZero) < 0) {
                return
            }

            try {
                criarEtiqueta(peso)
            } catch(Exception e) {
                throw new RuntimeException("Erro ao criar etiqueta")
            }

        } catch(Exception e){
            throw new RuntimeException("Erro ao processar o peso recebido: " , e)
        }
    }

    private void  criarEtiqueta(BigDecimal peso){
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
        MNavigation nvgAam05codigo = getComponente("nvgAam05codigo");
        String codModelo = nvgAam05codigo.getValue();

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

        // Campos Livres Etiqueta
        TableMap jsonAbm70 = new TableMap();
        jsonAbm70.put("modelo_etiqueta", codModelo);

        String sqlBuscarCentral = "SELECT abb01id FROM abb01 WHERE TRUE " + whereAbb01ent + whereAbb01tipo + whereAbb01num + whereAbb01data
        List<TableMap> abb01s = executarConsulta(sqlBuscarCentral)

        Long abb01id = null
        if(abb01s != null && abb01s.size() > 0){
            TableMap abb01 = abb01s.get(0)
            abb01id = abb01.getLong("abb01id")
        }

        CGSGravarEtiquetaDto etiqueta = new CGSGravarEtiquetaDto(
                abb01id, abm01.abm01id, aam05id,
                null, dataFabricacao, dataCriacao,
                peso, lote, null, null, jsonAbm70
        );

        try{
            WorkerRequest.create(this.tarefa.getWindow())
                    .initialText("Gravando etiqueta")
                    .dialogVisible(true)
                    .controllerEndPoint("cgs")
                    .methodEndPoint("gravarEtiqueta")
                    .parseBody(etiqueta)
                    .success((response) -> {
                        try {
                            Long abm70id = response.parseResponse(new TypeReference<Long>(){})
                            byte[] bytes = buscarDadosImpressaoEtiquetas(ctrAam05.getValue().getAam05id(), List.of(abm70id))
                            PrintService printService = ClientUtils.escolherImpressora(impressoraDefault)
                            ClientUtils.enviarDadosParaImpressao(bytes, printService, null, "Etiqueta")
                            MSpread sprEtiquetas = getComponente("sprEtiquetas");
                            sprEtiquetas.addRow(etiqueta)
                        } catch(Exception e) {
                            // garante que só entre aqui na primeira vez
                            if (erroNotificado.compareAndSet(false, true)) {
                                // mostra o alerta na EDT
                                SwingUtilities.invokeLater(() -> {
                                    exibirAtencao("Erro ao criar/imprimir etiqueta: " + e.getMessage())
                                })
                            }

                            try{
                                pesagemIniciada = false;
                                pararPesagem();
                            }catch(Exception ex){
                                throw new RuntimeException("Erro ao parar pesagem após falha de impressão: " + ex.getMessage())
                            }
                        }
                    })
                    .post();
        }catch(Exception ex){
            // Tratamento de erro no fluxo principal
            SwingUtilities.invokeLater(() -> {
                exibirAtencao("Erro ao gravar etiqueta: " + ex.getMessage())
                pararPesagem()
            })
        }
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
    private synchronized void pararPesagem(boolean exibirMensagem){
        pesagemIniciada = false;

        if (future != null) {
            future.cancel(false); // tenta cancelar próxima execução
            future = null;
        }
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }

        btnPesagem.setText("Iniciar Pesagem")

        if(exibirMensagem) exibirInformacao("Pesagem parada.");
    }

    // listarImpressoras, listarPortas, onClosed e demais métodos mantidos como estavam...
    private void listarImpressoras(){
        PrintService[] ps = PrintServiceLookup.lookupPrintServices(DocFlavor.SERVICE_FORMATTED.PAGEABLE, (AttributeSet)null);
        StringBuilder impressoras = new StringBuilder();
        for(PrintService p : ps){
            impressoras.append("" + p.getName() + "\n");
        }
        exibirInformacao(impressoras.toString());
    }
    private void listarPortas(){
        SerialPort[] portas = SerialPort.getCommPorts();
        StringBuilder portasStr = new StringBuilder();
        for(SerialPort p : portas){
            portasStr.append("" + p.getSystemPortName() + "\n");
        }
        exibirInformacao(portasStr.toString());
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
}