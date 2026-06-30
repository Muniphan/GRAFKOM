import java.awt.Color;
import java.awt.Font;

/**
 * Data tema dan struktur menu untuk tiruan Start Menu Windows 95.
 * Sengaja memakai warna klasik dan font standar Java/AWT.
 */
public class MenuData {
    public static final int WIN_W = 1024;
    public static final int WIN_H = 768;
    public static final int TASKBAR_H = 40;
    public static final int START_W = 74;
    public static final int START_H = 32;

    public static final int MENU_W = 258;
    public static final int MENU_STRIP_W = 42;
    public static final int MENU_ITEM_H = 32;
    public static final int MENU_SEP_H = 9;
    public static final int SUBMENU_W = 218;

    public static final Color DESKTOP_BG = new Color(0, 128, 128);
    public static final Color TASKBAR_BG = new Color(192, 192, 192);
    public static final Color WIN95_GRAY = new Color(192, 192, 192);
    public static final Color BORDER_LIGHT = Color.WHITE;
    public static final Color BORDER_MID = new Color(128, 128, 128);
    public static final Color BORDER_DARK = new Color(64, 64, 64);
    public static final Color BORDER_BLACK = Color.BLACK;
    public static final Color HOVER_BG = new Color(0, 0, 128);
    public static final Color STRIP_BG = new Color(0, 0, 128);
    public static final Color TITLE_BLUE = new Color(0, 0, 128);
    public static final Color TITLE_INACTIVE = new Color(128, 128, 128);
    public static final Color WINDOW_BG = new Color(192, 192, 192);
    public static final Color WINDOW_WHITE = Color.WHITE;
    public static final Color LINK_BLUE = new Color(0, 0, 160);

    public static final Font FONT_NORMAL = new Font("Dialog", Font.PLAIN, 12);
    public static final Font FONT_BOLD = new Font("Dialog", Font.BOLD, 12);
    public static final Font FONT_SMALL = new Font("Dialog", Font.PLAIN, 11);
    public static final Font FONT_TITLE = new Font("Dialog", Font.BOLD, 12);
    public static final Font FONT_STRIP = new Font("Dialog", Font.BOLD, 20);
    public static final Font FONT_DESKTOP = new Font("Dialog", Font.PLAIN, 11);

    public static final int ICON_PROGRAMS = 1;
    public static final int ICON_DOCUMENTS = 2;
    public static final int ICON_SETTINGS = 3;
    public static final int ICON_FIND = 4;
    public static final int ICON_HELP = 5;
    public static final int ICON_RUN = 6;
    public static final int ICON_SHUTDOWN = 7;
    public static final int ICON_FOLDER = 8;
    public static final int ICON_APP = 9;
    public static final int ICON_NOTEPAD = 10;
    public static final int ICON_CALC = 11;
    public static final int ICON_PAINT = 12;
    public static final int ICON_CMD = 13;
    public static final int ICON_COMPUTER = 14;
    public static final int ICON_IE = 15;
    public static final int ICON_RECYCLE = 16;
    public static final int ICON_BRIEFCASE = 17;
    public static final int ICON_PRINTER = 18;
    public static final int ICON_CONTROL = 19;

    public static class Item {
        public final String text;
        public final int icon;
        public final boolean separator;
        public final Item[] sub;

        public Item(String text, int icon) {
            this.text = text;
            this.icon = icon;
            this.separator = false;
            this.sub = null;
        }

        public Item(String text, int icon, Item[] sub) {
            this.text = text;
            this.icon = icon;
            this.separator = false;
            this.sub = sub;
        }

        private Item() {
            this.text = "-";
            this.icon = 0;
            this.separator = true;
            this.sub = null;
        }
    }

    public static Item sep() { return new Item(); }

    public static final Item[] PROGRAMS_MENU = new Item[] {
            new Item("Accessories", ICON_FOLDER),
            new Item("Games", ICON_FOLDER),
            new Item("StartUp", ICON_FOLDER),
            sep(),
            new Item("Windows Explorer", ICON_FOLDER),
            new Item("Internet Explorer", ICON_IE),
            new Item("MS-DOS Prompt", ICON_CMD),
            new Item("Notepad", ICON_NOTEPAD),
            new Item("Calculator", ICON_CALC),
            new Item("Paint", ICON_PAINT),
            new Item("MS Paint Effect", ICON_PAINT)
    };

    public static final Item[] DOCUMENTS_MENU = new Item[] {
            new Item("My Documents", ICON_DOCUMENTS),
            new Item("Readme.txt", ICON_NOTEPAD),
            new Item("Project Report.doc", ICON_DOCUMENTS),
            new Item("Recent File 1", ICON_DOCUMENTS),
            new Item("Recent File 2", ICON_DOCUMENTS)
    };

    public static final Item[] SETTINGS_MENU = new Item[] {
            new Item("Control Panel", ICON_CONTROL),
            new Item("Printers", ICON_PRINTER),
            new Item("Taskbar & Start Menu", ICON_SETTINGS),
            new Item("Folder Options", ICON_FOLDER)
    };

    public static final Item[] FIND_MENU = new Item[] {
            new Item("Files or Folders...", ICON_FIND),
            new Item("Computer...", ICON_COMPUTER),
            new Item("On The Internet", ICON_IE)
    };

    public static final Item[] MAIN_MENU = new Item[] {
            new Item("Programs", ICON_PROGRAMS, PROGRAMS_MENU),
            new Item("Documents", ICON_DOCUMENTS, DOCUMENTS_MENU),
            new Item("Settings", ICON_SETTINGS, SETTINGS_MENU),
            new Item("Find", ICON_FIND, FIND_MENU),
            new Item("Help", ICON_HELP),
            new Item("Run...", ICON_RUN),
            sep(),
            new Item("Shut Down...", ICON_SHUTDOWN)
    };

    public static final String[][] DESKTOP_ICONS = {
            {"My Computer", "14"},
            {"Network\nNeighborhood", "14"},
            {"Inbox", "2"},
            {"Recycle Bin", "16"},
            {"The Microsoft\nNetwork", "15"},
            {"My Briefcase", "17"}
    };
}
