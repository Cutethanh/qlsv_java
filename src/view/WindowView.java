package qlsv.view;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Window;

public abstract class WindowView {
    protected final JPanel root = new JPanel(new BorderLayout());
    private final String title;
    private final int width;
    private final int height;
    private final Component owner;
    private final boolean dialog;
    private JButton defaultButton;
    private boolean closeDisabled;
    private Window window;

    protected WindowView(String title, int width, int height, Component owner, boolean dialog) {
        this.title = title;
        this.width = width;
        this.height = height;
        this.owner = owner;
        this.dialog = dialog;
    }

    public JPanel getContent() { return root; }

    public String getTitle() { return title; }

    public int getWidth() { return width; }

    public int getHeight() { return height; }

    public boolean isDialog() { return dialog; }

    protected Window getWindow() { return window; }

    protected void setDefaultButton(JButton button) { this.defaultButton = button; }

    protected void disableCloseButton() { this.closeDisabled = true; }

    public void setVisible(boolean visible) {
        if (visible && window == null) {
            window = createWindow();
        }
        if (window != null) {
            window.setVisible(visible);
        }
    }

    public void dispose() {
        if (window != null) {
            window.dispose();
        }
    }

    private Window createWindow() {
        if (dialog) {
            Window ownerWindow = owner == null ? null
                    : owner instanceof Window ? (Window) owner : SwingUtilities.getWindowAncestor(owner);
            JDialog d = new JDialog(ownerWindow, title, Dialog.ModalityType.APPLICATION_MODAL);
            d.setContentPane(root);
            d.setSize(width, height);
            d.setLocationRelativeTo(ownerWindow);
            d.setDefaultCloseOperation(closeDisabled ? WindowConstants.DO_NOTHING_ON_CLOSE
                    : WindowConstants.DISPOSE_ON_CLOSE);
            if (defaultButton != null) {
                d.getRootPane().setDefaultButton(defaultButton);
            }
            return d;
        }
        JFrame f = new JFrame(title);
        f.setContentPane(root);
        f.setSize(width, height);
        f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        f.setLocationRelativeTo(null);
        if (defaultButton != null) {
            f.getRootPane().setDefaultButton(defaultButton);
        }
        return f;
    }
}
