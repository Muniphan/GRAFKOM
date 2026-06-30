import java.awt.*;
import java.awt.event.*;

public class DesktopPanel extends Panel implements MouseListener, MouseMotionListener, KeyListener {
    private boolean startMenuOpen = false;
    private boolean programsMenuOpen = false;
    private int hoveredMainMenu = -1;
    private int hoveredProgramMenu = -1;
    private Frame parentFrame;
    private Image offscreen;
    private Graphics2D offg2d;

    public DesktopPanel(Frame parent) {
        this.parentFrame = parent;
        setBackground(MenuData.DESKTOP_BG);
        addMouseListener(this);
        addMouseMotionListener(this);
        addKeyListener(this);
        setFocusable(true);
    }

    // Double buffering untuk mencegah AWT berkedip
    @Override
    public void update(Graphics g) {
        paint(g);
    }

    @Override
    public void paint(Graphics g) {
        if (offscreen == null || offscreen.getWidth(this) != getWidth() || offscreen.getHeight(this) != getHeight()) {
            offscreen = createImage(getWidth(), getHeight());
            offg2d = (Graphics2D) offscreen.getGraphics();
        }

        // Render Desktop Teal
        offg2d.setColor(MenuData.DESKTOP_BG);
        offg2d.fillRect(0, 0, getWidth(), getHeight());

        drawTaskbar(offg2d);

        if (startMenuOpen) {
            drawStartMenu(offg2d);
            if (programsMenuOpen) {
                drawProgramsMenu(offg2d);
            }
        }

        g.drawImage(offscreen, 0, 0, this);
    }

    private void drawTaskbar(Graphics2D g2) {
        int y = getHeight() - 40;
        g2.setColor(MenuData.TASKBAR_BG);
        g2.fillRect(0, y, getWidth(), 40);
        drawRaisedRect(g2, 0, y, getWidth() - 1, 39);

        // Tombol Start (Sunken jika ditekan)
        if (startMenuOpen) {
            drawSunkenRect(g2, 2, y + 4, 70, 32);
        } else {
            drawRaisedRect(g2, 2, y + 4, 70, 32);
        }

        g2.setColor(Color.BLACK);
        g2.setFont(MenuData.CLASSIC_FONT);
        g2.drawString("Start", 30, y + 25);

        // Logo Windows Manual (Bukan Emoji)
        g2.setColor(Color.RED); g2.fillRect(10, y + 10, 6, 6);
        g2.setColor(Color.GREEN); g2.fillRect(17, y + 10, 6, 6);
        g2.setColor(Color.BLUE); g2.fillRect(10, y + 17, 6, 6);
        g2.setColor(Color.YELLOW); g2.fillRect(17, y + 17, 6, 6);
    }

    private void drawStartMenu(Graphics2D g2) {
        int x = 2, width = 200;
        int height = MenuData.MAIN_MENU.length * 30 + 10;
        int y = getHeight() - 40 - height;

        g2.setColor(MenuData.TASKBAR_BG);
        g2.fillRect(x, y, width, height);
        drawRaisedRect(g2, x, y, width, height);

        // Strip Vertikal Kiri
        g2.setColor(MenuData.STRIP_BG);
        g2.fillRect(x + 2, y + 2, 30, height - 4);
        g2.setColor(Color.WHITE);
        g2.setFont(MenuData.STRIP_FONT);

        Graphics2D g2Rotated = (Graphics2D) g2.create();
        g2Rotated.translate(x + 20, y + height - 10);
        g2Rotated.rotate(-Math.PI / 2);
        g2Rotated.drawString("Windows 5", 0, 0);
        g2Rotated.dispose();

        g2.setFont(MenuData.CLASSIC_FONT);
        for (int i = 0; i < MenuData.MAIN_MENU.length; i++) {
            int itemY = y + 5 + (i * 30);
            if (i == hoveredMainMenu) {
                g2.setColor(MenuData.HOVER_BG);
                g2.fillRect(x + 32, itemY, width - 34, 30);
                g2.setColor(Color.WHITE);
            } else {
                g2.setColor(Color.BLACK);
            }
            g2.drawString(MenuData.MAIN_MENU[i], x + 60, itemY + 20);

            // Segitiga Manual untuk submenu Programs
            if (i == 0) {
                int[] px = {x + width - 15, x + width - 15, x + width - 5};
                int[] py = {itemY + 10, itemY + 20, itemY + 15};
                g2.fillPolygon(px, py, 3);
            }
        }
    }

