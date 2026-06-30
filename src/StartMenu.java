import java.awt.BorderLayout;
import java.awt.Event;
import java.awt.Frame;

/**
 * Main class. Jalankan dengan:
 *   javac *.java
 *   java StartMenu
 *
 * Catatan:
 * - Tidak memakai package event listener modern.
 * - Closing window memakai event model lama AWT: handleEvent(Event e).
 */
public class StartMenu extends Frame {
    private DesktopPanel desktop;

    public StartMenu() {
        super("Windows 95 Start Menu - Final Project Grafika Komputer");
        setSize(MenuData.WIN_W, MenuData.WIN_H);
        setLayout(new BorderLayout());
        setResizable(false);
        setLocationRelativeTo(null);

        desktop = new DesktopPanel(this);
        add(desktop, BorderLayout.CENTER);
    }

    public boolean handleEvent(Event e) {
        if (e.id == Event.WINDOW_DESTROY) {
            System.exit(0);
            return true;
        }
        return super.handleEvent(e);
    }

    public static void main(String[] args) {
        StartMenu app = new StartMenu();
        app.setVisible(true);
        app.requestFocusOnDesktop();
    }

    public void requestFocusOnDesktop() {
        if (desktop != null) desktop.requestFocus();
    }
}
