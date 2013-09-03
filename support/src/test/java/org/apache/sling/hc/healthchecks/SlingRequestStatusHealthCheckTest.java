package org.apache.sling.hc.healthchecks;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.engine.SlingRequestProcessor;
import org.apache.sling.hc.api.Result;
import org.apache.sling.hc.support.impl.SlingRequestStatusHealthCheck;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

@RunWith(Parameterized.class)
public class SlingRequestStatusHealthCheckTest {
    private SlingRequestStatusHealthCheck hc;
    private final Result.Status expectedStatus;
    private final String [] paths;
    
    @Parameters(name="{0}")
    public static List<Object[]> data() {
            final List<Object[]> result = new ArrayList<Object[]>();
            result.add(new Object[] { "200.html:200,502.html:1234,436.json:436", Result.Status.WARN });
            
            result.add(new Object[] { "", Result.Status.OK }); 
            result.add(new Object[] { "200.x", Result.Status.OK }); 
            result.add(new Object[] { "404.html:404", Result.Status.OK }); 
            result.add(new Object[] { "200.html:200,502.html:502,436.json:436" , Result.Status.OK }); 
            result.add(new Object[] { "200.html:1234,502.html:502,436.json:436" , Result.Status.WARN }); 
            result.add(new Object[] { "200.html:200,502.html:1234,436.json:436" , Result.Status.WARN }); 
            result.add(new Object[] { "200.html:200,502.html:502,436.json:1234" , Result.Status.WARN }); 
            result.add(new Object[] { "200.html:1234,502.html:1234,436.json:1234" , Result.Status.WARN }); 
            return result;
    }
    
    @Before
    public void setup() throws Exception {
        hc = new SlingRequestStatusHealthCheck();
        
        final ResourceResolverFactory rrf = Mockito.mock(ResourceResolverFactory.class);
        SetField.set(hc, "resolverFactory", rrf);
        
        final Answer<Void> a = new Answer<Void> () {
            @Override
            public Void answer(InvocationOnMock invocation) {
                final HttpServletRequest request = (HttpServletRequest)invocation.getArguments()[0];
                final HttpServletResponse response = (HttpServletResponse)invocation.getArguments()[1];
                final String path = request.getPathInfo();
                if(path.length() > 0) {
                    final String status = path.substring(0, path.indexOf('.'));
                    response.setStatus(Integer.valueOf(status));
                }
                return null;
            }
            
        };
        
        final SlingRequestProcessor srp = Mockito.mock(SlingRequestProcessor.class);
        SetField.set(hc, "requestProcessor", srp);
        Mockito.doAnswer(a).when(srp).processRequest(
                Matchers.any(HttpServletRequest.class),  
                Matchers.any(HttpServletResponse.class), 
                Matchers.any(ResourceResolver.class));
        
        
        final Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("path", paths);
        hc.activate(properties);
    }
    
    public SlingRequestStatusHealthCheckTest(String paths, Result.Status expectedStatus) {
        this.paths = paths.split(",");
        this.expectedStatus = expectedStatus;
        
    }
    
    @Test
    public void testResult() {
        assertEquals("Expecting result " + expectedStatus + " for paths=" + paths,
                expectedStatus, hc.execute().getStatus());
    }
}
