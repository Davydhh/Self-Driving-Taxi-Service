package rest.beans;


import javax.xml.bind.annotation.XmlRootElement;
import java.awt.*;
import java.util.List;

@XmlRootElement
public class RegistrationResponse {
    private Point starPos;

    private List<TaxiBean> taxis;

    public RegistrationResponse() {}

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

    @Override
    public String toString() {
        return "RegistrationResponse{" +
                "starPos=" + starPos +
                ", taxis=" + taxis +
                '}';
    }
}
