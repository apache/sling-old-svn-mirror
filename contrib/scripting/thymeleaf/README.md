Apache Sling Scripting Thymeleaf
================================

scripting engine for _Thymeleaf_ templates

* http://www.thymeleaf.org
* https://github.com/thymeleaf/thymeleaf

Features
--------

* out of the box support for _legacy_ HTML5 through embedded _NekoHTML_
* runtime configurable `TemplateModeHandler`s for _XML_, _VALIDXML_, _XHTML_, _VALIDXHTML_, _HTML5_ and _LEGACYHTML5_
* `ResourceResolver` backed by Sling's `ResourceResolver`
* `MessageResolver` backed by `ResourceBundleProvider` from `org.apache.sling.i18n`

relevant Thymeleaf issues
-------------------------

* [Create OSGi bundle](https://github.com/thymeleaf/thymeleaf/issues/32)
* [Remove initialize() steps in extension points](https://github.com/thymeleaf/thymeleaf/issues/54)
