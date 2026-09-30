package diarios.analise;

import diarios.extratores.Extrator;
import diarios.modelo.Ato;
import diarios.modelo.Diario;
import diarios.modelo.Fato;
import diarios.modelo.Relacao;
import java.util.ArrayList;

public class GrafoNormas {

    private static final int JANELA_ANTES = 160;
    private static final int JANELA_DEPOIS = 110;
    private static final int TRECHO_FUNDAMENTO = 45;
    private static final int TRECHO_VERBO = 70;
    private static final int TRECHO_DEPOIS = 90;

    private static final String[] FUNDAMENTO = {
        "nos termos d", "conforme", "com base n", "previsto n", "prevista n",
        "de acordo com", "na forma d", "a que se refere", "disposto n", "art."
    };

    private final ArrayList<Relacao> arestas = new ArrayList<>();

    public void limpar() {
        arestas.clear();
    }

    public void processar(ArrayList<Diario> diarios) {
        for (Diario d : diarios) {
            if (!d.isUtilizavel()) {
                continue;
            }
            for (Ato ato : d.getAtos()) {
                for (Fato ref : ato.getFatosDoTipo(Fato.REFERENCIA)) {
                    Relacao nova = new Relacao(ato.getIdentidade(),
                            classificar(d.getTextoLimpo(), ref.getInicio(), ref.getFim()),
                            ref.getValor(), ref.getInicio());
                    if (!arestas.contains(nova)) {
                        arestas.add(nova);
                    }
                }
            }
        }
    }

    private String classificar(String texto, int inicioCitacao, int fimCitacao) {
        String antes = trecho(texto, Math.max(0, inicioCitacao - JANELA_ANTES), inicioCitacao);
        String depois = trecho(texto, fimCitacao,
                Math.min(texto.length(), fimCitacao + JANELA_DEPOIS));

        String comecoDepois = primeiros(depois, TRECHO_DEPOIS);
        if (comecoDepois.contains("fica revogad") || comecoDepois.contains("ficam revogad")) {
            return Relacao.REVOGA;
        }
        if (comecoDepois.contains("passa a vigorar") || comecoDepois.contains("passam a vigorar")) {
            return Relacao.ALTERA;
        }

        if (ehFundamento(antes)) {
            return Relacao.CITA;
        }

        String fim = ultimos(antes, TRECHO_VERBO);
        if (operou(fim, "revogad", "revoga")) {
            return Relacao.REVOGA;
        }
        if (operou(fim, "prorrogad", "prorroga")) {
            return Relacao.PRORROGA;
        }
        if (operou(fim, "alterad", "altera") || fim.contains("nova redacao")) {
            return Relacao.ALTERA;
        }
        return Relacao.CITA;
    }

    private boolean operou(String trecho, String participio, String verbo) {
        return trecho.contains("fica " + participio) || trecho.contains("ficam " + participio)
                || trecho.contains(verbo + " o ") || trecho.contains(verbo + " a ");
    }

    private boolean ehFundamento(String antes) {
        String fim = ultimos(antes, TRECHO_FUNDAMENTO);
        for (String marca : FUNDAMENTO) {
            if (fim.contains(marca)) {
                return true;
            }
        }
        return false;
    }

    private String trecho(String texto, int de, int ate) {
        return Extrator.semAcento(texto.substring(de, ate).toLowerCase().replace('\n', ' '));
    }

    private String primeiros(String s, int n) {
        return s.length() > n ? s.substring(0, n) : s;
    }

    private String ultimos(String s, int n) {
        return s.length() > n ? s.substring(s.length() - n) : s;
    }

    public ArrayList<Relacao> getArestas() {
        return arestas;
    }

    public ArrayList<Relacao> doTipo(String acao) {
        ArrayList<Relacao> lista = new ArrayList<>();
        for (Relacao r : arestas) {
            if (r.getAcao().equals(acao)) {
                lista.add(r);
            }
        }
        return lista;
    }

    public ArrayList<String[]> maisAlteradas() {
        ArrayList<String> alvos = new ArrayList<>();
        ArrayList<Integer> contagem = new ArrayList<>();

        for (Relacao r : arestas) {
            if (r.getAcao().equals(Relacao.CITA)) {
                continue;
            }
            int i = alvos.indexOf(r.getAlvo());
            if (i < 0) {
                alvos.add(r.getAlvo());
                contagem.add(1);
            } else {
                contagem.set(i, contagem.get(i) + 1);
            }
        }

        ArrayList<String[]> linhas = new ArrayList<>();
        boolean[] usado = new boolean[alvos.size()];
        for (int n = 0; n < alvos.size(); n++) {
            int melhor = -1;
            for (int i = 0; i < alvos.size(); i++) {
                if (!usado[i] && (melhor < 0 || contagem.get(i) > contagem.get(melhor))) {
                    melhor = i;
                }
            }
            usado[melhor] = true;
            linhas.add(new String[] {alvos.get(melhor), String.valueOf(contagem.get(melhor))});
        }
        return linhas;
    }

    public ArrayList<String> vizinhanca(String norma, int niveis) {
        ArrayList<String> visitados = new ArrayList<>();
        ArrayList<String> fronteira = new ArrayList<>();
        visitados.add(norma);
        fronteira.add(norma);

        for (int nivel = 0; nivel < niveis && !fronteira.isEmpty(); nivel++) {
            ArrayList<String> proxima = new ArrayList<>();
            for (String atual : fronteira) {
                for (Relacao r : arestas) {
                    String outro = null;
                    if (r.getAlvo().equals(atual)) {
                        outro = r.getOrigem();
                    } else if (r.getOrigem().equals(atual)) {
                        outro = r.getAlvo();
                    }
                    if (outro != null && !visitados.contains(outro)) {
                        visitados.add(outro);
                        proxima.add(outro);
                    }
                }
            }
            fronteira = proxima;
        }
        return visitados;
    }
}
