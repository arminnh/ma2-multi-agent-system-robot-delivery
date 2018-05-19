package mas.agents;

public class Battery {
    public final int maxCapacity;
    private int capacity;

    public Battery(int maxCapacity) {
        this.maxCapacity = maxCapacity;
        capacity = maxCapacity;
    }

    public int getRemainingCapacity() {
        return capacity;
    }

    public boolean isAtMaxCapacity() {
        return capacity == maxCapacity;
    }

    public void decrementCapacity() {
        if (capacity > 0) {
            capacity--;
        }
    }

    public void incrementCapacity() {
        if (capacity < maxCapacity) {
            capacity++;
        }
    }
}
