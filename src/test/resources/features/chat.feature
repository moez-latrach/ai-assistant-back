Feature: Ask a question to the RAG assistant

  As a user of the document assistant
  I want to ask questions about the documents I select
  So that I get answers based only on those documents

  Scenario: No document selected
    Given a user question "What is an intermittent permanent contract?"
    And no document is selected
    When the user sends the question
    Then the user is asked to select at least one document
    And neither the embedding model, the vector store nor the LLM is called

  Scenario: Document list missing from the request
    Given a user question "What is an intermittent permanent contract?"
    And the request contains no document list
    When the user sends the question
    Then the user is asked to select at least one document
    And neither the embedding model, the vector store nor the LLM is called

  Scenario: No relevant passage found in the selected documents
    Given a user question "What is an intermittent permanent contract?"
    And 1 document is selected
    And the vector search returns no passage
    When the user sends the question
    Then the user is told that no relevant information was found
    And the LLM is not called
