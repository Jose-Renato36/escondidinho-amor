package diarios.servidor;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class Formulario {

    public static String parametro(String dados, String chave) {
        if (dados == null || dados.isEmpty()) {
            return "";
        }
        for (String par : dados.split("&")) {
            int igual = par.indexOf('=');
            if (igual < 0) {
                continue;
            }
            if (decodificar(par.substring(0, igual)).equals(chave)) {
                return decodificar(par.substring(igual + 1)).trim();
            }
        }
        return "";
    }

    public static int inteiro(String dados, String chave, int padrao) {
        String valor = parametro(dados, chave);
        if (valor.isEmpty()) {
            return padrao;
        }
        try {
            return Integer.parseInt(valor);
        } catch (NumberFormatException e) {
            return padrao;
        }
    }

    public static String codificar(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }

    private static String decodificar(String s) {
        try {
            return URLDecoder.decode(s, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return s;
        }
    }
}
