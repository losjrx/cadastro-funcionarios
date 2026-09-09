package trabalho.sd.rh;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FuncionarioDAO {
    public void cadastrarFuncionario(Funcionario funcionario) throws SQLException {
        String sql = "INSERT INTO funcionarios (nome, cargo, salario) VALUES (?, ?, ?)";

        try (Connection conn = ConexaoDB.conectar(); PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, funcionario.getNome());
            stmt.setString(2, funcionario.getCargo());
            stmt.setDouble(3, funcionario.getSalario());

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao cadastrar funcionário: " + e.getMessage(), e);
        }
    }

    public List<Funcionario> listarFuncionarios() throws SQLException {
        List<Funcionario> funcionarios = new ArrayList<>();
        String sql = "SELECT * FROM funcionarios";

        try (Connection conn = ConexaoDB.conectar(); 
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                funcionarios.add(mapResultSetToFuncionario(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar funcionários: " + e.getMessage(), e);
        }

        return funcionarios;
    }

    public List<Funcionario> listarPorCargo(String cargo) throws SQLException {
        List<Funcionario> funcionarios = new ArrayList<>();
        // LOWER nos dois lados torna a busca indiferente a maiúsculas/minúsculas
        String sql = "SELECT * FROM funcionarios WHERE LOWER(cargo) = LOWER(?)";

        try (Connection conn = ConexaoDB.conectar();
            PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, cargo.trim());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    funcionarios.add(mapResultSetToFuncionario(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao consultar funcionários por cargo: " + e.getMessage(), e);
        }

        return funcionarios;
    }

    private Funcionario mapResultSetToFuncionario(ResultSet rs) throws SQLException {
        String nome = rs.getString("nome");
        String cargo = rs.getString("cargo");
        double salario = rs.getDouble("salario");

        return new Funcionario(nome, cargo, salario);
    }
}
