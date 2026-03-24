/*
    SPV1001P2 - CONCLUIR PRÉ-VENDA
    FUNÇÃO:

    1. Verifica títulos vencidos: *Somente quando selecionado 'Documento'*
       - Se houver, pergunta se deseja continuar.
    2. Validação do limite de crédito: *Somente quando selecionado 'Documento'*
       - Verifica data limite (se vencida, interrompe).
       - Calcula saldo devedor:
            - DAA01 (Docs a Receber): abb01quita = 0 AND daa01rp = 0
            - EAA01 (Financeiro 2-Batch): abb10tipoCod = 1 AND eaa01esMov = 1 AND eaa01clasDoc = 1 AND eaa01cancData IS NULL AND eaa01iSCF = 2
            - Saldo devedor = soma(Doc. a Receber + Doc. Batch)
       - Se saldo > limite, pergunta se deseja continuar
     3. Altera o número de vias para 1 quando NFCE e inativa o campo de número de vias
     4. Remove o banco da tela quando pressionado concluir venda e tiver selecionado 'Documento' na moldura 'Venda'
     5. Exibe a mensagem (Abe02obsUsoInt) do cadastro da entidade
     6. Altera a view de busca de entidades (F4)

 */
package scripts

import br.com.multitec.utils.UiSqlColumn
import br.com.multitec.utils.ValidacaoException
import br.com.multitec.utils.collections.TableMap
import com.fasterxml.jackson.core.type.TypeReference
import multitec.swing.components.autocomplete.MNavigation
import multitec.swing.core.MultitecRootPanel
import multitec.swing.request.WorkerRequest

import javax.swing.JButton;

import br.com.multitec.utils.collections.TableMap
import multitec.swing.components.autocomplete.MNavigation
import multitec.swing.components.textfields.MTextArea
import multitec.swing.core.MultitecRootPanel

import java.awt.event.FocusEvent
import java.awt.event.FocusListener;
import javax.swing.SwingUtilities

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.function.Consumer;
import multitec.swing.components.MRadioButton;
import multitec.swing.request.WorkerRequest;
import multitec.swing.request.WorkerRunnable;
import com.fasterxml.jackson.core.type.TypeReference;
import multitec.swing.components.MCheckBox;

public class Script extends sam.swing.ScriptBase {
    public Consumer exibirRegistroPadrao;
    private ActionListener[] actionEventOriginal;
    MultitecRootPanel panel;
    public Runnable windowLoadOriginal;

    @Override
    public void execute(MultitecRootPanel tarefa) {
        this.panel = tarefa;
        this.windowLoadOriginal = tarefa.windowLoad;
        tarefa.windowLoad = {novoWindowLoad()};
        //adicionarEventoEntidade();
        //alterarChkImpressao();
        inserirBancoDefault();
        adicionarEventoChkDocumento();
        adicionarEventoChkNFE();
        adicionarEventoChkNFCe();
        adicionarEventoBtnConcluir();
    }
    protected void novoWindowLoad(){
        this.windowLoadOriginal.run();

        def ctrAbe01 = getComponente("ctrAbe01");

        ctrAbe01.f4Columns = () -> {
            java.util.List<UiSqlColumn> uiSqlColumn = new ArrayList<>();
            UiSqlColumn abe01codigo = new UiSqlColumn("abe01codigo", "abe01codigo", "Código", 10);
            UiSqlColumn abe01nome = new UiSqlColumn("abe01nome", "abe01nome", "Nome", 60);
            UiSqlColumn abe01complem = new UiSqlColumn("abe01complem", "abe01complem", "Endereço", 60);
            UiSqlColumn abe01na = new UiSqlColumn("abe01na", "abe01na", "Nome Abreviado", 40);
            UiSqlColumn abe01ni = new UiSqlColumn("abe01ni", "abe01ni", "Número da Inscrição", 60);
            uiSqlColumn.addAll(Arrays.asList(abe01codigo, abe01nome, abe01complem, abe01na, abe01ni));
            return uiSqlColumn;
        };
    }
    private void alterarChkImpressao() {
        Long idUser = obterUsuarioLogado().getAab10id();
        MCheckBox chkImprimir = getComponente("chkImprimir");
        MRadioButton rdoNFCe65 = getComponente("rdoNFCe65");

        //1902426 - ANDREIA
        // 26971862 - ANDRESSA
        // 2200978 - ARLETE
        // 15468639 - VANESSA
        // 1903463 - JANAINA
        // 18353414 - ISABEL
        // 1902688 - CASSIA
        // 1895862 - MASTER2

        if (idUser == 1902426 || idUser == 26971862 || idUser == 2200978 || idUser == 15468639 || idUser == 1903463 ||
                idUser == 18353414 || idUser == 1902688 || idUser == 1895862) {
            if (!rdoNFCe65.isSelected()) chkImprimir.setSelected(false)
        }
    }

