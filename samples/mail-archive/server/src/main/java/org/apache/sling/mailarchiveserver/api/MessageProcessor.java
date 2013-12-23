package org.apache.sling.mailarchiveserver.api;

import org.apache.james.mime4j.dom.Message;

/** MessageProcessors are used by our {@link MessageStore}
 *  implementation to pre-process messages before storing them.
 */
public interface MessageProcessor {
	void processMessage(Message m);
}
