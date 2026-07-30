# Plano de Implementação: spring-batch-csv-importer

## Visão Geral

Migração completa do `etl-importer` para Spring Batch chunk-oriented, cobrindo: correção de infraestrutura e configuração, remoção de código morto, correções nos componentes batch existentes, implementação dos novos listeners, refatoração da camada REST e suíte completa de testes unitários, de propriedade e de integração.

Linguagem de implementação: **Java 21** com **Spring Boot 4.1** e **Spring Batch**.

---

## Tasks

- [x] 1. Fase 1 — Infraestrutura e Configuração

  - [x] 1.1 Corrigir `application.properties`
    - Substituir o conteúdo atual (mistura inválida de formato `.properties` + sintaxe YAML) por formato `.properties` puro (chave=valor)
    - Adicionar as seguintes propriedades:
      - `spring.batch.job.enabled=false`
      - `spring.batch.jdbc.initialize-schema=always`
      - `spring.jpa.hibernate.ddl-auto=validate`
      - `spring.jpa.properties.hibernate.jdbc.batch_size=500`
      - `spring.jpa.properties.hibernate.order_inserts=true`
      - `spring.jpa.properties.hibernate.order_updates=true`
    - Preservar as propriedades existentes de datasource e nome da aplicação
    - _Requisitos: 10.1, 10.2, 10.3, 10.4, 10.5_

  - [x] 1.2 Adicionar dependência `jqwik` ao `pom.xml`
    - Inserir o bloco `<dependency>` com `groupId=net.jqwik`, `artifactId=jqwik`, `version=1.9.3`, `scope=test`
    - Verificar que nenhuma outra dependência de testes foi alterada
    - _Requisitos: 10.1 (compilação limpa do projeto)_

  - [x] 1.3 Criar migration `V5__fix_schema.sql`
    - Criar arquivo em `src/main/resources/db/migration/V5__fix_schema.sql`
    - Incluir os quatro ALTER TABLE necessários para alinhar schema ao JPA:
      1. `ALTER TABLE import_execution ADD COLUMN IF NOT EXISTS filename VARCHAR(255) NOT NULL DEFAULT 'unknown'`
      2. `ALTER TABLE import_execution DROP COLUMN IF EXISTS execution_id`
      3. `ALTER TABLE import_execution RENAME COLUMN total_line TO total_lines`
      4. `ALTER TABLE sale ADD COLUMN IF NOT EXISTS sale_id BIGINT NOT NULL DEFAULT 0`
    - Atenção: `ALTER COLUMN started_date SET NOT NULL` deve ser aplicado somente se a coluna aceitar nulos no estado atual
    - _Requisitos: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6_

- [x] 2. Fase 2 — Remoção de Código Morto

  - [x] 2.1 Remover classes stub e implementação manual
    - Excluir os seguintes arquivos:
      - `src/main/java/.../batch/job/ImportSalesJob.java`
      - `src/main/java/.../batch/step/SaleCsvStep.java`
      - `src/main/java/.../service/ImportServiceBatch.java`
      - `src/main/java/.../service/ImportExecutionService.java`
      - `src/main/java/.../service/ImportService.java`
      - `src/main/java/.../config/AsyncConfig.java`
    - Garantir que nenhum outro bean ou classe faça referência a essas classes antes da exclusão
    - _Requisitos: 1.4 (projeto deve compilar sem erros após remoção)_

- [x] 3. Fase 3 — Correções nos Componentes Batch Existentes

  - [x] 3.1 Corrigir `SaleFieldSetMapper` — package declaration
    - Alterar a primeira linha de `package io.github.evertonsoethe.etlimporter.batch.reader` para `package io.github.evertonsoethe.etlimporter.batch.mapper`
    - Remover o import inexistente de `io.github.evertonsoethe.etlimporter.dto.SaleCsvRecord`
    - _Requisitos: 1.3, 1.4_

  - [x] 3.2 Corrigir `SaleCsvReader` — import do mapper
    - Substituir `import io.github.evertonsoethe.etlimporter.batch.reader.SaleFieldSetMapper` pelo import correto `import io.github.evertonsoethe.etlimporter.batch.mapper.SaleFieldSetMapper`
    - Atualizar o tipo do campo injetado de `io.github.evertonsoethe.etlimporter.batch.reader.SaleFieldSetMapper` para `SaleFieldSetMapper` (usando o import corrigido)
    - Remover o import inexistente de `io.github.evertonsoethe.etlimporter.dto.SaleCsvRecord`
    - _Requisitos: 1.1, 1.2, 1.4, 4.1, 4.2, 4.3, 4.4_

  - [x] 3.3 Corrigir `SaleProcessor` — remover import inválido e renomear arquivo
    - Remover `import io.github.evertonsoethe.etlimporter.dto.SaleCsvRecord` (classe inexistente)
    - Renomear o arquivo `SalesCsvProcessor.java` para `SaleProcessor.java` para alinhar ao nome da classe `SaleProcessor`
    - A lógica de validação (quantity, paymentMethod, status) já existe — não alterar a lógica funcional
    - _Requisitos: 1.1, 1.2, 1.4, 5.1, 5.2, 5.3, 5.4, 5.5_

  - [x] 3.4 Corrigir `ImportSalesStepConfig` — substituir tipos e adicionar fault tolerance
    - Substituir todas as referências a `SaleCsvRecord` por `SaleCsvDto` (campos `ItemReader`, `ItemProcessor` e seus generics)
    - Atualizar os imports: remover `io.github.evertonsoethe.etlimporter.dto.SaleCsvRecord`, adicionar `io.github.evertonsoethe.etlimporter.batch.dto.SaleCsvDto`
    - Adicionar injeção de `ImportErrorListener importErrorListener` via construtor (`@RequiredArgsConstructor`)
    - Alterar o step builder para incluir `.faultTolerant().skipLimit(10000).skip(Exception.class).listener(importErrorListener)`
    - _Requisitos: 1.1, 1.2, 3.5, 3.6_

