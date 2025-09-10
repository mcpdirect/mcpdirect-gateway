# Changelog

## [1.2.0] - 2025-09-10

### Added
- Version endpoint constant for version reporting
- HstpServiceEngine dependency for enhanced service capabilities

### Changed
- Upgraded hstp-service-engine version from 1.3.0 to 1.4.0
- Refactored API key substring extraction for better readability
- Updated tool call response handling to return empty JSON object instead of empty string
- Minor code formatting improvements in servlet implementation

### Fixed
- Variable declaration syntax in doPost method

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