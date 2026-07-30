# Documento de Requisitos

## Introdução

Este documento define os requisitos para integração da documentação OpenAPI 3.0 (Swagger) à aplicação ETL Importer. O objetivo é disponibilizar documentação interativa e auto-gerada dos endpoints REST existentes, permitindo que desenvolvedores explorem e testem a API diretamente pelo navegador.

## Glossário

- **Sistema**: A aplicação ETL Importer (Spring Boot 4.1.0)
- **SpringDoc**: Biblioteca `springdoc-openapi-starter-webmvc-ui` que gera spec OpenAPI a partir de anotações
- **Swagger_UI**: Interface web interativa para visualização e teste de endpoints REST
- **Spec_OpenAPI**: Documento JSON gerado automaticamente no formato OpenAPI 3.0
- **ImportController**: Controller REST responsável pelos endpoints de importação de vendas
- **ImportExecution**: Entidade de domínio que representa uma execução de importação

## Requisitos

### Requisito 1: Dependência SpringDoc

**User Story:** Como desenvolvedor, quero que a dependência SpringDoc esteja adicionada ao projeto, para que a geração automática de documentação OpenAPI esteja disponível no classpath.

#### Critérios de Aceitação

1. THE Sistema SHALL incluir a dependência `springdoc-openapi-starter-webmvc-ui` no arquivo `pom.xml`
2. WHEN a aplicação é compilada, THE Sistema SHALL resolver a dependência SpringDoc sem erros de build

### Requisito 2: Acesso ao Swagger UI

**User Story:** Como desenvolvedor, quero acessar a interface Swagger UI pelo navegador, para que eu possa visualizar e testar os endpoints da API interativamente.

#### Critérios de Aceitação

1. WHEN um desenvolvedor acessa `/swagger-ui.html`, THE Swagger_UI SHALL renderizar a interface interativa de documentação
2. WHEN a Swagger_UI é carregada, THE Sistema SHALL exibir todos os endpoints documentados do ImportController
3. WHEN um desenvolvedor utiliza a funcionalidade "Try it out", THE Swagger_UI SHALL executar a requisição ao endpoint correspondente e exibir a resposta

### Requisito 3: Documentação dos Endpoints do ImportController

**User Story:** Como desenvolvedor, quero que os endpoints do ImportController estejam documentados com descrições claras e códigos de resposta, para que eu entenda o comportamento da API sem precisar ler o código-fonte.

#### Critérios de Aceitação

1. THE ImportController SHALL possuir a anotação `@Tag` com nome "Importação" e descrição dos endpoints
2. WHEN a Spec_OpenAPI é gerada, THE endpoint `POST /import` SHALL conter summary, description e os códigos de resposta 202, 409 e 500 documentados
3. WHEN a Spec_OpenAPI é gerada, THE endpoint `GET /import/{id}` SHALL conter summary, description e os códigos de resposta 200 e 404 documentados
4. WHEN a Spec_OpenAPI é gerada, THE Sistema SHALL incluir o schema de response `ImportExecution` nos códigos 202 e 200

### Requisito 4: Documentação do Modelo ImportExecution

**User Story:** Como desenvolvedor, quero que o modelo ImportExecution tenha descrições e exemplos nos campos, para que a documentação da API apresente informações claras sobre a estrutura de dados retornada.

#### Critérios de Aceitação

1. THE ImportExecution SHALL possuir a anotação `@Schema` de classe com descrição do modelo
2. WHEN a Spec_OpenAPI é gerada, THE ImportExecution SHALL apresentar cada campo com descrição textual via `@Schema`
3. WHEN a Spec_OpenAPI é gerada, THE ImportExecution SHALL apresentar valores de exemplo (`example`) nos campos: id, filename, status, totalLines, processedLines, successLines, errorLines e duration

### Requisito 5: Configuração Global da API

**User Story:** Como desenvolvedor, quero que a documentação da API tenha título, versão e descrição globais configurados, para que a identidade da API esteja clara na interface Swagger.

#### Critérios de Aceitação

1. THE Sistema SHALL possuir uma classe de configuração que define um bean `OpenAPI` com metadados globais
2. WHEN a Spec_OpenAPI é gerada, THE campo `info.title` SHALL conter o valor "ETL Importer API"
3. WHEN a Spec_OpenAPI é gerada, THE campo `info.version` SHALL conter o valor "1.0.0"
4. WHEN a Spec_OpenAPI é gerada, THE campo `info.description` SHALL conter uma descrição não vazia sobre a API
5. WHEN o endpoint `/v3/api-docs` é acessado, THE Sistema SHALL retornar um documento JSON válido no formato OpenAPI 3.0
