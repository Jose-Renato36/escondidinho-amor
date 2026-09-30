package diarios.servidor;

import diarios.Aplicacao;
import diarios.analise.Assunto;
import diarios.analise.LeituraDoAto;
import diarios.etapas.PaginaHtml;
import diarios.etapas.Privacidade;
import diarios.extratores.Extrator;
import diarios.modelo.Ato;
import diarios.modelo.Diario;
import diarios.modelo.Fato;
import java.util.ArrayList;

public class PaginaDiario {

    private static final int LIMITE_TEXTO_BRUTO = 60000;
    private static final int LIMITE_RESULTADOS = 200;
    private static final int MARGEM_DA_PROVA = 70;

    private final Aplicacao app;

    public PaginaDiario(Aplicacao app) {
        this.app = app;
    }

    public String diario(String arquivo) {
        Diario d = app.diarioDoArquivo(arquivo);
        ArrayList<String> h = abrir(arquivo);
        h.add("<a class=\"voltar\" href=\"/\">&larr; painel</a>");
        if (d == null) {
            h.add("<p>Esse arquivo nao esta entre os diarios lidos.</p>");
            return fechar(h);
        }

        h.add("<h1>" + esc(arquivo) + "</h1>");
        if (!d.isUtilizavel()) {
            h.add("<div class=\"estado estado-aviso\">RECUSADO: " + esc(d.getMotivoRecusa())
                    + "</div>");
            h.add("<p class=\"nota\">O texto abaixo e exatamente o que veio da fonte. Repare que "
                    + "quase so tem cabecalho e rodape de pagina - o conteudo esta em imagem.</p>");
            h.add("<pre class=\"texto\">" + esc(Privacidade.mascararCpf(
                    recortar(d.getTextoBruto(), LIMITE_TEXTO_BRUTO))) + "</pre>");
            return fechar(h);
        }

        h.add("<p class=\"nota\">" + d.getAtos().size() + " atos &middot; "
                + d.getTitulosCandidatos() + " titulos candidatos, " + d.getTitulosDescartados()
                + " descartados sem corpo &middot; " + d.getLinhasRemovidas()
                + " linhas de lixo removidas de " + d.getLinhasOriginais() + "</p>");

        LeituraDoAto leitor = app.leitor();
        h.add("<h2>Atos e o que cada um faz</h2>");
        h.add("<table><thead><tr><th>#</th><th>decisao</th><th>assunto</th><th>titulo</th>"
                + "<th>leitura</th></tr></thead><tbody>");
        for (int i = 0; i < d.getAtos().size(); i++) {
            Ato a = d.getAtos().get(i);
            h.add("<tr><td class=\"mono\">" + i + "</td>"
                    + "<td><span class=\"tag tag-" + classeDaDecisao(a.getDecisao()) + "\">"
                    + a.getDecisao() + "</span></td>"
                    + "<td class=\"mono\">" + Assunto.de(a) + "</td>"
                    + "<td><a href=\"" + linkDoAto(arquivo, i) + "\">"
                    + esc(PaginaHtml.cortar(a.getTitulo(), 60)) + "</a></td>"
                    + "<td class=\"leitura-curta\">" + esc(leitor.ler(a, d.getData())) + "</td></tr>");
        }
        h.add("</tbody></table>");

        h.add("<details class=\"log\"><summary>Texto como veio da fonte (bruto)</summary>");
        h.add("<pre class=\"texto\">" + esc(Privacidade.mascararCpf(
                recortar(d.getTextoBruto(), LIMITE_TEXTO_BRUTO))) + "</pre></details>");
        h.add("<details class=\"log\"><summary>Texto depois da limpeza</summary>");
        h.add("<pre class=\"texto\">" + esc(Privacidade.mascararCpf(
                recortar(d.getTextoLimpo(), LIMITE_TEXTO_BRUTO))) + "</pre></details>");
        return fechar(h);
    }

