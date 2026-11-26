package diarios.empresas;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

/**
 * A ficha de uma empresa como a Receita Federal a publica (via BrasilAPI).
 * A leitura e tolerante: campo que nao veio na resposta vira texto vazio, nao erro.
 */
public class Empresa {

    private static final ObjectMapper LEITOR = new ObjectMapper();

    private final String cnpj;
    private final String razaoSocial;
    private final String nomeFantasia;
    private final String situacao;
    private final String inicioAtividade;
    private final String atividade;
    private final String porte;
    private final String municipio;
    private final String uf;
    private final double capitalSocial;

    private Empresa(JsonNode j) {
        this.cnpj = texto(j, "cnpj");
        this.razaoSocial = texto(j, "razao_social");
        this.nomeFantasia = texto(j, "nome_fantasia");
        this.situacao = texto(j, "descricao_situacao_cadastral");
        this.inicioAtividade = texto(j, "data_inicio_atividade");
        this.atividade = texto(j, "cnae_fiscal_descricao");
        String descricaoPorte = texto(j, "descricao_porte");
        this.porte = descricaoPorte.isEmpty() ? texto(j, "porte") : descricaoPorte;
        this.municipio = texto(j, "municipio");
        this.uf = texto(j, "uf");
        this.capitalSocial = j.path("capital_social").asDouble(0);
    }

    public static Empresa deJson(String json) throws JsonProcessingException {
        return new Empresa(LEITOR.readTree(json));
    }

    private static String texto(JsonNode j, String campo) {
        JsonNode n = j.path(campo);
        return n.isMissingNode() || n.isNull() ? "" : n.asText().trim();
    }

    public boolean isAtiva() {
        return situacao.equalsIgnoreCase("ATIVA");
    }

    /** Meses desde a abertura, ou -1 se a data nao veio ou nao e uma data. */
    public long mesesDeAtividade(LocalDate hoje) {
        try {
            return ChronoUnit.MONTHS.between(LocalDate.parse(inicioAtividade), hoje);
        } catch (DateTimeParseException e) {
            return -1;
        }
    }

    public String getCnpj()            { return cnpj; }
    public String getRazaoSocial()     { return razaoSocial; }
    public String getNomeFantasia()    { return nomeFantasia; }
    public String getSituacao()        { return situacao; }
    public String getInicioAtividade() { return inicioAtividade; }
    public String getAtividade()       { return atividade; }
    public String getPorte()           { return porte; }
    public String getMunicipio()       { return municipio; }
    public String getUf()              { return uf; }
    public double getCapitalSocial()   { return capitalSocial; }
}
