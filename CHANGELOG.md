# Changelog

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