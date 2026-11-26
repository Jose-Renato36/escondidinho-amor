package diarios.desktop;

import diarios.Aplicacao;
import diarios.analise.Fornecedor;
import diarios.analise.SinaisDaEmpresa;
import diarios.empresas.Cnpj;
import diarios.empresas.Empresa;
import diarios.empresas.ServicoEmpresas;
import diarios.modelo.Achado;
import diarios.modelo.Contrato;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Optional;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Fornecedores a esquerda; a ficha da empresa (Receita Federal, via BrasilAPI) e os sinais
 * de atencao a direita. A consulta so acontece quando o usuario clica, e a resposta fica
 * guardada no banco: da segunda vez nao usa a rede.
 */
public class AbaEmpresas implements Aba {

    private static final String TITULO = "Empresas";

    private final Aplicacao app;
    private final ServicoEmpresas servico;
    private final TableView<Linha> tabela = new TableView<>();
    private final Button consultarTodas = new Button("Consultar todas");
    private final ProgressBar progresso = new ProgressBar(0);
    private final Label andamento = new Label();
    private final VBox ficha = new VBox(14);
    private final Node conteudo;

    public AbaEmpresas(Aplicacao app, ServicoEmpresas servico) {
        this.app = app;
        this.servico = servico;

        montarTabela();
        ficha.setPadding(new Insets(20));
        ScrollPane rolagem = new ScrollPane(ficha);
        rolagem.setFitToWidth(true);

        consultarTodas.getStyleClass().add("accent");
        consultarTodas.setOnAction(e -> consultarTodasAsEmpresas());
        progresso.setPrefWidth(120);
        progresso.setVisible(false);
        HBox barra = new HBox(10, consultarTodas, progresso, andamento);
        barra.setAlignment(Pos.CENTER_LEFT);
        barra.setPadding(new Insets(10, 12, 10, 12));
        VBox.setVgrow(tabela, Priority.ALWAYS);
        VBox esquerda = new VBox(barra, tabela);

        SplitPane divisao = new SplitPane(esquerda, rolagem);
        divisao.setDividerPositions(0.62);
        conteudo = divisao;

        tabela.getSelectionModel().selectedItemProperty().addListener((o, antes, agora) ->
                mostrar(agora == null ? null : agora.fornecedor));
        mostrar(null);
    }

