package diarios.desktop;

import diarios.Aplicacao;
import diarios.modelo.Ato;
import diarios.modelo.Decisao;
import diarios.modelo.Diario;
import diarios.modelo.Fato;
import diarios.analise.Assunto;
import java.util.ArrayList;
import javafx.scene.Node;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Todos os atos, com filtro por texto, decisao e municipio, e o detalhe do ato selecionado. */
public class AbaAtos implements Aba {

    private static final String TODOS = "(todos)";

    private final Aplicacao app;
    private static final String TITULO = "Atos";

    private Node conteudo;
    private final ObservableList<Ato> todos = FXCollections.observableArrayList();
    private final FilteredList<Ato> filtrados = new FilteredList<>(todos, a -> true);
    private final TableView<Ato> tabela = new TableView<>();
    private final TextField busca = new TextField();
    private final ComboBox<String> decisao = new ComboBox<>();
    private final ComboBox<String> municipio = new ComboBox<>();
    private final Label contagem = new Label();
    private final VBox detalhe = new VBox(12);

    public AbaAtos(Aplicacao app) {
        this.app = app;

        busca.setPromptText("filtrar por palavra no título ou no texto");
        HBox.setHgrow(busca, Priority.ALWAYS);
        decisao.setItems(FXCollections.observableArrayList(TODOS, Decisao.ACEITO.name(), Decisao.DUVIDOSO.name(),
                Decisao.REJEITADO.name()));
        decisao.setValue(TODOS);
        municipio.setValue(TODOS);
        busca.textProperty().addListener((o, a, b) -> aplicarFiltro());
        decisao.valueProperty().addListener((o, a, b) -> aplicarFiltro());
        municipio.valueProperty().addListener((o, a, b) -> aplicarFiltro());
        HBox barra = new HBox(8, busca, new Label("decisão"), decisao, new Label("município"),
                municipio, contagem);
        barra.setPadding(new Insets(10));

        montarColunas();
        SortedList<Ato> ordenados = new SortedList<>(filtrados);
        ordenados.comparatorProperty().bind(tabela.comparatorProperty());
        tabela.setItems(ordenados);
        tabela.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> mostrar(b));

        detalhe.setPadding(new Insets(16));
        ScrollPane rolagemDoDetalhe = new ScrollPane(detalhe);
        rolagemDoDetalhe.setFitToWidth(true);
        mostrar(null);

