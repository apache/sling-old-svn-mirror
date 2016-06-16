Apache Sling Scripting - Sightly implementation bundles
====
These bundles provide support for the Sightly web templating language.

## Contents
1. `engine` - `org.apache.sling.scripting.sightly`
  The Apache Sling Scripting Sightly Engine is a Java implementation of the Sightly specification. The bundle contains the Sightly 
  engine and its plugin framework implementation.
2. `js-use-provider` - `org.apache.sling.scripting.sightly.js.provider`
  The Apache Sling Sightly JavaScript Use Provider adds support for accessing JS scripts from Sightly's Use-API.
3. `repl` - `org.apache.sling.scripting.sightly.repl`
  A simple Read-Eval-Print-Loop environment for testing simple Sightly scripts
4. `testing-content` - `org.apache.sling.scripting.sightly.testing-content`
  A bundle containing initial content for running integration tests
5. `testing` - `org.apache.sling.scripting.sightly.testing`
  The testing project which builds a custom Sling Launchpad on which integration tests are run
  
## How To

**Build**
```bash
mvn clean install
```

**Test**
```bash
mvn clean verify
```

**Play with Sightly REPL**
```bash
cd testing
mvn clean package slingstart:start -Dlaunchpad.keep.running=true -Dhttp.port=8080
```
Then browse to [http://localhost:8080/sightly/repl.html](http://localhost:8080/sightly/repl.html).
 
