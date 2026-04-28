package jp.apple.arad.data;

import java.util.Collections;
import java.util.List;

public final class RouteSnapshot {

    public final String       id;
    public final String       name;
    public final List<String> stationIds;
    public final int          trainCount;

    public RouteSnapshot(String id, String name, List<String> stationIds, int trainCount) {
        this.id         = id;
        this.name       = name;
        this.stationIds = Collections.unmodifiableList(stationIds);
        this.trainCount = trainCount;
    }
}