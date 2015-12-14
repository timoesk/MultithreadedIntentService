package net.tolleri.android.fragments;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import net.tolleri.android.app.multithreadedintentservice.BuildConfig;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Drop-in replacement for Android <code>IntentService</code>.
 * This service executes <code>onHandleIntent</code> in multiple threads.
 */
public abstract class MultithreadedIntentService extends Service {

    private String tag;

    private Queue<Integer> startIds;

    /**
     * Executor service instance than can be used by the implementing class.
     */
    protected ExecutorService executor;

    public MultithreadedIntentService(String tag) {
        super();
        this.tag = tag;
    }

    @Override
    public void onCreate() {
        if (BuildConfig.DEBUG)
            Log.d(tag, "Creating executor");
        startIds = new ConcurrentLinkedQueue<>();
        executor = Executors.newCachedThreadPool();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (startIds.offer(startId))
            executor.execute(new ServiceCommand(intent));
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        if (BuildConfig.DEBUG)
            Log.d(tag, "Shutting down executor");
        executor.shutdown();
    }

    protected abstract void onHandleIntent(Intent intent);

    private class ServiceCommand implements Runnable {

        private Intent intent;

        public ServiceCommand(Intent intent) {
            this.intent = intent;
        }

        @Override
        public void run() {
            onHandleIntent(intent);
            Integer startId = startIds.poll();
            if (startId != null)
                stopSelf(startId);
        }
    }
}
