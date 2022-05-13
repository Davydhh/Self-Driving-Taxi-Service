package rest.beans;


import java.awt.*;
import java.util.List;

public class RegistrationResponse {
    private final Point starPos;

    private final List<TaxiBean> taxis;

    public RegistrationResponse(Point starPos, List<TaxiBean> taxis) {
        this.starPos = starPos;
        this.taxis = taxis;
    }

    public Point getStarPos() {
        return starPos;
    }

    public List<TaxiBean> getTaxis() {
        return taxis;
    }
}
