package diarios;

import com.sun.net.httpserver.HttpServer;
import diarios.analise.Fornecedor;
import diarios.analise.GrafoNormas;
import diarios.analise.PainelContratos;
import diarios.analise.SinaisDaEmpresa;
import diarios.empresas.ClienteBrasilApi;
import diarios.empresas.Cnpj;
import diarios.empresas.Empresa;
import diarios.empresas.ServicoEmpresas;
import diarios.erros.ColetaException;
import diarios.modelo.Achado;
import diarios.modelo.Contrato;
import diarios.persistencia.Banco;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes da parte de empresas. A BrasilAPI de verdade nao e chamada: um servidor HTTP
 * local, ligado so durante o teste, faz o papel dela.
 *
 * ATENCAO: o JSON de exemplo abaixo foi escrito a mao, com os nomes de campo que a
 * BrasilAPI documenta. A leitura e tolerante a campo faltando, mas o formato real so
 * se confirma na primeira consulta de verdade.
 */
class TestesEmpresas {

    private static final String JSON_EXEMPLO = """
            {"cnpj":"00000000000191","razao_social":"EMPRESA DE EXEMPLO LTDA",
             "nome_fantasia":"EXEMPLO","descricao_situacao_cadastral":"ATIVA",
             "data_inicio_atividade":"2015-03-10","cnae_fiscal_descricao":"Comercio varejista",
             "descricao_porte":"DEMAIS","municipio":"BELO HORIZONTE","uf":"MG","capital_social":50000.5,
             "qsa":[{"nome_socio":"NAO DEVE SER LIDO"}]}""";

    private static final String CNPJ_VALIDO = "00.000.000/0001-91";

    private HttpServer servidor;
    private final AtomicInteger pedidos = new AtomicInteger();
    private volatile int statusDaResposta = 200;
    private volatile String ultimoCaminho = "";

    @BeforeEach
    void ligarServidorFalso() throws Exception {
        servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        servidor.createContext("/", troca -> {
            pedidos.incrementAndGet();
            ultimoCaminho = troca.getRequestURI().getPath();
            byte[] corpo = (statusDaResposta == 200 ? JSON_EXEMPLO : "{}").getBytes(StandardCharsets.UTF_8);
            troca.sendResponseHeaders(statusDaResposta, corpo.length);
            troca.getResponseBody().write(corpo);
            troca.close();
        });
        servidor.start();
    }

    @AfterEach
    void desligarServidorFalso() {
        servidor.stop(0);
    }

    private ClienteBrasilApi cliente() {
        return new ClienteBrasilApi("http://localhost:" + servidor.getAddress().getPort() + "/cnpj", 0);
    }

    @Test
    @DisplayName("Cnpj: digito verificador")
    void digitoVerificador() {
        assertTrue(Cnpj.valido("00.000.000/0001-91"));
        assertTrue(Cnpj.valido("39316457000145"));
        assertFalse(Cnpj.valido("39.316.457/0001-46"), "ultimo digito errado");
        assertFalse(Cnpj.valido("11.111.111/1111-11"), "todos iguais passaria na conta, mas nao existe");
        assertFalse(Cnpj.valido("123"), "tamanho errado");
        assertFalse(Cnpj.valido(null));
    }

    @Test
    @DisplayName("Cnpj: acha a troca de digitos publicada num diario real")
    void trocaDeDigitosNoDiario() {
        // Belo Horizonte, 25/03/2026: a mesma associacao aparece com os dois numeros.
        assertFalse(Cnpj.valido("10.987.990/0001-66"), "digitado errado no diario");
        assertTrue(Cnpj.valido("10.897.990/0001-66"), "o numero correto");
    }

    @Test
    @DisplayName("Cnpj: formatar e tirar a pontuacao")
    void formatacao() {
        assertEquals("39316457000145", Cnpj.somenteDigitos("39.316.457/0001-45"));
        assertEquals("39.316.457/0001-45", Cnpj.formatar("39316457000145"));
        assertEquals("abc", Cnpj.formatar("abc"), "o que nao e CNPJ volta como veio");
    }

