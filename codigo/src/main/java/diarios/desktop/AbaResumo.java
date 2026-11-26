package diarios.desktop;

import diarios.Aplicacao;
import diarios.analise.Cobertura;
import diarios.modelo.Achado;
import diarios.modelo.Contrato;
import javafx.scene.Node;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Os numeros do periodo e a interpretacao em frases (as mesmas do painel web). */
public class AbaResumo implements Aba {

    private final Aplicacao app;
    private static final String TITULO = "Resumo";

    private Node conteudo;
    private final HBox tiles = new HBox(12);
    private final Label resumo = new Label();
    private final VBox achados = new VBox(10);

    public AbaResumo(Aplicacao app) {
        this.app = app;

        Label sub = new Label("Leitura, separação em atos e análise: tudo calculado por regra, sem IA.");
        sub.getStyleClass().add("sub");

        resumo.setWrapText(true);
        resumo.setMaxWidth(900);

        VBox caixa = new VBox(14, sub, tiles, resumo, achados);
        caixa.setPadding(new Insets(18));
        ScrollPane rolagem = new ScrollPane(caixa);
        rolagem.setFitToWidth(true);
        conteudo = rolagem;
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
        Cobertura c = app.getCobertura();
        double total = 0;
        for (Contrato k : app.getPainel().getContratos()) {
            total += k.getValor();
        }

        tiles.getChildren().setAll(
                Estilo.tile(String.valueOf(c.getTotalAtos()), "atos separados"),
                Estilo.tile(String.valueOf(app.getPainel().getContratos().size()),
                        "contratações · " + Contrato.formatarReal(total)),
                Estilo.tile(String.valueOf(app.getGrafo().getArestas().size()), "relações entre normas"),
                Estilo.tile(c.getUtilizaveis() + " de " + c.getLidos(), "diários utilizáveis"),
                Estilo.tile(String.valueOf(c.getRecusados()), "diário(s) recusado(s)"));
        for (javafx.scene.Node t : tiles.getChildren()) {
            HBox.setHgrow(t, Priority.ALWAYS);
            ((javafx.scene.layout.Region) t).setMaxWidth(Double.MAX_VALUE);
        }

        resumo.setText(app.getInterpretacao().getResumo());

        achados.getChildren().clear();
        for (Achado a : app.getInterpretacao().getAchados()) {
            Label t = new Label(a.getTitulo());
            t.getStyleClass().add("cartao-titulo");
            Label frase = new Label(a.getFrase());
            frase.setWrapText(true);
            Label prova = new Label(a.getEvidencia());
            prova.getStyleClass().add("sub");
            prova.setWrapText(true);
            VBox cartao = new VBox(4, t, frase, prova);
            cartao.getStyleClass().add("cartao");
            cartao.setStyle("-fx-border-width: 0 0 0 4; -fx-border-color: " + corDoNivel(a.getNivel()) + ";");
            cartao.setMaxWidth(900);
            achados.getChildren().add(cartao);
        }
    }

    private static String corDoNivel(String nivel) {
        if (Achado.ATENCAO.equals(nivel)) {
            return Estilo.AMARELO;
        }
        if (Achado.LIMITE.equals(nivel)) {
            return Estilo.VERMELHO;
        }
        return Estilo.VERDE;
    }
}
