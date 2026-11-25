package diarios.desktop;

import diarios.Aplicacao;
import diarios.modelo.Ato;
import diarios.modelo.Diario;
import diarios.modelo.Fato;
import java.util.ArrayList;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Todos os atos, com filtro por texto, decisao e municipio, e o detalhe do ato selecionado. */
public class AbaAtos implements Aba {

    private static final String TODOS = "(todos)";

    private final Aplicacao app;
    private final Tab tab = new Tab("Atos");
    private final ObservableList<Ato> todos = FXCollections.observableArrayList();
    private final FilteredList<Ato> filtrados = new FilteredList<>(todos, a -> true);
    private final TableView<Ato> tabela = new TableView<>();
    private final TextField busca = new TextField();
    private final ComboBox<String> decisao = new ComboBox<>();
    private final ComboBox<String> municipio = new ComboBox<>();
    private final Label contagem = new Label();
    private final TextArea detalhe = new TextArea();

    public AbaAtos(Aplicacao app) {
        this.app = app;
        tab.setClosable(false);

        busca.setPromptText("filtrar por palavra no título ou no texto");
        HBox.setHgrow(busca, Priority.ALWAYS);
        decisao.setItems(FXCollections.observableArrayList(TODOS, Ato.ACEITO, Ato.DUVIDOSO, Ato.REJEITADO));
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

        detalhe.setEditable(false);
        detalhe.setWrapText(true);
        detalhe.setPromptText("Selecione um ato para ver o que ele faz e a prova de cada fato.");

        SplitPane divisao = new SplitPane(tabela, detalhe);
        divisao.setOrientation(javafx.geometry.Orientation.VERTICAL);
        divisao.setDividerPositions(0.55);
        VBox.setVgrow(divisao, Priority.ALWAYS);
        tab.setContent(new VBox(barra, divisao));
    }

    private void montarColunas() {
        TableColumn<Ato, String> cMun = coluna("Município", 120, Ato::getMunicipio);
        TableColumn<Ato, String> cData = coluna("Data", 90, Ato::getDataDiario);
        TableColumn<Ato, String> cEsp = coluna("Espécie", 120, Ato::getEspecie);
        TableColumn<Ato, String> cNum = coluna("Nº", 70, Ato::getNumero);
        TableColumn<Ato, String> cDec = coluna("Decisão", 100, Ato::getDecisao);
        cDec.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean vazio) {
                super.updateItem(item, vazio);
                setText(vazio ? null : item);
                setStyle(vazio ? "" : "-fx-text-fill: " + Estilo.corDaDecisao(item) + "; -fx-font-weight: bold;");
            }
        });
        TableColumn<Ato, String> cTit = coluna("Título", 520, a -> Estilo.cortar(a.getTitulo(), 140));
        tabela.getColumns().addAll(java.util.List.of(cMun, cData, cEsp, cNum, cDec, cTit));
    }

    private static TableColumn<Ato, String> coluna(String nome, double largura,
                                                   java.util.function.Function<Ato, String> leitor) {
        TableColumn<Ato, String> c = new TableColumn<>(nome);
        c.setPrefWidth(largura);
        c.setCellValueFactory(v -> new javafx.beans.property.SimpleStringProperty(leitor.apply(v.getValue())));
        return c;
    }

    @Override
    public Tab getTab() {
        return tab;
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
                (dec == null || TODOS.equals(dec) || dec.equals(a.getDecisao()))
                && (mun == null || TODOS.equals(mun) || mun.equals(a.getMunicipio()))
                && (palavra.isEmpty()
                    || a.getTitulo().toLowerCase().contains(palavra)
                    || a.getTexto().toLowerCase().contains(palavra)));
        contagem.setText(filtrados.size() + " de " + todos.size() + " atos");
    }

    private void mostrar(Ato a) {
        if (a == null) {
            detalhe.clear();
            return;
        }
        StringBuilder s = new StringBuilder();
        s.append(a.getTitulo()).append("\n\n");
        s.append("O QUE ESTE ATO FAZ\n");
        s.append(app.leitor().ler(a, a.getDataDiario())).append("\n\n");
        s.append("DECISÃO: ").append(a.getDecisao()).append(" — ").append(a.getMotivo()).append("\n");
        s.append("IDENTIDADE: ").append(a.getIdentidade()).append("\n\n");
        s.append("FATOS E A PROVA DE CADA UM\n");
        for (Fato f : a.getFatos()) {
            s.append("• ").append(f.getTipo()).append("  ").append(f.getValor())
                    .append("  [").append(f.getInicio()).append("..").append(f.getFim()).append("]\n");
        }
        s.append("\nTEXTO DO ATO\n").append(a.getTexto());
        detalhe.setText(s.toString());
        detalhe.positionCaret(0);
    }
}
