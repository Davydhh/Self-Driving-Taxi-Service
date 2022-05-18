package rest.beans;

import javax.xml.bind.annotation.XmlRootElement;
import java.awt.*;

@XmlRootElement
public class RideRequest {
    private int id;

    private Point startPos;

    private Point endPoint;

    public RideRequest() {};

    public RideRequest(int id, Point startPos, Point endPoint) {
        this.id = id;
        this.startPos = startPos;
        this.endPoint = endPoint;
    }

    public int getId() {
        return id;
    }

    public Point getStartPos() {
        return startPos;
    }

    public Point getEndPoint() {
        return endPoint;
    }
}
