package trabalho.sd.rh.grpc;

import java.text.NumberFormat;
import java.util.Iterator;
import java.util.Locale;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import trabalho.sd.rh.grpc.*;

public class ClienteGrpc {
    private static final String HOST = "localhost";
    private static final int PORT = 9090;

    private static final int LARG_INDICE  = 3;
    private static final int LARG_NOME    = 20;
    private static final int LARG_CARGO   = 20;
    private static final int LARG_SALARIO = 14;

    public static void main(String[] args) throws InterruptedException {
        ManagedChannel channel = ManagedChannelBuilder.forAddress(HOST, PORT)
            .usePlaintext()
            .build();
        
        FuncionarioServiceGrpc.FuncionarioServiceBlockingStub stub = FuncionarioServiceGrpc.newBlockingStub(channel);
        
        try (Scanner scanner = new Scanner(System.in)) {
            boolean running = true;
            while (running) {
                showOptions();
                String option = scanner.nextLine();

                switch(option) {
                    case "1" -> {
                        System.out.print("Digite o nome do funcionário: ");
                        String nome = scanner.nextLine();

                        System.out.print("Digite o cargo do funcionário: ");
                        String cargo = scanner.nextLine();

                        System.out.print("Digite o salário do funcionário: ");
                        double salario = Double.parseDouble(scanner.nextLine());

                        FuncionarioResponse response = stub.cadastrar(FuncionarioRequest.newBuilder()
                            .setNome(nome)
                            .setCargo(cargo)
                            .setSalario(salario)
                            .build());
                        
                        System.out.println("Funcionário " + response.getNome() + " cadastrado com sucesso!");
                    }
                    case "2" -> exibirTabela(stub.listar(Empty.newBuilder().build()));
                    case "3" -> running = false;
                    default -> System.out.println("Opção inválida. Tente novamente.");
                }
            }
        } finally {
            channel.shutdown().awaitTermination(3, TimeUnit.SECONDS);
        }
    }

    private static void exibirTabela(Iterator<FuncionarioResponse> funcionarios) {
        NumberFormat moeda = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));

        String separador = "+" + "-".repeat(LARG_INDICE + 2)
                         + "+" + "-".repeat(LARG_NOME + 2)
                         + "+" + "-".repeat(LARG_CARGO + 2)
                         + "+" + "-".repeat(LARG_SALARIO + 2) + "+";

        // Índice e salário alinhados à direita; nome e cargo à esquerda
        String formato = "| %" + LARG_INDICE + "s | %-" + LARG_NOME + "s | %-"
                       + LARG_CARGO + "s | %" + LARG_SALARIO + "s |%n";

        // Cabeçalho sai antes do primeiro registro chegar pela rede
        System.out.println(separador);
        System.out.printf(formato, "#", "Nome", "Cargo", "Salário");
        System.out.println(separador);

        int quantidade = 0;
        double total = 0;

        // hasNext() bloqueia até o próximo registro chegar: cada linha é
        // impressa assim que o servidor a envia, sem guardar a lista inteira
        while (funcionarios.hasNext()) {
            FuncionarioResponse f = funcionarios.next();

            System.out.printf(formato,
                ++quantidade,
                truncar(f.getNome(), LARG_NOME),
                truncar(f.getCargo(), LARG_CARGO),
                moeda.format(f.getSalario()));

            total += f.getSalario();
        }

        // Só dá para saber que a lista está vazia depois do stream terminar,
        // quando o cabeçalho já foi impresso
        if (quantidade == 0) {
            System.out.printf(formato, "-", "(nenhum registro)", "", "");
        }

        System.out.println(separador);
        System.out.printf("%d funcionário(s) | Folha total: %s%n%n",
            quantidade, moeda.format(total));
    }

    // Com largura fixa, textos maiores que a coluna precisam ser cortados.
    // As colunas são largas o bastante para o "..." caber.
    private static String truncar(String texto, int largura) {
        if (texto.length() <= largura) {
            return texto;
        }
        return texto.substring(0, largura - 3) + "...";
    }

    private static void showOptions() {
        System.out.println("Escolha uma opção:");
        System.out.println("1. Cadastrar funcionário");
        System.out.println("2. Listar funcionários");
        System.out.println("3. Sair");
    }
}
