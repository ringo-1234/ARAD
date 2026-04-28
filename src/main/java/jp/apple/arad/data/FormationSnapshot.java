package jp.apple.arad.data;

import java.util.Collections;
import java.util.List;

public final class FormationSnapshot {

    public final String id;
    public final float speed;
    public final int notch;

    public final List<float[]> cars;

    public FormationSnapshot(String id, float speed, int notch, List<float[]> cars) {
        this.id = id;
        this.speed = speed;
        this.notch = notch;
        this.cars = Collections.unmodifiableList(cars);
    }

    public int carCount() {
        return cars.size();
    }

    public String speedLabel() {
        return String.format("%.0fkm/h", Math.abs(speed) * 72f);
    }

    public String idLabel() {
        return id;
    }
}