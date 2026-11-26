package diarios.empresas;

import com.fasterxml.jackson.core.JsonProcessingException;
import diarios.Aplicacao;
import diarios.erros.ColetaException;
import diarios.persistencia.Banco;
import java.sql.SQLException;
import java.util.Optional;

/** Busca a ficha de uma empresa: primeiro no cache do banco, depois na BrasilAPI. */
public class ServicoEmpresas {

    private final Banco banco;
    private final ClienteBrasilApi cliente;

    public ServicoEmpresas(Banco banco, ClienteBrasilApi cliente) {
        this.banco = banco;
        this.cliente = cliente;
    }

    public Optional<Empresa> doCache(String cnpj) throws SQLException {
        Optional<String> json = banco.jsonDaEmpresa(Cnpj.somenteDigitos(cnpj));
        if (json.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Empresa.deJson(json.get()));
        } catch (JsonProcessingException e) {
            return Optional.empty();
        }
    }

    /** Usa o cache se existir; senao consulta a API e guarda a resposta para a proxima vez. */
    public Empresa buscar(String cnpj) throws ColetaException, SQLException {
        Optional<Empresa> guardada = doCache(cnpj);
        if (guardada.isPresent()) {
            return guardada.get();
        }
        String json = cliente.consultar(cnpj);
        try {
            Empresa empresa = Empresa.deJson(json);
            banco.guardarEmpresa(Cnpj.somenteDigitos(cnpj), json, Aplicacao.agora());
            return empresa;
        } catch (JsonProcessingException e) {
            throw new ColetaException(cnpj, "resposta da BrasilAPI ilegivel: " + e.getOriginalMessage());
        }
    }
}
