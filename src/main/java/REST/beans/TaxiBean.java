package rest.beans;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Objects;

@XmlRootElement
public class TaxiBean {
    private int id;

    private int port;

    private String ip;

    public TaxiBean() {}

    public TaxiBean(int id, int port, String ip) {
        this.id = id;
        this.port = port;
        this.ip = ip;
    }

    public int getId() {
        return id;
    }

    public int getPort() {
        return port;
    }

    public String getIp() {
        return ip;
    }

    @Override
    public String toString() {
        return "TaxiBean{" +
                "id=" + id +
                ", port=" + port +
                ", ip='" + ip + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TaxiBean taxiBean = (TaxiBean) o;
        return id == taxiBean.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}