- [x] 4. Checkpoint — Compilação limpa após correções
  - Garantir que `mvn compile` execute sem erros após as tasks 1–3.
  - Todos os imports inválidos devem ter sido eliminados; nenhuma referência a `SaleCsvRecord` deve restar.
  - _Requisitos: 1.4_

- [x] 5. Fase 4 — Novos Componentes

  - [x] 5.1 Implementar `ImportJobListener` com persistência completa
    - Editar `src/main/java/.../batch/listener/ImportJobListener.java` (arquivo já existe com apenas logs)
    - Injetar `ImportExecutionRepository` via construtor
    - Implementar `beforeJob`:
      - Criar `ImportExecution` com `status=RUNNING`, `filename="sales_100000.csv"`, `startedDate=LocalDateTime.now()` e todos os contadores em `0L`
      - Persistir via `importExecutionRepository.save()`
      - Armazenar o `id` gerado em `jobExecution.getExecutionContext().putLong("executionId", execution.getId())`
    - Implementar `afterJob`:
      - Recuperar `executionId` do contexto: `jobExecution.getExecutionContext().getLong("executionId")`
      - Carregar o `ImportExecution` do banco via `findById`
      - Calcular `writeCount` e `skipCount` somando os contadores de todos os `StepExecution`
      - Atualizar `successLines`, `errorLines`, `totalLines` (soma), `finishedDate` e `duration` (milissegundos)
      - Definir `status=COMPLETED` se `BatchStatus.COMPLETED`, caso contrário `status=FAILED`
      - Persistir o registro atualizado
    - _Requisitos: 7.1, 7.2, 7.3, 7.4, 7.5_

  - [x] 5.2 Criar `ImportErrorListener`
    - Criar arquivo `src/main/java/.../batch/listener/ImportErrorListener.java`
    - Implementar `SkipListener<SaleCsvDto, Sale>` com anotação `@Component`
    - Injetar `ImportExecutionRepository` e `ImportErrorRepository` via construtor
    - Injetar `StepExecution` via `@BeforeStep` (método anotado com `@BeforeStep` que recebe `StepExecution`)
    - Implementar apenas `onSkipInProcess(SaleCsvDto item, Throwable t)`:
      - Recuperar `executionId` via `stepExecution.getJobExecution().getExecutionContext().getLong("executionId")`
      - Carregar `ImportExecution` via `importExecutionRepository.findById(executionId)`
      - Criar `ImportError` com `execution`, `lineNumber` (via `stepExecution.getReadCount()`), `errorMessage` (`t.getMessage()`), `rawData` (`item.toString()`), `createdAt=LocalDateTime.now()`
      - Persistir via `importErrorRepository.save()`
    - _Requisitos: 8.1, 8.2, 8.3, 8.4_

- [x] 6. Fase 5 — Camada REST

  - [x] 6.1 Refatorar `ImportController`
    - Substituir injeção de `ImportService` por `JobLauncher`, `Job importSalesJob` e `ImportExecutionRepository`
    - Atualizar imports conforme necessário; remover import de `ImportService`
    - Reimplementar `POST /import`:
      - Construir `JobParameters` com `new JobParametersBuilder().addLong("timestamp", System.currentTimeMillis()).toJobParameters()`
      - Chamar `jobLauncher.run(importSalesJob, jobParameters)`
      - Recuperar o `ImportExecution` criado: extrair `executionId` do `JobExecutionContext` via `jobExecution.getExecutionContext().getLong("executionId")` e buscar no `ImportExecutionRepository`
      - Retornar `ResponseEntity.accepted().body(importExecution)`
      - Capturar `JobExecutionAlreadyRunningException` → `ResponseEntity.status(HttpStatus.CONFLICT).body(...)` com mensagem descritiva
    - Reimplementar `GET /import/{id}`:
      - `importExecutionRepository.findById(id)` → `200 OK` ou `404 Not Found`
    - _Requisitos: 9.1, 9.2, 9.3, 9.4, 9.5_

