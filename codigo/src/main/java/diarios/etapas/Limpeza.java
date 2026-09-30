package diarios.etapas;

import diarios.erros.DiarioIlegivelException;
import diarios.modelo.Diario;

public class Limpeza {

    private static final int MINIMO_DE_CARACTERES = 3;
    private static final int MINIMO_DE_LINHAS_LONGAS = 10;
    private static final int LINHA_LONGA = 80;

    private static final String[] MOBILIA = {
        "CHAVE DE INÍCIO DA MATÉRIA",
        "CHAVE DE ACESSO DA MATÉRIA"
    };

    public void limpar(Diario diario) throws DiarioIlegivelException {
        String[] linhas = diario.getTextoBruto().split("\n");
        StringBuilder limpo = new StringBuilder();
        int removidas = 0;
        int longas = 0;

        for (String linha : linhas) {
            String s = linha.trim();
            if (s.isEmpty()) {
                continue;
            }
            if (s.length() <= MINIMO_DE_CARACTERES || ehMobiliaDePagina(s)) {
                removidas++;
                continue;
            }
            if (s.length() > LINHA_LONGA) {
                longas++;
            }
            limpo.append(s).append('\n');
        }

        diario.registrarLimpeza(limpo.toString(), removidas);

        if (longas < MINIMO_DE_LINHAS_LONGAS) {
            String motivo = "PDF sem texto extraivel (provavelmente escaneado)";
            diario.recusar(motivo);
            throw new DiarioIlegivelException(diario.getArquivo(), motivo,
                    longas + " linha(s) de corpo em " + linhas.length
                    + " linhas do arquivo; o minimo esperado e " + MINIMO_DE_LINHAS_LONGAS);
        }
    }

    private boolean ehMobiliaDePagina(String linha) {
        String s = linha.toUpperCase();
        for (String marca : MOBILIA) {
            if (s.contains(marca)) {
                return true;
            }
        }
        if (s.contains("DIÁRIO OFICIAL") && s.contains("EDIÇÃO N")) {
            return true;
        }
        return s.startsWith("HTTPS://") || s.startsWith("WWW.");
    }
}
