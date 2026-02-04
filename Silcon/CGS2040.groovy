    /*
        FUNÇÃO:
            1. Deixa o check de item flegado como default
            2. Deixa o check de condição de pagamento flegado como default
            3. Preenche a condição de pagamento com 000 inicial e final
            4. Cria dois botões na tela
                4.1 Botão 1: Atualiza o valor dos itens das demais tabelas de preço, exceto a tabela 001, utilizando os percentuais de cada uma;
                4.2 Botão 2: Atualiza o valor dos itens das demais tabelas, exceto tabela 001, sem considerar os percentuais das tabelas;
     */

    import br.com.multitec.utils.ValidacaoException
    import multitec.swing.components.MCheckBox
    import multitec.swing.core.MultitecRootPanel
    import sam.model.entities.ab.Abe4001
    import javax.swing.JPanel;
    import javax.swing.JButton
    import java.awt.Container
    import java.util.function.Consumer;
    import multitec.swing.components.spread.MSpread;
    import br.com.multitec.utils.collections.TableMap;
    import multitec.swing.components.textfields.MTextFieldString;
    import java.awt.Robot
    import java.awt.event.KeyEvent


    public class Script extends sam.swing.ScriptBase {
        public Consumer exibirRegistroPadrao;
        MultitecRootPanel panel;

        @Override
        public void execute(MultitecRootPanel tarefa) {
            this.panel = tarefa;
            this.exibirRegistroPadrao = tarefa.exibirRegistro
            tarefa.exibirRegistro = { definirCamposDefault(tarefa) }

            criarBotoesAtualizarPrecoEmOutrasTabelas();
        }

        @Override
        public void preSalvar(boolean salvo) {
        }

        @Override
        public void posSalvar(Long id) {
        }

        private void definirCamposDefault(MultitecRootPanel tarefa) {
            this.exibirRegistroPadrao.accept(tarefa.registro)
            MCheckBox chkItem = getComponente("chkItem");
            MCheckBox chkCondPgto = getComponente("chkCondPgto");
            def nvgAbe30codigoInicial = getComponente("nvgAbe30codigoInicial");
            def nvgAbe30codigoFinal = getComponente("nvgAbe30codigoFinal");

            chkItem.setValue(1);
            chkCondPgto.setValue(1);
            nvgAbe30codigoInicial.getNavigationController().setIdValue(420586);
            nvgAbe30codigoFinal.getNavigationController().setIdValue(420586);
        }

        private void criarBotoesAtualizarPrecoEmOutrasTabelas() {
            criarBotaoAtualizarPrecoComDesconto();
            criarBotaoAtualizarPrecoSemDesconto();
        }

        private void criarBotaoAtualizarPrecoComDesconto() {
            JButton btnAtualizarPreco = new JButton();
            btnAtualizarPreco.setText("Atualizar c/ Desconto");
            btnAtualizarPreco.setBounds(850, 303, 150, 30);
            btnAtualizarPreco.addActionListener(e -> atualizarPrecoComDesconto())

            panel.add(btnAtualizarPreco);
        }

        private void criarBotaoAtualizarPrecoSemDesconto() {
            JButton btnAtualizarPreco = new JButton();
            btnAtualizarPreco.setText("Atualizar s/ Desconto");
            btnAtualizarPreco.setBounds(1005, 303, 120, 30);
            btnAtualizarPreco.addActionListener(e -> atualizarPrecoSemDesconto())

            panel.add(btnAtualizarPreco);

        }
        private void atualizarPrecoComDesconto() {
            try{
                MSpread sprAbe4001s = getComponente("sprAbe4001s");
                MTextFieldString txtAbe40codigo = getComponente("txtAbe40codigo");
                String codTabela = txtAbe40codigo.getValue();
                List<Abe4001> sprItens = sprAbe4001s.getValue();

                if(codTabela == null) throw new ValidacaoException("Não foi informado o código da tabela de preõ na tela.");
                if(!codTabela.equals("001")) throw new ValidacaoException("Esse procedimento deverá ser realizado somente pela tabela 001.");
                if (sprItens.size() == 0) throw new ValidacaoException("Necessário haver pelo menos um item na lista de itens.");
                if (!exibirQuestao("Deseja atualizar os itens da lista nas demais tabelas de preço, considerando seus respectivos percentuais de ajustes?")) return;
                List<TableMap> listTmTabelas = buscarInformacoesTabelasPrecos();
                Integer itensAtualizados = 0;

                if(listTmTabelas == null || listTmTabelas.size() == 0) throw new ValidacaoException("Não foram encontradas outras tabelas para atualizar.");

                List<TableMap> idsItensTab = buscarItensTabela();

                if(listTmTabelas != null && listTmTabelas.size() > 0){
                    for(tabela in listTmTabelas){
                        Long idTabela = tabela.getLong("abe40id");
                        BigDecimal percentAjuste = tabela.getBigDecimal_Zero("percentAjuste");
                        Integer ajuste = tabela.getInteger("ajuste");

                        if(percentAjuste.compareTo(new BigDecimal(0)) == 0) continue;

                        for(abe4001 in sprItens){
                            Long idItem = abe4001.abe4001item.abm01id;
                            TableMap itemEncontrado = idsItensTab.stream().filter({f -> f.getLong("abe4001item") == idItem}).filter({f -> f.getLong("abe4001tab") == idTabela}).findFirst().orElse(null)
                            if(itemEncontrado == null) inserirItemTabelaPreco(abe4001, idTabela);
                            BigDecimal preco = abe4001.abe4001preco_Zero;
                            BigDecimal vlrAjuste = preco * percentAjuste / 100;
                            BigDecimal novoPreco;

                            if(preco.compareTo(new BigDecimal(0)) == 0) continue;

                            if(ajuste == 0){ // Somar ajuste
                                novoPreco = preco + vlrAjuste
                            }else{
                                novoPreco = preco - vlrAjuste;
                            }

                            executarSalvarOuExcluir("UPDATE abe4001 SET abe4001preco = " + novoPreco.round(2) + " WHERE abe4001tab = " + idTabela + " AND abe4001item = " + idItem);

                            itensAtualizados++
                        }
                    }
                }
                if(itensAtualizados == 0) throw new ValidacaoException("Não foram encontrados itens nas demais tabelas para realizar atualização!");

                exibirInformacao("Itens atualizados com sucesso!");

                executarF9();
            }catch (Exception err){
                interromper(err.getMessage())
            }
        }
        private void atualizarPrecoSemDesconto(){
            try{
                MSpread sprAbe4001s = getComponente("sprAbe4001s");
                MTextFieldString txtAbe40codigo = getComponente("txtAbe40codigo");
                String codTabela = txtAbe40codigo.getValue();
                List<Abe4001> sprItens = sprAbe4001s.getValue();

                if(codTabela == null) throw new ValidacaoException("Não foi informado o código da tabela de preõ na tela.")
                if(!codTabela.equals("001")) throw new ValidacaoException("Esse procedimento deverá ser realizado somente pela tabela 001.");
                if (sprItens.size() == 0) throw new ValidacaoException("Necessário haver pelo menos um item na lista de itens.");
                if (!exibirQuestao("Deseja atualizar o preço de todos os itens da lista nas demais tabelas?")) return;
                List<TableMap> listTmTabelas = buscarInformacoesTabelasPrecos();
                Integer itensAtualizados = 0;


                if(listTmTabelas == null || listTmTabelas.size() == 0) throw new ValidacaoException("Não foram encontradas outras tabelas para atualizar.");

                List<TableMap> idsItensTab = buscarItensTabela();

                for(tabela in listTmTabelas){
                    Long idTabela = tabela.getLong("abe40id");
                    for(abe4001 in sprItens){
                        Long idItem = abe4001.abe4001item.abm01id;
                        TableMap itemEncontrado = idsItensTab.stream().filter({f -> f.getLong("abe4001item") == idItem}).filter({f -> f.getLong("abe4001tab") == idTabela}).findFirst().orElse(null)
                        if(itemEncontrado == null) inserirItemTabelaPreco(abe4001, idTabela);
                        BigDecimal preco = abe4001.abe4001preco_Zero;

                        if(preco.compareTo(new BigDecimal(0)) == 0) continue;

                        executarSalvarOuExcluir("UPDATE abe4001 SET abe4001preco = " + preco.round(2) + " WHERE abe4001tab = " + idTabela + " AND abe4001item = " + idItem);

                        itensAtualizados++
                    }
                }

                if(itensAtualizados == 0) throw new ValidacaoException("Não foram encontrados itens nas demais tabelas para realizar atualização!");

                exibirInformacao("Itens atualizados com sucesso!");

                executarF9();
            }catch (Exception err){
                interromper(err.getMessage())
            }
        }
        private void inserirItemTabelaPreco(Abe4001 abe4001, Long idTabela){
            Long idItem = abe4001.abe4001item.abm01id;
            Long idCondPgto = abe4001.abe4001cp.abe30id;
            BigDecimal txDesc = abe4001.abe4001txDesc_Zero;
            BigDecimal qtdMax = abe4001.abe4001qtMax_Zero;
            BigDecimal preco = abe4001.abe4001preco_Zero;
            BigDecimal txComis0 = abe4001.abe4001txComis0_Zero;
            BigDecimal txComis1 = abe4001.abe4001txComis1_Zero;
            BigDecimal txComis2 = abe4001.abe4001txComis2_Zero;
            BigDecimal txComis3 = abe4001.abe4001txComis3_Zero;
            BigDecimal txComis4 = abe4001.abe4001txComis4_Zero;

            String sql = " INSERT INTO abe4001 " +
                            " VALUES " +
                            " ( " +
                            " NEXTVAL('DEFAULT_SEQUENCE'), " +
                            idTabela + ", " +
                            idItem + ", " +
                            idCondPgto + ", " +
                            txDesc + ", " +
                            qtdMax + ", " +
                            preco + ", " +
                            txComis0 + ", " +
                            txComis1 + ", " +
                            txComis2 + ", " +
                            txComis3 + ", " +
                            txComis4 +  ", " +
                            " NULL ) ";

            executarSalvarOuExcluir(sql);
        }
        private List<TableMap> buscarItensTabela(){
            String sql = "SELECT abe4001item, abe4001tab FROM abe4001 " +
                            " WHERE abe4001tab NOT IN (SELECT abe40id FROM abe40 WHERE abe40codigo = '001') ";

            return executarConsulta(sql);
        }
        private List<TableMap> buscarInformacoesTabelasPrecos() {
            String sql = "  SELECT abe40id, CAST(abe40camposCustom ->> 'percent_ajuste' AS NUMERIC(18,6)) AS percentAjuste, CAST(abe40camposCustom ->> 'ajuste_novo_preco' AS INTEGER) AS ajuste " +
                    " FROM abe40 " +
                    " WHERE abe40gc = 1075797 "+ // Empresa 001
                    " AND abe40codigo <> '001'";

            return executarConsulta(sql);

        }
        private void executarF9(){

            Robot robot = new Robot()
            robot.keyPress(KeyEvent.VK_F9)
            robot.keyRelease(KeyEvent.VK_F9)
        }
    }