- [x] 7. Checkpoint — Contexto Spring sobe corretamente
  - Após as tasks 5 e 6, verificar que o contexto Spring inicializa sem erros de autowiring ou beans faltantes.
  - _Requisitos: 1.4, 3.1, 3.7_

- [x] 8. Fase 6 — Testes

  - [x] 8.1 Criar `SaleProcessorTest` (testes unitários)
    - Criar `src/test/java/.../batch/processor/SaleProcessorTest.java`
    - Casos de teste:
      1. Item válido com todos os campos corretos → retorna `Sale` com todos os campos mapeados
      2. `quantity = 0` → lança `IllegalArgumentException`
      3. `quantity = -1` → lança `IllegalArgumentException`
      4. `paymentMethod = "INVALID"` → lança `IllegalArgumentException`
      5. `status = "invalid_status"` → lança `IllegalArgumentException`
      6. Edge case: `paymentMethod = "cash"` (lowercase) → lança `IllegalArgumentException` (case-sensitive)
      7. Edge case: `status = "completed"` (lowercase) → lança `IllegalArgumentException` (case-sensitive)
    - _Requisitos: 5.1, 5.2, 5.3, 5.4, 5.5_

  - [ ]* 8.2 Criar `SaleFieldSetMapperTest` (testes unitários)
    - Criar `src/test/java/.../batch/mapper/SaleFieldSetMapperTest.java`
    - Instanciar `SaleFieldSetMapper` diretamente (sem Spring context)
    - Usar `DefaultFieldSet` do Spring Batch para montar o `FieldSet` nos testes
    - Casos de teste:
      1. Linha CSV válida com todos os campos → `SaleCsvDto` com tipos corretos (`Long`, `LocalDate`, `Integer`, `BigDecimal`)
      2. Edge case: `sale_date` em formato inválido (ex: `"31/12/2023"`) → lança exceção de parse (`DateTimeParseException`)
    - _Requisitos: 4.4, 4.5_

  - [ ]* 8.3 Criar `SaleProcessorPropertyTest` (testes de propriedade com jqwik)
    - Criar `src/test/java/.../batch/processor/SaleProcessorPropertyTest.java`
    - Cada método deve conter o comentário: `// Feature: spring-batch-csv-importer, Property N: <texto>`
    - Implementar os três testes de propriedade:

      ```java
      // Feature: spring-batch-csv-importer, Property 2: Rejeição de quantity inválida pelo Processor
      @Property(tries = 100)
      void quantityInvalidaDeveLancarExcecao(@ForAll @IntRange(max = 0) int quantity)
      ```
      → Montar `SaleCsvDto` válido com `quantity` fornecido; chamar `processor.process(dto)`; assertar `IllegalArgumentException`

      ```java
      // Feature: spring-batch-csv-importer, Property 3: Rejeição de enums inválidos pelo Processor
      @Property(tries = 100)
      void paymentMethodInvalidoDeveLancarExcecao(@ForAll @StringLength(min = 1) @AlphaChars String paymentMethod)
      ```
      → Filtrar apenas valores que não são membros válidos de `PaymentMethodEnum`; assertar `IllegalArgumentException`

      ```java
      // Feature: spring-batch-csv-importer, Property 4: Mapeamento completo DTO → Sale pelo Processor
      @Property(tries = 100)
      void dtoValidoDeveMapearTodosOsCampos(@ForAll("validSaleCsvDtos") SaleCsvDto dto)
      ```
      → Fornecer `@Provide("validSaleCsvDtos")` com `Arbitraries` que geram DTOs válidos; assertar que todos os campos do `Sale` correspondem ao DTO
    - _Requisitos: 5.1, 5.2, 5.3, 5.4, 5.5_

  - [ ]* 8.4 Criar `SaleFieldSetMapperPropertyTest` (teste de propriedade com jqwik)
    - Criar `src/test/java/.../batch/mapper/SaleFieldSetMapperPropertyTest.java`
    - Implementar:

      ```java
      // Feature: spring-batch-csv-importer, Property 1: Round-trip de mapeamento CSV → DTO
      @Property(tries = 100)
      void roundTripCsvParaDto(@ForAll("validDtos") SaleCsvDto dto)
      ```
      → Fornecer `@Provide("validDtos")` com arbitrários para `SaleCsvDto` válido; montar um `DefaultFieldSet` com os valores do DTO; chamar `mapper.mapFieldSet(fieldSet)`; assertar que todos os campos do resultado são equivalentes ao DTO original
    - _Requisitos: 4.4, 4.5_

  - [x] 8.5 Criar `SchemaValidationIT` (integração com Testcontainers)
    - Criar `src/test/java/.../integration/SchemaValidationIT.java`
    - Usar `@SpringBootTest`, `@Testcontainers` e `@Container PostgreSQLContainer`
    - Configurar datasource dinâmico via `@DynamicPropertySource`
    - O único assertion necessário é que o contexto Spring suba sem `SchemaManagementException`
    - Cobre validação de que todas as migrations Flyway (V1–V5) são aplicadas corretamente e o Hibernate valida o schema sem erros
    - _Requisitos: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6_

  - [x] 8.6 Criar `ImportJobIT` (integração completa do Job com Testcontainers)
    - Criar `src/test/java/.../integration/ImportJobIT.java`
    - Usar `@SpringBootTest`, `@Testcontainers` e `@Container PostgreSQLContainer`
    - Criar um CSV de teste em `src/test/resources/static/sales_100000.csv` (ou configurar o reader para apontar para um arquivo de teste menor) com N linhas válidas e M linhas inválidas (ex: 5 válidas, 2 inválidas por quantity <= 0 ou enum inválido)
    - Injetar `JobLauncher`, `Job importSalesJob`, `ImportExecutionRepository`, `SaleRepository`, `ImportErrorRepository`
    - Executar `jobLauncher.run(importSalesJob, params)` e aguardar conclusão
    - Assertions:
      - `importExecution.getStatus() == ExecutionStatusEnum.COMPLETED`
      - `importExecution.getSuccessLines() == N`
      - `importExecution.getErrorLines() == M`
      - `importExecution.getTotalLines() == N + M`
      - `saleRepository.count() == N`
      - `importErrorRepository.count() == M`
    - Cada assertion deve ter comentário referenciando a propriedade:

      ```java
      // Property 5: Persistência completa de chunks pelo Writer
      // Property 6: Corretude dos contadores da execução
      // Property 7: Completude e corretude dos registros de erro por linha
      ```
    - _Requisitos: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 6.2, 7.2, 7.3, 8.1, 8.3_

  - [ ]* 8.7 Criar `ImportControllerIT` (integração da API REST com MockMvc)
    - Criar `src/test/java/.../integration/ImportControllerIT.java`
    - Usar `@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)`, `@Testcontainers` e `@Container PostgreSQLContainer`
    - Injetar `TestRestTemplate` ou usar `MockMvc` com `@AutoConfigureMockMvc`
    - Casos de teste:
      1. `POST /import` → `202 Accepted` com corpo contendo `ImportExecution` com `id` não nulo
      2. `POST /import` enquanto job já está em execução → `409 Conflict`
      3. `GET /import/{id}` com id válido → `200 OK` com `ImportExecution` correto
      4. `GET /import/999999` → `404 Not Found`
    - _Requisitos: 9.1, 9.2, 9.3, 9.4, 9.5_

