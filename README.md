# Delegate Multi-Agent System for Pickup-and-Delivery
_Project for the [Multi-Agent Systems](https://onderwijsaanbod.kuleuven.be/syllabi/e/H02H4AE.htm) course at KU Leuven._


## Problem definition
Pizzeria chain RoboPizza wants to use AGVs to deliver pizzas to their customers. They contacted iDelivery to rent the AGVs for the delivery tasks. iDelivery is responsible for the charging of the robots whilst RoboPizza is responsible for handling incoming pizza delivery requests.

#### Environment
American style city
Optional: AGVs cannot pass each other in one street. This means that once an AGV moves in one direction, other AGVs will have to move in the same direction as well.

#### AGVs and their capabilities
AGVs can move from and to any position in the city. They can carry up to 5 pizzas at once. AGVs have maps and can compute paths between locations by themselves.

#### Characteristics of tasks
A task consists of picking up an amount of pizzas at RoboPizza before a certain time point and delivering it to a position before another time point.  
If there are more than 5 pizzas for a task, the task will have to be split up.

#### Scale
Large city

#### Communication constraints
AGVs can only communicate with other entities that lie within a certain area around them.

#### Dynamism
Amount of pickup locations can increase/decrease.
Streets can become one way streets or can become closed off because of sudden road works.  

#### Potential AGV crashes
Running out of battery, going into one way streets, ???

#### Measure for efficiency
Efficiency of the multi-agent system will be measured by the waiting time for customers. This number is to be minimized.

#### Random generation of requests
Tasks will be created at every time step with a low probability. The amount of pizzas for the tasks will be determined randomly using a Gaussian distribution. The time window for the delivery will be determined by the distance from the customer to RoboPizza multiplied by a constant factor and added to a random number.
The position of the request could be generated uniformly in the whole city, or located near a certain area.

#### Battery charging
Charging happens on one position. Only a limited amount of AGVs can charge at the charging station at the same time. (Maximum pizzas of 30% of the AGVs?)



## Algorithms
- Belief-Desire-Intention model
- Delegate Multi-Agent Systems



## Robot strategy
1. Ask tasks from pizzeria (only when at pizzeria)
2. Send desire ants to each task
3. Desire ant comes back with delivery task ID, score, pizzas needed 
4. Choose desire in order
5. Figure out amount of pizzas that the robot can send
6. Exploration + Confirmation of pizzaAmount (Give ant PizzaAmount)
=> Exploration will figure out which routes we can take
    1. DeliveryTask will confirm if it's possible to deliver this pizza amount
    => If we have multiple tasks in an ant we need to ask all these deliverytasks if we can make the delivery
    => Give exploration ant list of pairs <Destination, # pizzas>
7. Intention
8. Create pizzaParcel
9. Try to deliver parcel
    1. Send intention ants regularly to refresh the reservation
    2. If refresh OK => keep driving
    3. If refresh NOK => return to pizzeria



## Todos
* Check that bestTime < this.intendedArrivalTime works correctly
  * Use a map with high road works probability and check that a new better path is actually chosen when roadworks happen
* Desire ant evaluation logic
  * Improve scoring function and selection function
  * Reorder chosen desired so that the shortest path is followed from when the robot starts driving again:
    * From the point the robot starts driving, the shortest path will always be the best one w.r.t. decreasing total waiting time.
