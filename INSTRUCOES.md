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
| Total de testes | 126 passando |
| Cobertura de linhas | 96,8% (mínimo: 85%) |
| Cobertura de branches | 100% |

O JaCoCo está configurado para **reprovar o build** se a cobertura cair abaixo de 85% — o requisito é verificado automaticamente a cada execução.

---

## Testes Implementados

| Categoria | Arquivo | Qtd. | O que cobre |
|---|---|---|---|
| Unitários | `ProductServiceTest.java` | 14 | Null guards, fail early, operações CRUD no service |
| Controller | `ProductControllerTest.java` | 15 | Endpoints via MockMvc, redirecionamentos, mensagens flash |
| Selenium | `ProductSeleniumTest.java` | 10 | Formulários, tabelas, botões, alertas de confirmação JS |
| Fuzz | `ProductFuzzTest.java` | 47 | SQL injection, XSS, strings aleatórias, valores extremos |
| Falhas | `FailureSimulationTest.java` | 14 | Timeout, banco indisponível, fail early/gracefully |
| Sobrecarga | `StressTest.java` | 3 | Volume sequencial, concorrência, listagem com 100 registros |
| Modelo | `ProductTest.java` | 12 | Bean Validation na entidade (testes parametrizados) |
| Exception Handler | `GlobalExceptionHandlerTest.java` | 3 | Tratamento centralizado de exceções |
| Contexto | `CrudApplicationTest.java` | 1 | Inicialização do contexto Spring Boot |

---

## Observações

**Selenium:** Os testes requerem Google Chrome instalado. Na ausência do browser, são ignorados via `assumeTrue` — o build não quebra.

**H2 em memória:** Os dados são perdidos ao reiniciar a aplicação. Para um contexto acadêmico, a troca de persistência por simplicidade de execução faz sentido.

**JaCoCo:** A cobertura mínima de 85% está configurada como gate no build. Não é apenas uma métrica observada — é um requisito enforçado.

---

## Premissas do Projeto

- **Entidade:** O enunciado não especificava qual entidade gerenciar. Foi escolhido *Produto* (nome, descrição, preço, quantidade) por ser um caso de uso direto e sem ambiguidades para demonstrar CRUD completo.
- **Banco de dados:** H2 em memória elimina dependência de infraestrutura externa.
- **Interface:** Thymeleaf com Bootstrap 5 via CDN — server-side rendering, sem build de frontend.
- **Idioma:** PT-BR na interface e mensagens de erro; inglês no código-fonte, seguindo a convenção Java.

---

## Estratégias de Tratamento de Erros

**Fail Early** — entradas inválidas são rejeitadas antes de qualquer processamento. O `@Valid` no controller barra dados que não atendem às constraints da entidade; o service usa *null guards* antes de qualquer acesso ao repositório.

**Fail Gracefully** — exceções que chegam ao sistema são capturadas pelo `GlobalExceptionHandler` e convertidas em mensagens compreensíveis. Stack traces nunca chegam ao usuário.

**Fuzz Testing** — payloads de SQL injection, XSS, caracteres unicode e valores numéricos extremos validam que as entradas são tratadas corretamente em todos os casos testados.

**Simulação de Falhas** — mocks simulam banco indisponível e timeouts para verificar resiliência fora do caminho feliz.