- [x] 9. Checkpoint final — Suíte completa de testes
  - Executar `mvn test` e garantir que todos os testes obrigatórios (não marcados com `*`) passam.
  - Garantir que testes de integração sobem e derrubam os containers corretamente.
  - Garantir que `mvn compile` retorna exit code 0 sem warnings de imports não utilizados.

---

## Notes

- Tasks marcadas com `*` são opcionais e podem ser puladas para uma entrega MVP mais rápida.
- A ordem das tasks é importante: a Fase 1 (infraestrutura) deve preceder a Fase 2 (remoção), que deve preceder a Fase 3 (correções de código), para evitar que o compilador referencie classes inexistentes durante as correções.
- Os checkpoints nas tasks 4 e 7 são pontos de validação explícitos — recomenda-se `mvn compile` (task 4) e inicialização manual da aplicação (task 7) antes de avançar para os testes.
- Cada teste de integração requer que o Docker esteja rodando na máquina de desenvolvimento para que o Testcontainers provisione o PostgreSQL.
- O arquivo de teste CSV para `ImportJobIT` deve ser criado com volume reduzido (< 20 linhas) para garantir tempo de execução razoável nos testes.
- Property tests com jqwik requerem a dependência adicionada na task 1.2.

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2", "1.3"] },
    { "id": 1, "tasks": ["2.1"] },
    { "id": 2, "tasks": ["3.1"] },
    { "id": 3, "tasks": ["3.2", "3.3"] },
    { "id": 4, "tasks": ["3.4"] },
    { "id": 5, "tasks": ["5.1", "5.2"] },
    { "id": 6, "tasks": ["6.1"] },
    { "id": 7, "tasks": ["8.1", "8.2", "8.3", "8.4", "8.5"] },
    { "id": 8, "tasks": ["8.6", "8.7"] }
  ]
}
```
