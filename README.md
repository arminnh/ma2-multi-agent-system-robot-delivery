# Delegate Multi-Agent System for Pickup-and-Delivery
_Project for the [Multi-Agent Systems](https://onderwijsaanbod.kuleuven.be/syllabi/e/H02H4AE.htm) course at KU Leuven._


## Problem definition
Pizzeria chain RoboPizza wants to use AGVs to deliver pizzas to their customers. They contacted iDelivery to rent the AGVs for the delivery tasks. iDelivery is responsible for the charging of the robots whilst RoboPizza is responsible for handling incoming pizza delivery requests.

#### Environment
* Grid city system.
* AGVs can go in both directions on streets and can pass each other.
* Dynamism: road works can cause streets to be closed off.

#### AGVs and their capabilities
* Can move from and to any position in the city.
* Have static maps (without road works) of the city and can compute paths between locations by themselves.
* Can carry up to 5 pizzas at once. 
* Can only communicate to entities that are within a certain area around them (i.e. the node in the city they are on).
* Run on batteries which need to be recharged
  * Charging happens at a charging station which only supports a limited amount of robots to charge simultaneously.
* Potential crashes: 
  * Can run out of battery. In this case, the battery is reset after a time to simulate that someone went out to replace the battery manually.

#### Delivery tasks
* Consist of picking up an amount of pizzas at RoboPizza and delivering them to a destination in the city.
* If there are more than 5 pizzas for a task, the task has to be split up.
* Pizzas have no preparation time and can be picked up instantly when a robot is at a pizzeria

#### Measure of efficiency
Efficiency of the multi-agent system is measured by the waiting time for customers. This number is to be minimized.

#### Random generation of delivery tasks
Tasks are created at every simulation tick with a low probability which grows with city size. The amount of pizzas for the tasks are determined by a Gaussian distribution with mean 4 and standard deviation 0.75. The position of the task is generated uniformly randomly throughout the city.



## Algorithms
* Belief-Desire-Intention model
* Delegate Multi-Agent Systems



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
