package diarios.analise;

import diarios.extratores.ExtratorValor;
import diarios.modelo.Ato;
import diarios.modelo.Contrato;
import diarios.modelo.Diario;
import diarios.modelo.Fato;
import java.util.ArrayList;

public class PainelContratos {

    private static final String[] SUFIXOS = {
        " LTDA", " S/A", " S.A", " EIRELI", " ME", " EPP", " COOPERATIVA"
    };

    private static final String[] RUIDOS = {
        "denominada ", "denominado ", "empresa ", "contratada ", "contratado ",
        "favor de ", "a ", "o "
    };

    private static final int JANELA_NOME = 220;
    private static final int NOME_MINIMO = 4;
    private static final int NOME_MAXIMO = 70;
    private static final int MAIUSCULAS_MINIMAS = 70;

    public static final String LADO_MUNICIPIO = "LADO_MUNICIPIO";
    public static final String NOTIFICADO = "NOTIFICADO";
    public static final String FORNECEDOR = "FORNECEDOR";
    public static final String INDEFINIDO = "INDEFINIDO";

    private static final String[][] PAPEIS = {
        {"NOTIFICANTE", LADO_MUNICIPIO},
        {"CONTRATANTE", LADO_MUNICIPIO},
        {"NOTIFICADO", NOTIFICADO},
        {"AUTUADO", NOTIFICADO},
        {"CONTRATADA", FORNECEDOR},
        {"CONTRATADO", FORNECEDOR},
        {"DETENTORA", FORNECEDOR},
        {"FAVORECIDO", FORNECEDOR},
        {"VENCEDORA", FORNECEDOR},
        {"ADJUDICAT", FORNECEDOR},
        {"FORNECEDOR", FORNECEDOR},
        {"EMPRESA", FORNECEDOR}
    };

    private static final int JANELA_PAPEL = 320;

    private final ArrayList<Contrato> contratos = new ArrayList<>();
    private final ArrayList<Contrato> descartados = new ArrayList<>();

    public void limpar() {
        contratos.clear();
        descartados.clear();
    }

    public void processar(ArrayList<Diario> diarios) {
        for (Diario d : diarios) {
            if (!d.isUtilizavel()) {
                continue;
            }
            for (Ato ato : d.getAtos()) {
                ArrayList<Fato> cnpjs = ato.getFatosDoTipo(Fato.CNPJ);
                ArrayList<Fato> valores = ato.getFatosDoTipo(Fato.VALOR);
                if (cnpjs.isEmpty() || valores.isEmpty()) {
                    continue;
                }
                ArrayList<Fato> processos = ato.getFatosDoTipo(Fato.PROCESSO);
                String processo = processos.isEmpty() ? "-" : processos.get(0).getValor();

                for (Fato cnpj : cnpjs) {
                    Fato valor = maisProximo(valores, cnpj.getInicio());
                    String[] papelENome = papelENomeAntesDe(d.getTextoLimpo(), cnpj.getInicio());

                    Contrato c = new Contrato(d.getMunicipio(), d.getData(),
                            cnpj.getValor(), papelENome[1], papelENome[0],
                            ExtratorValor.paraNumero(valor.getValor()),
                            processo, ato.getIdentidade());

                    if (papelENome[0].equals(FORNECEDOR) || papelENome[0].equals(INDEFINIDO)) {
                        contratos.add(c);
                    } else {
                        descartados.add(c);
                    }
                }
            }
        }
    }

    private Fato maisProximo(ArrayList<Fato> valores, int posicao) {
        Fato melhor = valores.get(0);
        int menorDistancia = Integer.MAX_VALUE;
        for (Fato v : valores) {
            int d = Math.abs(v.getInicio() - posicao);
            if (d < menorDistancia) {
                menorDistancia = d;
                melhor = v;
            }
        }
        return melhor;
    }

