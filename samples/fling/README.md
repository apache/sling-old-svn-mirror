Sling Fling Sample
==================

sample using Sling Models, Sling Query and Sling Scripting Thymeleaf

    karaf@root()> feature:repo-add mvn:org.apache.sling/org.apache.sling.launchpad.karaf-features/0.1.1-SNAPSHOT/xml/features
    karaf@root()> feature:install sling-launchpad-jackrabbit
    karaf@root()> feature:install sling-extension-models
    karaf@root()> feature:install sling-extension-query
    karaf@root()> feature:install sling-scripting-thymeleaf
    karaf@root()> feature:install sling-auth-form
