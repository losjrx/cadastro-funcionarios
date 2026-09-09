package trabalho.sd.rh.tcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import trabalho.sd.rh.TabelaFuncionarios;

public class ClienteTcp {
    private static final String HOST = "localhost";
    private static final int PORT = 5000;

    public static void main(String[] args) throws IOException {
        try (Socket socket = new Socket(HOST, PORT);
             BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter output = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
             BufferedReader keyboard = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))
        ) {
           boolean running = true;
           while (running) {
                showOptions();
                String option = keyboard.readLine();
                output.println(option);

                switch (option) {
                    case "1" -> {
                        System.out.print(input.readLine()); // Nome
                        output.println(keyboard.readLine());

                        System.out.print(input.readLine()); // Cargo
                        output.println(keyboard.readLine());

                        System.out.print(input.readLine()); // Salário
                        output.println(keyboard.readLine());

                        System.out.println();
                    }
                    case "2" -> renderizar(input);
                    case "3" -> {
                        System.out.print("Digite o cargo a consultar: ");
                        output.println(keyboard.readLine());

                        renderizar(input);
                    }
                    case "4" -> {
                        running = false;
                    }
                    default -> {
                        System.out.println("Opção inválida. Tente novamente.");
                        System.out.println();
                    }
                }
           }
        }       
    }

    private static void renderizar(BufferedReader input) throws IOException {
        TabelaFuncionarios tabela = new TabelaFuncionarios();
        tabela.imprimirCabecalho();

        // Cada registro é renderizado assim que chega, sem acumular a lista
        String linha;
        while ((linha = input.readLine()) != null && !linha.equals("END")) {
            String[] campos = linha.split("\t");

            if (campos.length == 3) {
                tabela.imprimirLinha(campos[0], campos[1], Double.parseDouble(campos[2]));
            } else {
                // Linha fora do formato esperado (ex.: mensagem de erro do servidor)
                System.out.println(linha);
            }
        }

        tabela.imprimirRodape();
    }

    private static void showOptions() {
        System.out.println("Escolha uma opção:");
        System.out.println("1. Cadastrar funcionário");
        System.out.println("2. Listar funcionários");
        System.out.println("3. Consultar funcionários por cargo");
        System.out.println("4. Sair");
    }
}
