package diarios.analise;

import diarios.modelo.Ato;
import diarios.modelo.Diario;
import java.util.ArrayList;

public class Cobertura {

    private final ArrayList<String> linhas = new ArrayList<>();

    private int lidos = 0;
    private int utilizaveis = 0;
    private int recusados = 0;
    private int totalAtos = 0;
    private int aceitos = 0;
    private int duvidosos = 0;
    private int rejeitados = 0;

    public void limpar() {
        linhas.clear();
        lidos = utilizaveis = recusados = 0;
        totalAtos = aceitos = duvidosos = rejeitados = 0;
    }

    public void registrarRecusa(String arquivo, String motivo, String evidencia) {
        lidos++;
        recusados++;
        linhas.add("  " + arquivo);
        linhas.add("     RECUSADO: " + motivo);
        linhas.add("     evidencia: " + evidencia);
        linhas.add("");
    }

    public void registrarProcessado(Diario d) {
        lidos++;
        utilizaveis++;
        totalAtos += d.getAtos().size();
        aceitos += d.contar(Ato.ACEITO);
        duvidosos += d.contar(Ato.DUVIDOSO);
        rejeitados += d.contar(Ato.REJEITADO);

        linhas.add("  " + d.getArquivo());
        linhas.add(String.format("     limpeza       : %d linhas -> %d removidas como lixo",
                d.getLinhasOriginais(), d.getLinhasRemovidas()));
        linhas.add(String.format("     fragmentacao  : %d titulos candidatos, %d descartados "
                + "(sem corpo), %d atos",
                d.getTitulosCandidatos(), d.getTitulosDescartados(), d.getAtos().size()));
        linhas.add(String.format("     portao        : ACEITO %d - DUVIDOSO %d - REJEITADO %d",
                d.contar(Ato.ACEITO), d.contar(Ato.DUVIDOSO), d.contar(Ato.REJEITADO)));
        linhas.add("");
    }

    public ArrayList<String> montar() {
        ArrayList<String> saida = new ArrayList<>();
        saida.add("RELATORIO DE COBERTURA");
        saida.add("======================");
        saida.add("");
        saida.add(String.format("  diarios lidos : %d", lidos));
        saida.add(String.format("  utilizaveis   : %d", utilizaveis));
        saida.add(String.format("  recusados     : %d", recusados));
        saida.add("");
        saida.addAll(linhas);
        saida.add("TOTAIS");
        saida.add(String.format("  atos      : %d", totalAtos));
        saida.add(String.format("  ACEITO    : %d", aceitos));
        saida.add(String.format("  DUVIDOSO  : %d", duvidosos));
        saida.add(String.format("  REJEITADO : %d", rejeitados));
        return saida;
    }

    public int getLidos()       { return lidos; }
    public int getUtilizaveis() { return utilizaveis; }
    public int getRecusados()   { return recusados; }
    public int getTotalAtos()   { return totalAtos; }
    public int getAceitos()     { return aceitos; }
    public int getDuvidosos()   { return duvidosos; }
    public int getRejeitados()  { return rejeitados; }
}
