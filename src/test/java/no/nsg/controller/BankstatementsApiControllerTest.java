package no.nsg.controller;

import no.nsg.repository.ConnectionManager;
import no.nsg.testcategories.ServiceTest;
import org.junit.*;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.containers.PostgreSQLContainer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;


@SpringBootTest
@RunWith(SpringRunner.class)
@ContextConfiguration(initializers = {BankstatementsApiControllerTest.Initializer.class})
@Category(ServiceTest.class)
public class BankstatementsApiControllerTest {

    @Mock
    HttpServletRequest httpServletRequestMock;

    @Mock
    HttpServletResponse httpServletResponseMock;

    @Autowired
    BankstatementsApiControllerImpl bankstatementsApiController;

    @Autowired
    ConnectionManager connectionManager;

    @ClassRule
    public static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres:latest")
            .withDatabaseName("integration-tests-db")
            .withUsername("testuser")
            .withPassword("testpassword");

    static class Initializer
            implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
            TestPropertyValues.of(
                    "spring.datasource.url=" + postgreSQLContainer.getJdbcUrl(),
                    "spring.datasource.username=" + postgreSQLContainer.getUsername(),
                    "spring.datasource.password=" + postgreSQLContainer.getPassword(),
                    "postgres.nsg.db_url=" + postgreSQLContainer.getJdbcUrl(),
                    "postgres.nsg.dbo_user=" + postgreSQLContainer.getUsername(),
                    "postgres.nsg.dbo_password=" + postgreSQLContainer.getPassword(),
                    "postgres.nsg.user=" + postgreSQLContainer.getUsername(),
                    "postgres.nsg.password=" + postgreSQLContainer.getPassword()
            ).applyTo(configurableApplicationContext.getEnvironment());
        }
    }

    @Before
    public void before() throws IOException {
        connectionManager.waitUntilSyntheticDataIsImported();
    }

    @Test
    public void happyDay()
    {
        Assert.assertTrue(true);
    }

    @Ignore //Resource is imported in getBankstatementByIdTest below
    @Test
    public void createBankstatementTest() throws IOException {
        final String companyId = "todo";
        ResponseEntity<Void> response = bankstatementsApiController.createBankStatement(httpServletRequestMock, httpServletResponseMock, companyId, resourceAsString("camt/NSG.2.xml", StandardCharsets.UTF_8));
        Assert.assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    public void createInvalidBankstatementTest() throws IOException {
        final String companyId = "983294";

        Mockito.when(httpServletRequestMock.getContentType()).thenReturn("application/vnd.nordicsmartgovernment.bank-statement");
        ResponseEntity<Void> response = bankstatementsApiController.createBankStatement(httpServletRequestMock, httpServletResponseMock, companyId, resourceAsString("ubl/Invoice_base-example.xml", StandardCharsets.UTF_8));
        Assert.assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    public void getBankstatementsTest() {
        final String companyId = "todo";
        ResponseEntity<Object> response = bankstatementsApiController.getBankStatements(httpServletRequestMock, httpServletResponseMock, companyId);
        Assert.assertTrue(response.getStatusCode()==HttpStatus.OK || response.getStatusCode()==HttpStatus.NO_CONTENT);
    }

    @Test
    public void getBankstatementByIdTest() throws IOException {
        final String companyId = "todo";

        Mockito.when(httpServletRequestMock.getContentType()).thenReturn("application/vnd.nordicsmartgovernment.bank-statement");
        ResponseEntity<Void> createResponse = bankstatementsApiController.createBankStatement(httpServletRequestMock, httpServletResponseMock, companyId, resourceAsString("camt/NSG.1.xml", StandardCharsets.UTF_8));
        Assert.assertTrue(createResponse.getStatusCode() == HttpStatus.CREATED);
        URI location = createResponse.getHeaders().getLocation();
        String[] paths = location.getPath().split("/");
        String createdId = paths[paths.length-1];

        ResponseEntity<Object> response = bankstatementsApiController.getBankStatementById(httpServletRequestMock, httpServletResponseMock, companyId, createdId);
        Assert.assertTrue(response.getStatusCode() == HttpStatus.OK);

        BankstatementsApiControllerImpl.Bankstatement bankstatement = (BankstatementsApiControllerImpl.Bankstatement) response.getBody();
        Assert.assertEquals(createdId, bankstatement.documentid);
    }

    private static String resourceAsString(final String resource, final Charset charset) throws IOException {
        InputStream resourceStream = BankstatementsApiControllerTest.class.getClassLoader().getResourceAsStream(resource);

        StringBuilder sb = new StringBuilder();
        String line;
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(resourceStream, charset))) {
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }
}
