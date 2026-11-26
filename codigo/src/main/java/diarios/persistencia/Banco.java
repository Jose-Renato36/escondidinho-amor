package diarios.persistencia;

import diarios.analise.GrafoNormas;
import diarios.analise.PainelContratos;
import diarios.modelo.Ato;
import diarios.modelo.Contrato;
import diarios.modelo.Diario;
import diarios.modelo.Relacao;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

/**
 * Guarda atos, contratos e relacoes entre normas num banco SQLite (um arquivo .db).
 * O resultado do pipeline continua sendo calculado em memoria; o banco e uma
 * "fotografia" dele que permite consultar com SQL sem reprocessar os diarios.
 */
public class Banco implements AutoCloseable {

    public static final String ARQUIVO_PADRAO = "dados/saida/diarios.db";

    private final Connection conexao;

    public Banco(String url) throws SQLException {
        this.conexao = DriverManager.getConnection(url);
        criarTabelas();
    }

    public static Banco emArquivo(String caminho) throws SQLException {
        return new Banco("jdbc:sqlite:" + caminho);
    }

    public static Banco emMemoria() throws SQLException {
        return new Banco("jdbc:sqlite::memory:");
    }

    private void criarTabelas() throws SQLException {
        try (Statement st = conexao.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS atos ("
                    + "identidade TEXT PRIMARY KEY, municipio TEXT, data TEXT, especie TEXT, "
                    + "numero TEXT, titulo TEXT, decisao TEXT, motivo TEXT, arquivo TEXT, texto TEXT)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS contratos ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, municipio TEXT, data TEXT, cnpj TEXT, "
                    + "contratada TEXT, papel TEXT, valor REAL, processo TEXT, ato TEXT)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS relacoes ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, origem TEXT, acao TEXT, alvo TEXT)");
        }
    }

    /** Troca o conteudo do banco pelo resultado do ultimo processamento, tudo numa transacao. */
    public void salvar(ArrayList<Diario> diarios, PainelContratos painel, GrafoNormas grafo)
            throws SQLException {
        conexao.setAutoCommit(false);
        try {
            try (Statement st = conexao.createStatement()) {
                st.executeUpdate("DELETE FROM atos");
                st.executeUpdate("DELETE FROM contratos");
                st.executeUpdate("DELETE FROM relacoes");
            }
            gravarAtos(diarios);
            gravarContratos(painel.getContratos());
            gravarRelacoes(grafo.getArestas());
            conexao.commit();
        } catch (SQLException | RuntimeException e) {
            conexao.rollback();
            throw e;
        } finally {
            conexao.setAutoCommit(true);
        }
    }

    private void gravarAtos(ArrayList<Diario> diarios) throws SQLException {
        String sql = "INSERT OR REPLACE INTO atos VALUES (?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conexao.prepareStatement(sql)) {
            for (Diario d : diarios) {
                for (Ato a : d.getAtos()) {
                    ps.setString(1, a.getIdentidade());
                    ps.setString(2, a.getMunicipio());
                    ps.setString(3, a.getDataDiario());
                    ps.setString(4, a.getEspecie());
                    ps.setString(5, a.getNumero());
                    ps.setString(6, a.getTitulo());
                    ps.setString(7, a.getDecisao().name());
                    ps.setString(8, a.getMotivo());
                    ps.setString(9, d.getArquivo());
                    ps.setString(10, a.getTexto());
                    ps.addBatch();
                }
            }
            ps.executeBatch();
        }
    }

    private void gravarContratos(ArrayList<Contrato> contratos) throws SQLException {
        String sql = "INSERT INTO contratos (municipio, data, cnpj, contratada, papel, valor, "
                + "processo, ato) VALUES (?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conexao.prepareStatement(sql)) {
            for (Contrato c : contratos) {
                ps.setString(1, c.getMunicipio());
                ps.setString(2, c.getData());
                ps.setString(3, c.getCnpj());
                ps.setString(4, c.getContratada());
                ps.setString(5, c.getPapel());
                ps.setDouble(6, c.getValor());
                ps.setString(7, c.getProcesso());
                ps.setString(8, c.getIdentidadeAto());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void gravarRelacoes(ArrayList<Relacao> relacoes) throws SQLException {
        String sql = "INSERT INTO relacoes (origem, acao, alvo) VALUES (?,?,?)";
        try (PreparedStatement ps = conexao.prepareStatement(sql)) {
            for (Relacao r : relacoes) {
                ps.setString(1, r.getOrigem());
                ps.setString(2, r.getAcao().name());
                ps.setString(3, r.getAlvo());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public int contar(String tabela) throws SQLException {
        if (!tabela.equals("atos") && !tabela.equals("contratos") && !tabela.equals("relacoes")) {
            throw new IllegalArgumentException("tabela desconhecida: " + tabela);
        }
        try (Statement st = conexao.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + tabela)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /** Cada linha: municipio, total contratado, numero de contratos. Maior total primeiro. */
    public ArrayList<Object[]> valorPorMunicipio() throws SQLException {
        return consultar("SELECT municipio, SUM(valor), COUNT(*) FROM contratos "
                + "GROUP BY municipio ORDER BY SUM(valor) DESC", 3);
    }

    /** Cada linha: cnpj, nome, total recebido, numero de contratos. Maior total primeiro. */
    public ArrayList<Object[]> maioresFornecedores(int limite) throws SQLException {
        return consultar("SELECT cnpj, MAX(contratada), SUM(valor), COUNT(*) FROM contratos "
                + "GROUP BY cnpj ORDER BY SUM(valor) DESC LIMIT " + Math.max(1, limite), 4);
    }

    /** Cada linha: decisao, quantidade. */
    public ArrayList<Object[]> atosPorDecisao() throws SQLException {
        return consultar("SELECT decisao, COUNT(*) FROM atos GROUP BY decisao "
                + "ORDER BY COUNT(*) DESC", 2);
    }

    /** Cada linha: identidade, municipio, data, titulo, decisao. Procura no titulo e no texto. */
    public ArrayList<Object[]> buscarAtos(String termo) throws SQLException {
        String sql = "SELECT identidade, municipio, data, titulo, decisao FROM atos "
                + "WHERE titulo LIKE ? ESCAPE '\\' OR texto LIKE ? ESCAPE '\\' "
                + "ORDER BY data DESC, identidade LIMIT 500";
        String padrao = "%" + termo.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        ArrayList<Object[]> linhas = new ArrayList<>();
        try (PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setString(1, padrao);
            ps.setString(2, padrao);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    linhas.add(lerLinha(rs, 5));
                }
            }
        }
        return linhas;
    }

    private ArrayList<Object[]> consultar(String sql, int colunas) throws SQLException {
        ArrayList<Object[]> linhas = new ArrayList<>();
        try (Statement st = conexao.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                linhas.add(lerLinha(rs, colunas));
            }
        }
        return linhas;
    }

    private static Object[] lerLinha(ResultSet rs, int colunas) throws SQLException {
        Object[] linha = new Object[colunas];
        for (int i = 0; i < colunas; i++) {
            linha[i] = rs.getObject(i + 1);
        }
        return linha;
    }

    @Override
    public void close() throws SQLException {
        conexao.close();
    }
}
