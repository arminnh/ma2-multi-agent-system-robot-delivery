# Todo's

* Check that bestTime < this.intendedArrivalTime works correctly
  * Use a map with high road works probability and check that a new better path is actually chosen when roadworks happen

* Desire ant evaluation logic
  * Improve scoring function and selection function
  * Reorder chosen desired so that the shortest path is followed from when the robot starts driving again:
    * From the point the robot starts driving, the shortest path will always be the best one w.r.t. decreasing total waiting time.
