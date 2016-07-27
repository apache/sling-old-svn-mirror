package org.apache.sling.jobs.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.sling.crankstart.junit.CrankstartSetup;
import org.junit.ClassRule;
import org.junit.rules.ExternalResource;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Basic tests of the launcher, verify that we 
 *  can start the Felix HTTP service and a few
 *  other things. 
 */
@RunWith(Suite.class)
@Suite.SuiteClasses(CheckRootIT.class)
public class TestSuiteLauncherIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestSuiteLauncherIT.class);
    private static final long classSetup = System.currentTimeMillis();
    private static final long MAX_BACKOFF = 5000;
    private static final long INITIAL_BACKOFF = 100;

    @ClassRule
    public final static CrankstartSetup crankstartSetup = new CrankstartSetup().withModelResources(Models.DEFAULT_MODELS);


    @ClassRule
    public final static ExternalResource R = new ExternalResource() {
        @Override
        protected void before() throws Throwable {
            LOGGER.info("Waiting for Crankstart to start");
            while(crankstartSetup.getTotalBundles() == 0) {
                Thread.sleep(100);
            }
            if (crankstartSetup.getBundlesFailed() > 0) {
                LOGGER.error("Bundles failed to start {} ",crankstartSetup.getBundlesFailed());
                fail();
            }
            LOGGER.info("Crankstart to started {} bundles ", crankstartSetup.getBundlesStarted());
            DefaultHttpClient client = new DefaultHttpClient();
            final HttpUriRequest get = new HttpGet(crankstartSetup.getBaseUrl());
            HttpResponse response = null;
            long backoff = INITIAL_BACKOFF;
            boolean noresponse = false;
            long ttl = System.currentTimeMillis()+60000;
            // try for no more than 60s
            while(System.currentTimeMillis() < ttl) {
                try {
                    response = client.execute(get);
                    if (noresponse) {
                        backoff = INITIAL_BACKOFF;
                        noresponse = false;
                    }
                    if (response.getStatusLine().getStatusCode() == 404) {
                        LOGGER.info("Sever up and responding to http requests, startup complete");
                        Thread.sleep(60000);
                        return;
                    }
                    LOGGER.info("Server not respoding as expected to http requests, waiting {} ms", backoff);
                    Thread.sleep(backoff);
                    backoff = Math.min((long)(backoff*1.5), MAX_BACKOFF);
                } catch (Exception ex) {
                    noresponse = true;
                    LOGGER.info("Server not respoding waiting {} ms {} ", backoff, ex.getClass());
                    Thread.sleep(backoff);
                    backoff = Math.min(backoff*2, MAX_BACKOFF);
                } finally {
                    Models.closeConnection(response);
                }

            }
            fail("Unable to contact server");
        }

    };


}