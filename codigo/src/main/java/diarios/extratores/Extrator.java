package diarios.extratores;

import diarios.modelo.Fato;
import java.util.ArrayList;

public abstract class Extrator {

    private final String tipo;

    protected Extrator(String tipo) {
        this.tipo = tipo;
    }

    public String getTipo() {
        return tipo;
    }

    public abstract ArrayList<Fato> extrair(String texto);

    protected Fato criar(String valor, int inicio, int fim) {
        return new Fato(tipo, valor, inicio, fim);
    }

    protected static boolean ehDigito(char c) {
        return c >= '0' && c <= '9';
    }

    protected static int contarDigitos(String texto, int posicao) {
        int n = 0;
        while (posicao + n < texto.length() && ehDigito(texto.charAt(posicao + n))) {
            n++;
        }
        return n;
    }

    protected static boolean casaMolde(String texto, int posicao, String molde) {
        if (posicao + molde.length() > texto.length()) {
            return false;
        }
        for (int i = 0; i < molde.length(); i++) {
            char esperado = molde.charAt(i);
            char achado = texto.charAt(posicao + i);
            if (esperado == '9' ? !ehDigito(achado) : achado != esperado) {
                return false;
            }
        }
        return true;
    }

    public static String semAcento(String s) {
        return s.replace('ç', 'c').replace('ã', 'a').replace('á', 'a')
                .replace('â', 'a').replace('à', 'a').replace('é', 'e')
                .replace('ê', 'e').replace('í', 'i').replace('ó', 'o')
                .replace('ô', 'o').replace('õ', 'o').replace('ú', 'u');
    }
}
