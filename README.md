# TP3 → TP4

## Visão Geral

O projeto implementa um sistema CRUD integrado com dois módulos — **Produtos** e **Categorias** — usando Java 21 com Spring Boot 3.2.5, interface web renderizada via Thymeleaf e uma suíte de testes que cobre desde validações unitárias até simulações de falha, integração entre os módulos e interações no navegador.

No TP4, o sistema original de produtos foi refatorado e integrado com um novo módulo de categorias. A refatoração incluiu a criação de uma interface genérica `CrudService<T, ID>` para abstrair operações CRUD comuns, além da integração bidirecional entre os dois módulos (um produto pode pertencer a uma categoria; uma categoria não pode ser excluída enquanto tiver produtos vinculados).

Uma esteira de CI foi configurada via GitHub Actions para automatizar build, testes e validação de cobertura.

---

## Tecnologias

- **Java 21** + **Spring Boot 3.2.5**
- **Thymeleaf** — renderização server-side
- **Spring Data JPA** + **H2** — persistência em memória
- **Bean Validation** — validação declarativa de entidades
- **Bootstrap 5** — interface responsiva via CDN
- **JUnit 5** + **Mockito** — testes unitários e de integração
- **Selenium WebDriver** — testes de interface no Chrome headless
- **JaCoCo** — relatório e enforcement de cobertura
- **GitHub Actions** — CI automatizado

---

## Estrutura do Projeto

```
src/
├── main/java/com/crud/
│   ├── CrudApplication.java
│   ├── model/
│   │   ├── Product.java
│   │   └── Category.java
│   ├── repository/
│   │   ├── ProductRepository.java
│   │   └── CategoryRepository.java
│   ├── service/
│   │   ├── CrudService.java              # interface genérica reutilizável
│   │   ├── ProductService.java
│   │   └── CategoryService.java
│   ├── controller/
│   │   ├── ProductController.java
│   │   ├── CategoryController.java
│   │   └── HomeController.java
│   └── exception/
│       └── GlobalExceptionHandler.java
├── main/resources/
│   ├── application.properties
│   ├── templates/
│   │   ├── fragments/header.html         # nav bar reutilizável
│   │   ├── products/                     # list.html, form.html
│   │   ├── categories/                   # list.html, form.html
│   │   └── error.html
│   └── static/css/style.css
└── test/java/com/crud/
    ├── CrudApplicationTest.java
    ├── model/
    │   ├── ProductTest.java
    │   └── CategoryTest.java
    ├── service/
    │   ├── ProductServiceTest.java
    │   └── CategoryServiceTest.java
    ├── controller/
    │   ├── ProductControllerTest.java
    │   └── CategoryControllerTest.java
    ├── integration/
    │   └── ProductCategoryIntegrationTest.java
    ├── exception/GlobalExceptionHandlerTest.java
    ├── selenium/ProductSeleniumTest.java
    ├── fuzz/ProductFuzzTest.java
    ├── failure/FailureSimulationTest.java
    └── stress/StressTest.java

.github/workflows/
└── ci.yml                                # workflow de CI
```

---

## Como Executar

**Pré-requisitos:** Java 21+, Maven 3.8+ e Google Chrome (apenas para os testes Selenium).

```bash
# Iniciar a aplicação
mvn spring-boot:run
# Acesse: http://localhost:8080/products

# Executar todos os testes
mvn clean test

# Gerar relatório de cobertura
mvn clean test jacoco:report
# Relatório em: target/site/jacoco/index.html
```

---

## Cobertura de Testes

O mínimo exigido era 85%. O JaCoCo está configurado para **falhar o build** caso a cobertura caia abaixo desse limiar, o que torna o requisito verificável automaticamente — não apenas declarativo.

---

## Testes Implementados — 172 no total

### Unitários (`ProductServiceTest`, `CategoryServiceTest`)

Cobrem todas as ramificações dos services, incluindo os *null guards* que compõem a estratégia de *fail early*. O `CategoryServiceTest` também verifica a regra de integridade que impede exclusão de categorias com produtos vinculados.

