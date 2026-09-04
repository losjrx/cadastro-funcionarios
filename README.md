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
