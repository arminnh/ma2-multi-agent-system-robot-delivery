package mas.managers;

import com.github.rinde.rinsim.core.model.comm.*;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import mas.RoutingTable;
import mas.ants.ExplorationAnt;
import org.apache.commons.math3.random.RandomGenerator;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ResourceManager implements CommUser, RoadUser, TickListener {

    private RoadModel roadModel;
    private Point position;
    private boolean first_tick = true;
    Optional<CommDevice> device;
    private List<CommUser> neighbors;
    private RoutingTable rTable;
    private Map<Integer, Map<Integer, Boolean>> passedAnt;
    private RandomGenerator rng;


    public ResourceManager(Point position, RandomGenerator rng){
        this.position = position;
        this.device = Optional.absent();
        neighbors = new LinkedList<>();
        rTable = new RoutingTable(1000);
        passedAnt = new HashMap<>();
        this.rng = rng;
    }

    @Override
    public Optional<Point> getPosition() {
        return Optional.of(this.position);
    }

    @Override
    public void setCommDevice(CommDeviceBuilder builder) {
        builder.setMaxRange(2);
        this.device = Optional.of(builder.build());
    }

    @Override
    public void initRoadUser(RoadModel model) {
        roadModel = model;
        roadModel.addObjectAt(this, this.position);
    }

    @Override
    public void tick(TimeLapse timeLapse) {
        if(first_tick){
            first_tick = false;
            this.device.get().broadcast(Messages.NICE_TO_MEET_YOU);
        }

        readMessages();

        rTable.decreaseLifeTime();
    }

    private void readMessages() {
        /*if(this.device.get().getUnreadCount() > 0){
            System.out.print("RM at: ");
            System.out.print(this.position);
            System.out.print(" has ");
            System.out.print(this.device.get().getUnreadCount());
            System.out.println(" messages ");
        }*/
        for(Message m: this.device.get().getUnreadMessages()){
            //System.out.print("Message from: ");
            //System.out.println(m.getSender().getPosition());
            if(m.getContents() == Messages.NICE_TO_MEET_YOU) {
                neighbors.add(m.getSender());
            }else if(m.getContents().getClass() == ExplorationAnt.class){
                ExplorationAntLogic(m);
            }
        }
    }

    private void ExplorationAntLogic(Message m) {
        ExplorationAnt ant = (ExplorationAnt) m.getContents();

        if(!passedAnt.containsKey(ant.getRobot_id())){
            passedAnt.put(ant.getRobot_id(), new HashMap<>());
        }

        /*if(passedAnt.get(ant.getRobot_id()).containsKey(ant.getId()) && !ant.hasReachedDestination()){
            return;
        }else{
            passedAnt.get(ant.getRobot_id()).put(ant.getId(), true);
        }*/


        // check if current position is destination
        // send message back
        if(ant.hasReachedDestination()){

            List<Point> return_path = ant.getReturning_path().subList(1, ant.getReturning_path().size());

            ExplorationAnt new_ant = new ExplorationAnt(
                    ant.getPath(), ant.getRobot_id(),
                    ant.getDestination(), ant.getId(), return_path, ant.getDistance() + 1, ant.getRobotComm());

            if(this.position != ant.getRobotComm().getPosition().get()){
                for(CommUser neighbor: neighbors) {
                    if(neighbor.getPosition().get() == return_path.get(0)){
                        this.device.get().send(new_ant, neighbor);
                    }
                }

                int currentIndex = ant.getPath().indexOf(this.getPosition().get());

                // For every node starting from our current position
                // We add it to our table
                for(int i=ant.getPath().size()-1; i>currentIndex+1; i--){
                    if(!rTable.hasHopForThrough(ant.getPath().get(i), ant.getPath().get(currentIndex+1))){
                        double distance = 0;//roadModel.getDistanceOfPath(ant.getPath().subList(currentIndex, i)).getValue();
                        rTable.addHop(ant.getPath().get(i), ant.getPath().get(currentIndex+1), distance);
                    }
                }
            }else{
                this.device.get().send(new_ant, ant.getRobotComm());
            }
        }else{
            List<Point> new_path = new LinkedList<>(ant.getPath());
            new_path.add(this.getPosition().get());

            // We can reach our destination
            /*if(this.rTable.canReach(ant.getDestination())){
                //List<Point> return_path = new LinkedList<>(ant.getPath());
                //return_path = Lists.reverse(return_path);


                for(CommUser neighbor: neighbors){
                    // Check if our neighbor is already visited
                    if(neighbor.getPosition().get() == rTable.getNextHop(ant.getDestination())){
                                //new_path, ant.getRobot_id(), ant.getDestination(), ant.getId(), ant.getRobotComm());
                        //ExplorationAnt new_ant = new ExplorationAnt(new_path, ant.getRobot_id(),
                        //        ant.getDestination(), ant.getId(), return_path,
                        //        rTable.getDistanceFor(ant.getDestination()), ant.getRobotComm());
                        ExplorationAnt new_ant = new ExplorationAnt(
                                new_path, ant.getRobot_id(), ant.getDestination(), ant.getId(), ant.getRobotComm());

                        this.device.get().send(new_ant,neighbor);
                        break;
                    }
                }

                //this.device.get().send(new_ant, m.getSender());

            }else{*/
                if(this.getPosition().get() == ant.getDestination()){

                    List<Point> return_path = new LinkedList<>(ant.getPath());
                    return_path = Lists.reverse(return_path);
                    System.out.println(new_path);
                    ExplorationAnt new_ant = new ExplorationAnt(new_path, ant.getRobot_id(),
                            ant.getDestination(), ant.getId(), return_path, roadModel.getDistanceOfPath(new_path).getValue(),
                            ant.getRobotComm());

                    this.device.get().send(new_ant, m.getSender());
                }else{

                    // destination not reached yet
                    for(CommUser neighbor: neighbors){
                        // Check if our neighbor is already visited
                        if(!ant.getPath().contains(neighbor.getPosition().get())){
                            ExplorationAnt new_ant = new ExplorationAnt(
                                    new_path, ant.getRobot_id(), ant.getDestination(), ant.getId(), ant.getRobotComm());

                            if(!rTable.hasHopForThrough(ant.getDestination(), neighbor.getPosition().get())){
                                if(rng.nextDouble() > 0.5){
                                    this.device.get().send(new_ant,neighbor);
                                }
                            }
                        }
                    }
                }
            //}
        }
    }

    @Override
    public void afterTick(TimeLapse timeLapse) { }

    enum Messages implements MessageContents {
        NICE_TO_MEET_YOU
    }
}
