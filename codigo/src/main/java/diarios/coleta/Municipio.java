package diarios.coleta;

public class Municipio {

    private final String codigo;
    private final String nome;
    private final String uf;

    public Municipio(String codigo, String nome, String uf) {
        this.codigo = codigo;
        this.nome = nome;
        this.uf = uf;
    }

    public String getCodigo() { return codigo; }
    public String getNome()   { return nome; }
    public String getUf()     { return uf; }

    public String getApelido() {
        StringBuilder s = new StringBuilder();
        String base = nome.toLowerCase();
        for (int i = 0; i < base.length(); i++) {
            char c = base.charAt(i);
            if (c == ' ') {
                s.append('-');
            } else if (Character.isLetterOrDigit(c)) {
                s.append(semAcento(c));
            }
        }
        return s.toString();
    }

    private char semAcento(char c) {
        String comAcento = "áàãâéêíóôõúç";
        String sem = "aaaaeeiooouc";
        int i = comAcento.indexOf(c);
        return i >= 0 ? sem.charAt(i) : c;
    }

    @Override
    public String toString() {
        return nome + " (" + uf + ")";
    }
}
