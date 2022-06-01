package rest.beans;

import javax.xml.bind.annotation.XmlRootElement;
import java.awt.*;
import java.util.Objects;

@XmlRootElement
public class RideRequest {
    private int id;

    private Point startPos;

    private Point endPos;

    public RideRequest() {};

    public RideRequest(int id, Point startPos, Point endPoint) {
        this.id = id;
        this.startPos = startPos;
        this.endPos = endPoint;
    }

    public int getId() {
        return id;
    }

    public Point getStartPos() {
        return startPos;
    }

    public Point getEndPos() {
        return endPos;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RideRequest that = (RideRequest) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "RideRequest{" +
                "id=" + id +
                '}';
    }
}
