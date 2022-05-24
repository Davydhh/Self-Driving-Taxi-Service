package model;

import java.awt.*;
import java.util.Objects;

public class ChargingStation {
    private final int id;
    private final Point position;
    public ChargingStation(int id, Point position) {
        this.id = id;
        this.position = position;
    }

    public int getId() {
        return id;
    }

    public Point getPosition() {
        return position;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChargingStation that = (ChargingStation) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
