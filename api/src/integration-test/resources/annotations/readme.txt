JSON files in this directory simulate responses received from Annotation API.

The "id" property for each annotation is a resolvable URL containing the domain for the Annotation API
instance being queried.

Annotation ids for testing should use the "http://test-annotation-host" domain. This value is replaced in
IntegrationTestUtils.loadFileAndReplaceServerUrl() with the server name and port of
the Mock Annotation API server used for integration testing.
