package diarios.modelo;

public class Fato {

    public static final String DATA = "DATA";
    public static final String VALOR = "VALOR";
    public static final String CNPJ = "CNPJ";
    public static final String PROCESSO = "PROCESSO";
    public static final String REFERENCIA = "REFERENCIA";

    private final String tipo;
    private final String valor;
    private final int inicio;
    private final int fim;

    public Fato(String tipo, String valor, int inicio, int fim) {
        this.tipo = tipo;
        this.valor = valor;
        this.inicio = inicio;
        this.fim = fim;
    }

    public String getTipo()  { return tipo; }
    public String getValor() { return valor; }
    public int getInicio()   { return inicio; }
    public int getFim()      { return fim; }

    public String provar(String textoOriginal, int margem) {
        int de = Math.max(0, inicio - margem);
        int ate = Math.min(textoOriginal.length(), fim + margem);
        return textoOriginal.substring(de, ate).replace('\n', ' ').trim();
    }

    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (!(outro instanceof Fato)) {
            return false;
        }
        Fato f = (Fato) outro;
        return tipo.equals(f.tipo) && valor.equals(f.valor);
    }

    @Override
    public int hashCode() {
        return (tipo + "|" + valor).hashCode();
    }

    @Override
    public String toString() {
        return String.format("%-10s %-30s [%d..%d]", tipo, valor, inicio, fim);
    }
}
