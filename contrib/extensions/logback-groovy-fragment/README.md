Logback Groovy Fragment
=======================

This fragment is required to make use of Groovy based event evaluation support 
provided by Logback. This enables programatic filtering of the log messages and
is useful to get desired logs without flooding the system. For example Oak
logs the JCR operations being performed via a particular session. if this lo is 
enabled it would flood the log with messages from all the active session. However
if you need logging only from session created in a particular thread then that 
can be done in following way

    <?xml version="1.0" encoding="UTF-8"?>
    <configuration scan="true" scanPeriod="1 second">
      <jmxConfigurator/>
      <newRule pattern="*/configuration/osgi" actionClass="org.apache.sling.commons.log.logback.OsgiAction"/>
      <newRule pattern="*/configuration/appender-ref-osgi" actionClass="org.apache.sling.commons.log.logback.OsgiAppenderRefAction"/>
      <osgi/>
    
       <appender name="OAK" class="ch.qos.logback.core.FileAppender">
        <filter class="ch.qos.logback.core.filter.EvaluatorFilter">      
          <evaluator class="ch.qos.logback.classic.boolex.GEventEvaluator"> 
            <expression><![CDATA[
                return e.getThreadName().contains("JobHandler");
            ]]></expression>
          </evaluator>
          <OnMismatch>DENY</OnMismatch>
          <OnMatch>ACCEPT</OnMatch>
        </filter>
        <file>${sling.home}/logs/oak.log</file>
        <encoder>
          <pattern>%d %-5level [%thread] %marker- %msg %n</pattern> 
          <immediateFlush>true</immediateFlush>
        </encoder>
      </appender>
    
      <logger name="org.apache.jackrabbit.oak.jcr.operations" level="DEBUG" additivity="false">
          <appender-ref ref="OAK"/>
      </logger>
    </configuration>
    
Logback exposes a variable `e` which is of type [ILoggingEvent][1]. It provides access to current logging
event. Above logback config would route all log messages from `org.apache.jackrabbit.oak.jcr.operations`
category to `${sling.home}/logs/oak.log`. Further only those log messages would be logged
where the `threadName` contains `JobHandler`. Depending on the requirement the expression can
be customised.

[1]: http://logback.qos.ch/apidocs/ch/qos/logback/classic/spi/ILoggingEvent.html