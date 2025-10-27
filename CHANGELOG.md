# Changelog

## [1.2.0] - 2025-10-08

### Changed
- Upgraded hstp-service-engine version from 1.4.2 to 1.5.0
- Upgraded MCP version from 0.12.1 to 0.14.1
- Updated project version from 1.1.2 to 1.2.0-SNAPSHOT
- Updated spring-ai-mcp-version from 1.0.1 to 1.1.0-M3
- Replaced Jackson ObjectMapper with McpJsonMapper for better MCP protocol compatibility
- Updated TypeReference to TypeRef for MCP JSON serialization/deserialization

### Added
- Enhanced MCP gateway functionality with improved transport handling
- Tool API endpoint functionality to support direct tool calls
- New MCP endpoint constant for enhanced routing
- JUnit dependency for testing
- Static McpJsonMapper initialization in MCPdirectGatewaySseHttpServlet and MCPdirectTransportProvider
- USL constant for list user tools endpoint in AIToolHubServiceHandler
- Support for SyncToolSpecification with proper input schema handling
- Database integration with PostgreSQL and MyBatis for data persistence
- Redis support with Jedis for caching
- DAO layer with AccountDataHelper and AIToolDataHelper for data access
- Entity classes for account management (AIPortUser, AIPortAccessKey, etc.)
- Entity classes for AI tool management (AIPortTool, AIPortToolAgent, etc.)
- MyBatis mappers for database operations (AccountMapper, AIToolMapper, etc.)
- MCPToolSchema utility for tool schema management

### Fixed
- Improved path parsing logic for authentication extraction
- Enhanced error handling for tool API endpoints
- Updated tool specification creation to use proper builder pattern
- Fixed unmarshalling method to use McpJsonMapper instead of Jackson ObjectMapper

### Refactored
- Replaced direct McpSchema.Tool constructor with builder pattern in MCPdirectTransportProvider
- Updated AIToolHubServiceHandler to use USL constant instead of string literal for service creation
- Renamed keyId to apiKeyHash for consistency in AIToolHubServiceHandler
- Cleaned up deprecated or unused ObjectMapper code
- Renamed DAO and mapper classes to use MCP prefixes for better consistency (AccountDataHelper → MCPAccessKeyDataHelper, AIToolDataHelper → MCPToolDataHelper, etc.)
- Updated application configuration and service classes to use new DAO/mapper class names
- Improved performance in AIToolHubServiceHandler by optimizing tool retrieval and using HashSet for better deduplication
- Updated AIToolHubServiceHandler to use more efficient stream operations and proper sorting

### Fixed
- Updated MCPdirectTransportProvider to properly deserialize tool results using JSON instead of returning raw text
- Improved error handling in AITool class to return proper MCP schema responses
- Added ResponseOfAIService class for better response structure handling
- Updated permission checks to allow agent_status > -1 instead of only = 1 for more flexible access control
- Improved error messages in AITool to use more professional language ("notify user" instead of "tell user")
- Updated tool availability message to be clearer when a tool is unavailable

## [1.1.2] - 2025-09-10

### Changed
- Upgraded hstp-service-engine version from 1.4.1 to 1.4.2
- Upgraded MCP version from 0.11.3 to 0.12.1

## [1.1.1] - 2025-09-10

### Changed
- Updated project version from 1.1.0-SNAPSHOT to 1.1.1-SNAPSHOT
- Upgraded hstp-service-engine version from 1.4.0 to 1.4.1

## [1.1.0] - 2025-09-03

### Added
- Tool API endpoint functionality to support direct tool calls
- New MCP endpoint constant for enhanced routing
- JUnit dependency for testing

### Changed
- Updated project version from 1.0.0-SNAPSHOT to 1.1.0-SNAPSHOT
- Upgraded MCP version from 0.11.2 to 0.11.3
- Refactored MCPdirectGatewaySseHttpServlet to support both SSE and MCP endpoints
- Updated protocol version to MCP_2025_03_26
- Renamed getMCPwingsTransportProvider to getMCPdirectTransportProvider for consistency
- Cleaned up commented-out code in servlet implementation

### Fixed
- Improved path parsing logic for authentication extraction
- Enhanced error handling for tool API endpoints

### Test
- Added initial test file for MCPdirectGatewaySseHttpServlet