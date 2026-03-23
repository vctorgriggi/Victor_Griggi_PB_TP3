# Instruções

## Pré-requisitos

- Java 21+
- Maven 3.8+
- Google Chrome (apenas para os testes Selenium)

---

## Executando a Aplicação

```bash
mvn spring-boot:run
```

Acesse: [http://localhost:8080/products](http://localhost:8080/products)

A barra de navegação permite alternar entre **Produtos** e **Categorias**.

---

## Testes e Cobertura

```bash
# Executar todos os testes
mvn clean test

# Gerar relatório de cobertura
mvn clean test jacoco:report
# Relatório em: target/site/jacoco/index.html
```

---

## Resultados Esperados

| Métrica | Resultado |
|---|---|
| Total de testes | 172 passando |
| Cobertura mínima | 85% (enforçado pelo JaCoCo) |

O JaCoCo está configurado para **reprovar o build** se a cobertura cair abaixo de 85%.

---

## Testes Implementados

| Categoria | Arquivo | O que cobre |
|---|---|---|
| Unitários | `ProductServiceTest.java` | Null guards, fail early, operações CRUD no service |
| Unitários | `CategoryServiceTest.java` | CRUD de categorias, impedimento de exclusão com produtos |
| Controller | `ProductControllerTest.java` | Endpoints via MockMvc, redirecionamentos, mensagens flash |
| Controller | `CategoryControllerTest.java` | Endpoints de categoria, validação, erros |
| Integração | `ProductCategoryIntegrationTest.java` | Vínculo Product↔Category via service e HTTP |
| Selenium | `ProductSeleniumTest.java` | Formulários, tabelas, botões, alertas JS |
| Fuzz | `ProductFuzzTest.java` | SQL injection, XSS, strings aleatórias, valores extremos |
| Falhas | `FailureSimulationTest.java` | Timeout, banco indisponível, fail early/gracefully |
| Sobrecarga | `StressTest.java` | Volume sequencial, concorrência, listagem com 100 registros |
| Modelo | `ProductTest.java` | Bean Validation na entidade Product |
| Modelo | `CategoryTest.java` | Bean Validation na entidade Category |
| Exception Handler | `GlobalExceptionHandlerTest.java` | Tratamento centralizado de exceções |
| Contexto | `CrudApplicationTest.java` | Inicialização do contexto Spring Boot |

---

## GitHub Actions — CI

O workflow de CI é executado automaticamente em:
- `push` na branch `main`
- `pull_request` na branch `main`
- `workflow_dispatch` (execução manual)

O workflow realiza: build, execução de testes, validação de cobertura (JaCoCo) e upload do relatório como artefato.

Para verificar o status: aba **Actions** do repositório no GitHub.

---

## Observações

**Selenium:** Os testes requerem Google Chrome instalado. Na ausência do browser, são ignorados via `assumeTrue` — o build não quebra.

**H2 em memória:** Os dados são perdidos ao reiniciar a aplicação.

**JaCoCo:** A cobertura mínima de 85% está configurada como gate no build.

---

## Premissas do Projeto

- **Entidades:** Produto (nome, descrição, preço, quantidade, categoria) e Categoria (nome, descrição).
- **Integração:** Produto pertence opcionalmente a uma Categoria. Categorias com produtos vinculados não podem ser excluídas.
- **Banco de dados:** H2 em memória elimina dependência de infraestrutura externa.
- **Interface:** Thymeleaf com Bootstrap 5 via CDN — server-side rendering, sem build de frontend.
- **Idioma:** PT-BR na interface e mensagens de erro; inglês no código-fonte, seguindo a convenção Java.

---

## Refatoração — Principais Mudanças

- **Interface genérica `CrudService<T, ID>`:** Abstrai operações CRUD comuns. `ProductService` e `CategoryService` implementam a mesma interface.
- **Integração Product↔Category:** Relacionamento `@ManyToOne` opcional. Restrição de exclusão de categoria com produtos vinculados.
- **Fragment Thymeleaf:** Barra de navegação compartilhada entre todas as páginas (`fragments/header.html`).
- **GitHub Actions:** Workflow de CI com build, testes e cobertura automatizados.
- **Testes de integração:** `ProductCategoryIntegrationTest` verifica a comunicação entre os dois módulos.
