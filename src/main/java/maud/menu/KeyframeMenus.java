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
package maud.menu;

import java.util.logging.Logger;
import jme3utilities.MyString;
import maud.Maud;
import maud.dialog.EditorDialogs;
import maud.model.cgm.Cgm;
import maud.model.cgm.EditableCgm;

/**
 * Keyframe menus in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class KeyframeMenus {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(KeyframeMenus.class.getName());
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private KeyframeMenus() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Handle a "select menuItem" action from the Keyframe menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    static boolean menuKeyframe(String remainder) {
        boolean handled = true;
        String selectPrefix = "Select" + EditorMenus.menuPathSeparator;
        if (remainder.startsWith(selectPrefix)) {
            String arg = MyString.remainder(remainder, selectPrefix);
            handled = menuKeyframeSelect(arg);

        } else {
            EditableCgm target = Maud.getModel().getTarget();
            switch (remainder) {
                case "Delete next":
                    EditorDialogs.deleteNextKeyframes();
                    break;

                case "Delete previous":
                    EditorDialogs.deletePreviousKeyframes();
                    break;

                case "Delete selected":
                    target.getTrack().deleteSelectedKeyframe();
                    break;

                case "Insert from pose":
                    target.getTrack().insertKeyframe();
                    break;

                case "Replace with pose":
                    target.getTrack().replaceKeyframe();
                    break;

                case "Select":
                    ShowMenus.selectKeyframe();
                    break;

                case "Tool":
                    Maud.gui.tools.select("keyframe");
                    break;

                default:
                    handled = false;
            }
        }

        return handled;
    }
    // *************************************************************************
    // private methods

    /**
     * Handle a "select menuItem" action from the "Keyframe -> Select" menu.
     *
     * @param remainder not-yet-parsed portion of the menu path (not null)
     * @return true if the action is handled, otherwise false
     */
    private static boolean menuKeyframeSelect(String remainder) {
        boolean handled = true;
        Cgm target = Maud.getModel().getTarget();
        switch (remainder) {
            case "First":
                target.getTrack().selectFirstKeyframe();
                break;

            case "Last":
                target.getTrack().selectLastKeyframe();
                break;

            case "Nearest":
                target.getTrack().selectNearestKeyframe();
                break;

            case "Next":
                target.getTrack().selectNextKeyframe();
                break;

            case "Previous":
                target.getTrack().selectPreviousKeyframe();
                break;

            default:
                handled = false;
        }

        return handled;
    }
}
