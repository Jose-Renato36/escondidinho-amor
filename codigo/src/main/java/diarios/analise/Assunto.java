package diarios.analise;

import diarios.extratores.Extrator;
import diarios.modelo.Ato;
import diarios.modelo.Fato;

public class Assunto {

    public static final String CONTRATACAO = "CONTRATACAO";
    public static final String PESSOAL = "PESSOAL";
    public static final String NORMA = "NORMA";
    public static final String ORCAMENTO = "ORCAMENTO";
    public static final String LICITACAO = "LICITACAO";
    public static final String OUTRO = "OUTRO";

    private static final String[] MARCAS_PESSOAL = {
        "nomear", "nomeia", "exonerar", "exonera", "designar", "designa",
        "conceder licenca", "aposentadoria", "cargo em comissao", "servidor"
    };

    private static final String[] MARCAS_ORCAMENTO = {
        "credito adicional", "credito suplementar", "credito especial",
        "abre credito", "dotacao orcamentaria", "suplementacao"
    };

    private static final String[] MARCAS_LICITACAO = {
        "homologa", "adjudica", "pregao", "licitacao", "dispensa de licitacao",
        "inexigibilidade", "concorrencia", "chamamento publico"
    };

    private static final String[] MARCAS_NORMA = {
        "fica alterad", "ficam alterad", "passa a vigorar", "passam a vigorar",
        "fica revogad", "ficam revogad", "nova redacao", "decreta:", "resolve:"
    };

    public static String de(Ato ato) {
        String texto = Extrator.semAcento(ato.getTexto().toLowerCase());
        String especie = ato.getEspecie();

        boolean temDinheiro = !ato.getFatosDoTipo(Fato.VALOR).isEmpty();
        boolean temEmpresa = !ato.getFatosDoTipo(Fato.CNPJ).isEmpty();

        if (temEmpresa && temDinheiro) {
            return CONTRATACAO;
        }
        if (contem(texto, MARCAS_ORCAMENTO)) {
            return ORCAMENTO;
        }
        if (contem(texto, MARCAS_PESSOAL)) {
            return PESSOAL;
        }
        if (contem(texto, MARCAS_LICITACAO) || especie.equals("EDITAL")) {
            return LICITACAO;
        }
        if (contem(texto, MARCAS_NORMA)
                && (especie.equals("DECRETO") || especie.equals("LEI")
                    || especie.equals("LEI COMPLEMENTAR") || especie.equals("PORTARIA"))) {
            return NORMA;
        }
        return OUTRO;
    }

    private static boolean contem(String texto, String[] marcas) {
        for (String m : marcas) {
            if (texto.contains(m)) {
                return true;
            }
        }
        return false;
    }
}
