package diarios.etapas;

import diarios.extratores.Extrator;
import diarios.extratores.ExtratorCnpj;
import diarios.extratores.ExtratorData;
import diarios.extratores.ExtratorProcesso;
import diarios.extratores.ExtratorReferencia;
import diarios.extratores.ExtratorValor;
import diarios.modelo.Ato;
import diarios.modelo.Diario;
import diarios.modelo.Fato;
import java.util.ArrayList;

public class Extracao {

    private final ArrayList<Extrator> extratores = new ArrayList<>();

    public Extracao() {
        extratores.add(new ExtratorData());
        extratores.add(new ExtratorValor());
        extratores.add(new ExtratorCnpj());
        extratores.add(new ExtratorProcesso());
        extratores.add(new ExtratorReferencia());
    }

    public void extrair(Diario diario) {
        for (Ato ato : diario.getAtos()) {
            for (Extrator e : extratores) {
                for (Fato f : e.extrair(ato.getTexto())) {
                    ato.adicionarFato(new Fato(f.getTipo(), f.getValor(),
                            f.getInicio() + ato.getPosicao(),
                            f.getFim() + ato.getPosicao()));
                }
            }
        }
    }
}
