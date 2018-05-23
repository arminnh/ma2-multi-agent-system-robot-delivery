# Notes on relevant papers
    
### Delegate MAS Patterns for Large-Scale Distributed Coordination and Control Applications
    TOM HOLVOET, DANNY WEYNS, PAUL VALCKENAERS
    https://lirias.kuleuven.be/bitstream/123456789/355841/1/acm.pdf
* ...


### Exploiting the Environment for Coordinating Agent Intentions
    Tom Holvoet and Paul Valckenaers
    https://pdfs.semanticscholar.org/84a6/0ebc03ba868756a1d941ff0a322694e05ab2.pdf
* Approach to BDI agents which alleviates agent complexity through "delegate MAS"
* Light-weight agents issued by resources (for building and maintaining information on the environment) or by task agents (explore the options on behalf of agents and coordinate their intentions)
* ...
    
    
### Delegate MAS for Large Scale and Dynamic PDP: A Case Study
    Shaza Hanif, Rinde R.S. van Lon, Ning Gui, and Tom Holvoet
    https://pdfs.semanticscholar.org/4a42/b57e6c1b88c526228f3ca293b9a6249fc9ec.pdf
* ...


### Multi-Agent Route Planning Using Delegate MAS
    Hoang Tung Dinh, Rinde R. S. van Lon, Tom Holvoet
    https://lirias.kuleuven.be/bitstream/123456789/541645/1/DMAP.pdf
* Dynamic, large scale, continuous planning with communication limitations and possible deadlock situations
* Decentralized Delegate MAS solution inspired by ant behavior
* Applied to AGV routing under realistic conditions
* Transportation requests assigned to vehicles. Each request has pickup and delivery locations.
* Vehicles need to go to battery charging stations
* Want to minimize transportation time and maximize throughput
* Roads may be blocked, vehicles may temporarily fail, new vehicles may become available and operating ones may leave
* Single-stage routing: agent has exactly one destination
* Multi-stage routing: agent has a sequence of destinations
* Context-aware routing: agent finds optimal plan that does not create a conflict with existing plans of other agents on a free time window graph using an A*-like algorithm.
* Infrastructure is bidirectional graph G = (V, E)
* Edges have some pizzas C(e)
* If multiple agents occupy an edge at the same time, they must all travel in the same direction
* 2 types of agents: resource agents and vehicle agents
* Resource agents:
  * can observe changes such as unexpected objects at its resource
  * manage a schedule on its resource and provide free time windows according to the current schedule
  * only allows a vehicle to enter if it is consistent with the schedule
  * only accepts reservations that are consistent with its existing schedule
  * reservations have time-to-live and should be confirmed regularly
  * can communicate with neighboring resource agents
* Vehicle agents:
  * represent an operating vehicle
  * know the static graph structure of the infrastructure, but not the schedules on the resources
  * responsible for planning routes and controlling vehicle towards destinations
  * continually explores alternative routes and reserves its intended route
  * a route is a path with a schedule
  * **BEHAVIOR DESCRIBED IN ALGORITHM 1**
    * generates set of feasible paths from current position to destination using STATIC INFRASTRUCTURE GRAPH
    * evaluates quality of each path by asking relevant resource agents about the existing schedules
    * selects the most preferable path
    * decides whether to deviate from current plan to new route
    * makes reservation for its intended route
* Coordination happens indirectly through ants (smart messages that interact with resource agents): exploration ants and intention ants
* Exploration ants:
  * follow a candidate path through the environment
  * at each resource agent, queries the existing schedule
  * at destination, calculates optimal schedule along the traveled path using context-aware routing
  * vehicle agents compare all schedules reported by exploration ants
  * vehicle agents select the schedule with earliest arrival time
* Intention ants:
  * follow intended path and make reservations with each resource agent
  * reports whether it successfully made reservations on all resources
  * if it cannot reserve all the resources, the vehicle explores again
  * reservations evaporate after a while 
* This allows agents to plan concurrently
* Delay propagation: if a vehicle agent expects a delay, can ask resource agent to rearrange the schedule and propagate the delay to other resources/vehicles 
* **Alternative path finding**:
  * need to find a good set of paths that are not only the shortest ones and are not too long
  * k-shortest paths are often similar
  * k-disjoint paths has large dependency on the shortest path that is also the first path in the set
  * Penalty approach with some modifications has comparably good results
  * **SEE ALGORITHM 2**
