package org.apache.sling.reseditor.resource;

import java.util.Iterator;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceWrapper;

/**
 * This ResourceWrapper adds a marker to the ResourceMetadata object that the {@link ResourceProviderBasedResourceDecorator}
 * uses.
 */
public class ResourceTypeResourceWrapper extends ResourceWrapper {

	public ResourceTypeResourceWrapper(Resource resource) {
		super(resource);
	}

	@Override
	public ResourceMetadata getResourceMetadata() {
		ResourceMetadata newResourceMetadata = new ResourceMetadata();
		newResourceMetadata.putAll(getResource().getResourceMetadata());
		newResourceMetadata.put(ResEditorResourceProvider.RESOURCE_EDITOR_PROVIDER_RESOURCE, null);
		return newResourceMetadata;
	}

	@Override
    public Iterator<Resource> listChildren() {
		Iterator<Resource> originalIterator = this.getResource().listChildren();
		return new ResourceIteratorWrapper(originalIterator);
	}
}
