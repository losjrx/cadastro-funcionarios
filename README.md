# Cadastro de Funcionários

Aplicação cliente-servidor para cadastro e listagem de funcionários, com
persistência em PostgreSQL. O projeto traz **duas implementações equivalentes**
da mesma funcionalidade, para fins de comparação (disciplina de Sistemas
Distribuídos):

- **TCP puro** (`trabalho.sd.rh.tcp`) — sockets `Socket`/`ServerSocket` com
  protocolo textual próprio, servidor multi-thread (pool de 10 threads).
- **gRPC** (`trabalho.sd.rh.grpc`) — serviço definido em
  [`funcionario.proto`](src/main/proto/funcionario.proto), com RPC unário para
  cadastro e RPC de streaming de servidor para listagem.

Ambas usam a mesma camada de domínio/persistência (`Funcionario`,
`FuncionarioDAO`, `ConexaoDB`).

## Pré-requisitos

- Java JDK 21 ou superior
- Maven
- PostgreSQL em execução (padrão: `localhost:5432`)

## Configuração do banco de dados

A aplicação espera um banco chamado `funcionarios`. **Ele precisa existir antes
de rodar o servidor** — a aplicação cria a tabela automaticamente, mas não o
banco.

### 1. Criar o banco

```bash
psql -U postgres -c "CREATE DATABASE funcionarios;"
```

### 2. Credenciais

As credenciais de acesso estão em
[`ConexaoDB.java`](src/main/java/trabalho/sd/rh/ConexaoDB.java):

| Parâmetro | Valor padrão                                    |
| --------- | ----------------------------------------------- |
| URL       | `jdbc:postgresql://localhost:5432/funcionarios` |
| Usuário   | `postgres`                                      |
| Senha     | `postgres`                                      |

Se o usuário `postgres` tiver outra senha, ajuste no arquivo ou defina a senha:

```bash
psql -U postgres -c "ALTER USER postgres PASSWORD 'postgres';"
```

### 3. Tabela

A tabela `funcionarios` é criada automaticamente ao iniciar o **servidor TCP**
(`ConexaoDB.inicializarBanco()`), a partir de
[`src/main/resources/db/schema.sql`](src/main/resources/db/schema.sql)
(`CREATE TABLE IF NOT EXISTS`, seguro rodar várias vezes).

> ```bash
> psql -U postgres -d funcionarios -f src/main/resources/db/schema.sql
> ```

## Instalando e configurando o PostgreSQL no servidor (Linux)

Passo a passo para preparar o banco em uma máquina Linux que será acessada
remotamente pelo cliente.

### 1. Instalar o PostgreSQL no servidor Linux (Ubuntu/Debian)

```bash
sudo apt install postgresql postgresql-contrib -y
```

**Inicie e habilite o serviço para rodar com o sistema:**

```bash
sudo systemctl start postgresql
sudo systemctl enable postgresql
```

### 2. Criar o banco e o usuário para a aplicação

**Acesse o terminal do PostgreSQL via usuário padrão `postgres`:**

```bash
sudo -u postgres psql
```

**Dentro do prompt do PostgreSQL (`postgres=#`), execute:**

```sql
-- Criar o banco de dados
CREATE DATABASE funcionarios;

-- Definir a senha do usuário postgres (ou crie outro usuário)
ALTER USER postgres WITH PASSWORD 'sua_senha_aqui';

-- Sair do psql
\q
```

### 3. Liberar conexões externas (para o cliente conseguir acessar)

Por padrão, o PostgreSQL só aceita conexões vindas do próprio servidor
(`localhost`). Para permitir acesso do cliente:

**Permitir escuta na rede:**

Edite o arquivo de configuração principal (ajuste o número da versão se
necessário, ex: 16, 15):

```bash
sudo nano /etc/postgresql/*/main/postgresql.conf
```

Procure por `listen_addresses` e altere para (retire o `#`):

```conf
listen_addresses = '*'
```

**Liberar a autenticação do cliente:**

Edite o arquivo de regras de acesso:

```bash
sudo nano /etc/postgresql/17/main/pg_hba.conf
```

Adicione ao final do arquivo a linha permitindo conexões com senha
(`scram-sha-256` ou `md5`):

```conf
host    all             all             0.0.0.0/0               scram-sha-256
```

**Reinicie o serviço:**

```bash
sudo systemctl restart postgresql
```

### 4. No código Java (cliente)

No arquivo de conexão do projeto
([`ConexaoDB.java`](src/main/java/trabalho/sd/rh/ConexaoDB.java)), aponte a URL
para o IP da máquina do servidor em vez de `localhost`:

```java
private static final String URL = "jdbc:postgresql://192.168.X.X:5432/funcionarios";
```

## Compilando o projeto

Na raiz do projeto:

```bash
mvn compile
```

Esse comando também gera, a partir do `.proto`, as classes Java do gRPC em
`target/generated-sources/protobuf/` (mensagens, stubs de cliente e a classe
base do serviço).

## Executando

São necessários dois terminais: um para o servidor e outro para o cliente.
Cliente e servidor precisam ser da **mesma implementação** (os dois TCP, ou os
dois gRPC).

### Opção 1 — TCP puro

**Servidor** (porta `5000`):

```bash
mvn exec:java -Dexec.mainClass="trabalho.sd.rh.tcp.ServidorTcp"
```

