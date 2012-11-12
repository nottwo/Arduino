package processing.app;

import static processing.app.I18n._;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

import de.mud.terminal.*;

import processing.app.legacy.PApplet;

@SuppressWarnings("serial")
public abstract class AbstractMonitor extends JFrame implements ActionListener {

  protected JComboBox serialRates;

  protected SwingTerminal terminal;
  protected vt320 vt;

  private Timer updateTimer;
  private StringBuffer updateBuffer;

  public AbstractMonitor(String title) {
    super(title);

    addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent event) {
        try {
          close();
        } catch (Exception e) {
          // ignore
        }
      }
    });

    // obvious, no?
    KeyStroke wc = Editor.WINDOW_CLOSE_KEYSTROKE;
    getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(wc, "close");
    getRootPane().getActionMap().put("close", (new AbstractAction() {
      public void actionPerformed(ActionEvent event) {
        try {
          close();
        } catch (Exception e) {
          // ignore
        }
        setVisible(false);
      }
    }));

    getContentPane().setLayout(new BorderLayout());

    Font consoleFont = Theme.getFont("console.font");
    Font editorFont = Preferences.getFont("editor.font");
    Font font = new Font(consoleFont.getName(), consoleFont.getStyle(), editorFont.getSize());

    terminal = new SwingTerminal(new VDUBuffer(), font);
    getContentPane().add(terminal, BorderLayout.CENTER);

    final JPanel pane = new JPanel();
    pane.setLayout(new BoxLayout(pane, BoxLayout.X_AXIS));
    pane.setBorder(new EmptyBorder(4, 4, 4, 4));

    String[] serialRateStrings = {
            "300", "1200", "2400", "4800", "9600",
            "19200", "38400", "57600", "115200"
    };

    serialRates = new JComboBox();
    for (String rate : serialRateStrings) {
      serialRates.addItem(rate + " " + _("baud"));
    }

    serialRates.setMaximumSize(serialRates.getMinimumSize());

    pane.add(serialRates);

    this.setMinimumSize(new Dimension(pane.getMinimumSize().width, this.getPreferredSize().height));

    getContentPane().add(pane, BorderLayout.SOUTH);

    pack();

    Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
    if (Preferences.get("last.screen.height") != null) {
      // if screen size has changed, the window coordinates no longer
      // make sense, so don't use them unless they're identical
      int screenW = Preferences.getInteger("last.screen.width");
      int screenH = Preferences.getInteger("last.screen.height");
      if ((screen.width == screenW) && (screen.height == screenH)) {
        String locationStr = Preferences.get("last.serial.location");
        if (locationStr != null) {
          int[] location = PApplet.parseInt(PApplet.split(locationStr, ','));
          setPlacement(location);
        }
      }
    }
    
    updateBuffer = new StringBuffer(1048576);
    updateTimer = new Timer(33, this);  // redraw serial monitor at 30 Hz
    updateTimer.start();
  }

  public void onSerialRateChange(ActionListener listener) {
    serialRates.addActionListener(listener);
  }

  public void onSendCommand(ActionListener listener) {
  }

  protected void setPlacement(int[] location) {
    setBounds(location[0], location[1], location[2], location[3]);
  }

  protected int[] getPlacement() {
    int[] location = new int[4];

    // Get the dimensions of the Frame
    Rectangle bounds = getBounds();
    location[0] = bounds.x;
    location[1] = bounds.y;
    location[2] = bounds.width;
    location[3] = bounds.height;

    return location;
  }

  public void message(final String s) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        vt.putString(s);
      }
    });
  }

  public boolean requiresAuthorization() {
    return false;
  }

  public String getAuthorizationKey() {
    return null;
  }

  public abstract void open() throws Exception;

  public abstract void close() throws Exception;
  
  public synchronized void addToUpdateBuffer(char buff[], int n) {
    updateBuffer.append(buff, 0, n);
  }

  private synchronized String consumeUpdateBuffer() {
    String s = updateBuffer.toString();
    updateBuffer.setLength(0);
    return s;
  }

  public void actionPerformed(ActionEvent e) {
    final String s = consumeUpdateBuffer();
    if (s.length() > 0) {
      //System.out.println("gui append " + s.length());
        vt.putString(s);
    }
  }

}
