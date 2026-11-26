package diarios.analise;

import diarios.empresas.Cnpj;
import diarios.empresas.Empresa;
import diarios.modelo.Achado;
import diarios.modelo.Contrato;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;

/**
 * Sinais de atencao sobre um fornecedor, por regra. Cada sinal diz o que viu e,
 * principalmente, o que isso NAO prova: e um lugar onde vale olhar, nao uma acusacao.
 */
public class SinaisDaEmpresa {

    static final int MESES_DE_EMPRESA_NOVA = 12;
    static final double VEZES_O_CAPITAL = 10;
    static final int CONTRATOS_REPETIDOS = 3;

    /** @param empresa a ficha da Receita; pode ser null se ainda nao foi consultada */
    public ArrayList<Achado> avaliar(Fornecedor fornecedor, Empresa empresa) {
        ArrayList<Achado> sinais = new ArrayList<>();
        cnpjInvalido(fornecedor, sinais);
        repeticao(fornecedor, sinais);
        if (empresa != null) {
            situacao(empresa, sinais);
            empresaNova(fornecedor, empresa, sinais);
            capitalPequeno(fornecedor, empresa, sinais);
        }
        return sinais;
    }

    private void cnpjInvalido(Fornecedor f, ArrayList<Achado> sinais) {
        if (!Cnpj.valido(f.getCnpj())) {
            sinais.add(new Achado(Achado.ATENCAO, "CNPJ com dígito verificador inválido",
                    "O número publicado não passa na conferência dos dois últimos dígitos. "
                    + "Quase sempre é erro de digitação no próprio diário (já aconteceu de a mesma "
                    + "empresa aparecer com dois números no mesmo dia). Não prova nada sobre a "
                    + "empresa; mostra que o dado publicado tem erro.",
                    f.getCnpj()));
        }
    }

    private void repeticao(Fornecedor f, ArrayList<Achado> sinais) {
        if (f.getContratos() >= CONTRATOS_REPETIDOS) {
            sinais.add(new Achado(Achado.LEITURA, "Aparece em vários contratos",
                    "A empresa aparece em " + f.getContratos() + " contratos do período. Tem "
                    + "explicação comum (ata de registro de preços, fornecedor único de um insumo) "
                    + "e não indica irregularidade por si só.",
                    f.getContratos() + " contratos, " + f.getTotalFormatado()));
        }
    }

    private void situacao(Empresa e, ArrayList<Achado> sinais) {
        if (!e.getSituacao().isEmpty() && !e.isAtiva()) {
            sinais.add(new Achado(Achado.ATENCAO, "Situação cadastral: " + e.getSituacao(),
                    "Segundo a Receita, a empresa não está ativa hoje. Ela pode ter estado ativa "
                    + "na data do contrato; a consulta mostra a situação de agora, não a de então.",
                    "situação cadastral " + e.getSituacao()));
        }
    }

    private void empresaNova(Fornecedor f, Empresa e, ArrayList<Achado> sinais) {
        LocalDate data = dataDe(f.getPrimeiraData());
        if (data == null) {
            return;
        }
        long meses = e.mesesDeAtividade(data);
        if (meses >= 0 && meses < MESES_DE_EMPRESA_NOVA) {
            sinais.add(new Achado(Achado.ATENCAO, "Empresa aberta há pouco tempo",
                    "Quando apareceu contratada no diário, a empresa tinha " + meses + " mês(es) de "
                    + "atividade. Empresa nova pode ganhar contrato legitimamente (um negócio "
                    + "recém-criado, uma empresa que mudou de CNPJ); só é um ponto para olhar.",
                    "aberta em " + e.getInicioAtividade() + ", contratada em " + f.getPrimeiraData()));
        }
    }

    private void capitalPequeno(Fornecedor f, Empresa e, ArrayList<Achado> sinais) {
        if (e.getCapitalSocial() > 0 && f.getTotal() >= e.getCapitalSocial() * VEZES_O_CAPITAL) {
            long vezes = Math.round(f.getTotal() / e.getCapitalSocial());
            sinais.add(new Achado(Achado.ATENCAO, "Valor muito acima do capital social",
                    "O total contratado equivale a cerca de " + vezes + " vezes o capital social "
                    + "registrado. Muitas empresas pequenas têm capital baixo e executam contratos "
                    + "grandes sem problema; o número só mostra que o porte registrado é pequeno "
                    + "perto do contrato.",
                    f.getTotalFormatado() + " contratados x capital social "
                    + Contrato.formatarReal(e.getCapitalSocial())));
        }
    }

    private static LocalDate dataDe(String texto) {
        try {
            return LocalDate.parse(texto);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
