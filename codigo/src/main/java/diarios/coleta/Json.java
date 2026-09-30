package diarios.coleta;

import java.util.ArrayList;

public class Json {

    public static ArrayList<String> objetos(String resposta, String lista) {
        ArrayList<String> partes = new ArrayList<>();
        String marca = "\"" + lista + "\"";
        int p = resposta.indexOf(marca);
        if (p < 0) {
            return partes;
        }
        int abre = resposta.indexOf('[', p);
        if (abre < 0) {
            return partes;
        }

        int profundidade = 0;
        int inicio = -1;
        boolean dentroDeTexto = false;

        for (int i = abre; i < resposta.length(); i++) {
            char c = resposta.charAt(i);

            if (dentroDeTexto) {
                if (c == '\\') {
                    i++;
                } else if (c == '"') {
                    dentroDeTexto = false;
                }
                continue;
            }
            if (c == '"') {
                dentroDeTexto = true;
            } else if (c == '{') {
                if (profundidade == 0) {
                    inicio = i;
                }
                profundidade++;
            } else if (c == '}') {
                profundidade--;
                if (profundidade == 0 && inicio >= 0) {
                    partes.add(resposta.substring(inicio, i + 1));
                    inicio = -1;
                }
            } else if (c == ']' && profundidade == 0) {
                break;
            }
        }
        return partes;
    }

    public static String campo(String objeto, String chave) {
        String marca = "\"" + chave + "\"";
        int p = objeto.indexOf(marca);
        if (p < 0) {
            return "";
        }
        int i = objeto.indexOf(':', p + marca.length());
        if (i < 0) {
            return "";
        }
        i++;
        while (i < objeto.length() && objeto.charAt(i) == ' ') {
            i++;
        }
        if (i >= objeto.length()) {
            return "";
        }
        if (objeto.charAt(i) != '"') {
            return valorSemAspas(objeto, i);
        }
        i++;

        StringBuilder valor = new StringBuilder();
        while (i < objeto.length()) {
            char c = objeto.charAt(i);
            if (c == '\\' && i + 1 < objeto.length()) {
                char seguinte = objeto.charAt(i + 1);
                if (seguinte == 'n') {
                    valor.append('\n');
                } else if (seguinte == 'u' && i + 5 < objeto.length()) {
                    valor.append((char) Integer.parseInt(objeto.substring(i + 2, i + 6), 16));
                    i += 4;
                } else {
                    valor.append(seguinte);
                }
                i += 2;
                continue;
            }
            if (c == '"') {
                break;
            }
            valor.append(c);
            i++;
        }
        return valor.toString();
    }

    private static String valorSemAspas(String objeto, int inicio) {
        int fim = inicio;
        while (fim < objeto.length() && ",}]".indexOf(objeto.charAt(fim)) < 0) {
            fim++;
        }
        String valor = objeto.substring(inicio, fim).trim();
        return valor.equals("null") ? "" : valor;
    }
}
