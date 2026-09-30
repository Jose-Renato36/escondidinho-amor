package diarios.servidor;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import diarios.Aplicacao;
import java.awt.Desktop;
import java.io.IOException;
import java.io.OutputStream;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;

public class Servidor {

    private static final int PORTA_PADRAO = 8080;
    private static final int TENTATIVAS_DE_PORTA = 10;
    private static final int DIAS_NA_PARTIDA = 1;

    private final Aplicacao aplicacao;
    private HttpServer http;
    private int porta;

    public Servidor(Aplicacao aplicacao) {
        this.aplicacao = aplicacao;
    }

    public static void main(String[] args) {
        boolean coletarNaPartida = true;
        boolean abrirNavegador = true;
        int porta = PORTA_PADRAO;
        for (String a : args) {
            if (a.equals("--sem-coleta")) {
                coletarNaPartida = false;
            } else if (a.equals("--sem-navegador")) {
                abrirNavegador = false;
            } else if (a.startsWith("--porta=")) {
                porta = Integer.parseInt(a.substring("--porta=".length()));
            }
        }

        System.out.println("Diarios Oficiais - painel");
        System.out.println("pasta de trabalho: " + System.getProperty("user.dir"));

        Aplicacao aplicacao = new Aplicacao();
        System.out.println("lendo os diarios que ja estao na pasta...");
        aplicacao.processarPasta();

        Servidor servidor = new Servidor(aplicacao);
        if (!servidor.iniciar(porta)) {
            return;
        }

        String url = "http://localhost:" + servidor.porta;
        System.out.println();
        System.out.println("PAINEL NO AR: " + url);
        System.out.println("(para parar: botao vermelho do IntelliJ, ou Ctrl+C)");
        System.out.println();

        if (abrirNavegador) {
            abrirNoNavegador(url);
        }
        if (coletarNaPartida) {
            System.out.println("coletando " + DIAS_NA_PARTIDA + " dia(s) para tras "
                    + "das fontes ligadas, em segundo plano...");
            aplicacao.coletarEmSegundoPlano(DIAS_NA_PARTIDA);
        }
    }

    public boolean iniciar(int portaDesejada) {
        for (int p = portaDesejada; p < portaDesejada + TENTATIVAS_DE_PORTA; p++) {
            try {
                http = HttpServer.create(new InetSocketAddress("localhost", p), 0);
                porta = p;
                break;
            } catch (BindException e) {
                System.out.println("porta " + p + " ocupada, tentando a proxima...");
            } catch (IOException e) {
                System.out.println("nao consegui abrir o servidor: " + e.getMessage());
                return false;
            }
        }
        if (http == null) {
            System.out.println("nenhuma porta livre entre " + portaDesejada + " e "
                    + (portaDesejada + TENTATIVAS_DE_PORTA - 1));
            return false;
        }

        http.createContext("/", this::tratarPainel);
        http.createContext("/coleta", this::tratarPaginaColeta);
        http.createContext("/coletar", this::tratarColetar);
        http.createContext("/descobrir", this::tratarDescobrir);
        http.createContext("/processar", this::tratarProcessar);
        http.createContext("/fonte/alternar", this::tratarAlternar);
        http.createContext("/fonte/todas", this::tratarTodas);
        http.createContext("/diario", this::tratarDiario);
        http.createContext("/ato", this::tratarAto);
        http.createContext("/busca", this::tratarBusca);
        http.setExecutor(null);
        http.start();
        return true;
    }

    public void parar() {
        if (http != null) {
            http.stop(0);
        }
    }

    public int getPorta() {
        return porta;
    }

    private void tratarPainel(HttpExchange troca) throws IOException {
        if (!troca.getRequestURI().getPath().equals("/")) {
            responder(troca, 404, PaginaDiario.paginaSimples("Nao encontrado",
                    "Endereco inexistente: " + troca.getRequestURI().getPath()));
            return;
        }
        String aviso = Formulario.parametro(troca.getRequestURI().getRawQuery(), "aviso");
        responder(troca, 200, new PainelWeb(aplicacao).montar(aviso));
    }

