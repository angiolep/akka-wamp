# Change log
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/) and this project adheres to [Semantic Versioning](http://semver.org/).


## Unreleased

### Changed
- Upgrade build to SBT 1.1.1

### Fixed
- Not compatible with akka 2.5.2 as reported by [\#46](https://github.com/angiolep/akka-wamp/issues/47)
- Routing occurs between sessions that have joined any realm as reported by [\#46](https://github.com/angiolep/akka-wamp/issues/45)


## [v0.15.1] _ 2017-06-17

### Fixed
- Standalone router could not read ``$WAMP_HOME/conf/application.conf`` as reported by [\#46](https://github.com/angiolep/akka-wamp/issues/46)


## [v0.15.0] _ 2017-01-23

### Changed
- Simplify access to ``args`` and ``kwargs`` from client APIs


## [v0.14.0] _ 2017-01-21

### Added
- Add more unit tests to increase coverage of Java API
- Router binds multiple listeners each configured with its own endpoint
- Provide WebSocket over TLS [\#14](https://github.com/angiolep/akka-wamp/issues/14)

### Changed
- Improve endpoint configuration
- Apply minor improvements to Java API
- Improve documentation


## [v0.13.0] _ 2017-01-07

### Added
- Apply **heavy refactorings** to provide client APIs usable by Java developers on Java8 platform

### Changed
- Improve the documentation significantly
- Provide more useful examples
 

## [v0.12.0] _ 2016-10-23

### Added
- Router serves static assets out of a configurable directory like a HTTP server [\#38](https://github.com/angiolep/akka-wamp/issues/38)
- Make Publisher tick a counter at given frequency [\#40](https://github.com/angiolep/akka-wamp/issues/40)
- Callee registers procedure with scala.Function handler [\#42](https://github.com/angiolep/akka-wamp/issues/42)

### Fixed
- Router doesn't validate role's feature announcements [\#37](https://github.com/angiolep/akka-wamp/issues/37)
- Caller doesn't handle Error(wamp.error.no_such_procedure) [\#39](https://github.com/angiolep/akka-wamp/issues/39)


## [v0.11.0] _ 2016-10-16

### Added
- ``unregister()`` method can be called on registration directly
- ``unsubscribe()`` method can be called on subscription directly
- ``Client.connect()`` method now accepts a maximum number of attempts before giving up

### Fixed
- Default JSON deserializer shall bind ``null`` to ``None``
- Support Unbind command messages to stop a router.TransportListener [\#13](https://github.com/angiolep/akka-wamp/issues/13)
- Support Disconnect command messages to stop a client.TransportHandler [\#29](https://github.com/angiolep/akka-wamp/issues/29)
- Router doesn't disconnect offending clients by default [\#33](https://github.com/angiolep/akka-wamp/issues/33)


## [v0.10.0] _ 2016-10-01

### Added
- Improve Payload Handling and Streaming support
- Implement deserialization of streamed text messages [\#27](https://github.com/angiolep/akka-wamp/issues/27)

### Fixed
- Both router and client shall validate RPC message types [\#34](https://github.com/angiolep/akka-wamp/issues/34)

    
## [v0.9.0] _ 2016-09-29    

### Added
- Provide routed RPC capabilities [\#26](https://github.com/angiolep/akka-wamp/issues/26)  
  - Client API now provides ``register``, ``unregister`` and ``call`` operations  
  - Router now able to register procedures and route calls to clients  
- Improve Payload Handling and Streaming support
- Improve tests
- Improve documentation

## [v0.8.0] _ 2016-09-19  

### Added  
- Validate dictionaries [\#19](https://github.com/angiolep/akka-wamp/issues/19)        
- Make decision on DeserializeException configurable [\#20](https://github.com/angiolep/akka-wamp/issues/20)  

### Fixed
- Repeated HELLOs are to be considered protocol error [\#21](https://github.com/angiolep/akka-wamp/issues/21)
- Offending messages are to be considered protocol errors [\#22](https://github.com/angiolep/akka-wamp/issues/22)

### Changed
- Improve ScalaDoc comments [\#23](https://github.com/angiolep/akka-wamp/issues/23)
- Improve [ReadTheDocs](http://akka-wamp.readthedocs.io/) 


## [v0.7.0] _ 2016-09-12

### Added
- Provide better API for payloads [\#16](https://github.com/angiolep/akka-wamp/issues/16)
- Parse incoming messages skipping the payload [\#17](https://github.com/angiolep/akka-wamp/issues/17)


## [v0.6.0] _ 2016-08-24

### Added
- Validate against loose URIs by default but strict URIs validation now configurable [\#7](https://github.com/angiolep/akka-wamp/issues/7)
- Auto-create realms by default but abort session (when client requests unknown realm) now configurable
- Log ``SessionException`` as warning in console for some unspecified session handling scenarios [\#21](https://github.com/angiolep/akka-wamp/issues/21)
- Improve validators for WAMP Ids and URIs
- Update user's documentation


## [v0.5.1] _ 2016-08-21

### Added
- Provide proper error handling in ``router.Connection`` [\#18](https://github.com/angiolep/akka-wamp/issues/18)


## 0.5.0 _ 2016-08-20

### Added
- WAMP Router with limited features
- Future based API for writing WAMP Clients with limited features
- Documentation

[Unreleased]: https://github.com/angiolep/akka-wamp/compare/v0.15.0...HEAD?&diff=split&name=HEAD
[v0.15.0]: https://github.com/angiolep/akka-wamp/compare/v0.15.0...v0.14.0?&diff=split&name=v0.15.0
[v0.14.0]: https://github.com/angiolep/akka-wamp/compare/v0.14.0...v0.13.0?&diff=split&name=v0.14.0
[v0.13.0]: https://github.com/angiolep/akka-wamp/compare/v0.13.0...v0.12.0?&diff=split&name=v0.13.0
[v0.12.0]: https://github.com/angiolep/akka-wamp/compare/v0.12.0...v0.11.0?&diff=split&name=v0.11.0
[v0.11.0]: https://github.com/angiolep/akka-wamp/compare/v0.11.0...v0.10.0?&diff=split&name=v0.10.0
[v0.10.0]: https://github.com/angiolep/akka-wamp/compare/v0.9.0...v0.10.0?diff=split&name=v0.9.0
[v0.9.0]: https://github.com/angiolep/akka-wamp/compare/v0.8.0...v0.9.0?diff=split&name=v0.8.0
[v0.8.0]: https://github.com/angiolep/akka-wamp/compare/v0.7.0...v0.8.0?diff=split&name=v0.7.0
[v0.7.0]: https://github.com/angiolep/akka-wamp/compare/v0.6.0...v0.7.0?diff=split&name=v0.7.0
[v0.6.0]: https://github.com/angiolep/akka-wamp/compare/v0.5.1...v0.6.0?diff=split&name=v0.6.0
[v0.5.1]: https://github.com/angiolep/akka-wamp/compare/v0.5.0...v0.5.1?diff=split&name=v0.5.1