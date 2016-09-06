Apache Sling HTL Maven Plugin
====
The Apache Sling HTL Maven Plugin, M2Eclipse compatible, provides support for validating HTML Template Language scripts from projects during build time.

## Goals overview

* [`htl:validate`](#htlvalidate) - validate the scripts from the build directory (`${project.build.sourceDirectory}`)

## Usage
```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.sling</groupId>
            <artifactId>htl-maven-plugin</artifactId>
            <version>1.0.0</version>
            <executions>
                <execution>
                    <id>validate-scripts</id>
                    <goals>
                        <goal>validate</goal>
                    </goals>
                    <phase>compile</phase>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

## `htl:validate`
**Full name:** `org.apache.sling:htl-maven-plugin:1.0.0:validate`

**Description:**
Validates HTL scripts syntax during the build process.

### Optional Parameters

|Name                                         |Type        |Since    |Description                                                                                       |
|---------------------------------------------|------------|---------|--------------------------------------------------------------------------------------------------|
| [`sourceDirectory`](#param-sourceDirectory) | `String`   | `1.0.0` | Defines the root folder where this goal expects to find Sightly scripts to validate.             |
| [`includes`](#param-includes)               | `String[]` | `1.0.0` | List of files to include, specified as fileset patterns which are relative to `sourceDirectory`. |
| [`excludes`](#param-excludes)               | `String[]` | `1.0.0` | List of files to exclude, specified as fileset patterns which are relative to `sourceDirectory`. |
| [`failOnWarnings`](#param-failOnWarnings)   | `boolean`  | `1.0.0` | If set to `true` it will fail the build on compiler warnings.                                    |

### Parameter Details

<a name="param-sourceDirectory"><code>sourceDirectory</code></a>:

Defines the root folder where this goal expects to find Sightly scripts to validate.
* **Type:** `java.lang.String`
* **Required:** No
* **User Property:** `sourceDirectory`
* **Default:** `${project.build.sourceDirectory}`

<a name="param-includes"><code>includes</code></a>:

List of files to include, specified as fileset patterns which are relative to `sourceDirectory`.
* **Type:** `java.lang.String[]`
* **Required:** No
* **User Property:** `includes`
* **Default:** `**/*.html`

<a name="param-excludes"><code>excludes</code></a>:

List of files to exclude, specified as fileset patterns which are relative to `sourceDirectory`.
* **Type:** `java.lang.String[]`
* **Required:** No
* **User Property:** `excludes`

<a name="param-failOnWarnings"><code>failOnWarnings</code></a>:

If set to `true` it will fail the build on compiler warnings.
* **Type:** `boolean`
* **Required:** No
* **User Property:** `failOnWarnings`
* **Default:** `false`

