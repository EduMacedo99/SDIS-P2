package src.utils;

public class Disk { 
    
    private final static int DEFAULT_DISK_SPACE = 9500;
    private int total_space;
    private int used_space;

    public Disk() {
        total_space = DEFAULT_DISK_SPACE;
        this.set_used_space(0);
    }

    /**Returns the used memory by the peer */
    public synchronized int get_used_space() {
        return used_space;
    }

    /* Defines a new used space */
    public synchronized void set_used_space(int used_space) {
        this.used_space = used_space;
    }

    /* Increases the space used */
    public synchronized void increase_used_space(int delta) {
        this.used_space += delta;
    }

    /* Decreases the used space */
    public synchronized void decrease_used_space(int delta) {
        this.used_space -= delta;
    }

    /* Returns the available space */
    public synchronized int get_available_space() {
        return total_space - used_space;
    }

    /* Returns true if available storage is higher than the specified value or false otherwise */
    public synchronized boolean has_space_for(int amount) {
        return get_available_space() >= amount;
    }

    /* Defines a new maximum storage capacity */
    public synchronized void set_max_capacity(int max_capacity) {
        total_space = max_capacity;
    }

    /* Returns the percentage of the used space */
    public synchronized double get_use_percentage() {
        return Math.max(((double)used_space)/((double)total_space), 0.0);
    }

    /* Prints the disk state */
    public void print_state() {
        System.out.println("Disk - " + total_space + " bytes");
        System.out.println(" -> Used: " + used_space + " bytes (" + (int)(get_use_percentage()*100.0) + " %)");
        System.out.println(" -> Available: " + get_available_space() + " bytes");
    }
    
}
