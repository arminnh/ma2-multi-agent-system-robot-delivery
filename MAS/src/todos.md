# Todo's

* Check that bestTime < this.intendedArrivalTime works correctly
  * Gebeurt enkel als je al een intention hebt, dus na resendExplorationAnts
  * this.intendedArrivalTime juist wordt gezet en gedecrement bij moves
  * estimatedTime wel juist word berekend. i.p.v. +1, + verwachte rijtijd afhankelijk van snelheid enzo.

* Desire ant evaluation logic
  * Implement good scoring function
  * Beter kiezen van tasks
    * Mogelijks beetje randomness toevoegen zodat robots niet dezelfde tasks als best tasks krijgen
    * Desires in de lijst droppen met een kans
    * Betere paden maken
  * Minder sturen, pizzeriaModel.getOldestDeliveryTasks(X)

* Scenarios
  * Compatibility met Scenarios zodat ze in experimenten kunnen worden gezet
  * Experiments opzetten
