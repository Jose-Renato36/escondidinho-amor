package diarios.coleta;

import diarios.erros.ColetaException;
import java.io.IOException;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;

public class Coletor {

    private static final String API = "https://api.queridodiario.org.br/gazettes";
    private static final String AGENTE = "ProjetoFaculdade/1.0 (trabalho academico)";
    private static final int LIMITE = 50;
    private static final int PAGINA_DESCOBERTA = 100;
    private static final int PAGINAS_DESCOBERTA = 3;
    private static final long PAUSA_MS = 1500;
    private static final long[] ESPERAS_ENTRE_TENTATIVAS_MS = {5000, 15000, 45000};
    private static final long ESPERA_MAXIMA_MS = 60000;

    private final String pastaDestino;
    private final HttpClient cliente;

    public Coletor(String pastaDestino) {
        this.pastaDestino = pastaDestino;
        if (System.getProperty("https.proxyHost") == null) {
            System.setProperty("java.net.useSystemProxies", "true");
        }
        this.cliente = HttpClient.newBuilder()
                .proxy(ProxySelector.getDefault())
                .connectTimeout(Duration.ofSeconds(20))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public ResultadoColeta coletar(Municipio municipio, String desde, String ate) {
        ResultadoColeta r = new ResultadoColeta(municipio);
        String url = "?territory_ids=" + municipio.getCodigo()
                + "&published_since=" + desde + "&published_until=" + ate
                + "&size=" + LIMITE;

        String resposta;
        try {
            resposta = consultarApi(url, "consulta");
        } catch (ColetaException e) {
            r.marcarFalhaDeRede(e.getMessage());
            r.anotar(municipio.getNome() + ": " + e.getMessage());
            return r;
        }

        ArrayList<String> gazetas = Json.objetos(resposta, "gazettes");
        if (gazetas.isEmpty()) {
            r.anotar(municipio.getNome() + ": nenhuma edicao de " + desde + " a " + ate);
            return r;
        }

        for (String gazeta : gazetas) {
            r.contarEdicao();
            baixarEdicao(municipio, gazeta, r);
        }
        return r;
    }

    public ArrayList<Municipio> descobrir(String desde, String ate) throws ColetaException {
        ArrayList<Municipio> achados = new ArrayList<>();
        ArrayList<String> codigos = new ArrayList<>();

        for (int pagina = 0; pagina < PAGINAS_DESCOBERTA; pagina++) {
            String url = "?published_since=" + desde + "&published_until=" + ate
                    + "&size=" + PAGINA_DESCOBERTA + "&offset=" + (pagina * PAGINA_DESCOBERTA);
            ArrayList<String> gazetas = Json.objetos(consultarApi(url, "descoberta"), "gazettes");

            for (String g : gazetas) {
                String codigo = Json.campo(g, "territory_id");
                if (!codigo.isEmpty() && !Json.campo(g, "txt_url").isEmpty()
                        && !codigos.contains(codigo)) {
                    codigos.add(codigo);
                    achados.add(new Municipio(codigo, Json.campo(g, "territory_name"),
                            Json.campo(g, "state_code")));
                }
            }
            if (gazetas.size() < PAGINA_DESCOBERTA) {
                break;
            }
            esperar();
        }
        return achados;
    }

    private void baixarEdicao(Municipio municipio, String gazeta, ResultadoColeta r) {
        String data = Json.campo(gazeta, "date");
        String urlTexto = Json.campo(gazeta, "txt_url");
        String nome = municipio.getNome() + " " + data;

        if (urlTexto.isEmpty()) {
            r.contarSemTexto();
            r.anotar(nome + ": so PDF, sem texto");
            return;
        }

        Path destino = Paths.get(pastaDestino, municipio.getApelido() + "_" + data
                + sufixoDeEdicao(gazeta) + ".txt");
        if (Files.exists(destino)) {
            r.contarJaExistia();
            r.anotar(nome + ": ja estava na pasta");
            return;
        }

        try {
            String texto = buscarTexto(urlTexto, "download de " + data);
            Files.createDirectories(destino.getParent());
            Files.writeString(destino, texto, StandardCharsets.UTF_8);
            r.contarBaixado();
            r.anotar(nome + ": " + (texto.length() / 1024) + " KB -> " + destino.getFileName());
        } catch (IOException e) {
            r.anotar(nome + ": falha ao gravar - " + e.getMessage());
        } catch (ColetaException e) {
            r.anotar(nome + ": " + e.getMessage());
        }
        esperar();
    }

    private String sufixoDeEdicao(String gazeta) {
        boolean extra = Json.campo(gazeta, "is_extra_edition").equals("true");
        String edicao = Json.campo(gazeta, "edition").replaceAll("[^0-9A-Za-z]", "");
        if (!extra) {
            return "";
        }
        return "-extra" + (edicao.isEmpty() ? "" : edicao);
    }

    private String consultarApi(String parametros, String oQue) throws ColetaException {
        String url = API + parametros;
        for (int tentativa = 0; ; tentativa++) {
            try {
                String resposta = buscarTexto(url, oQue);
                esperar();
                return resposta;
            } catch (ColetaException e) {
                boolean acabaram = tentativa >= ESPERAS_ENTRE_TENTATIVAS_MS.length;
                if (!e.podeTentarDeNovo() || acabaram) {
                    throw new ColetaException(url, e.getMessage()
                            + (tentativa > 0 ? " (depois de " + (tentativa + 1) + " tentativas)" : ""),
                            e.podeTentarDeNovo(), e.getEsperaSugeridaMs());
                }
                long espera = Math.max(ESPERAS_ENTRE_TENTATIVAS_MS[tentativa], e.getEsperaSugeridaMs());
                esperar(Math.min(espera, ESPERA_MAXIMA_MS));
            }
        }
    }

    private String buscarTexto(String url, String oQue) throws ColetaException {
        HttpRequest pedido = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", AGENTE)
                .header("Accept", "application/json, text/plain, */*")
                .timeout(Duration.ofSeconds(90))
                .GET()
                .build();
        try {
            HttpResponse<String> resposta =
                    cliente.send(pedido, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int status = resposta.statusCode();
            if (status != 200) {
                boolean passageiro = status == 429 || status >= 500;
                long sugerida = resposta.headers().firstValue("Retry-After")
                        .map(this::segundosParaMs).orElse(0L);
                throw new ColetaException(url, oQue + " respondeu HTTP " + status
                        + (status == 429 ? " (limite de pedidos da API)" : "")
                        + " [" + trechoDoCorpo(resposta.body()) + "]", passageiro, sugerida);
            }
            return resposta.body();
        } catch (IOException e) {
            throw new ColetaException(url, oQue + " falhou: " + e.getClass().getSimpleName()
                    + " " + e.getMessage(), true, 0);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ColetaException(url, oQue + " foi interrompido");
        }
    }

    private long segundosParaMs(String valor) {
        try {
            return Long.parseLong(valor.trim()) * 1000;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String trechoDoCorpo(String corpo) {
        if (corpo == null) {
            return "";
        }
        String limpo = corpo.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
        return limpo.length() > 160 ? limpo.substring(0, 160) + "..." : limpo;
    }

    private void esperar() {
        esperar(PAUSA_MS);
    }

    private void esperar(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
