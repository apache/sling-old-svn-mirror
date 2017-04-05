### Maven Project generated from Maven Archetype

#### Introduction

This project was created by the Full Project Sling Maven Archetype which created
three modules:

1. **core**: OSGi Bundle which is deployed as OSGi Bundle to Sling which includes your
             Servlets, Filters, Sling Models and much more. This module is **not intended**
             to contain Sling Content.
2. **ui.apps**: JCR Content Module which is used to install a JCR Package into Sling
                by using **Composum**. For that it must be installed and the Composum
                Package Manager must be whitelisted.
3. **all**: This is another JCR Content Module but it is only used to install the
            other two modules. 

There are also two more modules that provide some examples with the same name plus
the **.example** extension. This modules should not be deployed as is but rather
examples that you want to use should be copied to the core or ui.apps module.
The structure of both modules are the same and so copying them over just be
quite simple.

#### Why the All Package

Most real projects have many different OSGi bundles, Content Packages, Configuration
Modules and many more. Deploying them one by one is cumbersome and can lead to
inconsitency and to a lot of overhead in a Continious Integration system.
The **All** package allows you to deploy all theses artifacts in one swoop or it allows
you to deploy them to multiple targets by just repeating the **All** deployment.

##### Adding a new Module

If you create a new Maven module then you need to add them to the **All** POM as
well to include them into the All deployment. These are the steps:

1. Add the dependency to the new module in the All POM
2. Add the module to the **maven-vault-plugin** definition
    1. If this is a content package then into the **subPackages**
    2. If this is an OSGi Bundle then into the **embeddeds**

##### Package Filter

In any multi-content-package environment the developer needs to pay close attention
to the **content filtering** in the **META-INF/vault/filter.xml** as this can lead
to hard to detect issues. Please make sure that:

1. Exclude **/apps/&lt;apps-folder-name>/install** from any of your content package
   as in that folder the **All** package is installing the bundles into
2. Make sure that content packages are not removing each other contents. The rule is
   that each content package has their own sub folder inside **/apps/&lt;apps-folder-name>**
   and avoid overlap.
3. Any shared folders like **overlays** need to be separated from each other.
   It is a good idea to limit your filter to smallest subset possible to avoid
   future issues if another package needs to place their overlays into the
   same folder. 

The package filter is a **mask** that tells Sling which part of the JCR tree
your package maintains and after the deployment that part of the JCR tree will
be the same as in your package. All missing ndoes in Sling will be created, all
existing nodes will be updated and all missing nodes in your package will be
deleted in Sling.


#### Why a JCR Package instead of a Content Bundle

There a several reasons to use a JCR Package instead of a Content Bundle
but for the most important reason is that a JCR Package allows the **Sling
Tooling** to update a single file rather than an entire Bundle and also
to import a Node from Sling into the project.


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
        <module>all</module>
    </modules>

#### Build and Installation

The project is built quite simple:

    mvn clean install
    
To install the project **autoInstallAll**:

    mvn clean install -P autoInstallAll

##### ATTENTION

It is not a good idea to deploy code woth both approaches.
Choose one and stick with it as you can either loose a bundle
or the bundle is not updated during installation.

In case of a mishape the package and bundles needs to deinstalled
manullay:

1. Rmove /apps/${appsFolderName}/install folder
2. Uninstall the package using the package manager
3. Remove the package from /etc/packages including the snapshots if they are still there
4. Rmove the Bundle using the OSGi Console (/system/console/bundles)

