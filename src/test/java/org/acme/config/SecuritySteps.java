package org.acme.config;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Step definitions de features/security.feature (règle LlmSafetyPolicy).
 * Pas de mock nécessaire : la règle est une fonction pure (entrées → autorisé / refusé).
 */
public class SecuritySteps {

    private String provider;
    private String jdbcUrl;
    private boolean azureSettingsPresent = true;
    private boolean refused;

    @Given("the LLM provider is {string}")
    public void theLlmProviderIs(String provider) {
        this.provider = provider;
    }

    @Given("the database is {string}")
    public void theDatabaseIs(String database) {
        this.jdbcUrl = "jdbc:postgresql://localhost:5432/" + database;
    }

    @Given("the Azure credentials are missing")
    public void theAzureCredentialsAreMissing() {
        this.azureSettingsPresent = false;
    }

    @When("the application checks its configuration")
    public void theApplicationChecksItsConfiguration() {
        try {
            LlmSafetyPolicy.check(provider, jdbcUrl, azureSettingsPresent);
            refused = false;
        } catch (IllegalStateException e) {
            refused = true;
        }
    }

    @Then("the startup is refused")
    public void theStartupIsRefused() {
        assertTrue(refused);
    }

    @Then("the startup is allowed")
    public void theStartupIsAllowed() {
        assertFalse(refused);
    }
}
