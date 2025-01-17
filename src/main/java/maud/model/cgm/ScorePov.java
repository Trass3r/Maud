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
package maud.model.cgm;

import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import java.util.logging.Logger;
import jme3utilities.MyCamera;
import jme3utilities.Validate;
import maud.Maud;
import maud.view.ScoreView;

/**
 * The position of a score-mode camera in Maud's edit screen.
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
     * 1/2 the width of the camera's frustum (in world units)
     */
    final private static float halfWidth = 0.7f;
    /**
     * maximum half height of the camera's frustum (in world units)
     */
    final private static float maxHalfHeight = 100f;
    /**
     * minimum half height of the camera's frustum (in world units)
     */
    final private static float minHalfHeight = 0.1f;
    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(ScorePov.class.getName());
    // *************************************************************************
    // fields

    /**
     * C-G model using this POV (set by {@link #setCgm(Cgm)})
     */
    private Cgm cgm = null;
    /**
     * 1/2 the height of the camera's frustum (in world units, &gt;0)
     */
    private float halfHeight = 2f;
    /**
     * range of world Y coordinates occupied by the most recently selected bone
     */
    private Vector2f oldMinMaxY = new Vector2f();
    /**
     * location of the camera (in world coordinates)
     */
    private Vector3f cameraLocation = new Vector3f(0.45f, -4f, 0f);
    // *************************************************************************
    // new methods exposed

    /**
     * Calculate how much the camera magnifies the Y axis relative to the X
     * axis.
     *
     * @return compression factor (&gt;0)
     */
    public float compression() {
        ScoreView view = cgm.getScoreView();
        Camera camera = view.getCamera();
        float far = MyCamera.frustumAspectRatio(camera);
        float var = MyCamera.viewAspectRatio(camera);
        float factor = far / var;

        assert factor > 0f : factor;
        return factor;
    }

    /**
     * Read the camera's vertical location.
     *
     * @return Y coordinate of camera (in world space)
     */
    public float getCameraY() {
        return cameraLocation.y;
    }

    /**
     * Read the half height for the camera's frustum.
     *
     * @return half-height (in world units, &gt;0)
     */
    public float getHalfHeight() {
        assert halfHeight > 0f : halfHeight;
        return halfHeight;
    }

    /**
     * Calculate the world X-coordinate at the left edge of the camera's
     * frustum.
     *
     * @return coordinate value
     */
    public float leftX() {
        ScoreView view = cgm.getScoreView();
        Camera camera = view.getCamera();
        float left = camera.getFrustumLeft();
        assert left < 0f : left;
        float center = camera.getLocation().x;
        float result = center + left;

        return result;
    }

    /**
     * Calculate the world X-coordinate at the right edge of the camera's
     * frustum.
     *
     * @return coordinate value
     */
    public float rightX() {
        ScoreView view = cgm.getScoreView();
        Camera camera = view.getCamera();
        float right = camera.getFrustumRight();
        assert right > 0f : right;
        float center = camera.getLocation().x;
        float result = center + right;

        return result;
    }

    /**
     * Alter the camera's vertical location.
     *
     * @param yLocation new Y coordinate (in world space)
     */
    public void setCameraY(float yLocation) {
        ScoreView view = cgm.getScoreView();
        float maxY = 0f;
        float minY = -view.getHeight();
        cameraLocation.y = FastMath.clamp(yLocation, minY, maxY);
    }

    /**
     * Alter the half height for the frustum.
     *
     * @param newValue new half height for frustum (in world units, &gt;0)
     */
    public void setHalfHeight(float newValue) {
        Validate.positive(newValue, "new value");
        halfHeight = FastMath.clamp(newValue, minHalfHeight, maxHalfHeight);
    }

    /**
     * Partially update the camera for this POV.
     */
    public void updatePartial() {
        ScoreView view = cgm.getScoreView();
        Camera camera = view.getCamera();
        camera.setLocation(cameraLocation);
        camera.setFrustumBottom(-halfHeight);
        camera.setFrustumLeft(-halfWidth);
        camera.setFrustumRight(halfWidth);
        camera.setFrustumTop(halfHeight);
    }
    // *************************************************************************
    // Object methods

    /**
     * Create a deep copy of this object.
     *
     * @return a new object, equivalent to this one
     * @throws CloneNotSupportedException if the superclass isn't cloneable
     */
    @Override
    public ScorePov clone() throws CloneNotSupportedException {
        ScorePov clone = (ScorePov) super.clone();
        clone.cameraLocation = cameraLocation.clone();
        clone.oldMinMaxY = oldMinMaxY.clone();

        return clone;
    }
    // *************************************************************************
    // Pov methods

    /**
     * Zoom this POV when the scroll wheel is turned.
     *
     * @param amount scroll-wheel notches (non-zero)
     */
    @Override
    public void moveBackward(float amount) {
        Validate.nonZero(amount, "amount");

        float rate = 1f + dollyInOutRate / 100f;
        float factor = FastMath.pow(rate, amount);
        setHalfHeight(halfHeight * factor);
    }

    /**
     * Move this POV left/right when the mouse is dragged left/right.
     *
     * @param amount drag component (non-zero)
     */
    @Override
    public void moveLeft(float amount) {
        Validate.nonZero(amount, "amount");
        // Left/right movement is disabled in score mode.
    }

    /**
     * Move this POV up/down when the mouse is dragged up/down.
     *
     * @param amount drag component (non-zero)
     */
    @Override
    public void moveUp(float amount) {
        Validate.nonZero(amount, "amount");

        Maud app = Maud.getApplication();
        Camera camera = app.getCamera();
        float displayHeight = camera.getHeight();

        float yLocation = cameraLocation.y;
        yLocation += (2048f * halfHeight / displayHeight) * amount;
        setCameraY(yLocation);
    }

    /**
     * Alter which C-G model uses this POV. (Invoked only during initialization
     * and cloning.)
     *
     * @param newCgm (not null, alias created)
     */
    @Override
    public void setCgm(Cgm newCgm) {
        assert newCgm != null;
        assert newCgm.getScorePov() == this;

        cgm = newCgm;
    }

    /**
     * Update this POV and its camera.
     *
     * @param ignored not used
     */
    @Override
    public void update(float ignored) {
        ScoreView view = cgm.getScoreView();
        Camera camera = view.getCamera();
        if (camera != null) {
            float hh = getHalfHeight();
            float y = getCameraY();
            Vector2f minMaxY = view.selectedMinMaxY();
            if (minMaxY == null) {
                oldMinMaxY.zero();
            } else if (!minMaxY.equals(oldMinMaxY)) {
                y = (minMaxY.x + minMaxY.y) / 2;
                hh = 0.6f + 0.6f * Math.abs(minMaxY.y - y);
                oldMinMaxY.set(minMaxY);
            }
            setHalfHeight(hh);
            setCameraY(y);

            updatePartial();
        }
    }
}
