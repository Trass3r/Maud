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

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Misc;
import jme3utilities.MyString;
import maud.model.LoadedCgm;

/**
 * Menus in Maud's "3D View" screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class DddMenus {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            DddMenus.class.getName());
    /**
     * level separator in menu action strings
     */
    final private static String menuSeparator = " -> ";
    // *************************************************************************
    // fields

    /**
     * reusable builder for popup menus
     */
    final private static MenuBuilder builder = new MenuBuilder();
    // *************************************************************************
    // new methods exposed

    /**
     * Handle a "load model file" action where the argument may be the name of a
     * folder/directory.
     *
     * @param filePath action argument (not null)
     */
    void loadModelFile(String filePath) {
        File file = new File(filePath);
        if (file.isDirectory()) {
            buildFileMenu(filePath);
            String menuPrefix = DddInputMode.loadCgmFilePrefix + filePath;
            if (!menuPrefix.endsWith("/")) {
                menuPrefix += "/";
            }
            builder.show(menuPrefix);

        } else if (file.canRead()) {
            Maud.model.target.loadModelFile(file);
        }
    }

    /**
     * Handle a "load sourceModel file" action where the argument may be the
     * name of a folder/directory.
     *
     * @param filePath action argument (not null)
     */
    void loadSourceModelFile(String filePath) {
        File file = new File(filePath);
        if (file.isDirectory()) {
            buildFileMenu(filePath);
            String menuPrefix;
            menuPrefix = DddInputMode.loadSourceCgmFilePrefix + filePath;
            if (!menuPrefix.endsWith("/")) {
                menuPrefix += "/";
            }
            builder.show(menuPrefix);

        } else if (file.canRead()) {
            Maud.model.source.loadModelFile(file);
        }
    }

    /**
     * Handle a "select bone" action with an argument.
     *
     * @param argument action argument (not null)
     */
    void selectBone(String argument) {
        if (Maud.model.target.bones.hasBone(argument)) {
            Maud.model.target.bone.select(argument);

        } else {
            /*
             * Treat the argument as a bone-name prefix.
             */
            List<String> boneNames;
            boneNames = Maud.model.target.bones.listBoneNames(argument);
            showBoneSubmenu(boneNames);
        }
    }

    /**
     * Handle a "select boneChild" action with no argument.
     */
    void selectBoneChild() {
        if (Maud.model.target.bone.isSelected()) {
            int numChildren = Maud.model.target.bone.countChildren();
            if (numChildren == 1) {
                Maud.model.target.bone.selectFirstChild();
            } else if (numChildren > 1) {
                List<String> boneNames;
                boneNames = Maud.model.target.bone.listChildNames();
                showBoneSubmenu(boneNames);
            }
        }
    }

    /**
     * Handle a "select boneChild" action with an argument.
     *
     * @param argument action argument (not null)
     */
    void selectBoneChild(String argument) {
        if (argument.startsWith("!")) {
            String name = argument.substring(1);
            Maud.model.target.bone.select(name);
        } else {
            List<String> names;
            names = Maud.model.target.bones.listChildBoneNames(argument);

            builder.reset();
            builder.addBone("!" + argument);
            for (String name : names) {
                if (Maud.model.target.bones.isLeafBone(name)) {
                    builder.addBone("!" + name);
                } else {
                    builder.add(name);
                }
            }
            builder.show(DddInputMode.selectBoneChildPrefix);
        }
    }

    /**
     * Handle a "select boneWithTrack" action.
     */
    void selectBoneWithTrack() {
        List<String> boneNames;
        boneNames = Maud.model.target.animation.listBonesWithTrack();
        int numBoneTracks = boneNames.size();
        if (numBoneTracks == 1) {
            Maud.model.target.bone.select(boneNames.get(0));
        } else if (numBoneTracks > 1) {
            showBoneSubmenu(boneNames);
        }
    }

    /**
     * Handle a "select menuItem" action for the "3D View" screen.
     *
     * @param menuPath path to menu item (not null)
     * @return true if the action is handled, otherwise false
     */
    boolean selectMenuItem(String menuPath) {
        boolean handled;
        int separatorBegin = menuPath.indexOf(menuSeparator);
        if (separatorBegin == -1) {
            handled = menuBar(menuPath);
        } else {
            int separatorEnd = separatorBegin + menuSeparator.length();
            String menuName = menuPath.substring(0, separatorBegin);
            String remainder = menuPath.substring(separatorEnd);
            handled = menu(menuName, remainder);
        }

        return handled;
    }

    /**
     * Handle a "select sourceBone" action with an argument.
     *
     * @param argument action argument (not null)
     */
    void selectSourceBone(String argument) {
        if (Maud.model.source.bones.hasBone(argument)) {
            Maud.model.source.bone.select(argument);
        } else {
            /*
             * Treat the argument as a bone-name prefix.
             */
            List<String> boneNames;
            boneNames = Maud.model.source.bones.listBoneNames(argument);
            showBoneSubmenu(boneNames);
        }
    }

    /**
     * Handle a "select spatialChild" action with no argument.
     */
    void selectSpatialChild() {
        int numChildren = Maud.model.target.spatial.countChildren();
        if (numChildren == 1) {
            Maud.model.target.spatial.selectChild(0);

        } else if (numChildren > 1) {
            builder.reset();
            for (int childIndex = 0; childIndex < numChildren; childIndex++) {
                String choice = String.format("#%d", childIndex + 1);
                String name;
                name = Maud.model.target.spatial.getChildName(childIndex);
                if (name != null) {
                    choice += " " + MyString.quote(name);
                }
                boolean isANode;
                isANode = Maud.model.target.spatial.isChildANode(childIndex);
                if (isANode) {
                    builder.addNode(choice);
                } else {
                    builder.addGeometry(choice);
                }
            }
            builder.show(DddInputMode.selectSpatialChildPrefix);
        }
    }

    /**
     * Handle a "select spatial" action with an argument.
     *
     * @param argument action argument (not null)
     */
    void selectSpatial(String argument, boolean includeNodes) {
        if (Maud.model.target.hasSpatial(argument)) {
            Maud.model.target.spatial.select(argument);

        } else {
            /*
             * Treat the argument as a spatial-name prefix.
             */
            List<String> names;
            names = Maud.model.target.listSpatialNames(argument, includeNodes);
            showSpatialSubmenu(names, includeNodes);
        }
    }

    /**
     * Display a menu for selecting a user data type using the "new userKey "
     * action prefix.
     */
    void selectUserDataType() {
        builder.reset();
        builder.add("integer");
        builder.add("float");
        builder.add("boolean");
        builder.add("string");
        builder.add("long");
        // TODO savable, list, map, array
        builder.show(DddInputMode.newUserKeyPrefix);
    }

    /**
     * Display a menu for selecting a user key using the "select userKey "
     * action prefix.
     */
    void selectUserKey() {
        builder.reset();
        List<String> keyList = Maud.model.target.spatial.listUserKeys();
        for (String key : keyList) {
            builder.add(key);
        }
        builder.show(DddInputMode.selectUserKeyPrefix);
    }
    // *************************************************************************
    // private methods

    /**
     * Display a "Spatial -> Add control" menu.
     */
    private void addSgc() {
        builder.reset();
        builder.add("Anim");
        builder.add("RigidBody");
        builder.add("Skeleton");
        builder.show("select menuItem Spatial -> Add control -> ");
    }

    /**
     * Build an Animation menu.
     */
    private void buildAnimationMenu() {
        builder.addTool("Tool");
        builder.add("Load");
        builder.addDialog("New from pose");
        builder.addDialog("New from copy");
        builder.addTool("Source tool");
        if (Maud.model.source.isLoaded()) {
            builder.add("Load source");
        }
        builder.addTool("New from retarget");
        if (Maud.model.target.animation.isReal()) {
            builder.addDialog("Duration");
            builder.addDialog("Reduce");
            builder.addDialog("Rename");
            builder.add("Tweening");
            builder.addDialog("Delete");
        }
    }

    /**
     * Build a Bone menu.
     */
    private void buildBoneMenu() {
        builder.addTool("Tool");
        builder.add("Select");
        builder.addTool("Rotate");
        builder.addTool("Scale");
        builder.addTool("Translate");
        if (Maud.model.target.bone.isSelected()) {
            builder.add("Attach prop");
            builder.addDialog("Rename");
        }
        if (Maud.model.source.isLoaded()) {
            builder.add("Select source");
        }
    }

    /**
     * Build a "Bone -> Select" menu.
     */
    private void buildBoneSelectMenu() {
        builder.add("By name");

        int numBones = Maud.model.target.bones.countBones();
        if (numBones > 0) {
            builder.add("By parent");
        }

        int numRoots = Maud.model.target.bones.countRootBones();
        if (numRoots == 1) {
            builder.addBone("Root");
        } else if (numRoots > 1) {
            builder.add("Root");
        }

        int numTracks = Maud.model.target.animation.countBoneTracks();
        if (numTracks > 0) {
            builder.add("With track");
        }

        String sourceBoneName = Maud.model.source.bone.getName();
        String boneName = Maud.model.mapping.targetBoneName(sourceBoneName);
        if (boneName != null && Maud.model.target.bones.hasBone(boneName)) {
            builder.addBone("Mapped");
        }

        int numChildren = Maud.model.target.bone.countChildren();
        if (numChildren == 1) {
            builder.addBone("Child");
        } else if (numChildren > 1) {
            builder.add("Child");
        }

        boolean isSelected = Maud.model.target.bone.isSelected();
        boolean isRoot = Maud.model.target.bone.isRootBone();
        if (isSelected && !isRoot) {
            builder.addBone("Parent");
        }
        if (isSelected) {
            builder.addBone("Next");
            builder.addBone("Previous");
        }
    }

    /**
     * Build a CGM menu.
     */
    private void buildCgmMenu() {
        builder.add("Target");
        builder.add("Source");
        builder.add("Mapping");
        builder.addTool("History");
    }

    /**
     * Build a menu of the files (and subdirectories/subfolders) in the
     * specified directory/folder.
     *
     * @param path file path to the directory/folder (not null)
     */
    private void buildFileMenu(String path) {
        assert path != null;

        builder.reset();

        File file = new File(path);
        File[] files = file.listFiles();
        if (files == null) {
            return;
        }

        if (file.getParentFile() != null) {
            builder.addFolder("..");
        }
        for (File f : files) {
            String name = f.getName();
            if (f.isDirectory()) {
                builder.addFolder(name);
            } else if (name.endsWith(".blend")) {
                builder.addBlend(name);
            } else if (name.endsWith(".j3o")) {
                builder.addJme(name);
            } else if (name.endsWith(".mesh.xml")) {
                builder.addOgre(name);
            } else {
                builder.add(name);
            }
        }
    }

    /**
     * Build a Help menu.
     */
    private void buildHelpMenu() {
        builder.addDialog("About Maud");
        builder.addDialog("License");
        builder.add("Wiki");
        builder.add("Javadoc");
        builder.add("Source");
        builder.add("JME3 homepage");
    }

    /**
     * Build a Keyframe menu.
     */
    private void buildKeyframeMenu() {
        builder.addTool("Tool");
        if (Maud.model.target.bone.hasTrack()) {
            builder.addDialog("Reduce");
            builder.add("Select by time");
            builder.add("Select first");
            builder.add("Select previous");
            builder.add("Select next");
            builder.add("Select last");
            builder.add("Move");
            builder.add("Copy");
            builder.add("New from pose");
            builder.add("Delete");
        }
    }

    /**
     * Build a Physics menu.
     */
    private void buildPhysicsMenu() {
        builder.addTool("Tool");
        builder.add("Add");
        builder.add("Mass");
        builder.add("Remove");
    }

    /**
     * Build a Settings menu.
     */
    private void buildSettingsMenu() {
        builder.add("Initial model");
        builder.add("Hotkeys");
        builder.add("Locale");
    }

    /**
     * Build a "Bone -> Select source" menu.
     */
    private void buildSourceBoneSelectMenu() {
        int numRoots = Maud.model.source.bones.countRootBones();
        if (numRoots == 1) {
            builder.addBone("Root");
        } else if (numRoots > 1) {
            builder.add("Root");
        }

        String targetBoneName = Maud.model.target.bone.getName();
        String boneName = Maud.model.mapping.sourceBoneName(targetBoneName);
        if (boneName != null && Maud.model.source.bones.hasBone(boneName)) {
            builder.addBone("Mapped");
        }
    }

    /**
     * Build a Spatial menu.
     */
    private void buildSpatialMenu() {
        builder.addTool("Tool");
        builder.add("Select");
        builder.addTool("Rotate");
        builder.addTool("Scale");
        builder.addTool("Translate");
        builder.addTool("Control tool");
        builder.add("Select control");
        builder.add("Add control");
        if (Maud.model.target.sgc.isSelected()) {
            builder.add("Delete control");
        }
        builder.addTool("User data tool");
        builder.addTool("Material");
    }

    /**
     * Build a "CGM -> Source/Target -> Load -> Testdata" menu.
     */
    private void buildTestDataMenu() {
        builder.reset();
        /*
         * Add items for the CG models in the jme3-testdata asset pack.
         *
         * animated models:
         */
        builder.addOgre("Elephant");
        builder.addJme("Jaime");
        builder.addOgre("Ninja");
        builder.addOgre("Oto");
        builder.addOgre("Sinbad");
        /*
         * non-animated models:
         */
        builder.addJme("Boat");
        builder.addJme("Buggy");
        builder.add("Ferrari");
        builder.addOgre("HoverTank");
        builder.addOgre("MonkeyHead");
        builder.addOgre("Sign Post");
        builder.addOgre("SpaceCraft");
        builder.add("Teapot");
        builder.addOgre("Tree");
    }

    /**
     * Build a View menu.
     */
    private void buildViewMenu() {
        builder.addTool("Axes");
        builder.addTool("Bounds");
        builder.addTool("Camera");
        builder.addTool("Cursor");
        builder.addTool("Physics");
        builder.addTool("Platform");
        builder.addTool("Render");
        builder.addTool("Skeleton");
        builder.addTool("Skeleton color");
        builder.addTool("Sky");
    }

    /**
     * Display a "Animation -> Load source" menu.
     */
    private void loadSourceAnimation() {
        if (Maud.model.source.isLoaded()) {
            Collection<String> animationNames;
            animationNames = Maud.model.source.listAnimationNames();
            Maud.gui.showPopupMenu(DddInputMode.loadSourceAnimationPrefix,
                    animationNames);
        }
    }

    /**
     * Display a "CGM -> Source -> Load" menu.
     */
    private void loadSourceCgm() {
        builder.reset();
        builder.add("Testdata");
        builder.addDialog("Asset");
        builder.add("File");
        builder.show("select menuItem CGM -> Source -> Load -> ");
    }

    /**
     * Display a "CGM -> Target -> Load" menu.
     */
    private void loadTargetCgm() {
        builder.reset();
        builder.add("Testdata");
        builder.addDialog("Asset");
        builder.add("File");
        builder.show("select menuItem CGM -> Target -> Load -> ");
    }

    /**
     * Display a "CGM -> Mapping" menu.
     */
    private void mapping() {
        builder.reset();
        builder.addTool("Tool");
        builder.addDialog("Load");
        if (Maud.model.mapping.countMappings() > 0) {
            builder.add("Invert");
            builder.add("Unload");
        }
        builder.addDialog("Save");
        builder.addTool("Twist tool");
        builder.show("select menuItem CGM -> Mapping -> ");
    }

    /**
     * Handle a "select menuItem" action for a submenu.
     *
     * @param menuName name of the menu (not null)
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean menu(String menuName, String remainder) {
        assert menuName != null;
        assert remainder != null;

        boolean handled = false;
        switch (menuName) {
            case "Animation":
                handled = menuAnimation(remainder);
                break;
            case "Bone":
                handled = menuBone(remainder);
                break;
            case "CGM":
                handled = menuCgm(remainder);
                break;
            case "Help":
                handled = menuHelp(remainder);
                break;
            case "Keyframe":
                handled = menuKeyframe(remainder);
                break;
            case "Physics":
                //handled = menuPhysics(remainder);
                break;
            case "Settings":
                handled = menuSettings(remainder);
                break;
            case "Spatial":
                handled = menuSpatial(remainder);
                break;
            case "View":
                handled = menuView(remainder);
        }

        return handled;
    }

    /**
     * Handle a "select menuItem" action from the Animation menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean menuAnimation(String remainder) {
        assert remainder != null;

        boolean handled = true;
        switch (remainder) {
            case "Delete":
                Maud.gui.dialogs.deleteAnimation();
                break;
            case "Duration":
                Maud.gui.dialogs.setDuration();
                break;
            case "Load":
                Collection<String> animationNames;
                animationNames = Maud.model.target.listAnimationNames();
                Maud.gui.showPopupMenu(DddInputMode.loadAnimationPrefix,
                        animationNames);
                break;
            case "Load source":
                loadSourceAnimation();
                break;
            case "New from copy":
                Maud.gui.dialogs.copyAnimation();
                break;
            case "New from pose":
                Maud.gui.dialogs.newPose();
                break;
            case "New from retarget":
                Maud.gui.tools.select("retarget");
                break;
            case "Reduce":
                Maud.gui.dialogs.reduceAnimation();
                break;
            case "Rename":
                Maud.gui.dialogs.renameAnimation();
                break;
            case "Source tool":
                Maud.gui.tools.select("sourceAnimation");
                break;
            case "Tool":
                Maud.gui.tools.select("animation");
                break;
            default:
                handled = false;
        }

        return handled;
    }

    /**
     * Handle a "select menuItem" action for a top-level menu, typically from
     * the menu bar.
     *
     * @param menuName name of the menu to open (not null)
     * @return true if handled, otherwise false
     */
    private boolean menuBar(String menuName) {
        assert menuName != null;
        /**
         * Dynamically generate the menu's item list.
         */
        builder.reset();
        switch (menuName) {
            case "Animation":
                buildAnimationMenu();
                break;
            case "Bone":
                buildBoneMenu();
                break;
            case "CGM":
                buildCgmMenu();
                break;
            case "Help":
                buildHelpMenu();
                break;
            case "Keyframe":
                buildKeyframeMenu();
                break;
            case "Physics":
                buildPhysicsMenu();
                break;
            case "Settings":
                buildSettingsMenu();
                break;
            case "Spatial":
                buildSpatialMenu();
                break;
            case "View":
                buildViewMenu();
                break;
            default:
                return false;
        }
        if (builder.isEmpty()) {
            logger.log(Level.WARNING, "no items for the {0} menu",
                    MyString.quote(menuName));
        } else {
            String actionPrefix = DddInputMode.selectMenuItemPrefix + menuName
                    + menuSeparator;
            builder.show(actionPrefix);
        }

        return true;
    }

    /**
     * Handle a "select menuItem" action from the Bone menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean menuBone(String remainder) {
        assert remainder != null;

        boolean handled = true;
        String selectPrefix = "Select" + menuSeparator;
        String selectSourcePrefix = "Select source" + menuSeparator;
        if (remainder.startsWith(selectPrefix)) {
            String selectArg = MyString.remainder(remainder, selectPrefix);
            handled = menuBoneSelect(selectArg);

        } else if (remainder.startsWith(selectSourcePrefix)) {
            String selectArg = MyString.remainder(remainder, selectSourcePrefix);
            handled = menuBoneSelectSource(selectArg);

        } else {
            switch (remainder) {
                case "Rename":
                    Maud.gui.dialogs.renameBone();
                    break;
                case "Rotate":
                    Maud.gui.tools.select("boneRotation");
                    break;
                case "Scale":
                    Maud.gui.tools.select("boneScale");
                    break;
                case "Select":
                    selectBone();
                    break;
                case "Select source":
                    selectSourceBone();
                    break;
                case "Tool":
                    Maud.gui.tools.select("bone");
                    break;
                case "Translate":
                    Maud.gui.tools.select("boneTranslation");
                    break;
                default:
                    handled = false;
            }
        }

        return handled;
    }

    /**
     * Handle a "select menuItem" action from the "Bone -> Select" menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean menuBoneSelect(String remainder) {
        assert remainder != null;

        boolean handled = false;
        switch (remainder) {
            case "By name":
                selectBoneByName();
                handled = true;
                break;
            case "By parent":
                selectBoneByParent();
                handled = true;
                break;
            case "Child":
                selectBoneChild();
                handled = true;
                break;
            case "Mapped":
                Maud.model.mapping.selectFromSource();
                handled = true;
                break;
            case "Next":
                Maud.model.target.bone.selectNext();
                handled = true;
                break;
            case "Parent":
                Maud.model.target.bone.selectParent();
                handled = true;
                break;
            case "Previous":
                Maud.model.target.bone.selectPrevious();
                handled = true;
                break;
            case "Root":
                selectRootBone();
                handled = true;
                break;
            case "With track":
                selectBoneWithTrack();
                handled = true;
        }

        return handled;
    }

    /**
     * Handle a "select menuItem" action from the "Bone -> Select source" menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean menuBoneSelectSource(String remainder) {
        assert remainder != null;

        boolean handled = false;
        switch (remainder) {
            case "Mapped":
                Maud.model.mapping.selectFromTarget();
                handled = true;
                break;
            case "Root":
                selectSourceRootBone();
                handled = true;
        }

        return handled;
    }

    /**
     * Handle a "select menuItem" action from the CGM menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean menuCgm(String remainder) {
        assert remainder != null;

        boolean handled = false;
        String mappingPrefix = "Mapping" + menuSeparator;
        String sourcePrefix = "Source" + menuSeparator;
        String targetPrefix = "Target" + menuSeparator;
        if (remainder.startsWith(mappingPrefix)) {
            String selectArg = MyString.remainder(remainder, mappingPrefix);
            handled = menuMapping(selectArg);

        } else if (remainder.startsWith(sourcePrefix)) {
            String selectArg = MyString.remainder(remainder, sourcePrefix);
            handled = menuSourceCgm(selectArg);

        } else if (remainder.startsWith(targetPrefix)) {
            String selectArg = MyString.remainder(remainder, targetPrefix);
            handled = menuTargetCgm(selectArg);

        } else {
            switch (remainder) {
                case "History":
                    Maud.gui.tools.select("history");
                    handled = true;
                    break;

                case "Mapping":
                    mapping();
                    handled = true;
                    break;

                case "Source":
                    sourceCgm();
                    handled = true;
                    break;

                case "Target":
                    targetCgm();
                    handled = true;
            }
        }

        return handled;
    }

    /**
     * Handle a "select menuItem" action from the Help menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean menuHelp(String remainder) {
        assert remainder != null;

        boolean handled = false;
        switch (remainder) {
            case "About Maud":
                Maud.gui.dialogs.aboutMaud();
                handled = true;
                break;

            case "JME3 homepage":
                Misc.browseWeb("http://jmonkeyengine.org/");
                handled = true;
                break;

            case "License":
                Maud.gui.dialogs.license();
                handled = true;
                break;

            case "Source":
                Misc.browseWeb("https://github.com/stephengold/Maud");
                handled = true;
        }

        return handled;
    }

    /**
     * Handle a "select menuItem" action from the "CGM -> Source -> Load" menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean menuLoadSourceCgm(String remainder) {
        assert remainder != null;

        boolean handled = false;
        switch (remainder) {
            case "Asset":
                Maud.gui.dialogs.loadCgmFromAsset(
                        DddInputMode.loadSourceCgmAssetPrefix,
                        Maud.model.source);
                handled = true;
                break;

            case "File":
                buildFileMenu("/");
                builder.show(DddInputMode.loadSourceCgmFilePrefix + "/");
                handled = true;
                break;

            case "Testdata":
                buildTestDataMenu();
                builder.show(DddInputMode.loadSourceCgmNamedPrefix);
                handled = true;
        }

        return handled;
    }

    /**
     * Handle a "select menuItem" action from the "CGM -> Target -> Load" menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean menuLoadTargetCgm(String remainder) {
        assert remainder != null;

        boolean handled = false;
        switch (remainder) {
            case "Asset":
                Maud.gui.dialogs.loadCgmFromAsset(
                        DddInputMode.loadCgmAssetPrefix,
                        Maud.model.target);
                handled = true;
                break;

            case "File":
                buildFileMenu("/");
                builder.show(DddInputMode.loadCgmFilePrefix + "/");
                handled = true;
                break;

            case "Testdata":
                buildTestDataMenu();
                builder.show(DddInputMode.loadCgmNamedPrefix);
                handled = true;
        }

        return handled;
    }

    /**
     * Handle a "select menuItem" action the Keyframe menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean menuKeyframe(String remainder) {
        assert remainder != null;

        boolean handled = false;
        switch (remainder) {
            case "Reduce":
                Maud.gui.dialogs.reduceTrack();
                handled = true;
                break;
            case "Select first":
                Maud.model.target.animation.selectKeyframeFirst();
                handled = true;
                break;
            case "Select last":
                Maud.model.target.animation.selectKeyframeLast();
                handled = true;
                break;
            case "Select next":
                Maud.model.target.animation.selectKeyframeNext();
                handled = true;
                break;
            case "Select previous":
                Maud.model.target.animation.selectKeyframePrevious();
                handled = true;
                break;
            case "Tool":
                Maud.gui.tools.select("keyframe");
                handled = true;
        }

        return handled;
    }

    /**
     * Handle a "select menuItem" action from the "CGM -> Mapping" menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean menuMapping(String remainder) {
        assert remainder != null;

        boolean handled = false;
        switch (remainder) {
            case "Invert":
                Maud.model.mapping.invert();
                handled = true;
                break;

            case "Load":
                Maud.gui.dialogs.loadMappingAsset();
                handled = true;
                break;

            case "Save":
                Maud.gui.dialogs.saveMappingToAsset();
                handled = true;
                break;

            case "Tool":
                Maud.gui.tools.select("mapping");
                handled = true;
                break;

            case "Twist tool":
                Maud.gui.tools.select("twist");
                handled = true;
                break;

            case "Unload":
                Maud.model.mapping.unload();
                handled = true;
        }

        return handled;
    }

    /**
     * Handle a "select menuItem" action from the Settings menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean menuSettings(String remainder) {
        assert remainder != null;

        boolean handled = false;
        switch (remainder) {
            case "Hotkeys":
                Maud.gui.closeAllPopups();
                Maud.bindScreen.activate(Maud.gui.inputMode);
                handled = true;
        }

        return handled;
    }

    /**
     * Handle a "select menuItem" action from the "CGM -> Source" menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean menuSourceCgm(String remainder) {
        assert remainder != null;

        boolean handled = false;
        String loadPrefix = "Load" + menuSeparator;
        if (remainder.startsWith(loadPrefix)) {
            String selectArg = MyString.remainder(remainder, loadPrefix);
            handled = menuLoadSourceCgm(selectArg);

        } else {
            switch (remainder) {
                case "Load":
                    loadSourceCgm();
                    handled = true;
                    break;

                case "Unload":
                    Maud.model.source.unload();
                    handled = true;
            }
        }

        return handled;
    }

    /**
     * Handle a "select menuItem" action from the Spatial menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean menuSpatial(String remainder) {
        assert remainder != null;

        boolean handled = true;
        String addControlPrefix = "Add control" + menuSeparator;
        String selectPrefix = "Select" + menuSeparator;
        if (remainder.startsWith(addControlPrefix)) {
            String arg = MyString.remainder(remainder, addControlPrefix);
            handled = menuSpatialAddControl(arg);

        } else if (remainder.startsWith(selectPrefix)) {
            String arg = MyString.remainder(remainder, selectPrefix);
            handled = menuSpatialSelect(arg);

        } else {
            switch (remainder) {
                case "Add control":
                    addSgc();
                    break;
                case "Control tool":
                    Maud.gui.tools.select("sgc");
                    break;
                case "Delete control":
                    Maud.gui.dialogs.deleteSgc();
                    break;
                case "Rotate":
                    Maud.gui.tools.select("spatialRotation");
                    break;
                case "Scale":
                    Maud.gui.tools.select("spatialScale");
                    break;
                case "Select":
                    selectSpatial();
                    break;
                case "Select control":
                    selectSgc();
                    break;
                case "Tool":
                    Maud.gui.tools.select("spatial");
                    break;
                case "Translate":
                    Maud.gui.tools.select("spatialTranslation");
                    break;
                case "User data tool":
                    Maud.gui.tools.select("userData");
                    break;
                default:
                    handled = false;
            }
        }

        return handled;
    }

    /**
     * Handle a "select menuItem" action from the "Spatial -> Add control" menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean menuSpatialAddControl(String remainder) {
        assert remainder != null;

        boolean handled = false;
        switch (remainder) {
            case "Anim":
                Maud.model.target.spatial.addAnimControl();
                handled = true;
                break;

            case "RigidBody":
                Maud.model.target.spatial.addRigidBodyControl();
                handled = true;
                break;

            case "Skeleton":
                Maud.model.target.spatial.addSkeletonControl();
                handled = true;
        }

        return handled;
    }

    /**
     * Handle a "select menuItem" action from the "Spatial -> Select" menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean menuSpatialSelect(String remainder) {
        assert remainder != null;

        boolean handled = false;
        switch (remainder) {
            case "By name":
                selectSpatial("", true);
                handled = true;
                break;
            case "Child":
                selectSpatialChild();
                handled = true;
                break;
            case "Geometry":
                selectSpatial("", false);
                handled = true;
                break;
            case "Parent":
                Maud.model.target.spatial.selectParent();
                handled = true;
                break;
            case "Root":
                Maud.model.target.spatial.selectModelRoot();
                handled = true;
        }

        return handled;
    }

    /**
     * Handle a "select menuItem" action from the "CGM -> Target" menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean menuTargetCgm(String remainder) {
        assert remainder != null;

        boolean handled = false;
        String loadPrefix = "Load" + menuSeparator;
        if (remainder.startsWith(loadPrefix)) {
            String selectArg = MyString.remainder(remainder, loadPrefix);
            handled = menuLoadTargetCgm(selectArg);

        } else {
            switch (remainder) {
                case "Load":
                    loadTargetCgm();
                    handled = true;
                    break;

                case "Save as asset":
                    Maud.gui.dialogs.saveCgmToAsset();
                    handled = true;
                    break;

                case "Save as file":
                    Maud.gui.dialogs.saveCgmToFile();
                    handled = true;
                    break;

                case "Tool":
                    Maud.gui.tools.select("cgm");
                    handled = true;
            }
        }

        return handled;
    }

    /**
     * Handle a "select menuItem" action from the View menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean menuView(String remainder) {
        assert remainder != null;

        boolean handled = false;
        switch (remainder) {
            case "Axes":
                Maud.gui.tools.select("axes");
                handled = true;
                break;
            case "Bounds":
                Maud.gui.tools.select("bounds");
                handled = true;
                break;
            case "Camera":
                Maud.gui.tools.select("camera");
                handled = true;
                break;
            case "Cursor":
                Maud.gui.tools.select("cursor");
                handled = true;
                break;
            case "Physics":
                break;
            case "Platform":
                Maud.gui.tools.select("platform");
                handled = true;
                break;
            case "Render":
                Maud.gui.tools.select("render");
                handled = true;
                break;
            case "Skeleton":
                Maud.gui.tools.select("skeleton");
                handled = true;
                break;
            case "Skeleton color":
                Maud.gui.tools.select("skeletonColor");
                handled = true;
                break;
            case "Sky":
                Maud.gui.tools.select("sky");
                handled = true;
        }

        return handled;
    }

    /**
     * Handle a "select bone" action without an argument.
     */
    private void selectBone() {
        builder.reset();
        buildBoneSelectMenu();
        builder.show("select menuItem Bone -> Select -> ");
    }

    /**
     * Select a bone by name, using submenus.
     */
    private void selectBoneByName() {
        List<String> nameList = Maud.model.target.bones.listBoneNames();
        showBoneSubmenu(nameList);
    }

    /**
     * Select a bone by parent, using submenus.
     */
    private void selectBoneByParent() {
        List<String> boneNames = Maud.model.target.bones.listRootBoneNames();
        Maud.gui.showPopupMenu(DddInputMode.selectBoneChildPrefix, boneNames);
    }

    /**
     * Handle a "select rootBone" action.
     */
    private void selectRootBone() {
        int numRoots = Maud.model.target.bones.countRootBones();
        if (numRoots == 1) {
            Maud.model.target.bone.selectFirstRoot();
        } else if (numRoots > 1) {
            List<String> boneNames = Maud.model.target.bones.listRootBoneNames();
            showBoneSubmenu(boneNames);
        }
    }

    /**
     * Display a "Spatial -> Select control" menu.
     */
    private void selectSgc() {
        builder.reset();
        for (String name : Maud.model.target.spatial.listSgcNames()) {
            builder.add(name);
        }
        builder.add(LoadedCgm.noControl);
        builder.show(DddInputMode.selectControlPrefix);
    }

    /**
     * Display a "Bone -> Select source" menu.
     */
    private void selectSourceBone() {
        if (Maud.model.source.isLoaded()) {
            builder.reset();
            buildSourceBoneSelectMenu();
            builder.show("select menuItem Bone -> Select source -> ");
        }
    }

    /**
     * Handle a "select sourceRootBone" action.
     */
    private void selectSourceRootBone() {
        int numRoots = Maud.model.source.bones.countRootBones();
        if (numRoots == 1) {
            Maud.model.source.bone.selectFirstRoot();
        } else if (numRoots > 1) {
            List<String> names = Maud.model.source.bones.listRootBoneNames();
            showSourceBoneSubmenu(names);
        }
    }

    /**
     * Display a "Spatial -> Select" menu.
     */
    private void selectSpatial() {
        builder.reset();

        List<String> names = Maud.model.target.listSpatialNames("", true);
        if (!names.isEmpty()) {
            builder.add("By name");
        }

        boolean isRootANode = Maud.model.target.isRootANode();
        if (isRootANode) {
            builder.addNode("Root");
        } else {
            builder.addGeometry("Root");
        }

        names = Maud.model.target.listSpatialNames("", false);
        if (!names.isEmpty()) {
            builder.add("Geometry");
        }

        int numChildren = Maud.model.target.spatial.countChildren();
        if (numChildren == 1) {
            boolean isChildANode = Maud.model.target.spatial.isChildANode(0);
            if (isChildANode) {
                builder.addNode("Child");
            } else {
                builder.addGeometry("Child");
            }
        } else if (numChildren > 1) {
            builder.add("Child");
        }

        boolean isRoot = Maud.model.target.spatial.isModelRoot();
        if (!isRoot) {
            builder.addNode("Parent");
        }

        builder.show("select menuItem Spatial -> Select -> ");
    }

    /**
     * Display a submenu for selecting a target bone by name using the "select
     * bone" action prefix.
     *
     * @param nameList list of names from which to select (not null)
     */
    private void showBoneSubmenu(List<String> nameList) {
        assert nameList != null;

        MyString.reduce(nameList, 20);
        Collections.sort(nameList);

        builder.reset();
        for (String name : nameList) {
            if (Maud.model.target.bones.hasBone(name)) {
                builder.addBone(name);
            } else {
                builder.add(name);
            }
        }
        builder.show(DddInputMode.selectBonePrefix);
    }

    /**
     * Display a submenu for selecting a source bone by name using the "select
     * sourceBone" action prefix.
     *
     * @param nameList list of names from which to select (not null)
     */
    private void showSourceBoneSubmenu(List<String> nameList) {
        assert nameList != null;

        MyString.reduce(nameList, 20);
        Collections.sort(nameList);

        builder.reset();
        for (String name : nameList) {
            if (Maud.model.source.bones.hasBone(name)) {
                builder.addBone(name);
            } else {
                builder.add(name);
            }
        }
        builder.show(DddInputMode.selectSourceBonePrefix);
    }

    /**
     * Display a submenu for selecting spatials by name using the "select
     * spatial" action prefix.
     *
     * @param nameList list of names from which to select (not null)
     * @param includeNodes true &rarr; both nodes and geometries, false &rarr;
     * geometries only
     */
    private void showSpatialSubmenu(List<String> nameList,
            boolean includeNodes) {
        assert nameList != null;

        MyString.reduce(nameList, 20);
        Collections.sort(nameList);

        builder.reset();
        for (String name : nameList) {
            if (Maud.model.target.hasGeometry(name)) {
                builder.addGeometry(name);
            } else if (includeNodes && Maud.model.target.hasNode(name)) {
                builder.addNode(name);
            } else {
                builder.add(name);
            }
        }
        if (includeNodes) {
            builder.show(DddInputMode.selectSpatialPrefix);
        } else {
            builder.show(DddInputMode.selectGeometryPrefix);
        }
    }

    /**
     * Display a "CGM -> Source" menu.
     */
    private void sourceCgm() {
        builder.reset();

        builder.add("Load");
        if (Maud.model.source.isLoaded()) {
            builder.add("Unload");
        }

        builder.show("select menuItem CGM -> Source -> ");
    }

    /**
     * Display a "CGM -> Target" menu.
     */
    private void targetCgm() {
        builder.reset();

        builder.addTool("Tool");
        builder.add("Load");
        builder.addDialog("Save as asset");
        builder.addDialog("Save as file");

        builder.show("select menuItem CGM -> Target -> ");
    }
}
