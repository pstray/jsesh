/*
 * @(#)OSXApplication.java
 *
 * Copyright (c) 1996-2010 by the original authors of JHotDraw and all its
 * contributors. All rights reserved.
 *
 * You may not use, copy or modify this file, except in compliance with the 
 * license agreement you entered into with the copyright holders. For details
 * see accompanying license terms.
 */
package org.jhotdraw_7_6.app;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.net.URI;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Stack;
import java.util.prefs.Preferences;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRootPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.jhotdraw_7_6.app.action.ActionUtil;
import org.jhotdraw_7_6.app.action.app.AboutAction;
import org.jhotdraw_7_6.app.action.app.AbstractPreferencesAction;
import org.jhotdraw_7_6.app.action.app.ExitAction;
import org.jhotdraw_7_6.app.action.app.OpenApplicationAction;
import org.jhotdraw_7_6.app.action.app.OpenApplicationFileAction;
import org.jhotdraw_7_6.app.action.app.PrintApplicationFileAction;
import org.jhotdraw_7_6.app.action.app.ReOpenApplicationAction;
import org.jhotdraw_7_6.app.action.edit.AbstractFindAction;
import org.jhotdraw_7_6.app.action.edit.ClearSelectionAction;
import org.jhotdraw_7_6.app.action.edit.CopyAction;
import org.jhotdraw_7_6.app.action.edit.CutAction;
import org.jhotdraw_7_6.app.action.edit.DeleteAction;
import org.jhotdraw_7_6.app.action.edit.DuplicateAction;
import org.jhotdraw_7_6.app.action.edit.PasteAction;
import org.jhotdraw_7_6.app.action.edit.RedoAction;
import org.jhotdraw_7_6.app.action.edit.SelectAllAction;
import org.jhotdraw_7_6.app.action.edit.UndoAction;
import org.jhotdraw_7_6.app.action.file.ClearFileAction;
import org.jhotdraw_7_6.app.action.file.ClearRecentFilesMenuAction;
import org.jhotdraw_7_6.app.action.file.CloseFileAction;
import org.jhotdraw_7_6.app.action.file.ExportFileAction;
import org.jhotdraw_7_6.app.action.file.LoadDirectoryAction;
import org.jhotdraw_7_6.app.action.file.LoadFileAction;
import org.jhotdraw_7_6.app.action.file.NewFileAction;
import org.jhotdraw_7_6.app.action.file.NewWindowAction;
import org.jhotdraw_7_6.app.action.file.OpenDirectoryAction;
import org.jhotdraw_7_6.app.action.file.OpenFileAction;
import org.jhotdraw_7_6.app.action.file.PrintFileAction;
import org.jhotdraw_7_6.app.action.file.SaveFileAction;
import org.jhotdraw_7_6.app.action.file.SaveFileAsAction;
import org.jhotdraw_7_6.app.action.window.FocusWindowAction;
import org.jhotdraw_7_6.app.action.window.MaximizeWindowAction;
import org.jhotdraw_7_6.app.action.window.MinimizeWindowAction;
import org.jhotdraw_7_6.app.action.window.TogglePaletteAction;
import org.jhotdraw_7_6.app.osx.OSXAdapter;
import org.jhotdraw_7_6.app.osx.OSXPaletteHandler;
import org.jhotdraw_7_6.gui.Worker;
import org.jhotdraw_7_6.net.URIUtil;
import org.jhotdraw_7_6.util.ResourceBundleUtil;
import org.jhotdraw_7_6.util.prefs.PreferencesUtil;

