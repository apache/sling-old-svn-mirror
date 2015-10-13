package org.apache.sling.tooling.lc;

import static org.apache.sling.tooling.lc.Artifacts.launchpadCoordinates;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class ArtifactsTest {
    
    @Test
    public void launchpadV7() {
        
        assertThat(launchpadCoordinates("7"), equalTo("org.apache.sling:org.apache.sling.launchpad:xml:bundlelist:7"));
    }
    
    @Test
    public void launchpadV7Snapshot() {
        
        assertThat(launchpadCoordinates("7-SNAPSHOT"), equalTo("org.apache.sling:org.apache.sling.launchpad:xml:bundlelist:7-SNAPSHOT"));
    }

    @Test
    public void launchpadV8() {
        
        assertThat(launchpadCoordinates("8"), equalTo("org.apache.sling:org.apache.sling.launchpad:txt:slingfeature:8"));
    }
    
    @Test
    public void launchpadV8Snapshot() {
        
        assertThat(launchpadCoordinates("8-SNAPSHOT"), equalTo("org.apache.sling:org.apache.sling.launchpad:txt:slingfeature:8-SNAPSHOT"));
    }

}
