package org.apache.sling.mailarchiveserver.api;

import java.util.Iterator;

import org.apache.sling.api.resource.Resource;

public interface SearchService {

	Iterator<Resource> find(String phrase);

}
