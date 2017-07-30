# About
This sample is designed as an introduction to Sling and HTL.

# Features
* Create, update, delete posts with featured images.
* List posts, read posts.

# Requirements
* [Java 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
* [Sling 8](http://sling.apache.org/downloads.cgi)
* [Maven 3+](http://maven.apache.org/download.cgi)

# Installation
## 1. Run Sling

    java -jar org.apache.sling.launchpad-8.jar

## 2. Install HTLBlog

    mvn clean install -PautoInstallBundle

## 3. Explore

    http://localhost:8080/content/htlblog.html

# Parts of the Application
## Java
HTL Blog uses Sling Models extensively. You can read about them [here](https://sling.apache.org/documentation/bundles/models.html).

* `Author.java` - This model detects if our user is logged in or not.
* `Edit.java` - This is used to get a query parameter from our request, find the resource associated with the param, and adapt it to our post model.
* `List.java` - This is used to iterate through our posts, reverse their order, and adapt them to our post model.
* `Post.java` - This model injects our post properties, finds the featured image, and lists any children (comments).

## Apps (HTL)

* Admin
  * Page - The base admin page.
  * List - Based on Page. Used to list posts.
  * Edit - Based on Page. Used to create and edit posts.
* Public
  * Page - The base public page.
  * List - Based on Page. Used to publicly list posts.
  * Post - Based on Page. Used to view an individual post.
* Clientlibs
  * Mostly Bootstrap and some basic custom styles.

## Content
The content is built using a single JSON file called `htlblog.json`. It maps to the following nodes:

* `/content/htlblog` - Our homepage. Currently lists our posts.
* `/content/htlblog/admin` - Our admin home. Used to list our posts for editing or deleting.
* `/content/htlblog/admin/edit` - Our edit page. Use to create, or if a post path is supplied, it will edit an existing post.
* `/content/htlblog/posts` - Another post list, but also used as the parent of our posts.
* `/content/htlblog/posts/hello-world` - Used to scaffold a placeholder post.
* `/content/htlblog/posts/hola-mundo` - Used to scaffold a 2nd placeholder post.

# Additional Reading

* The [SlingPostServet](https://sling.apache.org/documentation/bundles/manipulating-content-the-slingpostservlet-servlets-post.html) and [Sling CRUD](https://sling.apache.org/documentation/the-sling-engine/sling-api-crud-support.html) docs. Everything in this sample uses these concepts extensively.
* The [HTL Specification](https://github.com/Adobe-Marketing-Cloud/htl-spec/blob/master/SPECIFICATION.md). If you have a question about HTL, it will likely be answered here. Note: The spec and the current release in Sling can be out of sync.
* [Sling Content Loading](https://sling.apache.org/documentation/bundles/content-loading-jcr-contentloader.html) - Understand how to create initial content and map resourceTypes.
