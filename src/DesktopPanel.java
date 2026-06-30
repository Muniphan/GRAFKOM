import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Frame;
import java.awt.Event;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Panel;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.ArrayList;

/**
 * Windows 95 Start Menu Simulation.
 *
 * Catatan penting untuk final project:
 * - Tidak memakai Swing, JavaFX, Qt, ImGui, Win32, Tkinter, atau library eksternal.
 * - Seluruh desktop, Start Menu, taskbar, window, ikon, titlebar, dan tombol
 *   digambar manual memakai AWT + Graphics2D.
 * - Aplikasi dibuat sebagai "internal simulated windows" sehingga tombol minimize
 *   dan taskbar bekerja di dalam desktop tiruan.
 */
public class DesktopPanel extends Panel {
    private final Frame parentFrame;
    private Image offscreen;
    private Graphics2D buffer;

    private boolean startOpen = false;
    private boolean hoverStart = false;
    private int hoverMain = -1;
    private int hoverSub = -1;
    private int activeSub = -1;
    private int hoverDesktopIcon = -1;
    private int hoverTaskButton = -1;

    private Rectangle startMenuRect = new Rectangle();
    private Rectangle subMenuRect = new Rectangle();
    private final Rectangle[] mainItemRects = new Rectangle[MenuData.MAIN_MENU.length];
    private Rectangle[] subItemRects = new Rectangle[0];
    private final Rectangle[] quickRects = new Rectangle[3];
    private final ArrayList<Rectangle> taskRects = new ArrayList<Rectangle>();

    private final ArrayList<AppWindow> windows = new ArrayList<AppWindow>();
    private AppWindow activeWindow = null;
    private AppWindow draggingWindow = null;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;
    private int nextWindowId = 1;

    private boolean welcomeCreated = false;

    private static class AppWindow {
        int id;
        String title;
        String type;
        Rectangle bounds;
        Rectangle restoreBounds;
        boolean minimized;
        boolean maximized;

        AppWindow(int id, String title, String type, Rectangle bounds) {
            this.id = id;
            this.title = title;
            this.type = type;
            this.bounds = bounds;
            this.restoreBounds = new Rectangle(bounds);
        }
    }

    public DesktopPanel(Frame parent) {
        this.parentFrame = parent;
        setBackground(MenuData.DESKTOP_BG);
        setCursor(Cursor.getDefaultCursor());
        setFocusable(true);        // Tidak memakai package event listener modern.
        // Interaksi mouse/keyboard memakai event model lama AWT:
        // mouseDown, mouseMove, mouseDrag, mouseUp, keyDown.

    }

    public void update(Graphics g) { paint(g); }

    public void paint(Graphics g) {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        if (offscreen == null || offscreen.getWidth(this) != w || offscreen.getHeight(this) != h) {
            offscreen = createImage(w, h);
            buffer = (Graphics2D) offscreen.getGraphics();
        }

        buffer.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        buffer.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);

        drawDesktop(buffer, w, h);
        drawDesktopIcons(buffer);
        ensureStartupWelcome(w, h);
        drawAllWindows(buffer);
        drawTaskbar(buffer);

        if (startOpen) {
            drawStartMenu(buffer);
            if (activeSub >= 0) drawSubMenu(buffer, activeSub);
        }

