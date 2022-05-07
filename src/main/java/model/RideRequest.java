package model;

import java.awt.*;

public class RideRequest {
    private final int id;

    private final Point startPos;

    private final Point endPoint;

    public RideRequest(int id, Point startPos, Point endPoint) {
        this.id = id;
        this.startPos = startPos;
        this.endPoint = endPoint;
    }
}
