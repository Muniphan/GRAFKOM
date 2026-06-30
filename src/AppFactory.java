import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Event;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.TextArea;
import java.awt.TextField;


public class AppFactory {
    public static void openApp(String appName, Frame owner) {
        if (appName == null) return;
        if (appName.equals("Run...")) {
            DialogHelper.showRunDialog(owner);
            return;
        }
        if (appName.equals("MS-DOS Prompt")) appName = "Command Prompt";
        if (appName.equals("My Computer") || appName.equals("Computer...") || appName.equals("Windows Explorer")) appName = "Windows Explorer";
        if (appName.equals("My Documents") || appName.equals("Documents")) appName = "My Documents";
        if (appName.equals("Control Panel") || appName.equals("Printers") || appName.equals("Taskbar & Start Menu") || appName.equals("Folder Options")) {
            appName = "Control Panel";
        }

        class AppDialog extends Dialog {
            Button closeButton;
            String title;

            AppDialog(Frame f, String t) {
                super(f, t, false);
                title = t;
                setSize(t.equals("Calculator") ? 260 : 620, t.equals("Calculator") ? 330 : 430);
                setLayout(new BorderLayout());
                setBackground(MenuData.WINDOW_BG);

                Panel titleBar = new Panel(new BorderLayout());
                titleBar.setBackground(MenuData.TITLE_BLUE);
                Label lbl = new Label("  " + t);
                lbl.setFont(MenuData.FONT_TITLE);
                lbl.setForeground(Color.WHITE);
                titleBar.add(lbl, BorderLayout.CENTER);

                Panel buttons = new Panel(new FlowLayout(FlowLayout.RIGHT, 2, 2));
                buttons.setBackground(MenuData.TITLE_BLUE);
                closeButton = new Button("X");
                closeButton.setFont(MenuData.FONT_SMALL);
                closeButton.setBackground(MenuData.WIN95_GRAY);
                closeButton.setForeground(Color.BLACK);
                buttons.add(closeButton);
                titleBar.add(buttons, BorderLayout.EAST);

                add(titleBar, BorderLayout.NORTH);
                add(makeContent(t), BorderLayout.CENTER);
                add(makeStatusBar(t), BorderLayout.SOUTH);
                setLocationRelativeTo(f);
            }

            public boolean action(Event e, Object arg) {
                if (e.target == closeButton) {
                    dispose();
                    return true;
                }
                return super.action(e, arg);
            }

            public boolean handleEvent(Event e) {
                if (e.id == Event.WINDOW_DESTROY) {
                    dispose();
                    return true;
                }
                return super.handleEvent(e);
            }
        }

        AppDialog d = new AppDialog(owner, appName);
        d.setVisible(true);
    }

    private static Panel makeStatusBar(String appName) {
        Panel status = new Panel(new BorderLayout());
        status.setBackground(MenuData.WINDOW_BG);
        Label l = new Label("  " + appName + " - Ready");
        l.setFont(MenuData.FONT_SMALL);
        status.add(l, BorderLayout.CENTER);
        return status;
    }

    private static Panel makeContent(String appName) {
        if (appName.equals("Notepad") || appName.equals("Readme.txt") || appName.endsWith(".txt")) return makeNotepad();
        if (appName.equals("Calculator")) return makeCalculator();
        if (appName.equals("Paint")) return makePaint();
        if (appName.equals("Command Prompt")) return makeCommandPrompt();
        if (appName.equals("Internet Explorer") || appName.equals("On The Internet")) return makeInternetExplorer();
        if (appName.equals("Windows Explorer") || appName.equals("My Documents") || appName.equals("My Computer") || appName.equals("Network Neighborhood")) return makeExplorer(appName);
        if (appName.equals("Control Panel")) return makeControlPanel();
        if (appName.equals("Recycle Bin")) return makeExplorer("Recycle Bin");
        if (appName.equals("The Microsoft Network")) return makeInternetExplorer();

        Panel p = new Panel(new BorderLayout());
        p.setBackground(MenuData.WINDOW_BG);
        p.add(new Label(appName + " - Dummy Application", Label.CENTER), BorderLayout.CENTER);
        return p;
    }

    private static Panel makeNotepad() {
        Panel p = new Panel(new BorderLayout());
        p.setBackground(MenuData.WINDOW_BG);
        Panel menu = new Panel(new FlowLayout(FlowLayout.LEFT, 12, 2));
        menu.setBackground(MenuData.WINDOW_BG);
        menu.add(new Label("File"));
        menu.add(new Label("Edit"));
        menu.add(new Label("Search"));
        menu.add(new Label("Help"));
        p.add(menu, BorderLayout.NORTH);
        TextArea ta = new TextArea("Welcome to Windows 95 Notepad.\n\nThis is a simple dummy text editor.", 12, 60);
        ta.setFont(new Font("Monospaced", Font.PLAIN, 12));
        p.add(ta, BorderLayout.CENTER);
        return p;
    }

