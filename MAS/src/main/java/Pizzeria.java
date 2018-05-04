import com.github.rinde.rinsim.core.model.pdp.Depot;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.geom.Point;

/**
 * Created by thierryderuyttere on 4/05/18.
 * Implementation of the Pizzeria in our PizzaWorld
 */
public class Pizzeria extends Depot {

    private Point location;
    Pizzeria(Point position, double capacity) {
        super(position);
        setCapacity(capacity);
        this.location = position;
    }

    public Point getLocation(){
        return this.location;
    }

    @Override
    public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {}

}
