package diarios.desktop;

import diarios.Aplicacao;
import diarios.modelo.Acao;
import diarios.modelo.Relacao;
import javafx.scene.Node;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** O grafo de normas em forma de tabela: quem altera, revoga, prorroga ou cita quem. */
public class AbaNormas implements Aba {

    private static final String TODAS = "(todas)";

    private final Aplicacao app;
    private static final String TITULO = "Normas";

    private Node conteudo;
    private final TableView<Relacao> tabela = new TableView<>();
    private final ComboBox<String> acao = new ComboBox<>();
    private final Label contagem = new Label();
    private FilteredList<Relacao> filtradas = new FilteredList<>(FXCollections.observableArrayList());

    public AbaNormas(Aplicacao app) {
        this.app = app;

        acao.setItems(FXCollections.observableArrayList(TODAS, Acao.ALTERA.name(), Acao.REVOGA.name(),
                Acao.PRORROGA.name(), Acao.CITA.name()));
        acao.setValue(TODAS);
        acao.valueProperty().addListener((o, a, b) -> filtrar());
        HBox barra = new HBox(8, new Label("ação"), acao, contagem);
        barra.setPadding(new Insets(10));
        barra.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        tabela.getColumns().addAll(java.util.List.of(
                Tabelas.texto("Ato de origem", 380, Relacao::getOrigem),
                Tabelas.<Relacao>texto("Ação", 100, r -> r.getAcao().name()),
                Tabelas.texto("Norma atingida", 260, Relacao::getAlvo)));
        VBox.setVgrow(tabela, Priority.ALWAYS);
        conteudo = new VBox(barra, tabela);
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
        filtradas = new FilteredList<>(FXCollections.observableArrayList(app.getGrafo().getArestas()));
        tabela.setItems(filtradas);
        filtrar();
    }

    private void filtrar() {
        String escolhida = acao.getValue();
        filtradas.setPredicate(r -> escolhida == null || TODAS.equals(escolhida)
                || escolhida.equals(r.getAcao().name()));
        contagem.setText(filtradas.size() + " relações");
    }
}
