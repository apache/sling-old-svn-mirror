### Maven Project generated from Maven Archetype

#### Introduction

This project was created by the Sling Project Maven Archetype which created
two modules:

1. **core**: OSGi Bundle which is deployed as OSGi Bundle to Sling which includes your
             Servlets, Filters, Sling Models and much more. This module is **not intended**
             to contain Sling Content.
2. **ui.apps**: JCR Content Module which is used to install a JCR Package into Sling
                by using **Composum**. For that it must be installed and the Composum
                Package Manager must be whitelisted.

There are also two more modules that provide some examples with the same name plus
the **.example** extension. This modules should not be deployed as is but rather
examples that you want to use should be copied to the core or ui.apps module.
The structure of both modules are the same and so copying them over just be
quite simple.

#### Attention:

Due to the way Apache Maven Archetypes work both **example** modules are added
to the parent POM's module list. Please **remove** them after you created them
to avoid the installation of these modules into Sling.
At the end of the parent POM you will find the lines below. Remove the lines
with **core.example** and **ui.apps.example**.

    <modules>
        <module>core</module>
        <module>core.example</module>
        <module>ui.apps</module>
        <module>ui.apps.example</module>
    </modules>

#### Why a JCR Package instead of a Content Bundle

There a several reasons to use a JCR Package instead of a Content Bundle
but for the most important reason is that a JCR Package allows the **Sling
Tooling** to update a single file rather than an entire Bundle and also
to import a Node from Sling into the project.


#### Build and Installation

The project is built quite simple:

    mvn clean install
    
To install the OSGi bundle use the project **autoInstallBundle**:

    mvn clean install -P autoInstallBundle

To install the Content together with the core bundle
use the project **autoInstallPackage**:

    mvn clean install -P autoInstallPackage

##### ATTENTION

It is probably best to not deploy the OSGi Bundle alone as this
may lead to conflicts with the package deployment.

In case of a mishape the package and bundles needs to deinstalled
manullay:

1. Rmove /apps/${appsFolderName}/install folder
2. Uninstall the package using the package manager
3. Remove the package from /etc/packages including the snapshots if they are still there
4. Rmove the Bundle using the OSGi Console (/system/console/bundles)

