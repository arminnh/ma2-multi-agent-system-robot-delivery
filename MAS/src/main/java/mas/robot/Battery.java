package mas.robot;

public class Battery {
    private static int maxCapacity;
    private static int capacity;

    public Battery(int maxCapacity) {
        Battery.maxCapacity = maxCapacity;
        capacity = maxCapacity;
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }

    public int getRemainingCapacity() {
        return capacity;
    }

    public void setCapacity(int c) {
        capacity = c;
    }

    public void decrementCapacity() {
        capacity--;
    }
}
