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

example for setting CORS headers:

    <?xml version="1.0" encoding="UTF-8"?>
    <!DOCTYPE urlrewrite PUBLIC "-//tuckey.org//DTD UrlRewrite 4.0//EN" "http://www.tuckey.org/res/dtds/urlrewrite4.0.dtd">
    <urlrewrite>
      <rule>
        <note>
          http://www.w3.org/TR/cors/
          https://developer.mozilla.org/en-US/docs/HTTP/Access_control_CORS
          http://fetch.spec.whatwg.org
          http://enable-cors.org
          http://www.html5rocks.com/en/tutorials/cors/
        </note>
        <condition type="header" name="Origin">.*</condition>
        <condition type="header" name="Access-Control-Request-Method">.*</condition>
        <condition type="header" name="Access-Control-Request-Headers">.*</condition>
        <set type="response-header" name="Access-Control-Allow-Origin">%{header:Origin}</set>
        <set type="response-header" name="Access-Control-Allow-Methods">%{header:Access-Control-Request-Method}</set>
        <set type="response-header" name="Access-Control-Allow-Headers">%{header:Access-Control-Request-Headers}</set>
        <set type="response-header" name="Access-Control-Allow-Credentials">true</set>
      </rule>
    </urlrewrite>
