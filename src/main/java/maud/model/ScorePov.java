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

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import java.util.logging.Logger;
import jme3utilities.Validate;
import maud.Maud;
import maud.ScoreView;

/**
 * The positions of a score camera and time cursor in Maud's edit screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class ScorePov implements Cloneable, Pov {
    // *************************************************************************
    // constants and loggers

    /**
     * rate to dolly in/out (percentage points per wheel notch)
     */
    final private static float dollyInOutRate = 15f;
    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            ScorePov.class.getName());
    /**
     * direction the score camera points (unit vector in world coordinates)
     */
    final private static Vector3f cameraDirection = new Vector3f(0f, 0f, -1f);
    // *************************************************************************
    // fields

    /**
     * half-height of the frustum (in world units)
     */
    private float halfHeight = 5f;
    /**
     * location of the camera (in world coordinates)
     */
    private Vector3f cameraLocation = new Vector3f(0.5f, -4f, 0f);
    /**
     * the location of the cursor (in world coordinates)
     */
    private Vector3f cursorLocation = new Vector3f(0f, 0f, 0f);
    /**
     * local copy of {@link com.jme3.math.Vector3f#UNIT_Y}
     */
    final private static Vector3f yAxis = new Vector3f(0f, 1f, 0f);
    // *************************************************************************
    // new methods exposed

    /**
     * Copy the location of the cursor.
     *
     * @param storeResult (modified if not null)
     * @return world coordinates (either storeResult or a new vector)
     */
    public Vector3f cursorLocation(Vector3f storeResult) {
        if (storeResult == null) {
            storeResult = new Vector3f();
        }
        storeResult.set(cursorLocation);

        return storeResult;
    }

    /**
     * Read the half-height for the frustum.
     *
     * @return half-height (in world units, &gt;0)
     */
    public float getHalfHeight() {
        return halfHeight;
    }

    /**
     * Alter the camera's Y-coordinate.
     *
     * @param yLocation new location for camera
     */
    public void setCameraY(float yLocation) {
        LoadedCgm cgm = Maud.gui.mouseCgm();
        ScoreView view = cgm.getScoreView();
        float viewHeight = view.getHeight();

        cameraLocation.y = FastMath.clamp(yLocation, -viewHeight, 0f);
    }

    /**
     * Alter the location of the cursor.
     *
     * @param timeFraction (&ge;0, &le;1)
     */
    public void setCursorLocation(float timeFraction) {
        Validate.fraction(timeFraction, "time fraction");

        // TODO
    }
    // *************************************************************************
    // Object methods

    /**
     * Create a deep copy of this object.
     *
     * @return a new object, equivalent to this one
     * @throws CloneNotSupportedException if superclass isn't cloneable
     */
    @Override
    public ScorePov clone() throws CloneNotSupportedException {
        ScorePov clone = (ScorePov) super.clone();
        clone.cameraLocation = cameraLocation.clone();
        clone.cursorLocation = cursorLocation.clone();

        return clone;
    }
    // *************************************************************************
    // Pov methods

    /**
     * Copy the location of the camera.
     *
     * @param storeResult (modified if not null)
     * @return world coordinates (either storeResult or a new vector)
     */
    @Override
    public Vector3f cameraLocation(Vector3f storeResult) {
        if (storeResult == null) {
            storeResult = new Vector3f();
        }
        storeResult.set(cameraLocation);

        return storeResult;
    }

    /**
     * Copy the orientation of the camera.
     *
     * @param storeResult (modified if not null)
     * @return rotation relative to world coordinates (either storeResult or a
     * new instance)
     */
    @Override
    public Quaternion cameraOrientation(Quaternion storeResult) {
        if (storeResult == null) {
            storeResult = new Quaternion();
        }
        storeResult.lookAt(cameraDirection, yAxis);

        return storeResult;
    }

    /**
     * Zoom the camera when the scroll wheel is turned.
     *
     * @param amount scroll wheel notches
     */
    @Override
    public void moveBackward(float amount) {
        float rate = 1f + dollyInOutRate / 100f;
        float factor = FastMath.pow(rate, amount);
        halfHeight *= factor;
    }

    /**
     * Move the camera left/right when the mouse is dragged from left/right.
     *
     * @param amount drag component
     */
    @Override
    public void moveLeft(float amount) {
        // Left/right movement is disabled.
    }

    /**
     * Move the camera up/down when the mouse is dragged up/down.
     *
     * @param amount drag component
     */
    @Override
    public void moveUp(float amount) {
        Maud app = Maud.getApplication();
        Camera camera = app.getCamera();
        float displayHeight = camera.getHeight();

        float yLocation = cameraLocation.y;
        yLocation += (2048f * halfHeight / displayHeight) * amount;
        setCameraY(yLocation);
    }
}
