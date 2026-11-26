package diarios.desktop;

import diarios.Aplicacao;
import diarios.coleta.EstadoFonte;
import diarios.coleta.Rodada;
import javafx.scene.Node;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Coleta e fontes. A coleta roda em outra thread (a da Aplicacao); aqui um Timeline
 * pergunta de tempo em tempo "ainda ocupado?" para atualizar a tela e avisar quando acabar.
 */
public class AbaColeta implements Aba {

    private final Aplicacao app;
    private final Runnable aoTerminar;
    private static final String TITULO = "Coleta e fontes";

    private Node conteudo;
    private final Spinner<Integer> dias = new Spinner<>(1, Aplicacao.DIAS_MAXIMO, 1);
    private final CheckBox refazer = new CheckBox("refazer todas");
    private final Button coletar = new Button("Coletar agora");
    private final Button descobrir = new Button("Descobrir municípios");
    private final Button ligar = new Button("Ligar / desligar selecionada");
    private final Button ligarTodas = new Button("Ligar todas");
    private final Button desligarTodas = new Button("Desligar todas");
    private final Label progresso = new Label("parado");
    private final ProgressBar barra = new ProgressBar(0);
    private final TableView<EstadoFonte> fontes = new TableView<>();
    private final ListView<String> historico = new ListView<>();
    private boolean estavaOcupado = false;

    public AbaColeta(Aplicacao app, Runnable aoTerminar) {
        this.app = app;
        this.aoTerminar = aoTerminar;

        dias.setEditable(true);
        dias.setPrefWidth(80);
        coletar.setOnAction(e -> {
            if (!app.coletarEmSegundoPlano(dias.getValue(), refazer.isSelected())) {
                progresso.setText("já existe uma coleta em andamento");
            }
        });
        descobrir.setOnAction(e -> app.descobrirEmSegundoPlano());
        ligar.setOnAction(e -> {
            EstadoFonte f = fontes.getSelectionModel().getSelectedItem();
            if (f != null) {
                app.alternarFonte(f.getMunicipio().getCodigo());
                atualizar();
            }
        });
        ligarTodas.setOnAction(e -> {
            app.ligarTodas(true);
            atualizar();
        });
        desligarTodas.setOnAction(e -> {
            app.ligarTodas(false);
            atualizar();
        });

        HBox acoes = new HBox(10, new Label("dias para trás"), dias, refazer, coletar, descobrir);
        acoes.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        barra.setPrefWidth(160);
        barra.setVisible(false);
        HBox andamento = new HBox(10, barra, progresso);
        andamento.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        HBox acoesFontes = new HBox(10, ligar, ligarTodas, desligarTodas);

        montarTabelaDeFontes();
        fontes.setPrefHeight(330);
        historico.setPrefHeight(150);

        Label tFontes = new Label("Fontes (somente as ligadas entram na coleta)");
        tFontes.getStyleClass().add("cartao-titulo");
        Label tHist = new Label("Histórico de rodadas");
        tHist.getStyleClass().add("cartao-titulo");

        VBox caixa = new VBox(10, acoes, andamento, tFontes, acoesFontes, fontes, tHist, historico);
        caixa.setPadding(new Insets(14));
        VBox.setVgrow(fontes, Priority.ALWAYS);
        conteudo = caixa;

        Timeline relogio = new Timeline(new KeyFrame(Duration.millis(500), e -> verificar()));
        relogio.setCycleCount(Animation.INDEFINITE);
        relogio.play();
    }

    private void montarTabelaDeFontes() {
        fontes.getColumns().addAll(java.util.List.of(
                Tabelas.<EstadoFonte>texto("Ligada", 70, f -> f.isLigada() ? "sim" : "não"),
                Tabelas.<EstadoFonte>texto("Município", 160, f -> f.getMunicipio().getNome()),
                Tabelas.<EstadoFonte>texto("UF", 50, f -> f.getMunicipio().getUf()),
                Tabelas.texto("Status", 130, EstadoFonte::getStatus),
                Tabelas.texto("Última tentativa", 150, EstadoFonte::getUltimaTentativa),
                Tabelas.<EstadoFonte>texto("No acervo", 80, f -> String.valueOf(f.getDiariosNoAcervo())),
                Tabelas.texto("Motivo", 380, EstadoFonte::getMotivo)));
    }

    private void verificar() {
        boolean ocupado = app.isOcupado();
        coletar.setDisable(ocupado);
        descobrir.setDisable(ocupado);
        barra.setVisible(ocupado);
        barra.setProgress(ocupado ? ProgressBar.INDETERMINATE_PROGRESS : 0);
        if (ocupado) {
            progresso.setText(app.getProgresso().isEmpty() ? "trabalhando…" : app.getProgresso());
        } else if (estavaOcupado) {
            progresso.setText("terminou");
            aoTerminar.run();
        }
        estavaOcupado = ocupado;
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
        EstadoFonte selecionada = fontes.getSelectionModel().getSelectedItem();
        fontes.setItems(FXCollections.observableArrayList(app.getFontes().todas()));
        if (selecionada != null) {
            fontes.getSelectionModel().select(app.getFontes().doCodigo(selecionada.getMunicipio().getCodigo()));
        }
        fontes.refresh();

        historico.getItems().clear();
        for (Rodada r : app.rodadasRecentes()) {
            historico.getItems().add(r.getInicio() + "  " + r.getTipo() + "  " + r.getDesde() + " a "
                    + r.getAte() + "  baixados " + r.baixados() + ", falhas " + r.falhas()
                    + (r.getObservacao() == null || r.getObservacao().isEmpty() ? "" : "  — " + r.getObservacao()));
        }
    }
}
