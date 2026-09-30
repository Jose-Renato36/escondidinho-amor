package diarios.extratores;

import diarios.modelo.Fato;
import java.util.ArrayList;

public class ExtratorData extends Extrator {

    private static final String[] MESES = {
        "janeiro", "fevereiro", "marco", "abril", "maio", "junho",
        "julho", "agosto", "setembro", "outubro", "novembro", "dezembro"
    };

    public ExtratorData() {
        super(Fato.DATA);
    }

    @Override
    public ArrayList<Fato> extrair(String texto) {
        ArrayList<Fato> achados = new ArrayList<>();
        acharNumericas(texto, achados);
        acharPorExtenso(texto, achados);
        return achados;
    }

    private void acharNumericas(String texto, ArrayList<Fato> achados) {
        for (int i = 0; i < texto.length(); i++) {
            if (casaMolde(texto, i, "99/99/9999")) {
                achados.add(criar(texto.substring(i, i + 10), i, i + 10));
                i += 9;
            } else if (casaMolde(texto, i, "99/99/99")) {
                achados.add(criar(texto.substring(i, i + 8), i, i + 8));
                i += 7;
            }
        }
    }

    private void acharPorExtenso(String texto, ArrayList<Fato> achados) {
        String base = semAcento(texto.toLowerCase());
        for (String mes : MESES) {
            int p = base.indexOf(mes);
            while (p >= 0) {
                int inicio = recuarAteDia(base, p);
                int fim = avancarAteAno(base, p + mes.length());
                if (inicio >= 0 && fim > 0) {
                    achados.add(criar(texto.substring(inicio, fim), inicio, fim));
                }
                p = base.indexOf(mes, p + mes.length());
            }
        }
    }

    private int recuarAteDia(String texto, int posMes) {
        int p = posMes - 1;
        if (p < 4 || texto.charAt(p) != ' ') {
            return -1;
        }
        if (!texto.startsWith("de", p - 2)) {
            return -1;
        }
        p -= 3;
        if (p < 0 || texto.charAt(p) != ' ') {
            return -1;
        }
        int fimDia = p;
        while (p > 0 && (ehDigito(texto.charAt(p - 1)) || texto.charAt(p - 1) == 'º')) {
            p--;
        }
        return p < fimDia ? p : -1;
    }

    private int avancarAteAno(String texto, int depoisDoMes) {
        int p = depoisDoMes;
        if (p + 4 > texto.length() || texto.charAt(p) != ' ') {
            return -1;
        }
        if (!texto.startsWith("de ", p + 1)) {
            return -1;
        }
        p += 4;
        return contarDigitos(texto, p) == 4 ? p + 4 : -1;
    }
}
