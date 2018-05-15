package mas;

public class RoutingTableValue {
    private Integer time;
    private Double distance;

    public RoutingTableValue(Integer time, Double distance){
        this.time = time;
        this.distance = distance;
    }

    public Integer getTime() {
        return time;
    }

    public Double getDistance() {
        return distance;
    }

    public void decreaseTime(){
        this.time -= 1;
    }

    public boolean timeUp(){
        return this.time <= 0;
    }
}
