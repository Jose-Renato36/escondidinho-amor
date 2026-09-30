package diarios.extratores;

import diarios.modelo.Fato;
import java.util.ArrayList;

public class ExtratorValor extends Extrator {

    public ExtratorValor() {
        super(Fato.VALOR);
    }

    @Override
    public ArrayList<Fato> extrair(String texto) {
        ArrayList<Fato> achados = new ArrayList<>();
        int i = texto.indexOf("R$");
        while (i >= 0) {
            int fim = medir(texto, i);
            if (fim > 0) {
                achados.add(criar(texto.substring(i, fim), i, fim));
                i = texto.indexOf("R$", fim);
            } else {
                i = texto.indexOf("R$", i + 2);
            }
        }
        return achados;
    }

    private int medir(String texto, int inicioRS) {
        int p = inicioRS + 2;
        while (p < texto.length() && (texto.charAt(p) == ' ' || texto.charAt(p) == ' ')) {
            p++;
        }
        if (contarDigitos(texto, p) == 0) {
            return -1;
        }
        while (p < texto.length() && (ehDigito(texto.charAt(p)) || texto.charAt(p) == '.')) {
            p++;
        }
        boolean temCentavos = p + 2 < texto.length() && texto.charAt(p) == ','
                && ehDigito(texto.charAt(p + 1)) && ehDigito(texto.charAt(p + 2));
        if (!temCentavos) {
            return -1;
        }
        boolean digitoExtra = p + 3 < texto.length() && ehDigito(texto.charAt(p + 3));
        return digitoExtra ? -1 : p + 3;
    }

    public static double paraNumero(String valorTexto) {
        StringBuilder limpo = new StringBuilder();
        for (int i = 0; i < valorTexto.length(); i++) {
            char c = valorTexto.charAt(i);
            if (c >= '0' && c <= '9') {
                limpo.append(c);
            } else if (c == ',') {
                limpo.append('.');
            }
        }
        try {
            return Double.parseDouble(limpo.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