    private void adicionarEventoEntidade() {
        MNavigation nvgAbe01codigo = getComponente("nvgAbe01codigo");
        nvgAbe01codigo.addFocusListener(new FocusListener() {
            @Override
            void focusGained(FocusEvent e) {}

            @Override
            void focusLost(FocusEvent e) {
                if (nvgAbe01codigo.getValue() != null) {
                    Long idEntidade = buscarIdEntidade(nvgAbe01codigo.getValue());
                    String msgEnt = buscarMensagemEntidade(idEntidade);

                    // Exibe caixa de dialogo na tela com a mensagem do cadastro da entidade
                    if (msgEnt != null && msgEnt != "") exibirTelaDeAtencaoComMensagemEntidade(msgEnt);
                }
            }
        })
    }

    private inserirBancoDefault() {
        TableMap jsonAab10 = buscarCamposCustomUser();
        MNavigation nvgAbf01codigo = getComponente("nvgAbf01codigo");
        Long idEmpresa = obterEmpresaAtiva().getAac10id();

        if (jsonAab10 != null && jsonAab10.size() > 0) {
            if (jsonAab10.getTableMap("aab10camposcustom").getInteger("setor") == 3 && idEmpresa == 1075797) { // Matriz
                nvgAbf01codigo.getNavigationController().setIdValue(322714)
            } else if (jsonAab10.getTableMap("aab10camposcustom").getInteger("setor") == 3 && idEmpresa == 2116598) { // Filial
                nvgAbf01codigo.getNavigationController().setIdValue(36288893)
            }
        }
    }

    private buscarCamposCustomUser() {
        Long idUser = obterUsuarioLogado().getAab10id();
        String sql = "SELECT aab10camposCustom FROM aab10 WHERE aab10id = " + idUser.toString();

        return executarConsulta(sql)[0]
    }

    private void adicionarEventoChkDocumento() {
        MRadioButton rdoDocumento = getComponente("rdoDocumento");

        rdoDocumento.addFocusListener(new FocusListener() {
            @Override
            void focusGained(FocusEvent e) {
                alterarNumeroDeVias(2, true)
            }

            @Override
            void focusLost(FocusEvent e) {}
        })

        //rdoNFCe65.addActionListener(e -> alterarNumeroDeVias(1, false));
    }

    private void adicionarEventoChkNFE() {
        MRadioButton rdoNFe55 = getComponente("rdoNFe55");

        rdoNFe55.addFocusListener(new FocusListener() {
            @Override
            void focusGained(FocusEvent e) {
                alterarNumeroDeVias(2, true)
            }

            @Override
            void focusLost(FocusEvent e) {}
        })

        //rdoNFCe65.addActionListener(e -> alterarNumeroDeVias(1, false));
    }

    private void adicionarEventoChkNFCe() {
        MRadioButton rdoNFCe65 = getComponente("rdoNFCe65");

        rdoNFCe65.addFocusListener(new FocusListener() {
            @Override
            void focusGained(FocusEvent e) {
                alterarNumeroDeVias(1, false)
            }

            @Override
            void focusLost(FocusEvent e) {}
        })

        //rdoNFCe65.addActionListener(e -> alterarNumeroDeVias(1, false));
    }

    private void alterarNumeroDeVias(Integer qtd, Boolean ativar) {
        try {
            def txtVias = getComponente("txtVias");
            txtVias.setValue(qtd);

            txtVias.setEnabled(ativar);
        } catch (Exception e) {
            interromper(e.getMessage())
        }
    }

    private void adicionarEventoBtnConcluir() {
        JButton btnConcluirVenda = getComponente("btnConcluirVenda");
        MRadioButton rdoDocumento = getComponente("rdoDocumento");
        MNavigation nvgAbf01codigo = getComponente("nvgAbf01codigo");
        actionEventOriginal = btnConcluirVenda.getActionListeners(); // Armazena os eventos default

        for (evento in actionEventOriginal) {
            btnConcluirVenda.removeActionListener(evento); // Remove os evento default do botão
        }

        btnConcluirVenda.addActionListener(new ActionListener() {
            @Override
            void actionPerformed(ActionEvent e) {
                if(rdoDocumento.isSelected()) nvgAbf01codigo.getNavigationController().setIdValue(null)
                verificarEntidade(e);
            }
        })
    }

    private void verificarEntidade(ActionEvent e) {
        try {
            MNavigation nvgAbe01codigo = getComponente("nvgAbe01codigo");
            MNavigation nvgAbe01na = getComponente("nvgAbe01na");
            String codEntidade = nvgAbe01codigo.getValue();
            String nomeEntidade = nvgAbe01na.getValue();
            Long idEntidade = buscarIdEntidade(codEntidade);
            MRadioButton rdoDocumento = getComponente("rdoDocumento");

            String msgEnt = buscarMensagemEntidade(idEntidade);

            // Exibe caixa de dialogo na tela com a mensagem do cadastro da entidade
            if (msgEnt != null && msgEnt != "") exibirTelaDeAtencaoComMensagemEntidade(msgEnt);

            if (nomeEntidade != null && nomeEntidade.toUpperCase() != "CONSUMIDOR" && rdoDocumento.isSelected()) {
                // Busca os titulos vencidos da entidade
                buscarTitulosVencidosEntidade(idEntidade, e);
            } else {
                if (actionEventOriginal != null && actionEventOriginal.size() > 0) {
                    for (evento in actionEventOriginal) {
                        evento.actionPerformed(e) // Executa os eventos originais novamente
                    }
                }
            }
        } catch (Exception err) {
            interromper(err.getMessage())
        }
    }

