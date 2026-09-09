package trabalho.sd.rh.tcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import trabalho.sd.rh.ConexaoDB;
import trabalho.sd.rh.Funcionario;
import trabalho.sd.rh.FuncionarioDAO;

public class ServidorTcp {
    private static final int PORT = 5000;
    private static final int THREAD_POOL_SIZE = 10;

    public static void main(String[] args) throws IOException {
        ConexaoDB.inicializarBanco();
        ExecutorService pool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
     
        try (ServerSocket server = new ServerSocket(PORT)) {
            System.out.println("Servidor TCP iniciado na porta " + PORT);

            while (true) {
                Socket clientSocket = server.accept();
                pool.submit(() -> handleClient(clientSocket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            pool.shutdown();
        }
    }

    private static void handleClient(Socket clientSocket) {
        try (clientSocket;
            BufferedReader input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
            PrintWriter output = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8), true)
        ) {
            boolean running = true;
            while (running) {
                String option = input.readLine();
                if (option == null) break;
                
                switch (option) {
                    case "1" -> { 
                        try {
                            output.println("Digite o nome do funcionário: ");
                            String nome = input.readLine();

                            output.println("Digite o cargo do funcionário: ");
                            String cargo = input.readLine();

                            output.println("Digite o salário do funcionário: ");
                            double salario = Double.parseDouble(input.readLine());
                            
                            FuncionarioDAO funcionarioDAO = new FuncionarioDAO();
                            Funcionario funcionario = new Funcionario(nome, cargo, salario);
                            funcionarioDAO.cadastrarFuncionario(funcionario);
                        } catch (SQLException e) {
                            output.println("Erro ao cadastrar funcionário: " + e.getMessage());
                        }
                        output.println("Funcionário cadastrado com sucesso!");
                    }
                    case "2" -> {
                        try {
                            FuncionarioDAO funcionarioDAO = new FuncionarioDAO();
                            List<Funcionario> funcionarios = funcionarioDAO.listarFuncionarios();
                            // Envia os campos crus separados por TAB; quem formata é o cliente
                            for (Funcionario f : funcionarios) {
                                output.println(f.getNome() + "\t" + f.getCargo() + "\t" + f.getSalario());
                            }
                            output.println("END"); // Indica fim da lista
                        } catch (SQLException e) {
                            output.println("Erro ao listar funcionários: " + e.getMessage());
                            output.println("END"); // Encerra a lista mesmo em caso de erro
                        }
                    }
                    case "3" -> {
                        try {
                            String cargo = input.readLine();

                            if (cargo != null) {
                                FuncionarioDAO funcionarioDAO = new FuncionarioDAO();
                                List<Funcionario> funcionarios = funcionarioDAO.listarPorCargo(cargo);
                                // Envia os campos crus separados por TAB; quem formata é o cliente
                                for (Funcionario f : funcionarios) {
                                    output.println(f.getNome() + "\t" + f.getCargo() + "\t" + f.getSalario());
                                }
                                output.println("END"); // Indica fim da lista
                            }
                        } catch (SQLException e) {
                            output.println("Erro ao consultar funcionários por cargo: " + e.getMessage());
                            output.println("END"); // Encerra a lista mesmo em caso de erro
                        }
                    }
                    case "4" -> running = false;
                    default -> output.println("Opção inválida. Tente novamente.");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }  
    }
}
