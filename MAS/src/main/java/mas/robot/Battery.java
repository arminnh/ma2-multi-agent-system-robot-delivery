package mas.robot;

public class Battery {
    private static int maxCapacity;
    private int capacity;

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

    public boolean isAtMaxCapacity(){return capacity == maxCapacity;}

    public void decrementCapacity() {
        if(capacity > 0){
            capacity--;
        }
    }

    public void incrementCapacity(){
        if(capacity < maxCapacity){
            capacity++;
        }
    }
}