    @Test
    @DisplayName("Empresa: le os campos e ignora os socios")
    void lerEmpresa() throws Exception {
        Empresa e = Empresa.deJson(JSON_EXEMPLO);
        assertEquals("EMPRESA DE EXEMPLO LTDA", e.getRazaoSocial());
        assertEquals("ATIVA", e.getSituacao());
        assertTrue(e.isAtiva());
        assertEquals("DEMAIS", e.getPorte());
        assertEquals("MG", e.getUf());
        assertEquals(50000.5, e.getCapitalSocial(), 0.001);
        assertEquals(130, e.mesesDeAtividade(LocalDate.of(2026, 1, 10)));
    }

    @Test
    @DisplayName("Empresa: campo que nao veio nao quebra a leitura")
    void leituraTolerante() throws Exception {
        Empresa e = Empresa.deJson("{\"razao_social\":\"SO O NOME\",\"capital_social\":null}");
        assertEquals("SO O NOME", e.getRazaoSocial());
        assertEquals("", e.getSituacao());
        assertFalse(e.isAtiva());
        assertEquals(0, e.getCapitalSocial(), 0.0);
        assertEquals(-1, e.mesesDeAtividade(LocalDate.of(2026, 1, 1)), "sem data de abertura");
    }

    @Test
    @DisplayName("ClienteBrasilApi: consulta e monta o endereco com so os digitos")
    void consultaNormal() throws Exception {
        String json = cliente().consultar(CNPJ_VALIDO);
        assertTrue(json.contains("EMPRESA DE EXEMPLO"));
        assertEquals("/cnpj/00000000000191", ultimoCaminho);
    }

    @Test
    @DisplayName("ClienteBrasilApi: nao gasta consulta com CNPJ invalido")
    void naoConsultaInvalido() {
        assertThrows(ColetaException.class, () -> cliente().consultar("10.987.990/0001-66"));
        assertEquals(0, pedidos.get());
    }

    @Test
    @DisplayName("ClienteBrasilApi: 404, 429 e 500 viram mensagens claras")
    void respostasDeErro() {
        statusDaResposta = 404;
        ColetaException naoAchou = assertThrows(ColetaException.class, () -> cliente().consultar(CNPJ_VALIDO));
        assertTrue(naoAchou.getMessage().contains("nao encontrado"));
        assertFalse(naoAchou.podeTentarDeNovo());

        statusDaResposta = 429;
        ColetaException limite = assertThrows(ColetaException.class, () -> cliente().consultar(CNPJ_VALIDO));
        assertTrue(limite.podeTentarDeNovo(), "429: vale esperar e tentar de novo");

        statusDaResposta = 500;
        ColetaException caiu = assertThrows(ColetaException.class, () -> cliente().consultar(CNPJ_VALIDO));
        assertTrue(caiu.getMessage().contains("500"));
    }

    @Test
    @DisplayName("ServicoEmpresas: a segunda consulta vem do cache, sem rede")
    void cache() throws Exception {
        try (Banco banco = Banco.emMemoria()) {
            ServicoEmpresas servico = new ServicoEmpresas(banco, cliente());
            assertEquals("EMPRESA DE EXEMPLO LTDA", servico.buscar(CNPJ_VALIDO).getRazaoSocial());
            assertEquals("EMPRESA DE EXEMPLO LTDA", servico.buscar("00000000000191").getRazaoSocial());
            assertEquals(1, pedidos.get(), "so a primeira foi para a rede");
            assertEquals(1, banco.contar("empresas"));
        }
    }

    @Test
    @DisplayName("Banco: reprocessar os diarios nao apaga o cache de empresas")
    void cacheSobreviveAoSalvar() throws Exception {
        try (Banco banco = Banco.emMemoria()) {
            banco.guardarEmpresa("00000000000191", JSON_EXEMPLO, "2026-01-01 10:00:00");
            banco.salvar(new ArrayList<>(), new PainelContratos(), new GrafoNormas());
            assertTrue(banco.jsonDaEmpresa("00000000000191").isPresent());
        }
    }

    // ---------- sinais de atencao (regras, sem rede)

    private static Fornecedor fornecedor(String cnpj, String data, double... valores) {
        Fornecedor f = new Fornecedor(cnpj, "FORNECEDOR");
        for (double v : valores) {
            f.somar(new Contrato("bh", data, cnpj, "FORNECEDOR", "FORNECEDOR", v, "", "ato"));
        }
        return f;
    }

