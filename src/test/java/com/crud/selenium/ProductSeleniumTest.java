package com.crud.selenium;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProductSeleniumTest {

    @LocalServerPort
    private int port;

    private static WebDriver driver;
    private static boolean driverAvailable = false;

    @BeforeAll
    static void setupDriver() {
        try {
            WebDriverManager.chromedriver().setup();
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless", "--no-sandbox", "--disable-dev-shm-usage",
                    "--window-size=1920,1080");
            driver = new ChromeDriver(options);
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
            driverAvailable = true;
        } catch (Exception e) {
            System.out.println("Chrome não disponível, testes Selenium serão ignorados: " + e.getMessage());
        }
    }

    @AfterAll
    static void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @BeforeEach
    void checkDriver() {
        assumeTrue(driverAvailable, "Chrome WebDriver não disponível");
    }

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    // Usa JavaScript para clicar em elementos que podem estar obscurecidos
    private void jsClick(WebElement element) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
    }

    @Test
    @Order(1)
    @DisplayName("Deve exibir página de lista de produtos")
    void shouldShowProductList() {
        driver.get(baseUrl() + "/products");
        assertEquals("Produtos - Sistema CRUD", driver.getTitle());
        assertNotNull(driver.findElement(By.id("btn-new")));
    }

    @Test
    @Order(2)
    @DisplayName("Deve exibir mensagem quando lista está vazia")
    void shouldShowEmptyMessage() {
        driver.get(baseUrl() + "/products");
        WebElement emptyMsg = driver.findElement(By.id("empty-message"));
        assertTrue(emptyMsg.isDisplayed());
        assertTrue(emptyMsg.getText().contains("Nenhum produto cadastrado"));
    }

    @Test
    @Order(3)
    @DisplayName("Deve navegar para formulário de criação")
    void shouldNavigateToCreateForm() {
        driver.get(baseUrl() + "/products");
        driver.findElement(By.id("btn-new")).click();

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("product-form")));

        assertNotNull(driver.findElement(By.id("product-form")));
        assertTrue(driver.findElement(By.id("form-title")).getText().contains("Cadastrar"));
    }

    @Test
    @Order(4)
    @DisplayName("Deve criar produto via formulário")
    void shouldCreateProduct() {
        driver.get(baseUrl() + "/products/new");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("product-form")));

        driver.findElement(By.id("name")).sendKeys("Teclado Mecânico");
        driver.findElement(By.id("description")).sendKeys("Teclado com switches blue");
        driver.findElement(By.id("price")).sendKeys("299.90");
        driver.findElement(By.id("quantity")).sendKeys("25");

        jsClick(driver.findElement(By.id("btn-save")));

        wait.until(ExpectedConditions.urlContains("/products"));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("success-alert")));

        WebElement successAlert = driver.findElement(By.id("success-alert"));
        assertTrue(successAlert.getText().contains("cadastrado com sucesso"));

        WebElement table = driver.findElement(By.id("products-table"));
        assertTrue(table.getText().contains("Teclado Mecânico"));
    }

    @Test
    @Order(5)
    @DisplayName("Deve exibir dados na tabela")
    void shouldShowDataInTable() {
        driver.get(baseUrl() + "/products");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("products-table")));

        WebElement table = driver.findElement(By.id("products-table"));
        assertNotNull(table.findElement(By.tagName("thead")));
        assertNotNull(table.findElement(By.tagName("tbody")));
        assertFalse(table.findElements(By.tagName("tr")).isEmpty());
    }

    @Test
    @Order(6)
    @DisplayName("Deve editar produto existente")
    void shouldEditProduct() {
        driver.get(baseUrl() + "/products");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".btn-edit")));

        jsClick(driver.findElement(By.cssSelector(".btn-edit")));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("form-title")));

        assertTrue(driver.findElement(By.id("form-title")).getText().contains("Editar"));

        WebElement nameField = driver.findElement(By.id("name"));
        nameField.clear();
        nameField.sendKeys("Teclado Atualizado");

        jsClick(driver.findElement(By.id("btn-save")));

        wait.until(ExpectedConditions.urlContains("/products"));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("success-alert")));

        WebElement successAlert = driver.findElement(By.id("success-alert"));
        assertTrue(successAlert.getText().contains("atualizado com sucesso"));
    }

    @Test
    @Order(7)
    @DisplayName("Deve exibir alerta de confirmação ao excluir")
    void shouldShowDeleteConfirmation() {
        driver.get(baseUrl() + "/products");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".btn-delete")));

        jsClick(driver.findElement(By.cssSelector(".btn-delete")));

        Alert alert = wait.until(ExpectedConditions.alertIsPresent());
        assertTrue(alert.getText().contains("Tem certeza"));

        alert.dismiss();

        // Produto deve permanecer na tabela após cancelar
        assertNotNull(driver.findElement(By.id("products-table")));
    }

    @Test
    @Order(8)
    @DisplayName("Deve excluir produto confirmando alerta")
    void shouldDeleteProductOnConfirm() {
        driver.get(baseUrl() + "/products");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".btn-delete")));

        jsClick(driver.findElement(By.cssSelector(".btn-delete")));

        Alert alert = wait.until(ExpectedConditions.alertIsPresent());
        alert.accept();

        wait.until(ExpectedConditions.urlContains("/products"));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("success-alert")));

        WebElement successAlert = driver.findElement(By.id("success-alert"));
        assertTrue(successAlert.getText().contains("excluído com sucesso"));
    }

    @Test
    @Order(9)
    @DisplayName("Deve exibir erros de validação no formulário")
    void shouldShowValidationErrors() {
        driver.get(baseUrl() + "/products/new");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("product-form")));

        jsClick(driver.findElement(By.id("btn-save")));

        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("product-form")));
        assertFalse(driver.findElements(By.className("invalid-feedback")).isEmpty());
    }

    @Test
    @Order(10)
    @DisplayName("Deve funcionar botão Cancelar")
    void shouldCancelRedirectToList() {
        driver.get(baseUrl() + "/products/new");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("btn-cancel")));

        driver.findElement(By.id("btn-cancel")).click();

        wait.until(ExpectedConditions.urlContains("/products"));
        assertNotNull(driver.findElement(By.id("btn-new")));
    }
}