/**
 * {@code OSXApplication} handles the lifecycle of multiple {@link View}s using
 * a Mac OS X application interface.
 * <p>
 * This user interface created by this application follows the guidelines given
 * in the
 * <a href="http://developer.apple.com/mac/library/documentation/UserExperience/Conceptual/AppleHIGuidelines/"
 * >Apple Human Interface Guidelines</a>.
 * <p>
 * An application of this type can open multiple {@link View}s. Each view is
 * shown in a separate {@code JFrame}.
 * <p>
 * Conceptually all views share a global 'screen menu bar'. In Swing this is
 * implemented as multiple JMenuBar instances. There is one JMenuBar for
 * each opened JFrame, and a special JMenuBar which is shown when all views of
 * the application are closed.
 * <p>
 * The application also provides floating toolbars and palette windows for the
 * views.
 * <p>
 * In order for the screen menu bar and the floating palettes to function
 * properly, it is essential that all code which opens JFrame's, JDialog's or
 * JWindow's calls addWindow/Palette and removeWindow/Palette on the application
 * object.
 * <p>
 * The life cycle of the application is tied to the screen menu bar. Choosing
 * the quit action in the screen menu bar quits the application.
 * <p>
 * The screen menu bar has the following standard menus:
 * <pre>
 * "Application-Name" &nbsp; File &nbsp; Edit &nbsp; Window</pre>
 *
 * The first menu, is the <b>application menu</b>. It has the following standard
 * menu items: 
 * <pre>
 *  About "Application-Name" ({@link AboutAction#ID})
 *  -
 *  Preferences... ({@link AbstractPreferencesAction#ID})
 *  -
 *  Services
 *  -
 *  Hide "Application-Name"
 *  Hide Others
 *  Show All
 *  -
 *  Quit "Application-Name" ({@link ExitAction#ID})
 * </pre>
 *
 * The <b>file menu</b> has the following standard menu items:
 * <pre>
 *  Clear ({@link ClearFileAction#ID}})
 *  New ({@link NewFileAction#ID}})
 *  New Window ({@link NewWindowAction#ID}})
 *  Load... ({@link LoadFileAction#ID}})
 *  Open... ({@link OpenFileAction#ID}})
 *  Load Directory... ({@link LoadDirectoryAction#ID}})
 *  Open Directory... ({@link OpenDirectoryAction#ID}})
 *  Load Recent &gt; "Filename" ({@link org.jhotdraw_7_6.app.action.file.LoadRecentFileAction#ID})
 *  Open Recent &gt; "Filename" ({@link org.jhotdraw_7_6.app.action.file.OpenRecentFileAction#ID})
 *  -
 *  Close ({@link CloseFileAction#ID})
 *  Save ({@link SaveFileAction#ID})
 *  Save As... ({@link SaveFileAsAction#ID})
 *  Export... ({@link ExportFileAction#ID})
 *  Print... ({@link PrintFileAction#ID})
 * </pre>
 *
 * The <b>edit menu</b> has the following standard menu items:
 * <pre>
 *  Undo ({@link UndoAction#ID}})
 *  Redo ({@link RedoAction#ID}})
 *  -
 *  Cut ({@link CutAction#ID}})
 *  Copy ({@link CopyAction#ID}})
 *  Paste ({@link PasteAction#ID}})
 *  Duplicate ({@link DuplicateAction#ID}})
 *  Delete... ({@link DeleteAction#ID}})
 *  -
 *  Select All ({@link SelectAllAction#ID}})
 *  Clear Selection ({@link ClearSelectionAction#ID}})
 *  -
 *  Find ({@link AbstractFindAction#ID}})
 * </pre>
 *
 * The <b>window menu</b> has the following standard menu items:
 * <pre>
 *  Minimize ({@link MinimizeWindowAction#ID})
 *  Zoom ({@link MaximizeWindowAction#ID})
 *  -
 *  "Filename" ({@link FocusWindowAction})
 *  -
 *  "Palette" ({@link TogglePaletteAction})
 * </pre>
 *
 * The menus provided by the {@code ApplicationModel} are inserted between
 * the file menu and the window menu. In case the application model supplies
 * a menu with the title "Help", it is inserted after the window menu.
 *
 * @author Werner Randelshofer
 * @version $Id: OSXApplication.java 717 2010-11-21 12:30:57Z rawcoder $
 */
public class OSXApplication extends AbstractApplication {

    private OSXPaletteHandler paletteHandler;
    private Preferences prefs;
    private LinkedList<Action> paletteActions;
    /** The "invisible" frame is used to hold the frameless menu bar on Mac OS X.
     */
    private JFrame invisibleFrame;

