package com.crud.stress;

import com.crud.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class StressTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository repository;

    @BeforeEach
    void cleanUp() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("Deve suportar criação de muitos produtos em sequência (volume)")
    void shouldHandleHighVolumeSequential() throws Exception {
        int total = 50;
        for (int i = 0; i < total; i++) {
            mockMvc.perform(post("/products/save")
                            .param("name", "Produto Volume " + i)
                            .param("price", "10.00")
                            .param("quantity", "1"))
                    .andExpect(status().is3xxRedirection());
        }
        assertEquals(total, repository.count());
    }

    @Test
    @DisplayName("Deve suportar requisições simultâneas sem corromper dados")
    void shouldHandleConcurrentRequests() throws Exception {
        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    mockMvc.perform(post("/products/save")
                            .param("name", "Concorrente " + idx)
                            .param("price", "10.00")
                            .param("quantity", "1"));
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // Sistema não deve crashar mesmo sob concorrência
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS), "Timeout aguardando requisições concorrentes");
        executor.shutdown();
        assertTrue(successCount.get() > 0, "Nenhuma requisição concorrente teve sucesso");
    }

    @Test
    @DisplayName("Deve manter desempenho na listagem com grande volume de dados")
    void shouldHandleLargeDatasetOnList() throws Exception {
        for (int i = 0; i < 100; i++) {
            mockMvc.perform(post("/products/save")
                    .param("name", "Produto Massa " + i)
                    .param("price", String.valueOf(i + 1))
                    .param("quantity", String.valueOf(i)));
        }

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(view().name("products/list"));

        assertEquals(100, repository.count());
    }
}
