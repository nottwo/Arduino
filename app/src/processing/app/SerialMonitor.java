/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 2 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package processing.app;

import cc.arduino.packages.BoardPort;
import processing.app.legacy.PApplet;

import de.mud.terminal.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static processing.app.I18n._;

@SuppressWarnings("serial")
public class SerialMonitor extends AbstractMonitor {

  private final String port;
  private Serial serial;
  private int serialRate;

  public SerialMonitor(BoardPort port) {
    super(port.getLabel());

    this.port = port.getAddress();

    vt = new vt320(80, 24) {
        public void write(byte[] b) {
            if(serial != null) {
                serial.write(new String(b));
            }
        }

        public void beep() {
        }

        public void sendTelnetCommand(byte cmd) {
        }

        public void setWindowSize(int c, int r) {
        }
    };

    vt.setLocalEcho(true);
    terminal.setVDUBuffer(vt);

    serialRate = Preferences.getInteger("serial.debug_rate");
    serialRates.setSelectedItem(serialRate + " " + _("baud"));
    onSerialRateChange(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        String wholeString = (String) serialRates.getSelectedItem();
        String rateString = wholeString.substring(0, wholeString.indexOf(' '));
        serialRate = Integer.parseInt(rateString);
        Preferences.set("serial.debug_rate", rateString);
        try {
          close();
          Thread.sleep(100); // Wait for serial port to properly close
          open();
        } catch (InterruptedException e) {
          // noop
        } catch (Exception e) {
          System.err.println(e);
        }
      }
    });

  }

  private void send(String s) {
    if (serial != null) {
      serial.write(s);
    }
  }

  public void open() throws Exception {
    if (serial != null) return;

    serial = new Serial(port, serialRate) {
      @Override
      protected void message(char buff[], int n) {
        addToUpdateBuffer(buff, n);
      }
    };
  }

  public void close() throws Exception {
    if (serial != null) {
      int[] location = getPlacement();
      String locationStr = PApplet.join(PApplet.str(location), ",");
      Preferences.set("last.serial.location", locationStr);
      serial.dispose();
      serial = null;
    }
  }
  
}
