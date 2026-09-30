package diarios.etapas;

public class Privacidade {

    private static final String MOLDE_CPF = "999.999.999-99";

    public static String mascararCpf(String texto) {
        if (texto == null) {
            return "";
        }
        StringBuilder saida = new StringBuilder(texto);
        int i = 0;
        while (i + MOLDE_CPF.length() <= saida.length()) {
            if (casa(saida, i) && !digitoAntes(saida, i)
                    && !digitoDepois(saida, i + MOLDE_CPF.length())) {
                for (int k = 0; k < 11; k++) {
                    char c = saida.charAt(i + k);
                    if (c >= '0' && c <= '9') {
                        saida.setCharAt(i + k, '*');
                    }
                }
                i += MOLDE_CPF.length();
            } else {
                i++;
            }
        }
        return saida.toString();
    }

    private static boolean casa(StringBuilder s, int p) {
        for (int k = 0; k < MOLDE_CPF.length(); k++) {
            char esperado = MOLDE_CPF.charAt(k);
            char achado = s.charAt(p + k);
            boolean ok = esperado == '9' ? (achado >= '0' && achado <= '9') : achado == esperado;
            if (!ok) {
                return false;
            }
        }
        return true;
    }

    private static boolean digitoAntes(StringBuilder s, int p) {
        return p > 0 && Character.isDigit(s.charAt(p - 1));
    }

    private static boolean digitoDepois(StringBuilder s, int p) {
        return p < s.length() && Character.isDigit(s.charAt(p));
    }
}
