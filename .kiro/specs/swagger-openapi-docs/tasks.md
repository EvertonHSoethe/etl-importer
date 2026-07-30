# Plano de Implementação: Swagger/OpenAPI Docs

## Visão Geral

Integrar a documentação OpenAPI 3.0 à aplicação ETL Importer via SpringDoc, adicionando a dependência, configuração global, anotações no controller e no modelo de domínio.

## Tarefas

- [x] 1. Adicionar dependência springdoc-openapi-starter-webmvc-ui ao pom.xml
  - Adicionar a dependência `org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.6` na seção `<dependencies>` do `pom.xml`
  - _Requisitos: 1.1, 1.2_

- [x] 2. Criar classe de configuração OpenApiConfig
  - Criar o arquivo `src/main/java/io/github/evertonsoethe/etlimporter/config/OpenApiConfig.java`
  - Definir um `@Bean` que retorna `OpenAPI` com `Info` contendo title="ETL Importer API", version="1.0.0" e description
  - _Requisitos: 5.1, 5.2, 5.3, 5.4_

- [x] 3. Adicionar propriedades do SpringDoc ao application.properties
  - Adicionar `springdoc.swagger-ui.path=/swagger-ui.html`
  - Adicionar `springdoc.api-docs.path=/v3/api-docs`
  - _Requisitos: 2.1, 5.5_

- [x] 4. Adicionar anotações de documentação ao ImportController
  - Adicionar `@Tag(name = "Importação", description = "...")` na classe
  - Adicionar `@Operation` com summary e description no método `importData()`
  - Adicionar `@ApiResponse` para códigos 202, 409 e 500 no método `importData()`
  - Adicionar `@Operation` com summary e description no método `getStatus()`
  - Adicionar `@ApiResponse` para códigos 200 e 404 no método `getStatus()`
  - Adicionar `@Content` e `@Schema` nos responses que retornam body
  - _Requisitos: 3.1, 3.2, 3.3, 3.4_

- [x] 5. Adicionar anotações @Schema ao modelo ImportExecution
  - Adicionar `@Schema(description = "...")` na classe `ImportExecution`
  - Adicionar `@Schema(description = "...", example = "...")` em cada campo da entidade
  - _Requisitos: 4.1, 4.2, 4.3_

- [x] 6. Checkpoint - Verificar integração completa
  - Garantir que a aplicação compila sem erros (`mvn compile`)
  - Garantir que todos os testes existentes continuam passando
  - Perguntar ao usuário se há dúvidas ou ajustes necessários

## Notas

- As tarefas são sequenciais: a dependência (1) deve ser adicionada antes das demais
- A configuração (2, 3) deve existir antes das anotações (4, 5) para validação contextual
- O checkpoint final (6) garante que nenhuma regressão foi introduzida
- Não há property-based tests aplicáveis — os requisitos são verificáveis via testes de integração com contexto Spring (smoke/example tests)
