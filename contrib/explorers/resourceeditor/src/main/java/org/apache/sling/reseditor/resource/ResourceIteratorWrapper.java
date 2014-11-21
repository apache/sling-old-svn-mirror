package org.apache.sling.reseditor.resource;
import java.util.Iterator;

import org.apache.sling.api.resource.Resource;

/**
 * It wraps the resources with the {@link ResourceTypeResourceWrapper} on 
 * {@code next()}.
 */
public class ResourceIteratorWrapper implements Iterator<Resource>{

	private Iterator<Resource> iterator;

	public ResourceIteratorWrapper(Iterator<Resource> iterator){
		this.iterator = iterator;
	}
	
	public boolean hasNext() {
		return iterator.hasNext();
	}

	public Resource next() {
		return new ResourceTypeResourceWrapper(iterator.next());
	}

	public void remove() {
		iterator.remove();
	}

}