    private static Empresa empresa(String situacao, String abertura, double capital) throws Exception {
        return Empresa.deJson("{\"descricao_situacao_cadastral\":\"" + situacao
                + "\",\"data_inicio_atividade\":\"" + abertura + "\",\"capital_social\":" + capital + "}");
    }

    private static boolean tem(java.util.List<Achado> sinais, String trechoDoTitulo) {
        return sinais.stream().anyMatch(a -> a.getTitulo().contains(trechoDoTitulo));
    }

    @Test
    @DisplayName("Sinais: empresa normal e sem ficha nao gera sinal nenhum")
    void semSinais() throws Exception {
        SinaisDaEmpresa regras = new SinaisDaEmpresa();
        assertEquals(0, regras.avaliar(fornecedor("00000000000191", "2026-03-25", 1000), null).size());
        assertEquals(0, regras.avaliar(fornecedor("00000000000191", "2026-03-25", 1000),
                empresa("ATIVA", "2010-01-01", 500000)).size());
    }

    @Test
    @DisplayName("Sinais: CNPJ invalido, situacao irregular, empresa nova e capital pequeno")
    void cadaSinal() throws Exception {
        SinaisDaEmpresa regras = new SinaisDaEmpresa();
        assertTrue(tem(regras.avaliar(fornecedor("10987990000166", "2026-03-25", 145), null), "inválido"));
        assertTrue(tem(regras.avaliar(fornecedor("00000000000191", "2026-03-25", 1),
                empresa("BAIXADA", "2010-01-01", 0)), "Situação cadastral: BAIXADA"));
        assertTrue(tem(regras.avaliar(fornecedor("00000000000191", "2026-03-25", 1),
                empresa("ATIVA", "2025-12-01", 0)), "aberta há pouco"));
        assertTrue(tem(regras.avaliar(fornecedor("00000000000191", "2026-03-25", 20000),
                empresa("ATIVA", "2010-01-01", 1000)), "capital social"));
    }

    @Test
    @DisplayName("Sinais: os limites - o que fica logo abaixo nao dispara")
    void limites() throws Exception {
        SinaisDaEmpresa regras = new SinaisDaEmpresa();
        // 12 meses exatos de atividade ja nao e "empresa nova"
        assertFalse(tem(regras.avaliar(fornecedor("00000000000191", "2026-03-25", 1),
                empresa("ATIVA", "2025-03-25", 0)), "aberta há pouco"));
        // 10 vezes o capital dispara, 9 nao
        assertTrue(tem(regras.avaliar(fornecedor("00000000000191", "2026-03-25", 10000),
                empresa("ATIVA", "2010-01-01", 1000)), "capital social"));
        assertFalse(tem(regras.avaliar(fornecedor("00000000000191", "2026-03-25", 9000),
                empresa("ATIVA", "2010-01-01", 1000)), "capital social"));
        // sem situacao informada nao acusa "nao ativa"
        assertFalse(tem(regras.avaliar(fornecedor("00000000000191", "2026-03-25", 1),
                empresa("", "2010-01-01", 0)), "Situacao"));
    }

    @Test
    @DisplayName("Sinais: tres contratos do mesmo CNPJ")
    void repeticao() {
        SinaisDaEmpresa regras = new SinaisDaEmpresa();
        assertFalse(tem(regras.avaliar(fornecedor("00000000000191", "2026-03-25", 1, 2), null), "vários"));
        assertTrue(tem(regras.avaliar(fornecedor("00000000000191", "2026-03-25", 1, 2, 3), null), "vários"));
    }

    @Test
    @DisplayName("Fornecedor: soma, conta e guarda a data mais antiga")
    void fornecedorSoma() {
        Fornecedor f = new Fornecedor("00000000000191", "(nao identificada)");
        f.somar(new Contrato("bh", "2026-03-25", "00000000000191", "ACME", "FORNECEDOR", 100, "", "a"));
        f.somar(new Contrato("bh", "2026-03-10", "00000000000191", "ACME", "FORNECEDOR", 50, "", "b"));
        assertEquals(150, f.getTotal(), 0.001);
        assertEquals(2, f.getContratos());
        assertEquals("2026-03-10", f.getPrimeiraData());
        assertEquals("ACME", f.getNome(), "troca o nome '(nao identificada)' pelo primeiro nome util");
    }
}
