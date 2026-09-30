package diarios.erros;

public class ColetaException extends Exception {

    private final String url;
    private final boolean valeTentarDeNovo;
    private final long esperaSugeridaMs;

    public ColetaException(String url, String motivo) {
        this(url, motivo, false, 0);
    }

    public ColetaException(String url, String motivo, boolean valeTentarDeNovo, long esperaSugeridaMs) {
        super(motivo);
        this.url = url;
        this.valeTentarDeNovo = valeTentarDeNovo;
        this.esperaSugeridaMs = esperaSugeridaMs;
    }

    public String getUrl()             { return url; }
    public boolean podeTentarDeNovo()  { return valeTentarDeNovo; }
    public long getEsperaSugeridaMs()  { return esperaSugeridaMs; }
}