    private String[] papelENomeAntesDe(String texto, int posicaoCnpj) {
        int de = Math.max(0, posicaoCnpj - JANELA_PAPEL);
        String janela = texto.substring(de, posicaoCnpj).replace('\n', ' ');
        String maiusculo = janela.toUpperCase();

        int ultimaMarca = -1;
        String papel = INDEFINIDO;
        for (String[] par : PAPEIS) {
            int p = maiusculo.lastIndexOf(par[0]);
            if (p > ultimaMarca) {
                ultimaMarca = p;
                papel = par[1];
            }
        }
        if (ultimaMarca < 0) {
            return new String[] {INDEFINIDO, nomeAntesDe(texto, posicaoCnpj)};
        }
        return new String[] {papel, nomeDepoisDaMarca(janela, ultimaMarca)};
    }

    private String nomeDepoisDaMarca(String janela, int marca) {
        int i = marca;
        while (i < janela.length() && janela.charAt(i) != ':' && janela.charAt(i) != ' ') {
            i++;
        }
        while (i < janela.length() && (janela.charAt(i) == ':' || janela.charAt(i) == ' ')) {
            i++;
        }
        String resto = janela.substring(Math.min(i, janela.length()));

        int corte = resto.length();
        String[] paradas = {",", ";", " sito", " SITO", " inscrit", " INSCRIT",
                            "CNPJ", "cnpj", " – ", " - "};
        for (String parada : paradas) {
            int p = resto.indexOf(parada);
            if (p > 0 && p < corte) {
                corte = p;
            }
        }
        String nome = limparBordas(resto.substring(0, corte));
        return nome.length() >= NOME_MINIMO && nome.length() <= NOME_MAXIMO
                ? nome : "(nao identificada)";
    }

    private String limparBordas(String s) {
        String nome = s.trim();
        while (!nome.isEmpty() && !Character.isLetterOrDigit(nome.charAt(0))) {
            nome = nome.substring(1).trim();
        }
        while (!nome.isEmpty() && ":,;-–".indexOf(nome.charAt(nome.length() - 1)) >= 0) {
            nome = nome.substring(0, nome.length() - 1).trim();
        }
        return tirarRestoDePalavra(nome);
    }

    private String tirarRestoDePalavra(String nome) {
        int espaco = nome.indexOf(' ');
        if (espaco < 0 || espaco > 3) {
            return nome;
        }
        String primeira = nome.substring(0, espaco);
        boolean itemDeLista = primeira.endsWith(")") || primeira.endsWith(":")
                || primeira.endsWith(".");
        boolean pedacoDePalavra = primeira.length() <= 2
                && primeira.equals(primeira.toLowerCase());
        return (itemDeLista || pedacoDePalavra) ? nome.substring(espaco + 1).trim() : nome;
    }

    private String nomeAntesDe(String texto, int posicaoCnpj) {
        int de = Math.max(0, posicaoCnpj - JANELA_NOME);
        String antes = texto.substring(de, posicaoCnpj).replace('\n', ' ');
        String maiusculo = antes.toUpperCase();

        int fimDoNome = -1;
        for (String sufixo : SUFIXOS) {
            int p = maiusculo.lastIndexOf(sufixo);
            int fim = p + sufixo.length();
            if (p >= 0 && fim > fimDoNome && terminaAqui(maiusculo, fim)) {
                fimDoNome = fim;
            }
        }
        if (fimDoNome > 0) {
            String nome = limparBordas(recuarNome(antes, fimDoNome));
            if (nome.length() >= NOME_MINIMO) {
                return nome;
            }
        }
        return nomePorSeparador(antes, maiusculo);
    }

    private String nomePorSeparador(String antes, String maiusculo) {
        int marcaCnpj = maiusculo.lastIndexOf("CNPJ");
        String ateMarca = marcaCnpj > 0 ? antes.substring(0, marcaCnpj) : antes;

        int corte = -1;
        for (char sep : new char[] {';', ':', '.'}) {
            corte = Math.max(corte, ateMarca.lastIndexOf(sep));
        }
        String nome = (corte >= 0 ? ateMarca.substring(corte + 1) : ateMarca).trim();
        while (!nome.isEmpty() && ",- ".indexOf(nome.charAt(nome.length() - 1)) >= 0) {
            nome = nome.substring(0, nome.length() - 1).trim();
        }
        nome = limparBordas(nome);
        boolean aceitavel = nome.length() >= NOME_MINIMO && nome.length() <= NOME_MAXIMO
                && pareceRazaoSocial(nome);
        return aceitavel ? nome : "(nao identificada)";
    }

