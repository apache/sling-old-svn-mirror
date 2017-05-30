### Sling Template through Maven Archetype

#### Introduction

This archetype will create a Sling project that can be deployed on the Sling 9. In contrast to
the Apache Sling Maven Archetypes this one is geared towards creating a full project and not
just a single module.

The **core** and **ui.apps** are empty Maven modules that are ready for you code.

There are also **example** modules with the extenions of **.exmaple**. Please copy whatever you
want from these module into the appropriate module. The example modules are not intended to be
installed into Sling.
Both type (regular and example) have the same structure and so you can copy them over without
any problems.

The **all** content package is a convenient way to deploy all content packages and OSGi bundles in
one step to avoid issues with dependencies.

#### Why a Separate All Package

For a simple project that only contains one or two modules (bundle and content package) this seems
like overkill and the same can be accomplished using the ui.apps as single deployment package.

That said for a more serious project where there are multiple bundles and content packages this
becomes difficult to handle. Not only needs one Content Package to take the role of the single
deployment package which requires it to add the other modules as dependencies. This can be confusing
and so the dedicated All content package solves that. It contains the dependencies and the logic
how to build and deploy the entire project in one step.

#### Archetype Properties

|Name                 |Description                                                                   |
|:--------------------|:-----------------------------------------------------------------------------|
|groupId              |Maven Group Id|
|artifactId           |Maven Artifact Id|
|version              |Version of your project|
|artifactName         |Project Label used in the Descriptions and Module Name|
|packageGroup         |Name of the Package Folder where the ui.apps is installed in (/etc/packages)|
|appsFolderName       |Folder name under /apps where components etc are installed|
|contentFolderName    |Folder name under /content where the content is added to|
|package              |Root Package of the Java Code|
|slingModelSubPackage |Sub Package where Sling Models should be place in with no trailing dots|
|slingHostName        |Host Name or IP Address of the server where Sling is hosted|
|slingPort            |Port to which your Sling instance is bound to|

#### Usage

Until this project is fully released in the public Maven Repo this is how to use it:

* Build this project locally

    mvn clean install

* Go to your folder where you want your generated project to be
* Generate it with:

    mvn archetype:generate -DarchetypeCatalog=local

* Select this Archetype from a given list
* Provide the requested properties

#### Build and Install Integration Test

There is a simple integration test setup with this archetype.
You can run this one using the attached Unix shell script:

    sh build.run.and.deploy.test.sh

The properties for the integration test can be found under
**/test/resources/projects/basic/archetype.properties**.

