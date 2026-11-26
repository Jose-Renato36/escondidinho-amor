package diarios.modelo;

public class Relacao {

    private final String origem;
    private final Acao acao;
    private final String alvo;
    private final int posicao;

    public Relacao(String origem, Acao acao, String alvo, int posicao) {
        this.origem = origem;
        this.acao = acao;
        this.alvo = alvo;
        this.posicao = posicao;
    }

    public String getOrigem() { return origem; }
    public Acao getAcao()     { return acao; }
    public String getAlvo()   { return alvo; }
    public int getPosicao()   { return posicao; }

    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (!(outro instanceof Relacao)) {
            return false;
        }
        Relacao r = (Relacao) outro;
        return origem.equals(r.origem) && acao.equals(r.acao) && alvo.equals(r.alvo);
    }

    @Override
    public int hashCode() {
        return (origem + acao + alvo).hashCode();
    }

    @Override
    public String toString() {
        return String.format("%-42s --%-8s--> %s", origem, acao, alvo);
    }
}
