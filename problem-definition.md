# Multi-agent systems project

## Problem definition

Pizzeria RoboPizza wants to use AGVs to deliver pizzas to their customers. They contacted iDelivery to rent the AGVs for the delivery tasks. iDelivery is responsible for the charging of the robots whilst RoboPizza is responsible for handling incoming pizza delivery requests.

#### Environment
American style city
Optional: AGVs cannot pass each other in one street. This means that once an AGV moves in one direction, other AGVs will have to move in the same direction as well.

#### AGVs with their capabilities
AGVs can move from and to any location in the city. They can carry up to 5 pizzas at once. AGVs have maps and can compute paths between locations by themselves.

#### Characteristics of tasks
A task consists of picking up an amount of pizzas at RoboPizza before a certain time point and delivering it to a location before another time point.  
If there are more than 5 pizzas for a task, the task will have to be split up.

#### Scale
Large city

#### Communication constraints
AGVs can only communicate with other entities that lie within a certain area around them.

#### Dynamism
Charging station can change location.  
Streets can become one way streets or can become closed off because of sudden road works.  
Optional: tasks can be cancelled.

#### Potential AGV crashes
Running out of battery, going into one way streets, ???

#### Measure for efficiency
Efficiency of the multi-agent system will be measured by the waiting time for customers. This number is to be minimized.

#### Random generation of requests
Tasks will be created at every time step with a low probability. The amount of pizzas for the tasks will be determined randomly using a Gaussian distribution. The time window for the delivery will be determined by the distance from the customer to RoboPizza multiplied by a constant factor and added to a random number.

#### Battery charging
Charging happens on one location. Only a limited amount of AGVs can charge at the charging station at the same time. (Maximum capacity of 30% of the AGVs?)

## Algorithms
- BDI
- delegate mas

Need to review this in more detail.

## Research plan

#### Objectives

#### Questions

#### Hypotheses

#### Plan
1. First create environment
2.