        SplitPane divisao = new SplitPane(tabela, rolagemDoDetalhe);
        divisao.setOrientation(javafx.geometry.Orientation.VERTICAL);
        divisao.setDividerPositions(0.42);
        VBox.setVgrow(divisao, Priority.ALWAYS);
        conteudo = new VBox(barra, divisao);
    }

    private void montarColunas() {
        TableColumn<Ato, String> cMun = Tabelas.texto("Município", 125, Ato::getMunicipio);
        TableColumn<Ato, String> cData = Tabelas.texto("Data", 105, Ato::getDataDiario);
        TableColumn<Ato, String> cEsp = Tabelas.texto("Espécie", 120, Ato::getEspecie);
        TableColumn<Ato, String> cNum = Tabelas.texto("Nº", 70, Ato::getNumero);
        TableColumn<Ato, String> cDec = Tabelas.<Ato>texto("Decisão", 100, a -> a.getDecisao().name());
        cDec.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean vazio) {
                super.updateItem(item, vazio);
                setText(vazio ? null : item);
                setStyle(vazio ? "" : "-fx-text-fill: " + Estilo.corDaDecisao(Decisao.valueOf(item)) + "; -fx-font-weight: bold;");
            }
        });
        TableColumn<Ato, String> cTit = Tabelas.<Ato>texto("Título", 520, a -> Estilo.cortar(a.getTitulo(), 140));
        tabela.getColumns().addAll(java.util.List.of(cMun, cData, cEsp, cNum, cDec, cTit));
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
        ArrayList<Ato> lista = new ArrayList<>();
        ArrayList<String> municipios = new ArrayList<>();
        municipios.add(TODOS);
        for (Diario d : app.getDiarios()) {
            lista.addAll(d.getAtos());
            if (!d.getAtos().isEmpty() && !municipios.contains(d.getMunicipio())) {
                municipios.add(d.getMunicipio());
            }
        }
        String escolhido = municipio.getValue();
        todos.setAll(lista);
        municipio.setItems(FXCollections.observableArrayList(municipios));
        municipio.setValue(municipios.contains(escolhido) ? escolhido : TODOS);
        aplicarFiltro();
    }

    private void aplicarFiltro() {
        String palavra = busca.getText() == null ? "" : busca.getText().trim().toLowerCase();
        String dec = decisao.getValue();
        String mun = municipio.getValue();
        filtrados.setPredicate(a ->
                (dec == null || TODOS.equals(dec) || dec.equals(a.getDecisao().name()))
                && (mun == null || TODOS.equals(mun) || mun.equals(a.getMunicipio()))
                && (palavra.isEmpty()
                    || a.getTitulo().toLowerCase().contains(palavra)
                    || a.getTexto().toLowerCase().contains(palavra)));
        contagem.setText(filtrados.size() + " de " + todos.size() + " atos");
    }

    private void mostrar(Ato a) {
        detalhe.getChildren().clear();
        if (a == null) {
            Label dica = new Label("Selecione um ato para ver o que ele faz e a prova de cada fato.");
            dica.getStyleClass().add("sub");
            detalhe.getChildren().add(dica);
            return;
        }
        Label titulo = new Label(a.getTitulo());
        titulo.getStyleClass().add("title-4");
        titulo.setWrapText(true);

        FlowPane chips = new FlowPane(8, 8);
        chips.getChildren().addAll(
                Estilo.chip(a.getDecisao().name(), tipoDoChip(a.getDecisao())),
                Estilo.chip(a.getEspecie(), "neutro"),
                Estilo.chip(Assunto.de(a), "neutro"));

        Diario dono = donoDe(a);
        detalhe.getChildren().addAll(titulo, chips,
                cartao("O que este ato faz", app.leitor().ler(a, a.getDataDiario())),
                cartao("Decisão do portão", a.getMotivo()),
                fatos(a, dono),
                textoDoAto(a));
    }

    private static String tipoDoChip(Decisao d) {
        return switch (d) {
            case ACEITO -> "ok";
            case DUVIDOSO -> "aviso";
            case REJEITADO -> "ruim";
        };
    }

    private Diario donoDe(Ato a) {
        for (Diario d : app.getDiarios()) {
            if (d.getAtos().contains(a)) {
                return d;
            }
        }
        return null;
    }

    private static Node cartao(String titulo, String texto) {
        Label t = new Label(titulo);
        t.getStyleClass().add("cartao-titulo");
        Label corpo = new Label(texto);
        corpo.setWrapText(true);
        VBox caixa = new VBox(6, t, corpo);
        caixa.getStyleClass().add("cartao");
        return caixa;
    }

    /** Cada fato com o trecho do diario de onde ele saiu: a prova de que nao foi inventado. */
    private static Node fatos(Ato a, Diario dono) {
        Label t = new Label("Fatos extraídos (" + a.getFatos().size() + ")");
        t.getStyleClass().add("cartao-titulo");
        VBox caixa = new VBox(8, t);
        caixa.getStyleClass().add("cartao");
        if (a.getFatos().isEmpty()) {
            Label vazio = new Label("Nenhum fato extraído.");
            vazio.getStyleClass().add("sub");
            caixa.getChildren().add(vazio);
        }
        for (Fato f : a.getFatos()) {
            Label valor = new Label(f.getValor());
            valor.getStyleClass().add("ficha-valor");
            HBox linha = new HBox(10, Estilo.chip(f.getTipo().name(), "neutro"), valor);
            linha.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            caixa.getChildren().add(linha);
            if (dono != null) {
                Label prova = new Label("…" + f.provar(dono.getTextoLimpo(), 60).replace('\n', ' ') + "…");
                prova.getStyleClass().add("sub");
                prova.setWrapText(true);
                caixa.getChildren().add(prova);
            }
        }
        return caixa;
    }

    private static Node textoDoAto(Ato a) {
        Label t = new Label("Texto do ato");
        t.getStyleClass().add("cartao-titulo");
        TextArea texto = new TextArea(a.getTexto());
        texto.setEditable(false);
        texto.setWrapText(true);
        texto.setPrefRowCount(10);
        VBox caixa = new VBox(6, t, texto);
        caixa.getStyleClass().add("cartao");
        return caixa;
    }
}
