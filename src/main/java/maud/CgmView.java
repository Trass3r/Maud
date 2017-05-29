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

import com.jme3.animation.AnimControl;
import com.jme3.animation.Bone;
import com.jme3.animation.Skeleton;
import com.jme3.animation.SkeletonControl;
import com.jme3.asset.AssetManager;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.util.logging.Logger;
import jme3utilities.MySpatial;
import jme3utilities.Validate;
import jme3utilities.debug.SkeletonDebugControl;
import jme3utilities.math.MyMath;
import maud.model.LoadedCGModel;

/**
 * A visualization of a loaded CG model.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class CgmView {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            CgmView.class.getName());
    // *************************************************************************
    // fields

    /**
     * the MVC model for the CG model
     */
    final private LoadedCGModel model;
    /**
     * the parent node of the CG model, used for rotation
     */
    final private Node parentNode;
    /**
     * the skeleton of this view's copy of the CG model
     */
    private Skeleton skeleton = null;
    /**
     * the skeleton control in this view's copy of the CG model
     */
    private SkeletonControl skeletonControl = null;
    /**
     * the skeleton debug control in this view's copy of the CG model
     */
    private SkeletonDebugControl skeletonDebugControl = null;
    /**
     * the root spatial in this view's copy of the CG model
     */
    private Spatial cgModelRoot = null;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a new view.
     *
     * @param m the MVC model for the CG model (not null)
     * @param parent parent node in the scene graph (not null)
     */
    CgmView(LoadedCGModel m, Node parent) {
        assert m != null;
        assert parent != null;

        parentNode = parent;
        model = m;
        model.setView(this);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Calculate the location of an indexed bone, for selection.
     *
     * @param boneIndex which bone to locate
     * @return a new vector (in world coordinates)
     */
    Vector3f boneLocation(int boneIndex) {
        Bone bone = skeleton.getBone(boneIndex);
        Vector3f modelLocation = bone.getModelSpacePosition();
        Geometry ag = findAnimatedGeometry();
        Vector3f location = ag.localToWorld(modelLocation, null);

        return location;
    }

    /**
     * Create a duplicate copy of this view, for checkpointing.
     *
     * @param m the MVC model for the CG model (not null)
     * @return a new instance
     */
    public CgmView createCopy(LoadedCGModel m) {
        assert m != null;

        CgmView result = new CgmView(m, parentNode);
        result.setCgmRoot(cgModelRoot);

        return result;
    }

    /**
     * Find an animated geometry of the CG model.
     *
     * @return a pre-existing instance, or null if none
     */
    Geometry findAnimatedGeometry() {
        Geometry result = MySpatial.findAnimatedGeometry(cgModelRoot);
        return result;
    }

    /**
     * Access the skeleton debug control for the CG model.
     *
     * @return the pre-existing instance, or null if none
     */
    SkeletonDebugControl getSkeletonDebugControl() {
        return skeletonDebugControl;
    }

    /**
     * Access the root spatial of the CG model. TODO reorder
     *
     * @return the pre-existing instance (not null)
     */
    Spatial getCgmRoot() {
        assert cgModelRoot != null;
        return cgModelRoot;
    }

    /**
     * Replace the CG model with a newly loaded one.
     *
     * @param loadedRoot (not null)
     */
    public void loadModel(Spatial loadedRoot) {
        /*
         * Detach the old spatial (if any) from the scene.
         */
        if (cgModelRoot != null) {
            parentNode.detachChild(cgModelRoot);
        }
        cgModelRoot = loadedRoot;

        prepareForEditing();
    }

    /**
     * Copy a saved state to this one.
     *
     * @param savedState (not null)
     */
    void restore(CgmView savedState) {
        assert cgModelRoot != null;
        /*
         * Detach the old spatial from the scene graph.
         */
        parentNode.detachChild(cgModelRoot);

        Spatial sp = savedState.getCgmRoot();
        cgModelRoot = sp.clone();
        parentNode.attachChild(cgModelRoot);

        skeletonControl = cgModelRoot.getControl(SkeletonControl.class);
        if (skeletonControl == null) {
            skeleton = null;
        } else {
            skeleton = skeletonControl.getSkeleton();
        }

        skeletonDebugControl = cgModelRoot.getControl(
                SkeletonDebugControl.class);
        assert skeletonDebugControl != null;
    }

    /**
     * Alter the CG model to visualize.
     *
     * @param newRoot root spatial (not null, unaffected)
     */
    void setCgmRoot(Spatial newRoot) {
        cgModelRoot = newRoot.clone();
    }

    /**
     * Alter the cull hint of the selected spatial.
     *
     * @param newHint new value for cull hint (not null)
     */
    public void setHint(Spatial.CullHint newHint) {
        Validate.nonNull(newHint, "cull hint");

        Spatial spatial = model.spatial.findSpatial(cgModelRoot);
        spatial.setCullHint(newHint);
    }

    /**
     * Alter the shadow mode of the selected spatial.
     *
     * @param newMode new value for shadow mode (not null)
     */
    public void setMode(RenderQueue.ShadowMode newMode) {
        Validate.nonNull(newMode, "shadow mode");

        Spatial spatial = model.spatial.findSpatial(cgModelRoot);
        spatial.setShadowMode(newMode);
    }

    /**
     * Alter the skeleton of the loaded CG model.
     *
     * @param newSkeleton (may be null, unaffected)
     */
    public void setSkeleton(Skeleton newSkeleton) {
        if (skeletonControl != null) {
            cgModelRoot.removeControl(skeletonControl);
        }

        if (newSkeleton == null) {
            skeleton = null;
            skeletonControl = null;
        } else {
            skeleton = new Skeleton(newSkeleton);
            skeletonControl = new SkeletonControl(skeleton);
            cgModelRoot.addControl(skeletonControl);
            Util.setUserControl(skeleton, true);
            skeletonControl.setHardwareSkinningPreferred(false);
        }

        skeletonDebugControl.setSkeleton(skeleton);
    }

    /**
     * Alter the local rotation of the selected spatial.
     *
     * @param rotation (not null, unaffected)
     */
    public void setSpatialRotation(Quaternion rotation) {
        Validate.nonNull(rotation, "rotation");

        Spatial spatial = model.spatial.findSpatial(cgModelRoot);
        spatial.setLocalRotation(rotation);
    }

    /**
     * Alter the local scale of the selected spatial.
     *
     * @param scale (not null, unaffected)
     */
    public void setSpatialScale(Vector3f scale) {
        Validate.nonNull(scale, "scale");

        Spatial spatial = model.spatial.findSpatial(cgModelRoot);
        spatial.setLocalScale(scale);
    }

    /**
     * Alter the local translation of the selected spatial.
     *
     * @param translation (not null, unaffected)
     */
    public void setSpatialTranslation(Vector3f translation) {
        Validate.nonNull(translation, "translation");

        Spatial spatial = model.spatial.findSpatial(cgModelRoot);
        spatial.setLocalTranslation(translation);
    }

    /**
     * Update the user transforms of all bones using the MVC model.
     */
    void updatePose() {
        int boneCount = model.bones.countBones();
        int numTransforms = model.pose.countTransforms();
        assert numTransforms == boneCount : numTransforms;

        Transform transform = new Transform();
        Vector3f translation = new Vector3f();
        Quaternion rotation = new Quaternion();
        Vector3f scale = new Vector3f();

        for (int boneIndex = 0; boneIndex < boneCount; boneIndex++) {
            model.pose.copyTransform(boneIndex, transform);
            transform.getTranslation(translation);
            transform.getRotation(rotation);
            transform.getScale(scale);

            Bone bone = skeleton.getBone(boneIndex);
            bone.setUserTransforms(translation, rotation, scale);
        }
    }

    /**
     * Update the transform of the CG model.
     *
     * @param angle in radians
     */
    void updateTransform() {
        Transform transform = model.transform.worldTransform();
        parentNode.setLocalTransform(transform);
    }
    // *************************************************************************
    // private methods

    /**
     * Alter a newly loaded CG model to prepare it for viewing and editing.
     */
    private void prepareForEditing() {
        /*
         * Attach the CG model to the scene graph.
         */
        parentNode.attachChild(cgModelRoot);

        skeletonControl = cgModelRoot.getControl(SkeletonControl.class);
        /*
         * Update reference to skeleton.
         */
        AnimControl animControl = cgModelRoot.getControl(AnimControl.class);
        if (animControl == null) {
            skeleton = null;
        } else {
            skeleton = animControl.getSkeleton();
        }
        if (skeleton == null) {
            SkeletonControl c = cgModelRoot.getControl(SkeletonControl.class);
            if (c != null) {
                skeleton = c.getSkeleton();
            }
        }
        if (skeleton != null) {
            /*
             * Enable user control for all bones in the skeleton.
             */
            Util.setUserControl(skeleton, true);
            /*
             * Disable hardware skinning so that the raycast in
             * CursorTool.findContact() will work.
             */
            skeletonControl.setHardwareSkinningPreferred(false);
        }
        /*
         * Add a new SkeletonDebugControl.
         */
        Maud application = Maud.getApplication();
        AssetManager assetManager = application.getAssetManager();
        skeletonDebugControl = new SkeletonDebugControl(assetManager);
        cgModelRoot.addControl(skeletonDebugControl);
        skeletonDebugControl.setEnabled(true);
        skeletonDebugControl.setSkeleton(skeleton);
        /*
         * Configure the CG model transform based on the range
         * of mesh coordinates in the CG model.
         */
        Vector3f[] minMax = MySpatial.findMinMaxCoords(cgModelRoot, false);
        Vector3f extents = minMax[1].subtract(minMax[0]);
        float maxExtent = MyMath.max(extents.x, extents.y, extents.z);
        assert maxExtent > 0f : maxExtent;
        float minY = minMax[0].y;
        model.transform.loadModel(minY, maxExtent);
        /*
         * reset the camera, cursor, and platform
         */
        Vector3f baseLocation = new Vector3f(0f, 0f, 0f);
        Maud.model.cursor.setLocation(baseLocation);
        Maud.model.misc.setPlatformDiameter(2f);
        Maud.model.misc.setPlatformLocation(baseLocation);

        Vector3f cameraLocation = new Vector3f(-2.4f, 1f, 1.6f);
        Maud.model.camera.setLocation(cameraLocation);
        Maud.model.camera.setScale(1f);
    }
}
