package rest.beans;


import java.awt.*;
import java.util.List;

public class RegistrationResponse {
    private final Point starPos;

    private final List<Taxi> taxis;

    public RegistrationResponse(Point starPos, List<Taxi> taxis) {
        this.starPos = starPos;
        this.taxis = taxis;
    }

    public Point getStarPos() {
        return starPos;
    }

    public List<Taxi> getTaxis() {
        return taxis;
    }
}
