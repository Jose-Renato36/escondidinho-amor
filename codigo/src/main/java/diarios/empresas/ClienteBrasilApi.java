package diarios.empresas;

import diarios.erros.ColetaException;
import java.io.IOException;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/** Consulta o cadastro de CNPJ na BrasilAPI (dados publicos da Receita Federal). */
public class ClienteBrasilApi {

    public static final String URL_PADRAO = "https://brasilapi.com.br/api/cnpj/v1";

    private static final String AGENTE = "ProjetoFaculdade/1.0 (trabalho academico)";
    private static final long PAUSA_PADRAO_MS = 1000;

    private final String urlBase;
    private final HttpClient cliente;
    private final long pausaEntreConsultasMs;
    private long ultimaConsulta = 0;

    public ClienteBrasilApi() {
        this(System.getProperty("diarios.brasilapi", URL_PADRAO));
    }

    /** A url base e parametro para os testes poderem apontar para um servidor local. */
    public ClienteBrasilApi(String urlBase) {
        this(urlBase, PAUSA_PADRAO_MS);
    }

    public ClienteBrasilApi(String urlBase, long pausaEntreConsultasMs) {
        this.urlBase = urlBase;
        this.pausaEntreConsultasMs = pausaEntreConsultasMs;
        if (System.getProperty("https.proxyHost") == null) {
            System.setProperty("java.net.useSystemProxies", "true");
        }
        this.cliente = HttpClient.newBuilder()
                .proxy(ProxySelector.getDefault())
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    /** Devolve o JSON bruto da empresa. Nao consulta CNPJ que ja reprova nos digitos. */
    public synchronized String consultar(String cnpj) throws ColetaException {
        String digitos = Cnpj.somenteDigitos(cnpj);
        if (!Cnpj.valido(digitos)) {
            throw new ColetaException(urlBase, "CNPJ invalido (digito verificador errado): " + cnpj);
        }
        esperarSuaVez();
        String url = urlBase + "/" + digitos;
        try {
            HttpRequest pedido = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(20))
                    .header("User-Agent", AGENTE)
                    .header("Accept", "application/json")
                    .GET().build();
            HttpResponse<String> resposta = cliente.send(pedido,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return tratar(url, resposta.statusCode(), resposta.body());
        } catch (IOException e) {
            throw new ColetaException(url, "sem conexao com a BrasilAPI: " + e.getMessage(), true, 0);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ColetaException(url, "consulta interrompida");
        }
    }

    private static String tratar(String url, int status, String corpo) throws ColetaException {
        if (status == 200) {
            return corpo;
        }
        if (status == 404) {
            throw new ColetaException(url, "CNPJ nao encontrado na base da Receita");
        }
        if (status == 429) {
            throw new ColetaException(url, "limite de consultas da BrasilAPI atingido; tente de novo em instantes",
                    true, 10000);
        }
        throw new ColetaException(url, "BrasilAPI respondeu HTTP " + status, status >= 500, 5000);
    }

    /** Educacao com o servidor publico: no maximo uma consulta por segundo. */
    private void esperarSuaVez() throws ColetaException {
        long espera = ultimaConsulta + pausaEntreConsultasMs - System.currentTimeMillis();
        if (espera > 0) {
            try {
                Thread.sleep(espera);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ColetaException(urlBase, "consulta interrompida");
            }
        }
        ultimaConsulta = System.currentTimeMillis();
    }
}
