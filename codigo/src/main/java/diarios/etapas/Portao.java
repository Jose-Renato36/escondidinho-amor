package diarios.etapas;

import diarios.modelo.Ato;
import diarios.modelo.Decisao;
import diarios.modelo.Diario;
import diarios.modelo.Fato;
import diarios.modelo.TipoFato;

public class Portao {

    private static final int TEXTO_MINIMO = 200;
    private static final int FATOS_MINIMOS = 2;

    public void avaliar(Diario diario) {
        for (Ato ato : diario.getAtos()) {
            avaliarAto(ato);
        }
    }

    private void avaliarAto(Ato ato) {
        int tamanho = ato.getTexto().trim().length();
        if (tamanho < TEXTO_MINIMO) {
            ato.decidir(Decisao.REJEITADO, "texto curto demais (" + tamanho
                    + " caracteres, minimo " + TEXTO_MINIMO + ")");
            return;
        }
        if (ato.getEspecie().isEmpty()) {
            ato.decidir(Decisao.REJEITADO, "sem especie identificada");
            return;
        }

        int datas = ato.getFatosDoTipo(TipoFato.DATA).size();
        int total = ato.getFatos().size();
        boolean temNumero = !ato.getNumero().isEmpty();
        boolean temDinheiro = !ato.getFatosDoTipo(TipoFato.VALOR).isEmpty()
                || !ato.getFatosDoTipo(TipoFato.CNPJ).isEmpty();
        boolean temNorma = !ato.getFatosDoTipo(TipoFato.REFERENCIA).isEmpty();

        if (total < FATOS_MINIMOS) {
            ato.decidir(Decisao.DUVIDOSO, "poucos fatos extraidos (" + total + ")");
            return;
        }
        if (!temNumero && !temDinheiro && !temNorma) {
            ato.decidir(Decisao.DUVIDOSO, "sem numero, sem valor/CNPJ e sem norma citada");
            return;
        }
        if (datas == 0) {
            ato.decidir(Decisao.DUVIDOSO, "nenhuma data encontrada no ato");
            return;
        }

        StringBuilder porque = new StringBuilder("tem ");
        if (temNumero) {
            porque.append("numero, ");
        }
        if (temDinheiro) {
            porque.append("valor/CNPJ, ");
        }
        if (temNorma) {
            porque.append("norma citada, ");
        }
        porque.append(datas).append(" data(s), ").append(total).append(" fatos");
        ato.decidir(Decisao.ACEITO, porque.toString());
    }
}