    private void drawProgramsMenu(Graphics2D g2) {
        int x = 202, width = 180;
        int height = MenuData.PROGRAMS_MENU.length * 30 + 10;
        int y = getHeight() - 40 - (MenuData.MAIN_MENU.length * 30 + 10);

        g2.setColor(MenuData.TASKBAR_BG);
        g2.fillRect(x, y, width, height);
        drawRaisedRect(g2, x, y, width, height);

        g2.setFont(MenuData.CLASSIC_FONT);
        for (int i = 0; i < MenuData.PROGRAMS_MENU.length; i++) {
            int itemY = y + 5 + (i * 30);
            if (i == hoveredProgramMenu) {
                g2.setColor(MenuData.HOVER_BG);
                g2.fillRect(x + 2, itemY, width - 4, 30);
                g2.setColor(Color.WHITE);
            } else {
                g2.setColor(Color.BLACK);
            }
            g2.drawString(MenuData.PROGRAMS_MENU[i], x + 40, itemY + 20);
        }
    }

    private void drawRaisedRect(Graphics2D g, int x, int y, int w, int h) {
        g.setColor(MenuData.BORDER_LIGHT);
        g.drawLine(x, y, x + w, y);
        g.drawLine(x, y, x, y + h);
        g.setColor(MenuData.BORDER_DARK);
        g.drawLine(x, y + h, x + w, y + h);
        g.drawLine(x + w, y, x + w, y + h);
    }

    private void drawSunkenRect(Graphics2D g, int x, int y, int w, int h) {
        g.setColor(MenuData.BORDER_DARK);
        g.drawLine(x, y, x + w, y);
        g.drawLine(x, y, x, y + h);
        g.setColor(MenuData.BORDER_LIGHT);
        g.drawLine(x, y + h, x + w, y + h);
        g.drawLine(x + w, y, x + w, y + h);
    }

    public void mousePressed(MouseEvent e) {
        int mx = e.getX(), my = e.getY();
        int taskbarY = getHeight() - 40;

        if (my >= taskbarY && mx >= 2 && mx <= 72) {
            startMenuOpen = !startMenuOpen;
            programsMenuOpen = false;
            repaint();
            return;
        }

        if (startMenuOpen) {
            int smY = taskbarY - (MenuData.MAIN_MENU.length * 30 + 10);

            if (mx >= 2 && mx <= 202 && my >= smY && my <= smY + (MenuData.MAIN_MENU.length * 30 + 10)) {
                if (hoveredMainMenu == 5) DialogHelper.showRunDialog(parentFrame);
                if (hoveredMainMenu == 6) DialogHelper.showShutdownDialog(parentFrame);
                startMenuOpen = false;
                repaint();
                return;
            }

            if (programsMenuOpen && mx >= 202 && mx <= 382 && my >= smY && my <= smY + (MenuData.PROGRAMS_MENU.length * 30 + 10)) {
                if (hoveredProgramMenu != -1) {
                    AppFactory.openApp(MenuData.PROGRAMS_MENU[hoveredProgramMenu], parentFrame);
                }
                startMenuOpen = false;
                programsMenuOpen = false;
                repaint();
                return;
            }
        }

        startMenuOpen = false;
        programsMenuOpen = false;
        repaint();
    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    public void mouseMoved(MouseEvent e) {
        if (!startMenuOpen) return;

        int mx = e.getX(), my = e.getY();
        int smY = getHeight() - 40 - (MenuData.MAIN_MENU.length * 30 + 10);
        boolean changed = false;

        if (mx >= 32 && mx <= 200 && my >= smY && my <= smY + (MenuData.MAIN_MENU.length * 30)) {
            int newHover = (my - smY - 5) / 30;
            if (newHover >= 0 && newHover < MenuData.MAIN_MENU.length && newHover != hoveredMainMenu) {
                hoveredMainMenu = newHover;
                programsMenuOpen = (hoveredMainMenu == 0);
                changed = true;
            }
        } else if (mx < 202 && hoveredMainMenu != -1) {
            hoveredMainMenu = -1;
            changed = true;
        }

        if (programsMenuOpen) {
            if (mx >= 202 && mx <= 382 && my >= smY && my <= smY + (MenuData.PROGRAMS_MENU.length * 30)) {
                int newPHover = (my - smY - 5) / 30;
                if (newPHover >= 0 && newPHover < MenuData.PROGRAMS_MENU.length && newPHover != hoveredProgramMenu) {
                    hoveredProgramMenu = newPHover;
                    changed = true;
                }
            } else if (hoveredProgramMenu != -1) {
                hoveredProgramMenu = -1;
                changed = true;
            }
        }
        if (changed) repaint();
    }

    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE && startMenuOpen) {
            startMenuOpen = false; programsMenuOpen = false; repaint();
        } else if (e.getKeyCode() == KeyEvent.VK_S) {
            startMenuOpen = !startMenuOpen; programsMenuOpen = false; repaint();
        }
    }
    public void keyReleased(KeyEvent e) {} public void keyTyped(KeyEvent e) {}
    public void mouseClicked(MouseEvent e) {} public void mouseEntered(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {} public void mouseDragged(MouseEvent e) {}
}