package diarios.desktop;

import diarios.Aplicacao;
import diarios.modelo.Contrato;
import diarios.persistencia.ExportadorCsv;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import javafx.scene.Node;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

/** Tabela ordenavel de contratacoes, com exportacao para CSV. */
public class AbaContratos implements Aba {

    private final Aplicacao app;
    private static final String TITULO = "Contratos";

    private Node conteudo;
    private final TableView<Contrato> tabela = new TableView<>();
    private final Label total = new Label();

    public AbaContratos(Aplicacao app) {
        this.app = app;

        Button exportar = new Button("Exportar CSV…");
        exportar.setOnAction(e -> exportar());
        HBox barra = new HBox(12, exportar, total);
        barra.setPadding(new Insets(10));
        barra.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        tabela.getColumns().addAll(java.util.List.of(
                Tabelas.texto("Município", 125, Contrato::getMunicipio),
                Tabelas.texto("Data", 105, Contrato::getData),
                Tabelas.texto("Contratada", 260, Contrato::getContratada),
                Tabelas.texto("CNPJ", 140, Contrato::getCnpj),
                Tabelas.texto("Papel", 110, Contrato::getPapel),
                valor(),
                Tabelas.texto("Processo", 140, Contrato::getProcesso),
                Tabelas.texto("Ato", 240, Contrato::getIdentidadeAto)));
        VBox.setVgrow(tabela, Priority.ALWAYS);
        conteudo = new VBox(barra, tabela);
    }

    /** Coluna numerica: ordena pelo numero, mas mostra formatado em reais. */
    private static TableColumn<Contrato, Double> valor() {
        TableColumn<Contrato, Double> c = new TableColumn<>("Valor");
        c.setPrefWidth(130);
        c.setCellValueFactory(v -> new javafx.beans.property.SimpleObjectProperty<>(v.getValue().getValor()));
        c.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean vazio) {
                super.updateItem(item, vazio);
                setText(vazio || item == null ? null : Contrato.formatarReal(item));
                setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
            }
        });
        return c;
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
        ArrayList<Contrato> lista = app.getPainel().getContratos();
        tabela.setItems(FXCollections.observableArrayList(lista));
        total.setText(lista.size() + " contratações · " + Contrato.formatarReal(app.getPainel().total()));
    }

    private void exportar() {
        FileChooser escolha = new FileChooser();
        escolha.setTitle("Exportar contratos");
        escolha.setInitialFileName("contratos.csv");
        escolha.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        Window janela = conteudo.getScene().getWindow();
        File destino = escolha.showSaveDialog(janela);
        if (destino == null) {
            return;
        }
        ArrayList<String[]> linhas = new ArrayList<>();
        for (Contrato c : tabela.getItems()) {
            linhas.add(new String[] {c.getMunicipio(), c.getData(), c.getContratada(), c.getCnpj(),
                c.getPapel(), String.format(java.util.Locale.forLanguageTag("pt-BR"), "%.2f", c.getValor()),
                c.getProcesso(), c.getIdentidadeAto()});
        }
        try {
            ExportadorCsv.escrever(destino.toPath(), new String[] {"municipio", "data", "contratada",
                "cnpj", "papel", "valor", "processo", "ato"}, linhas);
            new Alert(Alert.AlertType.INFORMATION, linhas.size() + " contratos salvos em\n" + destino)
                    .showAndWait();
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "Não consegui salvar: " + e.getMessage()).showAndWait();
        }
    }
}
