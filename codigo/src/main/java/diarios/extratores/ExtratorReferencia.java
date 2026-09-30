package diarios.extratores;

import diarios.modelo.Fato;
import java.util.ArrayList;

public class ExtratorReferencia extends Extrator {

    private static final String[] ESPECIES = {
        "lei complementar", "lei", "decreto", "portaria", "resolucao", "instrucao normativa"
    };

    private static final String[] ESFERAS = {"municipal", "federal", "estadual", "conjunta"};

    public ExtratorReferencia() {
        super(Fato.REFERENCIA);
    }

    @Override
    public ArrayList<Fato> extrair(String texto) {
        ArrayList<Fato> achados = new ArrayList<>();
        String base = semAcento(texto.toLowerCase());

        for (String especie : ESPECIES) {
            int p = base.indexOf(especie);
            while (p >= 0) {
                boolean ehPrefixoDeOutra = especie.equals("lei")
                        && base.startsWith("lei complementar", p);
                if (!ehPrefixoDeOutra) {
                    int inicioNumero = pularAteNumero(base, texto, p + especie.length());
                    if (inicioNumero > 0) {
                        int fimNumero = medirNumero(base, inicioNumero);
                        if (fimNumero > inicioNumero) {
                            achados.add(criar(
                                    canonica(especie, texto.substring(inicioNumero, fimNumero)),
                                    p, fimNumero));
                        }
                    }
                }
                p = base.indexOf(especie, p + especie.length());
            }
        }
        return achados;
    }

    public static String canonica(String especie, String numero) {
        StringBuilder digitos = new StringBuilder();
        for (int i = 0; i < numero.length(); i++) {
            char c = numero.charAt(i);
            if (c == '/') {
                break;
            }
            if (c >= '0' && c <= '9') {
                digitos.append(c);
            }
        }
        return especie.replace(' ', '-') + ":" + digitos;
    }

    private int pularAteNumero(String base, String original, int posicao) {
        int limite = Math.min(base.length(), posicao + 28);
        for (int i = posicao; i < limite; i++) {
            char c = base.charAt(i);
            if (ehDigito(c)) {
                return i;
            }
            if (" .:-/,º°noum".indexOf(c) >= 0) {
                continue;
            }
            int pulo = tamanhoDaEsfera(base, i);
            if (pulo > 0) {
                i += pulo - 1;
                continue;
            }
            boolean siglaDeOrgao = Character.isLetter(c) && i < original.length()
                    && Character.isUpperCase(original.charAt(i));
            if (!siglaDeOrgao) {
                return -1;
            }
        }
        return -1;
    }

    private int tamanhoDaEsfera(String base, int i) {
        for (String esfera : ESFERAS) {
            if (base.startsWith(esfera, i)) {
                return esfera.length();
            }
        }
        return 0;
    }

    private int medirNumero(String texto, int inicio) {
        int p = inicio;
        while (p < texto.length()) {
            char c = texto.charAt(p);
            if (!ehDigito(c) && c != '.' && c != '/') {
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
