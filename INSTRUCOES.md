# Instruções

## Pré-requisitos

- Java 21+
- Gradle 8.5+ (ou Maven 3.8+)
- Google Chrome (só para os testes Selenium — se não tiver, eles são ignorados e o build não quebra)

---

## Executando a Aplicação

```bash
# Com Gradle
gradle bootRun

# Com Maven
mvn spring-boot:run
```

Acesse [http://localhost:8080/products](http://localhost:8080/products). A barra de navegação no topo permite alternar entre Produtos e Categorias.

---

## Testes e Cobertura

```bash
# Rodar todos os testes (Gradle)
gradle test

# Rodar todos os testes (Maven)
mvn clean test

# Gerar relatório de cobertura (Gradle)
gradle test jacocoTestReport
# Relatório em: build/reports/jacoco/test/html/index.html

# Gerar relatório de cobertura (Maven)
mvn clean test jacoco:report
# Relatório em: target/site/jacoco/index.html
```

São 172 testes no total. O JaCoCo está configurado para reprovar o build automaticamente se a cobertura de linhas cair abaixo de 90%, então o `gradle test` ou `mvn clean test` já valida isso.

---

## Análise Estática (SAST)

```bash
gradle spotbugsMain
# Relatório em: build/reports/spotbugs/main.html
```

O SpotBugs detecta bugs, vulnerabilidades e code smells no código compilado. Está configurado com `ignoreFailures = true` para não bloquear o build, mas os achados ficam no relatório.

---

## GitHub Actions (CI/CD)

O workflow de CI/CD (`.github/workflows/ci.yml`) roda automaticamente em push, PR na main, release e dispatch manual.

### Pipeline

```
Build & Test → SAST → Deploy Dev → DAST → Deploy Test → Deploy Prod
```

### O que cada job faz

| Job | Descrição |
|---|---|
| Build & Test | Compila com Gradle, roda 172 testes, gera cobertura, sobe artefatos |
| SAST | Análise estática com SpotBugs |
| Deploy Dev | Deploy automático no ambiente dev |
| DAST | Testes de segurança dinâmicos (fuzz + stress) contra app rodando |
| Deploy Test | Deploy no ambiente test + validação pós-deploy com Selenium |
| Deploy Prod | Deploy em produção — requer aprovação manual |

### Acompanhando

- Aba **Actions** no repositório do GitHub
- Cada job gera um **resumo em Markdown** visível diretamente na interface
- Relatórios de cobertura, SAST e testes ficam nos **artefatos** de cada execução
- **Badges** no README mostram status do pipeline

### Deploy (Railway)

O deploy é feito no [Railway](https://railway.app) via CLI nos workflows.

**Setup inicial:**
1. Crie um projeto no Railway e conecte ao repositório
2. Gere um token em Railway → Settings → Tokens
3. Adicione o token como `RAILWAY_TOKEN` nos secrets do GitHub (Settings → Secrets → Actions)

**Ambientes:**

| Ambiente | Proteção |
|---|---|
| dev | Automático após SAST |
| test | Automático após DAST |
| prod | Aprovação manual obrigatória |

Para configurar aprovação manual: Settings → Environments → prod → Required reviewers.

Railway detecta o Java automaticamente, faz build com Gradle e sobe a aplicação na porta que ele injeta via `PORT`.

---

## Resumo dos Testes

| Tipo | Arquivo | O que testa |
|---|---|---|
| Unitários | `ProductServiceTest` | CRUD do service, null guards, fail early |
| Unitários | `CategoryServiceTest` | CRUD de categorias, bloqueio de exclusão com produtos |
| Controller | `ProductControllerTest` | Endpoints HTTP, validação, flash messages |
| Controller | `CategoryControllerTest` | Mesma cobertura para categorias |
| Integração | `ProductCategoryIntegrationTest` | Comunicação entre os dois módulos (service e HTTP) |
| Selenium | `ProductSeleniumTest` | Fluxo completo no Chrome headless (pós-deploy) |
| Fuzz/DAST | `ProductFuzzTest` | SQL injection, XSS, unicode, valores extremos |
| Falhas | `FailureSimulationTest` | Banco indisponível, timeout |
| Stress | `StressTest` | Volume, concorrência |
| Modelo | `ProductTest`, `CategoryTest` | Bean Validation |
| Exception | `GlobalExceptionHandlerTest` | Tratamento centralizado de exceções |
| Contexto | `CrudApplicationTest` | Inicialização do Spring Boot |

---

## Principais Mudanças (TP4 → TP5)

**`BaseEntity` (`@MappedSuperclass`)** — campos comuns (`id`, `name`, `description`) extraídos para superclasse. Product e Category herdam dela. Elimina duplicação e organiza hierarquia.

**Command-Query Separation** — `CrudService<T, ID>` substituída por `QueryService<T, ID>` (consultas) + `CommandService<T, ID>` (escritas). Separa operações de leitura das de modificação.

**Condicionais simplificadas** — ternário no ProductController, multi-catch no CategoryController. Menos código, mesma clareza.

**Build Gradle** — `build.gradle` com SpotBugs e JaCoCo integrados. Maven mantido para compatibilidade.

**Pipeline CI/CD completo** — 6 jobs, 3 ambientes com deploy no Railway, SAST/DAST, aprovação manual para prod, logs personalizados, resumos em Markdown, badges.

**Cobertura 90%** — aumentada de 85% do TP4.

---

## Observações

- O H2 é em memória — dados se perdem ao reiniciar. É proposital; elimina dependência de banco externo.
- Os testes Selenium precisam do Chrome, mas se ele não estiver instalado, os testes são ignorados via `assumeTrue` e o build segue normalmente.
- A categoria no produto é opcional. Dá para cadastrar produtos sem categoria.
- O Gradle e o Maven coexistem. O CI usa Gradle, mas o Maven continua funcional.
