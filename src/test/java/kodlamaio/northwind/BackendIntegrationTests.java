package kodlamaio.northwind;

import kodlamaio.northwind.core.dataAccess.UserDao;
import kodlamaio.northwind.dataAccess.abstracts.CategoryDao;
import kodlamaio.northwind.dataAccess.abstracts.ProductDao;
import kodlamaio.northwind.entities.concretes.Category;
import kodlamaio.northwind.entities.concretes.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class BackendIntegrationTests {
    @Autowired WebApplicationContext context;
    @Autowired CategoryDao categoryDao;
    @Autowired ProductDao productDao;
    @Autowired UserDao userDao;
    @Autowired PasswordEncoder passwordEncoder;
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        productDao.deleteAllInBatch();
        userDao.deleteAllInBatch();
        categoryDao.deleteAllInBatch();
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        Category category = new Category();
        category.setCategoryId(1);
        category.setCategoryName("İçecekler");
        categoryDao.saveAndFlush(category);
    }

    private Product product(String name) {
        Product product = new Product();
        product.setProductName(name);
        product.setCategory(categoryDao.findById(1).orElseThrow());
        product.setUnitPrice(new BigDecimal("12.50"));
        product.setUnitsInStock((short) 5);
        return productDao.saveAndFlush(product);
    }

    @Test
    void registrationHashesPasswordAndNeverReturnsIt() throws Exception {
        mvc.perform(post("/api/users/add").contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email":"TEST@example.com","password":"StrongPass123!"}
                """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.email").value("test@example.com"))
            .andExpect(jsonPath("$.data.password").doesNotExist());

        var user = userDao.findAll().get(0);
        assertThat(user.getPassword()).isNotEqualTo("StrongPass123!");
        assertThat(passwordEncoder.matches("StrongPass123!", user.getPassword())).isTrue();
    }

    @Test
    void duplicateEmailIsCaseInsensitiveAndReturnsConflict() throws Exception {
        String payload = """
            {"email":"test@example.com","password":"StrongPass123!"}
            """;
        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(payload))
            .andExpect(status().isCreated());
        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
            .content(payload.replace("test@", "TEST@")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.success").value(false));
        assertThat(userDao.count()).isEqualTo(1);
    }

    @Test
    void invalidRegistrationReportsFieldsWithoutSaving() throws Exception {
        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email":"invalid","password":"x"}
                """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.data.email").isArray())
            .andExpect(jsonPath("$.data.password").isArray());
        assertThat(userDao.count()).isZero();
    }

    @Test
    void bcryptByteLimitIsEnforcedForUnicodePasswords() throws Exception {
        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"test@example.com\",\"password\":\"" + "ş".repeat(40) + "\"}"))
            .andExpect(status().isBadRequest());
        assertThat(userDao.count()).isZero();
    }

    @Test
    void productCreationValidatesCategoryAndReturnsLocation() throws Exception {
        mvc.perform(post("/api/products/add").contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"productName":"Chai","unitPrice":12.50,"unitsInStock":5,
                 "quantityPerUnit":"10 kutu","categoryId":1}
                """))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.data.productName").value("Chai"))
            .andExpect(jsonPath("$.data.category.categoryName").value("İçecekler"))
            .andExpect(jsonPath("$.data.category.products").doesNotExist());
        assertThat(productDao.count()).isEqualTo(1);
    }

    @Test
    void unknownCategoryReturns404WithoutSavingProduct() throws Exception {
        mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"productName":"Chai","unitPrice":12.50,"unitsInStock":5,"categoryId":999}
                """))
            .andExpect(status().isNotFound());
        assertThat(productDao.count()).isZero();
    }

    @Test
    void invalidProductFieldsAreRejected() throws Exception {
        mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"productName":" ","unitPrice":-1,"unitsInStock":-1,"categoryId":0}
                """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.data.productName").isArray())
            .andExpect(jsonPath("$.data.unitPrice").isArray())
            .andExpect(jsonPath("$.data.unitsInStock").isArray())
            .andExpect(jsonPath("$.data.categoryId").isArray());
        assertThat(productDao.count()).isZero();
    }

    @Test
    void malformedJsonHasConsistentErrorResponse() throws Exception {
        mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content("{broken"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void missingProductReturns404() throws Exception {
        mvc.perform(get("/api/products/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false));
        mvc.perform(get("/api/products/getByProductName").param("productName", "missing"))
            .andExpect(status().isNotFound());
    }

    @Test
    void completedSearchesAndProjectionReturnActualData() throws Exception {
        product("Chai");
        product("Coffee");
        mvc.perform(get("/api/products/getByProductNameContains").param("productName", "ha"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].productName").value("Chai"));
        mvc.perform(get("/api/products/getByProductNameStartsWith").param("productName", "Co"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].productName").value("Coffee"));
        mvc.perform(get("/api/products/getByCategory_CategoryIdIn").param("categories", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(2));
        mvc.perform(get("/api/products/getProductWithCategoryDetails"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].categoryName").value("İçecekler"));
    }

    @Test
    void paginationIncludesMetadataAndHasStableOrder() throws Exception {
        Product first = product("Chai");
        Product second = product("Coffee");
        mvc.perform(get("/api/products").param("pageNo", "1").param("pageSize", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content[0].id").value(first.getId()))
            .andExpect(jsonPath("$.data.totalElements").value(2))
            .andExpect(jsonPath("$.data.totalPages").value(2))
            .andExpect(jsonPath("$.data.last").value(false));
        mvc.perform(get("/api/products").param("pageNo", "2").param("pageSize", "1"))
            .andExpect(jsonPath("$.data.content[0].id").value(second.getId()))
            .andExpect(jsonPath("$.data.last").value(true));
    }

    @Test
    void invalidPaginationAndTypesReturn400() throws Exception {
        mvc.perform(get("/api/products").param("pageNo", "0"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/products/getallbypage").param("pageSize", "101"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/products").param("pageNo", "abc"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void legacyRoutesStillReturnLists() throws Exception {
        product("Chai");
        mvc.perform(get("/api/products/getall"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray());
        mvc.perform(get("/api/products/getallbypage"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void clientSuppliedIdentityCannotOverwriteAnExistingProduct() throws Exception {
        Product existing = product("Original");
        mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"id":%d,"productName":"New","unitPrice":10,"unitsInStock":1,"categoryId":1}
                """.formatted(existing.getId())))
            .andExpect(status().isCreated());
        assertThat(productDao.count()).isEqualTo(2);
        assertThat(productDao.findById(existing.getId()).orElseThrow().getProductName()).isEqualTo("Original");
    }

    @Test
    void concurrentRegistrationsCannotCreateDuplicateAccounts() throws Exception {
        var executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var start = new java.util.concurrent.CountDownLatch(1);
            java.util.concurrent.Callable<Integer> register = () -> {
                start.await();
                return mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"email":"race@example.com","password":"StrongPass123!"}
                        """)).andReturn().getResponse().getStatus();
            };
            var first = executor.submit(register);
            var second = executor.submit(register);
            start.countDown();
            assertThat(java.util.List.of(first.get(15, java.util.concurrent.TimeUnit.SECONDS),
                second.get(15, java.util.concurrent.TimeUnit.SECONDS)))
                .containsExactlyInAnyOrder(201, 409);
            assertThat(userDao.count()).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }
}
