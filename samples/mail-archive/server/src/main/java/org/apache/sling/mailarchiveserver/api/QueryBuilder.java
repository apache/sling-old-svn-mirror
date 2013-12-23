package org.apache.sling.mailarchiveserver.api;

import java.util.List;
import java.util.Map;

public interface QueryBuilder {

	String buildQuery(Map<String, List<String>> tokens, String lang);
	
}
