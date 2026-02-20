# TP3

## Visão Geral

O projeto implementa um sistema CRUD completo usando Java 21 com Spring Boot 3.2.5, interface web renderizada via Thymeleaf e uma suíte de testes que cobre desde validações unitárias até simulações de falha e interações no navegador.

A escolha do stack não foi arbitrária: Spring Boot elimina grande parte da configuração manual, o H2 em memória dispensa a instalação de um banco externo para rodar os testes, e o Thymeleaf já escapa HTML por padrão — o que resolve uma classe inteira de vulnerabilidades XSS sem nenhum código adicional.

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

---

## Estrutura do Projeto

```
src/
├── main/java/com/crud/
│   ├── CrudApplication.java
│   ├── model/Product.java
│   ├── repository/ProductRepository.java
│   ├── service/ProductService.java
│   ├── controller/
│   │   ├── ProductController.java
│   │   └── HomeController.java
│   └── exception/
│       └── GlobalExceptionHandler.java
├── main/resources/
│   ├── application.properties
│   ├── templates/products/           # list.html, form.html
│   ├── templates/error.html
│   └── static/css/style.css
└── test/java/com/crud/
    ├── CrudApplicationTest.java
    ├── model/ProductTest.java
    ├── service/ProductServiceTest.java
    ├── controller/ProductControllerTest.java
    ├── exception/GlobalExceptionHandlerTest.java
    ├── selenium/ProductSeleniumTest.java
    ├── fuzz/ProductFuzzTest.java
    └── failure/FailureSimulationTest.java
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

| Métrica      | Cobertura |
|--------------|-----------|
| Linhas       | 96,8%     |
| Branches     | 100%      |
| Instruções   | 97,9%     |

O mínimo exigido era 85%. O JaCoCo está configurado para **falhar o build** caso a cobertura caia abaixo desse limiar, o que torna o requisito verificável automaticamente — não apenas declarativo.

---

## Testes Implementados — 126 no total

### Unitários (`ProductServiceTest`)

Cobrem todas as ramificações do service, incluindo os *null guards* que compõem a estratégia de *fail early*: nenhuma chamada chega ao repositório com dados inválidos ou nulos.

### Controller (`ProductControllerTest`)

Usam MockMvc para testar os endpoints HTTP diretamente, sem subir um servidor real. Verificam redirecionamentos, mensagens flash e o comportamento do controller ao receber entradas inválidas.

### Selenium (`ProductSeleniumTest`)

Executam o fluxo completo — criar, editar e excluir um produto — num Chrome headless. Também cobrem alertas de confirmação JavaScript e mensagens de erro exibidas na interface. Se o Chrome não estiver disponível na máquina, os testes são ignorados via `assumeTrue`, sem quebrar o build.

### Fuzz Testing (`ProductFuzzTest`)

Testam sistematicamente entradas maliciosas ou inesperadas. Os resultados estão documentados na seção de vulnerabilidades abaixo.

### Simulação de Falhas (`FailureSimulationTest`)

Simulam condições adversas como falha de conexão com o banco (`DataAccessResourceFailureException`) e timeout de queries, verificando se o sistema responde de forma controlada em ambos os casos.

### Stress (`StressTest`)

Criam 50 produtos sequencialmente, disparam 10 requisições simultâneas e listam 100 registros para verificar comportamento sob carga.

---

## Relatório de Vulnerabilidades

Os testes de fuzz cobriram os vetores de ataque mais comuns para aplicações web com formulários. Nenhum foi explorado com sucesso.

| Vetor | Payloads | Resultado |
|---|---|---|
| SQL Injection | 5 payloads (`'; DROP TABLE`, `' OR '1'='1`, `UNION SELECT`...) | **Mitigado.** JPA usa prepared statements; o payload é salvo como texto literal. |
| XSS | 5 payloads (`<script>`, `<img onerror>`, `<svg onload>`...) | **Mitigado.** Thymeleaf escapa HTML com `th:text` por padrão. |
| Null bytes | `\0\0\0` | **Tratado.** Sem crash ou comportamento anômalo. |
| Unicode / Emoji | Japonês, emoji, acentuação latina | **Aceito.** Armazenado e exibido corretamente. |
| Overflow de inteiro | `2147483648` (MAX_VALUE + 1) | **Rejeitado.** O binding falha antes de chegar ao banco. |
| Valores numéricos inválidos | `NaN`, `Infinity`, `abc` como preço ou quantidade | **Rejeitados.** Binding falha; formulário retorna com erro. |
| Strings longas | 1000+ caracteres | **Rejeitadas.** `@Size(max=100)` impede a persistência. |

A proteção contra SQL Injection e XSS é estrutural — decorre da forma como o JPA e o Thymeleaf funcionam, não de filtros manuais. Isso é preferível a qualquer abordagem baseada em sanitização manual, que tende a ser incompleta.

---

## Fail Early e Fail Gracefully

Duas estratégias complementares organizam o tratamento de erros na aplicação.

**Fail Early** significa rejeitar entradas inválidas o mais cedo possível, antes de qualquer processamento custoso. No controller, o `@Valid` barra dados que não atendem às constraints da entidade. No service, *null guards* rejeitam objetos nulos antes de qualquer acesso ao repositório.

**Fail Gracefully** trata os erros que inevitavelmente chegam até o sistema — exceções de banco, timeouts, estados inesperados — e os converte em respostas compreensíveis para o usuário. O `GlobalExceptionHandler` centraliza esse tratamento: stack traces nunca são expostos, e a página de erro genérica exibe apenas "Ocorreu um erro inesperado".

As duas estratégias não competem entre si; operam em camadas diferentes da aplicação.

---

## Decisões de Projeto e Premissas

**Entidade escolhida:** O enunciado não especificava qual entidade gerenciar. Foi escolhido *Produto* (nome, descrição, preço, quantidade) por ser um exemplo direto e sem ambiguidades para demonstrar as operações CRUD.

**H2 em memória:** Elimina dependência de banco externo para rodar a aplicação e os testes. A consequência é que os dados são perdidos ao reiniciar — aceitável para um contexto acadêmico.

**Idioma:** Interface e mensagens de erro em português brasileiro; código-fonte em inglês, seguindo a convenção Java.