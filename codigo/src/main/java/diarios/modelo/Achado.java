package diarios.modelo;

public class Achado {

    public static final String LEITURA = "LEITURA";
    public static final String ATENCAO = "ATENCAO";
    public static final String LIMITE = "LIMITE";

    private final String nivel;
    private final String titulo;
    private final String frase;
    private final String evidencia;

    public Achado(String nivel, String titulo, String frase, String evidencia) {
        this.nivel = nivel;
        this.titulo = titulo;
        this.frase = frase;
        this.evidencia = evidencia;
    }

    public String getNivel()     { return nivel; }
    public String getTitulo()    { return titulo; }
    public String getFrase()     { return frase; }
    public String getEvidencia() { return evidencia; }

    @Override
    public String toString() {
        return "[" + nivel + "] " + titulo + "\n      " + frase
                + "\n      evidencia: " + evidencia;
    }
}
