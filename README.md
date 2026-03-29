# TP5

![CI/CD Pipeline](https://github.com/Victor-Griggi/PB-TP3/actions/workflows/ci.yml/badge.svg)
![Java](https://img.shields.io/badge/Java-21-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-green)
![Coverage](https://img.shields.io/badge/coverage-%3E90%25-brightgreen)

## Visão Geral

Este TP finaliza o projeto com refatoração, automação completa de CI/CD e deploy multi-ambiente. Dá continuidade ao TP4, que integrou Produtos e Categorias com interface genérica e GitHub Actions.

O foco foi:
- Refatorar o código promovendo imutabilidade e polimorfismo via hierarquia de classes
- Migrar o build para Gradle com análise estática (SpotBugs) integrada
- Configurar pipeline CI/CD completo com SAST, DAST, deploy em três ambientes e aprovação manual para produção
- Adicionar logs personalizados e resumos em Markdown nos workflows

---

## Tecnologias

- **Java 21** + **Spring Boot 3.2.5**
- **Gradle 8.5** — build e gestão de dependências
- **Maven** — build alternativo (mantido para compatibilidade)
- **Thymeleaf** — renderização server-side
- **Spring Data JPA** + **H2** — persistência em memória
- **Bean Validation** — validação declarativa
- **Bootstrap 5** — interface responsiva via CDN
- **JUnit 5** + **Mockito** — testes unitários e de integração
- **Selenium WebDriver** — testes de interface (Chrome headless)
- **JaCoCo** — cobertura de testes (mínimo 90%)
- **SpotBugs** — análise estática de segurança (SAST)
- **GitHub Actions** — CI/CD com deploy multi-ambiente

---

## Arquitetura

```
src/main/java/com/crud/
├── CrudApplication.java
├── model/
│   ├── BaseEntity.java              # superclasse com campos comuns
│   ├── Product.java                 # extends BaseEntity
│   └── Category.java                # extends BaseEntity
├── repository/
│   ├── ProductRepository.java
│   └── CategoryRepository.java
├── service/
│   ├── QueryService.java            # interface de consulta (CQS)
│   ├── CommandService.java          # interface de comando (CQS)
│   ├── ProductService.java
│   └── CategoryService.java
├── controller/
│   ├── ProductController.java
│   ├── CategoryController.java
│   └── HomeController.java
└── exception/
    └── GlobalExceptionHandler.java
```

### Camadas

**Model → Repository → Service → Controller**

A separação é estrita: controllers nunca acessam repositórios diretamente, e services encapsulam toda a lógica de negócio.

### Hierarquia de Classes (BaseEntity)

`Product` e `Category` compartilham `id`, `name` e `description`. Esses campos foram extraídos para `BaseEntity` (`@MappedSuperclass`), eliminando duplicação e organizando a hierarquia conforme o princípio de superclasse/subclasse. Cada entidade herda os campos comuns e adiciona os seus próprios.

### Command-Query Separation

A interface genérica `CrudService<T, ID>` do TP4 foi substituída por duas interfaces:
- **`QueryService<T, ID>`** — `findAll()`, `findById()` (consultas puras, sem efeitos colaterais)
- **`CommandService<T, ID>`** — `save()`, `update()`, `delete()` (operações de escrita)

Essa separação aplica o princípio da imutabilidade: métodos de leitura ficam isolados dos que modificam estado.

---

## Pipeline CI/CD

O workflow `.github/workflows/ci.yml` roda em push, PR, release e dispatch manual.

```
build → sast → deploy-dev → dast → deploy-test → deploy-prod
                                                    ↑
                                              aprovação manual
```

### Jobs

| Job | O que faz |
|---|---|
| **Build & Test** | Compila com Gradle, roda testes, gera cobertura JaCoCo, sobe artefatos |
| **SAST** | Análise estática com SpotBugs (detecta bugs, vulnerabilidades, code smells) |
| **Deploy Dev** | Deploya artefato no ambiente dev |
| **DAST** | Testes dinâmicos de segurança (fuzz, SQL injection, XSS, stress) contra a aplicação rodando |
| **Deploy Test** | Deploya no ambiente test + validação pós-deploy com Selenium |
| **Deploy Prod** | Deploy em produção — requer aprovação manual, só roda em release ou dispatch |

### Gatilhos

- `push` na main → roda build, testes, SAST, deploy dev/test
- `pull_request` na main → roda build e SAST
- `release` → roda pipeline completo incluindo deploy prod
- `workflow_dispatch` → execução manual com deploy prod

### Logs e Resumos

Cada job produz:
- **Logs agrupados** (`::group::`) para facilitar navegação no GitHub Actions
- **Resumo em Markdown** (`$GITHUB_STEP_SUMMARY`) com status e métricas visíveis direto na interface
- **Artefatos** — relatórios JaCoCo, SpotBugs, resultados de testes e JAR da aplicação

### Ambientes e Proteções (Railway)

O deploy é feito no **Railway** via CLI nos workflows. O `RAILWAY_TOKEN` fica nos secrets do repositório.

| Ambiente | Quando deploya | Proteção |
|---|---|---|
| `dev` | Merge na main | Nenhuma (automático) |
| `test` | Após DAST passar | Nenhuma (automático) |
| `prod` | Release ou dispatch manual | Aprovação manual obrigatória |

### Segurança

- **OIDC** habilitado via `permissions: id-token: write` para integração segura com provedores de nuvem
- **Secrets:** `RAILWAY_TOKEN` armazenado nos secrets do GitHub, nunca exposto nos logs
- **Env vars** gerenciados via `env:` no workflow e variáveis de ambiente do Railway
- A aplicação lê a porta via `${PORT:8080}` — Railway injeta a porta automaticamente

---

## Como Executar

**Pré-requisitos:** Java 21+, Gradle 8.5+ (ou Maven 3.8+), Google Chrome (opcional, para Selenium).

```bash
# Iniciar a aplicação (Gradle)
gradle bootRun
# Ou com Maven
mvn spring-boot:run

# Acesse: http://localhost:8080/products

# Executar todos os testes
gradle test
# Ou com Maven
mvn clean test

# Gerar relatório de cobertura
gradle test jacocoTestReport
# Relatório em: build/reports/jacoco/test/html/index.html

# Análise estática (SAST)
gradle spotbugsMain
# Relatório em: build/reports/spotbugs/main.html
```

---

## Testes — 172 no total

| Categoria | Arquivo | O que cobre |
|---|---|---|
| Unitários | `ProductServiceTest` | Null guards, fail early, CRUD |
| Unitários | `CategoryServiceTest` | CRUD + impedimento de exclusão com produtos vinculados |
| Controller | `ProductControllerTest` | Endpoints HTTP, redirecionamentos, flash messages |
| Controller | `CategoryControllerTest` | Mesma cobertura para o módulo de categorias |
| Integração | `ProductCategoryIntegrationTest` | Vínculo entre os dois módulos via service e via HTTP |
| Selenium | `ProductSeleniumTest` | Fluxo completo no browser (Chrome headless) |
| Fuzz/DAST | `ProductFuzzTest` | SQL injection, XSS, unicode, valores extremos |
| Falhas | `FailureSimulationTest` | Banco indisponível, timeout, fail early/gracefully |
| Stress | `StressTest` | Volume, concorrência, listagem pesada |
| Modelo | `ProductTest`, `CategoryTest` | Bean Validation nas entidades |
| Exception | `GlobalExceptionHandlerTest` | Tratamento centralizado |
| Contexto | `CrudApplicationTest` | Inicialização do Spring Boot |

### Cobertura (JaCoCo)

O mínimo exigido é **90%**. O JaCoCo está configurado para reprovar o build se cair abaixo disso — tanto no Gradle quanto no Maven. O relatório é gerado automaticamente e enviado como artefato no pipeline de CI.

### Estratégias de Teste

**Testes unitários:** Mockito para isolar services dos repositórios. Cada método CRUD tem cobertura de cenários válidos, inválidos e edge cases (null, inexistente).

**Testes de controller:** MockMvc para testar endpoints HTTP sem subir o servidor. Validação de status codes, views, model attributes e flash messages.

**Testes de integração:** `@SpringBootTest` com banco H2 real. Verifica que a comunicação Product↔Category funciona end-to-end via service e via HTTP.

**Testes Selenium (pós-deploy):** Chrome headless com `@Order` para simular fluxo do usuário. Ignorados automaticamente se Chrome não está disponível (`assumeTrue`).

**Testes de segurança (DAST):** Fuzz testing com payloads de SQL injection, XSS e caracteres especiais. Stress testing com volume e concorrência. Executados contra a aplicação rodando no pipeline.

**Testes de resiliência:** Simulação de falhas de banco (DataAccessResourceFailureException, QueryTimeoutException). Verificação de que stack traces nunca chegam ao usuário.

---

## O que mudou do TP4 para o TP5

### Refatoração: BaseEntity

Extraí `id`, `name` e `description` para `BaseEntity` (`@MappedSuperclass`). Tanto `Product` quanto `Category` herdam dessa classe. Isso elimina os campos duplicados e organiza a hierarquia — campos comuns ficam na superclasse, campos específicos nas subclasses.

### Refatoração: Command-Query Separation

A interface `CrudService<T, ID>` foi substituída por `QueryService<T, ID>` (consultas) e `CommandService<T, ID>` (escritas). Isso aplica o princípio da imutabilidade separando operações de leitura das de modificação. Na prática, os services implementam as duas interfaces, mas o contrato é claro: consultas não têm efeitos colaterais.

### Refatoração: Condicionais simplificadas

Condicionais no `ProductController.save()` foram simplificadas com operador ternário. Multi-catch no `CategoryController.delete()` para exceções com tratamento idêntico. Guardas e early returns já existiam nos services — foram mantidos.

### Gradle

Adicionado `build.gradle` com os mesmos plugins e dependências do Maven. SpotBugs integrado para análise estática. JaCoCo com mínimo de 90%. O Maven (pom.xml) foi mantido para compatibilidade, mas o pipeline CI/CD agora usa Gradle.

### Pipeline CI/CD completo com Railway

O workflow básico do TP4 (build + test + upload) foi substituído por um pipeline completo:
- **6 jobs** sequenciais com dependências entre si
- **SAST** com SpotBugs
- **DAST** com testes de fuzz e stress contra a aplicação rodando
- **3 ambientes** (dev → test → prod) com deploy progressivo no Railway
- **Aprovação manual** obrigatória para produção
- **Logs personalizados** e resumos em Markdown em cada job
- **OIDC** habilitado para integração segura com cloud
- **Gatilhos** para push, PR, release e dispatch manual

### Cobertura mínima: 90%

Aumentada de 85% para 90% conforme requisito do TP5.

---

## Decisões e Premissas

**BaseEntity vs interfaces para campos comuns:** escolhi `@MappedSuperclass` porque é a forma natural do JPA para compartilhar campos entre entidades sem criar uma tabela separada. Interfaces não resolveriam o mapeamento.

**CQS sem CQRS completo:** separei as interfaces mas os services continuam lendo e escrevendo no mesmo repositório. CQRS com event sourcing seria over-engineering para esse contexto.

**Gradle + Maven:** mantive os dois porque o Maven já estava estável e não faz sentido quebrar builds anteriores. O CI usa Gradle, mas quem preferir Maven pode continuar usando.

**Railway para deploy:** escolhi Railway porque suporta Java nativamente, detecta o build system (Gradle/Maven) e injeta a porta via `PORT`. O free tier é suficiente para o projeto e o deploy é feito via CLI no workflow.

**H2 em memória:** mesma decisão dos TPs anteriores. Dados se perdem ao reiniciar, mas elimina dependência de banco externo.

**Idioma:** interface em PT-BR, código em inglês. Mesma convenção dos TPs anteriores.
