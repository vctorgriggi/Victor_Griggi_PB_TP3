package com.crud.failure;

import com.crud.model.Product;
import com.crud.repository.ProductRepository;
import com.crud.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class FailureSimulationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductRepository repository;

    @Autowired
    private ProductService service;

    @Nested
    @DisplayName("Simulação de falha de banco de dados")
    class DatabaseFailureTests {

        @Test
        @DisplayName("Deve tratar falha de conexão ao listar")
        void shouldHandleDatabaseFailureOnList() throws Exception {
            when(repository.findAll())
                    .thenThrow(new DataAccessResourceFailureException("Conexão perdida"));

            mockMvc.perform(get("/products"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("error"));
        }

        @Test
        @DisplayName("Deve tratar falha de conexão ao salvar")
        void shouldHandleDatabaseFailureOnSave() throws Exception {
            when(repository.save(any()))
                    .thenThrow(new DataAccessResourceFailureException("Banco indisponível"));

            mockMvc.perform(post("/products/save")
                            .param("name", "Produto")
                            .param("price", "10.00")
                            .param("quantity", "1"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("products/form"))
                    .andExpect(model().attributeExists("error"));
        }

        @Test
        @DisplayName("Deve tratar falha de conexão ao excluir")
        void shouldHandleDatabaseFailureOnDelete() throws Exception {
            when(repository.existsById(anyLong())).thenReturn(true);
            doThrow(new DataAccessResourceFailureException("Timeout"))
                    .when(repository).deleteById(anyLong());

            mockMvc.perform(post("/products/delete/1"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(flash().attributeExists("error"));
        }
    }

    @Nested
    @DisplayName("Simulação de timeout")
    class TimeoutTests {

        @Test
        @DisplayName("Deve tratar timeout na consulta")
        void shouldHandleQueryTimeout() throws Exception {
            when(repository.findAll())
                    .thenThrow(new QueryTimeoutException("Query timeout após 30s"));

            mockMvc.perform(get("/products"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("error"));
        }

        @Test
        @DisplayName("Deve tratar timeout ao buscar produto")
        void shouldHandleFindByIdTimeout() throws Exception {
            when(repository.findById(anyLong()))
                    .thenThrow(new QueryTimeoutException("Timeout"));

            mockMvc.perform(get("/products/edit/1"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("error"));
        }
    }

    @Nested
    @DisplayName("Entradas inválidas e maliciosas")
    class InvalidInputTests {

        @ParameterizedTest
        @CsvSource({
                "'', 10.00, 1",
                "'   ', 10.00, 1",
                "Produto, -5.00, 1",
                "Produto, 10.00, -1"
        })
        @DisplayName("Deve rejeitar entradas inválidas via formulário")
        void shouldRejectInvalidFormInputs(String name, String price, String quantity) throws Exception {
            mockMvc.perform(post("/products/save")
                            .param("name", name)
                            .param("price", price)
                            .param("quantity", quantity))
                    .andExpect(status().isOk())
                    .andExpect(view().name("products/form"));
        }

        @Test
        @DisplayName("Deve tratar erro ao acessar rota inexistente")
        void shouldHandle404() throws Exception {
            mockMvc.perform(get("/products/rota-invalida"))
                    .andExpect(result -> {
                        int statusCode = result.getResponse().getStatus();
                        org.junit.jupiter.api.Assertions.assertTrue(
                                statusCode == 200 || statusCode >= 400,
                                "Status inesperado: " + statusCode
                        );
                    });
        }
    }

    @Nested
    @DisplayName("Fail Early - Validação antecipada no service")
    class FailEarlyTests {

        @Test
        @DisplayName("Deve falhar antes de acessar o banco para produto nulo")
        void shouldFailEarlyForNullProduct() {
            assertThrows(IllegalArgumentException.class, () -> service.save(null));
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Deve falhar antes de acessar o banco para ID nulo no delete")
        void shouldFailEarlyForNullIdOnDelete() {
            assertThrows(IllegalArgumentException.class, () -> service.delete(null));
            verify(repository, never()).deleteById(any());
        }

        @Test
        @DisplayName("Deve falhar antes de acessar o banco para ID nulo no findById")
        void shouldFailEarlyForNullIdOnFind() {
            assertThrows(IllegalArgumentException.class, () -> service.findById(null));
            verify(repository, never()).findById(any());
        }

        @Test
        @DisplayName("Deve falhar antes de acessar o banco para ID nulo no update")
        void shouldFailEarlyForNullIdOnUpdate() {
            Product p = new Product("Test", "desc", new BigDecimal("10"), 1);
            assertThrows(IllegalArgumentException.class, () -> service.update(null, p));
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Deve falhar antes de acessar o banco para produto nulo no update")
        void shouldFailEarlyForNullProductOnUpdate() {
            assertThrows(IllegalArgumentException.class, () -> service.update(1L, null));
            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Fail Gracefully - Respostas amigáveis")
    class FailGracefullyTests {

        @Test
        @DisplayName("Não deve expor stack trace ao usuário")
        void shouldNotExposeStackTrace() throws Exception {
            when(repository.findAll())
                    .thenThrow(new RuntimeException("NullPointerException at com.crud.internal.SomeClass"));

            String content = mockMvc.perform(get("/products"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            assertFalse(content.contains("NullPointerException"));
            assertFalse(content.contains("com.crud.internal"));
            assertTrue(content.contains("Erro") || content.contains("erro"));
        }

        @Test
        @DisplayName("Deve exibir mensagem amigável para produto não encontrado")
        void shouldShowFriendlyNotFoundMessage() throws Exception {
            when(repository.findById(999L)).thenReturn(java.util.Optional.empty());

            mockMvc.perform(get("/products/edit/999"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(flash().attribute("error", org.hamcrest.Matchers.containsString("não encontrado")));
        }
    }
}
