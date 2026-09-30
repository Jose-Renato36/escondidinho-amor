package diarios.etapas;

import diarios.extratores.Extrator;
import diarios.modelo.Ato;
import diarios.modelo.Diario;
import java.util.ArrayList;

public class Fragmentacao {

    private static final String[] ESPECIES = {
        "DECRETO", "LEI COMPLEMENTAR", "LEI", "PORTARIA", "RESOLUCAO", "EDITAL",
        "INSTRUCAO NORMATIVA", "EXTRATO", "TERMO", "AVISO", "ATO", "ORDEM DE SERVICO",
        "DELIBERACAO", "COMUNICADO", "RETIFICACAO"
    };

    private static final int CORPO_MINIMO = 200;
    private static final int TITULO_MAXIMO = 120;
    private static final int PARAGRAFO_MINIMO = 60;

    private int candidatos = 0;
    private int descartados = 0;

    public void fragmentar(Diario diario) {
        String texto = diario.getTextoLimpo();
        ArrayList<Integer> inicios = new ArrayList<>();
        ArrayList<String> titulos = new ArrayList<>();
        ArrayList<String> especies = new ArrayList<>();

        int posicao = 0;
        for (String linha : texto.split("\n")) {
            String s = linha.trim();
            String especie = especieDaLinha(s);
            if (especie != null && s.length() <= TITULO_MAXIMO) {
                inicios.add(posicao);
                titulos.add(s);
                especies.add(especie);
            }
            posicao += linha.length() + 1;
        }

        candidatos = inicios.size();
        descartados = 0;

        for (int i = 0; i < inicios.size(); i++) {
            int comeco = inicios.get(i);
            int fim = (i + 1 < inicios.size()) ? inicios.get(i + 1) : texto.length();
            String bloco = texto.substring(comeco, fim);

            if (!temCorpo(bloco, titulos.get(i))) {
                descartados++;
                continue;
            }
            Ato novo = new Ato(diario.getMunicipio(), diario.getData(),
                    especies.get(i), numeroDoTitulo(titulos.get(i)),
                    titulos.get(i), bloco, comeco);
            int repeticoes = contarIdentidade(diario, novo.getIdentidade());
            if (repeticoes > 0) {
                novo.desambiguar(repeticoes + 1);
            }
            diario.adicionarAto(novo);
        }
        diario.registrarFragmentacao(candidatos, descartados);
    }

    private int contarIdentidade(Diario diario, String identidade) {
        int n = 0;
        for (Ato a : diario.getAtos()) {
            if (a.getIdentidade().startsWith(identidade)) {
                n++;
            }
        }
        return n;
    }

    public int getCandidatos()  { return candidatos; }
    public int getDescartados() { return descartados; }

    private boolean temCorpo(String bloco, String titulo) {
        if (bloco.length() <= titulo.length()) {
            return false;
        }
        String semTitulo = bloco.substring(titulo.length());
        if (semTitulo.trim().length() < CORPO_MINIMO) {
            return false;
        }
        for (String l : semTitulo.split("\n")) {
            if (l.trim().length() > PARAGRAFO_MINIMO) {
                return true;
            }
        }
        return false;
    }

    private String especieDaLinha(String linha) {
        String s = Extrator.semAcento(linha.toUpperCase());
        for (String e : ESPECIES) {
            if (!s.startsWith(e)) {
                continue;
            }
            if (e.length() >= s.length()) {
                return e;
            }
            char depois = s.charAt(e.length());
            if (" Nº:-.".indexOf(depois) >= 0) {
                return e;
            }
        }
        return null;
    }

    private String numeroDoTitulo(String titulo) {
        StringBuilder num = new StringBuilder();
        boolean comecou = false;
        for (int i = 0; i < titulo.length(); i++) {
            char c = titulo.charAt(i);
            if (c >= '0' && c <= '9') {
                num.append(c);
                comecou = true;
            } else if (comecou && c != '.') {
                break;
            }
        }
        return num.toString();
    }
}
