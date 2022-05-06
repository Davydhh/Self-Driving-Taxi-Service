package model;

public class RideRequest {
    private final String id;

    private final int[][] startPos;

    private final int[][] destPos;

    public RideRequest(String id, int[][] startPos, int[][] destPos) {
        this.id = id;
        this.startPos = startPos;
        this.destPos = destPos;
    }
}
