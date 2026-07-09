# ETL Importer

Aplicação responsável por processos de ETL (Extract, Transform, Load), desenvolvida em Java utilizando o ecossistema Spring.

## 🚀 Tecnologias

- **Java 21** — Versão do JDK utilizada no projeto
- **Spring Boot** — Framework principal da aplicação
- **Spring MVC** — Camada web / construção de endpoints REST
- **Spring Data JPA** — Persistência e acesso a dados
- **Jakarta EE** — Especificações utilizadas (imports `jakarta.*`)
- **Lombok** — Redução de boilerplate (getters, setters, construtores, etc.)
- **Maven** — Gerenciador de dependências e build (via Maven Wrapper `mvnw`)
- **Docker Compose** — Orquestração de serviços auxiliares (banco de dados, etc.) via `compose.yaml`

## 📁 Estrutura do Projeto
etl-importer ├── src │ ├── main │ │ ├── java/io/github/evertonsoethe/etlimporter │ │ │ └── EtlImporterApplication.java │ │ └── resources │ └── test ├── compose.yaml ├── pom.xml ├── mvnw / mvnw.cmd └── HELP.md## ▶️ Como Executar

### Pré-requisitos

- JDK 21+
- Docker e Docker Compose

### Executando a aplicação

bash docker compose up -d

```bash
./mvnw spring-boot:run
```

No Windows:
mvnw.cmd spring-boot:run

### Build do projeto
./mvnw clean package

🧪 Testes
./mvnw test