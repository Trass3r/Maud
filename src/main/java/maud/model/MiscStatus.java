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
package maud.model;

import com.jme3.math.Vector3f;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Validate;
import jme3utilities.math.MyMath;
import maud.Maud;

/**
 * The MVC model of miscellaneous details in Maud's "3D View" screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class MiscStatus implements Cloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            MiscStatus.class.getName());
    // *************************************************************************
    // fields

    /**
     * angle display mode (true &rarr; degrees, false &rarr; radians)
     */
    private boolean anglesInDegrees = true;
    /**
     * CG model for axis dragging (true &rarr; source, false &rarr; target)
     */
    private boolean dragSourceCgm;
    /**
     * which direction the drag axis is pointing (true &rarr; away from camera,
     * false &rarr; toward camera)
     */
    private boolean dragFarSideFlag;
    /**
     * shadows (true &rarr; rendered, false &rarr; not rendered)
     */
    private boolean shadowsRendered = true;
    /**
     * sky background (true &rarr; rendered, false &rarr; not rendered)
     */
    private boolean skyRendered = true;
    /**
     * diameter of the platform (in world units, &gt;0)
     */
    private float platformDiameter = 1f;
    /**
     * index of the axis being dragged (&ge;0, &lt;3) or -1 for no axis
     */
    private int dragAxisIndex = -1;
    /**
     * platform mode (either "none" or "square")
     */
    private String platformMode = "square";
    /**
     * selected user key, or null if none selected
     */
    private String selectedUserKey = null;
    /**
     * center location of the top of the platform (in world coordinates, not
     * null)
     */
    private Vector3f platformLocation = new Vector3f();
    // *************************************************************************
    // new methods exposed

    /**
     * Test whether shadows are rendered.
     *
     * @return true if rendered, otherwise false
     */
    public boolean areShadowsRendered() {
        return shadowsRendered;
    }

    /**
     * Deselect axis dragging.
     */
    public void clearDragAxis() {
        dragAxisIndex = -1;
        assert !isDraggingAxis();
    }

    /**
     * Delete (and deselect) the selected user key.
     */
    public void deleteUserKey() {
        if (selectedUserKey != null) {
            Maud.model.target.deleteUserKey();
            selectedUserKey = null;
        }
    }

    /**
     * Find the index of the selected user key.
     *
     * @return index, or -1 if none selected
     */
    public int findUserKeyIndex() {
        int index;
        if (selectedUserKey == null) {
            index = -1;
        } else {
            List<String> keyList = Maud.model.target.spatial.listUserKeys();
            index = keyList.indexOf(selectedUserKey);
        }

        return index;
    }

    /**
     * Copy the center location of the top of the platform.
     *
     * @return a new vector (in world coordinates)
     */
    public Vector3f copyPlatformLocation() {
        Vector3f result = platformLocation.clone();
        return result;
    }

    /**
     * Test whether to display angles in degrees.
     *
     * @return true for degrees, otherwise false
     */
    public boolean getAnglesInDegrees() {
        return anglesInDegrees;
    }

    /**
     * Read the index of the axis being dragged.
     *
     * @return axis index (&ge;0, &lt;3)
     */
    public int getDragAxis() {
        assert isDraggingAxis();
        assert dragAxisIndex >= 0 : dragAxisIndex;
        assert dragAxisIndex < 3 : dragAxisIndex;
        return dragAxisIndex;
    }

    /**
     * Access the CG model whose axes are being dragged.
     *
     * @return the pre-existing instance
     */
    public LoadedCGModel getDragCgm() {
        assert isDraggingAxis();
        if (dragSourceCgm) {
            return Maud.model.source;
        } else {
            return Maud.model.target;
        }
    }

    /**
     * Read the diameter of the platform.
     *
     * @return diameter (in world units, &gt;0)
     */
    public float getPlatformDiameter() {
        assert platformDiameter > 0f : platformDiameter;
        return platformDiameter;
    }

    /**
     * Read the platform mode.
     *
     * @return either "none" or "square"
     */
    public String getPlatformMode() {
        return platformMode;
    }

    /**
     * Read the selected user key.
     *
     * @return a key, or null if none selected
     */
    public String getSelectedUserKey() {
        return selectedUserKey;
    }

    /**
     * Test whether an axis is selected for dragging.
     *
     * @return true if selected, otherwise false
     */
    public boolean isDraggingAxis() {
        if (dragAxisIndex == -1) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Test whether the axis being dragged points away from the camera.
     *
     * @return true if pointing away from camera, otherwise false
     */
    public boolean isDraggingFarSide() {
        assert isDraggingAxis();
        return dragFarSideFlag;
    }

    /**
     * Test whether the sky background is being rendered.
     *
     * @return true if rendered, otherwise false
     */
    public boolean isSkyRendered() {
        return skyRendered;
    }

    /**
     * Select the next user key in alphabetical order.
     */
    public void selectNextUserKey() {
        List<String> keyList = Maud.model.target.spatial.listUserKeys();
        int numKeys = keyList.size();
        int index = keyList.indexOf(selectedUserKey);
        int nextIndex = MyMath.modulo(index + 1, numKeys);
        String nextName = keyList.get(nextIndex);
        selectUserKey(nextName);
    }

    /**
     * Select the previous user key in alphabetical order.
     */
    public void selectPreviousUserKey() {
        List<String> keyList = Maud.model.target.spatial.listUserKeys();
        int numKeys = keyList.size();
        int index = keyList.indexOf(selectedUserKey);
        int nextIndex = MyMath.modulo(index - 1, numKeys);
        String previousName = keyList.get(nextIndex);
        selectUserKey(previousName);
    }

    /**
     * Select the specified user key.
     *
     * @param key a key, or null to deselect
     */
    public void selectUserKey(String key) {
        selectedUserKey = key;
    }

    /**
     * Alter the angle display mode.
     *
     * @param newState true &rarr; degrees, false &rarr; radians
     */
    public void setAnglesInDegrees(boolean newState) {
        anglesInDegrees = newState;
    }

    /**
     * Start dragging the specified axis.
     *
     * @param axisIndex which axis to drag (&ge;0, &lt;3)
     * @param cgm which CG model (not null)
     * @param farSideFlag
     */
    public void setDraggingAxis(int axisIndex, LoadedCGModel cgm,
            boolean farSideFlag) {
        Validate.inRange(axisIndex, "axis index", 0, 2);
        Validate.nonNull(cgm, "model");

        dragAxisIndex = axisIndex;
        dragFarSideFlag = farSideFlag;
        if (cgm == Maud.model.source) {
            dragSourceCgm = true;
        } else {
            dragSourceCgm = false;
        }
        assert isDraggingAxis();
    }

    /**
     * Alter the diameter of the platform.
     *
     * @param diameter (in world units, &gt;0)
     */
    public void setPlatformDiameter(float diameter) {
        Validate.positive(diameter, "diameter");
        platformDiameter = diameter;
    }

    /**
     * Alter the center location of the top of the platform.
     *
     * @param newLocation world coordinates (not null, unaffected)
     */
    public void setPlatformLocation(Vector3f newLocation) {
        Validate.nonNull(newLocation, "location");
        platformLocation.set(newLocation);
    }

    /**
     * Alter the platform display mode.
     *
     * @param modeName either "none" or "square"
     */
    public void setPlatformMode(String modeName) {
        Validate.nonNull(modeName, "mode name");

        switch (modeName) {
            case "none":
            case "square":
                platformMode = modeName;
                break;
            default:
                logger.log(Level.SEVERE, "mode name={0}", modeName);
                throw new IllegalArgumentException("invalid mode name");
        }
    }

    /**
     * Alter the rendering of shadows.
     *
     * @param newState true &rarr; rendered, false &rarr; not rendered
     */
    public void setShadowsRendered(boolean newState) {
        shadowsRendered = newState;
    }

    /**
     * Alter the rendering of the sky background.
     *
     * @param newState true &rarr; rendered, false &rarr; not rendered
     */
    public void setSkyRendered(boolean newState) {
        skyRendered = newState;
    }

    /**
     * Toggle the angle display mode.
     */
    public void toggleAnglesInDegrees() {
        setAnglesInDegrees(!anglesInDegrees);
    }
    // *************************************************************************
    // Object methods

    /**
     * Create a copy of this object.
     *
     * @return a new object, equivalent to this one
     * @throws CloneNotSupportedException if superclass isn't cloneable
     */
    @Override
    public MiscStatus clone() throws CloneNotSupportedException {
        MiscStatus clone = (MiscStatus) super.clone();
        clone.platformLocation = platformLocation.clone();
        return clone;
    }
}
