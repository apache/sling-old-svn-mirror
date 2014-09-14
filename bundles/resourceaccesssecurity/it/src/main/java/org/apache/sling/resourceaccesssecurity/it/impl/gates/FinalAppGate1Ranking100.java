package org.apache.sling.resourceaccesssecurity.it.impl.gates;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.resourceaccesssecurity.ResourceAccessGate;
import org.osgi.framework.Constants;

@Component
@Service(value=ResourceAccessGate.class)
@Properties({
        @Property(name=ResourceAccessGate.PATH, label="Path", value="/test/(un|)secured-provider/read(-update|)/(app|mixed)/.*", 
                description="The path is a regular expression for which resources the service should be called"),
        @Property(name=ResourceAccessGate.FINALOPERATIONS, value="read,update"),
        @Property(name=ResourceAccessGate.CONTEXT, value=ResourceAccessGate.APPLICATION_CONTEXT),
        @Property(name = Constants.SERVICE_RANKING, intValue = 100, propertyPrivate = false)
})
public class FinalAppGate1Ranking100 extends AResourceAccessGate {

    public static String GATE_ID = "finalappgate1ranking100";

    @Override
    protected String getGateId() {
        return GATE_ID;
    }

}
