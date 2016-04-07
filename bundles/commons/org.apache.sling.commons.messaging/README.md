Apache Sling Commons Messaging
==============================

Simple API for sending *message*​s to *recipient*​s.

`MessageService`
----------------
  * `send(String, String)` - takes a *message*​ and a *recipient*, e.g.
    * send("A Message to You, Rudy", "rudy@ghosttown") - send a mail to Rudy in Ghost Town
    * send("Hello Apache!", "+1.919.573.9199") - send a fax to the ASF
  * `send(String, String, Map)` - takes a *message*, a *recipient* and additional *data* useful for the underlying implementation to process and/or send the message

`Result<T>`
-----------
  * `getMessage():T` - should return a serialized form of the sent *message*
  * `hasFailures():boolean` - should return `true` in case of failures, `false` otherwise
  * `getFailures():Collection<Failure>` - should return the failures occurred when processing or sending the message

`Failure`
---------
 * `getCode():String` - should return a failure code when available, e.g. an [SMTP Status Code](https://tools.ietf.org/html/rfc5248)
 * `getType():String` - should return a failure type when available, e.g. invalid input (message too big) or transport failure (host unavailable)
 * `getMessage():String` - should return a human readable failure message
