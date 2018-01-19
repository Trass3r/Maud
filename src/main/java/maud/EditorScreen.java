/*
 Copyright (c) 2017-2018, Stephen Gold
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

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.renderer.ViewPort;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.NiftyEventSubscriber;
import de.lessvoid.nifty.controls.CheckBoxStateChangedEvent;
import de.lessvoid.nifty.controls.SliderChangedEvent;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.screen.Screen;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyCamera;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.math.MyMath;
import jme3utilities.nifty.GuiScreenController;
import jme3utilities.nifty.SliderTransform;
import jme3utilities.ui.InputMode;
import maud.action.EditorInputMode;
import maud.menu.BuildMenus;
import maud.menu.ShowMenus;
import maud.model.EditorModel;
import maud.model.History;
import maud.model.cgm.Cgm;
import maud.model.cgm.EditableCgm;
import maud.model.cgm.LoadedAnimation;
import maud.model.cgm.PlayOptions;
import maud.model.cgm.Pov;
import maud.model.cgm.SelectedSpatial;
import maud.model.option.DisplaySettings;
import maud.model.option.MiscOptions;
import maud.model.option.scene.AxesOptions;
import maud.model.option.scene.CameraOptions;
import maud.model.option.scene.SceneOptions;
import maud.tool.EditorTools;
import maud.view.CgmTransform;
import maud.view.Drag;
import maud.view.EditorView;
import maud.view.SceneDrag;
import maud.view.SceneView;
import maud.view.Selection;
import maud.view.ViewType;
import org.lwjgl.input.Mouse;

/**
 * The screen controller for Maud's editor screen. The GUI includes a menu bar,
 * numerous tool windows, and a status bar.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class EditorScreen extends GuiScreenController {
    // *************************************************************************
    // constants and loggers

    /**
     * squared distance limit for most selections (in pixels squared)
     */
    final private static float maxDSquared = 1600f;
    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(EditorScreen.class.getName());
    /**
     * name of the signal that rotates the model counter-clockwise around +Y
     */
    final private static String modelCCWSignalName = "modelLeft";
    /**
     * name of the signal that rotates the model clockwise around +Y
     */
    final private static String modelCWSignalName = "modelRight";
    /**
     * name of the signal that controls POV movement
     */
    final private static String povSignalName = "moveCamera";
    // *************************************************************************
    // fields

    /**
     * flag that causes this controller to temporarily ignore change events from
     * GUI controls during an update
     */
    private boolean ignoreGuiChanges = false;
    /**
     * build menus for this screen
     */
    final public BuildMenus buildMenus = new BuildMenus();
    /**
     * input mode for this screen
     */
    final EditorInputMode inputMode = new EditorInputMode();
    /**
     * controllers for tool windows
     */
    final public EditorTools tools = new EditorTools(this);
    /**
     * POV that's being dragged, or null for none
     */
    private Pov dragPov = null;
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized, disabled display that will not be enabled
     * during initialization.
     */
    EditorScreen() {
        super("editor", "Interface/Nifty/huds/editor.xml", false);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Activate the "Bind" screen.
     */
    public void goBindScreen() {
        closeAllPopups();
        Maud.bindScreen.activate(inputMode);
    }

    /**
     * Select a loaded C-G model (source or target) based on the screen location
     * of the mouse pointer.
     *
     * @return a pre-existing instance, or null if none applies
     */
    public Cgm mouseCgm() {
        Cgm source = Maud.getModel().getSource();
        Cgm target = Maud.getModel().getTarget();
        ViewPort sScene = source.getSceneView().getViewPort();
        ViewPort sScore = source.getScoreView().getViewPort();
        ViewPort tScene = target.getSceneView().getViewPort();
        ViewPort tScore = target.getScoreView().getViewPort();

        Vector2f screenXY = inputManager.getCursorPosition();
        List<ViewPort> viewPorts
                = MyCamera.listViewPorts(renderManager, screenXY);
        Cgm cgm = null;
        for (ViewPort vp : viewPorts) {
            if (vp.isEnabled()) {
                if (vp == sScene || vp == sScore) {
                    cgm = source;
                    break;
                } else if (vp == tScene || vp == tScore) {
                    cgm = target;
                    break;
                }
            }
        }

        return cgm;
    }

    /**
     * Select a POV based on the screen location of the mouse pointer.
     *
     * @return the pre-existing instance, or null if none applies
     */
    public Pov mousePov() {
        Cgm source = Maud.getModel().getSource();
        Cgm target = Maud.getModel().getTarget();
        EditorView sScene = source.getSceneView();
        EditorView sScore = source.getScoreView();
        EditorView tScene = target.getSceneView();
        EditorView tScore = target.getScoreView();

        Vector2f screenXY = inputManager.getCursorPosition();
        List<ViewPort> viewPorts
                = MyCamera.listViewPorts(renderManager, screenXY);
        Pov result = null;
        for (ViewPort vp : viewPorts) {
            if (vp.isEnabled()) {
                if (vp == sScene.getViewPort()) {
                    result = source.getScenePov();
                    break;
                } else if (vp == sScore.getViewPort()) {
                    result = source.getScorePov();
                    break;
                } else if (vp == tScene.getViewPort()) {
                    result = target.getScenePov();
                    break;
                } else if (vp == tScore.getViewPort()) {
                    result = target.getScorePov();
                    break;
                }
            }
        }

        return result;
    }

    /**
     * Select a view based on the screen location of the mouse pointer.
     *
     * @return the pre-existing instance, or null if none applies
     */
    public EditorView mouseView() {
        Cgm source = Maud.getModel().getSource();
        Cgm target = Maud.getModel().getTarget();
        EditorView sScene = source.getSceneView();
        EditorView sScore = source.getScoreView();
        EditorView tScene = target.getSceneView();
        EditorView tScore = target.getScoreView();

        Vector2f screenXY = inputManager.getCursorPosition();
        List<ViewPort> viewPorts
                = MyCamera.listViewPorts(renderManager, screenXY);
        EditorView result = null;
        for (ViewPort vp : viewPorts) {
            if (vp.isEnabled()) {
                if (vp == sScene.getViewPort()) {
                    result = sScene;
                    break;
                } else if (vp == sScore.getViewPort()) {
                    result = sScore;
                    break;
                } else if (vp == tScene.getViewPort()) {
                    result = tScene;
                    break;
                } else if (vp == tScore.getViewPort()) {
                    result = tScore;
                    break;
                }
            }
        }

        return result;
    }

    /**
     * Select a view type based on the screen location of the mouse pointer.
     *
     * @return an enum value, or null if neither applies
     */
    public ViewType mouseViewType() {
        ViewType result = null;
        EditorView view = mouseView();
        if (view != null) {
            result = view.getType();
        }

        return result;
    }

    /**
     * Callback handler that Nifty invokes after a check box changes.
     *
     * @param checkBoxId Nifty element id of the check box (not null)
     * @param event details of the event (not null)
     */
    @NiftyEventSubscriber(pattern = ".*CheckBox")
    public void onCheckBoxChanged(final String checkBoxId,
            final CheckBoxStateChangedEvent event) {
        Validate.nonNull(checkBoxId, "check box id");
        Validate.nonNull(event, "event");
        assert checkBoxId.endsWith("CheckBox");

        if (ignoreGuiChanges || !hasStarted()) {
            return;
        }

        EditorModel model = Maud.getModel();
        SceneOptions scene = model.getScene();
        Cgm source = model.getSource();
        EditableCgm target = model.getTarget();
        Cgm animationCgm;
        if (target.getAnimation().isRetargetedPose()) {
            animationCgm = source;
        } else {
            animationCgm = target;
        }
        boolean isChecked = event.isChecked();

        String prefix = MyString.removeSuffix(checkBoxId, "CheckBox");
        switch (prefix) {
            case "3DCursor":
            case "3DCursor2":
                scene.getCursor().setVisible(isChecked);
                break;
            case "autoCheckpoint":
                History.setAutoAdd(isChecked);
                break;
            case "axesDepthTest":
                scene.getAxes().setDepthTestFlag(isChecked);
                break;
            case "boundsDepthTest":
                scene.getBounds().setDepthTestFlag(isChecked);
                break;
            case "freeze":
                animationCgm.getPose().setFrozen(isChecked);
                break;
            case "fullscreen":
                DisplaySettings.setFullscreen(isChecked);
                break;
            case "gammaCorrection":
                DisplaySettings.setGammaCorrection(isChecked);
                break;
            case "invertRma":
            case "invertRma2":
                model.getMap().setInvertMap(isChecked);
                break;
            case "lightEnable":
                target.getLight().setEnabled(isChecked);
                break;
            case "loop":
                animationCgm.getPlay().setContinue(isChecked);
                break;
            case "loopSource":
                source.getPlay().setContinue(isChecked);
                break;
            case "matDepthTest":
                target.setDepthTest(isChecked);
                break;
            case "matWireframe":
                target.setWireframe(isChecked);
                break;
            case "mpoEnable":
                target.setOverrideEnabled(isChecked);
                break;
            case "physics":
                scene.getRender().setPhysicsRendered(isChecked);
                break;
            case "pin":
                target.getAnimation().setPinned(isChecked);
                break;
            case "pinSource":
                source.getAnimation().setPinned(isChecked);
                break;
            case "pong":
                animationCgm.getPlay().setReverse(isChecked);
                break;
            case "pongSource":
                source.getPlay().setReverse(isChecked);
                break;
            case "scoreRotations":
                model.getScore().setShowRotations(isChecked);
                break;
            case "scoreScales":
                model.getScore().setShowScales(isChecked);
                break;
            case "scoreTranslations":
                model.getScore().setShowTranslations(isChecked);
                break;
            case "settingsDiagnose":
                model.getMisc().setDiagnoseLoads(isChecked);
                break;
            case "sgcEnable":
                target.setSgcEnabled(isChecked);
                break;
            case "sgcLocalPhysics":
                target.setApplyPhysicsLocal(isChecked);
                break;
            case "shadows":
                scene.getRender().setShadowsRendered(isChecked);
                break;
            case "sky":
            case "sky2":
                scene.getRender().setSkySimulated(isChecked);
                break;
            case "spatialIgnoreTransform":
                target.setIgnoreTransform(isChecked);
                break;
            case "vSync":
                DisplaySettings.setVSync(isChecked);
                break;
            default:
                logger.log(Level.WARNING, "check box with unknown id={0}",
                        MyString.quote(checkBoxId));
        }
    }

    /**
     * Callback handler that Nifty invokes after a slider changes.
     *
     * @param sliderId Nifty element id of the slider (not null)
     * @param event details of the event (not null, ignored)
     */
    @NiftyEventSubscriber(pattern = ".*Slider")
    public void onSliderChanged(final String sliderId,
            final SliderChangedEvent event) {
        Validate.nonNull(sliderId, "slider id");
        Validate.nonNull(event, "event");

        if (ignoreGuiChanges || !hasStarted()) {
            return;
        }

        tools.onSliderChanged(sliderId, event);
    }

    /**
     * Read a bank of 3 sliders that control a color. TODO storeResult
     *
     * @param name unique id prefix of the bank to read (not null)
     * @param transform how to transform the raw readings (not null)
     * @return color indicated by the sliders (new instance)
     */
    public ColorRGBA readColorBank(String name, SliderTransform transform) {
        Validate.nonNull(name, "name");
        Validate.nonNull(transform, "transform");

        float r = readSlider(name + "R", transform);
        float g = readSlider(name + "G", transform);
        float b = readSlider(name + "B", transform);
        ColorRGBA color = new ColorRGBA(r, g, b, 1f);

        return color;
    }

    /**
     * Select a bone based on the screen coordinates of the mouse pointer.
     */
    public void selectBone() {
        Cgm mouseCgm = mouseCgm();
        EditorView mouseView = mouseView();
        if (mouseCgm != null && mouseView != null) {
            Vector2f mouseXY = inputManager.getCursorPosition();
            Selection selection = new Selection(mouseXY, maxDSquared);
            mouseView.considerBones(selection);
            selection.select();
        }
    }

    /**
     * Select a gnomon based on the screen coordinates of the mouse pointer.
     */
    public void selectGnomon() {
        Cgm mouseCgm = mouseCgm();
        EditorView mouseView = mouseView();
        if (mouseCgm != null && mouseView != null) {
            Vector2f mouseXY = inputManager.getCursorPosition();
            Selection selection = new Selection(mouseXY, Float.MAX_VALUE);
            mouseView.considerGnomons(selection);
            selection.select();
        }
    }

    /**
     * Select a keyframe based on the screen coordinates of the mouse pointer.
     */
    public void selectKeyframe() {
        Cgm mouseCgm = mouseCgm();
        EditorView mouseView = mouseView();
        if (mouseCgm != null && mouseView != null) {
            Vector2f mouseXY = inputManager.getCursorPosition();
            Selection selection = new Selection(mouseXY, maxDSquared);
            mouseView.considerKeyframes(selection);
            selection.select();
        }
    }

    /**
     * Handle a "select spatialChild" action with an argument.
     *
     * @param argument action argument (not null)
     */
    public void selectSpatialChild(String argument) {
        SelectedSpatial spatial = Maud.getModel().getTarget().getSpatial();
        List<String> children = spatial.listNumberedChildren();
        int childIndex = children.indexOf(argument);
        if (childIndex >= 0) {
            spatial.selectChild(childIndex);
        } else {
            ShowMenus.selectSpatialChild(argument);
        }
    }

    /**
     * Select a vertex based on the screen coordinates of the mouse pointer.
     */
    public void selectVertex() {
        Cgm mouseCgm = mouseCgm();
        EditorView mouseView = mouseView();
        if (mouseCgm != null && mouseView != null) {
            Vector2f mouseXY = inputManager.getCursorPosition();
            Selection selection = new Selection(mouseXY, maxDSquared);
            mouseView.considerVertices(selection);
            selection.select();
        }
    }

    /**
     * Select an axis, bone, boundary, gnomon, or keyframe based on the screen
     * coordinates of the mouse pointer.
     */
    public void selectXY() {
        Cgm mouseCgm = mouseCgm();
        EditorView mouseView = mouseView();
        if (mouseCgm != null && mouseView != null) {
            Vector2f mouseXY = inputManager.getCursorPosition();
            Selection selection = new Selection(mouseXY, maxDSquared);
            mouseView.considerAxes(selection);
            mouseView.considerBones(selection);
            mouseView.considerBoundaries(selection);
            mouseView.considerGnomons(selection);
            mouseView.considerKeyframes(selection);
            selection.select();
        }
    }

    /**
     * Set a bank of 3 sliders that control a color and update the status
     * labels.
     *
     * @param name unique id prefix of the bank (not null)
     * @param transform how each component has been transformed (not null)
     * @param color (not null, unaffected)
     */
    public void setColorBank(String name, SliderTransform transform,
            ColorRGBA color) {
        Validate.nonNull(name, "name");
        Validate.nonNull(transform, "transform");

        setSlider(name + "R", transform, color.r);
        updateSliderStatus(name + "R", color.r, "");

        setSlider(name + "G", transform, color.g);
        updateSliderStatus(name + "G", color.g, "");

        setSlider(name + "B", transform, color.b);
        updateSliderStatus(name + "B", color.b, "");
    }

    /**
     * Alter the "ignore GUI changes" flag.
     *
     * @param newSetting true &rarr; ignore events, false &rarr; invoke callback
     * handlers for events
     */
    public void setIgnoreGuiChanges(boolean newSetting) {
        ignoreGuiChanges = newSetting;
    }
    // *************************************************************************
    // GuiScreenController methods

    /**
     * A callback that Nifty invokes the 1st time the screen is displayed.
     *
     * @param nifty (not null)
     * @param screen (not null)
     */
    @Override
    public void bind(Nifty nifty, Screen screen) {
        super.bind(nifty, screen);

        Maud maud = Maud.getApplication();
        maud.startup2();
    }

    /**
     * Initialize this controller prior to its 1st update.
     *
     * @param stateManager (not null)
     * @param application application that owns this screen (not null)
     */
    @Override
    public void initialize(AppStateManager stateManager,
            Application application) {
        if (isEnabled()) {
            throw new IllegalStateException("shouldn't be enabled yet");
        }

        InputMode.getActiveMode().setEnabled(false);
        inputMode.setEnabled(true);
        inputMode.influence(this);
        setListener(inputMode);

        setSubmenuWarp(true, true);

        super.initialize(stateManager, application);
    }

    /**
     * Callback to update the editor screen prior to rendering. (Invoked once
     * per render pass.)
     *
     * @param tpf time interval between render passes (in seconds, &ge;0)
     */
    @Override
    public void update(float tpf) {
        super.update(tpf);

        if (!tools.getTool("camera").isInitialized()) {
            return;
        }

        Drag.updateBoundary();
        EditorViewPorts.update();
        updateBars();
        /*
         * Update the loaded animations.
         */
        EditorModel model = Maud.getModel();
        Cgm source = model.getSource();
        if (source.getAnimation().isMoving()) {
            updateTrackTime(source, tpf);
        }
        Cgm target = model.getTarget();
        if (target.getAnimation().isMoving()) {
            updateTrackTime(target, tpf);
        } else if (target.getAnimation().isRetargetedPose()) {
            target.getPose().setToAnimation();
        }

        ViewType viewType = mouseViewType();
        if (viewType == ViewType.Scene) {
            /*
             * Based on the mouse-pointer location, select a loaded C-G model
             * to rotate around its Y-axis.
             */
            Cgm cgmToRotate = mouseCgm();
            if (cgmToRotate != null) {
                CgmTransform cgmTransform
                        = cgmToRotate.getSceneView().getTransform();
                if (signals.test(modelCCWSignalName)) {
                    cgmTransform.rotateY(tpf);
                }
                if (signals.test(modelCWSignalName)) {
                    cgmTransform.rotateY(-tpf);
                }
            }

            if (SceneDrag.isActive()) {
                Cgm dragCgm = SceneDrag.getCgm();
                SceneView sceneView = dragCgm.getSceneView();
                sceneView.dragAxis();
            }

        } else if (viewType == ViewType.Score) {
            Drag.updateGnomon();
        }
        updateDragPov();
        /*
         * Update the views.
         */
        source.getSceneView().update(null, tpf);
        target.getSceneView().update(null, tpf);
        source.getScoreView().update(source, tpf);
        target.getScoreView().update(target, tpf);
    }
    // *************************************************************************
    // private methods

    /**
     * Update the menu bar and status bar.
     */
    private void updateBars() {
        EditorModel model = Maud.getModel();
        MiscOptions misc = model.getMisc();
        /*
         * Update visibility of both bars.
         */
        boolean visible = misc.isMenuBarVisible();
        Element menuBar = getScreen().findElementById("menu bar");
        menuBar.setVisible(visible);
        Element statusBar = getScreen().findElementById("status bar");
        statusBar.setVisible(visible);

        if (visible) {
            /*
             * Update the description of axis-dragging options at the left end
             * of the status bar.
             */
            String description = "";
            ViewType viewType = mouseViewType();
            if (viewType == ViewType.Scene) {
                AxesOptions axesOptions = model.getScene().getAxes();
                description = axesOptions.describe();
            }
            setStatusText("statusLeft", " " + description);
            /*
             * Copy the status message to the center.
             */
            String message = misc.getStatusMessage();
            setStatusText("statusCenter", message);
            /*
             * Update the description of camera options at the right end.
             */
            description = "";
            if (viewType == ViewType.Scene) {
                CameraOptions options = model.getScene().getCamera();
                description = options.describe();
            }
            setStatusText("statusRight", description + " ");
        }
    }

    /**
     * If a POV is being dragged, update it.
     */
    void updateDragPov() {
        if (signals.test(povSignalName)) { // dragging a POV
            if (dragPov == null) { // a brand-new drag
                dragPov = mousePov();
            } else {
                float dx = Mouse.getDX();
                if (dx != 0f) {
                    dragPov.moveLeft(dx / 1024f);
                }
                float dy = Mouse.getDY();
                if (dy != 0f) {
                    dragPov.moveUp(-dy / 1024f);
                }
            }
        } else {
            dragPov = null;
        }
    }

    /**
     * Update the track time.
     *
     * @param cgm (not null)
     * @param tpf time interval between render passes (in seconds, &ge;0)
     */
    private void updateTrackTime(Cgm cgm, float tpf) {
        LoadedAnimation animation = cgm.getAnimation();
        PlayOptions play = cgm.getPlay();
        assert animation.isMoving();

        float speed = play.getSpeed();
        float time = animation.getTime();
        time += speed * tpf;

        boolean cont = play.willContinue();
        boolean reverse = play.willReverse();
        float duration = animation.getDuration();
        if (duration == 0f) {
            time = 0f;
        } else if (cont && !reverse) {
            time = MyMath.modulo(time, duration); // wrap
        } else {
            float freeTime = time;
            time = FastMath.clamp(time, 0f, duration);
            if (time != freeTime) { // reached a limit
                if (reverse) {
                    play.setSpeed(-speed); // pong
                } else {
                    time = duration - time; // wrap
                }
                play.setPaused(!cont);
            }
        }
        animation.setTime(time);
    }
}
