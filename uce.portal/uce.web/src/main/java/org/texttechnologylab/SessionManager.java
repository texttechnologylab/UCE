package org.texttechnologylab;

import org.texttechnologylab.cronjobs.SessionJob;

import java.util.HashMap;

public final class SessionManager {

    // TODO: I think these search states can stay in RAM for a while. Everything in this SessionManager
    // gets cleaned up anyways from time to time by a cronjob
    public static HashMap<String, SearchState> ActiveSearches = new HashMap<String, SearchState>();
    public static HashMap<String, String> CachedWikiPages = new HashMap<>();

    public static void InitSessionManager(long cleanupInterval){
        Runnable runnable = new SessionJob(cleanupInterval);
        var sessionJob = new Thread(runnable);
        sessionJob.start();
    }

    private SessionManager() {}
}
