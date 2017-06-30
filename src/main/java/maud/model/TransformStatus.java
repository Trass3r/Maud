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
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import java.util.logging.Logger;
import jme3utilities.Validate;
import jme3utilities.math.MyMath;
import maud.Maud;

/**
 * The transform applied to a particular CG model visualization in Maud's "3D
 * View" screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class TransformStatus implements Cloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * separation between the source and target CG models, when both are loaded
     * (in world units)
     */
    final private static float zSeparation = 1f;
    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            TransformStatus.class.getName());
    /**
     * local copy of {@link com.jme3.math.Vector3f#UNIT_Y}
     */
    final private static Vector3f yAxis = new Vector3f(0f, 1f, 0f);
    // *************************************************************************
    // fields

    /**
     * scale of the CG model on all 3 axes (&gt;0, 1 &rarr; unscaled)
     */
    private float scale = 1f;
    /**
     * rotation angle of the CG model around the +Y axis (in radians)
     */
    private float yAngle = 0f;
    /**
     * Y-offset of the CG model's base (in world units)
     */
    private float yOffset = 0f;
    /**
     * the CG model this transform applies to (set by
     * {@link #setCgm(LoadedCGModel)})
     */
    private LoadedCGModel loadedCgm = null;
    /**
     * the location (in model space) of the CG model's dominant root bone
     */
    private Vector3f bindLocation = new Vector3f();
    // *************************************************************************
    // new methods exposed

    /**
     * Automatically configure the transform for a newly loaded CG model.
     *
     * @param bindLoc the location of the CG model's dominant root bone (in
     * model space, not null, unaffected)
     * @param minY Y-offset of the CG model's base (in CG-model units)
     * @param maxExtent the greatest extent of the CG model over its 3 principal
     * axes in bind pose (in model units, &gt;0)
     */
    public void loadCgm(Vector3f bindLoc, float minY, float maxExtent) {
        Validate.nonNull(bindLoc, "bind location");
        Validate.positive(maxExtent, "max extent");

        bindLocation.set(bindLoc);
        scale = 1f / maxExtent;
        yOffset = -minY * scale;
    }

    /**
     * Add to the Y-axis rotation angle of the CG model.
     *
     * @param angle (in radians)
     */
    public void rotateY(float angle) {
        yAngle = MyMath.modulo(yAngle + angle, FastMath.TWO_PI);
    }

    /**
     * Alter which CG model this transform applies to. (Invoked only during
     * initialization and cloning.)
     *
     * @param newLoaded (not null)
     */
    void setCgm(LoadedCGModel newLoaded) {
        assert newLoaded != null;
        loadedCgm = newLoaded;
    }

    /**
     * Calculate the transform to be applied to the CG model. Note that this may
     * differ from the transform of the CG model's root node.
     *
     * @return a new instance (in world coordinates)
     */
    public Transform worldTransform() {
        Transform result = new Transform();
        result.getRotation().fromAngleNormalAxis(yAngle, yAxis);
        result.getScale().set(scale, scale, scale);
        /*
         * Offset the CGM so that (in bind pose) the root bone is
         * directly above the origin (center of platform).
         */
        Vector3f modelTranslation;
        modelTranslation = new Vector3f(-bindLocation.x, 0f, -bindLocation.z);
        Vector3f worldTranslation;
        worldTranslation = result.transformVector(modelTranslation, null);
        /*
         * Displace along the Z-axis if 2 CGMs are loaded.
         */
        float zOffset;
        if (loadedCgm == Maud.model.target) {
            if (Maud.model.source.isLoaded()) {
                zOffset = 0.5f * zSeparation;
            } else {
                zOffset = 0f;
            }
        } else {
            zOffset = -0.5f * zSeparation;
        }
        worldTranslation.addLocal(0f, yOffset, zOffset);
        result.setTranslation(worldTranslation);

        return result;
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
    public TransformStatus clone() throws CloneNotSupportedException {
        TransformStatus clone = (TransformStatus) super.clone();
        clone.bindLocation = bindLocation.clone();
        return clone;
    }
}
