# Documento de Requisitos

## Introdução

Este documento descreve os requisitos para a finalização da migração do `etl-importer` para Spring Batch. O sistema importa um arquivo CSV contendo até 100.000 registros de vendas de produtos de informática para um banco de dados PostgreSQL. A migração substitui a implementação manual assíncrona (`ImportService`) por um pipeline Spring Batch com rastreabilidade nativa, tolerância a falhas por linha e controle de execução via API REST.

O escopo inclui: correção das inconsistências de código existentes (tipos divergentes, referências a classes inexistentes, schema SQL vs. JPA), implementação completa do pipeline batch (Reader → Processor → Writer), integração do ciclo de vida do job com a entidade `ImportExecution`, tratamento de erros por linha com registro em `ImportError`, e exposição de endpoints REST para acionamento e consulta de status.

---

## Glossário

- **Importer**: O sistema Spring Boot `etl-importer` como um todo.
- **Job**: Uma execução Spring Batch do tipo `importSalesJob`, composta por um ou mais Steps.
- **Step**: A unidade de processamento `importSalesStep`, executada com chunk-oriented processing.
- **Chunk**: Conjunto de N itens lidos, processados e gravados em uma única transação. Tamanho padrão: 500.
- **Reader**: Bean `FlatFileItemReader<SaleCsvDto>` responsável por ler e mapear linhas do CSV.
- **Processor**: Bean `ItemProcessor<SaleCsvDto, Sale>` responsável por validar e transformar o DTO em entidade.
- **Writer**: Bean `ItemWriter<Sale>` responsável por persistir os registros de venda no banco.
- **ImportExecution**: Entidade JPA que registra metadados de cada execução de importação (status, contadores, duração).
- **ImportError**: Entidade JPA que registra o detalhe de cada linha do CSV que falhou durante o processamento.
- **SaleCsvDto**: DTO que representa uma linha mapeada do arquivo CSV antes da transformação.
- **SaleFieldSetMapper**: Componente que converte um `FieldSet` do Spring Batch para `SaleCsvDto`.
- **JobExecutionListener**: Listener que sincroniza o ciclo de vida do Job com a entidade `ImportExecution`.
- **SkipListener**: Listener que registra linhas com falha de processamento na tabela `import_error`.
- **JobLauncher**: Componente Spring Batch responsável por iniciar a execução de um Job.
- **JobParameters**: Parâmetros identificadores de uma execução única do Job (ex.: timestamp de início).
- **CSV**: Arquivo `sales_100000.csv` com 100.000 linhas de vendas, localizado em `src/main/resources/static/`.

---

## Requisitos

### Requisito 1: Correção da Consistência Interna do Código

**User Story:** Como desenvolvedor, quero que todos os componentes batch referenciem os mesmos tipos e beans, para que o projeto compile e execute sem erros de contexto Spring.

#### Critérios de Aceitação

1. THE Importer SHALL usar `SaleCsvDto` como o único tipo de transferência entre o Reader e o Processor, eliminando todas as referências à classe inexistente `SaleCsvRecord`.
2. THE `ImportSalesStepConfig` SHALL declarar `ItemReader<SaleCsvDto>`, `ItemProcessor<SaleCsvDto, Sale>` e `ItemWriter<Sale>` como dependências injetadas, consistentes com os beans definidos em `SaleCsvReader`, `SalesCsvProcessor` e `SaleWriter`.
3. THE `SaleFieldSetMapper` SHALL ser declarado no pacote `io.github.evertonsoethe.etlimporter.batch.mapper`, eliminando a declaração incorreta no pacote `reader`.
4. THE Importer SHALL compilar sem erros com `mvn compile` após a aplicação das correções.

---

### Requisito 2: Correção do Schema do Banco de Dados

**User Story:** Como desenvolvedor, quero que o schema SQL reflita exatamente as entidades JPA, para que a aplicação inicie sem erros de validação do Hibernate (`ddl-auto: validate`).

#### Critérios de Aceitação

1. THE `import_execution` table SHALL conter a coluna `filename VARCHAR(255) NOT NULL`, correspondente ao campo `filename` da entidade `ImportExecution`.
2. THE `import_execution` table SHALL remover a coluna `execution_id` que não possui correspondência na entidade JPA.
3. THE `import_execution` table SHALL conter a coluna `total_lines BIGINT NOT NULL`, correspondente ao campo `totalLines` da entidade (corrigindo `total_line` → `total_lines`).
4. THE `sale` table SHALL conter a coluna `sale_id BIGINT NOT NULL`, correspondente ao campo `saleId` da entidade `Sale`.
5. THE Importer SHALL iniciar sem erros de `SchemaManagementException` após a aplicação das migrations corretivas.
6. WHEN uma nova migration Flyway é criada, THE Importer SHALL aplicá-la automaticamente na inicialização sem requerer intervenção manual.

