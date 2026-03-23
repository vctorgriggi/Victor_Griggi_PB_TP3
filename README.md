# TP4

## Visão Geral

Este TP dá continuidade ao sistema CRUD de produtos desenvolvido no TP3. O objetivo agora foi refatorar o código existente, integrar um segundo módulo (Categorias) e configurar uma esteira de CI via GitHub Actions.

A ideia central da refatoração foi extrair uma interface genérica `CrudService<T, ID>` que ambos os módulos implementam. Isso evita duplicação de contrato e deixa claro que qualquer entidade nova no futuro segue o mesmo padrão — basta implementar a interface.

Para a integração, a escolha de Categorias não foi aleatória: é a relação mais natural com Produtos e permite demonstrar vínculo real entre os dois sistemas — um produto pode pertencer a uma categoria, e uma categoria não pode ser excluída enquanto tiver produtos associados. Essa restrição é validada no service antes de chegar ao banco, seguindo a mesma estratégia de fail early do TP3.

---

## Tecnologias

- **Java 21** + **Spring Boot 3.2.5**
- **Thymeleaf** — renderização server-side
- **Spring Data JPA** + **H2** — persistência em memória
- **Bean Validation** — validação declarativa
- **Bootstrap 5** — interface responsiva via CDN
- **JUnit 5** + **Mockito** — testes unitários e de integração
- **Selenium WebDriver** — testes de interface (Chrome headless)
- **JaCoCo** — cobertura de testes com enforcement no build
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
│   │   ├── CrudService.java              # interface genérica
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
│   │   ├── fragments/header.html         # nav compartilhada
│   │   ├── products/                     # list.html, form.html
│   │   ├── categories/                   # list.html, form.html
│   │   └── error.html
│   └── static/css/style.css
└── test/java/com/crud/
    ├── CrudApplicationTest.java
    ├── model/ProductTest.java, CategoryTest.java
    ├── service/ProductServiceTest.java, CategoryServiceTest.java
    ├── controller/ProductControllerTest.java, CategoryControllerTest.java
    ├── integration/ProductCategoryIntegrationTest.java
    ├── exception/GlobalExceptionHandlerTest.java
    ├── selenium/ProductSeleniumTest.java
    ├── fuzz/ProductFuzzTest.java
    ├── failure/FailureSimulationTest.java
    └── stress/StressTest.java

.github/workflows/ci.yml
```

---

## Como Executar

**Pré-requisitos:** Java 21+, Maven 3.8+ e Google Chrome (só para Selenium).

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

## O que mudou do TP3 para o TP4

### Refatoração: interface genérica

O TP3 tinha apenas o `ProductService` com os métodos CRUD soltos. Para o TP4, criei a interface `CrudService<T, ID>` com as cinco operações (`findAll`, `findById`, `save`, `update`, `delete`). Tanto o `ProductService` quanto o novo `CategoryService` implementam essa interface. Na prática, isso significa que o contrato é o mesmo para qualquer entidade — se amanhã precisar de um terceiro módulo, a estrutura já está pronta.

### Integração: Product ↔ Category

O `Product` ganhou um campo `@ManyToOne` opcional apontando para `Category`. Opcional porque não faz sentido forçar categorização — um produto pode existir sem categoria, e os dados do TP3 continuam válidos.

No formulário de produto, um dropdown lista as categorias disponíveis. Na listagem, uma coluna mostra a categoria de cada produto (ou "-" quando não tem).

A parte mais interessante da integração é a restrição de exclusão: o `CategoryService.delete()` consulta o `ProductRepository.existsByCategoryId()` antes de deletar. Se existem produtos vinculados, a operação é barrada com uma mensagem clara. É fail early aplicado à integridade referencial — o erro acontece antes de tocar no banco.

### Fragment Thymeleaf

Todas as páginas agora compartilham uma barra de navegação via `th:replace` de `fragments/header.html`. Antes, cada template era uma ilha. Agora existe navegação entre Produtos e Categorias sem precisar digitar URL.

### GitHub Actions

O workflow `.github/workflows/ci.yml` roda em todo push e PR na main. Usa `ubuntu-latest` com Java 21 (Temurin via `actions/setup-java@v4`), executa `mvn clean test` (que já inclui o check do JaCoCo) e sobe o relatório de cobertura como artefato. Também aceita `workflow_dispatch` para execução manual.

O runner é hospedado pelo GitHub — não configurei auto-hospedado porque para um projeto desse porte não faz sentido manter infraestrutura própria.

---

## Testes — 172 no total

Os 126 testes do TP3 continuam passando. Os 46 novos cobrem o módulo de categorias e a integração entre os dois sistemas.

| Categoria | Arquivo | O que cobre |
|---|---|---|
| Unitários | `ProductServiceTest` | Null guards, fail early, CRUD |
| Unitários | `CategoryServiceTest` | CRUD + impedimento de exclusão com produtos vinculados |
| Controller | `ProductControllerTest` | Endpoints HTTP, redirecionamentos, flash messages |
| Controller | `CategoryControllerTest` | Mesma cobertura para o módulo de categorias |
| Integração | `ProductCategoryIntegrationTest` | Vínculo entre os dois módulos via service e via HTTP |
| Selenium | `ProductSeleniumTest` | Fluxo completo no browser (Chrome headless) |
| Fuzz | `ProductFuzzTest` | SQL injection, XSS, unicode, valores extremos |
| Falhas | `FailureSimulationTest` | Banco indisponível, timeout, fail early/gracefully |
| Stress | `StressTest` | Volume, concorrência, listagem pesada |
| Modelo | `ProductTest`, `CategoryTest` | Bean Validation nas entidades |
| Exception | `GlobalExceptionHandlerTest` | Tratamento centralizado |
| Contexto | `CrudApplicationTest` | Inicialização do Spring Boot |

### Cobertura (JaCoCo)

| Métrica | Cobertura |
|---|---|
| Instruções | 98% |
| Branches | 100% |
| Linhas | 98% (178 de 181) |

O mínimo exigido era 85%. O JaCoCo está configurado pra reprovar o build se cair abaixo disso — então não é uma métrica que eu olho manualmente, é um gate automático. O relatório completo é gerado com `mvn clean test jacoco:report` (fica em `target/site/jacoco/index.html`) e também é enviado como artefato no workflow de CI.

---

## Fail Early e Fail Gracefully

A estratégia do TP3 foi mantida e estendida para o novo módulo.

**Fail Early:** validação com `@Valid` no controller, null guards nos services, e agora também a verificação de integridade referencial no `CategoryService.delete()`. A ideia é sempre rejeitar o mais cedo possível, antes de comprometer qualquer recurso.

**Fail Gracefully:** o `GlobalExceptionHandler` continua capturando exceções não tratadas e devolvendo mensagens genéricas. Stack traces nunca chegam ao usuário.

---

## Decisões e Premissas

**Por que Categorias?** Era a entidade que mais fazia sentido para integrar com Produtos. A relação é direta, permite demonstrar restrição de exclusão e preenche o requisito de comunicação entre os dois sistemas sem forçar uma abstração artificial.

**Categoria opcional:** o vínculo é nullable de propósito. Não quis quebrar compatibilidade com os dados do TP3 nem obrigar o usuário a criar categorias antes de cadastrar produtos.

**H2 em memória:** mesma decisão do TP3. Dados se perdem ao reiniciar, mas elimina dependência de banco externo. Para contexto acadêmico, a troca compensa.

**Idioma:** interface em PT-BR, código em inglês. Mesma convenção do TP3.
