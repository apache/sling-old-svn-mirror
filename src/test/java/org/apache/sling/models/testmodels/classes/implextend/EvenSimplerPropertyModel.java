package org.apache.sling.models.testmodels.classes.implextend;

import javax.inject.Inject;

public class EvenSimplerPropertyModel {

    @Inject
    private String first;

    public String getFirst() {
        return first;
    }

}
