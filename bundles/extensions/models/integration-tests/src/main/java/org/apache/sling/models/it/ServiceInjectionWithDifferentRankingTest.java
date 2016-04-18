package org.apache.sling.models.it;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.junit.annotations.SlingAnnotationsTestRunner;
import org.apache.sling.junit.annotations.TestReference;
import org.apache.sling.models.factory.ModelFactory;
import org.apache.sling.models.it.models.ServiceInjectionTestModel;
import org.apache.sling.models.it.services.SimpleService;
import org.apache.sling.models.it.services.SimpleServiceWithCustomRanking;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.ServiceTracker;

@RunWith(SlingAnnotationsTestRunner.class)
public class ServiceInjectionWithDifferentRankingTest {

    @TestReference
    ConfigurationAdmin configAdmin;

    @TestReference
    private ResourceResolverFactory rrFactory;

    @TestReference
    private ServiceTracker serviceTracker;

    @TestReference
    private ModelFactory modelFactory;

    private String value;
    private ResourceResolver resolver;
    private Resource resource;
    private Node createdNode;
    private BundleContext bundleContext;
    private Collection<ServiceRegistration<SimpleService>> serviceRegistrations;

    @Before
    public void setUp() throws Exception {
        value = RandomStringUtils.randomAlphanumeric(10);

        resolver = rrFactory.getAdministrativeResourceResolver(null);
        Session session = resolver.adaptTo(Session.class);
        Node rootNode = session.getRootNode();
        createdNode = rootNode.addNode("test_" + RandomStringUtils.randomAlphanumeric(10));
        createdNode.setProperty("testProperty", value);
        session.save();

        resource = resolver.getResource(createdNode.getPath());

        bundleContext = FrameworkUtil.getBundle(SimpleServiceWithCustomRanking.class).getBundleContext();
        serviceRegistrations = new ArrayList<ServiceRegistration<SimpleService>>();
    }

    @After
    public void tearDown() throws Exception {
        if (createdNode != null) {
            createdNode.remove();
        }
        if (resolver != null) {
            resolver.close();
        }

        for (ServiceRegistration<?> serviceRegistration : serviceRegistrations) {
            serviceRegistration.unregister();
        }
    }

    private void registerSimpleService(int ranking) {
        Dictionary<String, Object> serviceProps = new Hashtable<String, Object>();
        serviceProps.put(Constants.SERVICE_RANKING, new Integer(ranking));
        ServiceRegistration<SimpleService> serviceRegistration = bundleContext.registerService(SimpleService.class,
                new SimpleServiceWithCustomRanking(ranking), serviceProps);
        serviceRegistrations.add(serviceRegistration);
    }

    @Test
    public void testServiceInjectionConsideringRankingWithResource() throws IOException {

        registerSimpleService(0);
        // cannot use adaptTo due to adaptersCache
        ServiceInjectionTestModel model = modelFactory.createModel(resource, ServiceInjectionTestModel.class);
        assertNotNull("Model is null", model);
        // only the default service with ranking 0 is known
        assertEquals("The service with the highest ranking was not returned", 0, model.getSimpleService().getRanking());
        assertArrayEquals("Order on injected services is wrong", model.getSimpleServicesRankings(), new Integer[] {0});

        registerSimpleService(-1000);
        model = modelFactory.createModel(resource, ServiceInjectionTestModel.class);
        assertNotNull("Model is null", model);
        // ranking 0 is still the highest one
        assertEquals("The service with the highest ranking was not returned", 0, model.getSimpleService().getRanking());
        assertArrayEquals("Order on injected services is wrong", model.getSimpleServicesRankings(), new Integer[] {0, -1000});

        registerSimpleService(1000);
        model = modelFactory.createModel(resource, ServiceInjectionTestModel.class);
        assertNotNull("Model is null", model);
        // now ranking 1000 is the highest
        assertEquals("The service with the highest ranking was not returned", 1000, model.getSimpleService().getRanking());
        assertArrayEquals("Order on injected services is wrong", model.getSimpleServicesRankings(), new Integer[] {1000, 0, -1000});

    }

}
