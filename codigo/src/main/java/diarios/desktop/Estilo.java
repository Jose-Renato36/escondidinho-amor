package diarios.desktop;

import diarios.modelo.Decisao;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/** Cores e pequenos componentes visuais repetidos nas telas. */
final class Estilo {

    static final String VERDE = "#2da44e";
    static final String AMARELO = "#bf8700";
    static final String VERMELHO = "#cf222e";

    private static final String CSS = Estilo.class.getResource("estilo.css").toExternalForm();

    private Estilo() {
    }

    /** Caminho do arquivo estilo.css, para a cena carregar. */
    static String css() {
        return CSS;
    }

    static String corDaDecisao(Decisao decisao) {
        return switch (decisao) {
            case ACEITO -> VERDE;
            case DUVIDOSO -> AMARELO;
            case REJEITADO -> VERMELHO;
        };
    }

    /** Etiqueta arredondada: tipo e "ok", "aviso", "ruim" ou "neutro". */
    static Label chip(String texto, String tipo) {
        Label l = new Label(texto);
        l.getStyleClass().addAll("chip", "chip-" + tipo);
        return l;
    }

    static VBox tile(String numero, String nome) {
        Label n = new Label(numero);
        n.getStyleClass().add("tile-numero");
        Label t = new Label(nome);
        t.getStyleClass().add("tile-nome");
        VBox caixa = new VBox(2, n, t);
        caixa.getStyleClass().add("tile");
        caixa.setPadding(new Insets(14, 18, 14, 18));
        return caixa;
    }

    static String cortar(String texto, int max) {
        if (texto == null) {
            return "";
        }
        return texto.length() <= max ? texto : texto.substring(0, max - 1) + "…";
    }
}
