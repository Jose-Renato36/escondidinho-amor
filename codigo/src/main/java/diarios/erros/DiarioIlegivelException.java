package diarios.erros;

public class DiarioIlegivelException extends Exception {

    private final String arquivo;
    private final String evidencia;

    public DiarioIlegivelException(String arquivo, String motivo, String evidencia) {
        super(motivo);
        this.arquivo = arquivo;
        this.evidencia = evidencia;
    }

    public String getArquivo()   { return arquivo; }
    public String getEvidencia() { return evidencia; }
}
