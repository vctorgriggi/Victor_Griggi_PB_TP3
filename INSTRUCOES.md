# Instruções

## Pré-requisitos

Precisa de Java 21+ e Gradle 8.5+ (ou Maven 3.8+). Google Chrome só é necessário pros testes Selenium — se não tiver, eles são ignorados automaticamente e o build segue sem problemas.

---

## Rodando a aplicação

```bash
./gradlew bootRun
# ou: mvn spring-boot:run
```

Depois é só acessar [http://localhost:8080/products](http://localhost:8080/products). A nav bar no topo alterna entre Produtos e Categorias.

---

## Testes e cobertura

```bash
# rodar todos os testes
./gradlew test
# ou: mvn clean test

# gerar relatório de cobertura
./gradlew test jacocoTestReport
# relatório fica em: build/reports/jacoco/test/html/index.html

# com maven:
# mvn clean test jacoco:report → target/site/jacoco/index.html
```

São 172 testes no total. O JaCoCo tá configurado pra reprovar o build se a cobertura cair abaixo de 90%, então rodar os testes já valida isso automaticamente.

---

## Análise estática (SAST)

```bash
./gradlew spotbugsMain
# relatório em: build/reports/spotbugs/main.html
```

O SpotBugs roda no código compilado e detecta bugs, vulnerabilidades e code smells. Tá com `ignoreFailures = true` pra não travar o build, mas os achados ficam todos no relatório.

---

## CI/CD

O workflow (`.github/workflows/ci.yml`) roda em push na main, PR, release e dispatch manual. O pipeline tem 6 jobs:

```
Build & Test → SAST → Deploy Dev → DAST → Deploy Test → Deploy Prod
```

Primeiro compila e roda os testes. Depois faz análise estática com SpotBugs. Se tudo passa, deploya no ambiente dev e roda os testes de segurança dinâmicos (fuzz e stress) contra a aplicação rodando. Depois deploya no ambiente test com validação Selenium pós-deploy. O deploy em prod só acontece em releases ou dispatch manual, e precisa de aprovação.

Pra acompanhar: aba **Actions** no GitHub. Cada job gera um resumo em Markdown direto na interface, e os relatórios de cobertura, SAST e testes ficam nos artefatos.

---

## Deploy (Railway)

O deploy é feito no [Railway](https://railway.app) via CLI nos workflows. Pra configurar:

1. Cria um projeto no Railway e conecta ao repositório
2. Gera um project token (Settings do projeto → Tokens)
3. Adiciona como `RAILWAY_TOKEN` nos secrets do GitHub (Settings → Secrets → Actions)
4. Cria os environments `dev`, `test` e `prod` no GitHub (Settings → Environments)
5. No environment `prod`, adiciona Required reviewers pra aprovação manual

O Railway detecta o Java sozinho, faz build com Gradle e sobe a aplicação na porta que ele injeta via `PORT`.

| Ambiente | Proteção |
|---|---|
| dev | Automático |
| test | Automático |
| prod | Aprovação manual |

---

## Resumo dos testes

| Tipo | Arquivo | O que testa |
|---|---|---|
| Unitário | `ProductServiceTest` | CRUD, null guards, fail early |
| Unitário | `CategoryServiceTest` | CRUD, bloqueio de exclusão com produtos |
| Controller | `ProductControllerTest` | Endpoints HTTP, validação, flash messages |
| Controller | `CategoryControllerTest` | Mesma cobertura pra categorias |
| Integração | `ProductCategoryIntegrationTest` | Comunicação entre os dois módulos (service e HTTP) |
| Selenium | `ProductSeleniumTest` | Fluxo completo no Chrome headless |
| Fuzz | `ProductFuzzTest` | SQL injection, XSS, unicode, valores extremos |
| Falhas | `FailureSimulationTest` | Banco indisponível, timeout |
| Stress | `StressTest` | Volume, concorrência |
| Modelo | `ProductTest`, `CategoryTest` | Bean Validation |
| Exception | `GlobalExceptionHandlerTest` | Tratamento centralizado |
| Contexto | `CrudApplicationTest` | Inicialização do Spring Boot |

---

## O que mudou (TP4 → TP5)

Extraí os campos comuns (`id`, `name`, `description`) pra uma superclasse `BaseEntity` usando `@MappedSuperclass`. Product e Category herdam dela agora — sem mais duplicação.

A `CrudService<T, ID>` foi substituída por `QueryService` (consultas) + `CommandService` (escritas), separando leitura de modificação.

Nos controllers, simplifiquei condicionais: ternário no ProductController, multi-catch no CategoryController.

Adicionei `build.gradle` com SpotBugs e JaCoCo. O pipeline de CI que antes era um job passou pra 6, com SAST, DAST, deploy em 3 ambientes no Railway e aprovação manual pra prod. Cobertura mínima subiu de 85% pra 90%.

---

## Observações

O H2 é em memória — dados somem ao reiniciar. É de propósito, pra não depender de banco externo. Os testes Selenium precisam do Chrome mas se ignoram sozinhos se ele não tiver. A categoria no produto é opcional. O Gradle e o Maven coexistem — CI usa Gradle, mas Maven continua funcionando.
