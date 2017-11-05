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
package maud.action;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Spatial;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.wes.TweenRotations;
import jme3utilities.wes.TweenVectors;
import maud.Maud;
import maud.Util;
import maud.dialog.EditorDialogs;
import maud.menu.ShowMenus;
import maud.model.EditorModel;
import maud.model.cgm.EditableCgm;
import maud.model.option.DisplaySettings;
import maud.model.option.RigidBodyParameter;
import maud.model.option.ShowBones;
import maud.model.option.ViewMode;
import maud.model.option.scene.AxesMode;

/**
 * Process an action string that begins with "set".
 *
 * @author Stephen Gold sgold@sonic.net
 */
class SetAction {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(SetAction.class.getName());
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private SetAction() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Process an action string that begin with "set".
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    static boolean process(String actionString) {
        boolean handled = true;

        EditorModel model = Maud.getModel();
        EditableCgm target = model.getTarget();
        ShowBones currentOption;
        switch (actionString) {
            case Action.setAntiAliasing:
                ShowMenus.setAntiAliasing();
                break;

            case Action.setAxesMode:
                ShowMenus.setAxesMode();
                break;

            case Action.setBatchHint:
                ShowMenus.setBatchHint();
                break;

            case Action.setColorDepth:
                ShowMenus.setColorDepth();
                break;

            case Action.setCullHint:
                ShowMenus.setCullHint();
                break;

            case Action.setPhysicsRbpValue:
                RigidBodyParameter rbp = model.getMisc().getRbp();
                EditorDialogs.setPhysicsRbpValue(rbp);
                break;

            case Action.setQueueBucket:
                ShowMenus.setQueueBucket();
                break;

            case Action.setRefreshRate:
                ShowMenus.setRefreshRate();
                break;

            case Action.setResolution:
                ShowMenus.setResolution();
                break;

            case Action.setSceneBones:
                currentOption = model.getScene().getSkeleton().getShowBones();
                ShowMenus.setShowBones(ActionPrefix.setSceneBones,
                        currentOption);
                break;

            case Action.setScoreBonesNone:
                currentOption = model.getScore().getShowNoneSelected();
                ShowMenus.setShowBones(ActionPrefix.setScoreBonesNone,
                        currentOption);
                break;

            case Action.setScoreBonesWhen:
                currentOption = model.getScore().getShowWhenSelected();
                ShowMenus.setShowBones(ActionPrefix.setScoreBonesWhen,
                        currentOption);
                break;

            case Action.setShadowMode:
                ShowMenus.setShadowMode();
                break;

            case Action.setSpatialAngleCardinal:
                target.getSpatial().cardinalizeRotation();
                break;

            case Action.setSpatialAngleSnapX:
                target.getSpatial().snapRotation(PhysicsSpace.AXIS_X);
                break;

            case Action.setSpatialAngleSnapY:
                target.getSpatial().snapRotation(PhysicsSpace.AXIS_Y);
                break;

            case Action.setSpatialAngleSnapZ:
                target.getSpatial().snapRotation(PhysicsSpace.AXIS_Z);
                break;

            case Action.setTrackRotationAll:
                target.getTrack().setRotationAll();
                break;

            case Action.setTrackScaleAll:
                target.getTrack().setScaleAll();
                break;

            case Action.setTrackTranslationAll:
                target.getTrack().setTranslationAll();
                break;

            case Action.setTweenRotations:
                ShowMenus.setTweenRotations();
                break;

            case Action.setTweenScales:
                ShowMenus.setTweenScales();
                break;

            case Action.setTweenTranslations:
                ShowMenus.setTweenTranslations();
                break;

            case Action.setTwistCardinal:
                model.getMap().cardinalizeTwist();
                break;

            case Action.setTwistSnapX:
                model.getMap().snapTwist(PhysicsSpace.AXIS_X);
                break;

            case Action.setTwistSnapY:
                model.getMap().snapTwist(PhysicsSpace.AXIS_Y);
                break;

            case Action.setTwistSnapZ:
                model.getMap().snapTwist(PhysicsSpace.AXIS_Z);
                break;

            case Action.setUserData:
                EditorDialogs.setUserData();
                break;

            default:
                handled = testForPrefixes(actionString);
        }

        return handled;
    }
    // *************************************************************************
    // private methods

