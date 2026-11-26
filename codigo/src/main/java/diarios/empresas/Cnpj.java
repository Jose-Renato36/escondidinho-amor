package diarios.empresas;

/** Operacoes sobre o numero do CNPJ, sem depender de rede. */
public final class Cnpj {

    private static final int[] PESOS_1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] PESOS_2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    private Cnpj() {
    }

    /** "39.316.457/0001-45" vira "39316457000145". */
    public static String somenteDigitos(String cnpj) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; cnpj != null && i < cnpj.length(); i++) {
            char c = cnpj.charAt(i);
            if (c >= '0' && c <= '9') {
                s.append(c);
            }
        }
        return s.toString();
    }

    /** Confere os dois digitos verificadores. Numero inventado quase sempre reprova aqui. */
    public static boolean valido(String cnpj) {
        String d = somenteDigitos(cnpj);
        if (d.length() != 14 || todosIguais(d)) {
            return false;
        }
        int primeiro = digito(d, PESOS_1);
        int segundo = digito(d, PESOS_2);
        return primeiro == d.charAt(12) - '0' && segundo == d.charAt(13) - '0';
    }

    /** "39316457000145" vira "39.316.457/0001-45". */
    public static String formatar(String cnpj) {
        String d = somenteDigitos(cnpj);
        if (d.length() != 14) {
            return cnpj;
        }
        return d.substring(0, 2) + "." + d.substring(2, 5) + "." + d.substring(5, 8) + "/"
                + d.substring(8, 12) + "-" + d.substring(12);
    }

    private static int digito(String d, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < pesos.length; i++) {
            soma += (d.charAt(i) - '0') * pesos[i];
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    private static boolean todosIguais(String d) {
        for (int i = 1; i < d.length(); i++) {
            if (d.charAt(i) != d.charAt(0)) {
                return false;
            }
        }
        return true;
    }
}
