Apache Sling URL Rewriter
=========================

multi-purpose service for altering HTTP requests/responses based on Tuckey's UrlRewriteFilter

* http://tuckey.org/urlrewrite/manual/4.0/guide.html
* http://urlrewritefilter.googlecode.com/svn/trunk/src/doc/manual/4.0/index.html

example for setting a Cache-Control header:

    <?xml version="1.0" encoding="utf-8"?>
    <!DOCTYPE urlrewrite PUBLIC "-//tuckey.org//DTD UrlRewrite 4.0//EN" "http://www.tuckey.org/res/dtds/urlrewrite4.0.dtd">
    <urlrewrite>
      <rule>
        <from>.*</from>
        <set type="response-header" name="Cache-Control">max-age=600</set>
      </rule>
    </urlrewrite>

