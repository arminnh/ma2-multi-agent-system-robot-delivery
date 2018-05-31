package mas.agents;

public class Battery {
    public final double maxCapacity;
    private double capacity;

    public Battery(double maxCapacity) {
        this.maxCapacity = maxCapacity;
        capacity = maxCapacity;
    }

    public double getRemainingCapacityPercentage() {
        return Math.round(100.0 * this.capacity / this.maxCapacity) / 100.0;
    }

    public boolean isAtMaxCapacity() {
        return capacity >= maxCapacity;
    }

    public void decreaseCapacity(double distance) {
        capacity -= distance;
        if (capacity < 0.0) {
            capacity = 0.0;
        }
    }

    public void incrementCapacityWith(double amount) {
        capacity += amount;
        if (capacity > maxCapacity) {
            capacity = maxCapacity;
        }
    }

    public void incrementCapacity() {
        capacity++;
        if (capacity > maxCapacity) {
            capacity = maxCapacity;
        }
    }
}
