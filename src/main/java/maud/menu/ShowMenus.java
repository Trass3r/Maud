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
package maud.menu;

import com.jme3.light.Light;
import com.jme3.scene.control.Control;
import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.MyString;
import maud.Maud;
import maud.MaudUtil;
import maud.action.ActionPrefix;
import maud.model.cgm.Cgm;
import maud.model.cgm.EditableCgm;
import maud.model.cgm.SelectedLight;
import maud.model.cgm.SelectedOverride;
import maud.model.cgm.SelectedSgc;
import maud.model.cgm.SelectedSpatial;
import maud.model.cgm.WhichParams;
import maud.model.option.DisplaySettings;

/**
 * Display simple menus in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class ShowMenus {
    // *************************************************************************
    // constants and loggers

    /**
     * maximum number of items in a menu, determined by minimum screen height
     */
    final public static int maxItems = 19;
    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(ShowMenus.class.getName());
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private ShowMenus() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Display a menu for adding an undefined material parameter using the "new
     * matParam " action prefix.
     */
    public static void addNewMatParam() {
        MenuBuilder builder = new MenuBuilder();

        EditableCgm target = Maud.getModel().getTarget();
        List<String> nameList
                = target.getSpatial().listMatParamNames(WhichParams.Undefined);
        for (String name : nameList) {
            builder.add(name);
        }

        builder.show(ActionPrefix.newMatParam);
    }

    /**
     * Display a "SGC -&gt; Add new" menu.
     */
    public static void addNewSgc() {
        MenuBuilder builder = new MenuBuilder();

        builder.addEdit("Anim");
        builder.addEdit("Ghost");
        builder.addEdit("RigidBody");
        builder.addEdit("Skeleton");

        builder.show("select menuItem SGC -> Add new -> ");
    }

    /**
     * Display an "Animation -> Edit -> Change duration" menu.
     */
    static void changeDuration() {
        MenuBuilder builder = new MenuBuilder();

        builder.addDialog("Proportional times");
        builder.addDialog("Same times");

        builder.show(
                "select menuItem Animation -> Edit -> Change duration -> ");
    }

    /**
     * Display a "Settings -> Remove asset location" menu.
     */
    static void removeAssetLocation() {
        MenuBuilder builder = new MenuBuilder();
        List<String> specs = Maud.getModel().getLocations().listAll();
        for (String spec : specs) {
            builder.addFile(spec);
        }
        builder.show(ActionPrefix.deleteAssetLocationSpec);
    }

    /**
     * Display a menu of files or zip entries.
     *
     * @param names the list of names (not null, unaffected)
     * @param actionPrefix common prefix of the menu's action strings (not null,
     * usually the final character will be a space)
     */
    static void selectFile(List<String> names, String actionPrefix) {
        assert names != null;
        assert actionPrefix != null;

        MenuBuilder builder = new MenuBuilder();
        builder.addFiles(names, maxItems);
        builder.show(actionPrefix);
    }

    /**
     * Display a "select light" menu.
     */
    public static void selectLight() {
        MenuBuilder builder = new MenuBuilder();

        Cgm target = Maud.getModel().getTarget();
        String selectedName = target.getLight().name();
        List<String> names = target.listLightNames(Light.class);
        for (String name : names) {
            if (!name.equals(selectedName)) {
                builder.add(name);
            }
        }
        builder.add(SelectedLight.noLight);

        builder.show(ActionPrefix.selectLight);
    }

    /**
     * Display a menu for selecting a defined material parameter using the
     * "select matParam " action prefix.
     */
    public static void selectMatParam() {
        MenuBuilder builder = new MenuBuilder();

        EditableCgm target = Maud.getModel().getTarget();
        List<String> nameList
                = target.getSpatial().listMatParamNames(WhichParams.Defined);
        String selectedName = target.getMatParam().getName();
        for (String name : nameList) {
            if (!name.equals(selectedName)) {
                builder.add(name);
            }
        }

        builder.show(ActionPrefix.selectMatParam);
    }

    /**
     * Display a menu for selecting a material-parameter override using the
     * "select override " action prefix.
     */
    public static void selectOverride() {
        MenuBuilder builder = new MenuBuilder();

        EditableCgm target = Maud.getModel().getTarget();
        List<String> nameList = target.getSpatial().listOverrideNames();
        String selectedName = target.getOverride().getName();
        for (String name : nameList) {
            if (!name.equals(selectedName)) {
                builder.add(name);
            }
        }
        builder.add(SelectedOverride.noParam);

        builder.show(ActionPrefix.selectOverride);
    }

    /**
     * Display a "select sgc" menu.
     */
    public static void selectSgc() {
        MenuBuilder builder = new MenuBuilder();

        Cgm target = Maud.getModel().getTarget();
        String selectedName = target.getSgc().name();
        List<String> names = target.listSgcNames(Control.class);
        for (String name : names) {
            if (!name.equals(selectedName)) {
                builder.add(name);
            }
        }
        builder.add(SelectedSgc.noControl);

        builder.show(ActionPrefix.selectSgc);
    }

    /**
     * Display a menu for selecting a user key using the "select userKey "
     * action prefix.
     */
    public static void selectUserKey() {
        MenuBuilder builder = new MenuBuilder();

        EditableCgm target = Maud.getModel().getTarget();
        List<String> keyList = target.getSpatial().listUserKeys();
        String selectedKey = target.getUserData().getKey();
        for (String key : keyList) {
            if (!key.equals(selectedKey)) {
                builder.add(key);
            }
        }

        builder.show(ActionPrefix.selectUserKey);
    }

    /**
     * Display a menu to set the color depth (bits per pixel) for the display
     * using the "set colorDepth " action prefix.
     */
    public static void setColorDepth() {
        MenuBuilder builder = new MenuBuilder();

        int depth = DisplaySettings.getColorDepth();

        if (DisplaySettings.isFullscreen()) {
            int height = DisplaySettings.getHeight();
            int width = DisplaySettings.getWidth();
            GraphicsEnvironment environment
                    = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice device = environment.getDefaultScreenDevice();
            DisplayMode[] modes = device.getDisplayModes();
            for (DisplayMode mode : modes) {
                int modeDepth = mode.getBitDepth();
                int modeHeight = mode.getHeight();
                int modeWidth = mode.getWidth();
                if (modeDepth >= 16 && modeDepth != depth
                        && modeHeight == height && modeWidth == width) {
                    String modeItem = Integer.toString(modeDepth);
                    if (!builder.hasItem(modeItem)) {
                        builder.add(modeItem);
                    }
                }
            }

        } else {
            if (depth != 24) {
                builder.add("24");
            }
            if (depth != 32) {
                builder.add("32");
            }
        }

        builder.show(ActionPrefix.setColorDepth);
    }

    /**
     * Display a menu to set the (full-screen) display dimensions using the "set
     * dimensions " action prefix.
     */
    public static void setDimensions() {
        MenuBuilder builder = new MenuBuilder();

        int height = DisplaySettings.getHeight();
        int width = DisplaySettings.getWidth();

        GraphicsEnvironment environment
                = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice device = environment.getDefaultScreenDevice();
        DisplayMode[] modes = device.getDisplayModes();
        for (DisplayMode mode : modes) {
            int modeHeight = mode.getHeight();
            int modeWidth = mode.getWidth();
            if (modeHeight >= DisplaySettings.minHeight
                    && modeWidth >= DisplaySettings.minWidth
                    && (modeHeight != height || modeWidth != width)) {
                String modeItem
                        = MaudUtil.describeDimensions(modeWidth, modeHeight);
                if (!builder.hasItem(modeItem)) {
                    builder.add(modeItem);
                }
            }
        }

        builder.show(ActionPrefix.setDimensions);
    }

    /**
     * Display a menu to configure MSAA using the "set msaaFactor " action
     * prefix.
     */
    public static void setMsaaFactor() {
        MenuBuilder builder = new MenuBuilder();

        int selectedFactor = DisplaySettings.getMsaaFactor();
        for (int factor : new int[]{1, 2, 4, 6, 8, 16}) {
            if (factor != selectedFactor) {
                String description = MaudUtil.describeMsaaFactor(factor);
                builder.add(description);
            }
        }

        builder.show(ActionPrefix.setMsaaFactor);
    }

    /**
     * Display a menu to set the refresh rate for the display using the "set
     * refreshRate " action prefix.
     */
    public static void setRefreshRate() {
        if (DisplaySettings.isFullscreen()) {
            MenuBuilder builder = new MenuBuilder();
            int refreshRate = DisplaySettings.getRefreshRate();
            int height = DisplaySettings.getHeight();
            int width = DisplaySettings.getWidth();
            GraphicsEnvironment env
                    = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice device = env.getDefaultScreenDevice();
            DisplayMode[] modes = device.getDisplayModes();
            for (DisplayMode mode : modes) {
                int modeHeight = mode.getHeight();
                int modeRate = mode.getRefreshRate();
                int modeWidth = mode.getWidth();
                if (modeRate != refreshRate
                        && modeHeight == height && modeWidth == width) {
                    String modeItem = Integer.toString(modeRate);
                    if (!builder.hasItem(modeItem)) {
                        builder.add(modeItem);
                    }
                }
            }
            builder.show(ActionPrefix.setRefreshRate);
        }
    }

    /**
     * Display a submenu for selecting spatials by name using the "select
     * spatial" action prefix.
     *
     * @param nameList list of names from which to select (not null)
     * @param subset which kinds of spatials to include (not null)
     */
    static void showSpatialSubmenu(List<String> nameList,
            WhichSpatials subset) {
        assert nameList != null;
        assert subset != null;

        MyString.reduce(nameList, maxItems);
        Collections.sort(nameList);

        MenuBuilder builder = new MenuBuilder();
        Cgm target = Maud.getModel().getTarget();
        for (String name : nameList) {
            switch (subset) {
                case All:
                    break;

                case AttachmentsNodes:
                    if (!target.hasAttachmentsNode(name)) {
                        continue;
                    }
                    break;

                case Geometries:
                    if (!target.hasGeometry(name)) {
                        continue;
                    }
                    break;

                default:
                    throw new IllegalArgumentException();
            }

            if (target.hasGeometry(name)) {
                builder.addGeometry(name);
            } else if (target.hasNode(name)) {
                builder.addNode(name);
            } else {
                builder.addEllipsis(name);
            }       
        }
        builder.show(ActionPrefix.selectSpatial);
    }

    /**
     * Display a "CGM -> Source model" menu.
     */
    static void sourceCgm() {
        MenuBuilder builder = new MenuBuilder();

        builder.addSubmenu("Load");
        if (Maud.getModel().getSource().isLoaded()) {
            SelectedSpatial ss = Maud.getModel().getTarget().getSpatial();
            if (ss.isNode()) {
                builder.addEdit("Merge");
            }
            builder.add("Unload");
        }

        builder.show("select menuItem CGM -> Source model -> ");
    }
}
