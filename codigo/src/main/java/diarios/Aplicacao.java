package diarios;

import diarios.analise.Cobertura;
import diarios.analise.GrafoNormas;
import diarios.analise.Interpretacao;
import diarios.analise.LeituraDoAto;
import diarios.analise.PainelContratos;
import diarios.coleta.Coletor;
import diarios.coleta.EstadoFonte;
import diarios.coleta.Fontes;
import diarios.coleta.Municipio;
import diarios.coleta.ResultadoColeta;
import diarios.coleta.Rodada;
import diarios.erros.ColetaException;
import diarios.erros.DiarioIlegivelException;
import diarios.etapas.Extracao;
import diarios.etapas.Fragmentacao;
import diarios.etapas.Ingestao;
import diarios.etapas.Limpeza;
import diarios.etapas.Portao;
import diarios.modelo.Diario;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class Aplicacao {

    public static final String PASTA_DIARIOS = "dados/diarios";
    public static final String PASTA_SAIDA = "dados/saida";
    public static final String PASTA_ESTADO = "dados/estado";

    public static final int DIAS_MAXIMO = 90;
    private static final int DIAS_DESCOBERTA = 3;
    private static final int RODADAS_NA_TELA = 12;
    private static final int FALHAS_SEGUIDAS_PARA_PARAR = 3;

    private final Fontes fontes;
    private final Coletor coletor = new Coletor(PASTA_DIARIOS);
    private final Path arquivoRodadas = Paths.get(PASTA_ESTADO, "rodadas.txt");

    private ArrayList<Diario> diarios = new ArrayList<>();
    private Cobertura cobertura = new Cobertura();
    private PainelContratos painel = new PainelContratos();
    private GrafoNormas grafo = new GrafoNormas();
    private Interpretacao interpretacao = new Interpretacao();
    private ArrayList<String> falhasDeLeitura = new ArrayList<>();
    private String ultimoProcessamento = "-";

    private final ArrayList<Rodada> rodadas = new ArrayList<>();
    private Rodada ultimaRodada = null;

    private volatile boolean ocupado = false;
    private volatile String progresso = "";

    public Aplicacao() {
        criarPastas();
        fontes = new Fontes(PASTA_ESTADO);
        carregarRodadas();
        fontes.atualizarAcervo(PASTA_DIARIOS);
    }

    public void processarPasta() {
        ArrayList<String> falhas = new ArrayList<>();
        ArrayList<Diario> lidos = new Ingestao(PASTA_DIARIOS).lerTodos(falhas);

        ArrayList<Diario> novos = new ArrayList<>();
        Cobertura novaCobertura = new Cobertura();
        Limpeza limpeza = new Limpeza();
        Extracao extracao = new Extracao();
        Portao portao = new Portao();

        for (Diario d : lidos) {
            try {
                limpeza.limpar(d);
            } catch (DiarioIlegivelException e) {
                novaCobertura.registrarRecusa(e.getArquivo(), e.getMessage(), e.getEvidencia());
                novos.add(d);
                continue;
            }
            new Fragmentacao().fragmentar(d);
            extracao.extrair(d);
            portao.avaliar(d);
            novaCobertura.registrarProcessado(d);
            novos.add(d);
        }

        PainelContratos novoPainel = new PainelContratos();
        novoPainel.processar(novos);
        GrafoNormas novoGrafo = new GrafoNormas();
        novoGrafo.processar(novos);
        Interpretacao novaInterpretacao = new Interpretacao();
        novaInterpretacao.interpretar(novos, novaCobertura, novoPainel, novoGrafo);

        synchronized (this) {
            diarios = novos;
            cobertura = novaCobertura;
            painel = novoPainel;
            grafo = novoGrafo;
            interpretacao = novaInterpretacao;
            falhasDeLeitura = falhas;
            ultimoProcessamento = agora();
        }
        fontes.atualizarAcervo(PASTA_DIARIOS);
    }

    public Rodada coletar(int dias) {
        return coletar(dias, false);
    }

    public Rodada coletar(int dias, boolean refazerTudo) {
        int n = Math.max(1, Math.min(dias, DIAS_MAXIMO));
        LocalDate hoje = LocalDate.now();
        String desde = hoje.minusDays(n).toString();
        String ate = hoje.toString();

        ArrayList<EstadoFonte> alvo = fontes.ligadas();
        Rodada rodada = new Rodada(agora(), "coleta", n, desde, ate);

        int i = 0;
        int falhasSeguidas = 0;
        String interrompida = "";
        for (EstadoFonte estado : alvo) {
            i++;
            if (!refazerTudo && estado.jaCobre(desde, ate)) {
                ResultadoColeta pulada = new ResultadoColeta(estado.getMunicipio());
                pulada.marcarPulada();
                pulada.anotar(estado.getMunicipio().getNome() + ": pulada, ja coletada ("
                        + estado.getStatus() + " de " + estado.getCoberturaDesde() + " a "
                        + estado.getCoberturaAte() + ")");
                rodada.adicionar(pulada);
                continue;
            }
            progresso = "coletando " + estado.getMunicipio().getNome()
                    + " (" + i + " de " + alvo.size() + ")";
            ResultadoColeta r = coletor.coletar(estado.getMunicipio(), desde, ate);
            estado.registrarResultado(agora(), r, desde, ate);
            rodada.adicionar(r);

            falhasSeguidas = r.isFalhouRede() ? falhasSeguidas + 1 : 0;
            if (falhasSeguidas >= FALHAS_SEGUIDAS_PARA_PARAR && i < alvo.size()) {
                interrompida = "interrompida depois de " + FALHAS_SEGUIDAS_PARA_PARAR
                        + " falhas seguidas; " + (alvo.size() - i)
                        + " fonte(s) ficaram para a proxima rodada";
                break;
            }
        }
        fontes.salvar();

        progresso = "lendo e interpretando os diarios da pasta";
        processarPasta();

        String obs = alvo.isEmpty() ? "nenhuma fonte ligada" : interrompida;
        rodada.finalizar(agora(), obs);
        registrarRodada(rodada);
        return rodada;
    }

    public Rodada descobrir() {
        LocalDate hoje = LocalDate.now();
        String desde = hoje.minusDays(DIAS_DESCOBERTA).toString();
        String ate = hoje.toString();
        Rodada rodada = new Rodada(agora(), "descoberta", DIAS_DESCOBERTA, desde, ate);

        progresso = "perguntando a API quem publicou de " + desde + " a " + ate;
        try {
            ArrayList<Municipio> achados = coletor.descobrir(desde, ate);
            int novas = fontes.adicionarDescobertas(achados, agora());
            rodada.finalizar(agora(), achados.size() + " municipio(s) com texto publicado, "
                    + novas + " novo(s) na lista");
        } catch (ColetaException e) {
            rodada.finalizar(agora(), "falhou: " + e.getMessage());
        }
        fontes.atualizarAcervo(PASTA_DIARIOS);
        registrarRodada(rodada);
        return rodada;
    }

    public synchronized boolean coletarEmSegundoPlano(int dias) {
        return coletarEmSegundoPlano(dias, false);
    }

    public synchronized boolean coletarEmSegundoPlano(int dias, boolean refazerTudo) {
        if (ocupado) {
            return false;
        }
        ocupado = true;
        Thread t = new Thread(() -> {
            try {
                coletar(dias, refazerTudo);
            } finally {
                ocupado = false;
                progresso = "";
            }
        }, "coleta");
        t.setDaemon(true);
        t.start();
        return true;
    }

    public synchronized boolean descobrirEmSegundoPlano() {
        if (ocupado) {
            return false;
        }
        ocupado = true;
        Thread t = new Thread(() -> {
            try {
                descobrir();
            } finally {
                ocupado = false;
                progresso = "";
            }
        }, "descoberta");
        t.setDaemon(true);
        t.start();
        return true;
    }

    public void alternarFonte(String codigo) {
        fontes.alternar(codigo);
    }

    public void ligarTodas(boolean ligar) {
        fontes.ligarTodas(ligar);
    }

    public synchronized LeituraDoAto leitor() {
        return new LeituraDoAto(painel, grafo);
    }

    public synchronized Diario diarioDoArquivo(String arquivo) {
        for (Diario d : diarios) {
            if (d.getArquivo().equals(arquivo)) {
                return d;
            }
        }
        return null;
    }

    public synchronized ArrayList<Rodada> rodadasRecentes() {
        ArrayList<Rodada> lista = new ArrayList<>();
        for (int i = rodadas.size() - 1; i >= 0 && lista.size() < RODADAS_NA_TELA; i--) {
            lista.add(rodadas.get(i));
        }
        return lista;
    }

    private synchronized void registrarRodada(Rodada rodada) {
        rodadas.add(rodada);
        ultimaRodada = rodada;
        try {
            Files.writeString(arquivoRodadas, rodada.paraLinha() + System.lineSeparator(),
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.out.println("aviso: nao consegui registrar a rodada: " + e.getMessage());
        }
    }

    private void carregarRodadas() {
        if (!Files.exists(arquivoRodadas)) {
            return;
        }
        try {
            for (String linha : Files.readAllLines(arquivoRodadas, StandardCharsets.UTF_8)) {
                Rodada r = Rodada.daLinha(linha);
                if (r != null) {
                    rodadas.add(r);
                }
            }
        } catch (IOException | NumberFormatException e) {
            System.out.println("aviso: historico de rodadas ilegivel, comecando vazio.");
        }
    }

    private void criarPastas() {
        try {
            Files.createDirectories(Paths.get(PASTA_DIARIOS));
            Files.createDirectories(Paths.get(PASTA_SAIDA));
            Files.createDirectories(Paths.get(PASTA_ESTADO));
        } catch (IOException e) {
            System.out.println("aviso: nao consegui criar as pastas: " + e.getMessage());
        }
    }

    public static String agora() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public synchronized ArrayList<Diario> getDiarios()       { return diarios; }
    public synchronized Cobertura getCobertura()             { return cobertura; }
    public synchronized PainelContratos getPainel()          { return painel; }
    public synchronized GrafoNormas getGrafo()               { return grafo; }
    public synchronized Interpretacao getInterpretacao()     { return interpretacao; }
    public synchronized ArrayList<String> getFalhasDeLeitura() { return falhasDeLeitura; }
    public synchronized String getUltimoProcessamento()      { return ultimoProcessamento; }
    public synchronized Rodada getUltimaRodada()             { return ultimaRodada; }
    public Fontes getFontes()                                { return fontes; }
    public boolean isOcupado()                               { return ocupado; }
    public String getProgresso()                             { return progresso; }
}
