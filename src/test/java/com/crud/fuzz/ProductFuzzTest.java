package com.crud.fuzz;

import com.crud.model.Product;
import com.crud.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Random;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ProductFuzzTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository repository;

    private static final Random RANDOM = new Random(42);

    @BeforeEach
    void cleanUp() {
        repository.deleteAll();
    }

    // Payloads de SQL Injection
    @ParameterizedTest
    @ValueSource(strings = {
            "'; DROP TABLE products; --",
            "' OR '1'='1",
            "1; DELETE FROM products",
            "' UNION SELECT * FROM products --",
            "admin'--"
    })
    @DisplayName("Deve resistir a tentativas de SQL Injection")
    void shouldResistSqlInjection(String maliciousInput) throws Exception {
        mockMvc.perform(post("/products/save")
                        .param("name", maliciousInput)
                        .param("description", maliciousInput)
                        .param("price", "10.00")
                        .param("quantity", "1"))
                .andExpect(status().is3xxRedirection());

        // Garante que o banco não foi comprometido
        var products = repository.findAll();
        // O produto deve ter sido salvo com o texto literal, sem executar SQL
        if (!products.isEmpty()) {
            Product saved = products.get(0);
            assertEquals(maliciousInput, saved.getName());
        }
    }

    // Payloads de XSS
    @ParameterizedTest
    @ValueSource(strings = {
            "<script>alert('XSS')</script>",
            "<img src=x onerror=alert('XSS')>",
            "<svg onload=alert('XSS')>",
            "javascript:alert('XSS')",
            "<iframe src='javascript:alert(1)'></iframe>"
    })
    @DisplayName("Deve tratar payloads XSS sem executar scripts")
    void shouldHandleXssPayloads(String xssPayload) throws Exception {
        mockMvc.perform(post("/products/save")
                        .param("name", xssPayload)
                        .param("description", xssPayload)
                        .param("price", "10.00")
                        .param("quantity", "1"))
                .andExpect(status().is3xxRedirection());
    }

    // Strings aleatórias (fuzz testing)
    @RepeatedTest(10)
    @DisplayName("Deve lidar com strings aleatórias sem crashar")
    void shouldHandleRandomStrings() throws Exception {
        String randomName = generateRandomString(RANDOM.nextInt(200));
        String randomDesc = generateRandomString(RANDOM.nextInt(600));
        String randomPrice = String.valueOf(RANDOM.nextDouble() * 10000 - 5000); // pode ser negativo
        String randomQty = String.valueOf(RANDOM.nextInt(2001) - 1000); // pode ser negativo

        // O sistema não deve crashar com entradas aleatórias
        mockMvc.perform(post("/products/save")
                        .param("name", randomName)
                        .param("description", randomDesc)
                        .param("price", randomPrice)
                        .param("quantity", randomQty))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.junit.jupiter.api.Assertions.assertTrue(
                            status == 200 || status == 302,
                            "Status inesperado: " + status
                    );
                });
    }

    // Caracteres especiais e unicode
    @ParameterizedTest
    @MethodSource("specialCharInputs")
    @DisplayName("Deve lidar com caracteres especiais e unicode")
    void shouldHandleSpecialCharacters(String input) throws Exception {
        mockMvc.perform(post("/products/save")
                        .param("name", input)
                        .param("description", input)
                        .param("price", "10.00")
                        .param("quantity", "1"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    // Aceita tanto 200 (formulário com erros) quanto 302 (redirecionamento)
                    org.junit.jupiter.api.Assertions.assertTrue(
                            status == 200 || status == 302,
                            "Status inesperado: " + status
                    );
                });
    }

    static Stream<String> specialCharInputs() {
        return Stream.of(
                "Produto com acentuação: ñ ü ö",
                "日本語テスト",
                "🎉🚀💻",
                "\0\0\0", // null bytes
                "\t\n\r",
                "A".repeat(1000), // string muito longa
                "",
                "   ",
                "!@#$%^&*()_+-=[]{}|;':\",./<>?"
        );
    }

    // Valores extremos para preço
    @ParameterizedTest
    @ValueSource(strings = {
            "0", "-1", "99999999999", "0.001", "NaN", "Infinity",
            "-Infinity", "abc", "1e308", "-0"
    })
    @DisplayName("Deve lidar com valores extremos de preço")
    void shouldHandleExtremePriceValues(String price) throws Exception {
        mockMvc.perform(post("/products/save")
                        .param("name", "Produto Teste")
                        .param("price", price)
                        .param("quantity", "1"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.junit.jupiter.api.Assertions.assertTrue(
                            status == 200 || status == 302 || status == 400,
                            "Status inesperado: " + status
                    );
                });
    }

    // Valores extremos para quantidade
    @ParameterizedTest
    @ValueSource(strings = {
            "-1", "-999999", "0", "2147483647", "2147483648", "abc", "1.5"
    })
    @DisplayName("Deve lidar com valores extremos de quantidade")
    void shouldHandleExtremeQuantityValues(String quantity) throws Exception {
        mockMvc.perform(post("/products/save")
                        .param("name", "Produto Teste")
                        .param("price", "10.00")
                        .param("quantity", quantity))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.junit.jupiter.api.Assertions.assertTrue(
                            status == 200 || status == 302 || status == 400,
                            "Status inesperado: " + status
                    );
                });
    }

    @Test
    @DisplayName("Deve tratar IDs inválidos sem expor stack trace")
    void shouldHandleInvalidIds() throws Exception {
        // Tenta deletar com ID muito grande
        mockMvc.perform(post("/products/delete/999999999"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error"));
    }

    private static String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()_+-=[]{}|;':\",./<>?àáãâéêíóôõúüñ";
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(RANDOM.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
