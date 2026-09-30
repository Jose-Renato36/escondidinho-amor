package diarios.extratores;

import diarios.modelo.Fato;
import java.util.ArrayList;

public class ExtratorProcesso extends Extrator {

    private static final String MARCA = "rocesso";
    private static final int TAMANHO_MINIMO = 6;

    public ExtratorProcesso() {
        super(Fato.PROCESSO);
    }

    @Override
    public ArrayList<Fato> extrair(String texto) {
        ArrayList<Fato> achados = new ArrayList<>();
        int p = texto.indexOf(MARCA);
        while (p >= 0) {
            int inicio = pularAteNumero(texto, p + MARCA.length());
            if (inicio > 0) {
                int fim = medirNumero(texto, inicio);
                if (fim - inicio >= TAMANHO_MINIMO) {
                    achados.add(criar(texto.substring(inicio, fim), inicio, fim));
                }
            }
            p = texto.indexOf(MARCA, p + MARCA.length());
        }
        return achados;
    }

    private int pularAteNumero(String texto, int posicao) {
        int limite = Math.min(texto.length(), posicao + 12);
        for (int i = posicao; i < limite; i++) {
            char c = texto.charAt(i);
            if (ehDigito(c)) {
                return i;
            }
            boolean aceitavel = c == ':' || c == ' ' || c == 'n' || c == 'N'
                    || c == 'º' || c == '°' || c == '.';
            if (!aceitavel) {
                return -1;
            }
        }
        return -1;
    }

    private int medirNumero(String texto, int inicio) {
        int p = inicio;
        while (p < texto.length()) {
            char c = texto.charAt(p);
            if (!ehDigito(c) && c != '.' && c != '-' && c != '/') {
                break;
            }
            p++;
        }
        while (p > inicio && !ehDigito(texto.charAt(p - 1))) {
            p--;
        }
        return p;
    }
}
