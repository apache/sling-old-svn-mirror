package org.apache.sling.testing.mock.sling.jcrmock.loader;

import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.loader.AbstractContentLoaderAutoCommitTest;

public class ContentLoaderAutoCommitTest extends AbstractContentLoaderAutoCommitTest {

    @Override protected ResourceResolverType getResourceResolverType() {
        return ResourceResolverType.JCR_MOCK;
    }
}