    public String ato(String arquivo, int indice) {
        Diario d = app.diarioDoArquivo(arquivo);
        ArrayList<String> h = abrir("ato " + indice);
        h.add("<a class=\"voltar\" href=\"/diario?arquivo=" + Formulario.codificar(arquivo)
                + "\">&larr; " + esc(arquivo) + "</a>");
        if (d == null || indice < 0 || indice >= d.getAtos().size()) {
            h.add("<p>Ato nao encontrado.</p>");
            return fechar(h);
        }

        Ato ato = d.getAtos().get(indice);
        h.add("<h1>" + esc(Privacidade.mascararCpf(ato.getTitulo())) + "</h1>");
        h.add("<div class=\"leitura-ato\"><div class=\"rotulo-leitura\">O que este ato faz</div>"
                + esc(app.leitor().ler(ato, d.getData())) + "</div>");

        h.add("<table class=\"ficha\"><tbody>");
        linha(h, "identidade", "<span class=\"mono\">" + esc(ato.getIdentidade()) + "</span>");
        linha(h, "assunto", Assunto.de(ato));
        linha(h, "decisao", "<span class=\"tag tag-" + classeDaDecisao(ato.getDecisao()) + "\">"
                + ato.getDecisao() + "</span> " + esc(ato.getMotivo()));
        linha(h, "posicao no diario", "caracteres " + ato.getPosicao() + " a "
                + (ato.getPosicao() + ato.getTexto().length()));
        h.add("</tbody></table>");

        h.add("<h2>Fatos e a prova de cada um</h2>");
        h.add("<p class=\"nota\">Cada fato guarda a posicao exata onde foi achado. O trecho "
                + "abaixo e recortado do texto original a partir dessa posicao - e a prova de "
                + "que o programa nao inventou nada.</p>");
        if (ato.getFatos().isEmpty()) {
            h.add("<p class=\"nota\">Nenhum fato extraido deste ato.</p>");
        }
        for (Fato f : ato.getFatos()) {
            h.add("<div class=\"fato\"><span class=\"tag\">" + f.getTipo() + "</span> "
                    + "<strong>" + esc(Privacidade.mascararCpf(f.getValor())) + "</strong> "
                    + "<span class=\"mono pos\">[" + f.getInicio() + ".." + f.getFim() + "]</span>"
                    + "<div class=\"prova\">&hellip;" + esc(Privacidade.mascararCpf(
                    f.provar(d.getTextoLimpo(), MARGEM_DA_PROVA))) + "&hellip;</div></div>");
        }

        h.add("<h2>Texto do ato, com os fatos marcados</h2>");
        h.add("<pre class=\"texto\">" + destacar(ato) + "</pre>");
        return fechar(h);
    }

    public String busca(String termo) {
        ArrayList<String> h = abrir("busca");
        h.add("<a class=\"voltar\" href=\"/\">&larr; painel</a>");
        h.add("<h1>Busca</h1>");
        h.add("<form method=\"get\" action=\"/busca\" class=\"acoes\">"
                + "<input type=\"text\" name=\"q\" class=\"campo\" value=\"" + esc(termo) + "\">"
                + "<button type=\"submit\">Buscar</button></form>");
        if (termo.length() < 2) {
            h.add("<p class=\"nota\">Digite ao menos 2 letras.</p>");
            return fechar(h);
        }

        String alvo = Extrator.semAcento(termo.toLowerCase());
        int achados = 0;
        ArrayList<String> linhas = new ArrayList<>();

        for (Diario d : app.getDiarios()) {
            if (!d.isUtilizavel()) {
                continue;
            }
            String texto = d.getTextoLimpo();
            String base = Extrator.semAcento(texto.toLowerCase());
            int p = base.indexOf(alvo);
            while (p >= 0 && achados < LIMITE_RESULTADOS) {
                achados++;
                int ato = atoNaPosicao(d, p);
                String trecho = Privacidade.mascararCpf(
                        texto.substring(Math.max(0, p - 80), Math.min(texto.length(), p + alvo.length() + 80)))
                        .replace('\n', ' ');
                String destino = ato >= 0 ? linkDoAto(d.getArquivo(), ato)
                        : "/diario?arquivo=" + Formulario.codificar(d.getArquivo());
                linhas.add("<div class=\"fato\"><a href=\"" + destino + "\">" + esc(d.getArquivo())
                        + (ato >= 0 ? " &middot; ato " + ato : " &middot; fora de ato") + "</a>"
                        + "<div class=\"prova\">&hellip;" + esc(trecho) + "&hellip;</div></div>");
                p = base.indexOf(alvo, p + alvo.length());
            }
        }

        h.add("<p class=\"nota\">" + achados + (achados >= LIMITE_RESULTADOS ? "+" : "")
                + " ocorrencia(s) de <strong>" + esc(termo) + "</strong>.</p>");
        h.addAll(linhas);
        return fechar(h);
    }

    public static String paginaSimples(String titulo, String mensagem) {
        ArrayList<String> h = abrir(titulo);
        h.add("<a class=\"voltar\" href=\"/\">&larr; painel</a>");
        h.add("<h1>" + esc(titulo) + "</h1><p>" + esc(mensagem) + "</p>");
        return fechar(h);
    }

