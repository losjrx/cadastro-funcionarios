package trabalho.sd.rh;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Renderiza a listagem de funcionários em forma de tabela, registro a registro.
 *
 * Usada pelos dois clientes (TCP e gRPC) para que a saída das duas
 * implementações seja idêntica, permitindo comparar apenas o meio de
 * comunicação.
 *
 * As larguras são fixas de propósito: assim o cabeçalho pode ser impresso antes
 * de o primeiro registro chegar pela rede, preservando o comportamento de
 * streaming nas duas pontas.
 */
public class TabelaFuncionarios {
    private static final int LARG_INDICE  = 3;
    private static final int LARG_NOME    = 20;
    private static final int LARG_CARGO   = 20;
    private static final int LARG_SALARIO = 14;

    private final NumberFormat moeda = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));
    private final String separador;
    private final String formato;

    private int quantidade = 0;
    private double total = 0;

    public TabelaFuncionarios() {
        this.separador = "+" + "-".repeat(LARG_INDICE + 2)
                       + "+" + "-".repeat(LARG_NOME + 2)
                       + "+" + "-".repeat(LARG_CARGO + 2)
                       + "+" + "-".repeat(LARG_SALARIO + 2) + "+";

        // Índice e salário alinhados à direita; nome e cargo à esquerda
        this.formato = "| %" + LARG_INDICE + "s | %-" + LARG_NOME + "s | %-"
                     + LARG_CARGO + "s | %" + LARG_SALARIO + "s |%n";
    }

    public void imprimirCabecalho() {
        System.out.println(separador);
        System.out.printf(formato, "#", "Nome", "Cargo", "Salário");
        System.out.println(separador);
    }

    public void imprimirLinha(String nome, String cargo, double salario) {
        System.out.printf(formato,
            ++quantidade,
            truncar(nome, LARG_NOME),
            truncar(cargo, LARG_CARGO),
            moeda.format(salario));

        total += salario;
    }

    public void imprimirRodape() {
        // Só dá para saber que a lista está vazia depois do fim do stream,
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
}
