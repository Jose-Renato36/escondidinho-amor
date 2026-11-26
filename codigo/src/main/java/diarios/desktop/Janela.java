package diarios.desktop;

import atlantafx.base.controls.ToggleSwitch;
import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import atlantafx.base.theme.Styles;
import diarios.Aplicacao;
import diarios.empresas.ClienteBrasilApi;
import diarios.empresas.ServicoEmpresas;
import diarios.persistencia.Banco;
import diarios.servidor.Servidor;
import java.sql.SQLException;
import java.util.ArrayList;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Janela desktop (JavaFX). Usa a mesma Aplicacao do painel web: a logica de leitura,
 * analise e coleta nao foi duplicada, so ganhou outra tela.
 */
public class Janela extends Application {

    private static final int PORTA_WEB = 8080;

    private final Aplicacao app = new Aplicacao();
    private final ArrayList<Aba> abas = new ArrayList<>();
    private Banco banco;
    private Servidor servidorWeb;

    private final Label status = new Label("lendo os diários da pasta…");
    private final Label tituloDaPagina = new Label();
    private final StackPane area = new StackPane();
    private final ToggleGroup menu = new ToggleGroup();

    @Override
    public void start(Stage palco) {
        abrirBanco();
        setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

        abas.add(new AbaResumo(app));
        abas.add(new AbaGraficos(banco));
        abas.add(new AbaAtos(app));
        abas.add(new AbaContratos(app));
        abas.add(new AbaEmpresas(app, new ServicoEmpresas(banco, new ClienteBrasilApi())));
        abas.add(new AbaNormas(app));
        abas.add(new AbaColeta(app, this::reprocessarSemLer));

        BorderPane raiz = new BorderPane();
        raiz.setLeft(montarLateral());
        raiz.setCenter(montarPagina());

        Scene cena = new Scene(raiz, 1240, 800);
        cena.getStylesheets().add(Estilo.css());
        palco.setTitle("Diários Oficiais — análise de atos municipais");
        palco.setMinWidth(980);
        palco.setMinHeight(620);
        palco.setScene(cena);
        palco.show();

        ((ToggleButton) menu.getToggles().get(0)).fire();
        executar("lendo e interpretando os diários da pasta…", true);
    }

    private VBox montarLateral() {
        Label marca = new Label("Diários Oficiais");
        marca.getStyleClass().add("marca");
        Label sub = new Label("atos municipais, lidos por regra");
        sub.getStyleClass().addAll("sub", Styles.TEXT_SMALL);

        VBox lateral = new VBox(4, marca, sub, espaco(14));
        lateral.getStyleClass().add("lateral");
        lateral.setPrefWidth(230);
        lateral.setMinWidth(230);

        for (Aba aba : abas) {
            ToggleButton item = new ToggleButton(aba.getTitulo());
            item.getStyleClass().add("nav");
            item.setMaxWidth(Double.MAX_VALUE);
            item.setToggleGroup(menu);
            item.setOnAction(e -> {
                if (!item.isSelected()) {
                    item.setSelected(true); // clicar no item ja marcado nao desmarca
                }
                mostrar(aba);
            });
            lateral.getChildren().add(item);
        }

        Region folga = new Region();
        VBox.setVgrow(folga, Priority.ALWAYS);

        ToggleSwitch escuro = new ToggleSwitch("Modo escuro");
        escuro.selectedProperty().addListener((o, antes, agora) -> setUserAgentStylesheet(
                agora ? new PrimerDark().getUserAgentStylesheet() : new PrimerLight().getUserAgentStylesheet()));

        Button web = new Button("Abrir painel web");
        web.setMaxWidth(Double.MAX_VALUE);
        web.getStyleClass().add(Styles.BUTTON_OUTLINED);
        web.setOnAction(e -> abrirPainelWeb());

        lateral.getChildren().addAll(folga, escuro, espaco(6), web);
        return lateral;
    }

    private BorderPane montarPagina() {
        tituloDaPagina.getStyleClass().add(Styles.TITLE_2);
        status.getStyleClass().add("sub");
        Region folga = new Region();
        HBox.setHgrow(folga, Priority.ALWAYS);
        HBox cabecalho = new HBox(12, tituloDaPagina, folga, status);
        cabecalho.getStyleClass().add("cabecalho");
        cabecalho.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        area.getStyleClass().add("pagina");
        BorderPane pagina = new BorderPane(area);
        pagina.setTop(cabecalho);
        return pagina;
    }

    private void mostrar(Aba aba) {
        tituloDaPagina.setText(aba.getTitulo());
        area.getChildren().setAll(aba.getConteudo());
    }

    private static Region espaco(double altura) {
        Region r = new Region();
        r.setMinHeight(altura);
        r.setPadding(new Insets(0));
        return r;
    }

    private void abrirBanco() {
        try {
            banco = Banco.emArquivo(Banco.ARQUIVO_PADRAO);
        } catch (SQLException e) {
            System.out.println("aviso: sem banco em arquivo (" + e.getMessage() + "); usando memória");
            try {
                banco = Banco.emMemoria();
            } catch (SQLException ex) {
                throw new IllegalStateException("SQLite indisponível", ex);
            }
        }
    }

    /** A coleta ja processou a pasta sozinha; aqui so grava no banco e redesenha. */
    private void reprocessarSemLer() {
        executar("atualizando o banco e as telas…", false);
    }

    /** Roda o trabalho pesado fora da thread da interface e so depois mexe nas telas. */
    private void executar(String mensagem, boolean lerPasta) {
        status.setText(mensagem);
        Task<Void> tarefa = new Task<>() {
            @Override
            protected Void call() throws Exception {
                if (lerPasta) {
                    app.processarPasta();
                }
                banco.salvar(app.getDiarios(), app.getPainel(), app.getGrafo());
                return null;
            }
        };
        tarefa.setOnSucceeded(e -> {
            for (Aba a : abas) {
                a.atualizar();
            }
            status.setText(app.getCobertura().getTotalAtos() + " atos · "
                    + app.getPainel().getContratos().size() + " contratações · atualizado em "
                    + app.getUltimoProcessamento());
        });
        tarefa.setOnFailed(e -> {
            status.setText("erro: " + tarefa.getException().getMessage());
            new Alert(Alert.AlertType.ERROR, String.valueOf(tarefa.getException())).show();
        });
        Thread t = new Thread(tarefa, "processamento");
        t.setDaemon(true);
        t.start();
    }

    private void abrirPainelWeb() {
        if (servidorWeb == null) {
            servidorWeb = new Servidor(app);
            if (!servidorWeb.iniciar(PORTA_WEB)) {
                servidorWeb = null;
                new Alert(Alert.AlertType.ERROR, "Não consegui abrir o painel web.").show();
                return;
            }
        }
        getHostServices().showDocument("http://localhost:" + servidorWeb.getPorta());
    }

    @Override
    public void stop() throws Exception {
        if (servidorWeb != null) {
            servidorWeb.parar();
        }
        if (banco != null) {
            banco.close();
        }
    }
}
