package com.entityreborn.socpuppet.util;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Thread started on shutdown that monitors for and kills rogue non-daemon threads.
 */
public class ShutdownMonitorThread extends Thread {

    /**
     * The delay in milliseconds until leftover threads are killed.
     */
    private static final int DELAY = 2000;
    private Runnable postShutdown = null;
    public ShutdownMonitorThread() {
        setName("ShutdownMonitorThread");
    }
    
    public ShutdownMonitorThread(Runnable postCall) {
        this();
        postShutdown = postCall;
    }

    @Override
    public void run() {
        final Map<Thread, StackTraceElement[]> traces = Thread.getAllStackTraces();
        final Map<Thread, StackTraceElement[]> roguetraces = new HashMap<>();
        
        for (Map.Entry<Thread, StackTraceElement[]> entry : traces.entrySet()) {
            final Thread thread = entry.getKey();
            final StackTraceElement[] stack = entry.getValue();
            
            if (thread == this || thread.isDaemon() || !thread.isAlive() || stack.length == 0) {
                // won't keep JVM from exiting
                continue;
            }
            
            roguetraces.put(thread, stack);
        }
        
        try {
            if (!roguetraces.isEmpty()) {
                Thread.sleep(DELAY);
            }
        } catch (InterruptedException e) {
            Logger.getGlobal().log(Level.SEVERE, "Shutdown monitor interrupted", e);
            doPostCall();
            return;
        }

        for (Map.Entry<Thread, StackTraceElement[]> entry : roguetraces.entrySet()) {
            final Thread thread = entry.getKey();
            
            if (thread == this) {
                continue;
            }
            
            final StackTraceElement[] stack = entry.getValue();

            Logger.getGlobal().warning("Rogue thread: " + thread);
            for (StackTraceElement trace : stack) {
                Logger.getGlobal().warning("    at " + trace);
            }

            // ask nicely to kill them
            thread.interrupt();
            
            // wait for them to die on their own
            if (thread.isAlive()) {
                try {
                    thread.join(1000);
                } catch (InterruptedException ex) {
                    Logger.getGlobal().log(Level.SEVERE, "Shutdown monitor interrupted", ex);
                    doPostCall();
                    
                    return;
                }
            }
        }
        
        doPostCall();
    }
    
    private void doPostCall() {
        if (postShutdown != null) {
            try {
                postShutdown.run();
            } catch (Throwable t) {
                Logger.getGlobal().log(Level.SEVERE, "Shutdown monitor post-Shutdown generated an exception", t);
            }
        }
    }
}
