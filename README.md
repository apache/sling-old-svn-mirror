Apache Sling Scripting Thymeleaf
================================

scripting engine for [_Thymeleaf_](http://www.thymeleaf.org) templates

Features
--------

* Supporting all of Thymeleaf's extension points: _TemplateResolver_﻿s, _MessageResolver_﻿s, _Dialect_﻿s, _LinkBuilder_﻿s, _DecoupledTemplateLogicResolver_ and _CacheManager_﻿
* `NonCachingTemplateResolver` customizable through `TemplateModeProvider`﻿
* `MessageResolver` backed by `ResourceBundleProvider` from `org.apache.sling.i18n` customizable through optional `AbsentMessageRepresentationProvider`﻿
* `PatternSpecTemplateModeProvider` supporting `PatternSpec` configurations for all template modes (`HTML`, `XML`, `TEXT`, `JAVASCRIPT`, `CSS` and `RAW`)
* `SlingDialect`
* Thymeleaf's `TemplateEngine` registered as OSGi Service for direct use

Installation
------------

For running Sling Scripting Thymeleaf with Sling's Launchpad some dependencies need to be resolved. This can be achieved by installing the following bundle:

    mvn:org.javassist/javassist/3.20.0-GA

There is a feature for [Karaf](https://github.com/apache/sling/tree/trunk/contrib/launchpad/karaf):

    karaf@root()> feature:install sling-scripting-thymeleaf

relevant Thymeleaf issues
-------------------------

* [Create OSGi bundle](https://github.com/thymeleaf/thymeleaf/issues/32)
* [Remove initialize() steps in extension points](https://github.com/thymeleaf/thymeleaf/issues/54)
* [keep (custom) IContext accessible](https://github.com/thymeleaf/thymeleaf/issues/388)