    /**
     * Process an action that starts with "set" -- 2nd part: test for prefixes.
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    private static boolean testForPrefixes(String actionString) {
        boolean handled = true;

        EditorModel model = Maud.getModel();
        EditableCgm target = model.getTarget();
        String arg;
        if (actionString.startsWith(ActionPrefix.setAntiAliasing)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.setAntiAliasing);
            int numSamples;
            for (numSamples = 1; numSamples < 16; numSamples *= 2) {
                String aaDescription = Util.aaDescription(numSamples);
                if (arg.equals(aaDescription)) {
                    break;
                }
            }
            DisplaySettings.get().setSamples(numSamples);
            DisplaySettings.save();

        } else if (actionString.startsWith(ActionPrefix.setAxesMode)) {
            arg = MyString.remainder(actionString, ActionPrefix.setAxesMode);
            AxesMode value = AxesMode.valueOf(arg);
            model.getScene().getAxes().setMode(value);

        } else if (actionString.startsWith(ActionPrefix.setBatchHint)) {
            arg = MyString.remainder(actionString, ActionPrefix.setBatchHint);
            Spatial.BatchHint value = Spatial.BatchHint.valueOf(arg);
            target.setBatchHint(value);

        } else if (actionString.startsWith(ActionPrefix.setColorDepth)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.setColorDepth);
            int value = Integer.parseInt(arg);
            DisplaySettings.get().setBitsPerPixel(value);
            DisplaySettings.save();

        } else if (actionString.startsWith(ActionPrefix.setCullHint)) {
            arg = MyString.remainder(actionString, ActionPrefix.setCullHint);
            Spatial.CullHint value = Spatial.CullHint.valueOf(arg);
            target.setCullHint(value);

        } else if (actionString.startsWith(ActionPrefix.setDegrees)) {
            arg = MyString.remainder(actionString, ActionPrefix.setDegrees);
            boolean newSetting = Boolean.parseBoolean(arg);
            model.getMisc().setAnglesInDegrees(newSetting);

        } else if (actionString.startsWith(ActionPrefix.setDiagnose)) {
            arg = MyString.remainder(actionString, ActionPrefix.setDiagnose);
            boolean newSetting = Boolean.parseBoolean(arg);
            model.getMisc().setDiagnoseLoads(newSetting);

        } else if (actionString.startsWith(
                ActionPrefix.setDurationProportional)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.setDurationProportional);
            float value = Float.parseFloat(arg);
            target.getAnimation().setDurationProportional(value);

        } else if (actionString.startsWith(ActionPrefix.setDurationSame)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.setDurationSame);
            float value = Float.parseFloat(arg);
            target.getAnimation().setDurationSame(value);

        } else if (actionString.startsWith(ActionPrefix.setIndexBase)) {
            arg = MyString.remainder(actionString, ActionPrefix.setIndexBase);
            int newSetting = Integer.parseInt(arg);
            model.getMisc().setIndexBase(newSetting);

        } else if (actionString.startsWith(ActionPrefix.setPhysicsRbpValue)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.setPhysicsRbpValue);
            String[] args = arg.split(" ");
            RigidBodyParameter parm = RigidBodyParameter.valueOf(args[0]);
            if (args.length == 2) {
                float value = Float.parseFloat(args[1]);
                target.setRigidBodyParameter(parm, value);
            } else {
                handled = false;
            }

        } else if (actionString.startsWith(ActionPrefix.setQueueBucket)) {
            arg = MyString.remainder(actionString, ActionPrefix.setQueueBucket);
            RenderQueue.Bucket value = RenderQueue.Bucket.valueOf(arg);
            target.setQueueBucket(value);

        } else if (actionString.startsWith(ActionPrefix.setRefreshRate)) {
            arg = MyString.remainder(actionString, ActionPrefix.setRefreshRate);
            int value = Integer.parseInt(arg);
            DisplaySettings.get().setFrequency(value);
            DisplaySettings.save();

        } else if (actionString.startsWith(ActionPrefix.setResolution)) {
            arg = MyString.remainder(actionString, ActionPrefix.setResolution);
            String[] args = arg.split(" ");
            assert args.length == 3 : args.length;
            int width = Integer.parseInt(args[0]);
            assert "x".equals(args[1]) : args[1];
            int height = Integer.parseInt(args[2]);
            DisplaySettings.get().setWidth(width);
            DisplaySettings.get().setHeight(height);
            DisplaySettings.save();

        } else if (actionString.startsWith(ActionPrefix.setSceneBones)) {
            arg = MyString.remainder(actionString, ActionPrefix.setSceneBones);
            ShowBones value = ShowBones.valueOf(arg);
            model.getScene().getSkeleton().setShowBones(value);

        } else if (actionString.startsWith(ActionPrefix.setScoreBonesNone)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.setScoreBonesNone);
            ShowBones value = ShowBones.valueOf(arg);
            model.getScore().setShowNoneSelected(value);

        } else if (actionString.startsWith(ActionPrefix.setScoreBonesWhen)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.setScoreBonesWhen);
            ShowBones value = ShowBones.valueOf(arg);
            model.getScore().setShowWhenSelected(value);

        } else if (actionString.startsWith(ActionPrefix.setShadowMode)) {
            arg = MyString.remainder(actionString, ActionPrefix.setShadowMode);
            RenderQueue.ShadowMode value = RenderQueue.ShadowMode.valueOf(arg);
            target.setShadowMode(value);

        } else if (actionString.startsWith(ActionPrefix.setTweenRotations)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.setTweenRotations);
            TweenRotations value = TweenRotations.valueOf(arg);
            model.getTweenTransforms().setTweenRotations(value);

        } else if (actionString.startsWith(ActionPrefix.setTweenScales)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.setTweenScales);
            TweenVectors value = TweenVectors.valueOf(arg);
            model.getTweenTransforms().setTweenScales(value);

        } else if (actionString.startsWith(ActionPrefix.setTweenTranslations)) {
            arg = MyString.remainder(actionString,
                    ActionPrefix.setTweenTranslations);
            TweenVectors value = TweenVectors.valueOf(arg);
            model.getTweenTransforms().setTweenTranslations(value);

        } else if (actionString.startsWith(ActionPrefix.setUserData)) {
            arg = MyString.remainder(actionString, ActionPrefix.setUserData);
            target.setUserData(arg);

        } else if (actionString.startsWith(ActionPrefix.setViewMode)) {
            arg = MyString.remainder(actionString, ActionPrefix.setViewMode);
            ViewMode newSetting = ViewMode.valueOf(arg);
            model.getMisc().setViewMode(newSetting);

        } else {
            handled = false;
        }

        return handled;
    }
}
