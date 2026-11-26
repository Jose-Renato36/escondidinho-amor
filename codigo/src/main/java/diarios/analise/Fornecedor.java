package diarios.analise;

import diarios.modelo.Contrato;

/** Uma empresa e tudo que ela recebeu no periodo, somado por CNPJ. */
public class Fornecedor {

    private static final String SEM_NOME = "(nao identificada)";

    private final String cnpj;
    private String nome;
    private double total = 0;
    private int contratos = 0;

    public Fornecedor(String cnpj, String nome) {
        this.cnpj = cnpj;
        this.nome = nome;
    }

    /** O nome da empresa aparece escrito de varias formas; guarda o primeiro que for util. */
    public void somar(Contrato c) {
        total += c.getValor();
        contratos++;
        if (nome.equals(SEM_NOME)) {
            nome = c.getContratada();
        }
    }

    public String getCnpj()      { return cnpj; }
    public String getNome()      { return nome; }
    public double getTotal()     { return total; }
    public int getContratos()    { return contratos; }

    public String getTotalFormatado() {
        return Contrato.formatarReal(total);
    }
}
