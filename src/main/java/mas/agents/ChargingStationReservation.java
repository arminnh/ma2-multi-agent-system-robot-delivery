package mas.agents;

public class ChargingStationReservation {
    public final int robotID;
    public final long evaporationTimestamp;

    public ChargingStationReservation(int robotID, long evaporationTimestamp) {
        this.robotID = robotID;
        this.evaporationTimestamp = evaporationTimestamp;
    }

    public ChargingStationReservation copy(long evaporationTimestamp) {
        return new ChargingStationReservation(this.robotID, evaporationTimestamp);
    }
}
