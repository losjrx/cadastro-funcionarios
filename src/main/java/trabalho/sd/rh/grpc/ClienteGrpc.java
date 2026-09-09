package trabalho.sd.rh.grpc;

import java.util.Iterator;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import trabalho.sd.rh.TabelaFuncionarios;
import trabalho.sd.rh.grpc.*;

public class ClienteGrpc {
    private static final String HOST = "localhost";
    private static final int PORT = 9090;

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
                    case "2" -> renderizar(stub.listar(Empty.newBuilder().build()));
                    case "3" -> {
                        System.out.print("Digite o cargo a consultar: ");
                        String cargo = scanner.nextLine();

                        renderizar(stub.listarPorCargo(CargoRequest.newBuilder()
                            .setCargo(cargo)
                            .build()));
                    }
                    case "4" -> running = false;
                    default -> System.out.println("Opção inválida. Tente novamente.");
                }
            }
        } finally {
            channel.shutdown().awaitTermination(3, TimeUnit.SECONDS);
        }
    }

    // hasNext() bloqueia até o próximo registro chegar: cada linha é impressa
    // assim que o servidor a envia, sem acumular a lista em memória
    private static void renderizar(Iterator<FuncionarioResponse> funcionarios) {
        TabelaFuncionarios tabela = new TabelaFuncionarios();
        tabela.imprimirCabecalho();

        while (funcionarios.hasNext()) {
            FuncionarioResponse f = funcionarios.next();
            tabela.imprimirLinha(f.getNome(), f.getCargo(), f.getSalario());
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
