<!--
    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License
-->
### Slingbucks 2: HTL based Rewrite

#### Introduction

This is a rewrite of the original Slingbucks Sample Project using
HTL and Sling Models.

The project was split into two modules so that we can use a JCR Content
Package which allows a user to develop content using Sling Tooling Plugins
(Eclipse or IntelliJ) without having to deploy the entire content after
each change.

The deployment of the content (ui.apps.slingbucks) is also deploying the
bundle with it.

The class **ConfirmedOrdersObserver** uses the deprecated administrative
login but this application will whitelist it during installation.
Please look here: **ui.apps.slingbucks/src/main/content/jcr_root/apps/slingbucks2App/config**.

#### Project Structure

The project has two modules:

1. **core.slingbucks**: OSGi Bundle that contains the services and Sling Model
2. **ui.apps.slingbucks**: JCR Package with the content

**Note**: to make this sample a little bit more instructive and to showcase
where there is a reference to /apps rather than to /content the project folders
in apps and content are named differently (slingbucks2App, slingbucks2Content).
This is not necessary in a regular application but during the rewrite this was
an issue a few times.

#### Build and Installation

The project is built quite simple:

    mvn clean install
    
To install the entire project use the project **autoInstallPackage**:

    mvn clean install -P autoInstallPackage

#### Usage

In order to make this work the user needs to be logged in. You can
remove the Allow Anonymous Access from the
[Sling Authenticator](http://localhost:8080/system/console/configMgr/org.apache.sling.engine.impl.auth.SlingAuthenticator)
but if not just remember to login first.  

If you are not sure then just go to the [composum page](http://localhost:8080/bin/browser.html)
and check on the top right if you are logged in.

##### As Customer

To order a coffee go to the [Ordering Page](http://localhost:8080/slingbucks2Content/public/orders.html)
select your options (which have an impact on the prive) and click on the **Order
Coffee** button.

Then you can review your order, adjust the options which in turn adjust the price
by pressing the *Recalculate** button and when you ready click on the **Confirm this order**.

You will see a notice that the order is confirmed.

**Attention**: reloading this page after a few seconds will cause an error as that
page was moved in the background.

**Attention**: if you get an error stating that the resource was not modifiable
then you were logged out in the mean time.

##### As Barista

To view and handle the customers' orders you can go to the
[Barista Page](http://localhost:8080/slingbucks2Content/private/confirmed.html).

All confirmed (and moved orders) will be listed here. Reload the page if
no orders show up. If you __handled__ the order you can click **Delivered -
delete this order** button and the order will be removed from the Sling.

**Attention**: if the order fails to list here then the background task
**ConfirmedOrdersObserver** is not active. Please check the log and the
[OSGi Console](http://localhost:8080/system/console/bundles) if the service
is up and running. Most likely the whitelisting failed. To check go to
the [OSGi Console Configuration Manager](http://localhost:8080/system/console/configMgr)
and search for **Whitelist**. There should be an entry called
**org.apache.sling.samples.slingbucks2** which the package **org.apache.sling.slingbucks2**.
If this is in place just try to redeploy the application and check the error
log if there is an error about this service.

#### Why a JCR Package instead of a Content Bundle

There a several reasons to use a JCR Package instead of a Content Bundle
but for the most important reason is that a JCR Package allows the **Sling
Tooling** to update a single file rather than an entire Bundle and also
to import a Node from Sling into the project.
