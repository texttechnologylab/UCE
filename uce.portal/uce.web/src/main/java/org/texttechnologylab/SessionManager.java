package org.texttechnologylab;

import org.texttechnologylab.cronjobs.SessionJob;

import java.util.HashMap;

public final class SessionManager {

    // TODO: outsource this to a db or something.
    // TODO^2: This needs to be adressed! At some point, if we cache the search states in RAM, we will overflow eventually!
    public static HashMap<String, SearchState> ActiveSearches = new HashMap<String, SearchState>();

    public static void InitSessionManager(long cleanupInterval){
        Runnable runnable = new SessionJob(cleanupInterval);
        var sessionJob = new Thread(runnable);
        sessionJob.start();
    }

    private SessionManager() {}
}
