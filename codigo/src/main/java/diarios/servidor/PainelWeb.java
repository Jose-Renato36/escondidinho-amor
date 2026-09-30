package diarios.servidor;

import diarios.Aplicacao;
import diarios.analise.Cobertura;
import diarios.analise.PainelContratos;
import diarios.coleta.EstadoFonte;
import diarios.coleta.Municipio;
import diarios.coleta.Rodada;
import diarios.etapas.PaginaHtml;
import diarios.modelo.Ato;
import diarios.modelo.Contrato;
import diarios.modelo.Diario;
import java.util.ArrayList;

public class PainelWeb {

    private static final int SEGUNDOS_ENTRE_ATUALIZACOES = 3;

    private final Aplicacao app;
    private final PaginaHtml pagina = new PaginaHtml();

    public PainelWeb(Aplicacao app) {
        this.app = app;
    }

    public String montar(String aviso) {
        ArrayList<Diario> diarios = app.getDiarios();
        Cobertura cobertura = app.getCobertura();
        PainelContratos painel = app.getPainel();

        ArrayList<String> h = abrir("Painel");
        menu(h, "painel");
        pagina.cabecalho(h, diarios, cobertura, painel, app.getGrafo());
        barraDeEstado(h, aviso);

        if (diarios.isEmpty()) {
            h.add("<section><h2>Nenhum diario lido ainda</h2><p class=\"nota\">Va em "
                    + "<a href=\"/coleta\">Coleta e fontes</a>, ligue algumas fontes e clique em "
                    + "Coletar agora.</p></section>");
        } else {
            pagina.secaoInterpretacao(h, app.getInterpretacao());
            secaoPorMunicipio(h, diarios, painel);
            pagina.secaoFornecedores(h, painel);
            pagina.secaoGrafo(h, app.getGrafo());
            secaoDiarios(h, diarios);
            h.add("<h2 class=\"divisor\">Como o programa chegou nesses numeros</h2>");
            pagina.secaoFunil(h, diarios, cobertura);
            pagina.secaoPortao(h, cobertura);
            h.add("<details class=\"log\"><summary>Ver a lista completa de contratacoes ("
                    + painel.getContratos().size() + ")</summary>");
            pagina.secaoTabela(h, painel);
            h.add("</details>");
        }
        return fechar(h);
    }

    public String montarColeta(String aviso) {
        ArrayList<String> h = abrir("Coleta e fontes");
        menu(h, "coleta");
        h.add("<header><h1>Coleta e fontes</h1><p class=\"sub\">De onde vem o dado, e o que "
                + "aconteceu cada vez que o programa foi busca-lo.</p></header>");
        barraDeEstado(h, aviso);
        secaoColeta(h);
        secaoFontes(h);
        return fechar(h);
    }

