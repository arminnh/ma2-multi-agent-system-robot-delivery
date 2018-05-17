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
* Edges have some capacity C(e)
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
    * generates set of feasible paths from current location to destination using STATIC INFRASTRUCTURE GRAPH
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
* ...
 
    
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