    private void tratarPaginaColeta(HttpExchange troca) throws IOException {
        String aviso = Formulario.parametro(troca.getRequestURI().getRawQuery(), "aviso");
        responder(troca, 200, new PainelWeb(aplicacao).montarColeta(aviso));
    }

    private void tratarColetar(HttpExchange troca) throws IOException {
        if (!ehPost(troca)) {
            redirecionar(troca, "/");
            return;
        }
        String corpo = lerCorpo(troca);
        int dias = Formulario.inteiro(corpo, "dias", 1);
        boolean refazer = Formulario.parametro(corpo, "refazer").equals("sim");
        boolean comecou = aplicacao.coletarEmSegundoPlano(dias, refazer);
        redirecionar(troca, comecou ? "/coleta" : "/coleta?aviso=ocupado");
    }

    private void tratarDescobrir(HttpExchange troca) throws IOException {
        if (!ehPost(troca)) {
            redirecionar(troca, "/");
            return;
        }
        boolean comecou = aplicacao.descobrirEmSegundoPlano();
        redirecionar(troca, comecou ? "/coleta" : "/coleta?aviso=ocupado");
    }

    private void tratarProcessar(HttpExchange troca) throws IOException {
        if (ehPost(troca) && !aplicacao.isOcupado()) {
            aplicacao.processarPasta();
        }
        redirecionar(troca, "/coleta");
    }

    private void tratarAlternar(HttpExchange troca) throws IOException {
        if (ehPost(troca)) {
            aplicacao.alternarFonte(Formulario.parametro(lerCorpo(troca), "codigo"));
        }
        redirecionar(troca, "/coleta");
    }

    private void tratarTodas(HttpExchange troca) throws IOException {
        if (ehPost(troca)) {
            aplicacao.ligarTodas(Formulario.parametro(lerCorpo(troca), "ligar").equals("sim"));
        }
        redirecionar(troca, "/coleta");
    }

    private void tratarDiario(HttpExchange troca) throws IOException {
        String arquivo = Formulario.parametro(troca.getRequestURI().getRawQuery(), "arquivo");
        responder(troca, 200, new PaginaDiario(aplicacao).diario(arquivo));
    }

    private void tratarAto(HttpExchange troca) throws IOException {
        String consulta = troca.getRequestURI().getRawQuery();
        String arquivo = Formulario.parametro(consulta, "arquivo");
        int indice = Formulario.inteiro(consulta, "indice", -1);
        responder(troca, 200, new PaginaDiario(aplicacao).ato(arquivo, indice));
    }

    private void tratarBusca(HttpExchange troca) throws IOException {
        String termo = Formulario.parametro(troca.getRequestURI().getRawQuery(), "q");
        responder(troca, 200, new PaginaDiario(aplicacao).busca(termo));
    }

    private boolean ehPost(HttpExchange troca) {
        return troca.getRequestMethod().equalsIgnoreCase("POST");
    }

    private String lerCorpo(HttpExchange troca) throws IOException {
        return new String(troca.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    private void redirecionar(HttpExchange troca, String destino) throws IOException {
        troca.getResponseHeaders().set("Location", destino);
        troca.sendResponseHeaders(303, -1);
        troca.close();
    }

    private void responder(HttpExchange troca, int status, String html) throws IOException {
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        troca.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        troca.getResponseHeaders().set("Cache-Control", "no-store");
        troca.sendResponseHeaders(status, bytes.length);
        try (OutputStream saida = troca.getResponseBody()) {
            saida.write(bytes);
        }
    }

    private static void abrirNoNavegador(String url) {
        try {
            if (Desktop.isDesktopSupported()
                    && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(url));
                return;
            }
        } catch (Exception e) {
            System.out.println("nao consegui abrir o navegador sozinho: " + e.getMessage());
        }
        System.out.println("abra no navegador: " + url);
    }
}
