Apache Sling Commons Messaging Mail
===================================

Provide an OSGi Configuration for `SimpleMailBuilder` or a custom `MailBuilder` to send messages using [Apache Commons Email](https://commons.apache.org/proper/commons-email/).

To extend or override `SimpleMailBuilder`​s configuration call `MessageService#send(String, String, Map):Future<Result>` and supply a configuration map `mail` within the third parameter:

```
{
  "mail" : {
    "mail.subject": <String>,
    "mail.from": <String>,
    "mail.smtp.hostname": <String>,
    "mail.smtp.port": <int>,
    "mail.smtp.username": <String>,
    "mail.smtp.password": <String>
  }
}
```