---

### Requisito 3: Configuração do Pipeline Spring Batch

**User Story:** Como desenvolvedor, quero um pipeline Spring Batch completo e funcional, para que a importação processe 100.000 registros via arquitetura chunk-oriented.

#### Critérios de Aceitação

1. THE `importSalesJob` SHALL ser composto exatamente pelo Step `importSalesStep`, executado de forma sequencial.
2. THE `importSalesStep` SHALL processar os itens em chunks de 500 registros por transação.
3. THE `importSalesStep` SHALL utilizar o Reader, Processor e Writer definidos nos Requisitos 4, 5 e 6.
4. WHEN o Step processa um chunk com sucesso, THE `importSalesStep` SHALL confirmar a transação e avançar para o próximo chunk.
5. THE `importSalesStep` SHALL configurar `skip-limit` de no mínimo 10.000, permitindo que linhas inválidas sejam puladas sem abortar o Job inteiro.
6. WHEN uma exceção é lançada no Processor para um item específico, THE `importSalesStep` SHALL pular o item e continuar o processamento dos itens restantes.
7. THE `application.properties` SHALL configurar `spring.batch.job.enabled=false` para que o Job não execute automaticamente na inicialização da aplicação.

---

### Requisito 4: Reader — Leitura do Arquivo CSV

**User Story:** Como desenvolvedor, quero um Reader que leia e mapeie corretamente cada linha do CSV, para que os dados brutos sejam convertidos em `SaleCsvDto` válido.

#### Critérios de Aceitação

1. THE Reader SHALL ler o arquivo `static/sales_100000.csv` do classpath da aplicação.
2. THE Reader SHALL ignorar a primeira linha do arquivo (cabeçalho).
3. THE Reader SHALL usar vírgula (`,`) como delimitador de campos.
4. THE Reader SHALL mapear os campos nas posições corretas: `sale_id`, `sale_date`, `customer_id`, `customer_name`, `product_id`, `product_name`, `category`, `quantity`, `unit_price`, `total_price`, `payment_method`, `status`.
5. THE `SaleFieldSetMapper` SHALL converter `sale_id` para `Long`, `sale_date` para `LocalDate` (formato `yyyy-MM-dd`), `customer_id` para `Long`, `quantity` para `Integer`, `unit_price` e `total_price` para `BigDecimal`.
6. WHEN o arquivo CSV não é encontrado no classpath, THE Reader SHALL lançar `ItemStreamException` e o Job deverá ser marcado como `FAILED`.

---

### Requisito 5: Processor — Validação e Transformação

**User Story:** Como desenvolvedor, quero um Processor que valide os dados do DTO e construa a entidade `Sale`, para que apenas registros válidos sejam persistidos.

#### Critérios de Aceitação

1. WHEN o `SaleCsvDto` possui `quantity` menor ou igual a zero, THE Processor SHALL lançar `IllegalArgumentException` com mensagem descritiva.
2. WHEN o `SaleCsvDto` possui `paymentMethod` com valor não presente no enum `PaymentMethodEnum`, THE Processor SHALL lançar `IllegalArgumentException` com mensagem descritiva.
3. WHEN o `SaleCsvDto` possui `status` com valor não presente no enum `SaleStatusEnum`, THE Processor SHALL lançar `IllegalArgumentException` com mensagem descritiva.
4. WHEN todos os campos do `SaleCsvDto` são válidos, THE Processor SHALL retornar uma instância de `Sale` com todos os campos mapeados corretamente.
5. THE Processor SHALL mapear `paymentMethod` e `status` usando `Enum.valueOf()`, garantindo que valores com diferença de case resultem em `IllegalArgumentException`.

---

### Requisito 6: Writer — Persistência em Lote

**User Story:** Como desenvolvedor, quero um Writer que persista os registros de venda em lote, para que a performance de inserção seja maximizada.

#### Critérios de Aceitação

1. THE Writer SHALL utilizar `RepositoryItemWriter<Sale>` com o método `save` do `SaleRepository`.
2. WHEN um chunk de `Sale` é entregue ao Writer, THE Writer SHALL persistir todos os registros do chunk em uma única operação de batch no banco de dados.
3. THE Writer SHALL operar dentro da transação gerenciada pelo Step, garantindo rollback automático em caso de falha no chunk.

---

### Requisito 7: Gerenciamento do Ciclo de Vida da Execução

**User Story:** Como desenvolvedor, quero que cada execução do Job crie e atualize um registro `ImportExecution`, para que o status e os contadores da importação sejam rastreáveis.