    private void montarTabela() {
        TableColumn<Linha, String> nome = Tabelas.<Linha>texto("Fornecedor", 170,
                l -> Estilo.cortar(l.fornecedor.getNome(), 38));
        TableColumn<Linha, String> cnpj = Tabelas.<Linha>texto("CNPJ", 160,
                l -> Cnpj.formatar(l.fornecedor.getCnpj()));
        cnpj.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean vazio) {
                super.updateItem(item, vazio);
                setText(vazio ? null : item);
                boolean invalido = !vazio && !Cnpj.valido(item);
                setStyle(invalido ? "-fx-text-fill: -color-danger-fg;" : "");
                setTooltip(invalido ? new javafx.scene.control.Tooltip("Digito verificador invalido") : null);
            }
        });
        TableColumn<Linha, Double> total = new TableColumn<>("Total contratado");
        total.setPrefWidth(125);
        total.setCellValueFactory(v -> new javafx.beans.property.SimpleObjectProperty<>(v.getValue().fornecedor.getTotal()));
        total.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean vazio) {
                super.updateItem(item, vazio);
                setText(vazio || item == null ? null : Contrato.formatarReal(item));
                setAlignment(Pos.CENTER_RIGHT);
            }
        });
        TableColumn<Linha, Integer> vezes = new TableColumn<>("Contratos");
        vezes.setPrefWidth(80);
        vezes.setCellValueFactory(v -> new javafx.beans.property.SimpleObjectProperty<>(v.getValue().fornecedor.getContratos()));
        TableColumn<Linha, Integer> sinais = new TableColumn<>("Sinais");
        sinais.setPrefWidth(65);
        sinais.setCellValueFactory(v -> new javafx.beans.property.SimpleObjectProperty<>(v.getValue().sinais));
        sinais.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean vazio) {
                super.updateItem(item, vazio);
                setText(vazio || item == null ? null : item == 0 ? "–" : String.valueOf(item));
                setAlignment(Pos.CENTER);
                setStyle(!vazio && item != null && item > 0 ? "-fx-text-fill: -color-warning-fg; -fx-font-weight: bold;" : "");
            }
        });
        tabela.getColumns().addAll(java.util.List.of(nome, cnpj, total, vezes, sinais));
    }

    @Override
    public String getTitulo() {
        return TITULO;
    }

    @Override
    public Node getConteudo() {
        return conteudo;
    }

    @Override
    public void atualizar() {
        Fornecedor selecionado = tabela.getSelectionModel().getSelectedItem() == null
                ? null : tabela.getSelectionModel().getSelectedItem().fornecedor;
        recarregarLinhas();
        if (selecionado != null) {
            for (Linha l : tabela.getItems()) {
                if (l.fornecedor.getCnpj().equals(selecionado.getCnpj())) {
                    tabela.getSelectionModel().select(l);
                }
            }
        }
    }

    /** Refaz a tabela; a coluna Sinais usa a ficha em cache quando ela existe. */
    private void recarregarLinhas() {
        ArrayList<Linha> linhas = new ArrayList<>();
        SinaisDaEmpresa regras = new SinaisDaEmpresa();
        for (Fornecedor f : app.getPainel().ranking()) {
            linhas.add(new Linha(f, regras.avaliar(f, fichaEmCache(f)).size()));
        }
        tabela.setItems(FXCollections.observableArrayList(linhas));
        int semFicha = 0;
        for (Linha l : linhas) {
            if (Cnpj.valido(l.fornecedor.getCnpj()) && fichaEmCache(l.fornecedor) == null) {
                semFicha++;
            }
        }
        consultarTodas.setText("Consultar todas (" + semFicha + ")");
        consultarTodas.setDisable(semFicha == 0);
    }

    private Empresa fichaEmCache(Fornecedor f) {
        try {
            return servico.doCache(f.getCnpj()).orElse(null);
        } catch (SQLException e) {
            return null;
        }
    }

    /** Consulta uma por vez (o cliente ja espera 1 s entre chamadas) e mostra o andamento. */
    private void consultarTodasAsEmpresas() {
        ArrayList<Fornecedor> alvo = new ArrayList<>();
        for (Linha l : tabela.getItems()) {
            if (Cnpj.valido(l.fornecedor.getCnpj()) && fichaEmCache(l.fornecedor) == null) {
                alvo.add(l.fornecedor);
            }
        }
        consultarTodas.setDisable(true);
        progresso.setVisible(true);
        Task<Integer> tarefa = new Task<>() {
            @Override
            protected Integer call() {
                int falhas = 0;
                for (int i = 0; i < alvo.size(); i++) {
                    updateMessage("consultando " + (i + 1) + " de " + alvo.size() + "…");
                    updateProgress(i, alvo.size());
                    try {
                        servico.buscar(alvo.get(i).getCnpj());
                    } catch (Exception e) {
                        falhas++;
                    }
                }
                updateProgress(alvo.size(), alvo.size());
                return falhas;
            }
        };
        progresso.progressProperty().bind(tarefa.progressProperty());
        andamento.textProperty().bind(tarefa.messageProperty());
        tarefa.setOnSucceeded(e -> {
            progresso.progressProperty().unbind();
            andamento.textProperty().unbind();
            progresso.setVisible(false);
            andamento.setText(tarefa.getValue() == 0 ? "concluído"
                    : "concluído, " + tarefa.getValue() + " não consultada(s) (ver a ficha de cada uma)");
            atualizar();
        });
        tarefa.setOnFailed(e -> {
            progresso.progressProperty().unbind();
            andamento.textProperty().unbind();
            progresso.setVisible(false);
            andamento.setText("erro: " + tarefa.getException().getMessage());
            atualizar();
        });
        Thread t = new Thread(tarefa, "consulta-todas");
        t.setDaemon(true);
        t.start();
    }

    /** Uma linha da tabela: o fornecedor e quantos sinais de atencao ele tem hoje. */
    private static final class Linha {
        final Fornecedor fornecedor;
        final int sinais;

        Linha(Fornecedor fornecedor, int sinais) {
            this.fornecedor = fornecedor;
            this.sinais = sinais;
        }
    }

    // ---------- ficha

    private void mostrar(Fornecedor f) {
        ficha.getChildren().clear();
        if (f == null) {
            Label dica = new Label("Escolha um fornecedor para ver a ficha da empresa e os sinais de atenção.");
            dica.getStyleClass().add("sub");
            dica.setWrapText(true);
            ficha.getChildren().add(dica);
            return;
        }
        Empresa guardada = null;
        try {
            Optional<Empresa> cache = servico.doCache(f.getCnpj());
            guardada = cache.orElse(null);
        } catch (SQLException e) {
            // sem cache, a ficha aparece como "ainda nao consultada"
        }
        desenhar(f, guardada, "");
    }

    private void desenhar(Fornecedor f, Empresa empresa, String aviso) {
        ficha.getChildren().clear();

        Label nome = new Label(empresa != null && !empresa.getRazaoSocial().isEmpty()
                ? empresa.getRazaoSocial() : f.getNome());
        nome.getStyleClass().add("title-3");
        nome.setWrapText(true);
        Label cnpj = new Label(Cnpj.formatar(f.getCnpj()));
        cnpj.getStyleClass().add("sub");

        FlowPane chips = new FlowPane(8, 8);
        chips.getChildren().add(Cnpj.valido(f.getCnpj())
                ? Estilo.chip("CNPJ válido", "ok") : Estilo.chip("CNPJ inválido", "ruim"));
        if (empresa != null && !empresa.getSituacao().isEmpty()) {
            chips.getChildren().add(Estilo.chip(empresa.getSituacao(), empresa.isAtiva() ? "ok" : "ruim"));
        } else {
            chips.getChildren().add(Estilo.chip("não consultada", "neutro"));
        }

        ficha.getChildren().addAll(nome, cnpj, chips, botaoConsultar(f), resumoDoPeriodo(f));
        if (!aviso.isEmpty()) {
            ficha.getChildren().add(cartaoDeAviso(aviso));
        }
        if (empresa != null) {
            ficha.getChildren().add(dadosDaReceita(empresa));
        }

        Label tSinais = new Label("Sinais de atenção");
        tSinais.getStyleClass().add("cartao-titulo");
        ficha.getChildren().add(tSinais);
        java.util.List<Achado> sinais = new SinaisDaEmpresa().avaliar(f, empresa);
        if (sinais.isEmpty()) {
            Label nenhum = new Label("Nenhum sinal pelas regras atuais"
                    + (empresa == null ? " (consulte a empresa para aplicar as regras que dependem da Receita)." : "."));
            nenhum.getStyleClass().add("sub");
            nenhum.setWrapText(true);
            ficha.getChildren().add(nenhum);
        }
        for (Achado a : sinais) {
            ficha.getChildren().add(cartaoDeSinal(a));
        }
        Label nota = new Label("Sinal de atenção não é acusação: é um lugar onde vale olhar. "
                + "Cada sinal diz o que ele não prova.");
        nota.getStyleClass().add("sub");
        nota.setWrapText(true);
        ficha.getChildren().add(nota);
    }

    private boolean selecionado(Fornecedor f) {
        Linha l = tabela.getSelectionModel().getSelectedItem();
        return l != null && l.fornecedor == f;
    }

    private Node botaoConsultar(Fornecedor f) {
        Button b = new Button(Cnpj.valido(f.getCnpj()) ? "Consultar na Receita (BrasilAPI)"
                : "CNPJ inválido — não consultado");
        b.getStyleClass().add("accent");
        b.setDisable(!Cnpj.valido(f.getCnpj()));
        b.setOnAction(e -> consultar(f, b));
        return b;
    }

    private void consultar(Fornecedor f, Button botao) {
        botao.setDisable(true);
        botao.setText("Consultando…");
        Task<Empresa> tarefa = new Task<>() {
            @Override
            protected Empresa call() throws Exception {
                return servico.buscar(f.getCnpj());
            }
        };
        tarefa.setOnSucceeded(e -> {
            if (selecionado(f)) {
                desenhar(f, tarefa.getValue(), "");
            }
            atualizar();
        });
        tarefa.setOnFailed(e -> {
            if (selecionado(f)) {
                desenhar(f, null, "Não foi possível consultar: " + tarefa.getException().getMessage());
            }
        });
        Thread t = new Thread(tarefa, "consulta-cnpj");
        t.setDaemon(true);
        t.start();
    }

    private Node resumoDoPeriodo(Fornecedor f) {
        Label l = new Label(f.getContratos() + (f.getContratos() == 1 ? " contrato" : " contratos")
                + " no período, somando " + f.getTotalFormatado()
                + ". Aparece desde " + f.getPrimeiraData() + ".");
        l.setWrapText(true);
        return l;
    }

    private Node dadosDaReceita(Empresa e) {
        GridPane grade = new GridPane();
        grade.setHgap(16);
        grade.setVgap(6);
        int linha = 0;
        linha = linhaDaFicha(grade, linha, "Nome fantasia", e.getNomeFantasia());
        linha = linhaDaFicha(grade, linha, "Aberta em", abertura(e));
        linha = linhaDaFicha(grade, linha, "Atividade", e.getAtividade());
        linha = linhaDaFicha(grade, linha, "Porte", e.getPorte());
        linha = linhaDaFicha(grade, linha, "Município", (e.getMunicipio() + " " + e.getUf()).trim());
        linhaDaFicha(grade, linha, "Capital social", e.getCapitalSocial() > 0
                ? Contrato.formatarReal(e.getCapitalSocial()) : "");

        Label titulo = new Label("Dados da Receita Federal");
        titulo.getStyleClass().add("cartao-titulo");
        VBox cartao = new VBox(10, titulo, grade);
        cartao.getStyleClass().add("cartao");
        return cartao;
    }

    private static String abertura(Empresa e) {
        long meses = e.mesesDeAtividade(LocalDate.now());
        if (meses < 0) {
            return e.getInicioAtividade();
        }
        String data = LocalDate.parse(e.getInicioAtividade()).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        long anos = meses / 12;
        long resto = meses % 12;
        return data + "  (" + anos + (anos == 1 ? " ano" : " anos") + " e " + resto
                + (resto == 1 ? " mês" : " meses") + ")";
    }

    private static int linhaDaFicha(GridPane grade, int linha, String rotulo, String valor) {
        if (valor == null || valor.isBlank()) {
            return linha;
        }
        Label r = new Label(rotulo);
        r.getStyleClass().add("ficha-rotulo");
        Label v = new Label(valor);
        v.getStyleClass().add("ficha-valor");
        v.setWrapText(true);
        grade.add(r, 0, linha);
        grade.add(v, 1, linha);
        GridPane.setHgrow(v, Priority.ALWAYS);
        return linha + 1;
    }

    private static Node cartaoDeAviso(String texto) {
        Label l = new Label(texto);
        l.setWrapText(true);
        VBox cartao = new VBox(l);
        cartao.getStyleClass().addAll("cartao");
        cartao.setStyle("-fx-border-width: 0 0 0 4; -fx-border-color: -color-danger-emphasis;");
        return cartao;
    }

    private static Node cartaoDeSinal(Achado a) {
        Label t = new Label(a.getTitulo());
        t.getStyleClass().add("cartao-titulo");
        Label frase = new Label(a.getFrase());
        frase.setWrapText(true);
        Label prova = new Label(a.getEvidencia());
        prova.getStyleClass().add("sub");
        prova.setWrapText(true);
        VBox cartao = new VBox(4, t, frase, prova);
        cartao.getStyleClass().add("cartao");
        String cor = Achado.ATENCAO.equals(a.getNivel()) ? "-color-warning-emphasis" : "-color-success-emphasis";
        cartao.setStyle("-fx-border-width: 0 0 0 4; -fx-border-color: " + cor + ";");
        return cartao;
    }
}
