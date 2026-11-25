package diarios.desktop;

import diarios.persistencia.Banco;
import java.sql.SQLException;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.layout.VBox;

/** Graficos alimentados por consultas SQL no banco (GROUP BY), nao por lacos em Java. */
public class AbaGraficos implements Aba {

    private static final int FORNECEDORES_NO_GRAFICO = 8;

    private final Banco banco;
    private final Tab tab = new Tab("Gráficos");
    private final VBox conteudo = new VBox(18);
    private final Label aviso = new Label();

    public AbaGraficos(Banco banco) {
        this.banco = banco;
        tab.setClosable(false);
        conteudo.setPadding(new Insets(18));
        ScrollPane rolagem = new ScrollPane(conteudo);
        rolagem.setFitToWidth(true);
        tab.setContent(rolagem);
    }

    @Override
    public Tab getTab() {
        return tab;
    }

    @Override
    public void atualizar() {
        conteudo.getChildren().clear();
        try {
            conteudo.getChildren().addAll(
                    titulo("Decisão do portão sobre cada ato"), graficoDeDecisoes(),
                    titulo("Valor contratado por município (R$ mil)"), graficoPorMunicipio(),
                    titulo("Maiores fornecedores (R$ mil, somados por CNPJ)"), graficoDeFornecedores());
        } catch (SQLException e) {
            aviso.setText("Não consegui consultar o banco: " + e.getMessage());
            conteudo.getChildren().add(aviso);
        }
    }

    private static Label titulo(String texto) {
        Label l = new Label(texto);
        l.getStyleClass().add("cartao-titulo");
        return l;
    }

    private PieChart graficoDeDecisoes() throws SQLException {
        PieChart pizza = new PieChart();
        for (Object[] linha : banco.atosPorDecisao()) {
            PieChart.Data fatia = new PieChart.Data(linha[0] + " (" + linha[1] + ")",
                    ((Number) linha[1]).doubleValue());
            String cor = Estilo.corDaDecisao(String.valueOf(linha[0]));
            fatia.nodeProperty().addListener((o, antigo, no) -> no.setStyle("-fx-pie-color: " + cor + ";"));
            pizza.getData().add(fatia);
        }
        pizza.setLegendVisible(false);
        pizza.setLabelsVisible(true);
        pizza.setPrefHeight(280);
        pizza.setMinHeight(280);
        return pizza;
    }

    private BarChart<String, Number> graficoPorMunicipio() throws SQLException {
        CategoryAxis x = new CategoryAxis();
        NumberAxis y = new NumberAxis();
        BarChart<String, Number> barras = new BarChart<>(x, y);
        barras.setLegendVisible(false);
        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        for (Object[] linha : banco.valorPorMunicipio()) {
            serie.getData().add(new XYChart.Data<>(String.valueOf(linha[0]),
                    ((Number) linha[1]).doubleValue() / 1000.0));
        }
        barras.setData(FXCollections.observableArrayList(java.util.List.of(serie)));
        barras.setPrefHeight(300);
        barras.setMinHeight(300);
        return barras;
    }

    private BarChart<Number, String> graficoDeFornecedores() throws SQLException {
        NumberAxis x = new NumberAxis();
        CategoryAxis y = new CategoryAxis();
        BarChart<Number, String> barras = new BarChart<>(x, y);
        barras.setLegendVisible(false);
        XYChart.Series<Number, String> serie = new XYChart.Series<>();
        java.util.ArrayList<Object[]> linhas = banco.maioresFornecedores(FORNECEDORES_NO_GRAFICO);
        for (int i = linhas.size() - 1; i >= 0; i--) {
            Object[] linha = linhas.get(i);
            String nome = linha[1] == null ? String.valueOf(linha[0]) : String.valueOf(linha[1]);
            serie.getData().add(new XYChart.Data<>(((Number) linha[2]).doubleValue() / 1000.0,
                    Estilo.cortar(nome, 34)));
        }
        barras.setData(FXCollections.observableArrayList(java.util.List.of(serie)));
        barras.setPrefHeight(380);
        barras.setMinHeight(380);
        return barras;
    }
}