    private ArrayList<String> abrir(String titulo) {
        ArrayList<String> h = new ArrayList<>();
        h.add("<!DOCTYPE html><html lang=\"pt-BR\"><head><meta charset=\"UTF-8\">");
        h.add("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">");
        if (app.isOcupado()) {
            h.add("<meta http-equiv=\"refresh\" content=\"" + SEGUNDOS_ENTRE_ATUALIZACOES + "\">");
        }
        h.add("<title>" + esc(titulo) + " - Diarios Oficiais</title>");
        h.add("<style>" + pagina.estilo() + estiloDoPainel() + "</style>");
        h.add("</head><body><div class=\"viz-root\">");
        return h;
    }

    private String fechar(ArrayList<String> h) {
        rodape(h);
        h.add("</div></body></html>");
        return String.join("\n", h);
    }

    static void menu(ArrayList<String> h, String ativo) {
        h.add("<nav class=\"menu\">");
        h.add("<a href=\"/\"" + (ativo.equals("painel") ? " class=\"ativo\"" : "") + ">Painel</a>");
        h.add("<a href=\"/coleta\"" + (ativo.equals("coleta") ? " class=\"ativo\"" : "")
                + ">Coleta e fontes</a>");
        h.add("<form method=\"get\" action=\"/busca\"><input type=\"text\" name=\"q\" "
                + "placeholder=\"buscar nos diarios...\"></form>");
        h.add("</nav>");
    }

    private void barraDeEstado(ArrayList<String> h, String aviso) {
        if (app.isOcupado()) {
            h.add("<div class=\"estado estado-ocupado\"><span class=\"girando\">&#9696;</span> "
                    + esc(app.getProgresso()) + " &mdash; a pagina se atualiza sozinha.</div>");
        } else if (aviso.equals("ocupado")) {
            h.add("<div class=\"estado estado-aviso\">Ja existe uma coleta em andamento. "
                    + "Espere ela terminar.</div>");
        } else {
            h.add("<div class=\"estado\">Ultima leitura da pasta: "
                    + esc(app.getUltimoProcessamento()) + "</div>");
        }
    }

    private void secaoColeta(ArrayList<String> h) {
        String desabilitar = app.isOcupado() ? " disabled" : "";
        h.add("<section id=\"coleta\">");
        h.add("<h2>Coleta</h2>");
        h.add("<p class=\"nota\">Baixa, das fontes ligadas, os diarios publicados nos ultimos N "
                + "dias. Fontes ja coletadas para o periodo (com dado, sem edicao ou so PDF) "
                + "sao puladas; so coleta as que falharam ou nunca foram tentadas. "
                + "Marque &quot;refazer todas&quot; para forcar. Diario ja baixado nunca e baixado de novo.</p>");
        h.add("<div class=\"acoes\">");
        h.add("<form method=\"post\" action=\"/coletar\">");
        h.add("<label>dias para tras <input type=\"number\" name=\"dias\" value=\"1\" min=\"1\" max=\""
                + Aplicacao.DIAS_MAXIMO + "\"></label>");
        h.add("<label title=\"Sem marcar, pula as fontes ja coletadas para esse periodo "
                + "(com dado, sem edicao ou so PDF) e coleta so as que falharam ou nunca foram "
                + "tentadas\"><input type=\"checkbox\" name=\"refazer\" value=\"sim\"> refazer todas</label>");
        h.add("<button type=\"submit\" class=\"primario\"" + desabilitar + ">Coletar agora</button>");
        h.add("</form>");
        h.add("<form method=\"post\" action=\"/descobrir\"><button type=\"submit\"" + desabilitar
                + " title=\"Pergunta a API quais municipios publicaram texto nos ultimos 3 dias\">"
                + "Descobrir municipios com publicacao recente</button></form>");
        h.add("<form method=\"post\" action=\"/processar\"><button type=\"submit\"" + desabilitar
                + ">Reler a pasta</button></form>");
        h.add("</div>");

        Rodada ultima = app.getUltimaRodada();
        if (ultima != null && !ultima.registro().isEmpty()) {
            h.add("<details class=\"log\"><summary>O que aconteceu na ultima rodada ("
                    + ultima.registro().size() + " linhas)</summary><pre>");
            for (String linha : ultima.registro()) {
                h.add(esc(linha));
            }
            h.add("</pre></details>");
        }

        ArrayList<Rodada> rodadas = app.rodadasRecentes();
        if (!rodadas.isEmpty()) {
            h.add("<h3>Historico de rodadas</h3>");
            h.add("<table><thead><tr><th>inicio</th><th>tipo</th><th>periodo</th>"
                    + "<th class=\"num\">fontes</th><th class=\"num\">puladas</th>"
                    + "<th class=\"num\">baixados</th>"
                    + "<th class=\"num\">ja tinha</th><th class=\"num\">falhas</th>"
                    + "<th>observacao</th></tr></thead><tbody>");
            for (Rodada r : rodadas) {
                h.add("<tr><td class=\"mono\">" + esc(r.getInicio()) + "</td>"
                        + "<td>" + esc(r.getTipo()) + "</td>"
                        + "<td class=\"mono\">" + esc(r.getDesde()) + " a " + esc(r.getAte()) + "</td>"
                        + "<td class=\"num\">" + r.fontes() + "</td>"
                        + "<td class=\"num\">" + r.puladas() + "</td>"
                        + "<td class=\"num\">" + r.baixados() + "</td>"
                        + "<td class=\"num\">" + r.jaExistiam() + "</td>"
                        + "<td class=\"num" + (r.falhas() > 0 ? " ruim" : "") + "\">" + r.falhas() + "</td>"
                        + "<td>" + esc(r.getObservacao()) + "</td></tr>");
            }
            h.add("</tbody></table>");
        }
        h.add("</section>");
    }

    private void secaoFontes(ArrayList<String> h) {
        ArrayList<EstadoFonte> todas = app.getFontes().todas();
        int ligadas = 0;
        int comDado = 0;
        for (EstadoFonte e : todas) {
            if (e.isLigada()) {
                ligadas++;
            }
            if (e.getStatus().equals(EstadoFonte.COM_DADO)) {
                comDado++;
            }
        }

        h.add("<section id=\"fontes\">");
        h.add("<h2>Fontes</h2>");
        h.add("<p class=\"nota\">" + ligadas + " de " + todas.size() + " ligadas &middot; "
                + comDado + " com conteudo confirmado. So as ligadas entram na proxima coleta. "
                + "Clique no botao da linha para ligar ou desligar. O estado fica salvo em "
                + "<span class=\"mono\">dados/estado/fontes.txt</span>.</p>");
        h.add("<div class=\"acoes\">");
        h.add("<form method=\"post\" action=\"/fonte/todas\"><input type=\"hidden\" name=\"ligar\" "
                + "value=\"sim\"><button type=\"submit\">ligar todas</button></form>");
        h.add("<form method=\"post\" action=\"/fonte/todas\"><input type=\"hidden\" name=\"ligar\" "
                + "value=\"nao\"><button type=\"submit\">desligar todas</button></form>");
        h.add("</div>");

        h.add("<div class=\"legenda\">");
        legenda(h, "ok", "COM_DADO", "baixou ou ja tinha diario com texto");
        legenda(h, "duv", "SEM_EDICAO / SO_PDF", "respondeu, mas sem texto no periodo");
        legenda(h, "rej", "FALHOU", "a rede ou a API nao respondeu");
        legenda(h, "neutro", "NUNCA_TENTADA", "ainda nao foi coletada");
        h.add("</div>");

        ArrayList<EstadoFonte> ligadasLista = new ArrayList<>();
        ArrayList<EstadoFonte> desligadas = new ArrayList<>();
        for (EstadoFonte e : todas) {
            if (e.isLigada()) {
                ligadasLista.add(e);
            } else {
                desligadas.add(e);
            }
        }
        h.add("<h3>Ligadas (" + ligadasLista.size() + ")</h3>");
        tabelaDeFontes(h, ligadasLista);
        h.add("<details class=\"log\"><summary>Desligadas (" + desligadas.size()
                + ") &mdash; clique para ver e ligar</summary>");
        tabelaDeFontes(h, desligadas);
        h.add("</details>");
        h.add("</section>");
    }

    private void tabelaDeFontes(ArrayList<String> h, ArrayList<EstadoFonte> lista) {
        if (lista.isEmpty()) {
            h.add("<p class=\"nota\">nenhuma</p>");
            return;
        }
        h.add("<table class=\"fontes\"><thead><tr><th></th><th>municipio</th><th>uf</th>"
                + "<th>status</th><th>ultima tentativa</th><th>motivo</th>"
                + "<th class=\"num\">no acervo</th></tr></thead><tbody>");
        for (EstadoFonte e : lista) {
            Municipio m = e.getMunicipio();
            h.add("<tr class=\"" + (e.isLigada() ? "ligada" : "desligada") + "\">");
            h.add("<td><form method=\"post\" action=\"/fonte/alternar\">"
                    + "<input type=\"hidden\" name=\"codigo\" value=\"" + esc(m.getCodigo()) + "\">"
                    + "<button type=\"submit\" class=\"chave " + (e.isLigada() ? "on" : "off") + "\">"
                    + (e.isLigada() ? "LIGADA" : "ligar") + "</button></form></td>");
            h.add("<td>" + esc(m.getNome())
                    + (e.isDescoberta() ? " <span class=\"tag\">descoberta</span>" : "") + "</td>");
            h.add("<td>" + esc(m.getUf()) + "</td>");
            h.add("<td><span class=\"tag tag-" + classeDoStatus(e.getStatus()) + "\">"
                    + esc(e.getStatus()) + "</span></td>");
            h.add("<td class=\"mono\">" + esc(e.getUltimaTentativa()) + "</td>");
            h.add("<td class=\"motivo\">" + esc(PaginaHtml.cortar(e.getMotivo(), 90)) + "</td>");
            h.add("<td class=\"num\">" + e.getDiariosNoAcervo() + "</td>");
            h.add("</tr>");
        }
        h.add("</tbody></table>");
    }

    private void legenda(ArrayList<String> h, String classe, String nome, String explicacao) {
        h.add("<span><span class=\"tag tag-" + classe + "\">" + nome + "</span> "
                + esc(explicacao) + "</span>");
    }

    private void secaoPorMunicipio(ArrayList<String> h, ArrayList<Diario> diarios,
                                   PainelContratos painel) {
        ArrayList<String> municipios = new ArrayList<>();
        for (Diario d : diarios) {
            if (!municipios.contains(d.getMunicipio())) {
                municipios.add(d.getMunicipio());
            }
        }

        h.add("<section>");
        h.add("<h2>Por municipio</h2>");
        h.add("<p class=\"nota\">Os mesmos numeros do topo, separados por municipio. "
                + "E aqui que as fontes podem ser comparadas entre si.</p>");
        h.add("<table><thead><tr><th>municipio</th><th class=\"num\">diarios</th>"
                + "<th class=\"num\">recusados</th><th class=\"num\">atos</th>"
                + "<th class=\"num\">aceitos</th><th class=\"num\">contratacoes</th>"
                + "<th class=\"num\">valor contratado</th></tr></thead><tbody>");
        for (String m : municipios) {
            int qtd = 0;
            int recusados = 0;
            int atos = 0;
            int aceitos = 0;
            for (Diario d : diarios) {
                if (!d.getMunicipio().equals(m)) {
                    continue;
                }
                qtd++;
                if (!d.isUtilizavel()) {
                    recusados++;
                }
                atos += d.getAtos().size();
                aceitos += d.contar(Ato.ACEITO);
            }
            int contratos = 0;
            double valor = 0;
            for (Contrato c : painel.getContratos()) {
                if (c.getMunicipio().equals(m)) {
                    contratos++;
                    valor += c.getValor();
                }
            }
            h.add("<tr><td>" + esc(bonito(m)) + "</td><td class=\"num\">" + qtd + "</td>"
                    + "<td class=\"num" + (recusados > 0 ? " ruim" : "") + "\">" + recusados + "</td>"
                    + "<td class=\"num\">" + atos + "</td><td class=\"num\">" + aceitos + "</td>"
                    + "<td class=\"num\">" + contratos + "</td>"
                    + "<td class=\"num\">" + Contrato.formatarReal(valor) + "</td></tr>");
        }
        h.add("</tbody></table>");
        h.add("</section>");
    }

    private void secaoDiarios(ArrayList<String> h, ArrayList<Diario> diarios) {
        h.add("<section>");
        h.add("<h2>Diarios lidos</h2>");
        h.add("<p class=\"nota\">Clique para abrir o diario: o texto como veio, o texto limpo, "
                + "cada ato separado e o que o programa leu em cada um.</p>");
        h.add("<table><thead><tr><th>arquivo</th><th>situacao</th><th class=\"num\">atos</th>"
                + "<th class=\"num\">aceitos</th><th class=\"num\">duvidosos</th>"
                + "<th class=\"num\">lixo removido</th></tr></thead><tbody>");
        for (Diario d : diarios) {
            String link = "<a href=\"/diario?arquivo=" + Formulario.codificar(d.getArquivo())
                    + "\">" + esc(d.getArquivo()) + "</a>";
            if (!d.isUtilizavel()) {
                h.add("<tr><td>" + link + "</td><td><span class=\"tag tag-rej\">RECUSADO</span> "
                        + esc(d.getMotivoRecusa()) + "</td><td class=\"num\">-</td>"
                        + "<td class=\"num\">-</td><td class=\"num\">-</td><td class=\"num\">-</td></tr>");
                continue;
            }
            h.add("<tr><td>" + link + "</td><td><span class=\"tag tag-ok\">PROCESSADO</span></td>"
                    + "<td class=\"num\">" + d.getAtos().size() + "</td>"
                    + "<td class=\"num\">" + d.contar(Ato.ACEITO) + "</td>"
                    + "<td class=\"num\">" + d.contar(Ato.DUVIDOSO) + "</td>"
                    + "<td class=\"num\">" + d.getLinhasRemovidas() + "</td></tr>");
        }
        h.add("</tbody></table>");
        h.add("</section>");
    }

    private void rodape(ArrayList<String> h) {
        h.add("<footer>");
        h.add("<p>Pagina montada a cada requisicao pelo proprio programa Java "
                + "(<span class=\"mono\">com.sun.net.httpserver</span>, que ja vem no JDK). "
                + "Sem biblioteca externa, sem JavaScript.</p>");
        h.add("<p>Deterministico: a mesma pasta de diarios produz sempre os mesmos numeros "
                + "e as mesmas frases. Nenhuma interpretacao usa IA.</p>");
        h.add("<p>CPFs publicados nos diarios aparecem mascarados. Dados: Querido Diario, "
                + "Open Knowledge Brasil.</p>");
        h.add("</footer>");
    }

    static String classeDoStatus(String status) {
        if (status.equals(EstadoFonte.COM_DADO)) {
            return "ok";
        }
        if (status.equals(EstadoFonte.FALHOU)) {
            return "rej";
        }
        if (status.equals(EstadoFonte.SEM_EDICAO) || status.equals(EstadoFonte.SO_PDF)) {
            return "duv";
        }
        return "neutro";
    }

    static String bonito(String apelido) {
        StringBuilder nome = new StringBuilder();
        for (String palavra : apelido.split("-")) {
            if (palavra.isEmpty()) {
                continue;
            }
            nome.append(nome.length() > 0 ? " " : "")
                .append(Character.toUpperCase(palavra.charAt(0))).append(palavra.substring(1));
        }
        return nome.toString();
    }

    private static String esc(String s) {
        return PaginaHtml.escapar(s);
    }

    static String estiloDoPainel() {
        return String.join("\n",
        ".estado{font-size:13px;color:var(--ink2);padding:10px 14px;border-radius:8px;",
        "  border:1px solid var(--grade);background:var(--surface);margin-bottom:18px}",
        ".estado-ocupado{border-left:3px solid var(--serie);color:var(--ink)}",
        ".estado-aviso{border-left:3px solid var(--duv)}",
        ".girando{display:inline-block;animation:gira 1s linear infinite}",
        "@keyframes gira{to{transform:rotate(360deg)}}",
        ".acoes{display:flex;flex-wrap:wrap;gap:10px;align-items:center;margin-bottom:12px}",
        ".acoes form{display:flex;gap:8px;align-items:center;margin:0}",
        "button{font:inherit;font-size:13px;padding:6px 12px;border-radius:6px;cursor:pointer;",
        "  border:1px solid var(--eixo);background:var(--surface);color:var(--ink)}",
        "button:hover{border-color:var(--ink3)}",
        "button:disabled{opacity:.45;cursor:not-allowed}",
        "button.primario{background:var(--serie);border-color:var(--serie);color:#fff}",
        "input[type=number]{width:64px}",
        "input{font:inherit;font-size:13px;padding:5px 8px;border-radius:6px;",
        "  border:1px solid var(--eixo);background:var(--plano);color:var(--ink)}",
        ".campo{flex:1;min-width:220px}",
        "label{font-size:13px;color:var(--ink2);display:flex;gap:6px;align-items:center}",
        ".chave{min-width:86px;font-size:11px;letter-spacing:.05em;padding:3px 8px}",
        ".chave.on{background:var(--ok);border-color:var(--ok);color:#fff}",
        ".chave.off{color:var(--ink3)}",
        "tr.desligada td{color:var(--ink3)}",
        ".fontes td{padding:4px 8px}",
        ".tag-ok{border-color:var(--ok);color:var(--ok)}",
        ".tag-duv{border-color:var(--duv);color:var(--ink2)}",
        ".tag-rej{border-color:var(--rej);color:var(--rej)}",
        ".tag-neutro{color:var(--ink3)}",
        ".legenda{display:flex;flex-wrap:wrap;gap:14px;font-size:12px;color:var(--ink3);",
        "  margin-bottom:8px}",
        "td.ruim{color:var(--rej);font-weight:600}",
        ".log{margin:6px 0 4px}",
        ".log summary{cursor:pointer;font-size:13px;color:var(--ink2)}",
        ".log pre{font-size:11.5px;color:var(--ink2);white-space:pre-wrap;max-height:260px;",
        "  overflow:auto;background:var(--plano);padding:10px;border-radius:6px}",
        "a{color:var(--serie)}",
        ".menu{display:flex;gap:6px;align-items:center;margin:-8px 0 22px;padding:8px;",
        "  border:1px solid var(--grade);border-radius:10px;background:var(--surface);",
        "  position:sticky;top:8px;z-index:10}",
        ".menu a{padding:6px 12px;border-radius:6px;text-decoration:none;color:var(--ink2);",
        "  font-size:14px}",
        ".menu a.ativo{background:var(--serie);color:#fff}",
        ".menu form{margin-left:auto}",
        ".menu input{width:230px}",
        ".divisor{margin:34px 0 12px;font-size:15px;color:var(--ink3);text-transform:uppercase;",
        "  letter-spacing:.06em}",
        "td.motivo{font-size:11.5px;color:var(--ink3);max-width:320px}",
        "details.log>section{margin-top:10px}");
    }
}
