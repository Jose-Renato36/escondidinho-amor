package diarios.modelo;

public class Contrato {

    private final String municipio;
    private final String data;
    private final String cnpj;
    private final String contratada;
    private final String papel;
    private final double valor;
    private final String processo;
    private final String identidadeAto;

    public Contrato(String municipio, String data, String cnpj, String contratada,
                    String papel, double valor, String processo, String identidadeAto) {
        this.municipio = municipio;
        this.data = data;
        this.cnpj = cnpj;
        this.contratada = contratada;
        this.papel = papel;
        this.valor = valor;
        this.processo = processo;
        this.identidadeAto = identidadeAto;
    }

    public String getPapel() { return papel; }

    public String getMunicipio()     { return municipio; }
    public String getData()          { return data; }
    public String getCnpj()          { return cnpj; }
    public String getContratada()    { return contratada; }
    public double getValor()         { return valor; }
    public String getProcesso()      { return processo; }
    public String getIdentidadeAto() { return identidadeAto; }

    public static String formatarReal(double v) {
        String s = String.format(java.util.Locale.ROOT, "%.2f", v);
        int ponto = s.indexOf('.');
        String inteiro = s.substring(0, ponto);
        String centavos = s.substring(ponto + 1);

        StringBuilder comPonto = new StringBuilder();
        int digitos = 0;
        for (int i = inteiro.length() - 1; i >= 0; i--) {
            comPonto.insert(0, inteiro.charAt(i));
            digitos++;
            if (digitos % 3 == 0 && i > 0) {
                comPonto.insert(0, '.');
            }
        }
        return "R$ " + comPonto + "," + centavos;
    }

    @Override
    public String toString() {
        return String.format("%-20s %-38s %18s  %s",
                cnpj, contratada, formatarReal(valor), processo);
    }
}
