package diarios.extratores;

import diarios.modelo.Fato;
import java.util.ArrayList;

public class ExtratorCnpj extends Extrator {

    private static final String MOLDE = "99.999.999/9999-99";

    public ExtratorCnpj() {
        super(Fato.CNPJ);
    }

    @Override
    public ArrayList<Fato> extrair(String texto) {
        ArrayList<Fato> achados = new ArrayList<>();
        for (int i = 0; i + MOLDE.length() <= texto.length(); i++) {
            if (casaMolde(texto, i, MOLDE)) {
                achados.add(criar(texto.substring(i, i + MOLDE.length()), i, i + MOLDE.length()));
                i += MOLDE.length() - 1;
            }
        }
        return achados;
    }
}
