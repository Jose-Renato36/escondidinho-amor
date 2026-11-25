package diarios.desktop;

import diarios.Aplicacao;
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
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
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

    @Override
    public void start(Stage palco) {
        abrirBanco();

        abas.add(new AbaResumo(app));
        abas.add(new AbaGraficos(banco));
        abas.add(new AbaAtos(app));
        abas.add(new AbaContratos(app));
        abas.add(new AbaNormas(app));
        abas.add(new AbaColeta(app, this::reprocessarSemLer));

        TabPane painel = new TabPane();
        for (Aba a : abas) {
            painel.getTabs().add(a.getTab());
        }

        Button web = new Button("Abrir painel web");
        web.setOnAction(e -> abrirPainelWeb());
        Region espaco = new Region();
        HBox.setHgrow(espaco, Priority.ALWAYS);
        HBox topo = new HBox(8, new Label("Diários Oficiais"), espaco, web);
        topo.setPadding(new Insets(8, 12, 8, 12));
        topo.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        status.getStyleClass().add("barra-status");
        status.setMaxWidth(Double.MAX_VALUE);

        BorderPane raiz = new BorderPane(painel);
        raiz.setTop(topo);
        raiz.setBottom(status);

        Scene cena = new Scene(raiz, 1180, 780);
        cena.getStylesheets().add("data:text/css;base64,"
                + java.util.Base64.getEncoder().encodeToString(
                        Estilo.CSS.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        palco.setTitle("Diários Oficiais — análise de atos municipais");
        palco.setScene(cena);
        palco.show();

        executar("lendo e interpretando os diários da pasta…", true);
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
