package org.apache.sling.testing.mock.sling.rrmock.loader;

import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.loader.AbstractContentLoaderAutoCommitTest;

public class ContentLoaderAutoCommitTest extends AbstractContentLoaderAutoCommitTest {

    @Override
    protected ResourceResolverType getResourceResolverType() {
        return ResourceResolverType.RESOURCERESOLVER_MOCK;
    }
}
