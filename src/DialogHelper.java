import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Event;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.Window;

/**
 * Dialog klasik untuk Run, Shut Down, dan pesan sederhana.
 *
 * Versi ini sengaja TIDAK memakai package event listener modern.
 * Interaksi tombol dan window close memakai event model lama AWT:
 * - handleEvent(Event e)
 * - action(Event e, Object arg)
 * - mouseDown / keyDown pada Window sleep
 */
public class DialogHelper {
    public static void showRunDialog(final Frame owner) {
        class RunDialog extends Dialog {
            TextField input;
            Button ok;
            Button cancel;
            Button browse;

            RunDialog(Frame f) {
                super(f, "Run", true);
                setLayout(new BorderLayout(8, 8));
                setSize(420, 170);
                setBackground(MenuData.WINDOW_BG);

                Panel body = new Panel(new BorderLayout(8, 8));
                body.setBackground(MenuData.WINDOW_BG);
                body.add(new Label("Type the name of a program, folder, document, or Internet resource."), BorderLayout.NORTH);

                Panel row = new Panel(new BorderLayout(6, 0));
                row.setBackground(MenuData.WINDOW_BG);
                row.add(new Label("Open:"), BorderLayout.WEST);
                input = new TextField("notepad", 32);
                row.add(input, BorderLayout.CENTER);
                body.add(row, BorderLayout.CENTER);

                Panel buttons = new Panel(new FlowLayout(FlowLayout.RIGHT, 6, 4));
                buttons.setBackground(MenuData.WINDOW_BG);
                ok = new Button("OK");
                cancel = new Button("Cancel");
                browse = new Button("Browse...");
                buttons.add(ok);
                buttons.add(cancel);
                buttons.add(browse);
                body.add(buttons, BorderLayout.SOUTH);

                add(body, BorderLayout.CENTER);
                setLocationRelativeTo(f);
            }

            public boolean action(Event e, Object arg) {
                if (e.target == ok || e.target == cancel || e.target == browse) {
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

        RunDialog d = new RunDialog(owner);
        d.setVisible(true);
    }

    public static void showClassicMessage(final Frame owner, String title, String message) {
        class MessageDialog extends Dialog {
            Button ok;

            MessageDialog(Frame f) {
                super(f, title, true);
                setLayout(new BorderLayout(8, 8));
                setSize(360, 140);
                setBackground(MenuData.WINDOW_BG);
                add(new Label(message, Label.CENTER), BorderLayout.CENTER);

                Panel buttons = new Panel(new FlowLayout(FlowLayout.RIGHT));
                buttons.setBackground(MenuData.WINDOW_BG);
                ok = new Button("OK");
                buttons.add(ok);
                add(buttons, BorderLayout.SOUTH);

                setLocationRelativeTo(f);
            }

            public boolean action(Event e, Object arg) {
                if (e.target == ok) {
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

        MessageDialog d = new MessageDialog(owner);
        d.setVisible(true);
    }

    public static void showShutdownDialog(final Frame owner) {
        class ShutdownDialog extends Dialog {
            Checkbox chkShutdown;
            Checkbox chkRestart;
            Checkbox chkSleep;
            Button ok;
            Button cancel;

            ShutdownDialog(Frame f) {
                super(f, "Shut Down Windows", true);
                setLayout(new BorderLayout(8, 8));
                setSize(360, 220);
                setBackground(MenuData.WINDOW_BG);

                Panel body = new Panel(new BorderLayout(6, 6));
                body.setBackground(MenuData.WINDOW_BG);
                body.add(new Label("What do you want the computer to do?"), BorderLayout.NORTH);

                CheckboxGroup cbg = new CheckboxGroup();
                Panel choices = new Panel(new FlowLayout(FlowLayout.LEFT, 12, 4));
                choices.setBackground(MenuData.WINDOW_BG);
                chkShutdown = new Checkbox("Shut down", cbg, true);
                chkRestart = new Checkbox("Restart", cbg, false);
                chkSleep = new Checkbox("Sleep", cbg, false);
                choices.add(chkShutdown);
                choices.add(chkRestart);
                choices.add(chkSleep);
                body.add(choices, BorderLayout.CENTER);

                Panel buttons = new Panel(new FlowLayout(FlowLayout.RIGHT));
                buttons.setBackground(MenuData.WINDOW_BG);
                ok = new Button("OK");
                cancel = new Button("Cancel");
                buttons.add(ok);
                buttons.add(cancel);
                body.add(buttons, BorderLayout.SOUTH);

                add(body, BorderLayout.CENTER);
                setLocationRelativeTo(f);
            }

            public boolean action(Event e, Object arg) {
                if (e.target == cancel) {
                    dispose();
                    return true;
                }
                if (e.target == ok) {
                    boolean restart = chkRestart.getState();
                    boolean sleep = chkSleep.getState();
                    dispose();

                    if (sleep) activateSleepMode(owner);
                    else if (restart) {
                        owner.dispose();
                        StartMenu app = new StartMenu();
                        app.setVisible(true);
                        app.requestFocusOnDesktop();
                    } else {
                        System.exit(0);
                    }
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

        ShutdownDialog d = new ShutdownDialog(owner);
        d.setVisible(true);
    }

    private static void activateSleepMode(Frame owner) {
        class SleepWindow extends Window {
            SleepWindow(Frame f) {
                super(f);
                setBackground(Color.BLACK);
                setBounds(f.getBounds());
            }

            public void paint(java.awt.Graphics g) {
                g.setColor(Color.WHITE);
                g.setFont(MenuData.FONT_BOLD);
                g.drawString("SLEEP MODE - klik atau tekan tombol untuk kembali", 80, getHeight() / 2);
            }

            public boolean mouseDown(Event e, int x, int y) {
                dispose();
                return true;
            }

            public boolean keyDown(Event e, int key) {
                dispose();
                return true;
            }
        }

        SleepWindow sleep = new SleepWindow(owner);
        sleep.setVisible(true);
        sleep.requestFocus();
    }
}
