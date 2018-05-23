package mas.messages;

import com.github.rinde.rinsim.core.model.comm.CommUser;
import com.github.rinde.rinsim.core.model.comm.MessageContents;
import com.github.rinde.rinsim.geom.Point;
import mas.tasks.DeliveryTask;
import mas.tasks.PizzaParcel;

import javax.annotation.Nullable;
import java.util.List;

/*
 * IMPORTANT: THE ANTS ARE MESSAGES THIS MEANS THEY SHOULD IMPLEMENT {@link messagecontents}
 *            THE ANTS SHOULD BE IMMUTABLE TOO, THIS MEANS THAT EVERYTIME YOU WANT TO SEND AN ANT TO A NEW DEVICE
 *            YOU NEED TO COPY THE DATA OF THE OLD ANT AND CREATE A NEW ONE
 */
public class IntentionAnt extends Ant {

    public PizzaParcel parcel;
    public boolean toChargingStation = false;
    public boolean toDeliveryTask = false;

    public IntentionAnt(List<Point> path, long estimatedTime, boolean isReturning, int id,
                        Integer robotID, CommUser robot, @Nullable PizzaParcel parcel){
        super(path, estimatedTime, isReturning, id, robotID, robot);
        this.parcel = parcel;
        if(parcel == null){
            toChargingStation = true;
        }else{
            toDeliveryTask = true;
        }
    }


}
