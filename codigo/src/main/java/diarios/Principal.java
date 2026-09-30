package diarios;

import diarios.coleta.EstadoFonte;
import diarios.coleta.Rodada;
import diarios.etapas.PaginaHtml;
import diarios.etapas.Relatorio;
import diarios.etapas.Visualizador;
import diarios.modelo.Achado;
import diarios.modelo.Ato;
import diarios.modelo.Contrato;
import diarios.modelo.Diario;
import diarios.modelo.Fato;
import diarios.modelo.Relacao;
import diarios.servidor.Servidor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class Principal {

    private final Aplicacao app = new Aplicacao();
    private final Relatorio relatorio = new Relatorio(Aplicacao.PASTA_SAIDA);
    private Servidor servidor = null;

    public static void main(String[] args) {
        Principal p = new Principal();
        p.app.processarPasta();
        if (args.length > 0 && args[0].equals("--tudo")) {
            p.mostrarInterpretacao();
            p.mostrarCobertura();
            p.mostrarPainel();
            p.mostrarGrafo();
            p.gravarTudo();
            return;
        }
        p.menu();
    }

    private void menu() {
        Scanner teclado = new Scanner(System.in);
        boolean rodando = true;

        while (rodando) {
            System.out.println();
            System.out.println("====================================================");
            System.out.println("  DIARIOS OFICIAIS - coleta, leitura e analise");
            System.out.println("====================================================");
            System.out.println("  1 - Coletar das fontes ligadas (escolher dias)");
            System.out.println("  2 - Fontes: ver, ligar e desligar");
            System.out.println("  3 - Descobrir municipios com publicacao recente");
            System.out.println("  4 - Reler a pasta de diarios");
            System.out.println("  5 - INTERPRETACAO: o que esses dados dizem");
            System.out.println("  6 - Ver os arquivos (entrada e saida)");
            System.out.println("  7 - Painel de contratacoes");
            System.out.println("  8 - Grafo de normas");
            System.out.println("  9 - Relatorio de cobertura");
            System.out.println(" 10 - Consultar uma norma no grafo");
            System.out.println(" 11 - Conferir um fato no texto original");
            System.out.println(" 12 - Gravar relatorios em saida/");
            System.out.println(" 13 - Abrir o painel no navegador (localhost)");
            System.out.println("  0 - Sair");
            System.out.print("  escolha: ");

            String escolha = teclado.hasNextLine() ? teclado.nextLine().trim() : "0";

            switch (escolha) {
                case "1":  coletar(teclado); break;
                case "2":  gerenciarFontes(teclado); break;
                case "3":  descobrir(); break;
                case "4":  reler(); break;
                case "5":  mostrarInterpretacao(); break;
                case "6":  verArquivos(teclado); break;
                case "7":  mostrarPainel(); break;
                case "8":  mostrarGrafo(); break;
                case "9":  mostrarCobertura(); break;
                case "10": consultarNorma(teclado); break;
                case "11": conferirFato(teclado); break;
                case "12": gravarTudo(); break;
                case "13": abrirPainel(); break;
                case "0":  rodando = false; break;
                default:   System.out.println("  opcao invalida.");
            }
        }
        if (servidor != null) {
            servidor.parar();
        }
        System.out.println("  ate mais.");
        teclado.close();
    }

    private void coletar(Scanner teclado) {
        ArrayList<EstadoFonte> ligadas = app.getFontes().ligadas();
        if (ligadas.isEmpty()) {
            System.out.println("  nenhuma fonte ligada. Use a opcao 2.");
            return;
        }
        System.out.print("  quantos dias para tras [1]: ");
        int dias = lerInteiro(teclado, 1);
        System.out.println();
        System.out.println("  coletando " + ligadas.size() + " fonte(s), " + dias + " dia(s)...");

        Rodada r = app.coletar(dias);
        System.out.println("  puladas (ja coletadas): " + r.puladas());
        for (String linha : r.registro()) {
            System.out.println("   " + linha);
        }
        System.out.println();
        System.out.println("  baixados: " + r.baixados() + "   ja existiam: " + r.jaExistiam()
                + "   falhas de rede: " + r.falhas());
        reler();
    }

    private void gerenciarFontes(Scanner teclado) {
        while (true) {
            ArrayList<EstadoFonte> todas = app.getFontes().todas();
            System.out.println();
            for (int i = 0; i < todas.size(); i++) {
                EstadoFonte e = todas.get(i);
                System.out.printf("  %2d  [%s] %-24s %-3s %-14s acervo=%d%n", i,
                        e.isLigada() ? "ON " : "off", corta(e.getMunicipio().getNome(), 24),
                        e.getMunicipio().getUf(), e.getStatus(), e.getDiariosNoAcervo());
            }
            System.out.print("  numero para ligar/desligar, 't' liga todas, 'n' desliga todas "
                    + "(enter volta): ");
            String entrada = teclado.hasNextLine() ? teclado.nextLine().trim() : "";
            if (entrada.isEmpty()) {
                return;
            }
            if (entrada.equalsIgnoreCase("t") || entrada.equalsIgnoreCase("n")) {
                app.ligarTodas(entrada.equalsIgnoreCase("t"));
                continue;
            }
            try {
                int i = Integer.parseInt(entrada);
                if (i >= 0 && i < todas.size()) {
                    app.alternarFonte(todas.get(i).getMunicipio().getCodigo());
                } else {
                    System.out.println("  fora da lista.");
                }
            } catch (NumberFormatException e) {
                System.out.println("  isso nao e um numero.");
            }
        }
    }

    private void descobrir() {
        System.out.println();
        System.out.println("  perguntando a API quais municipios publicaram recentemente...");
        Rodada r = app.descobrir();
        System.out.println("  " + r.getObservacao());
        System.out.println("  as novas entram desligadas. Use a opcao 2 para ligar.");
    }

    private void reler() {
        app.processarPasta();
        System.out.println();
        for (String f : app.getFalhasDeLeitura()) {
            System.out.println("  ! " + f);
        }
        for (Diario d : app.getDiarios()) {
            if (d.isUtilizavel()) {
                System.out.println("  " + d.getArquivo() + " ... " + d.getAtos().size() + " atos  ("
                        + d.contar(Ato.ACEITO) + " aceitos, " + d.contar(Ato.DUVIDOSO)
                        + " duvidosos, " + d.contar(Ato.REJEITADO) + " rejeitados)");
            } else {
                System.out.println("  " + d.getArquivo() + " ... RECUSADO ("
                        + d.getMotivoRecusa() + ")");
            }
        }
        System.out.println();
        System.out.println("  pronto. " + app.getCobertura().getUtilizaveis() + " utilizavel(is), "
                + app.getCobertura().getRecusados() + " recusado(s).");
    }

    private void abrirPainel() {
        if (servidor == null) {
            servidor = new Servidor(app);
            if (!servidor.iniciar(8080)) {
                servidor = null;
                return;
            }
        }
        System.out.println("  painel em http://localhost:" + servidor.getPorta()
                + " (fica no ar enquanto o menu estiver aberto)");
    }

    private void mostrarInterpretacao() {
        System.out.println();
        System.out.println("O QUE ESSES DADOS DIZEM");
        System.out.println("=======================");
        System.out.println();
        System.out.println("  " + app.getInterpretacao().getResumo());

        mostrarAchados("LEITURA DO PERIODO", app.getInterpretacao().doNivel(Achado.LEITURA));
        mostrarAchados("ONDE VALE A PENA OLHAR", app.getInterpretacao().doNivel(Achado.ATENCAO));
        mostrarAchados("LIMITES DESTA LEITURA", app.getInterpretacao().doNivel(Achado.LIMITE));

        System.out.println();
        System.out.println("  Cada frase acima sai de uma regra no codigo, sobre numero");
        System.out.println("  medido no texto. Nenhuma foi escrita a mao.");
    }

    private void mostrarAchados(String secao, ArrayList<Achado> lista) {
        if (lista.isEmpty()) {
            return;
        }
        System.out.println();
        System.out.println("  " + secao);
        System.out.println("  " + "-".repeat(secao.length()));
        for (Achado a : lista) {
            System.out.println();
            System.out.println("  " + a.getTitulo());
            for (String linha : quebrarTexto(a.getFrase(), 72)) {
                System.out.println("    " + linha);
            }
            System.out.println("    -> " + a.getEvidencia());
        }
    }

    private ArrayList<String> quebrarTexto(String texto, int largura) {
        ArrayList<String> linhas = new ArrayList<>();
        StringBuilder atual = new StringBuilder();
        for (String palavra : texto.split(" ")) {
            if (atual.length() + palavra.length() + 1 > largura) {
                linhas.add(atual.toString());
                atual = new StringBuilder();
            }
            atual.append(atual.length() > 0 ? " " : "").append(palavra);
        }
        if (atual.length() > 0) {
            linhas.add(atual.toString());
        }
        return linhas;
    }

    private void verArquivos(Scanner teclado) {
        System.out.println();
        System.out.println("  1 - diarios/  (o que entrou)");
        System.out.println("  2 - saida/    (o que o programa produziu)");
        System.out.print("  escolha: ");

        String escolha = teclado.hasNextLine() ? teclado.nextLine().trim() : "";
        Visualizador visual = new Visualizador(teclado);

        if (escolha.equals("1")) {
            visual.abrir(Aplicacao.PASTA_DIARIOS, app.getDiarios());
        } else if (escolha.equals("2")) {
            visual.abrir(Aplicacao.PASTA_SAIDA, new ArrayList<>());
        }
    }

    private void mostrarCobertura() {
        System.out.println();
        for (String l : app.getCobertura().montar()) {
            System.out.println(l);
        }
    }

    private void mostrarPainel() {
        ArrayList<Contrato> contratos = app.getPainel().getContratos();
        System.out.println();
        System.out.println("PAINEL DE CONTRATACOES");
        System.out.println("======================");
        System.out.println();

        if (contratos.isEmpty()) {
            System.out.println("  nenhuma contratacao encontrada.");
            return;
        }

        System.out.println("  contratacoes : " + contratos.size());
        System.out.println("  valor total  : " + Contrato.formatarReal(app.getPainel().total()));
        System.out.println();
        System.out.printf("  %-20s %-38s %18s %6s%n", "CNPJ", "CONTRATADA", "TOTAL", "VEZES");
        System.out.println("  " + "-".repeat(84));

        ArrayList<String[]> ranking = app.getPainel().ranking();
        int mostrar = Math.min(15, ranking.size());
        for (int i = 0; i < mostrar; i++) {
            String[] r = ranking.get(i);
            System.out.printf("  %-20s %-38s %18s %6s%n", r[0], corta(r[1], 38), r[2], r[3]);
        }
        if (ranking.size() > mostrar) {
            System.out.println("  ... e mais " + (ranking.size() - mostrar) + " fornecedor(es)");
        }
    }

    private void mostrarGrafo() {
        System.out.println();
        System.out.println("GRAFO DE NORMAS");
        System.out.println("===============");
        System.out.println();
        System.out.println("  relacoes : " + app.getGrafo().getArestas().size());
        System.out.println("     ALTERA   : " + app.getGrafo().doTipo(Relacao.ALTERA).size());
        System.out.println("     REVOGA   : " + app.getGrafo().doTipo(Relacao.REVOGA).size());
        System.out.println("     PRORROGA : " + app.getGrafo().doTipo(Relacao.PRORROGA).size());
        System.out.println("     CITA     : " + app.getGrafo().doTipo(Relacao.CITA).size());

        ArrayList<String[]> mexidas = app.getGrafo().maisAlteradas();
        if (!mexidas.isEmpty()) {
            System.out.println();
            System.out.println("  NORMAS MAIS MEXIDAS");
            int mostrar = Math.min(10, mexidas.size());
            for (int i = 0; i < mostrar; i++) {
                System.out.printf("   %2sx  %s%n", mexidas.get(i)[1], mexidas.get(i)[0]);
            }
        }
    }

    private void consultarNorma(Scanner teclado) {
        System.out.print("  norma (ex: lei:11065): ");
        String norma = teclado.hasNextLine() ? teclado.nextLine().trim() : "";
        if (norma.isEmpty()) {
            return;
        }

        System.out.println();
        System.out.println("  QUEM MEXEU EM " + norma);
        boolean achou = false;
        for (Relacao r : app.getGrafo().getArestas()) {
            if (r.getAlvo().equals(norma)) {
                System.out.println("   " + r);
                achou = true;
            }
        }
        if (!achou) {
            System.out.println("   nenhuma relacao com essa norma.");
            return;
        }

        ArrayList<String> vizinhos = app.getGrafo().vizinhanca(norma, 2);
        System.out.println();
        System.out.println("  VIZINHANCA EM 2 NIVEIS: " + vizinhos.size() + " nos");
        int mostrar = Math.min(12, vizinhos.size());
        for (int i = 0; i < mostrar; i++) {
            System.out.println("   " + vizinhos.get(i));
        }
    }

    private void conferirFato(Scanner teclado) {
        Diario escolhido = null;
        for (Diario d : app.getDiarios()) {
            if (d.isUtilizavel() && !d.getAtos().isEmpty()) {
                escolhido = d;
                break;
            }
        }
        if (escolhido == null) {
            System.out.println("  nenhum diario utilizavel.");
            return;
        }

        System.out.println();
        System.out.println("  atos de " + escolhido.getArquivo() + ":");
        int mostrar = Math.min(15, escolhido.getAtos().size());
        for (int i = 0; i < mostrar; i++) {
            System.out.printf("   %2d - %s%n", i, escolhido.getAtos().get(i));
        }
        System.out.print("  numero do ato: ");

        int indice = lerInteiro(teclado, -1);
        if (indice < 0 || indice >= escolhido.getAtos().size()) {
            System.out.println("  fora da lista.");
            return;
        }

        Ato ato = escolhido.getAtos().get(indice);
        System.out.println();
        System.out.println("  " + ato.getTitulo());
        System.out.println("  leitura    : " + app.leitor().ler(ato, escolhido.getData()));
        System.out.println("  identidade : " + ato.getIdentidade());
        System.out.println("  decisao    : " + ato.getDecisao() + " - " + ato.getMotivo());
        System.out.println();
        System.out.println("  FATOS E A PROVA DE CADA UM NO TEXTO ORIGINAL:");

        int limite = Math.min(12, ato.getFatos().size());
        for (int i = 0; i < limite; i++) {
            Fato f = ato.getFatos().get(i);
            System.out.println("   " + f);
            System.out.println("      ..." + f.provar(escolhido.getTextoLimpo(), 45) + "...");
        }
    }

    private void gravarTudo() {
        try {
            System.out.println();
            System.out.println("  gravado em:");
            System.out.println("   " + relatorio.gravar("interpretacao.txt", linhasDeInterpretacao()));
            System.out.println("   " + relatorio.gravar("cobertura.txt", app.getCobertura().montar()));
            System.out.println("   " + relatorio.gravar("contratos.txt", linhasDeContratos()));
            System.out.println("   " + relatorio.gravar("grafo.txt", linhasDeGrafo()));
            System.out.println("   " + relatorio.gravar("atos.txt", linhasDeAtos()));
            System.out.println("   " + new PaginaHtml(relatorio).gerar(app.getDiarios(),
                    app.getCobertura(), app.getPainel(), app.getGrafo(), app.getInterpretacao()));
        } catch (IOException e) {
            System.out.println("  falha ao gravar: " + e.getMessage());
        }
    }

    private ArrayList<String> linhasDeInterpretacao() {
        ArrayList<String> l = new ArrayList<>();
        l.add("O QUE ESSES DADOS DIZEM");
        l.add("=======================");
        l.add("");
        l.add(app.getInterpretacao().getResumo());
        for (Achado a : app.getInterpretacao().getAchados()) {
            l.add("");
            l.add("[" + a.getNivel() + "] " + a.getTitulo());
            l.add("  " + a.getFrase());
            l.add("  evidencia: " + a.getEvidencia());
        }
        return l;
    }

    private ArrayList<String> linhasDeContratos() {
        ArrayList<String> l = new ArrayList<>();
        l.add("CONTRATACOES");
        l.add("municipio;data;cnpj;contratada;valor;processo;ato");
        for (Contrato c : app.getPainel().getContratos()) {
            l.add(String.join(";", c.getMunicipio(), c.getData(), c.getCnpj(),
                    c.getContratada().replace(';', ','),
                    String.format(java.util.Locale.ROOT, "%.2f", c.getValor()), c.getProcesso(), c.getIdentidadeAto()));
        }
        l.add("");
        l.add("TOTAL;" + Contrato.formatarReal(app.getPainel().total()));
        l.add("");
        l.add("RANKING DE FORNECEDORES");
        l.add("cnpj;contratada;total;vezes");
        for (String[] r : app.getPainel().ranking()) {
            l.add(String.join(";", r[0], r[1].replace(';', ','), r[2], r[3]));
        }
        return l;
    }

    private ArrayList<String> linhasDeGrafo() {
        ArrayList<String> l = new ArrayList<>();
        l.add("GRAFO DE NORMAS");
        l.add("origem;acao;alvo;posicao");
        for (Relacao r : app.getGrafo().getArestas()) {
            l.add(String.join(";", r.getOrigem(), r.getAcao(), r.getAlvo(),
                    String.valueOf(r.getPosicao())));
        }
        l.add("");
        l.add("NORMAS MAIS MEXIDAS");
        l.add("norma;vezes");
        for (String[] m : app.getGrafo().maisAlteradas()) {
            l.add(m[0] + ";" + m[1]);
        }
        return l;
    }

    private ArrayList<String> linhasDeAtos() {
        ArrayList<String> l = new ArrayList<>();
        l.add("ATOS");
        l.add("identidade;especie;decisao;motivo;fatos;leitura;titulo");
        for (Diario d : app.getDiarios()) {
            for (Ato a : d.getAtos()) {
                l.add(String.join(";", a.getIdentidade(), a.getEspecie(), a.getDecisao(),
                        a.getMotivo().replace(';', ','), String.valueOf(a.getFatos().size()),
                        app.leitor().ler(a, d.getData()).replace(';', ','),
                        a.getTitulo().replace(';', ',')));
            }
        }
        return l;
    }

    private int lerInteiro(Scanner teclado, int padrao) {
        String entrada = teclado.hasNextLine() ? teclado.nextLine().trim() : "";
        if (entrada.isEmpty()) {
            return padrao;
        }
        try {
            return Integer.parseInt(entrada);
        } catch (NumberFormatException e) {
            System.out.println("  numero invalido, usando " + padrao + ".");
            return padrao;
        }
    }

    private static String corta(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max - 1) + ".";
    }
}
