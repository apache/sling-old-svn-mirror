package org.apache.sling.models.it.services;

public class SimpleServiceWithCustomRanking implements SimpleService {

    private final int ranking;
    
    public SimpleServiceWithCustomRanking(int ranking) {
        this.ranking = ranking;
    }
    
    public int getRanking() {
        return ranking;
    }
}
