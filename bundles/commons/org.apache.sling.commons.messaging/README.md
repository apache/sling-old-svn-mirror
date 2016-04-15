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
