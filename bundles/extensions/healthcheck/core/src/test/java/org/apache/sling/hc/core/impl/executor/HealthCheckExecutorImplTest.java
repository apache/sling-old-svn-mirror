package org.apache.sling.hc.core.impl.executor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.sling.hc.api.HealthCheckResult;
import org.apache.sling.hc.api.Result;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class HealthCheckExecutorImplTest {

    private HealthCheckExecutorImpl healthCheckExecutorImpl;

    @Mock
    private HealthCheckFuture future;

    @Mock
    private HealthCheckDescriptor healthCheckDescriptor;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        when(future.getHealthCheckDescriptor()).thenReturn(healthCheckDescriptor);
        when(healthCheckDescriptor.getName()).thenReturn("Test Check");

        healthCheckExecutorImpl = new HealthCheckExecutorImpl();
        // 2 sec normal timeout
        healthCheckExecutorImpl.setTimeoutInMs(2000L);
        // 10 sec timeout for critical
        healthCheckExecutorImpl.setLongRunningFutureThresholdForRedMs(10000L);
    }

    @Test
    public void testCollectResultsFromFutures() throws Exception {

        List<HealthCheckFuture> futures = new LinkedList<HealthCheckFuture>();
        futures.add(future);
        Collection<HealthCheckResult> results = new TreeSet<HealthCheckResult>();

        when(future.isDone()).thenReturn(true);
        ExecutionResult testResult = new ExecutionResult(healthCheckDescriptor, new Result(Result.Status.OK, "test"), 10L);
        when(future.get()).thenReturn(testResult);

        healthCheckExecutorImpl.collectResultsFromFutures(futures, results);

        verify(future, times(1)).get();

        assertEquals(1, results.size());
        assertTrue(results.contains(testResult));

    }

    @Test
    public void testCollectResultsFromFuturesTimeout() throws Exception {

        List<HealthCheckFuture> futures = new LinkedList<HealthCheckFuture>();
        futures.add(future);
        Set<HealthCheckResult> results = new TreeSet<HealthCheckResult>();

        when(future.isDone()).thenReturn(false);
        when(future.getCreatedTime()).thenReturn(new Date());


        healthCheckExecutorImpl.collectResultsFromFutures(futures, results);

        verify(future, times(0)).get();

        assertEquals(1, results.size());
        HealthCheckResult result = results.iterator().next();

        assertEquals(Result.Status.WARN, result.getStatus());

    }

    @Test
    public void testCollectResultsFromFuturesCriticalTimeout() throws Exception {

        List<HealthCheckFuture> futures = new LinkedList<HealthCheckFuture>();
        futures.add(future);
        Set<HealthCheckResult> results = new TreeSet<HealthCheckResult>();

        when(future.isDone()).thenReturn(false);

        // use an old date now (simulating a future that has run for a min)
        when(future.getCreatedTime()).thenReturn(new Date(new Date().getTime() - 1000 * 60 * 60));

        healthCheckExecutorImpl.collectResultsFromFutures(futures, results);
        assertEquals(1, results.size());
        HealthCheckResult result = results.iterator().next();

        verify(future, times(0)).get();

        assertEquals(Result.Status.CRITICAL, result.getStatus());

    }

}
