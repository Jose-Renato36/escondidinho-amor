package diarios.modelo;

import java.util.ArrayList;

public class Ato {

    public static final String ACEITO = "ACEITO";
    public static final String DUVIDOSO = "DUVIDOSO";
    public static final String REJEITADO = "REJEITADO";

    private final String municipio;
    private final String dataDiario;
    private final String especie;
    private final String numero;
    private final String titulo;
    private final String texto;
    private final int posicaoNoDiario;

    private final ArrayList<Fato> fatos = new ArrayList<>();

    private String decisao = DUVIDOSO;
    private String motivo = "ainda nao avaliado";
    private String desambiguador = "";

    public Ato(String municipio, String dataDiario, String especie, String numero,
               String titulo, String texto, int posicaoNoDiario) {
        this.municipio = municipio;
        this.dataDiario = dataDiario;
        this.especie = especie;
        this.numero = numero;
        this.titulo = titulo;
        this.texto = texto;
        this.posicaoNoDiario = posicaoNoDiario;
    }

    public String getIdentidade() {
        String num = numero.isEmpty() ? "s-num@" + posicaoNoDiario : numero;
        return municipio + ":" + especie.toLowerCase() + ":" + num + ":" + dataDiario
                + desambiguador;
    }

    public void desambiguar(int ordem) {
        this.desambiguador = "#" + ordem;
    }

    public void adicionarFato(Fato f) {
        fatos.add(f);
    }

    public ArrayList<Fato> getFatos() {
        return fatos;
    }

    public ArrayList<Fato> getFatosDoTipo(String tipo) {
        ArrayList<Fato> achados = new ArrayList<>();
        for (Fato f : fatos) {
            if (f.getTipo().equals(tipo)) {
                achados.add(f);
            }
        }
        return achados;
    }

    public void decidir(String decisao, String motivo) {
        this.decisao = decisao;
        this.motivo = motivo;
    }

    public String getMunicipio()  { return municipio; }
    public String getDataDiario() { return dataDiario; }
    public String getEspecie()    { return especie; }
    public String getNumero()     { return numero; }
    public String getTitulo()     { return titulo; }
    public String getTexto()      { return texto; }
    public String getDecisao()    { return decisao; }
    public String getMotivo()     { return motivo; }
    public int getPosicao()       { return posicaoNoDiario; }

    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (!(outro instanceof Ato)) {
            return false;
        }
        return getIdentidade().equals(((Ato) outro).getIdentidade());
    }

    @Override
    public int hashCode() {
        return getIdentidade().hashCode();
    }

    @Override
    public String toString() {
        return String.format("%-9s %-46s %d fatos", decisao, getIdentidade(), fatos.size());
    }
}
