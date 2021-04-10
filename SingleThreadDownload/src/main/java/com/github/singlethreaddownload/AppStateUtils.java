package com.github.singlethreaddownload;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.SparseArray;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 获取应用状态工具类
 */
class AppStateUtils {
    public interface AppStateChangeListener {
        void onStateChange(boolean intoFront);
    }

    private SparseArray<AppStateChangeListener> stateChangeListenerMap;

    /*0:后台，1：前台*/
    private boolean currentStateIsFront;

    private static AppStateUtils singleObj;
    private AtomicInteger atomicInteger;
    //刚刚进入application,但是还没进入activity或者activity某些生命周期没执行完成，那么状态还是属于前台
    private boolean firstIntoApp;


    private Application.ActivityLifecycleCallbacks activityLifecycleCallbacks;

    private AppStateUtils() {
        stateChangeListenerMap = new SparseArray<>();
        atomicInteger = new AtomicInteger();
        firstIntoApp = true;
        currentStateIsFront = true;
        initCallback();
    }

    private void initCallback() {
        if (activityLifecycleCallbacks != null) {
            return;
        }
        activityLifecycleCallbacks = new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            }

            @Override
            public void onActivityStarted(Activity activity) {
                atomicInteger.incrementAndGet();
                if (currentStateIsFront && firstIntoApp) {
                    firstIntoApp = false;
                    notifyStateChangeListener(true);
                    currentStateIsFront = true;
                    return;
                }
                if (isFront() && !currentStateIsFront) {
                    notifyStateChangeListener(true);
                    currentStateIsFront = true;
                }
            }

            @Override
            public void onActivityResumed(Activity activity) {
            }

            @Override
            public void onActivityPaused(Activity activity) {
            }

            @Override
            public void onActivityStopped(Activity activity) {
                atomicInteger.decrementAndGet();
                if (atomicInteger.get() <= 0) {
                    firstIntoApp = false;
                }
                if (isBackground() && currentStateIsFront) {
                    notifyStateChangeListener(false);
                    currentStateIsFront = false;
                }
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
                /*只要有activity退出，也保存下载记录，为了保证当前下载页面退出及时保存下载记录*/
                notifyStateChangeListener(false);
            }
        };
    }

    public static AppStateUtils get() {
        if (singleObj == null) {
            synchronized (AppStateUtils.class) {
                if (singleObj == null) {
                    singleObj = new AppStateUtils();
                }
            }
        }
        return singleObj;
    }

    public static void register(Application application) {
        AppStateUtils.get().registerActivityLifecycleCallback(application);
    }

    public static void unRegister(Application application) {
        AppStateUtils.get().unRegisterActivityLifecycleCallback(application);
    }

    private void registerActivityLifecycleCallback(Application application) {
        if (application == null) {
            throw new IllegalStateException("AppStateUtils.init(Application application),application can not null");
        }
        initCallback();
        application.registerActivityLifecycleCallbacks(activityLifecycleCallbacks);
    }

    private void unRegisterActivityLifecycleCallback(Application application) {
        if (application == null) {
            throw new IllegalStateException("AppStateUtils.init(Application application),application can not null");
        }
        application.unregisterActivityLifecycleCallbacks(activityLifecycleCallbacks);
    }

    public static boolean isFront() {
        int count = AppStateUtils.get().atomicInteger.get();
        return count > 0 || AppStateUtils.get().firstIntoApp;
    }

    public static boolean isBackground() {
        return isFront() == false;
    }


    public void addAppStateChangeListener(Object object, AppStateChangeListener appStateChangeListener) {
        if (appStateChangeListener == null) {
            return;
        }
        if (object == null) {
            return;
        }
        stateChangeListenerMap.put(object.hashCode(), appStateChangeListener);
    }

    public void removeAppStateChangeListener(Object object) {
        if (object == null) {
            return;
        }
        if (stateChangeListenerMap == null) {
            return;
        }
        stateChangeListenerMap.remove(object.hashCode());
    }

    private void notifyStateChangeListener(boolean intoFront) {
        if (stateChangeListenerMap == null) {
            return;
        }
        for (int i = 0; i < stateChangeListenerMap.size(); i++) {
            AppStateChangeListener appStateChangeListener = stateChangeListenerMap.valueAt(i);
            if (appStateChangeListener == null) {
                continue;
            }
            appStateChangeListener.onStateChange(intoFront);
        }
    }
}
