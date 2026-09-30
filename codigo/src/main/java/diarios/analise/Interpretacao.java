package diarios.analise;

import diarios.modelo.Achado;
import diarios.modelo.Ato;
import diarios.modelo.Contrato;
import diarios.modelo.Diario;
import diarios.modelo.Fato;
import diarios.modelo.Relacao;
import java.util.ArrayList;

public class Interpretacao {

    private static final int CONCENTRACAO_ALTA = 60;
    private static final int UM_SO_CONTRATO_ALTO = 30;
    private static final int REPETICAO_MINIMA = 3;
    private static final int SEM_PROCESSO_ALTO = 30;
    private static final int DUVIDA_ALTA = 25;

    private final ArrayList<Achado> achados = new ArrayList<>();
    private String resumo = "";

    public void limpar() {
        achados.clear();
        resumo = "";
    }

    public void interpretar(ArrayList<Diario> diarios, Cobertura cobertura,
                            PainelContratos painel, GrafoNormas grafo) {
        limpar();
        montarResumo(diarios, painel, grafo);
        perfilDoDiario(diarios);
        concentracaoDeGasto(painel);
        contratoDominante(painel);
        fornecedorRepetido(painel);
        rastreabilidade(painel);
        papeisSeparados(painel);
        normasDesatualizadas(grafo);
        buracosDeCobertura(cobertura);
        oQueNaoFoiEntendido(diarios, cobertura);
    }

    private void montarResumo(ArrayList<Diario> diarios, PainelContratos painel,
                              GrafoNormas grafo) {
        int atos = 0;
        String periodo = periodoDe(diarios);
        ArrayList<String> municipios = new ArrayList<>();

        for (Diario d : diarios) {
            if (!d.isUtilizavel()) {
                continue;
            }
            atos += d.getAtos().size();
            if (!municipios.contains(d.getMunicipio())) {
                municipios.add(d.getMunicipio());
            }
        }
        if (atos == 0) {
            resumo = "Nenhum ato pode ser lido no periodo.";
            return;
        }

        int contratacoes = painel.getContratos().size();
        int atosDeContrato = contarAtosDoAssunto(diarios, Assunto.CONTRATACAO);
        int operacoes = grafo.doTipo(Relacao.ALTERA).size()
                + grafo.doTipo(Relacao.REVOGA).size()
                + grafo.doTipo(Relacao.PRORROGA).size();
        int normasAtingidas = grafo.maisAlteradas().size();

        StringBuilder s = new StringBuilder();
        s.append("Em ").append(periodo).append(", ");
        s.append(municipios.size() == 1 ? municipios.get(0) : municipios.size() + " municipios");
        s.append(" publicou ").append(atos).append(" atos. ");

        if (contratacoes > 0) {
            s.append("Em ").append(atosDeContrato)
             .append(atosDeContrato == 1 ? " deles ha " : " deles ha ")
             .append(contratacoes).append(contratacoes == 1 ? " contratacao" : " contratacoes")
             .append(", somando ").append(Contrato.formatarReal(painel.total())).append(". ");
        }
        if (operacoes > 0) {
            s.append(operacoes).append(operacoes == 1 ? " alteracao atinge " : " alteracoes atingem ")
             .append(normasAtingidas)
             .append(normasAtingidas == 1 ? " norma anterior. " : " normas anteriores. ");
        } else {
            s.append("Nenhuma norma anterior foi tocada. ");
        }
        resumo = s.toString().trim();
    }

    private void perfilDoDiario(ArrayList<Diario> diarios) {
        int total = 0;
        int contratacao = 0;
        int pessoal = 0;
        int norma = 0;
        int licitacao = 0;
        int orcamento = 0;

        for (Diario d : diarios) {
            if (!d.isUtilizavel()) {
                continue;
            }
            for (Ato a : d.getAtos()) {
                total++;
                String assunto = Assunto.de(a);
                if (assunto.equals(Assunto.CONTRATACAO)) {
                    contratacao++;
                } else if (assunto.equals(Assunto.PESSOAL)) {
                    pessoal++;
                } else if (assunto.equals(Assunto.NORMA)) {
                    norma++;
                } else if (assunto.equals(Assunto.LICITACAO)) {
                    licitacao++;
                } else if (assunto.equals(Assunto.ORCAMENTO)) {
                    orcamento++;
                }
            }
        }
        if (total == 0) {
            return;
        }

        int gastoPct = (contratacao + licitacao) * 100 / total;
        int normaPct = norma * 100 / total;
        String tipo = gastoPct > normaPct * 3
                ? "um diario de EXECUCAO, nao de legislacao"
                : "um diario com peso legislativo relevante";

        achados.add(new Achado(Achado.LEITURA,
                "O que este diario e",
                "E " + tipo + ": " + gastoPct + "% dos atos tratam de gastar dinheiro "
                + "(contrato ou licitacao) e " + normaPct + "% mexem em norma. "
                + "Quem acompanha a prefeitura por aqui esta vendo, sobretudo, "
                + "para onde o dinheiro vai.",
                "de " + total + " atos: " + contratacao + " contratacoes, " + licitacao
                + " licitacoes, " + pessoal + " pessoal, " + norma + " normas, "
                + orcamento + " orcamento"));
    }

