package org.apache.sling.models.it.models;

import java.util.ArrayList;
import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.it.services.SimpleService;

@Model(adaptables=Resource.class)
public class ServiceInjectionTestModel {

    @OSGiService
    private SimpleService simpleService; // must return the service impl with the highest ranking

    @OSGiService
    private List<SimpleService> simpleServices;
    
    public SimpleService getSimpleService() {
        return simpleService;
    }

    public Integer[] getSimpleServicesRankings() {
        List<Integer> serviceRankings = new ArrayList<Integer>();
        for (SimpleService service : simpleServices) {
            serviceRankings.add(service.getRanking());
        }
        return serviceRankings.toArray(new Integer[serviceRankings.size()]);
    }
}
