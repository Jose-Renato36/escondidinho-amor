package diarios.coleta;

import java.util.ArrayList;

public class ResultadoColeta {

    private final Municipio municipio;
    private final ArrayList<String> registro = new ArrayList<>();
    private int baixados = 0;
    private int semTexto = 0;
    private int jaExistiam = 0;
    private int edicoes = 0;
    private boolean falhouRede = false;
    private boolean pulada = false;
    private String motivoFalha = "";

    public ResultadoColeta(Municipio municipio) {
        this.municipio = municipio;
    }

    public void anotar(String linha)    { registro.add(linha); }
    public void contarEdicao()          { edicoes++; }
    public void contarBaixado()         { baixados++; }
    public void contarSemTexto()        { semTexto++; }
    public void contarJaExistia()       { jaExistiam++; }

    public void marcarFalhaDeRede(String motivo) {
        falhouRede = true;
        motivoFalha = motivo;
    }

    public Municipio getMunicipio()          { return municipio; }
    public ArrayList<String> getRegistro()   { return registro; }
    public int getBaixados()                 { return baixados; }
    public int getSemTexto()                 { return semTexto; }
    public int getJaExistiam()               { return jaExistiam; }
    public int getEdicoes()                  { return edicoes; }
    public boolean isFalhouRede()            { return falhouRede; }
    public boolean isPulada()                { return pulada; }
    public void marcarPulada()               { pulada = true; }
    public String getMotivoFalha()           { return motivoFalha; }
}
