package diarios.coleta;

import java.util.ArrayList;

public class Rodada {

    private final String inicio;
    private final String tipo;
    private final int dias;
    private final String desde;
    private final String ate;
    private final ArrayList<ResultadoColeta> resultados = new ArrayList<>();
    private String fim = "";
    private String observacao = "";

    public Rodada(String inicio, String tipo, int dias, String desde, String ate) {
        this.inicio = inicio;
        this.tipo = tipo;
        this.dias = dias;
        this.desde = desde;
        this.ate = ate;
    }

    public static Rodada daLinha(String linha) {
        String[] c = linha.split(";", -1);
        if (c.length < 11) {
            return null;
        }
        Rodada r = new Rodada(c[0], c[1], Integer.parseInt(c[2]), c[3], c[4]);
        r.fim = c[5];
        r.observacao = c[10];
        r.puladasSalvas = c.length >= 12 ? Integer.parseInt(c[11]) : 0;
        r.resumoSalvo = new int[] {
            Integer.parseInt(c[6]), Integer.parseInt(c[7]),
            Integer.parseInt(c[8]), Integer.parseInt(c[9])
        };
        return r;
    }

    private int[] resumoSalvo = null;
    private int puladasSalvas = 0;

    public void adicionar(ResultadoColeta r) {
        resultados.add(r);
    }

    public void finalizar(String quando, String observacao) {
        this.fim = quando;
        this.observacao = observacao;
    }

    public int fontes() {
        return resumoSalvo != null ? resumoSalvo[0] : resultados.size();
    }

    public int baixados() {
        if (resumoSalvo != null) {
            return resumoSalvo[1];
        }
        int n = 0;
        for (ResultadoColeta r : resultados) {
            n += r.getBaixados();
        }
        return n;
    }

    public int jaExistiam() {
        if (resumoSalvo != null) {
            return resumoSalvo[2];
        }
        int n = 0;
        for (ResultadoColeta r : resultados) {
            n += r.getJaExistiam();
        }
        return n;
    }

    public int falhas() {
        if (resumoSalvo != null) {
            return resumoSalvo[3];
        }
        int n = 0;
        for (ResultadoColeta r : resultados) {
            if (r.isFalhouRede()) {
                n++;
            }
        }
        return n;
    }

    public int puladas() {
        if (resumoSalvo != null) {
            return puladasSalvas;
        }
        int n = 0;
        for (ResultadoColeta r : resultados) {
            if (r.isPulada()) {
                n++;
            }
        }
        return n;
    }

    public ArrayList<String> registro() {
        ArrayList<String> linhas = new ArrayList<>();
        for (ResultadoColeta r : resultados) {
            linhas.addAll(r.getRegistro());
        }
        return linhas;
    }

    public String paraLinha() {
        return String.join(";", inicio, tipo, String.valueOf(dias), desde, ate, fim,
                String.valueOf(fontes()), String.valueOf(baixados()),
                String.valueOf(jaExistiam()), String.valueOf(falhas()),
                observacao.replace(';', ',').replace('\n', ' '), String.valueOf(puladas()));
    }

    public String getInicio()     { return inicio; }
    public String getTipo()       { return tipo; }
    public int getDias()          { return dias; }
    public String getDesde()      { return desde; }
    public String getAte()        { return ate; }
    public String getFim()        { return fim; }
    public String getObservacao() { return observacao; }
}