    private String buscarMensagemEntidade(Long idEntidade) {
        String sql = "SELECT abe02obsUsoInt FROM abe02 WHERE abe02ent = " + idEntidade.toString();

        TableMap tmEntidade = executarConsulta(sql)[0]

        return tmEntidade.getString("abe02obsUsoInt");
    }

    private void exibirTelaDeAtencaoComMensagemEntidade(String msg) {

        String caminhoImagem = "H:/Sam4/imagens/atencao.png";

        // Caixa de Mensagem
        JDialog dialog = new JDialog((Frame) null, "Mensagem de Alerta", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setResizable(false);

        // Painel principal
        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setBorder(new EmptyBorder(8, 8, 8, 8)); // margem externa
        dialog.setContentPane(content);

        // --- Top: título centralizado em vermelho
        JLabel titulo = new JLabel("*** A T E N Ç Ã O ***", SwingConstants.CENTER);
        titulo.setFont(new Font("Arial", Font.BOLD, 25));
        titulo.setForeground(Color.RED);

        // Painel para manter título com pequena margem
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(titulo, BorderLayout.CENTER);
        content.add(topPanel, BorderLayout.NORTH);

        // --- Centro: área de texto não-editável dentro de um JScrollPane
        JTextArea txt = new JTextArea();
        txt.setText(msg); // texto inicial
        txt.setEditable(false);
        txt.setLineWrap(true);
        txt.setWrapStyleWord(true);
        txt.setFont(new Font("Arial", Font.PLAIN, 14));
        txt.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        JScrollPane scroll = new JScrollPane(txt);
        scroll.setPreferredSize(new Dimension(620, 260));
        content.add(scroll, BorderLayout.CENTER);

        // --- Sul: imagem + botão OK (imagem acima do botão)
        JPanel south = new JPanel();
        south.setLayout(new BoxLayout(south, BoxLayout.Y_AXIS));
        south.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Botão OK centralizado
        JButton btnOk = new JButton("OK");
        btnOk.setFont(new Font("Arial", Font.BOLD, 16));
        btnOk.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnOk.setPreferredSize(new Dimension(120, 28));
        btnOk.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.dispose();
            }
        });

        south.add(btnOk);

        content.add(south, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setLocationRelativeTo(null); // centraliza na tela
        dialog.setVisible(true);
    }

    private void buscarTitulosVencidosEntidade(Long idEntidade, ActionEvent e) {
        try {
            TableMap body = new TableMap()
            body.put("abe01id", idEntidade)
            WorkerRequest.create(panel.getWindow())
                    .initialText("Buscando Limite de Crédito")
                    .dialogVisible(false)
                    .controllerEndPoint("servlet")
                    .methodEndPoint("run")
                    .param("name", "Silcon.servlet.Buscar_Titulos_Vencidos_Entidade")
                    .header("ignore-body-decrypt", "true")
                    .parseBody(body)
                    .success((response) -> {
                        Boolean contemTituloVencido = response.parseResponse(new TypeReference<Boolean>() {});
                        if (contemTituloVencido && !exibirQuestao("Constam títulos vencidos para esse cliente, necessário consultar financeiro. Deseja continuar?")) {
                            throw new ValidacaoException("Operação Cancelada.")
                        } else {
                            verificarLimiteDeCredito(idEntidade, e);
                        }
                    })
                    .post();

        } catch (Exception err) {
            throw new ValidacaoException(err.getMessage());
        }
    }

    private void verificarLimiteDeCredito(Long idEntidade, ActionEvent e) {
        TableMap body = new TableMap()
        body.put("abe01id", idEntidade)
        WorkerRequest.create(panel.getWindow())
                .initialText("Verificando Limite de Crédito")
                .dialogVisible(false)
                .controllerEndPoint("servlet")
                .methodEndPoint("run")
                .param("name", "Silcon.servlet.Verificar_Limite_Credito_Entidade")
                .header("ignore-body-decrypt", "true")
                .parseBody(body)
                .success((response) -> {
                    Boolean limiteCreditoExcedido = response.parseResponse(new TypeReference<Boolean>() {});
                    if (limiteCreditoExcedido && !exibirQuestao("Limite de crédito ultrapassado, necessario consultar o financeiro. Deseja continuar?")) {
                        throw new ValidacaoException("Operação Cancelada.")
                    } else {
                        if (actionEventOriginal != null && actionEventOriginal.size() > 0) {
                            for (evento in actionEventOriginal) {
                                evento.actionPerformed(e) // Executa os eventos originais novamente
                            }
                        }
                    }
                })
                .post();
    }

    private Long buscarIdEntidade(String codEntidade) {
        String sql = "SELECT abe01id FROM abe01 WHERE abe01codigo = '" + codEntidade + "' AND abe01gc = 1075797 ";
        TableMap tmEntidade = executarConsulta(sql)[0];
        Long idEntidade = tmEntidade.getLong("abe01id");

        return idEntidade;
    }
}