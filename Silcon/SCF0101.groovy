    /*
        TELA: SCF0101 - RECEBIMENTOS
        FUNCÃO:

        1- Cria um botão customizado na tela do financeiro para imprimir os boletos individualmente
        2- Verifica se foi inserido departamento e naturezas antes de salvar o documento
     */
    package scripts

    import multitec.swing.components.autocomplete.MNavigation
    import multitec.swing.components.textfields.MTextFieldInteger
    import multitec.swing.components.textfields.MTextFieldLocalDate
    import multitec.swing.components.textfields.MTextFieldString
    import multitec.swing.components.textfields.MTextFieldBigDecimal
    import sam.swing.tarefas.scf.SCF0103
    import sam.swing.tarefas.scf.SCF0116

    import java.awt.event.ActionEvent
    import java.awt.event.ActionListener
    import java.awt.print.PrinterJob

    import javax.print.DocFlavor
    import javax.print.PrintService
    import javax.print.PrintServiceLookup
    import javax.swing.JButton
    import javax.swing.JComboBox
    import javax.swing.JOptionPane

    import org.apache.pdfbox.pdmodel.PDDocument
    import org.apache.pdfbox.printing.PDFPageable

    import br.com.multitec.utils.ValidacaoException
    import br.com.multitec.utils.collections.TableMap
    import br.com.multitec.utils.http.HttpRequest
    import multitec.swing.components.spread.MSpread
    import multitec.swing.core.MultitecRootPanel
    import multitec.swing.core.dialogs.ErrorDialog
    import multitec.swing.core.dialogs.Messages
    import multitec.swing.request.WorkerRunnable
    import multitec.swing.request.WorkerSupplier
    import sam.swing.ScriptBase
    import sam.swing.tarefas.spv.SPV1001
    import sam.swing.tarefas.scf.SCF0101
    import sam.model.entities.da.Daa01;
    import multitec.swing.core.utils.WindowUtils;
    import sam.swing.tarefas.sce.SCE1503;



    import com.fasterxml.jackson.databind.JsonNode
    import com.fasterxml.jackson.databind.ObjectMapper

    import java.time.LocalDate

    class SCF0101 extends sam.swing.ScriptBase {
        private MultitecRootPanel tarefa;

        public void preSalvar(boolean salvo) {
            // Verifica se foi informado departamento ou natureza no documento
            verificarDepartamentosNaturezas();
        }

        @Override
        public void execute(MultitecRootPanel tarefa) {
            this.tarefa = tarefa;
            adicionaBotaoImprimirDoc();
            adicionarBotaoEstornarDocumento();
            criarMenu("Customizado", "Estornar Documento", e -> estornarDocumento(), null);
        }

        private void verificarDepartamentosNaturezas(){
            MSpread sprDaa0101s = getComponente("sprDaa0101s");
            MSpread sprDaa01011s = getComponente("sprDaa01011s");

            def spreadDepartamento = sprDaa0101s.value();
            def spreadNaturezas = sprDaa01011s.value();

            if(spreadDepartamento.size() == 0 || spreadNaturezas.size() == 0 ) interromper("Não é permitido a inclusão de documentos sem departamentos ou naturezas.")
        }

        private void estornarDocumento(){
            try{
                MNavigation nvgAbb10codigo = getComponente("nvgAbb10codigo");
                MNavigation nvgAah01codigo = getComponente("nvgAah01codigo");
                MNavigation nvgAbe01codigo = getComponente("nvgAbe01codigo");
                MTextFieldInteger txtAbb01num = getComponente("txtAbb01num");
                MTextFieldString txtAbb01serie = getComponente("txtAbb01serie");
                MTextFieldString txtAbb01parcela = getComponente("txtAbb01parcela");
                MTextFieldInteger txtAbb01quita = getComponente("txtAbb01quita");
                MTextFieldBigDecimal txtAbb01valor = getComponente("txtAbb01valor");
                MTextFieldLocalDate txtDaa01dtPgto = getComponente("txtDaa01dtPgto");
                MTextFieldLocalDate txtDaa01dtBaixa = getComponente("txtDaa01dtBaixa");
                MTextFieldLocalDate txtAbb01data = getComponente("txtAbb01data");
                MSpread sprDaa0101s = getComponente("sprDaa0101s");
                MSpread sprDaa01011s = getComponente("sprDaa01011s");

                String codOperacao = nvgAbb10codigo.getValue();
                String codTipoDoc = nvgAah01codigo.getValue();
                String codEntidade = nvgAbe01codigo.getValue();
                Integer numDoc = txtAbb01num.getValue();
                String serie = txtAbb01serie.getValue();
                String parcela = txtAbb01parcela.getValue();
                String quita = txtAbb01quita.getValue();
                String valor = txtAbb01valor.getValue();
                Long idEmpresa = obterEmpresaAtiva().getAac10id();
                LocalDate dtPgto = txtDaa01dtPgto.getValue();
                LocalDate dtBaixa = txtDaa01dtBaixa.getValue();
                LocalDate dtDoc = txtAbb01data.getValue();
                def departamentos = sprDaa0101s.getValue();
                def naturezas = sprDaa01011s.getValue();

                if(dtPgto == null || dtBaixa == null) throw new RuntimeException("Não é possível estornar um documento não baixado.")

                if(codOperacao == null) interromper("Necessário informar o código da operação.");
                if(codTipoDoc == null) interromper("Necessário informar o tipo de documento.");
                if(codEntidade == null) interromper("Necessário informar o código da entidade.");
                if(numDoc == null) interromper("Necessário informar o número do documento.");
                if(parcela == null) interromper("Necessário informar a parcela.");

                Long idOperacao = obterIdOperacao(codOperacao, idEmpresa);
                Long idTipoDoc = obterIdTipoDoc(codTipoDoc);
                Long idEntidade = obterIdEntidade(codEntidade, idEmpresa);

                if(idOperacao == null) interromper("Não foi encontrado operação comercial com o código " + codOperacao + " na empresa ativa.");
                if(idTipoDoc == null) interromper("Não foi encontrado tipo de documento com o código " + codTipoDoc);
                if(idEntidade == null) interromper("Não foi encontrado entidade com o código " + codEntidade + " na empresa ativa.");

                SCF0116 scf0116 = new SCF0116();
                WindowUtils.createJDialog(scf0116.getWindow(), scf0116);
                scf0116.ctrAbb01operCod.setIdValue(idOperacao);
                scf0116.ctrAbb01tipo.setIdValue(idTipoDoc);
                scf0116.ctrAbb01ent.setIdValue(idEntidade);
                scf0116.txtAbb01num.setValue(numDoc);
                if(serie != null) scf0116.txtAbb01serie.setValue(serie);
                if(parcela != null) scf0116.txtAbb01parcela.setValue(parcela);
                scf0116.txtAbb01quita.setValue(quita);
                scf0116.txtAbb01valor.setValue(new BigDecimal(valor));
                scf0116.txtAbb01data.setValue(dtDoc);
                scf0116.sprDaa0101s.setValue(departamentos)
                scf0116.sprDaa01011s.setValue(naturezas)
                scf0116.open.run();
            }catch(Exception e){
                interromper(e.getMessage())
            }
        }
        private Long obterIdOperacao(String codOperacao, Long idEmpresa){
            TableMap tmOperacao = executarConsulta("SELECT abb10id FROM abb10 WHERE abb10codigo = '" + codOperacao + "' AND abb10gc = " + idEmpresa.toString())[0];

            return tmOperacao != null ? tmOperacao.getLong("abb10id") : null;
        }
        private Long obterIdTipoDoc(String codTipoDoc){
            TableMap tmTipoDoc = executarConsulta("SELECT aah01id FROM aah01 WHERE aah01codigo = '" + codTipoDoc + "'")[0];

            return tmTipoDoc != null ? tmTipoDoc.getLong("aah01id") : null;
        }
        private Long obterIdEntidade(String codEntidade, Long idEmpresa){
            TableMap tmEntidade = executarConsulta("SELECT abe01id FROM abe01 WHERE abe01codigo = '" + codEntidade + "' AND abe01gc = " + idEmpresa)[0];

            return tmEntidade != null ? tmEntidade.getLong("abe01id") : null;
        }
        private void adicionaBotaoImprimirDoc(){
            def tela = tarefa.getWindow();
            tela.setBounds((int) tela.getBounds().x, (int) tela.getBounds().y, (int) tela.getBounds().width, (int) tela.getBounds().height + 40);

            JButton btnImprimir = new JButton();
            btnImprimir.setText("Imprimir Boleto");
            tarefa.add(btnImprimir);
            // X    Y    W  H
            btnImprimir.setBounds(890,635, 120, 36);

            btnImprimir.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    selectionButtonPressed();
                }
            });
        }
        private void adicionarBotaoEstornarDocumento(){
            def tela = tarefa.getWindow();
            tela.setBounds((int) tela.getBounds().x, (int) tela.getBounds().y, (int) tela.getBounds().width, (int) tela.getBounds().height + 40);

            JButton btnImprimir = new JButton();
            btnImprimir.setText("Estornar");
            tarefa.add(btnImprimir);
            // X    Y    W  H
            btnImprimir.setBounds(770,635, 120, 36);

            btnImprimir.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    estornarDocumento();
                }
            });
        }
        private void selectionButtonPressed() {
            try{
                MSpread sprDaa0102s = getComponente("sprDaa0102s");
                def spread = sprDaa0102s.getValue();
                MTextFieldInteger txtAbb01num = getComponente("txtAbb01num");
                MTextFieldString txtAbb01serie = getComponente("txtAbb01serie");
                MNavigation nvgAbe01codigo = getComponente("nvgAbe01codigo");
                MTextFieldString txtAbb01parcela = getComponente("txtAbb01parcela");
                MTextFieldInteger txtAbb01quita = getComponente("txtAbb01quita");
                MNavigation nvgAbf01codigo = getComponente("nvgAbf01codigo");
                MTextFieldLocalDate txtDaa01dtBaixa = getComponente("txtDaa01dtBaixa");
                MTextFieldLocalDate txtDaa01dtPgto = getComponente("txtDaa01dtPgto");
                Integer numDoc = txtAbb01num.getValue();
                String serie = txtAbb01serie.getValue();
                String codigoEnt =nvgAbe01codigo.getValue();
                String parcela =txtAbb01parcela.getValue();
                Integer quita = txtAbb01quita.getValue();
                String codBanco = nvgAbf01codigo.getValue();
                String dtBaixa = txtDaa01dtBaixa.getValue();
                String dtPgto = txtDaa01dtPgto.getValue();

                if(dtBaixa != null || dtPgto != null) interromper("Documento já pago/baixado, não é possível imprimir o boleto.")

                //Verifica se existe integração bancária no documento
                if(spread.size() == 0){
                    interromper("Não foi Possível Gerar Boleto, Documento Sem Integração Bancária");
                }

                //SQL para buscar ID do documento financeiro selecionado em tela
                TableMap documento = executarConsulta( "SELECT daa01id "+
                        "FROM daa01 "+
                        "INNER JOIN abb01 ON abb01id = daa01central "+
                        "INNER JOIN abe01 abe01 ON abe01id = abb01ent "+
                        "WHERE abb01num = '"+numDoc+"' "+
                        "AND abe01codigo = '"+codigoEnt+"' "+
                        "AND abb01serie = '"+serie+"' "+
                        "AND abb01parcela = '"+parcela+"' "+
                        "AND abb01quita = '"+quita+"' ")[0];

                if(documento.size() == 0) interromper("Nenhum documento financeiro encontrado para impressão do boleto.");

                Long idDoc = documento.getLong("daa01id")

                WorkerSupplier.create(this.tarefa.getWindow(), {
                    return buscarDadosImpressao(idDoc, codBanco);
                })
                        .initialText("Imprimindo BOLETO")
                        .dialogVisible(true)
                        .success({ bytes ->
                            enviarDadosParaImpressao(bytes, idDoc);
                        })
                        .start();
            } catch (Exception err) {
                ErrorDialog.defaultCatch(this.tarefa.getWindow(), err);
            }
        }

        private byte[] buscarDadosImpressao(Long idDoc, String codBanco) {
            String json = "{\"nome\":\"Silcon.relatorios.scf.SCF_Boleto_Itau\",\"filtros\":{\"daa01id\":"+idDoc+"}}";

            ObjectMapper mapper = new ObjectMapper();
            JsonNode obj = mapper.readTree(json);
            return HttpRequest.create().controllerEndPoint("relatorio").methodEndPoint("gerarRelatorio").parseBody(obj).post().getResponseBody()
        }

        protected void enviarDadosParaImpressao(byte[] bytes, Long idDoc) {
            try {
                if(bytes == null || bytes.length == 0) {
                    interromper("Não foi encontrado o relatório ou parametrizações para a impressão.");
                }

                PrintService myService = escolherImpressora();

                WorkerRunnable load = WorkerRunnable.create(this.tarefa.getWindow());
                load.dialogVisible(true);
                load.initialText("Enviando Documento para impressão");
                load.runnable({
                    try {
                        PDDocument document = PDDocument.load(bytes);
                        PrinterJob job = PrinterJob.getPrinterJob();
                        job.setPageable(new PDFPageable(document));
                        job.setPrintService(myService);
                        job.setCopies(1);
                        job.setJobName("Boleto " + idDoc);
                        job.print();
                        document.close();
                    }catch (Exception err) {
                        interromper("Erro ao imprimir Documento. Verifique a impressora utilizada.");
                    }
                });
                load.start();
            }catch (Exception err) {
                ErrorDialog.defaultCatch(this.tarefa.getWindow(), err, "Erro ao enviar dados para impressão.");
            }
        }

        protected PrintService escolherImpressora() {
            PrintService myService = null;

            PrintService[] ps = PrintServiceLookup.lookupPrintServices(DocFlavor.SERVICE_FORMATTED.PAGEABLE, null);
            if (ps.length == 0) {
                throw new ValidacaoException("Não foram encontradas impressoras.");
            }else {
                String nomeImpressoraComum = null;

                if(ps.length == 1) {
                    nomeImpressoraComum = ps[0].getName();
                }else {
                    JComboBox<String> jcb = new JComboBox<>();

                    for (PrintService printService : ps) {
                        jcb.addItem(printService.getName());
                    }

                    JOptionPane.showMessageDialog(null, jcb, "Selecione a impressora", JOptionPane.QUESTION_MESSAGE);

                    if (jcb.getSelectedItem() == null) {
                        throw new ValidacaoException("Nenhuma impressora selecionada.");
                    }

                    nomeImpressoraComum = (String)jcb.getSelectedItem();
                }

                for (PrintService printService : ps) {
                    if (printService.getName().equalsIgnoreCase(nomeImpressoraComum)) {
                        myService = printService;
                        break;
                    }
                }

                if (myService == null) {
                    throw new ValidacaoException("Nenhuma impressora selecionada.");
                }
            }

            return myService;
        }
    }