    private static Panel makeCalculator() {
        Panel p = new Panel(new BorderLayout(4, 4));
        p.setBackground(MenuData.WINDOW_BG);
        TextField display = new TextField("0");
        display.setEditable(false);
        display.setFont(new Font("Dialog", Font.BOLD, 20));
        p.add(display, BorderLayout.NORTH);

        Panel grid = new Panel(new GridLayout(5, 4, 3, 3));
        String[] buttons = {"7", "8", "9", "/", "4", "5", "6", "*", "1", "2", "3", "-", "0", ".", "=", "+", "C", "CE", "%", "sqrt"};
        for (int i = 0; i < buttons.length; i++) grid.add(new Button(buttons[i]));
        p.add(grid, BorderLayout.CENTER);
        return p;
    }

    private static Panel makePaint() {
        Panel p = new Panel(new BorderLayout());
        p.setBackground(MenuData.WINDOW_BG);
        Panel tools = new Panel(new FlowLayout(FlowLayout.LEFT, 4, 3));
        tools.setBackground(MenuData.WINDOW_BG);
        String[] names = {"Pencil", "Brush", "Line", "Rect", "Fill", "Eraser"};
        for (int i = 0; i < names.length; i++) tools.add(new Button(names[i]));
        p.add(tools, BorderLayout.NORTH);

        Panel canvas = new Panel() {
            public void paint(java.awt.Graphics g) {
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(Color.BLACK);
                g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
                g.setColor(Color.RED); g.drawOval(60, 45, 120, 80);
                g.setColor(Color.BLUE); g.drawLine(40, 160, 280, 70);
                g.setColor(Color.GREEN); g.drawRect(250, 120, 120, 90);
            }
        };
        p.add(canvas, BorderLayout.CENTER);
        return p;
    }

    private static Panel makeCommandPrompt() {
        Panel p = new Panel(new BorderLayout());
        TextArea area = new TextArea("Microsoft(R) Windows 95\n   (C)Copyright Microsoft Corp 1981-1995.\n\nC:\\WINDOWS>dir\nCOMMAND.COM\nEXPLORER.EXE\nNOTEPAD.EXE\n\nC:\\WINDOWS>", 18, 60);
        area.setFont(new Font("Monospaced", Font.PLAIN, 12));
        area.setBackground(Color.BLACK);
        area.setForeground(Color.WHITE);
        p.add(area, BorderLayout.CENTER);
        return p;
    }

    private static Panel makeInternetExplorer() {
        Panel p = new Panel(new BorderLayout());
        p.setBackground(MenuData.WINDOW_BG);
        Label logo = new Label("Microsoft Internet Explorer", Label.CENTER);
        logo.setFont(new Font("Dialog", Font.BOLD, 24));
        logo.setForeground(MenuData.TITLE_BLUE);
        p.add(logo, BorderLayout.NORTH);
        p.add(new Label("Welcome to the Internet - Windows 95 style browser simulation", Label.CENTER), BorderLayout.CENTER);
        return p;
    }

    private static Panel makeExplorer(String title) {
        Panel p = new Panel(new BorderLayout());
        p.setBackground(MenuData.WINDOW_BG);

        Panel top = new Panel(new BorderLayout());
        top.setBackground(MenuData.WINDOW_BG);
        Panel menu = new Panel(new FlowLayout(FlowLayout.LEFT, 12, 2));
        menu.setBackground(MenuData.WINDOW_BG);
        String[] menus = {"File", "Edit", "View", "Go", "Favorites", "Tools", "Help"};
        for (int i = 0; i < menus.length; i++) menu.add(new Label(menus[i]));
        top.add(menu, BorderLayout.NORTH);
        p.add(top, BorderLayout.NORTH);

        Panel split = new Panel(new GridLayout(1, 2));
        split.setBackground(Color.WHITE);
        List left = new List();
        left.add("[-] Desktop");
        left.add("    [-] My Computer");
        left.add("        3½ Floppy (A:)");
        left.add("        (C:)");
        left.add("        (D:)");
        left.add("        Printers");
        left.add("        Control Panel");
        left.add("    My Documents");
        left.add("    Network Neighborhood");
        left.add("    Recycle Bin");

        List right = new List();
        if (title.equals("Recycle Bin")) {
            right.add("Deleted Document.txt");
            right.add("Old Shortcut.lnk");
        } else if (title.equals("My Documents")) {
            right.add("Project Report.doc");
            right.add("Readme.txt");
            right.add("Images");
            right.add("Final Project");
        } else {
            right.add("3½ Floppy (A:)");
            right.add("(C:)");
            right.add("(D:)");
            right.add("Printers");
            right.add("Control Panel");
            right.add("Dial-Up Networking");
            right.add("Scheduled Tasks");
            right.add("Web Folders");
        }
        split.add(left);
        split.add(right);
        p.add(split, BorderLayout.CENTER);
        return p;
    }

    private static Panel makeControlPanel() {
        Panel p = new Panel(new BorderLayout());
        p.setBackground(MenuData.WINDOW_BG);
        List list = new List();
        list.add("Add/Remove Programs");
        list.add("Display");
        list.add("Fonts");
        list.add("Keyboard");
        list.add("Mouse");
        list.add("Network");
        list.add("Printers");
        list.add("System");
        p.add(list, BorderLayout.CENTER);
        return p;
    }
}
