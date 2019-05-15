/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filessortapp;

import java.util.concurrent.ArrayBlockingQueue;


/**
 *
 * @author Simon
 */
public class ThreadPoolQueue extends ArrayBlockingQueue<Runnable> {

    /**
     *
     * @param n
     */
    public ThreadPoolQueue(int n) {
        super(n);
    }
    
    @Override
    public boolean offer(Runnable e) {
        try {
            put(e);
        } catch (InterruptedException exc) {
            return false;
        }
        return true;
    }
    
}