        g.drawImage(offscreen, 0, 0, this);
    }

    private int taskbarY() { return 0; }

    private Rectangle startButtonRect() {
        return new Rectangle(2, taskbarY() + 4, MenuData.START_W, MenuData.START_H);
    }

    private int startMenuHeight() {
        int h = 8;
        for (int i = 0; i < MenuData.MAIN_MENU.length; i++) {
            h += MenuData.MAIN_MENU[i].separator ? MenuData.MENU_SEP_H : MenuData.MENU_ITEM_H;
        }
        return h + 8;
    }

    private void computeStartMenuLayout() {
        // Menu utama sekarang berupa lingkaran/radial menu yang muncul dari tombol Start
        // pada taskbar atas. Rectangle tetap dipakai hanya untuk hit-test tiap item.
        int diameter = 318;
        startMenuRect = new Rectangle(4, MenuData.TASKBAR_H + 6, diameter, diameter);
        int cx = startMenuRect.x + startMenuRect.width / 2;
        int cy = startMenuRect.y + startMenuRect.height / 2;
        int radius = 118;

        int visibleCount = 0;
        for (int i = 0; i < MenuData.MAIN_MENU.length; i++) if (!MenuData.MAIN_MENU[i].separator) visibleCount++;

        int n = 0;
        for (int i = 0; i < MenuData.MAIN_MENU.length; i++) {
            MenuData.Item item = MenuData.MAIN_MENU[i];
            if (item.separator) {
                mainItemRects[i] = new Rectangle(0, 0, 0, 0);
                continue;
            }
            double angle = Math.toRadians(-125 + (250.0 * n / Math.max(1, visibleCount - 1)));
            int ix = cx + (int) Math.round(Math.cos(angle) * radius);
            int iy = cy + (int) Math.round(Math.sin(angle) * radius);
            mainItemRects[i] = new Rectangle(ix - 52, iy - 18, 104, 36);
            n++;
        }
    }

    private void computeSubMenuLayout(int mainIndex) {
        if (mainIndex < 0 || mainIndex >= MenuData.MAIN_MENU.length || MenuData.MAIN_MENU[mainIndex].sub == null) {
            subMenuRect = new Rectangle();
            subItemRects = new Rectangle[0];
            return;
        }
        MenuData.Item[] items = MenuData.MAIN_MENU[mainIndex].sub;
        int diameter = 300;
        int x = startMenuRect.x + startMenuRect.width - 18;
        int y = startMenuRect.y + 12;
        if (x + diameter > getWidth() - 4) x = getWidth() - diameter - 4;
        subMenuRect = new Rectangle(x, y, diameter, diameter);

        int cx = subMenuRect.x + subMenuRect.width / 2;
        int cy = subMenuRect.y + subMenuRect.height / 2;
        int radius = 112;
        int visibleCount = 0;
        for (int i = 0; i < items.length; i++) if (!items[i].separator) visibleCount++;

        subItemRects = new Rectangle[items.length];
        int n = 0;
        for (int i = 0; i < items.length; i++) {
            if (items[i].separator) {
                subItemRects[i] = new Rectangle(0, 0, 0, 0);
                continue;
            }
            double angle = Math.toRadians(-130 + (260.0 * n / Math.max(1, visibleCount - 1)));
            int ix = cx + (int) Math.round(Math.cos(angle) * radius);
            int iy = cy + (int) Math.round(Math.sin(angle) * radius);
            subItemRects[i] = new Rectangle(ix - 58, iy - 17, 116, 34);
            n++;
        }
    }


    private void ensureStartupWelcome(int w, int h) {
        if (welcomeCreated) return;
        if (w <= 0 || h <= 0) return;
        Rectangle b = new Rectangle(70, MenuData.TASKBAR_H + 18, 560, 330);
        AppWindow welcome = new AppWindow(nextWindowId++, "Welcome", "welcome", b);
        windows.add(welcome);
        activeWindow = welcome;
        welcomeCreated = true;
    }

    // ============================================================
    // Desktop dan taskbar
    // ============================================================
    private void drawDesktop(Graphics2D g, int w, int h) {
        g.setColor(MenuData.DESKTOP_BG);
        g.fillRect(0, 0, w, h);

        // Wallpaper baru: logo Windows 95 seperti referensi, tetapi digambar manual
        // memakai primitif AWT/Graphics2D, bukan gambar bitmap eksternal.
        drawWindows95WallpaperLogo(g, w, h);
    }

    private void drawWindows95WallpaperLogo(Graphics2D g, int w, int h) {
        Object oldAA = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int cx = w / 2 + 70;
        int cy = MenuData.TASKBAR_H + 205;
        int tile = 10;

        // Jejak pixel hitam/berwarna di kiri logo, dibuat manual agar mirip logo Windows 95.
        Color[] trail = {Color.BLACK, new Color(255, 110, 28), new Color(45, 120, 220), new Color(45, 165, 70)};
        int[][] pts = {
                {-190,-76,0},{-168,-90,1},{-145,-80,0},{-122,-93,0},
                {-205,-52,2},{-182,-60,0},{-158,-50,1},{-134,-61,0},{-110,-48,0},
                {-218,-28,0},{-194,-35,2},{-170,-25,0},{-146,-34,3},{-122,-22,0},
                {-206,-2,2},{-180,-9,0},{-155,0,0},{-130,-8,0},
                {-188,22,3},{-162,16,0},{-138,27,0}
        };
        for (int i = 0; i < pts.length; i++) {
            g.setColor(trail[pts[i][2] % trail.length]);
            g.fillRect(cx + pts[i][0], cy + pts[i][1], tile, tile);
        }

        // Empat panel bendera bergaya melengkung memakai polygon.
        drawFlagPane(g, cx - 82, cy - 82, new Color(255, 110, 28), true);
        drawFlagPane(g, cx - 2,  cy - 72, new Color(96, 174, 73), true);
        drawFlagPane(g, cx - 88, cy + 8,  new Color(61, 168, 236), false);
        drawFlagPane(g, cx - 5,  cy + 16, new Color(255, 218, 35), false);

        // Garis hitam antar panel dan outline logo.
        g.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(Color.BLACK);
        g.drawArc(cx - 140, cy - 103, 270, 210, -65, 130);
        g.drawArc(cx - 135, cy - 100, 260, 210, 120, 115);
        g.drawLine(cx - 15, cy - 96, cx - 20, cy + 112);
        g.drawLine(cx - 104, cy - 12, cx + 95, cy - 3);
        g.setStroke(new BasicStroke(1));

        // Teks Microsoft Windows 95.
        int tx = cx - 125;
        int ty = cy + 150;
        g.setFont(new Font("Dialog", Font.PLAIN, 20));
        g.setColor(new Color(230, 230, 230));
        g.drawString("Microsoft", tx, ty - 38);
        g.setFont(new Font("Dialog", Font.BOLD, 44));
        g.setColor(Color.BLACK);
        g.drawString("Windows", tx + 40, ty);
        g.setFont(new Font("Dialog", Font.PLAIN, 42));
        g.setColor(Color.WHITE);
        g.drawString("95", tx + 252, ty);

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
    }

    private void drawFlagPane(Graphics2D g, int x, int y, Color c, boolean top) {
        Polygon shadow = new Polygon();
        shadow.addPoint(x + 4, y + 4);
        shadow.addPoint(x + 64, y + (top ? -6 : 8));
        shadow.addPoint(x + 62, y + 66);
        shadow.addPoint(x - 2, y + 58);
        g.setColor(Color.BLACK);
        g.fillPolygon(shadow);

        Polygon pane = new Polygon();
        pane.addPoint(x, y);
        pane.addPoint(x + 58, y + (top ? -10 : 4));
        pane.addPoint(x + 56, y + 58);
        pane.addPoint(x - 6, y + 52);
        g.setColor(c);
        g.fillPolygon(pane);
        g.setColor(Color.BLACK);
        g.drawPolygon(pane);
    }

    private void drawDesktopIcons(Graphics2D g) {
        int x = 18;
        int y = MenuData.TASKBAR_H + 18;
        int slotH = 76;
        for (int i = 0; i < MenuData.DESKTOP_ICONS.length; i++) {
            int icon = Integer.parseInt(MenuData.DESKTOP_ICONS[i][1]);
            int ix = x + 13;
            int iy = y + i * slotH;
            drawDesktopIconBubble(g, ix - 6, iy - 4, 42, i == hoverDesktopIcon);
            drawIcon(g, icon, ix, iy, 32, false);
            if (i == hoverDesktopIcon) {
                g.setColor(MenuData.HOVER_BG);
                g.fillRoundRect(x - 4, iy + 36, 86, 28, 18, 18);
            }
            drawDesktopLabel(g, MenuData.DESKTOP_ICONS[i][0], x - 3, iy + 46, 90, i == hoverDesktopIcon);
        }
    }

    private void drawDesktopLabel(Graphics2D g, String text, int x, int y, int w, boolean selected) {
        g.setFont(MenuData.FONT_DESKTOP);
        String[] lines = splitLines(text);
        for (int i = 0; i < lines.length; i++) {
            FontMetrics fm = g.getFontMetrics();
            int tx = x + (w - fm.stringWidth(lines[i])) / 2;
            int ty = y + i * 13;
            if (!selected) {
                g.setColor(Color.BLACK);
                g.drawString(lines[i], tx + 1, ty + 1);
            }
            g.setColor(Color.WHITE);
            g.drawString(lines[i], tx, ty);
        }
    }

    private String[] splitLines(String s) {
        int idx = s.indexOf('\n');
        if (idx < 0) return new String[] { s };
        return new String[] { s.substring(0, idx), s.substring(idx + 1) };
    }

    private void drawTaskbar(Graphics2D g) {
        int y = taskbarY();
        int w = getWidth();
        g.setColor(MenuData.TASKBAR_BG);
        g.fillRect(0, y, w, MenuData.TASKBAR_H);
        g.setColor(MenuData.BORDER_LIGHT);
        g.drawLine(0, y, w, y);
        g.drawLine(0, y, 0, y + MenuData.TASKBAR_H - 1);
        g.setColor(MenuData.BORDER_DARK);
        g.drawLine(0, y + MenuData.TASKBAR_H - 1, w, y + MenuData.TASKBAR_H - 1);
        g.setColor(MenuData.BORDER_BLACK);
        g.drawLine(0, y + MenuData.TASKBAR_H - 2, w, y + MenuData.TASKBAR_H - 2);

        drawStartButton(g);
        drawQuickLaunch(g, y);
        drawDynamicTaskButtons(g, y);
        drawTray(g, y);
    }

    private void drawStartButton(Graphics2D g) {
        Rectangle r = startButtonRect();
        g.setColor(MenuData.TASKBAR_BG);
        g.fillRoundRect(r.x, r.y, r.width, r.height, 30, 30);
        if (startOpen) drawSunkenRoundRect(g, r.x, r.y, r.width, r.height, 30);
        else drawRaisedRoundRect(g, r.x, r.y, r.width, r.height, 30);

        drawMiniWinLogo(g, r.x + 9, r.y + 7, 18);
        g.setColor(Color.BLACK);
        g.setFont(new Font("Dialog", Font.BOLD, 12));
        g.drawString("Start", r.x + 33, r.y + 22);
    }

    private void drawQuickLaunch(Graphics2D g, int taskY) {
        int x = 84;
        g.setColor(MenuData.BORDER_MID);
        g.drawLine(x - 5, taskY + 6, x - 5, taskY + 34);
        g.setColor(MenuData.BORDER_LIGHT);
        g.drawLine(x - 4, taskY + 6, x - 4, taskY + 34);

        quickRects[0] = new Rectangle(x, taskY + 8, 20, 22);
        quickRects[1] = new Rectangle(x + 24, taskY + 8, 20, 22);
        quickRects[2] = new Rectangle(x + 48, taskY + 8, 20, 22);
        drawTinyIconButton(g, quickRects[0], MenuData.ICON_IE);
        drawTinyIconButton(g, quickRects[1], MenuData.ICON_FOLDER);
        drawTinyIconButton(g, quickRects[2], MenuData.ICON_CMD);
    }

    private void drawTinyIconButton(Graphics2D g, Rectangle r, int icon) {
        g.setColor(MenuData.TASKBAR_BG);
        g.fillOval(r.x, r.y, r.width, r.height);
        drawRaisedOval(g, r.x, r.y, r.width, r.height);
        drawIcon(g, icon, r.x + 3, r.y + 4, 14, false);
    }

    private void drawDynamicTaskButtons(Graphics2D g, int taskY) {
        taskRects.clear();
        int trayW = 86;
        int startX = 158;
        int endX = getWidth() - trayW - 16;
        int count = windows.size();
        if (count == 0) return;

        int gap = 4;
        int available = Math.max(120, endX - startX);
        int bw = Math.min(190, Math.max(105, (available - gap * Math.max(0, count - 1)) / count));
        for (int i = 0; i < count; i++) {
            AppWindow win = windows.get(i);
            int x = startX + i * (bw + gap);
            Rectangle r = new Rectangle(x, taskY + 5, bw, 29);
            taskRects.add(r);
            boolean active = (win == activeWindow && !win.minimized);
            g.setColor(MenuData.TASKBAR_BG);
            g.fillRoundRect(r.x, r.y, r.width, r.height, 26, 26);
            if (active) drawSunkenRoundRect(g, r.x, r.y, r.width, r.height, 26);
            else drawRaisedRoundRect(g, r.x, r.y, r.width, r.height, 26);
            drawMiniWinLogo(g, r.x + 6, r.y + 7, 14);
            g.setFont(MenuData.FONT_BOLD);
            g.setColor(Color.BLACK);
            String t = truncateText(g, win.title, r.width - 32);
            g.drawString(t, r.x + 26, r.y + 19);
        }
    }

    private String truncateText(Graphics2D g, String text, int maxW) {
        FontMetrics fm = g.getFontMetrics();
        if (fm.stringWidth(text) <= maxW) return text;
        String ell = "...";
        int n = text.length();
        while (n > 1 && fm.stringWidth(text.substring(0, n) + ell) > maxW) n--;
        return text.substring(0, Math.max(1, n)) + ell;
    }

    private void drawTray(Graphics2D g, int taskY) {
        int trayW = 78;
        int x = getWidth() - trayW - 6;
        int y = taskY + 8;
        g.setColor(MenuData.TASKBAR_BG);
        g.fillRoundRect(x, y, trayW, 24, 20, 20);
        drawSunkenRoundRect(g, x, y, trayW, 24, 20);

        // Jam dibuat statis agar tidak memakai fitur otomatis/timer waktu.
        // Sesuai arahan final project: semua tampilan digambar manual dan jam tidak berjalan.
        String time = "16:00";
        g.setFont(MenuData.FONT_SMALL);
        g.setColor(Color.BLACK);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(time, x + (trayW - fm.stringWidth(time)) / 2, y + 16);
    }

    // ============================================================
    // Window manager internal
    // ============================================================
    private void openSimulatedWindow(String name) {
        String normalized = normalizeAppName(name);
        String type = detectType(normalized);

        // Kalau window yang sama sudah ada dan sedang minimize, restore saja.
        for (int i = 0; i < windows.size(); i++) {
            AppWindow w = windows.get(i);
            if (w.title.equals(normalized)) {
                w.minimized = false;
                bringToFront(w);
                repaint();
                return;
            }
        }

        Rectangle b = defaultBounds(type, windows.size());
        AppWindow win = new AppWindow(nextWindowId++, normalized, type, b);
        windows.add(win);
        activeWindow = win;
        repaint();
    }

    private String normalizeAppName(String name) {
        if (name == null) return "Application";
        if (name.equals("MS-DOS Prompt")) return "MS-DOS Prompt";
        if (name.equals("MS Paint Effect") || name.equals("Paint Effect")) return "MS Paint Effect";
        if (name.equals("Command Prompt")) return "MS-DOS Prompt";
        if (name.equals("Windows Explorer") || name.equals("My Computer") || name.equals("Computer...") || name.equals("Network Neighborhood")) return "Exploring - My Computer";
        if (name.equals("My Documents") || name.equals("Documents")) return "Exploring - My Documents";
        if (name.equals("Control Panel") || name.equals("Printers") || name.equals("Taskbar & Start Menu") || name.equals("Folder Options")) return "Control Panel";
        if (name.equals("Files or Folders...") || name.equals("Computer...")) return "Find";
        if (name.equals("On The Internet")) return "Internet Explorer";
        if (name.equals("The Microsoft Network")) return "The Microsoft Network";
        return name;
    }

    private String detectType(String title) {
        if (title.equals("Welcome")) return "welcome";
        if (title.equals("MS Paint Effect")) return "paint-effect";
        if (title.indexOf("Exploring") >= 0 || title.equals("Recycle Bin")) return "explorer";
        if (title.equals("Notepad") || title.endsWith(".txt") || title.indexOf("Readme") >= 0) return "notepad";
        if (title.equals("Calculator")) return "calculator";
        if (title.equals("Paint")) return "paint";
        if (title.equals("MS-DOS Prompt")) return "cmd";
        if (title.equals("Internet Explorer") || title.equals("The Microsoft Network")) return "ie";
        if (title.equals("Control Panel") || title.equals("Settings")) return "control";
        if (title.equals("Find")) return "find";
        return "default";
    }

    private Rectangle defaultBounds(String type, int offset) {
        int ox = (offset % 4) * 28;
        int oy = (offset % 4) * 22;
        if (type.equals("calculator")) return new Rectangle(620 - ox, 165 + oy, 230, 300);
        if (type.equals("cmd")) return new Rectangle(280 + ox, 180 + oy, 520, 280);
        if (type.equals("welcome")) return new Rectangle(70, MenuData.TASKBAR_H + 18, 560, 330);
        if (type.equals("paint")) return new Rectangle(210 + ox, 110 + oy, 650, 460);
        if (type.equals("paint-effect")) return new Rectangle(160 + ox, 95 + oy, 720, 500);
        if (type.equals("notepad")) return new Rectangle(250 + ox, 130 + oy, 520, 360);
        if (type.equals("ie")) return new Rectangle(210 + ox, 115 + oy, 650, 420);
        if (type.equals("control")) return new Rectangle(260 + ox, 135 + oy, 520, 360);
        if (type.equals("find")) return new Rectangle(300 + ox, 170 + oy, 430, 230);
        return new Rectangle(170 + ox, 95 + oy, 620, 420);
    }

    private void drawAllWindows(Graphics2D g) {
        for (int i = 0; i < windows.size(); i++) {
            AppWindow win = windows.get(i);
            if (!win.minimized) drawAppWindow(g, win);
        }
    }

    private void drawAppWindow(Graphics2D g, AppWindow win) {
        Rectangle b = win.bounds;
        boolean active = (win == activeWindow);

        g.setColor(new Color(70, 70, 70, 110));
        g.fillRoundRect(b.x + 7, b.y + 7, b.width, b.height, 64, 64);

        g.setColor(MenuData.WINDOW_BG);
        g.fillRoundRect(b.x, b.y, b.width, b.height, 58, 58);
        drawRaisedRoundRect(g, b.x, b.y, b.width, b.height, 58);
        drawRaisedRoundRect(g, b.x + 2, b.y + 2, b.width - 4, b.height - 4, 54);

        Rectangle title = titleBarRect(win);
        g.setColor(active ? MenuData.TITLE_BLUE : MenuData.TITLE_INACTIVE);
        g.fillRoundRect(title.x, title.y, title.width, title.height, 28, 28);
        drawMiniWinLogo(g, title.x + 4, title.y + 3, 14);
        g.setFont(MenuData.FONT_TITLE);
        g.setColor(Color.WHITE);
        g.drawString(truncateText(g, win.title, title.width - 84), title.x + 24, title.y + 14);

        drawCaptionButton(g, minButtonRect(win), "_");
        drawCaptionButton(g, maxButtonRect(win), win.maximized ? "❐" : "□");
        drawCaptionButton(g, closeButtonRect(win), "x");

        Rectangle c = contentRect(win);
        g.setColor(MenuData.WINDOW_BG);
        g.fillRoundRect(c.x, c.y, c.width, c.height, 34, 34);
        if (win.type.equals("welcome")) drawWelcomeContent(g, win, c);
        else if (win.type.equals("explorer")) drawExplorerContent(g, win, c);
        else if (win.type.equals("notepad")) drawNotepadContent(g, c);
        else if (win.type.equals("calculator")) drawCalculatorContent(g, c);
        else if (win.type.equals("paint")) drawPaintContent(g, c);
        else if (win.type.equals("paint-effect")) drawPaintEffectContent(g, c);
        else if (win.type.equals("cmd")) drawCommandContent(g, c);
        else if (win.type.equals("ie")) drawIEContent(g, c, win.title);
        else if (win.type.equals("control")) drawControlPanelContent(g, c);
        else if (win.type.equals("find")) drawFindContent(g, c);
        else drawDefaultContent(g, c, win.title);
    }

    private Rectangle titleBarRect(AppWindow win) {
        return new Rectangle(win.bounds.x + 10, win.bounds.y + 8, win.bounds.width - 20, 24);
    }

    private Rectangle contentRect(AppWindow win) {
        return new Rectangle(win.bounds.x + 10, win.bounds.y + 36, win.bounds.width - 20, win.bounds.height - 46);
    }

    private Rectangle minButtonRect(AppWindow win) {
        return new Rectangle(win.bounds.x + win.bounds.width - 72, win.bounds.y + 9, 16, 16);
    }

    private Rectangle maxButtonRect(AppWindow win) {
        return new Rectangle(win.bounds.x + win.bounds.width - 50, win.bounds.y + 9, 16, 16);
    }

    private Rectangle closeButtonRect(AppWindow win) {
        return new Rectangle(win.bounds.x + win.bounds.width - 28, win.bounds.y + 9, 16, 16);
    }

    private Rectangle welcomeBodyCloseRect(AppWindow win) {
        Rectangle c = contentRect(win);
        return new Rectangle(c.x + c.width - 146, c.y + 158, 100, 26);
    }

    private void drawCaptionButton(Graphics2D g, Rectangle r, String label) {
        g.setColor(MenuData.WIN95_GRAY);
        g.fillOval(r.x, r.y, r.width, r.height);
        drawRaisedOval(g, r.x, r.y, r.width, r.height);
        g.setFont(MenuData.FONT_SMALL);
        g.setColor(Color.BLACK);
        if ("_".equals(label)) {
            g.drawLine(r.x + 4, r.y + r.height - 5, r.x + r.width - 5, r.y + r.height - 5);
            return;
        }
        if ("□".equals(label) || "❐".equals(label)) {
            g.drawRect(r.x + 4, r.y + 3, 7, 7);
            if ("❐".equals(label)) g.drawRect(r.x + 6, r.y + 1, 7, 7);
            return;
        }
        g.drawString("x", r.x + 5, r.y + 11);
    }

    private void bringToFront(AppWindow win) {
        windows.remove(win);
        windows.add(win);
        activeWindow = win;
    }

    private AppWindow topWindowAt(Point p) {
        for (int i = windows.size() - 1; i >= 0; i--) {
            AppWindow w = windows.get(i);
            if (!w.minimized && w.bounds.contains(p)) return w;
        }
        return null;
    }

    private void minimizeWindow(AppWindow w) {
        w.minimized = true;
        if (activeWindow == w) activeWindow = findLastVisibleWindow();
        repaint();
    }

    private AppWindow findLastVisibleWindow() {
        for (int i = windows.size() - 1; i >= 0; i--) {
            AppWindow w = windows.get(i);
            if (!w.minimized) return w;
        }
        return null;
    }

    private void toggleMaximize(AppWindow w) {
        if (!w.maximized) {
            w.restoreBounds = new Rectangle(w.bounds);
            w.bounds = new Rectangle(4, MenuData.TASKBAR_H + 4, getWidth() - 8, getHeight() - MenuData.TASKBAR_H - 8);
            w.maximized = true;
        } else {
            w.bounds = new Rectangle(w.restoreBounds);
            w.maximized = false;
        }
        bringToFront(w);
        repaint();
    }

    private void closeWindow(AppWindow w) {
        windows.remove(w);
        if (activeWindow == w) activeWindow = findLastVisibleWindow();
        repaint();
    }

    private void handleTaskButtonClick(Point p) {
        for (int i = 0; i < taskRects.size() && i < windows.size(); i++) {
            if (taskRects.get(i).contains(p)) {
                AppWindow win = windows.get(i);
                if (win == activeWindow && !win.minimized) minimizeWindow(win);
                else {
                    win.minimized = false;
                    bringToFront(win);
                    repaint();
                }
                return;
            }
        }
    }

    // ============================================================
    // Konten window internal
    // ============================================================
    private void drawWelcomeContent(Graphics2D g, AppWindow win, Rectangle c) {
        g.setColor(MenuData.WINDOW_BG);
        g.fillRect(c.x, c.y, c.width, c.height);

        Rectangle cream = new Rectangle(c.x + 16, c.y + 18, 260, c.height - 36);
        g.setColor(new Color(255, 255, 220));
        g.fillRect(cream.x, cream.y, cream.width, cream.height);
        g.setColor(Color.WHITE);
        g.drawLine(cream.x, cream.y, cream.x + cream.width - 1, cream.y);
        g.drawLine(cream.x, cream.y, cream.x, cream.y + cream.height - 1);
        g.setColor(MenuData.BORDER_MID);
        g.drawLine(cream.x + cream.width, cream.y, cream.x + cream.width, cream.y + cream.height);

        g.setFont(new Font("Serif", Font.BOLD, 25));
        g.setColor(Color.BLACK);
        g.drawString("Welcome to Windows", cream.x + 15, cream.y + 38);
        g.setColor(new Color(210, 210, 210));
        g.drawString("95", cream.x + 224, cream.y + 38);

        drawHelpIcon(g, cream.x + 38, cream.y + 62, 24);
        g.setFont(MenuData.FONT_BOLD);
        g.setColor(Color.BLACK);
        g.drawString("Did you know...", cream.x + 78, cream.y + 80);
        g.setFont(MenuData.FONT_SMALL);
        g.drawString("To open a program, you just click the Start", cream.x + 78, cream.y + 108);
        g.drawString("button, and then click the program's icon.", cream.x + 78, cream.y + 122);

        int mx = cream.x + 78;
        int my = cream.y + 150;
        drawMonitorWithArrow(g, mx, my);

        Rectangle whatsNew = new Rectangle(c.x + c.width - 160, c.y + 28, 122, 28);
        Rectangle online = new Rectangle(c.x + c.width - 160, c.y + 68, 122, 28);
        Rectangle close = welcomeBodyCloseRect(win);
        drawClassicButton(g, whatsNew, "What's New");
        drawClassicButton(g, online, "Online Registration");
        g.setColor(MenuData.BORDER_MID);
        g.drawLine(c.x + c.width - 170, c.y + 130, c.x + c.width - 30, c.y + 130);
        g.setColor(MenuData.BORDER_LIGHT);
        g.drawLine(c.x + c.width - 170, c.y + 131, c.x + c.width - 30, c.y + 131);
        drawClassicButton(g, close, "Close");
    }

    private void drawMonitorWithArrow(Graphics2D g, int x, int y) {
        g.setColor(new Color(110, 110, 110));
        g.fillRect(x + 18, y + 2, 126, 90);
        g.setColor(new Color(0, 128, 128));
        g.fillRect(x + 28, y + 12, 106, 70);
        g.setColor(Color.BLACK);
        g.drawRect(x + 18, y + 2, 126, 90);
        g.setColor(new Color(170, 170, 170));
        g.fillRect(x + 62, y + 92, 38, 15);
        g.fillRect(x + 44, y + 107, 75, 8);
        g.setColor(Color.BLACK);
        g.drawRect(x + 44, y + 107, 75, 8);

        g.setColor(Color.CYAN);
        Polygon arrow = new Polygon();
        arrow.addPoint(x + 103, y + 22);
        arrow.addPoint(x + 52, y + 55);
        arrow.addPoint(x + 70, y + 58);
        arrow.addPoint(x + 45, y + 78);
        arrow.addPoint(x + 78, y + 65);
        arrow.addPoint(x + 83, y + 80);
        arrow.addPoint(x + 111, y + 30);
        g.fillPolygon(arrow);
    }

    private void drawClassicButton(Graphics2D g, Rectangle r, String text) {
        g.setColor(MenuData.WIN95_GRAY);
        g.fillRect(r.x, r.y, r.width, r.height);
        drawRaisedRect(g, r.x, r.y, r.width, r.height);
        g.setFont(MenuData.FONT_SMALL);
        g.setColor(Color.BLACK);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, r.x + (r.width - fm.stringWidth(text)) / 2, r.y + 17);
    }

    private void drawExplorerContent(Graphics2D g, AppWindow win, Rectangle c) {
        drawClassicMenuBar(g, c, new String[] {"File", "Edit", "View", "Go", "Favorites", "Tools", "Help"});
        int toolbarY = c.y + 22;
        g.setColor(MenuData.WINDOW_BG);
        g.fillRect(c.x, toolbarY, c.width, 32);
        g.setColor(MenuData.BORDER_MID);
        g.drawLine(c.x, toolbarY, c.x + c.width, toolbarY);
        String[] tools = {"Back", "Forward", "Up", "Cut", "Copy", "Paste", "Delete", "Properties"};
        int tx = c.x + 8;
        for (int i = 0; i < tools.length; i++) {
            drawSmallToolbarButton(g, tx, toolbarY + 4, tools[i]);
            tx += 64;
        }

        int addrY = c.y + 55;
        g.setColor(MenuData.WINDOW_BG);
        g.fillRect(c.x, addrY, c.width, 24);
        g.setFont(MenuData.FONT_SMALL);
        g.setColor(Color.BLACK);
        g.drawString("Address", c.x + 6, addrY + 16);
        Rectangle addr = new Rectangle(c.x + 58, addrY + 3, c.width - 65, 18);
        g.setColor(Color.WHITE);
        g.fillRect(addr.x, addr.y, addr.width, addr.height);
        drawSunkenRect(g, addr.x, addr.y, addr.width, addr.height);
        g.setColor(Color.BLACK);
        String path = win.title.indexOf("Documents") >= 0 ? "C:\\My Documents" : "My Computer";
        g.drawString(path, addr.x + 5, addr.y + 13);

        int bodyY = c.y + 81;
        int statusH = 22;
        Rectangle left = new Rectangle(c.x + 4, bodyY, 190, c.height - 83 - statusH);
        Rectangle right = new Rectangle(left.x + left.width + 5, bodyY, c.width - left.width - 13, left.height);
        g.setColor(Color.WHITE);
        g.fillRect(left.x, left.y, left.width, left.height);
        g.fillRect(right.x, right.y, right.width, right.height);
        drawSunkenRect(g, left.x, left.y, left.width, left.height);
        drawSunkenRect(g, right.x, right.y, right.width, right.height);

        g.setFont(MenuData.FONT_SMALL);
        String[] tree = {"Desktop", "  My Computer", "    3½ Floppy (A:)", "    (C:)", "    (D:)", "  Control Panel", "  Dial-Up Networking", "  My Documents", "  Recycle Bin"};
        int yy = left.y + 18;
        for (int i = 0; i < tree.length; i++) {
            g.setColor(i == 1 ? MenuData.HOVER_BG : Color.WHITE);
            if (i == 1) g.fillRect(left.x + 4, yy - 12, left.width - 8, 15);
            drawIcon(g, (tree[i].indexOf("Computer") >= 0) ? MenuData.ICON_COMPUTER : MenuData.ICON_FOLDER, left.x + 8 + (tree[i].startsWith("    ") ? 28 : tree[i].startsWith("  ") ? 14 : 0), yy - 12, 12, false);
            g.setColor(i == 1 ? Color.WHITE : Color.BLACK);
            g.drawString(tree[i], left.x + 26, yy);
            yy += 17;
        }

        String[][] files = {
                {"3½ Floppy (A:)", "folder"}, {"(C:)", "folder"}, {"(D:)", "folder"},
                {"Printers", "printer"}, {"Control Panel", "control"}, {"Dial-Up Networking", "folder"},
                {"Scheduled Tasks", "folder"}, {"Web Folders", "ie"}
        };
        int colX = right.x + 10;
        int colY = right.y + 20;
        for (int i = 0; i < files.length; i++) {
            int icon = MenuData.ICON_FOLDER;
            if (files[i][1].equals("printer")) icon = MenuData.ICON_PRINTER;
            if (files[i][1].equals("control")) icon = MenuData.ICON_CONTROL;
            if (files[i][1].equals("ie")) icon = MenuData.ICON_IE;
            drawIcon(g, icon, colX, colY - 14, 14, false);
            g.setColor(Color.BLACK);
            g.drawString(files[i][0], colX + 20, colY);
            colY += 18;
        }

        Rectangle st = new Rectangle(c.x, c.y + c.height - statusH, c.width, statusH);
        g.setColor(MenuData.WINDOW_BG);
        g.fillRect(st.x, st.y, st.width, st.height);
        drawSunkenRect(g, st.x + 2, st.y + 2, st.width - 4, st.height - 4);
        g.setColor(Color.BLACK);
        g.drawString("8 object(s)", st.x + 8, st.y + 15);
    }

    private void drawSmallToolbarButton(Graphics2D g, int x, int y, String text) {
        drawRaisedRect(g, x, y, 58, 23);
        g.setFont(new Font("Dialog", Font.PLAIN, 9));
        g.setColor(Color.BLACK);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, x + (58 - fm.stringWidth(text)) / 2, y + 15);
    }

    private void drawClassicMenuBar(Graphics2D g, Rectangle c, String[] menus) {
        g.setColor(MenuData.WINDOW_BG);
        g.fillRect(c.x, c.y, c.width, 21);
        g.setFont(MenuData.FONT_SMALL);
        g.setColor(Color.BLACK);
        int x = c.x + 8;
        for (int i = 0; i < menus.length; i++) {
            g.drawString(menus[i], x, c.y + 15);
            x += g.getFontMetrics().stringWidth(menus[i]) + 18;
        }
    }

    private void drawNotepadContent(Graphics2D g, Rectangle c) {
        drawClassicMenuBar(g, c, new String[] {"File", "Edit", "Search", "Help"});
        Rectangle area = new Rectangle(c.x + 2, c.y + 23, c.width - 4, c.height - 25);
        g.setColor(Color.WHITE);
        g.fillRect(area.x, area.y, area.width, area.height);
        drawSunkenRect(g, area.x, area.y, area.width, area.height);
        g.setFont(new Font("Monospaced", Font.PLAIN, 12));
        g.setColor(Color.BLACK);
        g.drawString("Welcome to Windows 95 Notepad.", area.x + 8, area.y + 22);
        g.drawString("This dummy editor is drawn inside the AWT canvas.", area.x + 8, area.y + 42);
        g.drawString("Minimize button and taskbar button are functional.", area.x + 8, area.y + 62);
    }

    private void drawCalculatorContent(Graphics2D g, Rectangle c) {
        g.setColor(MenuData.WINDOW_BG);
        g.fillRect(c.x, c.y, c.width, c.height);
        Rectangle display = new Rectangle(c.x + 12, c.y + 15, c.width - 24, 30);
        g.setColor(Color.WHITE);
        g.fillRect(display.x, display.y, display.width, display.height);
        drawSunkenRect(g, display.x, display.y, display.width, display.height);
        g.setFont(new Font("Dialog", Font.BOLD, 18));
        g.setColor(Color.BLACK);
        g.drawString("0.", display.x + display.width - 30, display.y + 22);
        String[] buttons = {"7", "8", "9", "/", "4", "5", "6", "*", "1", "2", "3", "-", "0", ".", "=", "+", "C", "CE", "%", "√"};
        int bw = 42, bh = 28, gap = 6;
        int sx = c.x + 14;
        int sy = c.y + 60;
        g.setFont(MenuData.FONT_NORMAL);
        for (int i = 0; i < buttons.length; i++) {
            int row = i / 4;
            int col = i % 4;
            int x = sx + col * (bw + gap);
            int y = sy + row * (bh + gap);
            g.setColor(MenuData.WIN95_GRAY);
            g.fillRect(x, y, bw, bh);
            drawRaisedRect(g, x, y, bw, bh);
            g.setColor(Color.BLACK);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(buttons[i], x + (bw - fm.stringWidth(buttons[i])) / 2, y + 18);
        }
    }

    private void drawPaintContent(Graphics2D g, Rectangle c) {
        drawClassicMenuBar(g, c, new String[] {"File", "Edit", "View", "Image", "Options", "Help"});
        int toolbarW = 58;
        Rectangle tools = new Rectangle(c.x + 2, c.y + 23, toolbarW, c.height - 25);
        g.setColor(MenuData.WINDOW_BG);
        g.fillRect(tools.x, tools.y, tools.width, tools.height);
        drawSunkenRect(g, tools.x, tools.y, tools.width, tools.height);
        for (int i = 0; i < 10; i++) {
            int x = tools.x + 8 + (i % 2) * 22;
            int y = tools.y + 8 + (i / 2) * 24;
            drawRaisedRect(g, x, y, 18, 18);
            g.setColor(i % 3 == 0 ? Color.BLACK : i % 3 == 1 ? Color.BLUE : Color.RED);
            g.drawLine(x + 4, y + 13, x + 14, y + 5);
        }
        Rectangle canvas = new Rectangle(c.x + toolbarW + 8, c.y + 27, c.width - toolbarW - 18, c.height - 36);
        g.setColor(Color.WHITE);
        g.fillRect(canvas.x, canvas.y, canvas.width, canvas.height);
        drawSunkenRect(g, canvas.x, canvas.y, canvas.width, canvas.height);
        g.setColor(Color.RED); g.drawOval(canvas.x + 70, canvas.y + 45, 120, 80);
        g.setColor(Color.BLUE); g.drawLine(canvas.x + 35, canvas.y + 170, canvas.x + 300, canvas.y + 70);
        g.setColor(Color.GREEN); g.drawRect(canvas.x + 280, canvas.y + 125, 120, 80);
    }

    private void drawPaintEffectContent(Graphics2D g, Rectangle c) {
        drawClassicMenuBar(g, c, new String[] {"File", "Edit", "View", "Image", "Colors", "Help"});

        Rectangle toolbar = new Rectangle(c.x + 2, c.y + 23, c.width - 4, 34);
        g.setColor(MenuData.WINDOW_BG);
        g.fillRect(toolbar.x, toolbar.y, toolbar.width, toolbar.height);
        drawSunkenRect(g, toolbar.x + 2, toolbar.y + 4, 34, 24);
        drawPaintIcon(g, toolbar.x + 9, toolbar.y + 7, 20);
        g.setFont(MenuData.FONT_SMALL);
        g.setColor(Color.BLACK);
        g.drawString("MS Paint Effect - reference sketch style, drawn manually with AWT Graphics2D", toolbar.x + 48, toolbar.y + 21);

        Rectangle page = new Rectangle(c.x + 10, c.y + 65, c.width - 20, c.height - 78);
        g.setColor(Color.WHITE);
        g.fillRect(page.x, page.y, page.width, page.height);
        drawSunkenRect(g, page.x, page.y, page.width, page.height);

        g.setFont(new Font("Dialog", Font.BOLD, 18));
        g.setColor(Color.BLACK);
        g.drawString("MS PAINT SKETCH EFFECT", page.x + 16, page.y + 26);
        g.setFont(MenuData.FONT_SMALL);
        g.setColor(new Color(80, 80, 80));
        g.drawString("Tampilan dibuat menyerupai referensi: garis sketsa, objek utama besar, dan figur kecil berwarna.", page.x + 18, page.y + 44);

        Rectangle art = new Rectangle(page.x + 22, page.y + 58, page.width - 44, page.height - 78);
        g.setColor(Color.WHITE);
        g.fillRect(art.x, art.y, art.width, art.height);
        drawSunkenRect(g, art.x, art.y, art.width, art.height);

        Rectangle inner = fitAspect(art, 132, 126, 12);
        drawReferenceSketch(g, inner);
    }

    private Rectangle fitAspect(Rectangle r, int aw, int ah, int pad) {
        int maxW = Math.max(1, r.width - pad * 2);
        int maxH = Math.max(1, r.height - pad * 2);
        double scale = Math.min(maxW / (double) aw, maxH / (double) ah);
        int w = (int) Math.round(aw * scale);
        int h = (int) Math.round(ah * scale);
        int x = r.x + (r.width - w) / 2;
        int y = r.y + (r.height - h) / 2;
        return new Rectangle(x, y, w, h);
    }

    private int sx(Rectangle r, double x) { return r.x + (int) Math.round(x * r.width / 132.0); }
    private int sy(Rectangle r, double y) { return r.y + (int) Math.round(y * r.height / 126.0); }

    private void drawReferenceSketch(Graphics2D g, Rectangle r) {
        // Gambar ini bukan file bitmap eksternal; seluruhnya digambar ulang memakai garis/shape AWT.
        Object oldAA = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setColor(Color.WHITE);
        g.fillRect(r.x, r.y, r.width, r.height);
        g.setColor(new Color(235, 235, 235));
        g.drawRect(r.x, r.y, r.width - 1, r.height - 1);
        g.setFont(new Font("Dialog", Font.BOLD, Math.max(8, r.width / 24)));
        g.setColor(new Color(225, 225, 225));
        g.drawString("AFTER", sx(r, 106), sy(r, 8));

        // Garis lingkungan yang sangat tipis seperti sketsa foto referensi.
        g.setStroke(new BasicStroke(Math.max(1f, r.width / 260f)));
        g.setColor(new Color(210, 210, 210));
        g.drawLine(sx(r, 6), sy(r, 99), sx(r, 124), sy(r, 96));
        g.drawLine(sx(r, 88), sy(r, 103), sx(r, 128), sy(r, 83));
        g.drawRect(sx(r, 112), sy(r, 76), sx(r, 130) - sx(r, 112), sy(r, 101) - sy(r, 76));
        g.drawLine(sx(r, 113), sy(r, 80), sx(r, 127), sy(r, 80));
        g.drawLine(sx(r, 115), sy(r, 91), sx(r, 129), sy(r, 91));

        // Figur kiri besar yang berwarna seperti coretan manual.
        drawSketchPerson(g, r, 11, 57, 1.22, new Color(230, 30, 30), new Color(20, 80, 220), true);
        g.setColor(new Color(45, 135, 55));
        g.drawLine(sx(r, 8), sy(r, 69), sx(r, 2), sy(r, 83));
        g.setColor(new Color(220, 120, 20));
        g.drawLine(sx(r, 15), sy(r, 66), sx(r, 25), sy(r, 51));

        // Objek utama: kepala/monumen besar berwarna coklat muda.
        g.setStroke(new BasicStroke(Math.max(2f, r.width / 90f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Color hair = new Color(197, 146, 76);
        Color face = new Color(185, 140, 82);
        g.setColor(hair);
        g.drawArc(sx(r, 44), sy(r, 18), sx(r, 91) - sx(r, 44), sy(r, 92) - sy(r, 18), 112, 250);
        g.drawArc(sx(r, 51), sy(r, 16), sx(r, 84) - sx(r, 51), sy(r, 88) - sy(r, 16), 80, 265);
        g.drawArc(sx(r, 55), sy(r, 24), sx(r, 85) - sx(r, 55), sy(r, 86) - sy(r, 24), 70, 265);

        // Dreadlock / garis rambut turun.
        for (int i = 0; i < 13; i++) {
            int x0 = sx(r, 43 + i * 4.1);
            int y0 = sy(r, 28 + (i % 3) * 2);
            int x1 = sx(r, 33 + i * 5.4);
            int y1 = sy(r, 91 + (i % 4) * 5);
            g.drawLine(x0, y0, x1, y1);
        }
        g.setStroke(new BasicStroke(Math.max(1.2f, r.width / 160f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(face);
        g.drawArc(sx(r, 57), sy(r, 38), sx(r, 75) - sx(r, 57), sy(r, 48) - sy(r, 38), 190, 150);
        g.drawArc(sx(r, 80), sy(r, 38), sx(r, 96) - sx(r, 80), sy(r, 48) - sy(r, 38), 190, 150);
        g.drawLine(sx(r, 75), sy(r, 48), sx(r, 73), sy(r, 62));
        g.drawLine(sx(r, 73), sy(r, 62), sx(r, 82), sy(r, 62));
        g.drawArc(sx(r, 67), sy(r, 71), sx(r, 89) - sx(r, 67), sy(r, 83) - sy(r, 71), 190, 160);

        // Figur kecil di depan objek utama.
        drawSketchPerson(g, r, 50, 91, 0.50, new Color(40, 130, 230), new Color(230, 160, 20), false);
        drawSketchPerson(g, r, 59, 90, 0.46, new Color(220, 30, 30), new Color(30, 120, 220), false);
        drawSketchPerson(g, r, 68, 92, 0.44, new Color(255, 155, 0), new Color(20, 100, 210), false);
        drawSketchPerson(g, r, 76, 91, 0.48, new Color(60, 160, 60), new Color(30, 120, 230), false);

        // Figur kanan besar, dominan biru seperti referensi.
        drawSketchPerson(g, r, 98, 66, 1.12, new Color(230, 40, 40), new Color(55, 150, 230), true);
        g.setColor(new Color(60, 160, 230));
        g.drawLine(sx(r, 99), sy(r, 74), sx(r, 91), sy(r, 91));
        g.drawLine(sx(r, 104), sy(r, 75), sx(r, 116), sy(r, 91));

        // Mobil/objek kanan bawah seperti garis coklat tipis.
        g.setStroke(new BasicStroke(Math.max(1.2f, r.width / 160f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(155, 95, 45));
        g.drawRect(sx(r, 112), sy(r, 96), sx(r, 130) - sx(r, 112), sy(r, 105) - sy(r, 96));
        g.drawArc(sx(r, 114), sy(r, 91), sx(r, 127) - sx(r, 114), sy(r, 102) - sy(r, 91), 0, 180);
        g.setColor(Color.BLACK);
        g.drawOval(sx(r, 115), sy(r, 103), 4, 4);
        g.drawOval(sx(r, 125), sy(r, 103), 4, 4);

        // Coretan kecil tambahan supaya terasa seperti hasil trace/MS Paint.
        g.setStroke(new BasicStroke(1f));
        Color[] dots = {Color.RED, Color.BLUE, Color.ORANGE, Color.MAGENTA, new Color(30, 160, 70)};
        for (int i = 0; i < 18; i++) {
            g.setColor(dots[i % dots.length]);
            int xx = sx(r, 25 + (i * 7) % 78);
            int yy = sy(r, 53 + (i * 11) % 42);
            g.drawLine(xx, yy, xx + 3, yy + 2);
        }

        g.setColor(Color.BLACK);
        g.drawRect(r.x, r.y, r.width - 1, r.height - 1);
        g.setStroke(new BasicStroke(1));
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
    }

    private void drawSketchPerson(Graphics2D g, Rectangle r, double bx, double by, double scale, Color shirt, Color pants, boolean armsUp) {
        int head = Math.max(3, (int) Math.round(7 * scale * r.width / 132.0));
        int x = sx(r, bx);
        int y = sy(r, by);
        int body = Math.max(8, (int) Math.round(19 * scale * r.height / 126.0));
        int arm = Math.max(6, (int) Math.round(13 * scale * r.width / 132.0));
        int leg = Math.max(8, (int) Math.round(18 * scale * r.height / 126.0));

        g.setStroke(new BasicStroke(Math.max(1f, (float)(scale * r.width / 145.0)), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(130, 85, 45));
        g.drawOval(x - head / 2, y - head, head, head);
        g.setColor(shirt);
        g.drawLine(x, y, x, y + body);
        if (armsUp) {
            g.drawLine(x, y + body / 3, x - arm, y + body / 3 - arm / 2);
            g.drawLine(x, y + body / 3, x + arm, y + body / 3 - arm / 3);
        } else {
            g.drawLine(x, y + body / 3, x - arm / 2, y + body / 2);
            g.drawLine(x, y + body / 3, x + arm / 2, y + body / 2);
        }
        g.setColor(pants);
        g.drawLine(x, y + body, x - arm / 2, y + body + leg);
        g.drawLine(x, y + body, x + arm / 2, y + body + leg);
        g.setColor(Color.BLACK);
        g.drawLine(x - arm / 2, y + body + leg, x - arm / 2 - 4, y + body + leg);
        g.drawLine(x + arm / 2, y + body + leg, x + arm / 2 + 4, y + body + leg);
        g.setStroke(new BasicStroke(1));
    }

    private void drawCommandContent(Graphics2D g, Rectangle c) {
        g.setColor(Color.BLACK);
        g.fillRect(c.x, c.y, c.width, c.height);
        g.setFont(new Font("Monospaced", Font.PLAIN, 13));
        g.setColor(Color.WHITE);
        String[] lines = {
                "Microsoft(R) Windows 95",
                "   (C)Copyright Microsoft Corp 1981-1995.",
                "",
                "C:\\WINDOWS>dir",
                "COMMAND.COM        EXPLORER.EXE       NOTEPAD.EXE",
                "CALC.EXE           MPLAYER.EXE        WIN.COM",
                "",
                "C:\\WINDOWS>_"
        };
        for (int i = 0; i < lines.length; i++) g.drawString(lines[i], c.x + 10, c.y + 22 + i * 18);
    }

    private void drawIEContent(Graphics2D g, Rectangle c, String title) {
        drawClassicMenuBar(g, c, new String[] {"File", "Edit", "View", "Go", "Favorites", "Help"});
        int addrY = c.y + 23;
        g.setColor(MenuData.WINDOW_BG);
        g.fillRect(c.x, addrY, c.width, 26);
        g.setFont(MenuData.FONT_SMALL);
        g.setColor(Color.BLACK);
        g.drawString("Address", c.x + 6, addrY + 17);
        Rectangle addr = new Rectangle(c.x + 58, addrY + 4, c.width - 65, 18);
        g.setColor(Color.WHITE);
        g.fillRect(addr.x, addr.y, addr.width, addr.height);
        drawSunkenRect(g, addr.x, addr.y, addr.width, addr.height);
        g.setColor(Color.BLACK);
        g.drawString("http://www.microsoft.com/windows95", addr.x + 5, addr.y + 13);

        Rectangle page = new Rectangle(c.x + 4, c.y + 54, c.width - 8, c.height - 58);
        g.setColor(Color.WHITE);
        g.fillRect(page.x, page.y, page.width, page.height);
        drawSunkenRect(g, page.x, page.y, page.width, page.height);
        g.setFont(new Font("Dialog", Font.BOLD, 26));
        g.setColor(MenuData.TITLE_BLUE);
        g.drawString(title.equals("The Microsoft Network") ? "The Microsoft Network" : "Internet Explorer", page.x + 34, page.y + 64);
        g.setFont(MenuData.FONT_NORMAL);
        g.setColor(Color.BLACK);
        g.drawString("Welcome to the Internet - Windows 95 style browser simulation.", page.x + 34, page.y + 100);
        g.setColor(MenuData.LINK_BLUE);
        g.drawString("Register Now", page.x + 34, page.y + 132);
        g.drawString("Connect to the Internet", page.x + 34, page.y + 154);
    }

    private void drawControlPanelContent(Graphics2D g, Rectangle c) {
        drawClassicMenuBar(g, c, new String[] {"File", "Edit", "View", "Help"});
        Rectangle page = new Rectangle(c.x + 4, c.y + 25, c.width - 8, c.height - 29);
        g.setColor(Color.WHITE);
        g.fillRect(page.x, page.y, page.width, page.height);
        drawSunkenRect(g, page.x, page.y, page.width, page.height);
        String[] names = {"Display", "Keyboard", "Mouse", "Printers", "Network", "System", "Sounds", "Date/Time", "Add/Remove", "Fonts", "Modems", "Password"};
        int[] icons = {MenuData.ICON_CONTROL, MenuData.ICON_APP, MenuData.ICON_APP, MenuData.ICON_PRINTER, MenuData.ICON_COMPUTER, MenuData.ICON_SETTINGS, MenuData.ICON_APP, MenuData.ICON_APP, MenuData.ICON_FOLDER, MenuData.ICON_DOCUMENTS, MenuData.ICON_COMPUTER, MenuData.ICON_HELP};
        int cols = 4;
        for (int i = 0; i < names.length; i++) {
            int col = i % cols;
            int row = i / cols;
            int x = page.x + 34 + col * 118;
            int y = page.y + 28 + row * 75;
            drawIcon(g, icons[i], x + 24, y, 32, false);
            drawDesktopLabel(g, names[i], x - 4, y + 42, 90, false);
        }
    }

    private void drawFindContent(Graphics2D g, Rectangle c) {
        g.setColor(MenuData.WINDOW_BG);
        g.fillRect(c.x, c.y, c.width, c.height);
        g.setFont(MenuData.FONT_NORMAL);
        g.setColor(Color.BLACK);
        g.drawString("Search for files or folders named:", c.x + 18, c.y + 30);
        Rectangle field = new Rectangle(c.x + 18, c.y + 40, c.width - 120, 22);
        g.setColor(Color.WHITE);
        g.fillRect(field.x, field.y, field.width, field.height);
        drawSunkenRect(g, field.x, field.y, field.width, field.height);
        drawRaisedRect(g, c.x + c.width - 88, c.y + 38, 70, 25);
        g.drawString("Find Now", c.x + c.width - 76, c.y + 55);
        g.drawString("Look in: My Computer", c.x + 18, c.y + 88);
        drawRaisedRect(g, c.x + 18, c.y + 105, c.width - 36, c.height - 120);
    }

    private void drawDefaultContent(Graphics2D g, Rectangle c, String title) {
        g.setColor(Color.WHITE);
        g.fillRect(c.x + 8, c.y + 8, c.width - 16, c.height - 16);
        drawSunkenRect(g, c.x + 8, c.y + 8, c.width - 16, c.height - 16);
        g.setFont(MenuData.FONT_BOLD);
        g.setColor(Color.BLACK);
        g.drawString(title + " - Windows 95 dummy application", c.x + 26, c.y + 44);
    }

    // ============================================================
    // Start Menu
    // ============================================================
    private void drawStartMenu(Graphics2D g) {
        computeStartMenuLayout();
        Rectangle r = startMenuRect;

        Object oldAA = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Bayangan dan badan menu berbentuk lingkaran.
        g.setColor(new Color(45, 45, 45));
        g.fillOval(r.x + 5, r.y + 5, r.width, r.height);
        g.setColor(MenuData.WIN95_GRAY);
        g.fillOval(r.x, r.y, r.width, r.height);
        drawRaisedOval(g, r.x, r.y, r.width, r.height);
        drawRaisedOval(g, r.x + 5, r.y + 5, r.width - 10, r.height - 10);

        // Pusat menu: identitas Windows 95 dan tombol kecil gaya klasik.
        int cx = r.x + r.width / 2;
        int cy = r.y + r.height / 2;
        g.setColor(MenuData.STRIP_BG);
        g.fillOval(cx - 58, cy - 58, 116, 116);
        drawRaisedOval(g, cx - 58, cy - 58, 116, 116);
        drawMiniWinLogo(g, cx - 18, cy - 43, 36);
        g.setFont(new Font("Dialog", Font.BOLD, 16));
        g.setColor(Color.WHITE);
        FontMetrics fm = g.getFontMetrics();
        g.drawString("Windows", cx - fm.stringWidth("Windows") / 2, cy + 17);
        g.setColor(new Color(205, 205, 205));
        g.drawString("95", cx - 9, cy + 38);

        for (int i = 0; i < MenuData.MAIN_MENU.length; i++) {
            if (!MenuData.MAIN_MENU[i].separator) drawCircularMenuItem(g, MenuData.MAIN_MENU[i], mainItemRects[i], i == hoverMain, false);
        }
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
    }

    private void drawSubMenu(Graphics2D g, int mainIndex) {
        computeSubMenuLayout(mainIndex);
        MenuData.Item[] items = MenuData.MAIN_MENU[mainIndex].sub;
        if (items == null) return;

        Rectangle r = subMenuRect;
        Object oldAA = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setColor(new Color(45, 45, 45));
        g.fillOval(r.x + 5, r.y + 5, r.width, r.height);
        g.setColor(MenuData.WIN95_GRAY);
        g.fillOval(r.x, r.y, r.width, r.height);
        drawRaisedOval(g, r.x, r.y, r.width, r.height);
        drawRaisedOval(g, r.x + 5, r.y + 5, r.width - 10, r.height - 10);

        int cx = r.x + r.width / 2;
        int cy = r.y + r.height / 2;
        g.setColor(MenuData.STRIP_BG);
        g.fillOval(cx - 45, cy - 45, 90, 90);
        drawSunkenOval(g, cx - 45, cy - 45, 90, 90);
        g.setFont(new Font("Dialog", Font.BOLD, 14));
        g.setColor(Color.WHITE);
        String title = MenuData.MAIN_MENU[mainIndex].text;
        FontMetrics fm = g.getFontMetrics();
        g.drawString(title, cx - fm.stringWidth(title) / 2, cy + 5);

        for (int i = 0; i < items.length; i++) {
            if (!items[i].separator) drawCircularMenuItem(g, items[i], subItemRects[i], i == hoverSub, true);
        }
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
    }

    private void drawCircularMenuItem(Graphics2D g, MenuData.Item item, Rectangle r, boolean hover, boolean submenu) {
        int bubble = submenu ? 30 : 34;
        int bx = r.x + (r.width - bubble) / 2;
        int by = r.y;
        g.setColor(hover ? MenuData.HOVER_BG : new Color(224, 224, 224));
        g.fillOval(bx, by, bubble, bubble);
        if (hover) drawSunkenOval(g, bx, by, bubble, bubble);
        else drawRaisedOval(g, bx, by, bubble, bubble);
        drawIcon(g, item.icon, bx + 6, by + 6, bubble - 12, hover);

        g.setFont(MenuData.FONT_BOLD);
        FontMetrics fm = g.getFontMetrics();
        int tx = r.x + (r.width - fm.stringWidth(item.text)) / 2;
        int ty = r.y + r.height + 10;
        if (hover) {
            int pad = 5;
            int tw = fm.stringWidth(item.text) + pad * 2;
            g.setColor(MenuData.HOVER_BG);
            g.fillRoundRect(tx - pad, ty - 13, tw, 17, 12, 12);
            g.setColor(Color.WHITE);
        } else {
            g.setColor(Color.BLACK);
        }
        g.drawString(item.text, tx, ty);

        if (item.sub != null) {
            int ax = bx + bubble - 5;
            int ay = by + bubble / 2 - 4;
            Polygon p = new Polygon();
            p.addPoint(ax, ay);
            p.addPoint(ax, ay + 9);
            p.addPoint(ax + 7, ay + 4);
            g.fillPolygon(p);
        }
    }

    private void drawMenuItem(Graphics2D g, MenuData.Item item, Rectangle r, boolean hover) {
        if (item.separator) {
            int y = r.y + r.height / 2;
            g.setColor(MenuData.BORDER_MID);
            g.drawLine(r.x + 5, y, r.x + r.width - 5, y);
            g.setColor(MenuData.BORDER_LIGHT);
            g.drawLine(r.x + 5, y + 1, r.x + r.width - 5, y + 1);
            return;
        }
        if (hover) {
            g.setColor(MenuData.HOVER_BG);
            g.fillRect(r.x, r.y, r.width, r.height);
        }
        drawIcon(g, item.icon, r.x + 7, r.y + 6, 20, hover);
        g.setFont(MenuData.FONT_NORMAL);
        g.setColor(hover ? Color.WHITE : Color.BLACK);
        g.drawString(item.text, r.x + 36, r.y + 21);
        if (item.sub != null) {
            int ax = r.x + r.width - 15;
            int ay = r.y + 11;
            Polygon p = new Polygon();
            p.addPoint(ax, ay);
            p.addPoint(ax, ay + 10);
            p.addPoint(ax + 6, ay + 5);
            g.fillPolygon(p);
        }
    }

    // ============================================================
    // Ikon manual / pixel-ish
    // ============================================================
    private void drawIcon(Graphics2D g, int icon, int x, int y, int s, boolean inverse) {
        switch (icon) {
            case MenuData.ICON_PROGRAMS: drawFolderIcon(g, x, y, s, new Color(255, 220, 80)); break;
            case MenuData.ICON_DOCUMENTS: drawDocumentIcon(g, x, y, s); break;
            case MenuData.ICON_SETTINGS: drawGearIcon(g, x, y, s); break;
            case MenuData.ICON_FIND: drawFindIcon(g, x, y, s); break;
            case MenuData.ICON_HELP: drawHelpIcon(g, x, y, s); break;
            case MenuData.ICON_RUN: drawRunIcon(g, x, y, s); break;
            case MenuData.ICON_SHUTDOWN: drawShutdownIcon(g, x, y, s); break;
            case MenuData.ICON_FOLDER: drawFolderIcon(g, x, y, s, new Color(255, 220, 80)); break;
            case MenuData.ICON_NOTEPAD: drawNotepadIcon(g, x, y, s); break;
            case MenuData.ICON_CALC: drawCalcIcon(g, x, y, s); break;
            case MenuData.ICON_PAINT: drawPaintIcon(g, x, y, s); break;
            case MenuData.ICON_CMD: drawCmdIcon(g, x, y, s); break;
            case MenuData.ICON_COMPUTER: drawComputerIcon(g, x, y, s); break;
            case MenuData.ICON_IE: drawIEIcon(g, x, y, s); break;
            case MenuData.ICON_RECYCLE: drawRecycleIcon(g, x, y, s); break;
            case MenuData.ICON_BRIEFCASE: drawBriefcaseIcon(g, x, y, s); break;
            case MenuData.ICON_PRINTER: drawPrinterIcon(g, x, y, s); break;
            case MenuData.ICON_CONTROL: drawControlPanelIcon(g, x, y, s); break;
            default: drawAppIcon(g, x, y, s); break;
        }
    }

    private void drawMiniWinLogo(Graphics2D g, int x, int y, int s) {
        g.setColor(new Color(232, 232, 232));
        g.fillOval(x - 2, y - 2, s + 4, s + 4);
        drawRaisedOval(g, x - 2, y - 2, s + 4, s + 4);
        int q = Math.max(3, s / 2 - 1);
        g.setColor(Color.RED); g.fillRect(x, y, q, q);
        g.setColor(Color.GREEN); g.fillRect(x + q + 1, y, q, q);
        g.setColor(Color.BLUE); g.fillRect(x, y + q + 1, q, q);
        g.setColor(Color.YELLOW); g.fillRect(x + q + 1, y + q + 1, q, q);
        g.setColor(Color.BLACK); g.drawRect(x - 1, y - 1, s + 1, s + 1);
    }

    private void drawDesktopIconBubble(Graphics2D g, int x, int y, int d, boolean hover) {
        Object oldAA = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(hover ? new Color(0, 0, 128, 150) : new Color(235, 245, 255, 125));
        g.fillOval(x, y, d, d);
        if (hover) drawSunkenOval(g, x, y, d, d);
        else drawRaisedOval(g, x, y, d, d);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
    }

    private void drawFolderIcon(Graphics2D g, int x, int y, int s, Color fill) {
        g.setColor(new Color(210, 160, 40));
        g.fillRect(x + 2, y + 5, s / 2, s / 4);
        g.setColor(fill);
        g.fillRect(x + 1, y + 8, s - 2, s - 9);
        g.setColor(Color.BLACK);
        g.drawRect(x + 1, y + 8, s - 2, s - 9);
        g.drawRect(x + 2, y + 5, s / 2, s / 4);
    }

    private void drawDocumentIcon(Graphics2D g, int x, int y, int s) {
        g.setColor(Color.WHITE);
        g.fillRect(x + 4, y + 1, s - 8, s - 2);
        g.setColor(Color.BLACK);
        g.drawRect(x + 4, y + 1, s - 8, s - 2);
        g.setColor(new Color(180, 180, 180));
        for (int i = 0; i < 4; i++) g.drawLine(x + 7, y + 6 + i * 4, x + s - 7, y + 6 + i * 4);
    }

    private void drawAppIcon(Graphics2D g, int x, int y, int s) {
        g.setColor(Color.WHITE);
        g.fillRect(x + 2, y + 2, s - 4, s - 4);
        g.setColor(MenuData.TITLE_BLUE);
        g.fillRect(x + 3, y + 3, s - 6, 5);
        g.setColor(Color.BLACK);
        g.drawRect(x + 2, y + 2, s - 4, s - 4);
    }

    private void drawNotepadIcon(Graphics2D g, int x, int y, int s) { drawDocumentIcon(g, x, y, s); }

    private void drawCalcIcon(Graphics2D g, int x, int y, int s) {
        g.setColor(new Color(220, 220, 220));
        g.fillRect(x + 3, y + 2, s - 6, s - 4);
        g.setColor(Color.WHITE);
        g.fillRect(x + 6, y + 5, s - 12, 5);
        g.setColor(Color.BLACK);
        g.drawRect(x + 3, y + 2, s - 6, s - 4);
        for (int r = 0; r < 3; r++) for (int c = 0; c < 3; c++) g.drawRect(x + 6 + c * 5, y + 13 + r * 5, 3, 3);
    }

    private void drawPaintIcon(Graphics2D g, int x, int y, int s) {
        g.setColor(new Color(180, 120, 60));
        g.fillOval(x + 2, y + 3, s - 4, s - 7);
        g.setColor(Color.BLACK);
        g.drawOval(x + 2, y + 3, s - 4, s - 7);
        g.setColor(Color.RED); g.fillOval(x + 7, y + 8, 4, 4);
        g.setColor(Color.BLUE); g.fillOval(x + 13, y + 7, 4, 4);
        g.setColor(Color.YELLOW); g.fillOval(x + 9, y + 14, 4, 4);
    }

    private void drawCmdIcon(Graphics2D g, int x, int y, int s) {
        g.setColor(Color.BLACK);
        g.fillRect(x + 2, y + 4, s - 4, s - 8);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Monospaced", Font.BOLD, Math.max(8, s / 2)));
        g.drawString("C>", x + 5, y + s - 7);
    }

    private void drawComputerIcon(Graphics2D g, int x, int y, int s) {
        g.setColor(new Color(0, 128, 128));
        g.fillRect(x + 3, y + 3, s - 6, s - 10);
        g.setColor(Color.WHITE);
        g.drawRect(x + 3, y + 3, s - 6, s - 10);
        g.setColor(Color.BLACK);
        g.drawRect(x + 2, y + 2, s - 4, s - 8);
        g.setColor(new Color(180, 180, 180));
        g.fillRect(x + s / 2 - 4, y + s - 7, 8, 4);
        g.fillRect(x + 6, y + s - 3, s - 12, 3);
        g.setColor(Color.BLACK);
        g.drawRect(x + 6, y + s - 3, s - 12, 3);
    }

    private void drawIEIcon(Graphics2D g, int x, int y, int s) {
        g.setColor(new Color(0, 110, 210));
        g.fillOval(x + 3, y + 3, s - 6, s - 6);
        g.setColor(new Color(0, 255, 255));
        g.drawArc(x + 1, y + 5, s - 2, s - 10, 25, 290);
        g.setColor(Color.BLACK);
        g.drawOval(x + 3, y + 3, s - 6, s - 6);
    }

    private void drawRecycleIcon(Graphics2D g, int x, int y, int s) {
        g.setColor(new Color(220, 240, 240));
        Polygon p = new Polygon();
        p.addPoint(x + 6, y + 7); p.addPoint(x + s - 6, y + 7); p.addPoint(x + s - 8, y + s - 2); p.addPoint(x + 8, y + s - 2);
        g.fillPolygon(p);
        g.setColor(Color.BLACK);
        g.drawPolygon(p);
        g.drawLine(x + 5, y + 5, x + s - 5, y + 5);
    }

    private void drawBriefcaseIcon(Graphics2D g, int x, int y, int s) {
        g.setColor(new Color(150, 90, 35));
        g.fillRect(x + 2, y + 8, s - 4, s - 10);
        g.setColor(new Color(90, 50, 20));
        g.fillRect(x + 8, y + 3, s - 16, 5);
        g.setColor(Color.BLACK);
        g.drawRect(x + 2, y + 8, s - 4, s - 10);
        g.drawRect(x + 8, y + 3, s - 16, 5);
    }

    private void drawGearIcon(Graphics2D g, int x, int y, int s) {
        g.setColor(new Color(100, 100, 100));
        g.fillOval(x + 4, y + 4, s - 8, s - 8);
        g.setColor(Color.WHITE);
        g.fillOval(x + 8, y + 8, s - 16, s - 16);
        g.setColor(Color.BLACK);
        g.drawOval(x + 4, y + 4, s - 8, s - 8);
        g.drawLine(x + s/2, y + 1, x + s/2, y + s - 1);
        g.drawLine(x + 1, y + s/2, x + s - 1, y + s/2);
    }

    private void drawFindIcon(Graphics2D g, int x, int y, int s) {
        g.setColor(Color.WHITE);
        g.fillOval(x + 3, y + 3, s - 11, s - 11);
        g.setColor(Color.BLACK);
        g.drawOval(x + 3, y + 3, s - 11, s - 11);
        g.setStroke(new BasicStroke(2));
        g.drawLine(x + s - 9, y + s - 9, x + s - 2, y + s - 2);
        g.setStroke(new BasicStroke(1));
    }

    private void drawHelpIcon(Graphics2D g, int x, int y, int s) {
        g.setColor(new Color(0, 0, 180));
        g.fillOval(x + 2, y + 2, s - 4, s - 4);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Dialog", Font.BOLD, Math.max(12, s - 4)));
        g.drawString("?", x + s / 2 - 4, y + s - 5);
    }

    private void drawRunIcon(Graphics2D g, int x, int y, int s) {
        g.setColor(Color.WHITE);
        g.fillRect(x + 2, y + 4, s - 4, s - 8);
        g.setColor(Color.BLACK);
        g.drawRect(x + 2, y + 4, s - 4, s - 8);
        g.setColor(MenuData.TITLE_BLUE);
        g.fillRect(x + 3, y + 5, s - 6, 4);
        g.setColor(Color.BLACK);
        g.drawString(">", x + 6, y + s - 6);
    }

    private void drawShutdownIcon(Graphics2D g, int x, int y, int s) {
        drawComputerIcon(g, x, y, s);
        g.setColor(Color.RED);
        g.fillOval(x + s - 8, y + s - 8, 7, 7);
    }

    private void drawPrinterIcon(Graphics2D g, int x, int y, int s) {
        g.setColor(new Color(220, 220, 220));
        g.fillRect(x + 3, y + 8, s - 6, s - 10);
        g.setColor(Color.WHITE);
        g.fillRect(x + 6, y + 2, s - 12, 9);
        g.setColor(Color.BLACK);
        g.drawRect(x + 3, y + 8, s - 6, s - 10);
        g.drawRect(x + 6, y + 2, s - 12, 9);
    }

    private void drawControlPanelIcon(Graphics2D g, int x, int y, int s) {
        g.setColor(new Color(220, 220, 220));
        g.fillRect(x + 2, y + 4, s - 4, s - 8);
        g.setColor(Color.BLACK);
        g.drawRect(x + 2, y + 4, s - 4, s - 8);
        g.setColor(Color.RED); g.fillRect(x + 5, y + 7, 5, 5);
        g.setColor(Color.GREEN); g.fillRect(x + 12, y + 7, 5, 5);
        g.setColor(Color.BLUE); g.fillRect(x + 5, y + 14, 5, 5);
        g.setColor(Color.YELLOW); g.fillRect(x + 12, y + 14, 5, 5);
    }

    private void drawRaisedOval(Graphics2D g, int x, int y, int w, int h) {
        g.setColor(MenuData.BORDER_LIGHT);
        g.drawArc(x, y, w - 1, h - 1, 45, 180);
        g.setColor(MenuData.BORDER_DARK);
        g.drawArc(x, y, w - 1, h - 1, 225, 180);
        g.setColor(MenuData.BORDER_BLACK);
        g.drawArc(x + 1, y + 1, w - 3, h - 3, 225, 180);
    }

    private void drawSunkenOval(Graphics2D g, int x, int y, int w, int h) {
        g.setColor(MenuData.BORDER_DARK);
        g.drawArc(x, y, w - 1, h - 1, 45, 180);
        g.setColor(MenuData.BORDER_BLACK);
        g.drawArc(x + 1, y + 1, w - 3, h - 3, 45, 180);
        g.setColor(MenuData.BORDER_LIGHT);
        g.drawArc(x, y, w - 1, h - 1, 225, 180);
    }

    // ============================================================
    // Border 3D klasik
    // ============================================================
    private void drawRaisedRect(Graphics2D g, int x, int y, int w, int h) {
        g.setColor(MenuData.BORDER_LIGHT);
        g.drawLine(x, y, x + w - 1, y);
        g.drawLine(x, y, x, y + h - 1);
        g.setColor(MenuData.BORDER_DARK);
        g.drawLine(x, y + h - 1, x + w - 1, y + h - 1);
        g.drawLine(x + w - 1, y, x + w - 1, y + h - 1);
        g.setColor(MenuData.BORDER_BLACK);
        g.drawLine(x + 1, y + h - 2, x + w - 2, y + h - 2);
        g.drawLine(x + w - 2, y + 1, x + w - 2, y + h - 2);
    }

    private void drawSunkenRect(Graphics2D g, int x, int y, int w, int h) {
        g.setColor(MenuData.BORDER_DARK);
        g.drawLine(x, y, x + w - 1, y);
        g.drawLine(x, y, x, y + h - 1);
        g.setColor(MenuData.BORDER_BLACK);
        g.drawLine(x + 1, y + 1, x + w - 2, y + 1);
        g.drawLine(x + 1, y + 1, x + 1, y + h - 2);
        g.setColor(MenuData.BORDER_LIGHT);
        g.drawLine(x, y + h - 1, x + w - 1, y + h - 1);
        g.drawLine(x + w - 1, y, x + w - 1, y + h - 1);
    }

    private void drawRaisedRoundRect(Graphics2D g, int x, int y, int w, int h, int arc) {
        g.setColor(MenuData.BORDER_LIGHT);
        g.drawRoundRect(x, y, w - 1, h - 1, arc, arc);
        g.drawRoundRect(x + 1, y + 1, w - 3, h - 3, Math.max(arc - 2, 8), Math.max(arc - 2, 8));
        g.setColor(MenuData.BORDER_DARK);
        g.drawArc(x + w - arc, y + h - arc, arc - 1, arc - 1, 270, 90);
        g.drawLine(x + arc / 2, y + h - 1, x + w - arc / 2, y + h - 1);
        g.drawLine(x + w - 1, y + arc / 2, x + w - 1, y + h - arc / 2);
        g.setColor(MenuData.BORDER_BLACK);
        g.drawArc(x + w - arc + 1, y + h - arc + 1, arc - 3, arc - 3, 270, 90);
    }

    private void drawSunkenRoundRect(Graphics2D g, int x, int y, int w, int h, int arc) {
        g.setColor(MenuData.BORDER_DARK);
        g.drawRoundRect(x, y, w - 1, h - 1, arc, arc);
        g.setColor(MenuData.BORDER_BLACK);
        g.drawRoundRect(x + 1, y + 1, w - 3, h - 3, Math.max(arc - 2, 8), Math.max(arc - 2, 8));
        g.setColor(MenuData.BORDER_LIGHT);
        g.drawArc(x + w - arc, y + h - arc, arc - 1, arc - 1, 270, 90);
        g.drawLine(x + arc / 2, y + h - 1, x + w - arc / 2, y + h - 1);
        g.drawLine(x + w - 1, y + arc / 2, x + w - 1, y + h - arc / 2);
    }

    // ============================================================
    // Hit testing & interaksi
    // ============================================================
    private int hitMainItem(Point p) {
        computeStartMenuLayout();
        for (int i = 0; i < mainItemRects.length; i++) if (mainItemRects[i] != null && mainItemRects[i].contains(p)) return i;
        return -1;
    }

    private int hitSubItem(Point p) {
        if (activeSub < 0) return -1;
        computeSubMenuLayout(activeSub);
        for (int i = 0; i < subItemRects.length; i++) if (subItemRects[i] != null && subItemRects[i].contains(p)) return i;
        return -1;
    }

    private int hitDesktopIcon(Point p) {
        int x = 18;
        int y = MenuData.TASKBAR_H + 18;
        int slotH = 76;
        for (int i = 0; i < MenuData.DESKTOP_ICONS.length; i++) {
            Rectangle r = new Rectangle(x - 4, y + i * slotH, 86, 66);
            if (r.contains(p)) return i;
        }
        return -1;
    }

    private void openItem(MenuData.Item item) {
        if (item == null || item.separator) return;
        startOpen = false;
        activeSub = -1;
        hoverMain = -1;
        hoverSub = -1;
        repaint();

        String t = item.text;
        if ("Run...".equals(t)) DialogHelper.showRunDialog(parentFrame);
        else if ("Shut Down...".equals(t)) DialogHelper.showShutdownDialog(parentFrame);
        else if ("Help".equals(t)) DialogHelper.showClassicMessage(parentFrame, "Windows Help", "This is a Windows 95 style help window.");
        else openSimulatedWindow(t);
    }

    private boolean isInsideOpenMenus(Point p) {
        computeStartMenuLayout();
        if (startMenuRect.contains(p)) return true;
        if (activeSub >= 0) {
            computeSubMenuLayout(activeSub);
            if (subMenuRect.contains(p)) return true;
        }
        return false;
    }

    private void handleMouseMove(Point p) {
        hoverStart = startButtonRect().contains(p);
        hoverDesktopIcon = hitDesktopIcon(p);
        hoverTaskButton = -1;
        for (int i = 0; i < taskRects.size(); i++) if (taskRects.get(i).contains(p)) hoverTaskButton = i;

        if (!startOpen) {
            hoverMain = -1;
            hoverSub = -1;
            activeSub = -1;
            repaint();
            return;
        }

        int oldMain = hoverMain;
        int oldSub = hoverSub;
        int oldActive = activeSub;
        int subHit = hitSubItem(p);
        if (subHit >= 0) {
            hoverSub = subHit;
        } else {
            hoverSub = -1;
            int mainHit = hitMainItem(p);
            if (mainHit >= 0) {
                MenuData.Item item = MenuData.MAIN_MENU[mainHit];
                if (!item.separator) {
                    hoverMain = mainHit;
                    activeSub = item.sub != null ? mainHit : -1;
                    if (activeSub >= 0) computeSubMenuLayout(activeSub);
                }
            } else if (!isInsideOpenMenus(p)) {
                hoverMain = -1;
                activeSub = -1;
            }
        }
        if (oldMain != hoverMain || oldSub != hoverSub || oldActive != activeSub) repaint();
        else repaint();
    }

    private void handleMousePress(Point p, int clickCount) {
        requestFocus();

        if (startButtonRect().contains(p)) {
            startOpen = !startOpen;
            hoverMain = -1;
            hoverSub = -1;
            activeSub = -1;
            repaint();
            return;
        }

        // Taskbar buttons: restore/minimize/activate.
        if (p.y >= taskbarY() && p.y < taskbarY() + MenuData.TASKBAR_H) {
            for (int i = 0; i < quickRects.length; i++) {
                if (quickRects[i] != null && quickRects[i].contains(p)) {
                    if (i == 0) openSimulatedWindow("Internet Explorer");
                    if (i == 1) openSimulatedWindow("Windows Explorer");
                    if (i == 2) openSimulatedWindow("MS-DOS Prompt");
                    return;
                }
            }
            handleTaskButtonClick(p);
            return;
        }

        if (startOpen) {
            int subHit = hitSubItem(p);
            if (subHit >= 0 && activeSub >= 0) {
                MenuData.Item item = MenuData.MAIN_MENU[activeSub].sub[subHit];
                if (!item.separator) openItem(item);
                return;
            }
            int mainHit = hitMainItem(p);
            if (mainHit >= 0) {
                MenuData.Item item = MenuData.MAIN_MENU[mainHit];
                if (item.separator) return;
                hoverMain = mainHit;
                if (item.sub != null) {
                    activeSub = mainHit;
                    computeSubMenuLayout(activeSub);
                    repaint();
                } else openItem(item);
                return;
            }
            if (!isInsideOpenMenus(p)) {
                startOpen = false;
                hoverMain = -1;
                hoverSub = -1;
                activeSub = -1;
                repaint();
            }
            return;
        }

        AppWindow hit = topWindowAt(p);
        if (hit != null) {
            bringToFront(hit);
            if (closeButtonRect(hit).contains(p)) { closeWindow(hit); return; }
            if (maxButtonRect(hit).contains(p)) { toggleMaximize(hit); return; }
            if (minButtonRect(hit).contains(p)) { minimizeWindow(hit); return; }
            if (hit.type.equals("welcome") && welcomeBodyCloseRect(hit).contains(p)) { closeWindow(hit); return; }
            if (titleBarRect(hit).contains(p) && !hit.maximized) {
                draggingWindow = hit;
                dragOffsetX = p.x - hit.bounds.x;
                dragOffsetY = p.y - hit.bounds.y;
            }
            repaint();
            return;
        }

        int deskIcon = hitDesktopIcon(p);
        if (deskIcon >= 0 && clickCount >= 2) {
            String name = MenuData.DESKTOP_ICONS[deskIcon][0].replace('\n', ' ');
            openSimulatedWindow(name);
        }
    }

    private void handleMouseDrag(Point p) {
        if (draggingWindow != null && !draggingWindow.maximized) {
            int nx = p.x - dragOffsetX;
            int ny = p.y - dragOffsetY;
            int minY = MenuData.TASKBAR_H + 2;
            int maxY = getHeight() - 35;
            if (ny < minY) ny = minY;
            if (ny > maxY) ny = maxY;
            if (nx < -draggingWindow.bounds.width + 80) nx = -draggingWindow.bounds.width + 80;
            if (nx > getWidth() - 40) nx = getWidth() - 40;
            draggingWindow.bounds.x = nx;
            draggingWindow.bounds.y = ny;
            repaint();
        }
    }

    private void handleKeyPress(int key) {
        if (key == Event.ESCAPE || key == 27) {
            startOpen = false;
            hoverMain = -1;
            hoverSub = -1;
            activeSub = -1;
            repaint();
        } else if (key == 's' || key == 'S') {
            startOpen = !startOpen;
            hoverMain = -1;
            hoverSub = -1;
            activeSub = -1;
            repaint();
        }
    }

    // ============================================================
    // Event model lama AWT 1.0
    // Tidak menggunakan package event listener modern
    // ============================================================
    public boolean mouseMove(Event evt, int x, int y) {
        handleMouseMove(new Point(x, y));
        return true;
    }

    public boolean mouseDown(Event evt, int x, int y) {
        handleMousePress(new Point(x, y), evt.clickCount);
        return true;
    }

    public boolean mouseDrag(Event evt, int x, int y) {
        handleMouseDrag(new Point(x, y));
        return true;
    }

    public boolean mouseUp(Event evt, int x, int y) {
        draggingWindow = null;
        return true;
    }

    public boolean mouseEnter(Event evt, int x, int y) {
        requestFocus();
        return true;
    }

    public boolean mouseExit(Event evt, int x, int y) {
        hoverStart = false;
        repaint();
        return true;
    }

    public boolean keyDown(Event evt, int key) {
        handleKeyPress(key);
        return true;
    }

}