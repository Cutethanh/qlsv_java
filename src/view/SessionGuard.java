package qlsv.view;

import javax.swing.Timer;
import java.awt.AWTEvent;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;

public class SessionGuard {
    private final Timer timer;
    private final AWTEventListener activityListener;

    public SessionGuard(int minutes, Runnable onTimeout) {
        timer = new Timer(minutes * 60_000, e -> {
            stop();
            onTimeout.run();
        });
        timer.setRepeats(false);
        activityListener = event -> timer.restart();
    }

    public void start() {
        Toolkit.getDefaultToolkit().addAWTEventListener(activityListener,
                AWTEvent.KEY_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
        timer.start();
    }

    public void stop() {
        timer.stop();
        Toolkit.getDefaultToolkit().removeAWTEventListener(activityListener);
    }
}