    private boolean terminaAqui(String texto, int posicao) {
        return posicao >= texto.length() || !Character.isLetterOrDigit(texto.charAt(posicao));
    }

    private String recuarNome(String texto, int fimDoNome) {
        int p = fimDoNome;
        int limite = Math.max(0, fimDoNome - NOME_MAXIMO);
        int inicio = fimDoNome;
        while (p > limite) {
            char c = texto.charAt(p - 1);
            boolean parteDoNome = Character.isLetterOrDigit(c) || " &-./".indexOf(c) >= 0;
            if (!parteDoNome) {
                break;
            }
            p--;
            if (c != ' ') {
                inicio = p;
            }
        }
        String nome = texto.substring(inicio, fimDoNome).trim();
        for (String ruido : RUIDOS) {
            if (nome.toLowerCase().startsWith(ruido)) {
                nome = nome.substring(ruido.length()).trim();
            }
        }
        return nome;
    }

    private boolean pareceRazaoSocial(String s) {
        int maiusculas = 0;
        int letras = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isLetter(c)) {
                letras++;
                if (Character.isUpperCase(c)) {
                    maiusculas++;
                }
            }
        }
        return letras > 0 && (maiusculas * 100 / letras) >= MAIUSCULAS_MINIMAS;
    }

    public ArrayList<Contrato> getContratos() {
        return contratos;
    }

    public ArrayList<Contrato> getDescartados() {
        return descartados;
    }

    public int contarDescartadosDoPapel(String papel) {
        int n = 0;
        for (Contrato c : descartados) {
            if (c.getPapel().equals(papel)) {
                n++;
            }
        }
        return n;
    }

    public double total() {
        double soma = 0;
        for (Contrato c : contratos) {
            soma += c.getValor();
        }
        return soma;
    }

    public ArrayList<String[]> ranking() {
        ArrayList<String> cnpjs = new ArrayList<>();
        ArrayList<String> nomes = new ArrayList<>();
        ArrayList<Double> somas = new ArrayList<>();
        ArrayList<Integer> vezes = new ArrayList<>();

        for (Contrato c : contratos) {
            int i = cnpjs.indexOf(c.getCnpj());
            if (i < 0) {
                cnpjs.add(c.getCnpj());
                nomes.add(c.getContratada());
                somas.add(c.getValor());
                vezes.add(1);
            } else {
                somas.set(i, somas.get(i) + c.getValor());
                vezes.set(i, vezes.get(i) + 1);
                if (nomes.get(i).equals("(nao identificada)")) {
                    nomes.set(i, c.getContratada());
                }
            }
        }

        ArrayList<String[]> linhas = new ArrayList<>();
        boolean[] usado = new boolean[cnpjs.size()];
        for (int n = 0; n < cnpjs.size(); n++) {
            int melhor = -1;
            for (int i = 0; i < cnpjs.size(); i++) {
                if (!usado[i] && (melhor < 0 || somas.get(i) > somas.get(melhor))) {
                    melhor = i;
                }
            }
            usado[melhor] = true;
            linhas.add(new String[] {
                cnpjs.get(melhor), nomes.get(melhor),
                Contrato.formatarReal(somas.get(melhor)), String.valueOf(vezes.get(melhor))
            });
        }
        return linhas;
    }

    public ArrayList<String[]> repeticoes(int minimo) {
        ArrayList<String[]> alertas = new ArrayList<>();
        for (String[] linha : ranking()) {
            if (Integer.parseInt(linha[3]) >= minimo) {
                alertas.add(linha);
            }
        }
        return alertas;
    }
}
