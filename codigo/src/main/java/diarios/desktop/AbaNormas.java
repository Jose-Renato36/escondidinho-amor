package diarios.desktop;

import diarios.Aplicacao;
import diarios.modelo.Relacao;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** O grafo de normas em forma de tabela: quem altera, revoga, prorroga ou cita quem. */
public class AbaNormas implements Aba {

    private static final String TODAS = "(todas)";

    private final Aplicacao app;
    private final Tab tab = new Tab("Normas");
    private final TableView<Relacao> tabela = new TableView<>();
    private final ComboBox<String> acao = new ComboBox<>();
    private final Label contagem = new Label();
    private FilteredList<Relacao> filtradas = new FilteredList<>(FXCollections.observableArrayList());

    public AbaNormas(Aplicacao app) {
        this.app = app;
        tab.setClosable(false);

        acao.setItems(FXCollections.observableArrayList(TODAS, Relacao.ALTERA, Relacao.REVOGA,
                Relacao.PRORROGA, Relacao.CITA));
        acao.setValue(TODAS);
        acao.valueProperty().addListener((o, a, b) -> filtrar());
        HBox barra = new HBox(8, new Label("ação"), acao, contagem);
        barra.setPadding(new Insets(10));
        barra.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        tabela.getColumns().addAll(java.util.List.of(
                coluna("Ato de origem", 380, Relacao::getOrigem),
                coluna("Ação", 100, Relacao::getAcao),
                coluna("Norma atingida", 260, Relacao::getAlvo)));
        VBox.setVgrow(tabela, Priority.ALWAYS);
        tab.setContent(new VBox(barra, tabela));
    }

    private static TableColumn<Relacao, String> coluna(String nome, double largura,
            java.util.function.Function<Relacao, String> leitor) {
        TableColumn<Relacao, String> c = new TableColumn<>(nome);
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
        filtradas = new FilteredList<>(FXCollections.observableArrayList(app.getGrafo().getArestas()));
        tabela.setItems(filtradas);
        filtrar();
    }

    private void filtrar() {
        String escolhida = acao.getValue();
        filtradas.setPredicate(r -> escolhida == null || TODAS.equals(escolhida)
                || escolhida.equals(r.getAcao()));
        contagem.setText(filtradas.size() + " relações");
    }
}
