Feature: Keep local documents away from cloud LLMs

  As the owner of confidential documents
  I want the application to refuse any cloud LLM on my local database
  So that my documents can never be sent outside my machine by mistake

  Scenario: Cloud LLM is refused on the local database
    Given the LLM provider is "azure"
    And the database is "ragdb"
    When the application checks its configuration
    Then the startup is refused

  Scenario: Cloud LLM is refused on any database that is not explicitly allowed
    Given the LLM provider is "azure"
    And the database is "another_db"
    When the application checks its configuration
    Then the startup is refused

  Scenario: Cloud LLM is allowed on the demo database
    Given the LLM provider is "azure"
    And the database is "ragdb_demo"
    When the application checks its configuration
    Then the startup is allowed

  Scenario: Cloud LLM without credentials is refused
    Given the LLM provider is "azure"
    And the database is "ragdb_demo"
    And the Azure credentials are missing
    When the application checks its configuration
    Then the startup is refused

  Scenario: Local LLM is allowed on the local database
    Given the LLM provider is "ollama"
    And the database is "ragdb"
    When the application checks its configuration
    Then the startup is allowed

  Scenario: Unknown LLM provider is refused
    Given the LLM provider is "openai"
    And the database is "ragdb_demo"
    When the application checks its configuration
    Then the startup is refused