Valida a conexão com o banco, cria a tabela se necessário e sobe a porta. Se o
banco não estiver acessível, encerra com erro sem abrir a porta.

**Cliente** (conecta em `localhost:5000`):

```bash
mvn exec:java -Dexec.mainClass="trabalho.sd.rh.tcp.ClienteTcp"
```

### Opção 2 — gRPC

**Servidor** (porta `9090`):

```bash
mvn exec:java -Dexec.mainClass="trabalho.sd.rh.grpc.ServidorGrpc"
```

**Cliente** (conecta em `localhost:9090`):

```bash
mvn exec:java -Dexec.mainClass="trabalho.sd.rh.grpc.ClienteGrpc"
```

### Usando o cliente

Em ambas as versões, o cliente exibe um menu com as opções:

1. **Cadastrar funcionário** — solicita nome, cargo e salário.
2. **Listar funcionários** — exibe todos os funcionários cadastrados no servidor.
3. **Sair** — encerra a conexão.

## Acentuação no console (Windows)

No Windows, o cliente pode exibir `op├º├úo` no lugar de `opção`, e
`R$┬á54.545,00` no lugar de `R$ 54.545,00`. Não é defeito do programa: são bytes
UTF-8 sendo lidos como se pertencessem a outra tabela de caracteres.

Para a acentuação sair correta, **três camadas precisam concordar**:

| Camada                      | Papel                                             |
| --------------------------- | ------------------------------------------------- |
| `chcp`                      | em que codificação o console **lê**                |
| `[Console]::OutputEncoding` | como o PowerShell **repassa** a saída do programa  |
| `stdout.encoding` (JVM)     | em que codificação o Java **escreve**              |

Ajustar apenas o `chcp` não basta, porque as outras duas continuam divergentes.
No PowerShell, execute uma vez por terminal, **antes** de subir o cliente:

```powershell
chcp 65001; [Console]::OutputEncoding = [System.Text.Encoding]::UTF8; $env:MAVEN_OPTS = "-Dstdout.encoding=UTF-8"
```

Alternativas, caso não seja possível preparar o terminal:

- Executar pelo `cmd.exe`, que não repassa a saída do processo filho e por isso
  dispensa a segunda instrução.
- Executar pela IntelliJ, acrescentando `-Dstdout.encoding=UTF-8` nas VM options
  da configuração de execução.

**No servidor Linux nada disso é necessário**: o sistema já opera em UTF-8, e o
servidor imprime apenas a mensagem de inicialização.

Convém registrar que **os dados nunca são afetados** por esse ajuste. Strings em
protobuf trafegam sempre em UTF-8, por especificação — um nome como "João" sai
do PostgreSQL, atravessa o gRPC e chega ao cliente íntegro, qualquer que seja o
console. O que a preparação corrige é somente a forma como o cliente desenha o
texto na tela.

## Mensagem "Epoll available" do gRPC

Na primeira listagem, aparece uma linha semelhante a esta:

```text
INFO: Epoll available during static init of TcpMetrics:false
```

O gRPC usa o **Netty** como camada de transporte (dependência
`grpc-netty-shaded`, daí o nome de pacote `io.grpc.netty.shaded...`). O Netty
pode aproveitar o **epoll**, uma API de entrada e saída do kernel Linux, mais
eficiente sob carga; onde ela não existe, recorre ao NIO padrão do Java. A
classe `TcpMetrics` apenas registra qual dos dois encontrou:

| Onde     | Sistema | Valor   | Transporte em uso |
| -------- | ------- | ------- | ----------------- |
| Servidor | Linux   | `true`  | epoll nativo      |
| Cliente  | Windows | `false` | NIO do Java       |

É uma mensagem de nível `INFO`, não um erro: as duas pontas operam
normalmente, apenas com transportes diferentes.

Ela costuma surgir **no meio da tabela**, por dois motivos somados:

1. O log é escrito no `stderr`, enquanto a tabela é escrita no `stdout`. São
   canais independentes, e o terminal intercala os dois sem garantia de ordem.
2. A classe `TcpMetrics` só é carregada na **primeira chamada remota** — ou
   seja, no instante em que a opção `2` é acionada, logo após o cabeçalho da
   tabela ter sido impresso.

Por isso ela aparece uma única vez: numa segunda listagem, dentro da mesma
sessão, a classe já está carregada e nada é registrado.

## Estrutura do projeto

```text
src/main/proto/
└── funcionario.proto         # Contrato do serviço gRPC (mensagens + RPCs)

src/main/java/trabalho/sd/rh/
├── tcp/
│   ├── ServidorTcp.java       # Servidor TCP multi-thread
│   └── ClienteTcp.java        # Cliente TCP interativo
├── grpc/
│   ├── ServidorGrpc.java      # Servidor gRPC
│   ├── ClienteGrpc.java       # Cliente gRPC interativo (stub bloqueante)
│   └── FuncionarioServiceImpl.java  # Implementação do serviço (usa FuncionarioDAO)
├── ConexaoDB.java             # Fábrica de conexões JDBC + inicialização do schema
├── FuncionarioDAO.java        # Operações de persistência (INSERT / SELECT)
└── Funcionario.java           # Modelo de funcionário

src/main/resources/
└── db/schema.sql              # DDL da tabela funcionarios
```
