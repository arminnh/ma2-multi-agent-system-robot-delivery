package mas;


import com.github.rinde.rinsim.geom.Point;

public class DeliveryTaskData {
    public final Point location;
    public final Integer id;
    public final Integer capacity;

    public DeliveryTaskData(Point location, Integer id, Integer capacity){
        this.location = location;
        this.id = id;
        this.capacity = capacity;
    }
}
