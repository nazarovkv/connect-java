package cd.connect.lifecycle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Singleton that keeps track of the lifecycle of an application. It also allows things to register with it
 * so they will get notified when the status changes so they can allocate or unallocate resources.
 */
public class ApplicationLifecycleManager {

    private static final Logger log = LoggerFactory.getLogger(ApplicationLifecycleManager.class);

    private LifecycleStatus status = LifecycleStatus.STARTING;

    private static ApplicationLifecycleManager instance;
    private List<Consumer<LifecycleTransition>> stateChangeListeners = new ArrayList<>();

    private static ApplicationLifecycleManager getInstance() {
        if (instance == null) {
            instance = new ApplicationLifecycleManager();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
              // tell everyone who cares we are shutting down
              updateStatus(LifecycleStatus.TERMINATING);
            }));
        }
        return instance;
    }

    public static LifecycleStatus getStatus() {
        return getInstance().status;
    }

    public static void registerListener(Consumer<LifecycleTransition> trans) {
        getInstance().stateChangeListeners.add(trans);
    }

    public static void updateStatus(LifecycleStatus newStatus) {
        log.debug("Setting status to {}", newStatus);

        LifecycleTransition trans = new LifecycleTransition(getInstance().status, newStatus);
        // we take a copy because each listener can add new listeners (for shutdown hooks)
        List<Consumer<LifecycleTransition>> listeners = new ArrayList<>(getInstance().stateChangeListeners);
        listeners.forEach(s -> {
                try {
                    s.accept(trans);
                } catch (RuntimeException re) {
                    // if it is a runtime exception when we are starting, pass it up and try and
                    // stop the app
                    log.error("Failed to notify of state change: {}", trans, re);
                    if (newStatus == LifecycleStatus.STARTED) {
                        System.exit(-1);
                    }
                } catch (Exception e) {
                    log.error("Failed to notify of state change: {}", trans, e);
                }
            }
        );
        getInstance().status = newStatus;
    }

    public static boolean isAlive() {
        ApplicationLifecycleManager state = getInstance();
        return state.status == LifecycleStatus.STARTING || state.status == LifecycleStatus.STARTED;
    }

    public static boolean isReady() {
        return getInstance().status == LifecycleStatus.STARTED;
    }

}
