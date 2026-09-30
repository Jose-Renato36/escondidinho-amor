package diarios.coleta;

public class EstadoFonte {

    public static final String NUNCA_TENTADA = "NUNCA_TENTADA";
    public static final String COM_DADO = "COM_DADO";
    public static final String SEM_EDICAO = "SEM_EDICAO";
    public static final String SO_PDF = "SO_PDF";
    public static final String FALHOU = "FALHOU";

    private final Municipio municipio;
    private final boolean descoberta;
    private boolean ligada;
    private String status = NUNCA_TENTADA;
    private String ultimaTentativa = "-";
    private String motivo = "";
    private int diariosNoAcervo = 0;
    private String coberturaDesde = "";
    private String coberturaAte = "";

    public EstadoFonte(Municipio municipio, boolean ligada, boolean descoberta) {
        this.municipio = municipio;
        this.ligada = ligada;
        this.descoberta = descoberta;
    }

    public void registrarResultado(String quando, ResultadoColeta r, String desde, String ate) {
        ultimaTentativa = quando;
        motivo = "";
        coberturaDesde = "";
        coberturaAte = "";
        if (r.isFalhouRede()) {
            status = FALHOU;
            motivo = r.getMotivoFalha();
            return;
        }
        coberturaDesde = desde;
        coberturaAte = ate;
        if (r.getBaixados() > 0 || r.getJaExistiam() > 0) {
            status = COM_DADO;
        } else if (r.getSemTexto() > 0) {
            status = SO_PDF;
        } else {
            status = SEM_EDICAO;
        }
    }

    public boolean jaCobre(String desde, String ate) {
        boolean resolvida = status.equals(COM_DADO) || status.equals(SEM_EDICAO)
                || status.equals(SO_PDF);
        return resolvida && !coberturaDesde.isEmpty()
                && coberturaDesde.compareTo(desde) <= 0 && coberturaAte.compareTo(ate) >= 0;
    }

    public void restaurarCobertura(String desde, String ate) {
        this.coberturaDesde = desde;
        this.coberturaAte = ate;
    }

    public void restaurar(String status, String ultimaTentativa, String motivo) {
        this.status = status;
        this.ultimaTentativa = ultimaTentativa;
        this.motivo = motivo;
    }

    public void marcarComoVistaNaDescoberta(String quando) {
        if (status.equals(NUNCA_TENTADA)) {
            status = COM_DADO;
            ultimaTentativa = quando;
            motivo = "vista na descoberta";
        }
    }

    public Municipio getMunicipio()     { return municipio; }
    public boolean isLigada()           { return ligada; }
    public boolean isDescoberta()       { return descoberta; }
    public String getStatus()           { return status; }
    public String getUltimaTentativa()  { return ultimaTentativa; }
    public String getMotivo()           { return motivo; }
    public int getDiariosNoAcervo()     { return diariosNoAcervo; }
    public String getCoberturaDesde()   { return coberturaDesde; }
    public String getCoberturaAte()     { return coberturaAte; }

    public void setLigada(boolean ligada)       { this.ligada = ligada; }
    public void setDiariosNoAcervo(int n)       { this.diariosNoAcervo = n; }
}