### Controller (`ProductControllerTest`, `CategoryControllerTest`)

Usam MockMvc para testar os endpoints HTTP diretamente, sem subir um servidor real. Verificam redirecionamentos, mensagens flash e o comportamento dos controllers ao receber entradas inválidas.

### Integração (`ProductCategoryIntegrationTest`)

Verificam a comunicação entre os dois módulos: criação de produtos com e sem categoria, vínculo bidirecional, impedimento de exclusão de categoria com produtos, e os mesmos fluxos via HTTP.

### Selenium (`ProductSeleniumTest`)

Executam o fluxo completo — criar, editar e excluir um produto — num Chrome headless. Se o Chrome não estiver disponível na máquina, os testes são ignorados via `assumeTrue`, sem quebrar o build.

### Fuzz Testing (`ProductFuzzTest`)

Testam sistematicamente entradas maliciosas ou inesperadas: SQL injection, XSS, strings aleatórias, valores extremos.

### Simulação de Falhas (`FailureSimulationTest`)

Simulam condições adversas como falha de conexão com o banco e timeout de queries.

### Stress (`StressTest`)

Criam 50 produtos sequencialmente, disparam 10 requisições simultâneas e listam 100 registros.

### Modelo (`ProductTest`, `CategoryTest`)

Validam Bean Validation nas entidades com testes parametrizados.

---

## Refatoração — TP3 → TP4

### Interface genérica `CrudService<T, ID>`

Abstrai as cinco operações CRUD (`findAll`, `findById`, `save`, `update`, `delete`) numa interface reutilizável. Ambos os services implementam essa interface, eliminando duplicação de contrato e permitindo polimorfismo.

### Integração Product ↔ Category

- `Product` tem um relacionamento `@ManyToOne` opcional com `Category`.
- `CategoryService.delete()` verifica se existem produtos vinculados antes de permitir a exclusão — fail early aplicado à integridade referencial.
- O formulário de produto exibe um dropdown com as categorias disponíveis.
- A listagem de produtos mostra a categoria associada.

### Fragment Thymeleaf reutilizável

A barra de navegação (`fragments/header.html`) é compartilhada entre todas as páginas via `th:replace`, evitando duplicação de HTML.

---

## GitHub Actions — CI

O workflow `.github/workflows/ci.yml` automatiza:
- Build do projeto com Maven
- Execução de todos os testes
- Validação de cobertura via JaCoCo (mínimo 85%)
- Upload do relatório de cobertura como artefato

**Triggers:** `push` e `pull_request` na branch `main`, além de `workflow_dispatch` para execução manual.

O runner utilizado é `ubuntu-latest` (hospedado pelo GitHub), com Java 21 (Temurin) configurado via `actions/setup-java@v4`.

---

## Fail Early e Fail Gracefully

**Fail Early** — entradas inválidas são rejeitadas o mais cedo possível. O `@Valid` no controller barra dados que não atendem às constraints da entidade. Nos services, *null guards* rejeitam objetos nulos antes de qualquer acesso ao repositório. No `CategoryService`, a verificação de produtos vinculados impede exclusão indevida.

**Fail Gracefully** — exceções que chegam ao sistema são capturadas pelo `GlobalExceptionHandler` e convertidas em mensagens compreensíveis. Stack traces nunca são expostos.

---

## Decisões de Projeto e Premissas

**Segunda entidade:** O módulo de categorias foi escolhido como segundo sistema por ter relação direta com produtos e demonstrar integração real — restrição de exclusão, dropdown no formulário, coluna na listagem.

**Categoria opcional:** O vínculo entre produto e categoria é nullable para manter compatibilidade com dados existentes e não forçar categorização.

**H2 em memória:** Elimina dependência de banco externo. Dados são perdidos ao reiniciar — aceitável para contexto acadêmico.

**Idioma:** Interface e mensagens de erro em português brasileiro; código-fonte em inglês, seguindo a convenção Java.
