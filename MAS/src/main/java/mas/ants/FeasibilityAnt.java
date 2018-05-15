package mas.ants;

import com.github.rinde.rinsim.core.model.comm.MessageContents;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
/*
 * IMPORTANT: THE ANTS ARE MESSAGES THIS MEANS THEY SHOULD IMPLEMENT {@link messagecontents}
 *            THE ANTS SHOULD BE IMMUTABLE TOO, THIS MEANS THAT EVERYTIME YOU WANT TO SEND AN ANT TO A NEW DEVICE
 *            YOU NEED TO COPY THE DATA OF THE OLD ANT AND CREATE A NEW ONE
 */
public class FeasibilityAnt implements MessageContents {


}
