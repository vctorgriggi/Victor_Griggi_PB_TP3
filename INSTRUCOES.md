# Instruções

## Pré-requisitos

- Java 21+
- Maven 3.8+
- Google Chrome (só para os testes Selenium — se não tiver, eles são ignorados e o build não quebra)

---

## Executando a Aplicação

```bash
mvn spring-boot:run
```

Acesse [http://localhost:8080/products](http://localhost:8080/products). A barra de navegação no topo permite alternar entre Produtos e Categorias.

---

## Testes e Cobertura

```bash
# Rodar todos os testes
mvn clean test

# Gerar relatório de cobertura (abre em target/site/jacoco/index.html)
mvn clean test jacoco:report
```

São 172 testes no total. O JaCoCo está configurado para reprovar o build automaticamente se a cobertura de linhas cair abaixo de 85%, então o `mvn clean test` já valida isso — não precisa rodar nada separado.

---

## GitHub Actions

O workflow de CI (`.github/workflows/ci.yml`) roda automaticamente em push e PR na main. Também dá para disparar manualmente via `workflow_dispatch`.

O que ele faz:
1. Configura Java 21 (Temurin) no runner `ubuntu-latest`
2. Roda `mvn clean test` (build + testes + check de cobertura)
3. Sobe o relatório JaCoCo como artefato

Para acompanhar: aba **Actions** no repositório do GitHub. O relatório de cobertura fica disponível para download nos artefatos de cada execução.

---

## Resumo dos Testes

| Tipo | Arquivo | O que testa |
|---|---|---|
| Unitários | `ProductServiceTest` | CRUD do service, null guards, fail early |
| Unitários | `CategoryServiceTest` | CRUD de categorias, bloqueio de exclusão com produtos |
| Controller | `ProductControllerTest` | Endpoints HTTP, validação, flash messages |
| Controller | `CategoryControllerTest` | Mesma cobertura para categorias |
| Integração | `ProductCategoryIntegrationTest` | Comunicação entre os dois módulos (service e HTTP) |
| Selenium | `ProductSeleniumTest` | Fluxo completo no Chrome headless |
| Fuzz | `ProductFuzzTest` | SQL injection, XSS, unicode, valores extremos |
| Falhas | `FailureSimulationTest` | Banco indisponível, timeout |
| Stress | `StressTest` | Volume, concorrência |
| Modelo | `ProductTest`, `CategoryTest` | Bean Validation |
| Exception | `GlobalExceptionHandlerTest` | Tratamento centralizado de exceções |
| Contexto | `CrudApplicationTest` | Inicialização do Spring Boot |

---

## Principais Mudanças (TP3 → TP4)

**Interface genérica `CrudService<T, ID>`** — antes, o `ProductService` tinha os métodos CRUD sem nenhum contrato formal. Agora existe uma interface que tanto ele quanto o `CategoryService` implementam. Se precisar de um terceiro módulo no futuro, a estrutura já está pronta.

**Módulo de Categorias** — CRUD completo com model, repository, service, controller e templates. Segue exatamente o mesmo padrão do módulo de Produtos.

**Integração Product ↔ Category** — produto tem um `@ManyToOne` opcional com categoria. O formulário de produto mostra um dropdown de categorias, e a listagem exibe a categoria associada. Categorias com produtos vinculados não podem ser excluídas (o service valida antes de deletar).

**Nav bar compartilhada** — fragment Thymeleaf (`fragments/header.html`) incluído em todas as páginas via `th:replace`. Antes cada template era independente.

**CI com GitHub Actions** — build, testes e cobertura automatizados em cada push/PR.

---

## Observações

- O H2 é em memória — dados se perdem ao reiniciar. É proposital; elimina dependência de banco externo.
- Os testes Selenium precisam do Chrome, mas se ele não estiver instalado, os testes são ignorados via `assumeTrue` e o build segue normalmente.
- A categoria no produto é opcional. Dá para cadastrar produtos sem categoria, e os dados antigos continuam válidos.
