package diarios.modelo;

import java.util.ArrayList;

public class Diario {

    private final String arquivo;
    private final String municipio;
    private final String data;
    private final String textoBruto;
    private final int linhasOriginais;

    private String textoLimpo = "";
    private int linhasRemovidas = 0;
    private int titulosCandidatos = 0;
    private int titulosDescartados = 0;

    private final ArrayList<Ato> atos = new ArrayList<>();

    private boolean utilizavel = true;
    private String motivoRecusa = "";

    public Diario(String arquivo, String municipio, String data, String textoBruto) {
        this.arquivo = arquivo;
        this.municipio = municipio;
        this.data = data;
        this.textoBruto = textoBruto;
        this.linhasOriginais = textoBruto.split("\n").length;
    }

    public void registrarLimpeza(String textoLimpo, int linhasRemovidas) {
        this.textoLimpo = textoLimpo;
        this.linhasRemovidas = linhasRemovidas;
    }

    public void registrarFragmentacao(int candidatos, int descartados) {
        this.titulosCandidatos = candidatos;
        this.titulosDescartados = descartados;
    }

    public void recusar(String motivo) {
        this.utilizavel = false;
        this.motivoRecusa = motivo;
    }

    public void adicionarAto(Ato a) {
        atos.add(a);
    }

    public int contar(String decisao) {
        int n = 0;
        for (Ato a : atos) {
            if (a.getDecisao().equals(decisao)) {
                n++;
            }
        }
        return n;
    }

    public String getArquivo()          { return arquivo; }
    public String getMunicipio()        { return municipio; }
    public String getData()             { return data; }
    public String getTextoBruto()       { return textoBruto; }
    public String getTextoLimpo()       { return textoLimpo; }
    public int getLinhasRemovidas()     { return linhasRemovidas; }
    public int getLinhasOriginais()     { return linhasOriginais; }
    public int getTitulosCandidatos()   { return titulosCandidatos; }
    public int getTitulosDescartados()  { return titulosDescartados; }
    public ArrayList<Ato> getAtos()     { return atos; }
    public boolean isUtilizavel()       { return utilizavel; }
    public String getMotivoRecusa()     { return motivoRecusa; }

    @Override
    public String toString() {
        if (!utilizavel) {
            return municipio + " " + data + "  RECUSADO: " + motivoRecusa;
        }
        return municipio + " " + data + "  " + atos.size() + " atos";
    }
}
