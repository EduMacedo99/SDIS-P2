package src.helper;

import java.util.Timer;
import java.util.TimerTask;


public class HelperThread extends Thread{
    
    //wait_time between executions of thread
    private int wait_time;
    private Timer timer;

    HelperThread(int value){
        this.timer = new Timer();
        this.wait_time = value;

    }

    @Override
    public synchronized void start() {
        Runnable runnable = this;
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                runnable.run();
            }
        }, wait_time, wait_time);
    }

}