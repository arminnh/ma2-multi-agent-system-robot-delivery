package mas.agents;

public class Battery {
    public final double maxCapacity;
    private double capacity;

    public Battery(double maxCapacity) {
        this.maxCapacity = maxCapacity;
        capacity = maxCapacity;
    }

    public double getRemainingCapacity() {
        return capacity;
    }

    public double getRemainingCapacityPercentage() {
        return Math.round(100.0 * this.capacity / this.maxCapacity) / 100.0;
    }

    public boolean isAtMaxCapacity() {
        return capacity == maxCapacity;
    }

    public void decreaseCapacity(double distance) {
        if (capacity > 0) {
            capacity -= distance;
        }
    }

    public void incrementCapacity() {
        if (capacity < maxCapacity) {
            capacity++;
        }
    }
}