    private String destacar(Ato ato) {
        String texto = ato.getTexto();
        ArrayList<int[]> trechos = new ArrayList<>();
        for (Fato f : ato.getFatos()) {
            trechos.add(new int[] {f.getInicio() - ato.getPosicao(), f.getFim() - ato.getPosicao()});
        }
        ordenar(trechos);

        StringBuilder s = new StringBuilder();
        int cursor = 0;
        for (int[] t : trechos) {
            int ini = Math.max(0, Math.min(t[0], texto.length()));
            int fim = Math.max(ini, Math.min(t[1], texto.length()));
            if (ini < cursor) {
                continue;
            }
            s.append(esc(Privacidade.mascararCpf(texto.substring(cursor, ini))));
            s.append("<mark>").append(esc(Privacidade.mascararCpf(texto.substring(ini, fim))))
             .append("</mark>");
            cursor = fim;
        }
        s.append(esc(Privacidade.mascararCpf(texto.substring(cursor))));
        return s.toString();
    }

    private void ordenar(ArrayList<int[]> trechos) {
        for (int i = 1; i < trechos.size(); i++) {
            int[] atual = trechos.get(i);
            int j = i - 1;
            while (j >= 0 && trechos.get(j)[0] > atual[0]) {
                trechos.set(j + 1, trechos.get(j));
                j--;
            }
            trechos.set(j + 1, atual);
        }
    }

    private int atoNaPosicao(Diario d, int posicao) {
        for (int i = 0; i < d.getAtos().size(); i++) {
            Ato a = d.getAtos().get(i);
            if (posicao >= a.getPosicao() && posicao < a.getPosicao() + a.getTexto().length()) {
                return i;
            }
        }
        return -1;
    }

    private static String linkDoAto(String arquivo, int indice) {
        return "/ato?arquivo=" + Formulario.codificar(arquivo) + "&amp;indice=" + indice;
    }

    private void linha(ArrayList<String> h, String rotulo, String valor) {
        h.add("<tr><th>" + esc(rotulo) + "</th><td>" + valor + "</td></tr>");
    }

    private String classeDaDecisao(String decisao) {
        if (decisao.equals(Ato.ACEITO)) {
            return "ok";
        }
        return decisao.equals(Ato.REJEITADO) ? "rej" : "duv";
    }

    private String recortar(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "\n\n[... cortado para exibicao ...]";
    }

    private static ArrayList<String> abrir(String titulo) {
        ArrayList<String> h = new ArrayList<>();
        h.add("<!DOCTYPE html><html lang=\"pt-BR\"><head><meta charset=\"UTF-8\">");
        h.add("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">");
        h.add("<title>" + esc(titulo) + " - Diarios Oficiais</title>");
        h.add("<style>" + new PaginaHtml().estilo() + PainelWeb.estiloDoPainel() + estiloExtra()
                + "</style>");
        h.add("</head><body><div class=\"viz-root\">");
        PainelWeb.menu(h, "");
        return h;
    }

    private static String fechar(ArrayList<String> h) {
        h.add("</div></body></html>");
        return String.join("\n", h);
    }

    private static String esc(String s) {
        return PaginaHtml.escapar(s);
    }

    private static String estiloExtra() {
        return String.join("\n",
        ".voltar{display:inline-block;margin-bottom:14px;color:var(--ink2);text-decoration:none;",
        "  font-size:13px}",
        ".voltar:hover{text-decoration:underline}",
        "h1{font-size:21px;margin-bottom:12px}",
        "h2{margin-top:26px}",
        ".texto{white-space:pre-wrap;word-break:break-word;font-size:12.5px;line-height:1.6;",
        "  background:var(--surface);border:1px solid var(--grade);border-radius:8px;",
        "  padding:14px;max-height:620px;overflow:auto}",
        "mark{background:#fde68a;color:#111;border-radius:2px;padding:0 1px}",
        ".fato{border:1px solid var(--grade);border-radius:8px;padding:9px 12px;",
        "  margin-bottom:7px;background:var(--surface);font-size:13px}",
        ".fato .pos{margin-left:6px;font-size:11px;color:var(--ink3)}",
        ".prova{margin-top:5px;font-size:12.5px;color:var(--ink2)}",
        ".leitura-ato{font-size:16px;line-height:1.5;padding:14px 16px;border-radius:8px;",
        "  border:1px solid var(--grade);border-left:3px solid var(--serie);",
        "  background:var(--surface);margin-bottom:16px}",
        ".rotulo-leitura{font-size:11px;letter-spacing:.06em;text-transform:uppercase;",
        "  color:var(--ink3);margin-bottom:4px}",
        ".leitura-curta{font-size:12.5px;color:var(--ink2);max-width:380px}",
        ".ficha th{width:170px;text-transform:none;letter-spacing:0;font-size:12.5px}");
    }
}