* Results:
  * Static: CA computes single-agent optimal routes sequentially leading to a global Pareto-optimal solution and there is no guarantee about global optimality. 
  * Static: Delegate MAS regularly samples the environment and its solution also gradually converges to a Pareto-optimum.
  

### A Decentralized Approach for Anticipatory Vehicle Routing using Delegate Multi-Agent Systems
    Rutger Claes, Tom Holvoet and Danny Weyns Member, IEEE
    https://www.diva-portal.org/smash/get/diva2:477513/FULLTEXT01.pdf
* Delegate MAS, ant-like agents
* Direct vehicle routing by accounting for traffic forecast information
* Large-scale dynamic environments
* Challenges: (1) large scale of traffic, (2) dynamics (accidents, road blocks, demand peaks, etc.), (3) stability
* agents are embedded, i.e., directly linked to the environment
* Delegate MAS used for coordination model. Inspired by ant behavior where relevant information is dropped on locations.
* Here, ant-like agents explore the traffic environment on behalf of vehicles, and drop relevant information in ICT infrastructure that is coupled with the road infrastructure elements.
* Responsibility of the driver to make route choices
* 3 main elements:
  * A. Multi-agent based vehicle routing
    * 3 basic types of entities: vehicle agent, infrastructure agent, virtual environment
    * Vehicle and infrastructure agents are responsible for coordinating traffic
    * Vehicle agents have 2 responsibilities: 
      1) explore through the environment and search for viable routes (= assess quality of routes). 
      2) inform other agents of intended route by informing all infrastructure agents representing elements that are part of its intention
  * B. Delegate MSA for anticipatory vehicle routing
    * Use ants to achieve both exploration and intention propagation functionality
    1) Exploration ants:
      * vehicle sends them out at regular time intervals
      * ants explore various paths between agent position and destination
      * at every road element, it asks infrastructure agent what the departure time from its element would be if the vehicle would arrive at a certain time.
      * exploration ants assume basic, static routing information to be available
      * at the destination, the ant has an estimate of how long it would take the vehicle to get there
      * the ant follows its path reversely towards the vehicle agent 
    2) Intention ants:
      * vehicle selects an explored path to follow and intends to follow it
      * intention ants propagate this intention over the intended route at regular intervals
      * they inform the infrastructure agents that the vehicle agent intends to make use of the road element
      * vehicle agents can change their intentions, notifications by old intention ants will evaporate over time
  * C. Design decisions in the implementation
    1) Vehicle agent architecture
      * BDI architecture. 
      * **SEE PAPER FOR ALGORITHM**
      * Agent selects the route with the shorted trip duration based on the information sent back by the exploration ants.
      * Sends out intention ant across its current intention
    2) Infrastructure agents
      * Collect notifications from intention ants and use them to provide predictive traffic intensity information
      * Learning algorithm to learn to predict future traversal times based on number of notifications received
      * No drawbacks of reservation schemes thanks to learning algorithm
* Experiment setup:
  * Alternative routing strategies:
    * 3 alternative routing strategies based on the A* algorithm
      1) optimistic fastest route
      2) pessimistic fastest route
      3) Based on real-world usage of Traffic Message Channel (TMC)
    * **SEE PAPER FOR ALGORITHM DETAILS**
* Experiment results:
  * TODO ...
* Related work:
  1) Anticipatory vehicle routing:
    * TODO ...
  2) Propagation of information:
    * TODO ...
  3) Reservation based mechanisms:
    * TODO ...
    
### An adaptive multi-agent routing algorithm inspired by ants behavior
    Gianni Di Caro and Marco Dorigo
    http://staff.washington.edu/paymana/swarm/dicaro98-part.pdf
* AntNet
* Inspired by stigmergy
* Compared to OSPF, SPF, Q-routing, Predictive Q-routing
* AntNet outperforms the others in network throughput and average packet delay


### When do agents outperform centralized algorithms? A systematic empirical evaluation in logistics
    Rinde R.S. van Lon · Tom Holvoet
    https://lirias.kuleuven.be/bitstream/123456789/585362/1/jaamas2017.pdf
* ...