    /** Creates a new instance. */
    public OSXApplication() {
    }

    
    public void init() {
        super.init();
        ResourceBundleUtil.putPropertyNameModifier("os", "mac", "default");
        prefs = PreferencesUtil.userNodeForPackage((getModel() == null) ? getClass() : getModel().getClass());
        initLookAndFeel();
        paletteHandler = new OSXPaletteHandler(this);

        initLabels();

        paletteActions = new LinkedList<Action>();
        setActionMap(createModelActionMap(model));
        initPalettes(paletteActions);
        initScreenMenuBar();
        model.initApplication(this);
    }

    
    public void launch(String[] args) {
        System.setProperty("apple.awt.graphics.UseQuartz", "false");
        super.launch(args);
    }

    
    public void configure(String[] args) {
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("com.apple.macos.useScreenMenuBar", "true");
    }

    protected void initLookAndFeel() {
        try {
            UIManager.setLookAndFeel("ch.randelshofer.quaqua.QuaquaLookAndFeel");
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (UIManager.getString("OptionPane.css") == null) {
            UIManager.put("OptionPane.css", "<head>"
                    + "<style type=\"text/css\">"
                    + "b { font: 13pt \"Dialog\" }"
                    + "p { font: 11pt \"Dialog\"; margin-top: 8px }"
                    + "</style>"
                    + "</head>");
        }
    }

    
    public void dispose(View p) {
        FocusWindowAction a = (FocusWindowAction) getAction(p, FocusWindowAction.ID);
        if (a != null) {
            a.dispose();
        }
        super.dispose(p);
    }

    
    public void addPalette(Window palette) {
        paletteHandler.addPalette(palette);
    }

    
    public void removePalette(Window palette) {
        paletteHandler.removePalette(palette);
    }

    
    public void addWindow(Window window, final View view) {
        if (window instanceof JFrame) {
            ((JFrame) window).setJMenuBar(createMenuBar(view));
        } else if (window instanceof JDialog) {
            // ((JDialog) window).setJMenuBar(createMenuBar(null));
        }

        paletteHandler.add(window, view);
    }

    
    public void removeWindow(Window window) {
        if (window instanceof JFrame) {
            
            // Unlink all menu items from action objects
            JMenuBar mb = ((JFrame) window).getJMenuBar();
            Stack<JMenu> s = new Stack<JMenu>();
            for (int i = 0, n = mb.getMenuCount(); i < n; ++i) {
                if (mb.getMenu(i) != null) {
                    s.push(mb.getMenu(i));
                }
            }
            while (!s.isEmpty()) {
                JPopupMenu m = s.pop().getPopupMenu();
                for (int i = 0, n = m.getComponentCount(); i < n; ++i) {
                    if (m.getComponent(i) instanceof JMenu) {
                        s.push((JMenu) m.getComponent(i));
                    } else if (m.getComponent(i) instanceof AbstractButton) {
                        ((AbstractButton) m.getComponent(i)).setAction(null);
                    }
                }
            }
            // We explicitly set the JMenuBar to null to facilitate garbage
            // collection
            ((JFrame) window).setJMenuBar(null);
        }
        paletteHandler.remove(window);
    }

    
    public void show(View view) {
        if (!view.isShowing()) {
            view.setShowing(true);
            JFrame f = new JFrame();
            f.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            f.setSize(new Dimension(600, 400));
            updateViewTitle(view, f);

            PreferencesUtil.installFramePrefsHandler(prefs, "view", f);
            Point loc = f.getLocation();
            boolean moved;
            do {
                moved = false;
                for (Iterator i = views().iterator(); i.hasNext();) {
                    View aView = (View) i.next();
                    if (aView != view && aView.isShowing()
                            && SwingUtilities.getWindowAncestor(aView.getComponent()).
                            getLocation().equals(loc)) {
                        loc.x += 22;
                        loc.y += 22;
                        moved = true;
                        break;
                    }
                }
            } while (moved);
            f.setLocation(loc);

            new FrameHandler(f, view);
            addWindow(f, view);

            f.getContentPane().add(view.getComponent());
            f.setVisible(true);
            view.start();
        }
    }

    /**
     * Updates the title of a view and displays it in the given frame.
     * 
     * @param v The view.
     * @param f The frame.
     */
    protected void updateViewTitle(View v, JFrame f) {
        String title;
        URI uri = v.getURI();
        if (uri == null) {
            title = labels.getString("unnamedFile");
        } else {
            title = URIUtil.getName(uri);
        }
        v.setTitle(labels.getFormatted("frame.title", title, getName(), v.getMultipleOpenId()));
        f.setTitle(v.getTitle());

        // Adds a proxy icon for the file to the title bar
        // See http://developer.apple.com/technotes/tn2007/tn2196.html#WINDOW_DOCUMENTFILE
        if (uri != null && uri.getScheme() != null && uri.getScheme().equals("file")) {
            f.getRootPane().putClientProperty("Window.documentFile", new File(uri));
        } else {
            f.getRootPane().putClientProperty("Window.documentFile", null);
        }
    }

    
    public void hide(View p) {
        if (p.isShowing()) {
            JFrame f = (JFrame) SwingUtilities.getWindowAncestor(p.getComponent());
            if (getActiveView() == p) {
                setActiveView(null);
            }
            f.setVisible(false);
            removeWindow(f);
            f.remove(p.getComponent());
            f.dispose();
        }
    }

    /**
     * Creates a menu bar.
     */
    protected JMenuBar createMenuBar( View v) {
        JMenuBar mb = new JMenuBar();

        // Get menus from application model
        JMenu fileMenu = null;
        JMenu editMenu = null;
        JMenu helpMenu = null;
        JMenu viewMenu = null;
        JMenu windowMenu = null;
        String fileMenuText = labels.getString("file.text");
        String editMenuText = labels.getString("edit.text");
        String viewMenuText = labels.getString("view.text");
        String windowMenuText = labels.getString("window.text");
        String helpMenuText = labels.getString("help.text");
        LinkedList<JMenu> ll = new LinkedList<JMenu>();
        getModel().getMenuBuilder().addOtherMenus(ll, this, v);
        for (JMenu mm : ll) {
            String text = mm.getText();
            if (text == null) {
                mm.setText("-null-");
            } else if (text.equals(fileMenuText)) {
                fileMenu = mm;
                continue;
            } else if (text.equals(editMenuText)) {
                editMenu = mm;
                continue;
            } else if (text.equals(viewMenuText)) {
                viewMenu = mm;
                continue;
            } else if (text.equals(windowMenuText)) {
                windowMenu = mm;
                continue;
            } else if (text.equals(helpMenuText)) {
                helpMenu = mm;
                continue;
            }
            mb.add(mm);
        }

        // Create missing standard menus
        if (fileMenu == null) {
            fileMenu = createFileMenu(v);
        }
        if (editMenu == null) {
            editMenu = createEditMenu(v);
        }
        if (viewMenu == null) {
            viewMenu = createViewMenu(v);
        }
        if (windowMenu == null) {
            windowMenu = createWindowMenu(v);
        }
        if (helpMenu == null) {
            helpMenu = createHelpMenu(v);
        }

        // Insert standard menus into menu bar
        if (fileMenu != null) {
            mb.add(fileMenu, 0);
        }
        if (editMenu != null) {
            mb.add(editMenu, Math.min(1, mb.getComponentCount()));
        }
        if (viewMenu != null) {
            mb.add(viewMenu, Math.min(2, mb.getComponentCount()));
        }
        if (windowMenu != null) {
            mb.add(windowMenu);
        }
        if (helpMenu != null) {
            mb.add(helpMenu);
        }

        return mb;
    }

    
    
    public JMenu createViewMenu(final View view) {
        JMenu m = new JMenu();
        labels.configureMenu(m, "view");

        MenuBuilder mb = model.getMenuBuilder();
        mb.addOtherViewItems(m, this, view);

        return (m.getItemCount() > 0) ? m : null;
    }

    
    public JMenu createWindowMenu(View view) {
        JMenu m;
        JMenuItem mi;

        m = new JMenu();
        JMenu windowMenu = m;
        labels.configureMenu(m, "window");
        m.addSeparator();

        MenuBuilder mb = model.getMenuBuilder();
        mb.addOtherWindowItems(m, this, view);

        new WindowMenuHandler(windowMenu, view);


        return (m.getItemCount() == 0) ? null : m;
    }

    
    
    public JMenu createFileMenu(View view) {
        JMenu m;

        m = new JMenu();
        labels.configureMenu(m, "file");
        MenuBuilder mb = model.getMenuBuilder();
        mb.addClearFileItems(m, this, view);
        mb.addNewFileItems(m, this, view);
        mb.addNewWindowItems(m, this, view);

        mb.addLoadFileItems(m, this, view);
        mb.addOpenFileItems(m, this, view);

        if (getAction(view, LoadFileAction.ID) != null ||//
                getAction(view, OpenFileAction.ID) != null ||//
                getAction(view, LoadDirectoryAction.ID) != null ||//
                getAction(view, OpenDirectoryAction.ID) != null) {
            m.add(createOpenRecentFileMenu(view));
        }
        maybeAddSeparator(m);

        mb.addCloseFileItems(m, this, view);
        mb.addSaveFileItems(m, this, view);
        mb.addExportFileItems(m, this, view);
        mb.addPrintFileItems(m, this, view);

        mb.addOtherFileItems(m, this, view);

        return (m.getItemCount() == 0) ? null : m;
    }

    
    
    public JMenu createEditMenu(View view) {

        JMenu m;
        JMenuItem mi;
        Action a;
        m = new JMenu();
        labels.configureMenu(m, "edit");
        MenuBuilder mb = model.getMenuBuilder();
        mb.addUndoItems(m, this, view);
        maybeAddSeparator(m);
        mb.addClipboardItems(m, this, view);
        maybeAddSeparator(m);
        mb.addSelectionItems(m, this, view);
        maybeAddSeparator(m);
        mb.addFindItems(m, this, view);
        maybeAddSeparator(m);
        mb.addOtherEditItems(m, this, view);

        return (m.getItemCount() == 0) ? null : m;
    }

    
    public JMenu createHelpMenu(View view) {
        JMenu m = new JMenu();
        labels.configureMenu(m, "help");

        MenuBuilder mb = model.getMenuBuilder();
        mb.addHelpItems(m, this, view);

        return (m.getItemCount() == 0) ? null : m;
    }

    protected void initScreenMenuBar() {
        setScreenMenuBar(createMenuBar(null));
        paletteHandler.add((JFrame) getComponent(), null);

        Action a;
        if (null != (a = getAction(null, OpenApplicationAction.ID))) {
            OSXAdapter.setOpenApplicationHandler(a);
        }
        if (null != (a = getAction(null, ReOpenApplicationAction.ID))) {
            OSXAdapter.setReOpenApplicationHandler(a);
        }
        if (null != (a = getAction(null, OpenApplicationFileAction.ID))) {
            OSXAdapter.setOpenFileHandler(a);
        }
        if (null != (a = getAction(null, PrintApplicationFileAction.ID))) {
            OSXAdapter.setPrintFileHandler(a);
        }
        if (null != (a = getAction(null, AboutAction.ID))) {
            OSXAdapter.setAboutHandler(a);
        }
        if (null != (a = getAction(null, AbstractPreferencesAction.ID))) {
            OSXAdapter.setPreferencesHandler(a);
        }
        if (null != (a = getAction(null, ExitAction.ID))) {
            OSXAdapter.setQuitHandler(a);
        }
    }

    protected void initPalettes(final LinkedList<Action> paletteActions) {
        SwingUtilities.invokeLater(new Worker<LinkedList<JFrame>>() {

            
            public LinkedList<JFrame> construct() {
                LinkedList<JFrame> palettes = new LinkedList<JFrame>();
                LinkedList<JToolBar> toolBars = new LinkedList<JToolBar>(getModel().createToolBars(OSXApplication.this, null));

                int i = 0;
                int x = 0;
                for (JToolBar tb : toolBars) {
                    i++;
                    tb.setFloatable(false);
                    tb.setOrientation(JToolBar.VERTICAL);
                    tb.setFocusable(false);

                    JFrame d = new JFrame();

                    // Note: Client properties must be set before heavy-weight
                    // peers are created
                    d.getRootPane().putClientProperty("Window.style", "small");
                    d.getRootPane().putClientProperty("Quaqua.RootPane.isVertical", Boolean.FALSE);
                    d.getRootPane().putClientProperty("Quaqua.RootPane.isPalette", Boolean.TRUE);

                    d.setFocusable(false);
                    d.setResizable(false);
                    d.getContentPane().setLayout(new BorderLayout());
                    d.getContentPane().add(tb, BorderLayout.CENTER);
                    d.setAlwaysOnTop(true);
                    d.setUndecorated(true);
                    d.getRootPane().setWindowDecorationStyle(JRootPane.FRAME);
                    d.getRootPane().setFont(
                            new Font("Lucida Grande", Font.PLAIN, 11));

                    d.setJMenuBar(createMenuBar(null));

                    d.pack();
                    d.setFocusableWindowState(false);
                    PreferencesUtil.installPalettePrefsHandler(prefs, "toolbar." + i, d, x);
                    x += d.getWidth();

                    TogglePaletteAction tpa = new TogglePaletteAction(OSXApplication.this, d, tb.getName());
                    palettes.add(d);
                    if (prefs.getBoolean("toolbar." + i + ".visible", true)) {
                        addPalette(d);
                        tpa.putValue(ActionUtil.SELECTED_KEY, true);
                    }
                    paletteActions.add(tpa);
                }
                return palettes;

            }

            
            protected void done(LinkedList<JFrame> result) {
                @SuppressWarnings("unchecked")
                LinkedList<JFrame> palettes = (LinkedList<JFrame>) result;
                if (palettes != null) {
                    /*for (JFrame p : palettes) {
                    if (prefs.getBoolean("toolbar.", true))
                    addPalette(p);
                    }*/
                    firePropertyChange("paletteCount", 0, palettes.size());
                }
            }
        });
    }

    
    public boolean isSharingToolsAmongViews() {
        return true;
    }

    /** Returns the Frame which holds the frameless JMenuBar.
     */
    
    public Component getComponent() {
        if (invisibleFrame == null) {
            invisibleFrame = new JFrame();
            invisibleFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            invisibleFrame.setUndecorated(true);
            // Move it way off screen
            invisibleFrame.setLocation(10000, 10000);
            // make the frame transparent and shadowless
            // see https://developer.apple.com/mac/library/technotes/tn2007/tn2196.html
            invisibleFrame.getRootPane().putClientProperty("Window.alpha", 0f);
            invisibleFrame.getRootPane().putClientProperty("Window.shadow", false);
            // make it visible, so the menu bar will show
            invisibleFrame.setVisible(true);
        }
        return invisibleFrame;
    }

    protected void setScreenMenuBar(JMenuBar mb) {
        ((JFrame) getComponent()).setJMenuBar(mb);
        // pack it (without calling pack, the screen menu bar won't work for some reason)
        invisibleFrame.pack();
    }

    protected ActionMap createModelActionMap(ApplicationModel mo) {
        ActionMap rootMap = new ActionMap();
        rootMap.put(AboutAction.ID, new AboutAction(this));
        rootMap.put(ExitAction.ID, new ExitAction(this));
        rootMap.put(OpenApplicationAction.ID, new OpenApplicationAction(this));
        rootMap.put(OpenApplicationFileAction.ID, new OpenApplicationFileAction(this));
        rootMap.put(ReOpenApplicationAction.ID, new ReOpenApplicationAction(this));
        rootMap.put(ClearRecentFilesMenuAction.ID, new ClearRecentFilesMenuAction(this));
        rootMap.put(MaximizeWindowAction.ID, new MaximizeWindowAction(this, null));
        rootMap.put(MinimizeWindowAction.ID, new MinimizeWindowAction(this, null));

        ActionMap moMap = mo.createActionMap(this, null);
        moMap.setParent(rootMap);
        return moMap;
    }

    
    protected ActionMap createViewActionMap(View v) {
        ActionMap intermediateMap = new ActionMap();
        intermediateMap.put(FocusWindowAction.ID, new FocusWindowAction(v));
        intermediateMap.put(MaximizeWindowAction.ID, new MaximizeWindowAction(this, v));
        intermediateMap.put(MinimizeWindowAction.ID, new MinimizeWindowAction(this, v));

        ActionMap vMap = model.createActionMap(this, v);
        vMap.setParent(intermediateMap);
        intermediateMap.setParent(getActionMap(null));
        return vMap;
    }

    /** Updates the menu items in the "Window" menu. */
    private class WindowMenuHandler implements PropertyChangeListener, Disposable {

        private JMenu windowMenu;
        
        private View view;

        public WindowMenuHandler(JMenu windowMenu,  View view) {
            this.windowMenu = windowMenu;
            this.view = view;
            OSXApplication.this.addPropertyChangeListener(this);
            if (view != null) {
                view.addDisposable(this);
            }
            updateWindowMenu();
        }

        
        public void propertyChange(PropertyChangeEvent evt) {
            String name = evt.getPropertyName();
            if (name == VIEW_COUNT_PROPERTY || name == "paletteCount") {
                updateWindowMenu();
            }
        }

        protected void updateWindowMenu() {
            JMenu m = windowMenu;
            JMenuItem mi;

            // FIXME - We leak memory here!!
            m.removeAll();
            mi = m.add(getAction(view, MinimizeWindowAction.ID));
            mi.setIcon(null);
            mi = m.add(getAction(view, MaximizeWindowAction.ID));
            mi.setIcon(null);
            m.addSeparator();
            for (Iterator i = views().iterator(); i.hasNext();) {
                View pr = (View) i.next();
                if (getAction(pr, FocusWindowAction.ID) != null) {
                    mi = m.add(getAction(pr, FocusWindowAction.ID));
                }
            }
            if (paletteActions.size() > 0) {
                m.addSeparator();
                for (Action a : paletteActions) {
                    JCheckBoxMenuItem cbmi = new JCheckBoxMenuItem(a);
                    ActionUtil.configureJCheckBoxMenuItem(cbmi, a);
                    cbmi.setIcon(null);
                    m.add(cbmi);
                }
            }

            MenuBuilder mb = model.getMenuBuilder();
            mb.addOtherWindowItems(m, OSXApplication.this, view);
        }

        
        public void dispose() {
            windowMenu.removeAll();
            removePropertyChangeListener(this);
            view = null;
        }
    }

    /** Updates the modifedState of the frame. */
    private class FrameHandler extends WindowAdapter implements PropertyChangeListener, Disposable {

        private JFrame frame;
        private View view;

        public FrameHandler(JFrame frame, View view) {
            this.frame = frame;
            this.view = view;
            view.addPropertyChangeListener(this);
            frame.addWindowListener(this);
            view.addDisposable(this);
        }

        
        public void propertyChange(PropertyChangeEvent evt) {
            String name = evt.getPropertyName();
            if (name.equals(View.HAS_UNSAVED_CHANGES_PROPERTY)) {
                frame.getRootPane().putClientProperty("windowModified", view.hasUnsavedChanges());
            } else if (name.equals(View.URI_PROPERTY) || name.equals(View.TITLE_PROPERTY)) {
                updateViewTitle(view, frame);
            }
        }

        
        public void windowClosing(final WindowEvent evt) {
            getAction(view, CloseFileAction.ID).actionPerformed(
                    new ActionEvent(evt.getSource(), ActionEvent.ACTION_PERFORMED,
                    "windowClosing"));
        }

        
        public void windowClosed(final WindowEvent evt) {
        }

        
        public void windowIconified(WindowEvent e) {
            if (view == getActiveView()) {
                setActiveView(null);
            }
            view.stop();
        }

        
        public void windowDeiconified(WindowEvent e) {
            view.start();
        }

        
        public void dispose() {
            frame.removeWindowListener(this);
            view.removePropertyChangeListener(this);
        }

        
        public void windowGainedFocus(WindowEvent e) {
            setActiveView(view);
        }
    }

    private static class QuitHandler {

        /** This method is invoked, when the user has selected the Quit menu item.
         *
         * @return Returns true if the application has no unsaved changes and
         * can be closed.
         */
        public boolean handleQuit() {
            return false;
        }
    }
}
