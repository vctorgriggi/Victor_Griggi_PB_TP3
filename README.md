# TP5

![CI/CD Pipeline](https://github.com/vctorgriggi/Victor_Griggi_PB_TP3/actions/workflows/ci.yml/badge.svg)
![Java](https://img.shields.io/badge/Java-21-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-green)
![Coverage](https://img.shields.io/badge/coverage-%3E90%25-brightgreen)

## Visão Geral

Esse TP fecha o projeto. A ideia foi pegar tudo que já existia nos TPs anteriores — o CRUD de produtos, a integração com categorias, a esteira de CI — e levar pra um nível de entrega real: código refatorado, pipeline completo com análise de segurança e deploy automatizado em múltiplos ambientes.

Do lado de código, a refatoração focou em três frentes: extrair uma superclasse pra eliminar campos duplicados entre Product e Category, separar as interfaces de leitura e escrita (command-query separation) e simplificar condicionais nos controllers. Do lado de infra, migrei o build pra Gradle, configurei SpotBugs pra análise estática, e montei um pipeline de CI/CD com 6 jobs que vai do build até o deploy em produção no Railway — passando por SAST, DAST e validação pós-deploy com Selenium.

A cobertura de testes subiu pra 90% (era 85% no TP4). Os 172 testes continuam passando.

---

## Tecnologias

O stack é o mesmo dos TPs anteriores, com algumas adições:

- Java 21 + Spring Boot 3.2.5
- Thymeleaf pra renderização server-side e Bootstrap 5 pra UI
- Spring Data JPA + H2 (banco em memória)
- Bean Validation pra validação declarativa
- JUnit 5 + Mockito pra testes, Selenium pra E2E
- JaCoCo pra cobertura (mínimo 90%, enforced no build)
- **Novo:** Gradle 8.5 como build principal (Maven mantido pra compatibilidade)
- **Novo:** SpotBugs pra análise estática de segurança (SAST)
- **Novo:** GitHub Actions com pipeline completo + deploy no Railway

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

A organização segue o padrão Model → Repository → Service → Controller. Controllers nunca falam direto com o banco, tudo passa pelo service.

No TP4 o `Product` e o `Category` tinham `id`, `name` e `description` repetidos. Agora esses campos vivem no `BaseEntity` (`@MappedSuperclass`) e as duas entidades herdam dele. Cada uma adiciona só o que é específico — preço e quantidade no Product, nada a mais no Category.

A outra mudança grande foi a separação de interfaces. Antes existia uma `CrudService<T, ID>` que misturava consultas e escritas. Agora são duas: `QueryService` (findAll, findById) e `CommandService` (save, update, delete). Os services implementam as duas, mas o contrato fica mais claro — leitura separada de modificação.

---

## Pipeline CI/CD

O workflow roda em push na main, pull request, release e dispatch manual. São 6 jobs encadeados:

```
build → sast → deploy-dev → dast → deploy-test → deploy-prod
                                                    ↑
                                              aprovação manual
```

O **Build & Test** compila com Gradle, roda os 172 testes e gera o relatório de cobertura. Se tudo passa, o **SAST** roda o SpotBugs pra análise estática. Depois vem o **Deploy Dev** que sobe a aplicação no Railway. Com o app rodando, o **DAST** executa os testes de fuzz (SQL injection, XSS, caracteres especiais) e stress (volume, concorrência) contra a aplicação real. Se o DAST passa, o **Deploy Test** sobe no ambiente de teste e roda os testes Selenium como validação pós-deploy. O **Deploy Prod** só roda em releases ou dispatch manual, e exige aprovação.

Cada job gera logs agrupados (`::group::`) pra facilitar a navegação no GitHub Actions e um resumo em Markdown (`$GITHUB_STEP_SUMMARY`) que aparece direto na interface. Os relatórios de cobertura, SpotBugs e testes ficam nos artefatos de cada execução.

### Deploy no Railway

O deploy é feito via Railway CLI nos workflows. O token de autenticação (`RAILWAY_TOKEN`) fica nos secrets do repositório — nunca aparece nos logs. A aplicação lê a porta via `${PORT:8080}`, que o Railway injeta automaticamente.

| Ambiente | Quando roda | Proteção |
|---|---|---|
| dev | Merge na main | Automático |
| test | Após DAST passar | Automático |
| prod | Release ou dispatch | Aprovação manual |

O OIDC tá habilitado no workflow (`permissions: id-token: write`) pra caso seja necessário integrar com outro provedor de nuvem no futuro.

---

## Como Executar

Precisa de Java 21+ e Gradle 8.5+ (ou Maven 3.8+). Chrome é opcional — sem ele os testes Selenium são ignorados e o build segue normal.

```bash
# iniciar a aplicação
./gradlew bootRun
# ou: mvn spring-boot:run

# acessar: http://localhost:8080/products

# rodar todos os testes
./gradlew test
# ou: mvn clean test

# gerar relatório de cobertura
./gradlew test jacocoTestReport
# relatório em: build/reports/jacoco/test/html/index.html

# análise estática
./gradlew spotbugsMain
# relatório em: build/reports/spotbugs/main.html
```

---

## Testes

São 172 testes distribuídos em várias categorias:

| Tipo | Arquivo | O que cobre |
|---|---|---|
| Unitário | `ProductServiceTest` | CRUD, null guards, fail early |
| Unitário | `CategoryServiceTest` | CRUD, bloqueio de exclusão com produtos vinculados |
| Controller | `ProductControllerTest` | Endpoints HTTP, validação, flash messages |
| Controller | `CategoryControllerTest` | Mesma cobertura pra categorias |
| Integração | `ProductCategoryIntegrationTest` | Vínculo entre os dois módulos (service e HTTP) |
| Selenium | `ProductSeleniumTest` | Fluxo completo no Chrome headless |
| Fuzz | `ProductFuzzTest` | SQL injection, XSS, unicode, valores extremos |
| Falhas | `FailureSimulationTest` | Banco indisponível, timeout, fail early/gracefully |
| Stress | `StressTest` | Volume, concorrência, listagem pesada |
| Modelo | `ProductTest`, `CategoryTest` | Bean Validation |
| Exception | `GlobalExceptionHandlerTest` | Tratamento centralizado |
| Contexto | `CrudApplicationTest` | Inicialização do Spring Boot |

A cobertura mínima é 90%, enforced pelo JaCoCo — se cair abaixo disso, o build quebra. O relatório sobe como artefato no CI.

Os testes unitários usam Mockito pra isolar os services dos repositórios. Os de controller usam MockMvc pra testar endpoints sem subir o servidor. Os de integração usam `@SpringBootTest` com o H2 real pra verificar que Product e Category se comunicam corretamente. Os de Selenium simulam o fluxo do usuário no Chrome headless — se o Chrome não tiver instalado, o `assumeTrue` pula eles sem quebrar o build. Os de fuzz jogam payloads maliciosos (SQL injection, XSS, null bytes) na aplicação pra garantir que ela não crashe. Os de stress criam 50-100 produtos em sequência e testam concorrência com 10 threads simultâneas.

---

## O que mudou do TP4 pro TP5

A principal mudança de código foi a extração do `BaseEntity`. No TP4, `Product` e `Category` tinham os mesmos campos (`id`, `name`, `description`) copiados. Agora esses campos vivem na superclasse e as entidades herdam. Parece simples, mas elimina duplicação e deixa a hierarquia limpa — se amanhã aparecer uma terceira entidade com nome e descrição, é só estender `BaseEntity`.

A interface `CrudService<T, ID>` do TP4 foi substituída por duas: `QueryService` pra consultas e `CommandService` pra escritas. A motivação é command-query separation — métodos que leem dados ficam separados dos que modificam. Os services implementam as duas, então pra quem usa nada muda, mas o contrato fica mais explícito.

Nos controllers, simplifiquei algumas condicionais. O `ProductController.save()` tinha um if/else pra setar categoria que virou um ternário. O `CategoryController.delete()` tinha dois catches idênticos que virou um multi-catch. Pouca coisa, mas deixa o código mais direto.

Do lado de infra, a mudança foi grande. Adicionei o `build.gradle` com SpotBugs e JaCoCo integrados, e o pipeline de CI que antes era um job só (build + test + upload) agora tem 6 jobs com SAST, DAST, deploy em 3 ambientes e aprovação manual pra produção. O deploy vai pro Railway, que detecta o Java e sobe a aplicação automaticamente. A cobertura mínima subiu de 85% pra 90%.

---

## Decisões

Escolhi `@MappedSuperclass` pro `BaseEntity` porque é a forma natural do JPA pra compartilhar campos sem criar tabela separada. Interfaces não resolveriam o mapeamento.

Separei as interfaces de consulta e comando mas não fui pra CQRS completo com event sourcing — seria over-engineering pro contexto. O repositório continua o mesmo pra leitura e escrita.

Mantive o Maven junto com o Gradle porque o Maven já tava estável e não queria quebrar quem já usava. O CI usa Gradle, mas `mvn clean test` continua funcionando.

Railway foi a escolha pra deploy porque suporta Java direto, detecta o build system e injeta a porta automaticamente. O free tier é mais que suficiente pro projeto.

O H2 em memória é proposital — dados se perdem ao reiniciar, mas elimina dependência de banco externo. Pra contexto acadêmico, compensa.
