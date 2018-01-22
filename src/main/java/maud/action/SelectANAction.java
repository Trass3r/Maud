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
package maud.action;

import com.jme3.shadow.EdgeFilteringMode;
import java.util.logging.Logger;
import jme3utilities.MyString;
import maud.Maud;
import maud.menu.AnimationMenus;
import maud.menu.BoneMenus;
import maud.menu.EditorMenus;
import maud.menu.EnumMenus;
import maud.menu.PhysicsMenus;
import maud.menu.ShowMenus;
import maud.menu.SpatialMenus;
import maud.model.EditorModel;
import maud.model.cgm.Cgm;
import maud.model.option.Background;
import maud.model.option.ShowBones;
import maud.model.option.scene.AxesDragEffect;
import maud.model.option.scene.AxesSubject;
import maud.model.option.scene.MovementMode;

/**
 * Process actions that start with the word "select" and a letter in the a-n
 * range.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class SelectANAction {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(SelectANAction.class.getName());
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private SelectANAction() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Process an ongoing action that starts with the word "select" and a letter
     * in the a-n range.
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    static boolean process(String actionString) {
        boolean handled = true;

        EditorModel model = Maud.getModel();
        Cgm target = model.getTarget();
        ShowBones currentOption;
        switch (actionString) {
            case Action.selectAnimationEditMenu:
                if (target.getAnimation().isReal()) {
                    AnimationMenus.editAnimation();
                }
                break;

            case Action.selectAnimControl:
                AnimationMenus.selectAnimControl(target);
                break;

            case Action.selectAxesDragEffect:
                EnumMenus.selectAxesDragEffect();
                break;

            case Action.selectAxesSubject:
                EnumMenus.selectAxesSubject();
                break;

            case Action.selectBackground:
                EnumMenus.selectBackground();
                break;

            case Action.selectBone:
                BoneMenus.selectBone();
                break;

            case Action.selectBoneChild:
                BoneMenus.selectBoneChild();
                break;

            case Action.selectBoneParent:
                target.getBone().selectParent();
                break;

            case Action.selectEdgeFilter:
                EnumMenus.selectEdgeFilter();
                break;

            case Action.selectJoint:
                PhysicsMenus.selectJoint(target);
                break;

            case Action.selectKeyframeFirst:
                target.getTrack().selectFirstKeyframe();
                break;

            case Action.selectKeyframeLast:
                target.getTrack().selectLastKeyframe();
                break;

            case Action.selectKeyframeNearest:
                target.getTrack().selectNearestKeyframe();
                break;

            case Action.selectKeyframeNext:
                target.getTrack().selectNextKeyframe();
                break;

            case Action.selectKeyframePrevious:
                target.getTrack().selectPreviousKeyframe();
                break;

            case Action.selectLight:
                ShowMenus.selectLight();
                break;

            case Action.selectLightOwner:
                target.getSpatial().selectLightOwner();
                break;

            case Action.selectMapSourceBone:
                model.getMap().selectFromSource();
                break;

            case Action.selectMapTargetBone:
                model.getMap().selectFromTarget();
                break;

            case Action.selectMatParam:
                ShowMenus.selectMatParam();
                break;

            default:
                handled = testForPrefixes(actionString);
        }

        return handled;
    }
    // *************************************************************************
    // private methods

    /**
     * Process an ongoing action that starts with the word "select" and a letter
     * in the a-n range -- 2nd part: test for prefixes.
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    private static boolean testForPrefixes(String actionString) {
        boolean handled = true;

        EditorModel model = Maud.getModel();
        Cgm target = model.getTarget();
        String arg;
        if (actionString.startsWith(ActionPrefix.selectAnimControl)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.selectAnimControl);
            target.getAnimControl().select(arg);

        } else if (actionString.startsWith(ActionPrefix.selectAxesDragEffect)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.selectAxesDragEffect);
            AxesDragEffect value = AxesDragEffect.valueOf(arg);
            model.getScene().getAxes().setDragEffect(value);

        } else if (actionString.startsWith(ActionPrefix.selectAxesSubject)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.selectAxesSubject);
            AxesSubject value = AxesSubject.valueOf(arg);
            model.getScene().getAxes().setSubject(value);

        } else if (actionString.startsWith(ActionPrefix.selectBackground)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.selectBackground);
            Background value = Background.valueOf(arg);
            model.getMisc().setBackground(value);

        } else if (actionString.startsWith(ActionPrefix.selectBone)) {
            arg = MyString.remainder(actionString, ActionPrefix.selectBone);
            BoneMenus.selectBone(arg);

        } else if (actionString.startsWith(ActionPrefix.selectBoneChild)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.selectBoneChild);
            ShowMenus.selectBoneChild(arg);

        } else if (actionString.startsWith(ActionPrefix.selectEdgeFilter)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.selectEdgeFilter);
            EdgeFilteringMode newMode = EdgeFilteringMode.valueOf(arg);
            model.getScene().getRender().setEdgeFilter(newMode);

        } else if (actionString.startsWith(ActionPrefix.selectGeometry)) {
            arg = MyString.remainder(actionString, ActionPrefix.selectGeometry);
            SpatialMenus.selectSpatial(arg, false);

        } else if (actionString.startsWith(ActionPrefix.selectJoint)) {
            arg = MyString.remainder(actionString, ActionPrefix.selectJoint);
            long id = Long.parseLong(arg, 16);
            target.getJoint().select(id);

        } else if (actionString.startsWith(ActionPrefix.selectLight)) {
            arg = MyString.remainder(actionString, ActionPrefix.selectLight);
            target.getLight().select(arg);

        } else if (actionString.startsWith(ActionPrefix.selectMatParam)) {
            arg = MyString.remainder(actionString, ActionPrefix.selectMatParam);
            target.getMatParam().select(arg);

        } else if (actionString.startsWith(ActionPrefix.selectMovement)) {
            arg = MyString.remainder(actionString, ActionPrefix.selectMovement);
            MovementMode mode = MovementMode.valueOf(arg);
            model.getScene().getCamera().setMode(mode);

        } else {
            handled = false;
        }

        if (!handled && actionString.startsWith(ActionPrefix.selectMenuItem)) {
            String menuPath = MyString.remainder(actionString,
                    ActionPrefix.selectMenuItem);
            handled = EditorMenus.selectMenuItem(menuPath);
        }

        return handled;
    }
}