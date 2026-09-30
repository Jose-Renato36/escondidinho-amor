package diarios.analise;

import diarios.extratores.Extrator;
import diarios.modelo.Ato;
import diarios.modelo.Contrato;
import diarios.modelo.Fato;
import diarios.modelo.Relacao;
import java.util.ArrayList;

public class LeituraDoAto {

    private static final String[][] PESSOAL = {
        {"exonera", "exoneracao"}, {"nomea", "nomeacao"}, {"nomeia", "nomeacao"},
        {"designa", "designacao"}, {"aposentadoria", "aposentadoria"},
        {"licenca", "concessao de licenca"}, {"ferias", "ferias"}
    };

    private static final String[][] LICITACAO = {
        {"homologa", "homologacao"}, {"adjudica", "adjudicacao"},
        {"pregao", "pregao"}, {"dispensa de licitacao", "dispensa de licitacao"},
        {"inexigibilidade", "inexigibilidade"}, {"chamamento publico", "chamamento publico"},
        {"concorrencia", "concorrencia"}
    };

    private final PainelContratos painel;
    private final GrafoNormas grafo;

    public LeituraDoAto(PainelContratos painel, GrafoNormas grafo) {
        this.painel = painel;
        this.grafo = grafo;
    }

    public String ler(Ato ato, String dataDoDiario) {
        String texto = Extrator.semAcento(ato.getTexto().toLowerCase());
        String assunto = Assunto.de(ato);
        StringBuilder s = new StringBuilder();

        if (assunto.equals(Assunto.CONTRATACAO)) {
            s.append(lerContratacao(ato));
        } else if (assunto.equals(Assunto.NORMA)) {
            s.append(lerNorma(ato));
        } else if (assunto.equals(Assunto.PESSOAL)) {
            s.append("Ato de pessoal: ").append(primeiraMarca(texto, PESSOAL, "movimentacao de servidor"))
             .append(".");
        } else if (assunto.equals(Assunto.LICITACAO)) {
            s.append("Ato de licitacao: ").append(primeiraMarca(texto, LICITACAO, "procedimento licitatorio"))
             .append(".");
        } else if (assunto.equals(Assunto.ORCAMENTO)) {
            s.append(lerOrcamento(ato));
        } else {
            s.append("Ato sem categoria reconhecida; ").append(ato.getFatos().size())
             .append(" fato(s) extraido(s).");
        }

        if (texto.contains("entra em vigor na data de sua publicacao")
                || texto.contains("entra em vigor na data da sua publicacao")) {
            s.append(" Vigora desde a publicacao (").append(dataDoDiario).append(").");
        }
        if (ato.getDecisao().equals(Ato.DUVIDOSO)) {
            s.append(" Leitura com ressalva: ").append(ato.getMotivo()).append(".");
        }
        return s.toString();
    }

    private String lerContratacao(Ato ato) {
        ArrayList<Contrato> doAto = contratosDoAto(painel.getContratos(), ato);
        if (doAto.isEmpty()) {
            ArrayList<Contrato> multas = contratosDoAto(painel.getDescartados(), ato);
            if (!multas.isEmpty()) {
                return "Notificacao ou multa envolvendo " + multas.size()
                        + " CNPJ(s); nao conta como contratacao.";
            }
            return "Menciona empresa e valor, mas o papel de cada parte nao ficou claro.";
        }
        double total = 0;
        for (Contrato c : doAto) {
            total += c.getValor();
        }
        Contrato principal = doAto.get(0);
        for (Contrato c : doAto) {
            if (c.getValor() > principal.getValor()) {
                principal = c;
            }
        }
        StringBuilder s = new StringBuilder();
        if (doAto.size() == 1) {
            s.append("Contrata ").append(quem(principal)).append(" por ")
             .append(Contrato.formatarReal(total)).append(".");
        } else {
            s.append(doAto.size()).append(" contratacoes somando ")
             .append(Contrato.formatarReal(total)).append("; a maior com ")
             .append(quem(principal)).append(".");
        }
        if (!principal.getProcesso().equals("-")) {
            s.append(" Processo ").append(principal.getProcesso()).append(".");
        }
        return s.toString();
    }

    private String quem(Contrato c) {
        if (c.getContratada().startsWith("(")) {
            return "a empresa de CNPJ " + c.getCnpj() + " (nome nao lido)";
        }
        return c.getContratada() + " (" + c.getCnpj() + ")";
    }

    private String lerNorma(Ato ato) {
        ArrayList<String> acoes = new ArrayList<>();
        int citacoes = 0;
        for (Relacao r : grafo.getArestas()) {
            if (!r.getOrigem().equals(ato.getIdentidade())) {
                continue;
            }
            if (r.getAcao().equals(Relacao.CITA)) {
                citacoes++;
            } else {
                String frase = r.getAcao().toLowerCase() + " " + r.getAlvo();
                if (!acoes.contains(frase)) {
                    acoes.add(frase);
                }
            }
        }
        if (acoes.isEmpty()) {
            return "Ato normativo que cria regra propria; cita " + citacoes
                    + " norma(s) como fundamento.";
        }
        return "Ato normativo que " + String.join(", ", acoes) + ".";
    }

    private String lerOrcamento(Ato ato) {
        ArrayList<Fato> valores = ato.getFatosDoTipo(Fato.VALOR);
        if (valores.isEmpty()) {
            return "Ato orcamentario (credito ou dotacao), sem valor identificado.";
        }
        return "Ato orcamentario movimentando " + valores.get(0).getValor() + ".";
    }

    private ArrayList<Contrato> contratosDoAto(ArrayList<Contrato> lista, Ato ato) {
        ArrayList<Contrato> achados = new ArrayList<>();
        for (Contrato c : lista) {
            if (c.getIdentidadeAto().equals(ato.getIdentidade())) {
                achados.add(c);
            }
        }
        return achados;
    }

    private String primeiraMarca(String texto, String[][] marcas, String padrao) {
        for (String[] par : marcas) {
            if (texto.contains(par[0])) {
                return par[1];
            }
        }
        return padrao;
    }
}
