package diarios.desktop;

import diarios.modelo.Ato;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/** Cores e pequenos componentes visuais repetidos nas abas. */
final class Estilo {

    static final String VERDE = "#0ca30c";
    static final String AMARELO = "#c98a00";
    static final String VERMELHO = "#d03b3b";
    static final String AZUL = "#2a78d6";

    static final String CSS = """
            .root { -fx-font-family: "Segoe UI", "Helvetica Neue", sans-serif; -fx-font-size: 13px; }
            .titulo { -fx-font-size: 22px; -fx-font-weight: bold; }
            .sub { -fx-text-fill: #6b6a66; }
            .tile { -fx-background-color: #f4f4f1; -fx-background-radius: 8; -fx-padding: 12 16; }
            .tile-numero { -fx-font-size: 24px; -fx-font-weight: bold; }
            .tile-nome { -fx-text-fill: #52514e; }
            .cartao { -fx-background-color: white; -fx-border-color: #e1e0d9; -fx-border-radius: 8;
                      -fx-background-radius: 8; -fx-padding: 12 14; }
            .cartao-titulo { -fx-font-weight: bold; -fx-font-size: 14px; }
            .chart-bar { -fx-bar-fill: #2a78d6; }
            .barra-status { -fx-background-color: #f4f4f1; -fx-padding: 6 12; }
            """;

    private Estilo() {
    }

    static String corDaDecisao(String decisao) {
        if (Ato.ACEITO.equals(decisao)) {
            return VERDE;
        }
        if (Ato.DUVIDOSO.equals(decisao)) {
            return AMARELO;
        }
        return VERMELHO;
    }

    static VBox tile(String numero, String nome) {
        Label n = new Label(numero);
        n.getStyleClass().add("tile-numero");
        Label t = new Label(nome);
        t.getStyleClass().add("tile-nome");
        VBox caixa = new VBox(2, n, t);
        caixa.getStyleClass().add("tile");
        caixa.setPadding(new Insets(12, 16, 12, 16));
        return caixa;
    }

    static String cortar(String texto, int max) {
        if (texto == null) {
            return "";
        }
        return texto.length() <= max ? texto : texto.substring(0, max - 1) + "…";
    }
}
