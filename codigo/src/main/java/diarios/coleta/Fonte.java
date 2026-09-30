package diarios.coleta;

import java.util.ArrayList;

public class Fonte {

    private static final String[][] CAPITAIS = {
        {"1200401", "Rio Branco", "AC"},
        {"2704302", "Maceió", "AL"},
        {"1600303", "Macapá", "AP"},
        {"1302603", "Manaus", "AM"},
        {"2927408", "Salvador", "BA"},
        {"2304400", "Fortaleza", "CE"},
        {"5300108", "Brasília", "DF"},
        {"3205309", "Vitória", "ES"},
        {"5208707", "Goiânia", "GO"},
        {"2111300", "São Luís", "MA"},
        {"5103403", "Cuiabá", "MT"},
        {"5002704", "Campo Grande", "MS"},
        {"3106200", "Belo Horizonte", "MG"},
        {"1501402", "Belém", "PA"},
        {"2507507", "João Pessoa", "PB"},
        {"4106902", "Curitiba", "PR"},
        {"2611606", "Recife", "PE"},
        {"2211001", "Teresina", "PI"},
        {"3304557", "Rio de Janeiro", "RJ"},
        {"2408102", "Natal", "RN"},
        {"4314902", "Porto Alegre", "RS"},
        {"1100205", "Porto Velho", "RO"},
        {"1400100", "Boa Vista", "RR"},
        {"4205407", "Florianópolis", "SC"},
        {"3550308", "São Paulo", "SP"},
        {"2800308", "Aracaju", "SE"},
        {"1721000", "Palmas", "TO"}
    };

    private final ArrayList<Municipio> municipios = new ArrayList<>();

    public Fonte() {
        for (String[] linha : CAPITAIS) {
            municipios.add(new Municipio(linha[0], linha[1], linha[2]));
        }
    }

    public ArrayList<Municipio> todos() {
        return municipios;
    }

    public Municipio porNome(String busca) {
        String alvo = busca.trim().toLowerCase();
        for (Municipio m : municipios) {
            if (m.getNome().toLowerCase().startsWith(alvo)
                    || m.getApelido().startsWith(alvo)
                    || m.getCodigo().equals(alvo)) {
                return m;
            }
        }
        return null;
    }

    public ArrayList<Municipio> padrao() {
        ArrayList<Municipio> lista = new ArrayList<>();
        String[] codigos = {"3106200", "2611606", "2304400", "4106902", "2800308"};
        for (String c : codigos) {
            Municipio m = porNome(c);
            if (m != null) {
                lista.add(m);
            }
        }
        return lista;
    }
}