    private void concentracaoDeGasto(PainelContratos painel) {
        ArrayList<String[]> ranking = painel.ranking();
        double total = painel.total();
        if (ranking.size() < 3 || total <= 0) {
            return;
        }

        double top3 = 0;
        StringBuilder nomes = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            top3 += valorDe(ranking.get(i)[2]);
            nomes.append(i > 0 ? ", " : "").append(ranking.get(i)[1]);
        }
        int pct = (int) Math.round(top3 * 100 / total);

        String leitura = pct >= CONCENTRACAO_ALTA
                ? "O gasto do periodo esta CONCENTRADO: tres empresas ficam com a "
                  + "maior parte do valor, enquanto as outras " + (ranking.size() - 3)
                  + " dividem o resto."
                : "O gasto do periodo esta DISTRIBUIDO entre os " + ranking.size()
                  + " fornecedores: nenhum trio domina o valor.";

        achados.add(new Achado(pct >= CONCENTRACAO_ALTA ? Achado.ATENCAO : Achado.LEITURA,
                "Concentracao do gasto",
                leitura + " Os tres maiores somam " + pct + "% do total.",
                nomes + " = " + Contrato.formatarReal(top3) + " de "
                + Contrato.formatarReal(total)));
    }

    private void contratoDominante(PainelContratos painel) {
        ArrayList<Contrato> contratos = painel.getContratos();
        double total = painel.total();
        if (contratos.isEmpty() || total <= 0) {
            return;
        }

        Contrato maior = contratos.get(0);
        for (Contrato c : contratos) {
            if (c.getValor() > maior.getValor()) {
                maior = c;
            }
        }
        int pct = (int) Math.round(maior.getValor() * 100 / total);
        if (pct < UM_SO_CONTRATO_ALTO) {
            return;
        }

        achados.add(new Achado(Achado.ATENCAO,
                "Um contrato pesa mais que todos os outros",
                "Um unico contrato responde por " + pct + "% de tudo que foi contratado "
                + "no periodo. Os outros " + (contratos.size() - 1)
                + " contratos somados valem menos que ele.",
                maior.getContratada() + " (" + maior.getCnpj() + ") = "
                + Contrato.formatarReal(maior.getValor()) + ", processo "
                + maior.getProcesso()));
    }

    private void fornecedorRepetido(PainelContratos painel) {
        ArrayList<String[]> repetidos = painel.repeticoes(REPETICAO_MINIMA);
        if (repetidos.isEmpty()) {
            return;
        }

        StringBuilder lista = new StringBuilder();
        for (String[] r : repetidos) {
            lista.append(lista.length() > 0 ? "; " : "").append(r[1]).append(" ")
                 .append(r[3]).append("x");
        }

        achados.add(new Achado(Achado.ATENCAO,
                "Fornecedor que se repete",
                repetidos.size() + (repetidos.size() == 1 ? " fornecedor aparece" : " fornecedores aparecem")
                + " em tres ou mais contratos do mesmo periodo. Isso tem explicacao "
                + "legitima (ata de registro de precos, fornecedor unico de um insumo) "
                + "e nao e acusacao de nada - e o lugar onde vale a pena olhar o "
                + "processo original.",
                lista.toString()));
    }

    private void rastreabilidade(PainelContratos painel) {
        ArrayList<Contrato> contratos = painel.getContratos();
        if (contratos.isEmpty()) {
            return;
        }
        int semProcesso = 0;
        for (Contrato c : contratos) {
            if (c.getProcesso().equals("-")) {
                semProcesso++;
            }
        }
        int pct = semProcesso * 100 / contratos.size();
        if (pct < SEM_PROCESSO_ALTO) {
            return;
        }

        achados.add(new Achado(Achado.LIMITE,
                "Contrato sem processo identificado",
                pct + "% das contratacoes nao trazem numero de processo no texto "
                + "publicado. Sem ele nao da para ligar o contrato a licitacao que o "
                + "originou - nem pelo programa, nem por uma pessoa lendo o diario.",
                semProcesso + " de " + contratos.size() + " contratacoes"));
    }

    private void papeisSeparados(PainelContratos painel) {
        int prefeitura = painel.contarDescartadosDoPapel(PainelContratos.LADO_MUNICIPIO);
        int notificados = painel.contarDescartadosDoPapel(PainelContratos.NOTIFICADO);
        if (prefeitura + notificados == 0) {
            return;
        }

        achados.add(new Achado(Achado.LIMITE,
                "Nem todo CNPJ com valor ao lado e um contrato",
                (prefeitura + notificados) + " CNPJs apareciam junto de um valor mas nao "
                + "entram no total contratado: " + prefeitura + " sao o proprio orgao "
                + "municipal aplicando multa, e " + notificados + " sao empresas sendo "
                + "multadas. Somar tudo daria um numero maior e errado - multa nao e "
                + "compra, e o orgao nao e fornecedor de si mesmo.",
                "separados pelo papel escrito antes do CNPJ: NOTIFICANTE, NOTIFICADO, "
                + "CONTRATADA, DETENTORA"));
    }

    private void normasDesatualizadas(GrafoNormas grafo) {
        ArrayList<String[]> mexidas = grafo.maisAlteradas();
        if (mexidas.isEmpty()) {
            return;
        }

        StringBuilder lista = new StringBuilder();
        int mostrar = Math.min(5, mexidas.size());
        for (int i = 0; i < mostrar; i++) {
            lista.append(lista.length() > 0 ? "; " : "")
                 .append(mexidas.get(i)[0]).append(" (").append(mexidas.get(i)[1]).append("x)");
        }

        achados.add(new Achado(Achado.LEITURA,
                "Normas que mudaram de texto",
                mexidas.size() + (mexidas.size() == 1
                        ? " norma anterior foi alterada, revogada ou prorrogada"
                        : " normas anteriores foram alteradas, revogadas ou prorrogadas")
                + " neste periodo. Quem estiver "
                + "trabalhando com a versao antiga do texto esta desatualizado, e nao "
                + "tem como saber disso lendo so a norma original - a mudanca esta no "
                + "diario, nao nela.",
                lista.toString()));
    }

    private void buracosDeCobertura(Cobertura cobertura) {
        if (cobertura.getRecusados() == 0) {
            return;
        }
        int pct = cobertura.getRecusados() * 100 / cobertura.getLidos();

        achados.add(new Achado(Achado.LIMITE,
                "O que nao deu para ler",
                pct + "% dos diarios do periodo nao puderam ser lidos - sao PDFs "
                + "escaneados, sem texto por baixo. Tudo que este relatorio diz vale "
                + "para os outros " + (100 - pct) + "%. Nao e que nao houve publicacao "
                + "nesses dias: e que o programa nao consegue enxerga-la.",
                cobertura.getRecusados() + " de " + cobertura.getLidos() + " diarios"));
    }

    private void oQueNaoFoiEntendido(ArrayList<Diario> diarios, Cobertura cobertura) {
        if (cobertura.getTotalAtos() == 0) {
            return;
        }
        int pct = cobertura.getDuvidosos() * 100 / cobertura.getTotalAtos();
        if (pct < DUVIDA_ALTA) {
            return;
        }

        ArrayList<String> motivos = new ArrayList<>();
        ArrayList<Integer> vezes = new ArrayList<>();
        for (Diario d : diarios) {
            for (Ato a : d.getAtos()) {
                if (!a.getDecisao().equals(Ato.DUVIDOSO)) {
                    continue;
                }
                String motivo = resumirMotivo(a.getMotivo());
                int i = motivos.indexOf(motivo);
                if (i < 0) {
                    motivos.add(motivo);
                    vezes.add(1);
                } else {
                    vezes.set(i, vezes.get(i) + 1);
                }
            }
        }

        StringBuilder lista = new StringBuilder();
        for (int i = 0; i < motivos.size(); i++) {
            lista.append(lista.length() > 0 ? "; " : "")
                 .append(vezes.get(i)).append("x ").append(motivos.get(i));
        }

        achados.add(new Achado(Achado.LIMITE,
                "O que o programa ainda nao trata",
                pct + "% dos atos ficaram em duvida. Eles nao foram descartados nem "
                + "contados como certos: estao separados, com o motivo de cada um. "
                + "Esta e a lista do que falta melhorar, e ela so existe porque o "
                + "portao tem tres saidas em vez de duas.",
                lista.toString()));
    }

    private int contarAtosDoAssunto(ArrayList<Diario> diarios, String assunto) {
        int n = 0;
        for (Diario d : diarios) {
            if (!d.isUtilizavel()) {
                continue;
            }
            for (Ato a : d.getAtos()) {
                if (Assunto.de(a).equals(assunto)) {
                    n++;
                }
            }
        }
        return n;
    }

    private String resumirMotivo(String motivo) {
        int parenteses = motivo.indexOf('(');
        return parenteses > 0 ? motivo.substring(0, parenteses).trim() : motivo;
    }

    private String periodoDe(ArrayList<Diario> diarios) {
        String menor = null;
        String maior = null;
        for (Diario d : diarios) {
            if (menor == null || d.getData().compareTo(menor) < 0) {
                menor = d.getData();
            }
            if (maior == null || d.getData().compareTo(maior) > 0) {
                maior = d.getData();
            }
        }
        if (menor == null) {
            return "periodo vazio";
        }
        return menor.equals(maior) ? menor : menor + " a " + maior;
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

    public String getResumo() {
        return resumo;
    }

    public ArrayList<Achado> getAchados() {
        return achados;
    }

    public ArrayList<Achado> doNivel(String nivel) {
        ArrayList<Achado> lista = new ArrayList<>();
        for (Achado a : achados) {
            if (a.getNivel().equals(nivel)) {
                lista.add(a);
            }
        }
        return lista;
    }
}
