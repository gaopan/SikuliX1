/*
 * Copyright (c) 2010-2018, sikuli.org, sikulix.com - MIT license
 */
package org.sikuli.natives;

import org.sikuli.basics.Debug;
import org.sikuli.script.*;
import org.sikuli.util.ProcessRunner;

import java.awt.Rectangle;
import java.awt.Window;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WinUtil implements OSUtil {

  @Override
  public void checkFeatureAvailability() {
    RunTime.loadLibrary("WinUtil");
  }

  @Override
  public App getApp(App app) {
    if (app.getPID() > 0) {
      app = getTaskByPID(app);
    } else {
      app = getTaskByName(app);
    }
    return app;
  }

  //<editor-fold desc="old getApp">
/*
  @Override
  public App getApp(App app) {
    if (app.getPID() == 0) {
      return app;
    }
    Object filter;
    if (appPID < 0) {
      filter = appName;
    } else {
      filter = appPID;
    }
    String name = "";
    String execName = "";
    String options = "";
    Integer pid = -1;
    String[] parts;
    if (filter instanceof String) {
      name = (String) filter;
      if (name.startsWith("!")) {
        name = name.substring(1);
        execName = name;
      } else {
        if (name.startsWith("\"")) {
          parts = name.substring(1).split("\"");
          if (parts.length > 1) {
            options = name.substring(parts[0].length() + 3);
            name = "\"" + parts[0] + "\"";
          }
        } else {
          parts = name.split(" -- ");
          if (parts.length > 1) {
            options = parts[1];
            name = parts[0];
          }
        }
        if (name.startsWith("\"")) {
          execName = new File(name.substring(1, name.length() - 1)).getName().toUpperCase();
        } else {
          execName = new File(name).getName().toUpperCase();
        }
      }
    } else if (filter instanceof Integer) {
      pid = (Integer) filter;
    } else {
      return app;
    }
    Debug.log(3, "WinUtil.getApp: %s", filter);
    String cmd;
    if (pid < 0) {
      cmd = cmd = "!tasklist /V /FO CSV /NH /FI \"SESSIONNAME eq Console\"";
    } else {
      cmd = cmd = "!tasklist /V /FO CSV /NH /FI \"PID eq " + pid.toString() + "\"";
    }
    String result = RunTime.get().runcmd(cmd);
    String[] lines = result.split("\r\n");
    if ("0".equals(lines[0].trim())) {
      for (int nl = 1; nl < lines.length; nl++) {
        parts = lines[nl].split("\"");
        if (parts.length < 2) {
          continue;
        }
        String theWindow = parts[parts.length - 1];
        String theName = parts[1];
        String thePID = parts[3];
        //Debug.log(3, "WinUtil.getApp: %s:%s(%s)", thePID, theName, theWindow);
        if (!name.isEmpty()) {
          if ((theName.toUpperCase().contains(execName) && !theWindow.contains("N/A"))
                  || theWindow.contains(name)) {
            return new App.AppEntry(theName, thePID, theWindow, "", "");
          }
        } else {
          try {
            if (Integer.parseInt(thePID) == pid) {
              return new App.AppEntry(theName, thePID, theWindow, "", "");
            }
          } catch (Exception ex) {
          }
        }
      }
    } else {
      Debug.logp(result);
    }
    if (!options.isEmpty()) {
      return new App.AppEntry(name, "", "", "", options);
    }
    if (app == null) {
      List<String> theApp = getTaskByName(execName);
      if (theApp.size() > 0) {
        app = new App.AppEntry(theApp.get(0), theApp.get(1), theApp.get(2), "", "");
      }
    }
    return app;
  }
*/
  //</editor-fold>

  private static App getTaskByName(App app) {
    String cmd = String.format("!tasklist /V /FO CSV /NH /FI \"IMAGENAME eq %s\"", app.getName());
    String sysout = RunTime.get().runcmd(cmd);
    String[] lines = sysout.split("\r\n");
    String[] parts = null;
    app.reset();
    if ("0".equals(lines[0].trim())) {
      for (int n = 1; n < lines.length; n++) {
        parts = lines[n].split("\"");
        if (parts.length < 2) {
          continue;
        }
        if (parts[parts.length - 1].contains("N/A")) continue;
        app.setPID(parts[3]);
        app.setWindow(parts[parts.length - 1]);
        break;
      }
    }
    return app;
  }

  private static App getTaskByPID(App app) {
    if (!app.isValid()) {
      return app;
    }
    String[] name_pid_window = evalTaskByPID(app.getPID());
    if (name_pid_window[1].isEmpty()) {
      app.reset();
    } else {
      app.setWindow(name_pid_window[2]);
    }
    return app;
  }

  private static String[] evalTaskByPID(int pid) {
    String cmd = String.format("!tasklist /V /FO CSV /NH /FI \"PID eq %d\"", pid);
    String sysout = RunTime.get().runcmd(cmd);
    String[] lines = sysout.split("\r\n");
    String[] parts = null;
    if ("0".equals(lines[0].trim())) {
      for (int n = 1; n < lines.length; n++) {
        parts = lines[n].split("\"");
        if (parts.length < 2) {
          continue;
        }
        return new String[]{parts[1], "pid", parts[parts.length - 1]}; //name, window
      }
    }
    return new String[]{"", "", ""};
  }

  private static App getTaskByWindow(String title) {
    App app = new App();
    return app;
  }

  @Override
  public Map<Integer, String[]> getApps(String name) {
    Map<Integer, String[]> apps = new HashMap<Integer, String[]>();
    String cmd;
    if (name == null || name.isEmpty()) {
      cmd = cmd = "!tasklist /V /FO CSV /NH /FI \"SESSIONNAME eq Console\"";
    } else {
      cmd = String.format("!tasklist /V /FO CSV /NH /FI \"IMAGENAME eq %s\"", name);
    }
    String result = RunTime.get().runcmd(cmd);
    String[] lines = result.split("\r\n");
    if ("0".equals(lines[0].trim())) {
      for (int nl = 1; nl < lines.length; nl++) {
        String[] parts = lines[nl].split("\"");
        if (parts.length < 3) {
          continue;
        }
        String theWindow = parts[parts.length - 1];
        String thePID = parts[3];
        String theName = parts[1];
        Integer pid = -1;
        try {
          pid = Integer.parseInt(thePID);
        } catch (Exception ex) {
        }
        if (pid != -1) {
          if (theWindow.contains("N/A")) {
            pid = -pid;
          }
          apps.put(pid, new String[]{theName, theWindow});
        }
      }
    } else {
      Debug.logp(result);
    }
    return apps;
  }

  @Override
  public App open(App app) {
    if (app.isValid()) {
      int ret = switchApp(app.getPID(), 0);
    } else {
      String cmd = app.getExec();
      if (!app.getOptions().isEmpty()) {
        start(cmd, app.getOptions());
      } else {
        start(cmd);
      }
    }
    return app;
  }

  private int start(String... cmd) {
    return ProcessRunner.startApp(cmd);
  }

  @Override
  public App switchto(App app, int num) {
    if (!app.isValid()) {
      return app;
    }
//    int ret = switchApp(app.getPID(), num);
    int ret = switchApp(app.getWindow(), 0);
    return app;
  }

  @Override
  public App switchto(String appName) {
    App app = new App();
    int pid = switchApp(appName, 0);
    if (pid > 0) {
      app.setPID(pid);
      String[] name_pid_window = evalTaskByPID(pid);
      app.setName(name_pid_window[0]);
      app.setWindow(name_pid_window[2]);
    }
    return app;
  }

  @Override
  public App close(App app) {
    if (closeApp(app.getPID()) == 0) {
      app.reset();
    }
    return app;
  }

  @Override
  public Rectangle getWindow(App app) {
    return getWindow(app, 0);
  }

  @Override
  public Rectangle getWindow(App app, int winNum) {
    getApp(app);
    if (!app.isValid()) {
      return new Rectangle();
    }
    return getWindow(app.getPID(), winNum);
  }

  @Override
  public Rectangle getWindow(String title) {
    return getWindow(title, 0);
  }

  private Rectangle getWindow(String title, int winNum) {
    long hwnd = getHwnd(title, winNum);
    return _getWindow(hwnd, winNum);
  }

  private Rectangle getWindow(int pid, int winNum) {
    long hwnd = getHwnd(pid, winNum);
    return _getWindow(hwnd, winNum);
  }

  private Rectangle _getWindow(long hwnd, int winNum) {
    Rectangle rect = getRegion(hwnd, winNum);
    return rect;
  }

  @Override
  public Rectangle getFocusedWindow() {
    Rectangle rect = getFocusedRegion();
    return rect;
  }

  @Override
  public List<Region> getWindows(App app) {
    return new ArrayList<>();
  }

  public native int switchApp(String appName, int num);

  public native int switchApp(int pid, int num);

  public native int openApp(String appName);

  public native int closeApp(String appName);

  public native int closeApp(int pid);

  @Override
  public native void bringWindowToFront(Window win, boolean ignoreMouse);

  private static native long getHwnd(String appName, int winNum);

  private static native long getHwnd(int pid, int winNum);

  private static native Rectangle getRegion(long hwnd, int winNum);

  private static native Rectangle getFocusedRegion();
}
