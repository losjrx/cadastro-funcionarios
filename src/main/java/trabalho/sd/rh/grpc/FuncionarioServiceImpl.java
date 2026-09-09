package trabalho.sd.rh.grpc;

import io.grpc.stub.StreamObserver;

import java.util.List;

import trabalho.sd.rh.Funcionario;
import trabalho.sd.rh.FuncionarioDAO;
import trabalho.sd.rh.grpc.*;

public class FuncionarioServiceImpl extends FuncionarioServiceGrpc.FuncionarioServiceImplBase {

    private final FuncionarioDAO dao = new FuncionarioDAO();

    @Override
    public void cadastrar(FuncionarioRequest request, StreamObserver<FuncionarioResponse> responseObserver) {
        try {
            Funcionario funcionario = new Funcionario(request.getNome(), request.getCargo(), request.getSalario());
            dao.cadastrarFuncionario(funcionario);

            responseObserver.onNext(FuncionarioResponse.newBuilder()
                    .setNome(funcionario.getNome())
                    .setCargo(funcionario.getCargo())
                    .setSalario(funcionario.getSalario())
                    .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void listar(Empty request, StreamObserver<FuncionarioResponse> responseObserver) {
        try {
            List<Funcionario> funcionarios = dao.listarFuncionarios();
            for (Funcionario f : funcionarios) {
                responseObserver.onNext(FuncionarioResponse.newBuilder()
                        .setNome(f.getNome())
                        .setCargo(f.getCargo())
                        .setSalario(f.getSalario())
                        .build());
            }
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void listarPorCargo(CargoRequest request, StreamObserver<FuncionarioResponse> responseObserver) {
        try {
            List<Funcionario> funcionarios = dao.listarPorCargo(request.getCargo());
            for (Funcionario f : funcionarios) {
                responseObserver.onNext(FuncionarioResponse.newBuilder()
                        .setNome(f.getNome())
                        .setCargo(f.getCargo())
                        .setSalario(f.getSalario())
                        .build());
            }
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }
}