#### Critérios de Aceitação

1. WHEN o Job é iniciado, THE `JobExecutionListener` SHALL criar um registro `ImportExecution` com `status = RUNNING`, `filename = "sales_100000.csv"`, `totalLines = 0`, `processedLines = 0`, `successLines = 0` e `errorLines = 0`, e SHALL armazenar o `id` gerado nos `JobExecutionContext`.
2. WHEN o Job é concluído com `BatchStatus.COMPLETED`, THE `JobExecutionListener` SHALL atualizar o registro `ImportExecution` com `status = COMPLETED`, `successLines` igual ao total de itens escritos, `errorLines` igual ao total de itens pulados, `totalLines` igual à soma de ambos, `finishedDate` igual à data/hora atual e `duration` em milissegundos.
3. WHEN o Job é concluído com `BatchStatus.FAILED`, THE `JobExecutionListener` SHALL atualizar o registro `ImportExecution` com `status = FAILED`, `finishedDate` igual à data/hora atual e `duration` em milissegundos.
4. WHEN o Job é concluído com qualquer status, THE `JobExecutionListener` SHALL calcular `duration` como a diferença em milissegundos entre `afterJob` e o instante de início registrado em `beforeJob`.
5. THE `JobExecutionListener` SHALL armazenar o `id` do `ImportExecution` criado no `beforeJob` como `executionId` no `JobExecutionContext`, para que o `afterJob` possa recuperá-lo.

---

### Requisito 8: Registro de Erros por Linha

**User Story:** Como desenvolvedor, quero que cada linha inválida seja registrada em `ImportError`, para que erros possam ser investigados após a importação.

#### Critérios de Aceitação

1. WHEN o Processor lança uma exceção ao processar um item, THE `SkipListener` SHALL criar um registro `ImportError` contendo: `lineNumber` com o número da linha no CSV, `errorMessage` com a mensagem da exceção, `rawData` com o conteúdo bruto da linha e `execution` referenciando o `ImportExecution` corrente.
2. THE `SkipListener` SHALL recuperar o `ImportExecution` corrente usando o `executionId` armazenado no `JobExecutionContext`.
3. WHEN múltiplos itens falham no mesmo Step, THE `SkipListener` SHALL criar um registro `ImportError` separado para cada item com falha.
4. THE `SkipListener` SHALL persistir cada `ImportError` via `ImportErrorRepository`.

---

### Requisito 9: Acionamento do Job via API REST

**User Story:** Como desenvolvedor, quero acionar a importação via endpoint HTTP POST e consultar o status via GET, para que o processo seja operável sem acesso direto ao servidor.

#### Critérios de Aceitação

1. WHEN uma requisição `POST /import` é recebida, THE `ImportController` SHALL acionar o `importSalesJob` via `JobLauncher` com um `JobParameter` único baseado no timestamp atual.
2. WHEN o Job é acionado com sucesso, THE `ImportController` SHALL retornar `HTTP 202 Accepted` com o corpo contendo o registro `ImportExecution` criado (incluindo o `id` para rastreamento).
3. WHEN uma requisição `GET /import/{id}` é recebida, THE `ImportController` SHALL retornar `HTTP 200 OK` com o registro `ImportExecution` correspondente ao `id` fornecido.
4. WHEN o `id` fornecido em `GET /import/{id}` não corresponde a nenhum registro, THE `ImportController` SHALL retornar `HTTP 404 Not Found`.
5. WHEN o `JobLauncher` lança `JobExecutionAlreadyRunningException`, THE `ImportController` SHALL retornar `HTTP 409 Conflict` com mensagem indicando que um job já está em execução.

---

### Requisito 10: Configuração da Aplicação

**User Story:** Como desenvolvedor, quero que `application.properties` esteja corretamente formatado e com todas as configurações necessárias para o Spring Batch, para que a aplicação inicialize sem erros.

#### Critérios de Aceitação

1. THE `application.properties` SHALL usar exclusivamente o formato Java `.properties` (chave=valor), eliminando a mistura com sintaxe YAML que causa falha de parse.
2. THE `application.properties` SHALL configurar `spring.batch.job.enabled=false`.
3. THE `application.properties` SHALL configurar `spring.batch.jdbc.initialize-schema=always` para criação automática das tabelas de metadados do Spring Batch no banco de dados PostgreSQL.
4. THE `application.properties` SHALL configurar `spring.jpa.hibernate.ddl-auto=validate`.
5. THE `application.properties` SHALL configurar o pool de JDBC Batch do Hibernate: `spring.jpa.properties.hibernate.jdbc.batch_size=500`, `spring.jpa.properties.hibernate.order_inserts=true` e `spring.jpa.properties.hibernate.order_updates=true`.
