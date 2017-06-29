/*
 Copyright (c) 2017, Stephen Gold
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright
 notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 notice, this list of conditions and the following disclaimer in the
 documentation and/or other materials provided with the distribution.
 * Neither the name of the copyright holder nor the names of its contributors
 may be used to endorse or promote products derived from this software without
 specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package maud;

import com.jme3.app.SimpleApplication;
import com.jme3.app.state.ScreenshotAppState;
import com.jme3.audio.openal.ALAudioRenderer;
import com.jme3.scene.Node;
import com.jme3.scene.plugins.bvh.BVHLoader;
import com.jme3.system.AppSettings;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Misc;
import jme3utilities.MyString;
import jme3utilities.debug.Printer;
import jme3utilities.nifty.GuiApplication;
import jme3utilities.nifty.bind.BindScreen;
import jme3utilities.ui.InputMode;
import maud.model.DddModel;
import maud.model.History;
import maud.model.LoadedCGModel;

/**
 * GUI application to edit jMonkeyEngine animated 3-D CG models. The
 * application's main entry point is in this class.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class Maud extends GuiApplication {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(Maud.class.getName());
    /**
     * path to hotkey bindings configuration asset
     */
    final private static String hotkeyBindingsAssetPath = "Interface/bindings/3DView.properties";
    /**
     * application name for the window's title bar
     */
    final private static String windowTitle = "Maud";
    // *************************************************************************
    // fields

    /**
     * true once {@link #startup1()} has completed, until then false
     */
    private boolean didStartup1 = false;
    /**
     * Nifty screen for editing hotkey bindings
     */
    final static BindScreen bindScreen = new BindScreen();
    /**
     * GUI portion of the "3D View" screen, with links to tools
     */
    final public static DddGui gui = new DddGui();
    /**
     * MVC model for the "3D View" screen
     */
    public static DddModel model = new DddModel();
    /**
     * application instance, set by {@link #main(java.lang.String[])}
     */
    private static Maud application;
    /**
     * printer for scene dumps
     */
    final private static Printer printer = new Printer();
    // *************************************************************************
    // new methods exposed

    /**
     * Access the application.
     *
     * @return the pre-existing instance
     */
    public static Maud getApplication() {
        return application;
    }

    /**
     * Main entry point for Maud.
     *
     * @param arguments array of command-line arguments (not null)
     */
    public static void main(String[] arguments) {
        /*
         * Mute the chatty loggers found in some imported packages.
         */
        Misc.setLoggingLevels(Level.WARNING);
        Logger.getLogger(ALAudioRenderer.class.getName())
                .setLevel(Level.SEVERE);
        /*
         * Lower logging thresholds for classes of interest.
         */
        Logger.getLogger(LoadedCGModel.class.getName()).setLevel(Level.INFO);
        History.logger.setLevel(Level.INFO);
        /*
         * Instantiate the application.
         */
        application = new Maud();
        /*
         * Customize the window's title bar.
         */
        AppSettings settings = new AppSettings(true);
        settings.setTitle(windowTitle);
        application.setSettings(settings);

        application.start();
        /*
         * ... and onward to Maud.guiInitializeApplication()!
         */
    }

    /**
     * Initialization performed the 1st time the "3D View" screen is displayed.
     */
    void startup2() {
        logger.info("");
        /*
         * Attach controllers for windows in the "3D View" screen.
         */
        gui.tools.attachAll(stateManager);
        /*
         * Disable flyCam.
         */
        flyCam.setEnabled(false);
        /*
         * Capture a screenshot each time the SYSRQ hotkey is pressed.
         */
        ScreenshotAppState screenShotState = new ScreenshotAppState();
        boolean success = stateManager.attach(screenShotState);
        assert success;
    }
    // *************************************************************************
    // ActionApplication methods

    /**
     * Callback invoked when an ongoing action isn't handled.
     *
     * @param actionString textual description of the action (not null)
     */
    @Override
    public void didntHandle(String actionString) {
        super.didntHandle(actionString);

        String message = String.format("unimplemented feature (action = %s)",
                MyString.quote(actionString));
        gui.setStatus(message);
    }
    // *************************************************************************
    // ActionListener methods

    /**
     * Process an action (from the GUI or keyboard) that wasn't handled by the
     * input mode.
     *
     * @param actionString textual description of the action (not null)
     * @param ongoing true if the action is ongoing, otherwise false
     * @param tpf time interval between render passes (in seconds, &ge;0)
     */
    @Override
    public void onAction(String actionString, boolean ongoing, float tpf) {
        boolean handled = false;
        if (ongoing) {
            logger.log(Level.INFO, "Got ongoing action {0}",
                    MyString.quote(actionString));

            switch (actionString) {
                case "edit bindings":
                    InputMode im = InputMode.getActiveMode();
                    bindScreen.activate(im);
                    handled = true;
                    break;

                case "print scene":
                    printer.setPrintCull(true);
                    printer.setPrintTransform(true);
                    printer.printSubtree(rootNode);
                    handled = true;
                    break;

                case "quit":
                    QuitDialog controller = new QuitDialog();
                    gui.showConfirmDialog("Quit Maud?", "",
                            SimpleApplication.INPUT_MAPPING_EXIT, controller);
                    handled = true;
            }
        }

        if (!handled) {
            /*
             * Forward unhandled action to the superclass.
             */
            super.onAction(actionString, ongoing, tpf);
        }
    }
    // *************************************************************************
    // GuiApplication methods

    /**
     * Initialize this application.
     */
    @Override
    public void guiInitializeApplication() {
        logger.info("");

        StartScreen startScreen = new StartScreen();
        stateManager.attach(startScreen);
    }
    // *************************************************************************
    // SimpleApplication methods

    /**
     * Callback invoked once per render pass.
     *
     * @param tpf time interval between render passes (in seconds, &ge;0)
     */
    @Override
    public void simpleUpdate(float tpf) {
        super.simpleUpdate(tpf);

        if (!didStartup1) {
            startup1();
            didStartup1 = true;
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Initialization performed during the 1st invocation of
     * {@link #simpleUpdate(float)}.
     */
    private void startup1() {
        logger.info("");
        /*
         * register a loader for BVH files
         */
        assetManager.registerLoader(BVHLoader.class, "bvh", "BVH");
        /*
         * Add attachment points to the scene graph.
         */        
        Node sourceParent = new Node("parent for source CGM");
        rootNode.attachChild(sourceParent);
        CgmView sourceView = new CgmView(model.source, sourceParent);
        model.source.setView(sourceView);

        Node targetParent = new Node("parent for target CGM");
        rootNode.attachChild(targetParent);
        CgmView targetView = new CgmView(model.target, targetParent);
        model.target.setView(targetView);
        /*
         * Attach screen controllers for the "3D View" screen and BindScreen.
         */
        stateManager.attachAll(gui, bindScreen);
        /*
         * Configure and attach input mode for the "3D View" screen.
         */
        gui.inputMode.setConfigPath(hotkeyBindingsAssetPath);
        stateManager.attach(gui.inputMode);
        /*
         * Disable the JME statistic displays.
         * These can be re-enabled by pressing the F5 hotkey.
         */
        setDisplayFps(false);
        setDisplayStatView(false);
    }
}
