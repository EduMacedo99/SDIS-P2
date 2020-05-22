package src.utils;

public class Disk { 
    
    private final static int DEFAULT_DISK_SPACE = 1000;
    private int totalSpace;
    private int usedSpace;

    public Disk() {
        totalSpace = DEFAULT_DISK_SPACE;
        this.setUsedSpace(0);
    }

    /**Returns the used memory by the peer */
    public synchronized int getUsedSpace() {
        return usedSpace;
    }

    /* Defines a new used space */
    public synchronized void setUsedSpace(int usedSpace) {
        this.usedSpace = usedSpace;
    }

    /* Increases the space used */
    public synchronized void increaseUsedSpace(int delta) {
        this.usedSpace += delta;
    }

    /* Decreases the used space */
    public synchronized void decreaseUsedSpace(int delta) {
        this.usedSpace -= delta;
    }

    /* Returns the available space */
    public synchronized int getAvailableSpace() {
        return totalSpace - usedSpace;
    }

    /* Defines a new maximum storage capacity */
    public synchronized void setMaxCapacity(int maxCapacity) {
        totalSpace = maxCapacity;
    }

    /* Returns the percentage of the used space */
    public synchronized double getUsePercentage() {
        return Math.max(((double)usedSpace)/((double)totalSpace), 0.0);
    }
    
}
