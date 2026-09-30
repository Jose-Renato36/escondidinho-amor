package diarios.etapas;

import diarios.analise.Cobertura;
import diarios.analise.GrafoNormas;
import diarios.analise.Interpretacao;
import diarios.analise.PainelContratos;
import diarios.modelo.Achado;
import diarios.modelo.Ato;
import diarios.modelo.Contrato;
import diarios.modelo.Diario;
import diarios.modelo.Relacao;
import java.io.IOException;
import java.util.ArrayList;

public class PaginaHtml {

    private final Relatorio relatorio;

    public PaginaHtml() {
        this.relatorio = null;
    }

    public PaginaHtml(Relatorio relatorio) {
        this.relatorio = relatorio;
    }

    public String gerar(ArrayList<Diario> diarios, Cobertura cobertura,
                        PainelContratos painel, GrafoNormas grafo,
                        Interpretacao interpretacao) throws IOException {
        ArrayList<String> h = new ArrayList<>();

        h.add("<!DOCTYPE html>");
        h.add("<html lang=\"pt-BR\">");
        h.add("<head>");
        h.add("<meta charset=\"UTF-8\">");
        h.add("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">");
        h.add("<title>Diarios Oficiais - resultado</title>");
        h.add("<style>" + estilo() + "</style>");
        h.add("</head>");
        h.add("<body>");
        h.add("<div class=\"viz-root\">");

        cabecalho(h, diarios, cobertura, painel, grafo);
        secaoInterpretacao(h, interpretacao);
        secaoCobertura(h, diarios, cobertura);
        secaoFunil(h, diarios, cobertura);
        secaoPortao(h, cobertura);
        secaoFornecedores(h, painel);
        secaoGrafo(h, grafo);
        secaoTabela(h, painel);
        rodape(h);

        h.add("</div>");
        h.add("</body>");
        h.add("</html>");

        return relatorio.gravar("painel.html", h);
    }

    public void cabecalho(ArrayList<String> h, ArrayList<Diario> diarios, Cobertura c,
                           PainelContratos painel, GrafoNormas grafo) {
        h.add("<header>");
        h.add("<h1>Diarios Oficiais municipais</h1>");
        h.add("<p class=\"sub\">Leitura, separacao em atos e analise &mdash; "
                + c.getLidos() + " diario(s) processado(s)</p>");
        h.add("</header>");
        h.add("<section class=\"tiles\">");
        tile(h, String.valueOf(c.getTotalAtos()), "atos separados",
                c.getUtilizaveis() + " diario(s) utilizavel(is)");
        tile(h, String.valueOf(painel.getContratos().size()), "contratacoes",
                Contrato.formatarReal(painel.total()) + " no total");
        tile(h, String.valueOf(grafo.getArestas().size()), "relacoes entre normas",
                grafo.doTipo(Relacao.ALTERA).size() + " alteracoes reais");
        tile(h, String.valueOf(c.getRecusados()), "diario(s) recusado(s)",
                "sem texto aproveitavel");
        h.add("</section>");
    }

    private void tile(ArrayList<String> h, String numero, String rotulo, String apoio) {
        h.add("<div class=\"tile\">");
        h.add("<div class=\"tile-num\">" + escapar(numero) + "</div>");
        h.add("<div class=\"tile-lab\">" + escapar(rotulo) + "</div>");
        h.add("<div class=\"tile-sub\">" + escapar(apoio) + "</div>");
        h.add("</div>");
    }
    public void secaoInterpretacao(ArrayList<String> h, Interpretacao i) {
        if (i.getAchados().isEmpty() && i.getResumo().isEmpty()) {
            return;
        }
        h.add("<section class=\"leitura\">");
        h.add("<h2>O que esses dados dizem</h2>");
        h.add("<p class=\"resumo\">" + escapar(i.getResumo()) + "</p>");

        blocoDeAchados(h, "Leitura do periodo", i.doNivel(Achado.LEITURA), "ok");
        blocoDeAchados(h, "Onde vale a pena olhar", i.doNivel(Achado.ATENCAO), "duv");
        blocoDeAchados(h, "Limites desta leitura", i.doNivel(Achado.LIMITE), "rej");

        h.add("<p class=\"nota\">Cada frase acima sai de uma regra no codigo, sobre "
                + "numero medido no texto dos diarios. Nenhuma foi escrita a mao.</p>");
        h.add("</section>");
    }

    private void blocoDeAchados(ArrayList<String> h, String titulo,
                                ArrayList<Achado> lista, String classe) {
        if (lista.isEmpty()) {
            return;
        }
        h.add("<h3>" + escapar(titulo) + "</h3>");
        for (Achado a : lista) {
            h.add("<div class=\"achado achado-" + classe + "\">");
            h.add("<strong>" + escapar(a.getTitulo()) + "</strong>");
            h.add("<p>" + escapar(a.getFrase()) + "</p>");
            h.add("<p class=\"evidencia\">" + escapar(a.getEvidencia()) + "</p>");
            h.add("</div>");
        }
    }

    public void secaoCobertura(ArrayList<String> h, ArrayList<Diario> diarios, Cobertura c) {
        h.add("<section>");
        h.add("<h2>Cobertura</h2>");
        h.add("<p class=\"nota\">O que o programa conseguiu ler &mdash; e o que nao "
                + "conseguiu, com o motivo. &quot;Zero atos&quot; e &quot;nao consegui "
                + "ler&quot; sao coisas diferentes.</p>");

        for (Diario d : diarios) {
            boolean ok = d.isUtilizavel();
            h.add("<div class=\"card " + (ok ? "card-ok" : "card-no") + "\">");
            h.add("<div class=\"card-top\">");
            h.add("<span class=\"sinal\">" + (ok ? "&#10003;" : "&#10007;") + "</span>");
            h.add("<strong>" + escapar(d.getArquivo()) + "</strong>");
            h.add("<span class=\"pill " + (ok ? "pill-ok" : "pill-no") + "\">"
                    + (ok ? "PROCESSADO" : "RECUSADO") + "</span>");
            h.add("</div>");
            if (ok) {
                h.add("<div class=\"card-corpo\">" + d.getAtos().size() + " atos &middot; "
                        + d.getLinhasRemovidas() + " linhas de lixo removidas de "
                        + d.getLinhasOriginais() + "</div>");
            } else {
                h.add("<div class=\"card-corpo\">" + escapar(d.getMotivoRecusa()) + "</div>");
            }
            h.add("</div>");
        }
        h.add("</section>");
    }
    public void secaoFunil(ArrayList<String> h, ArrayList<Diario> diarios, Cobertura c) {
        Diario d = null;
        for (Diario cand : diarios) {
            if (cand.isUtilizavel()) {
                d = cand;
                break;
            }
        }
        if (d == null) {
            return;
        }

        h.add("<section>");
        h.add("<h2>O funil, etapa a etapa</h2>");
        h.add("<p class=\"nota\">" + escapar(d.getArquivo()) + "</p>");

        int linhas = d.getLinhasOriginais();
        int limpas = linhas - d.getLinhasRemovidas();
        int atos = d.getAtos().size();

        h.add("<div class=\"funil\">");
        etapaFunil(h, "1. Limpeza", limpas, linhas,
                d.getLinhasRemovidas() + " linhas descartadas (marca d'agua da conversao de PDF)");
        etapaFunil(h, "2. Fragmentacao", atos, d.getTitulosCandidatos(),
                d.getTitulosCandidatos() + " titulos candidatos, " + d.getTitulosDescartados()
                + " descartados por nao terem corpo de texto");
        etapaFunil(h, "3. Portao", d.contar(Ato.ACEITO), atos,
                d.contar(Ato.ACEITO) + " aceitos de " + atos + " atos");
        h.add("</div>");
        h.add("</section>");
    }

    private void etapaFunil(ArrayList<String> h, String nome, int resta, int total, String nota) {
        int pct = total > 0 ? (resta * 100 / total) : 0;
        h.add("<div class=\"etapa\">");
        h.add("<div class=\"etapa-cab\"><span>" + escapar(nome) + "</span>"
                + "<span class=\"etapa-num\">" + resta + " / " + total + "</span></div>");
        h.add("<div class=\"trilho\" title=\"" + escapar(nota) + "\">");
        h.add("<div class=\"barra\" style=\"width:" + pct + "%\"></div>");
        h.add("</div>");
        h.add("<div class=\"etapa-nota\">" + escapar(nota) + "</div>");
        h.add("</div>");
    }
    public void secaoPortao(ArrayList<String> h, Cobertura c) {
        int total = c.getTotalAtos();
        if (total == 0) {
            return;
        }
        h.add("<section>");
        h.add("<h2>O portao: tres saidas</h2>");
        h.add("<p class=\"nota\">Com duas saidas, o caso duvidoso e empurrado para um "
                + "dos lados e some. Com a terceira, ele fica contavel.</p>");
        h.add("<div class=\"portao\">");
        faixaPortao(h, "&#10003;", "ACEITO", c.getAceitos(), total, "ok",
                "teve sinal positivo: numero, valor/CNPJ ou norma citada, mais data");
        faixaPortao(h, "&#9888;", "DUVIDOSO", c.getDuvidosos(), total, "duv",
                "passou, mas faltou evidencia - e a lista do que o programa ainda nao trata");
        faixaPortao(h, "&#10007;", "REJEITADO", c.getRejeitados(), total, "rej",
                "reprovou em checagem definitiva, com o motivo gravado");
        h.add("</div>");
        h.add("</section>");
    }

    private void faixaPortao(ArrayList<String> h, String icone, String nome, int n,
                             int total, String classe, String explicacao) {
        int pct = total > 0 ? (n * 100 / total) : 0;
        h.add("<div class=\"faixa\">");
        h.add("<div class=\"faixa-cab\">");
        h.add("<span class=\"ic ic-" + classe + "\">" + icone + "</span>");
        h.add("<strong>" + nome + "</strong>");
        h.add("<span class=\"faixa-num\">" + n + " <span class=\"pct\">(" + pct + "%)</span></span>");
        h.add("</div>");
        h.add("<div class=\"trilho\"><div class=\"barra b-" + classe
                + "\" style=\"width:" + pct + "%\"></div></div>");
        h.add("<div class=\"etapa-nota\">" + escapar(explicacao) + "</div>");
        h.add("</div>");
    }
    public void secaoFornecedores(ArrayList<String> h, PainelContratos painel) {
        ArrayList<String[]> ranking = painel.ranking();
        if (ranking.isEmpty()) {
            return;
        }
        h.add("<section>");
        h.add("<h2>Maiores fornecedores</h2>");
        h.add("<p class=\"nota\">Somado por CNPJ &mdash; o nome da empresa aparece "
                + "escrito de varias formas no diario, o CNPJ nao.</p>");

        double maior = valorDe(ranking.get(0)[2]);
        int mostrar = Math.min(10, ranking.size());

        h.add("<div class=\"grafico\">");
        for (int i = 0; i < mostrar; i++) {
            String[] r = ranking.get(i);
            double v = valorDe(r[2]);
            int pct = maior > 0 ? (int) Math.round(v * 100 / maior) : 0;
            String dica = r[1] + " - " + r[2] + " em " + r[3] + " contrato(s)";

            h.add("<div class=\"linha\">");
            h.add("<div class=\"rotulo\" title=\"" + escapar(r[0]) + "\">"
                    + escapar(cortar(r[1], 34)) + "</div>");
            h.add("<div class=\"trilho\" data-dica=\"" + escapar(dica) + "\">");
            h.add("<div class=\"barra\" style=\"width:" + Math.max(pct, 1) + "%\"></div>");
            h.add("</div>");
            h.add("<div class=\"valor\">" + escapar(r[2]) + "</div>");
            h.add("</div>");
        }
        h.add("</div>");

        ArrayList<String[]> repetidos = painel.repeticoes(3);
        if (!repetidos.isEmpty()) {
            h.add("<div class=\"aviso\">");
            h.add("<span class=\"ic ic-duv\">&#9888;</span>");
            h.add("<div><strong>Aparecem 3 vezes ou mais no periodo</strong>");
            h.add("<p class=\"nota\">E uma contagem, nao uma acusacao: repeticao tem "
                    + "explicacao legitima (ata de registro de precos, fornecedor unico). "
                    + "Serve para saber onde olhar.</p>");
            h.add("<ul>");
            for (String[] r : repetidos) {
                h.add("<li><strong>" + escapar(r[3]) + "x</strong> "
                        + escapar(r[1]) + " <span class=\"mono\">" + escapar(r[0])
                        + "</span> &mdash; " + escapar(r[2]) + "</li>");
            }
            h.add("</ul></div></div>");
        }
        h.add("</section>");
    }

    public void secaoGrafo(ArrayList<String> h, GrafoNormas grafo) {
        if (grafo.getArestas().isEmpty()) {
            return;
        }
        h.add("<section>");
        h.add("<h2>Grafo de normas</h2>");
        h.add("<p class=\"nota\">Nem toda mencao a uma norma e uma acao sobre ela. "
                + "A maior parte das vezes em que aparece &quot;altera&quot; ou "
                + "&quot;prorroga&quot; num diario municipal e clausula de contrato "
                + "(&quot;alteracao dos precos&quot;, &quot;prorrogacao da vigencia da "
                + "Ata&quot;), nao norma. Por isso a classificacao comeca eliminando: "
                + "citacao de fundamento legal e forma nao-dispositiva viram apenas "
                + "CITA.</p>");

        h.add("<div class=\"contagem\">");
        contar(h, "ALTERA", grafo.doTipo(Relacao.ALTERA).size());
        contar(h, "REVOGA", grafo.doTipo(Relacao.REVOGA).size());
        contar(h, "PRORROGA", grafo.doTipo(Relacao.PRORROGA).size());
        contar(h, "CITA", grafo.doTipo(Relacao.CITA).size());
        h.add("</div>");

        ArrayList<String[]> mexidas = grafo.maisAlteradas();
        if (!mexidas.isEmpty()) {
            h.add("<h3>Normas mais mexidas</h3>");
            h.add("<table><thead><tr><th>norma</th><th class=\"num\">vezes</th>"
                    + "</tr></thead><tbody>");
            int mostrar = Math.min(8, mexidas.size());
            for (int i = 0; i < mostrar; i++) {
                h.add("<tr><td class=\"mono\">" + escapar(mexidas.get(i)[0]) + "</td>"
                        + "<td class=\"num\">" + escapar(mexidas.get(i)[1]) + "</td></tr>");
            }
            h.add("</tbody></table>");
        }

        h.add("<h3>Relacoes que operam sobre a norma</h3>");
        h.add("<table><thead><tr><th>ato</th><th>acao</th><th>norma</th>"
                + "</tr></thead><tbody>");
        int mostradas = 0;
        for (Relacao r : grafo.getArestas()) {
            if (r.getAcao().equals(Relacao.CITA) || mostradas >= 10) {
                continue;
            }
            h.add("<tr><td class=\"mono\">" + escapar(r.getOrigem()) + "</td>"
                    + "<td><span class=\"tag\">" + escapar(r.getAcao()) + "</span></td>"
                    + "<td class=\"mono\">" + escapar(r.getAlvo()) + "</td></tr>");
            mostradas++;
        }
        if (mostradas == 0) {
            h.add("<tr><td colspan=\"3\">nenhuma relacao de acao neste periodo "
                    + "&mdash; so citacoes de fundamento legal</td></tr>");
        }
        h.add("</tbody></table>");
        h.add("</section>");
    }

    private void contar(ArrayList<String> h, String nome, int n) {
        h.add("<div class=\"cont\"><div class=\"cont-num\">" + n + "</div>"
                + "<div class=\"cont-lab\">" + nome + "</div></div>");
    }
    public void secaoTabela(ArrayList<String> h, PainelContratos painel) {
        ArrayList<Contrato> contratos = painel.getContratos();
        if (contratos.isEmpty()) {
            return;
        }
        h.add("<section>");
        h.add("<h2>Todas as contratacoes</h2>");
        h.add("<p class=\"nota\">" + contratos.size() + " registros. Os mesmos dados "
                + "das barras acima, em texto.</p>");
        h.add("<table><thead><tr><th>CNPJ</th><th>contratada</th>"
                + "<th class=\"num\">valor</th><th>processo</th></tr></thead><tbody>");
        for (Contrato c : contratos) {
            h.add("<tr><td class=\"mono\">" + escapar(c.getCnpj()) + "</td>"
                    + "<td>" + escapar(c.getContratada()) + "</td>"
                    + "<td class=\"num\">" + escapar(Contrato.formatarReal(c.getValor())) + "</td>"
                    + "<td class=\"mono\">" + escapar(c.getProcesso()) + "</td></tr>");
        }
        h.add("</tbody></table>");
        h.add("</section>");
    }

    private void rodape(ArrayList<String> h) {
        h.add("<footer>");
        h.add("<p>Pagina gerada pelo proprio programa Java &mdash; a classe "
                + "<span class=\"mono\">PaginaHtml</span> monta este arquivo com "
                + "concatenacao de String e o grava com BufferedWriter. "
                + "Quem desenha e o navegador.</p>");
        h.add("<p>Dados: <span class=\"mono\">Querido Diario</span>, "
                + "Open Knowledge Brasil.</p>");
        h.add("</footer>");
    }
    private double valorDe(String formatado) {
        StringBuilder n = new StringBuilder();
        for (int i = 0; i < formatado.length(); i++) {
            char c = formatado.charAt(i);
            if (c >= '0' && c <= '9') {
                n.append(c);
            } else if (c == ',') {
                n.append('.');
            }
        }
        try {
            return Double.parseDouble(n.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    public static String cortar(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
    public static String escapar(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
    public String estilo() {
        return String.join("\n",
        ":root{",
        "  --surface:#fcfcfb; --plano:#f9f9f7;",
        "  --ink:#0b0b0b; --ink2:#52514e; --ink3:#898781;",
        "  --grade:#e1e0d9; --eixo:#c3c2b7;",
        "  --serie:#2a78d6;",
        "  --ok:#0ca30c; --duv:#fab219; --rej:#d03b3b;",
        "  color-scheme:light;",
        "}",
        "@media (prefers-color-scheme:dark){ :root:not([data-theme=\"light\"]){",
        "  --surface:#1a1a19; --plano:#0d0d0d;",
        "  --ink:#ffffff; --ink2:#c3c2b7; --ink3:#898781;",
        "  --grade:#2c2c2a; --eixo:#383835;",
        "  --serie:#3987e5;",
        "  color-scheme:dark;",
        "}}",
        ":root[data-theme=\"dark\"]{",
        "  --surface:#1a1a19; --plano:#0d0d0d;",
        "  --ink:#ffffff; --ink2:#c3c2b7; --ink3:#898781;",
        "  --grade:#2c2c2a; --eixo:#383835;",
        "  --serie:#3987e5;",
        "  color-scheme:dark;",
        "}",
        "*{box-sizing:border-box}",
        "body{margin:0;background:var(--plano);color:var(--ink);",
        "  font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;",
        "  font-size:15px;line-height:1.55}",
        ".viz-root{max-width:940px;margin:0 auto;padding:32px 16px 64px}",
        "header{padding:8px 0 24px;border-bottom:1px solid var(--grade);margin-bottom:28px}",
        "h1{font-size:26px;margin:0 0 4px;letter-spacing:-.01em}",
        "h2{font-size:19px;margin:0 0 6px;letter-spacing:-.01em}",
        "h3{font-size:15px;margin:22px 0 8px;color:var(--ink2)}",
        ".sub{margin:0;color:var(--ink2)}",
        ".nota{margin:0 0 16px;color:var(--ink2);font-size:13.5px;max-width:70ch}",
        "section{background:var(--surface);border:1px solid var(--grade);",
        "  border-radius:10px;padding:22px;margin-bottom:18px}",
        ".tiles{display:grid;grid-template-columns:repeat(auto-fit,minmax(170px,1fr));",
        "  gap:12px;background:none;border:none;padding:0}",
        ".tile{background:var(--surface);border:1px solid var(--grade);",
        "  border-radius:10px;padding:16px}",
        ".tile-num{font-size:30px;font-weight:650;letter-spacing:-.02em;line-height:1.1}",
        ".tile-lab{font-size:13.5px;color:var(--ink);margin-top:2px}",
        ".tile-sub{font-size:12.5px;color:var(--ink3);margin-top:2px}",
        ".card{border:1px solid var(--grade);border-radius:8px;padding:12px 14px;",
        "  margin-bottom:10px;border-left-width:3px}",
        ".card-ok{border-left-color:var(--ok)} .card-no{border-left-color:var(--rej)}",
        ".card-top{display:flex;align-items:center;gap:8px;flex-wrap:wrap}",
        ".card-corpo{color:var(--ink2);font-size:13.5px;margin-top:4px}",
        ".sinal{font-weight:700}",
        ".card-ok .sinal{color:var(--ok)} .card-no .sinal{color:var(--rej)}",
        ".pill{font-size:11px;letter-spacing:.06em;padding:2px 8px;border-radius:99px;",
        "  border:1px solid var(--eixo);color:var(--ink2)}",
        ".trilho{position:relative;height:10px;background:var(--grade);",
        "  border-radius:5px;overflow:hidden}",
        ".barra{height:100%;background:var(--serie);border-radius:0 4px 4px 0}",
        ".b-ok{background:var(--ok)} .b-duv{background:var(--duv)} .b-rej{background:var(--rej)}",
        ".etapa{margin-bottom:16px}",
        ".etapa-cab{display:flex;justify-content:space-between;font-size:13.5px;",
        "  margin-bottom:5px}",
        ".etapa-num{color:var(--ink2);font-variant-numeric:tabular-nums}",
        ".etapa-nota{font-size:12.5px;color:var(--ink3);margin-top:5px}",
        ".faixa{margin-bottom:16px}",
        ".faixa-cab{display:flex;align-items:center;gap:8px;margin-bottom:5px;font-size:14px}",
        ".faixa-num{margin-left:auto;font-variant-numeric:tabular-nums}",
        ".pct{color:var(--ink3)}",
        ".ic{font-weight:700}",
        ".ic-ok{color:var(--ok)} .ic-duv{color:var(--duv)} .ic-rej{color:var(--rej)}",
        ".grafico{display:flex;flex-direction:column;gap:9px}",
        ".linha{display:grid;grid-template-columns:200px 1fr 120px;gap:12px;",
        "  align-items:center}",
        ".rotulo{font-size:13px;color:var(--ink2);overflow:hidden;",
        "  text-overflow:ellipsis;white-space:nowrap}",
        ".valor{font-size:13px;text-align:right;font-variant-numeric:tabular-nums}",
        ".linha .trilho{height:14px;border-radius:4px}",
        ".trilho[data-dica]:hover::after{content:attr(data-dica);position:absolute;",
        "  left:0;top:-30px;background:var(--ink);color:var(--surface);",
        "  padding:4px 8px;border-radius:5px;font-size:12px;white-space:nowrap;z-index:5}",
        ".aviso{display:flex;gap:10px;margin-top:20px;padding:14px;",
        "  border:1px solid var(--grade);border-left:3px solid var(--duv);",
        "  border-radius:8px}",
        ".aviso ul{margin:8px 0 0;padding-left:18px}",
        ".aviso li{font-size:13.5px;margin-bottom:3px}",
        ".contagem{display:grid;grid-template-columns:repeat(auto-fit,minmax(110px,1fr));",
        "  gap:10px;margin-bottom:6px}",
        ".cont{border:1px solid var(--grade);border-radius:8px;padding:12px;",
        "  text-align:center}",
        ".cont-num{font-size:22px;font-weight:650}",
        ".cont-lab{font-size:11.5px;color:var(--ink3);letter-spacing:.06em}",
        "table{width:100%;border-collapse:collapse;font-size:13px;margin-top:6px}",
        "th{text-align:left;color:var(--ink3);font-weight:500;font-size:11.5px;",
        "  letter-spacing:.06em;text-transform:uppercase;padding:6px 8px;",
        "  border-bottom:1px solid var(--eixo)}",
        "td{padding:6px 8px;border-bottom:1px solid var(--grade)}",
        "th.num,td.num{text-align:right;font-variant-numeric:tabular-nums}",
        ".mono{font-family:ui-monospace,SFMono-Regular,Menlo,monospace;font-size:12px;",
        "  color:var(--ink2)}",
        ".tag{font-size:11px;letter-spacing:.05em;padding:2px 7px;border-radius:4px;",
        "  border:1px solid var(--eixo);color:var(--ink2)}",
        ".resumo{font-size:16.5px;line-height:1.5;margin:0 0 18px;color:var(--ink)}",
        ".achado{border:1px solid var(--grade);border-left-width:3px;border-radius:8px;",
        "  padding:12px 14px;margin-bottom:10px}",
        ".achado-ok{border-left-color:var(--ok)}",
        ".achado-duv{border-left-color:var(--duv)}",
        ".achado-rej{border-left-color:var(--rej)}",
        ".achado p{margin:5px 0 0;font-size:13.5px;color:var(--ink2)}",
        ".achado .evidencia{font-family:ui-monospace,SFMono-Regular,Menlo,monospace;",
        "  font-size:12px;color:var(--ink3);margin-top:6px}",
        "footer{color:var(--ink3);font-size:12.5px;margin-top:24px}",
        "footer p{margin:4px 0}",
        "@media (max-width:640px){",
        "  .linha{grid-template-columns:1fr;gap:4px}",
        "  .valor{text-align:left}",
        "  .viz-root{padding:20px 16px 48px}",
        "}");
    }
}
