package org.apache.sling.mailarchiveserver.api;

public interface ThreadKeyGenerator {

	String getThreadKey(String subject);

}
