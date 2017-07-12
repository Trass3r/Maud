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
import com.jme3.post.FilterPostProcessor;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.plugins.bvh.BVHLoader;
import com.jme3.shadow.DirectionalLightShadowFilter;
import com.jme3.shadow.EdgeFilteringMode;
import com.jme3.system.AppSettings;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Misc;
import jme3utilities.MyString;
import jme3utilities.debug.Dumper;
import jme3utilities.nifty.GuiApplication;
import jme3utilities.nifty.bind.BindScreen;
import jme3utilities.ui.InputMode;
import maud.model.EditorModel;
import maud.model.History;
import maud.model.LoadedCgm;

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
     * width and height of rendered shadow maps (pixels per side, &gt;0)
     */
    final private static int shadowMapSize = 4_096;
    /**
     * number of shadow map splits in shadow filters (&gt;0)
     */
    final private static int shadowMapSplits = 3;
    /**
     * additional scenes (besides rootNode and guiRoot) for rendering
     */
    final private static List<Spatial> addedScenes = new ArrayList<>(3);
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
     * Nifty screen for editing hotkey bindings
     */
    final static BindScreen bindScreen = new BindScreen();
    /**
     * true once {@link #startup1()} has completed, until then false
     */
    private boolean didStartup1 = false;
    /**
     * dumper for scene dumps
     */
    final private static Dumper dumper = new Dumper();
    /**
     * GUI portion of the editor screen, with links to tools
     */
    final public static EditorScreen gui = new EditorScreen();
    /**
     * MVC model for the editor screen
     */
    public static EditorModel model = new EditorModel();
    /**
     * application instance, set by {@link #main(java.lang.String[])}
     */
    private static Maud application;
    /**
     * view port for left half of split screen in scene mode
     */
    private ViewPort sourceSceneViewPort;
    /**
     * view port for left half of split screen in score mode
     */
    private ViewPort sourceScoreViewPort;
    /**
     * view port for right half of split screen in scene mode
     */
    private ViewPort targetSceneHalfViewPort;
    /**
     * view port for right half of split screen in score mode
     */
    private ViewPort targetScoreHalfViewPort;
    /**
     * view port for unified screen in score mode
     */
    private ViewPort targetScoreWideViewPort;
    // *************************************************************************
    // new methods exposed

    /**
     * Process a "dump renderer" action.
     */
    public void dumpRenderer() {
        dumper.setPrintCull(true);
        dumper.setPrintTransform(true);
        dumper.dump(renderManager);
    }

    /**
     * Process a "dump scene" action.
     */
    public void dumpScene() {
        dumper.setPrintCull(true);
        dumper.setPrintTransform(true);
        dumper.dump(rootNode);
    }

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
        Logger.getLogger(LoadedCgm.class.getName()).setLevel(Level.INFO);
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
     * Initialization performed the 1st time the editor screen is displayed.
     */
    void startup2() {
        logger.info("");
        /*
         * Attach controllers for windows in the editor screen.
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

    /**
     * Update the configuration of view ports to reflect the MVC model.
     */
    public void updateViewPorts() {
        String viewMode = Maud.model.misc.getViewMode();
        boolean scoreMode = viewMode.equals("score");
        boolean splitScreen = Maud.model.source.isLoaded();

        sourceSceneViewPort.setEnabled(!scoreMode && splitScreen);
        targetSceneHalfViewPort.setEnabled(!scoreMode && splitScreen);
        viewPort.setEnabled(!scoreMode && !splitScreen);

        sourceScoreViewPort.setEnabled(scoreMode && splitScreen);
        targetScoreHalfViewPort.setEnabled(scoreMode && splitScreen);
        targetScoreWideViewPort.setEnabled(scoreMode && !splitScreen);
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
                case "dump renderer":
                    dumpRenderer();
                    handled = true;
                    break;

                case "dump scene":
                    dumpScene();
                    handled = true;
                    break;

                case "edit bindings":
                    InputMode im = InputMode.getActiveMode();
                    bindScreen.activate(im);
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

        Locators.setAssetManager(assetManager);
        Locators.useDefault();

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
        /*
         * Update the logical/geometric state of all scenes other than
         * rootNode and guiNode. TODO: an app state for each view port
         */
        for (Spatial scene : addedScenes) {
            scene.updateLogicalState(tpf);
            scene.updateGeometricState();
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Add shadows to the specified view port, without specifying a light.
     */
    private void addShadows(ViewPort vp) {
        DirectionalLightShadowFilter dlsf = new DirectionalLightShadowFilter(
                assetManager, shadowMapSize, shadowMapSplits);
        dlsf.setEdgeFilteringMode(EdgeFilteringMode.PCF8);
        dlsf.setEnabled(false);

        FilterPostProcessor fpp = Misc.getFpp(vp, assetManager);
        fpp.addFilter(dlsf);
    }

    /**
     * Instantiate a camera for a half-width view port.
     *
     * @param leftEdge which side (0 &rarr; left, 0.5 &rarr; right)
     * @return a new instance with perspective projection
     */
    private Camera createHalfCamera(float leftEdge) {
        Camera camera = cam.clone();
        float bottomEdge = 0f;
        float rightEdge = leftEdge + 0.5f;
        float topEdge = 1f;
        camera.setViewPort(leftEdge, rightEdge, bottomEdge, topEdge);

        return camera;
    }

    /**
     * Create a view port for the source scene.
     *
     * @return the attachment point (a new instance)
     */
    private Node createSourceSceneViewPort() {
        Camera camera = createHalfCamera(0f);
        sourceSceneViewPort = renderManager.createMainView("Source Scene",
                camera);
        sourceSceneViewPort.setClearFlags(true, true, true);
        sourceSceneViewPort.setEnabled(false);
        addShadows(sourceSceneViewPort);
        /*
         * Attach a scene to the new view port.
         */
        Node scene = new Node("Root for source scene");
        sourceSceneViewPort.attachScene(scene);
        addedScenes.add(scene);
        /*
         * Add an attachment point to the scene.
         */
        Node parent = new Node("parent for source CGM");
        scene.attachChild(parent);

        return parent;
    }

    /**
     * Create a half-width view port for the source score.
     */
    private void createSourceScoreViewPort() {
        Camera camera = createHalfCamera(0f);
        camera.setParallelProjection(true);
        sourceScoreViewPort = renderManager.createMainView(
                "Source Score", camera);
        sourceScoreViewPort.setClearFlags(true, true, true);
        sourceScoreViewPort.setEnabled(false);
        /*
         * Attach a scene to the new view port.
         */
        Node root = new Node("Root for source score");
        sourceScoreViewPort.attachScene(root);
        addedScenes.add(root);
    }

    /**
     * Create a half-width view port for the target scene in split-screen mode.
     *
     * @return the attachment point (a new instance)
     */
    private Node createTargetSceneViewPort() {
        Camera camera = createHalfCamera(0.5f);
        targetSceneHalfViewPort = renderManager.createMainView("Target Scene",
                camera);
        targetSceneHalfViewPort.setClearFlags(true, true, true);
        targetSceneHalfViewPort.setEnabled(false);
        addShadows(targetSceneHalfViewPort);
        /*
         * Attach the existing scene to the new view port.
         */
        targetSceneHalfViewPort.attachScene(rootNode);
        /*
         * Add an attachment point to the scene.
         */
        Node parent = new Node("parent for target CGM");
        rootNode.attachChild(parent);

        return parent;
    }

    /**
     * Create a half-width view port for the target score in split-screen mode.
     */
    private void createTargetScoreHalfViewPort() {
        Camera camera = createHalfCamera(0.5f);
        camera.setParallelProjection(true);
        targetScoreHalfViewPort = renderManager.createMainView(
                "Target Score Half", camera);
        targetScoreHalfViewPort.setClearFlags(true, true, true);
        targetScoreHalfViewPort.setEnabled(false);
        /*
         * Attach a scene to the new view port.
         */
        Node root = new Node("Root for target score half");
        targetScoreHalfViewPort.attachScene(root);
        addedScenes.add(root);
    }

    /**
     * Create a full-width view port for the target score.
     */
    private void createTargetScoreWideViewPort() {
        Camera camera = cam.clone();
        camera.setParallelProjection(true);
        targetScoreWideViewPort = renderManager.createMainView(
                "Target Score Wide", camera);
        targetScoreWideViewPort.setClearFlags(true, true, true);
        targetScoreWideViewPort.setEnabled(false);
        /*
         * Attach a scene to the new view port.
         */
        Node root = new Node("Root for target score wide");
        targetScoreWideViewPort.attachScene(root);
        addedScenes.add(root);
    }

    /**
     * Initialization performed during the 1st invocation of
     * {@link #simpleUpdate(float)}. TODO sort methods
     */
    private void startup1() {
        logger.info("");
        /*
         * Register a loader for BVH files.
         */
        assetManager.registerLoader(BVHLoader.class, "bvh", "BVH");
        /*
         * Add shadows to the view port for the main scene.
         */
        addShadows(viewPort);
        /*
         * Create 2 view ports for split-screen scenes, also with shadows.
         */
        Node sourceSceneParent = createSourceSceneViewPort();
        Node targetSceneParent = createTargetSceneViewPort();
        /*
         * Create 3 view ports for scores.
         */
        createSourceScoreViewPort();
        createTargetScoreHalfViewPort();
        createTargetScoreWideViewPort();
        /*
         * Create 2 scene views and 2 score views.
         */
        SceneView sourceSceneView = new SceneView(Maud.model.source,
                sourceSceneParent, null, sourceSceneViewPort);
        SceneView targetSceneView = new SceneView(Maud.model.target,
                targetSceneParent, viewPort, targetSceneHalfViewPort);
        ScoreView sourceScoreView = new ScoreView(null, sourceScoreViewPort);
        ScoreView targetScoreView = new ScoreView(targetScoreWideViewPort,
                targetScoreHalfViewPort);

        Maud.model.source.setViews(sourceSceneView, sourceScoreView);
        Maud.model.target.setViews(targetSceneView, targetScoreView);
        /*
         * Attach screen controllers for the editor screen and bind screen.
         */
        stateManager.attachAll(gui, bindScreen);
        /*
         * Configure and attach input mode for the editor screen.
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
