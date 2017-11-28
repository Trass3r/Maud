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
package maud.model.cgm;

import com.jme3.animation.AnimControl;
import com.jme3.animation.Animation;
import com.jme3.animation.Bone;
import com.jme3.animation.BoneTrack;
import com.jme3.animation.Skeleton;
import com.jme3.animation.SkeletonControl;
import com.jme3.animation.Track;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.control.PhysicsControl;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.UserData;
import com.jme3.scene.control.Control;
import com.jme3.scene.plugins.bvh.SkeletonMapping;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyControl;
import jme3utilities.MySkeleton;
import jme3utilities.MySpatial;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.ui.ActionApplication;
import jme3utilities.wes.TrackEdit;
import jme3utilities.wes.TweenTransforms;
import maud.Maud;
import maud.MaudUtil;
import maud.PhysicsUtil;
import maud.model.History;
import maud.model.option.RigidBodyParameter;
import maud.model.option.ShapeParameter;
import maud.view.SceneView;

/**
 * MVC model for an editable computer-graphics (C-G) model in the Maud
 * application: keeps track of edits made to the loaded C-G model.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class EditableCgm extends LoadedCgm {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(EditableCgm.class.getName());
    // *************************************************************************
    // fields

    /**
     * count of unsaved edits (&ge;0)
     */
    private int editCount = 0;
    /**
     * model state that's being continuously edited, either:
     * <ul>
     * <li> tree position of the spatial whose transform is being edited, or
     * <li> name of the physics object whose position is being edited, or
     * <li> "" for none of the above
     * </ul>
     */
    private String continousEditState = "";
    // *************************************************************************
    // new methods exposed

    /**
     * Add a new animation to the selected anim control.
     *
     * @param newAnimation (not null, name not in use)
     */
    void addAnimation(Animation newAnimation) {
        assert newAnimation != null;
        SelectedAnimControl sac = getAnimControl();
        String newAnimationName = newAnimation.getName();
        assert !sac.hasRealAnimation(newAnimationName);

        History.autoAdd();
        AnimControl control = sac.find();
        if (control == null) {
            SelectedSkeleton ss = getSkeleton();
            Skeleton skeleton = ss.find();
            assert skeleton != null;
            control = new AnimControl(skeleton);

            Spatial skeletonSpatial = ss.findSpatial();
            skeletonSpatial.addControl(control);
        }
        control.addAnim(newAnimation);
        String description;
        description = "add animation " + MyString.quote(newAnimationName);
        setEdited(description);
    }

    /**
     * Add an attachments node for the selected bone.
     */
    public void addAttachmentsNode() {
        SelectedBone selectedBone = getBone();
        assert !selectedBone.hasAttachmentsNode();

        History.autoAdd();
        Node newNode = selectedBone.createAttachments();

        Node parent = newNode.getParent();
        List<Integer> parentPosition = findSpatial(parent);
        getSceneView().attachSpatial(parentPosition, newNode);

        String boneName = selectedBone.getName();
        String description = "add attachments node for "
                + MyString.quote(boneName);
        setEdited(description);

        assert selectedBone.hasAttachmentsNode();
    }

    /**
     * Add a new S-G control to the selected spatial.
     *
     * @param newSgc (not null)
     */
    void addSgc(Control newSgc) {
        assert newSgc != null;

        History.autoAdd();
        Spatial selectedSpatial = getSpatial().find();
        if (newSgc instanceof PhysicsControl) {
            PhysicsControl physicsControl = (PhysicsControl) newSgc;
            SceneView sceneView = getSceneView();
            sceneView.addPhysicsControl(physicsControl);
        }
        selectedSpatial.addControl(newSgc);
        setEdited("add control");
    }

    /**
     * Add a track to the loaded animation.
     *
     * @param newTrack (not null, alias created)
     * @param eventDescription description of causative event (not null)
     */
    void addTrack(Track newTrack, String eventDescription) {
        assert newTrack != null;
        assert eventDescription != null;

        Animation animation = getAnimation().getAnimation();

        History.autoAdd();
        animation.addTrack(newTrack);
        setEdited(eventDescription);
    }

    /**
     * Add a user key to the selected spatial.
     *
     * @param type name of the data type ("boolean", "float", "integer", "long",
     * or "string")
     * @param key user key to create (not null)
     */
    public void addUserKey(String type, String key) {
        Validate.nonNull(key, "key");

        Object object = null;
        switch (type) {
            case "boolean":
                object = false;
                break;
            case "float":
                object = 0f;
                break;
            case "integer":
                object = 0;
                break;
            case "long":
                object = 0L;
                break;
            case "string":
                object = "";
                break;
            default:
                assert false;
        }
        byte objectType = UserData.getObjectType(object);
        UserData data = new UserData(objectType, object);
        Spatial selectedSpatial = getSpatial().find();

        History.autoAdd();
        selectedSpatial.setUserData(key, data);
        String description;
        description = String.format("add user key %s", MyString.quote(key));
        setEdited(description);
        getUserData().selectKey(key);
    }

    /**
     * Attach a child subtree to the specified parent node.
     *
     * @param parent (not null)
     * @param child (not null)
     * @param eventDescription description of causative event (not null)
     */
    void attachSpatial(Node parent, Spatial child, String eventDescription) {
        assert parent != null;
        assert child != null;
        assert eventDescription != null;

        SceneView sceneView = getSceneView();
        List<Integer> parentPosition = findSpatial(parent);

        History.autoAdd();
        sceneView.attachSpatial(parentPosition, child);
        parent.attachChild(child);
        setEdited(eventDescription);
    }

    /**
     * Determine the default base path for writing the C-G model to the
     * filesystem.
     *
     * @return absolute filesystem path less extension (not null, not empty)
     */
    public String baseFilePathForWrite() {
        String folder = assetFolderForWrite();
        String assetPath = getAssetPath();
        if (assetPath.isEmpty()) {
            assetPath = "Models/Untitled/Untitled";
        }
        File file = new File(folder, assetPath);
        String result = file.getAbsolutePath();
        result = result.replaceAll("\\\\", "/");

        return result;
    }

    /**
     * Count unsaved edits.
     *
     * @return count (&ge;0)
     */
    public int countUnsavedEdits() {
        return editCount;
    }

    /**
     * Delete the loaded animation. The invoker is responsible for loading a
     * different animation.
     */
    void deleteAnimation() {
        Animation anim = getAnimation().getAnimation();
        AnimControl animControl = getAnimControl().find();

        History.autoAdd();
        animControl.removeAnim(anim);
        setEdited("delete animation");
    }

    /**
     * Delete the attachments node for the selected bone.
     */
    public void deleteAttachmentsNode() {
        SelectedBone selectedBone = getBone();
        assert selectedBone.hasAttachmentsNode();

        History.autoAdd();
        Bone bone = selectedBone.get();
        Node node = MySkeleton.getAttachments(bone);
        List<Integer> nodePosition = findSpatial(node);

        MySkeleton.cancelAttachments(bone);
        boolean success = node.removeFromParent();
        assert success;
        getSceneView().deleteSubtree(nodePosition);

        String boneName = selectedBone.getName();
        String description = "delete attachments node for "
                + MyString.quote(boneName);
        setEdited(description);

        assert !selectedBone.hasAttachmentsNode();
    }

    /**
     * Delete all "extra" spatials in the model, but not the root.
     */
    public void deleteExtraSpatials() {
        if (rootSpatial instanceof Node) {
            History.autoAdd();
            int oldNumSpatials = MySpatial.countSpatials(rootSpatial,
                    Spatial.class);

            Node rootNode = (Node) rootSpatial;
            Map<Bone, Spatial> map = mapAttachments();
            deleteExtraSpatials(rootNode, map.values());

            getSpatial().selectCgmRoot();
            int newNumSpatials = MySpatial.countSpatials(rootSpatial,
                    Spatial.class);
            int numDeleted = oldNumSpatials - newNumSpatials;
            String description = String.format("delete %d extra spatial%s",
                    numDeleted, numDeleted == 1 ? "" : "s");
            setEdited(description);
        }
    }

    /**
     * Delete the selected S-G control. The invoker is responsible for
     * deselecting the control.
     */
    void deleteSgc() {
        Spatial controlled = getSgc().getControlled();
        Control selectedSgc = getSgc().get();
        SceneView sceneView = getSceneView();

        History.autoAdd();
        if (selectedSgc instanceof SkeletonControl) {
            SkeletonControl skeletonControl = (SkeletonControl) selectedSgc;
            Skeleton skeleton = skeletonControl.getSkeleton();
            Map<Bone, Spatial> map = MySkeleton.mapAttachments(skeleton, null);
            for (Bone bone : map.keySet()) {
                Node attachmentsNode = MySkeleton.getAttachments(bone);
                List<Integer> nodePosition = findSpatial(attachmentsNode);

                MySkeleton.cancelAttachments(bone);
                /*
                 * Detach the attachments node from its parent.
                 */
                boolean success = attachmentsNode.removeFromParent();
                assert success;
                /*
                 * Sychronize with the scene view.
                 */
                sceneView.deleteSubtree(nodePosition);
            }

        } else if (selectedSgc instanceof PhysicsControl) {
            List<Integer> treePosition = findSpatial(controlled);
            PhysicsControl pc = (PhysicsControl) selectedSgc;
            int pcPosition = PhysicsUtil.pcToPosition(controlled, pc);
            sceneView.removePhysicsControl(treePosition, pcPosition);
        }

        boolean success = controlled.removeControl(selectedSgc);
        assert success;
        setEdited("delete control");
    }

    /**
     * Delete the selected spatial and its descendents, if any. The invoker is
     * responsible for updating selections.
     */
    void deleteSubtree() {
        SelectedSpatial ss = getSpatial();
        assert !ss.isCgmRoot();

        History.autoAdd();
        Spatial subtree = ss.find();
        /*
         * Cancel all attachments nodes in the subtree.
         */
        if (subtree instanceof Node) {
            Node subtreeNode = (Node) subtree;
            Map<Bone, Spatial> map = mapAttachments();
            for (Entry<Bone, Spatial> mapEntry : map.entrySet()) {
                Spatial spatial = mapEntry.getValue();
                if (spatial == subtree || spatial.hasAncestor(subtreeNode)) {
                    Bone bone = mapEntry.getKey();
                    MySkeleton.cancelAttachments(bone);
                }
            }
        }
        /*
         * Detach the subtree from its parent.
         */
        boolean success = subtree.removeFromParent();
        assert success;
        /*
         * Sychronize the scene view.
         */
        SceneView sceneView = getSceneView();
        sceneView.deleteSubtree();

        setEdited("delete subtree");
    }

    /**
     * Delete the selected user data from the selected spatial. The invoker is
     * responsible for deselecting the user data.
     */
    void deleteUserData() {
        Spatial selectedSpatial = getSpatial().find();
        String key = getUserData().getKey();

        History.autoAdd();
        selectedSpatial.setUserData(key, null);
        String description;
        description = String.format("delete user data %s", MyString.quote(key));
        setEdited(description);
    }

    /**
     * Callback before a checkpoint is created.
     */
    public void preCheckpoint() {
        /*
         * Potentially new continuous edits.
         */
        continousEditState = "";
    }

    /**
     * Rename the selected bone.
     *
     * @param newName new name (not null)
     * @return true if successful, otherwise false
     */
    public boolean renameBone(String newName) {
        Validate.nonNull(newName, "bone name");

        String oldName = getBone().getName();
        boolean success;
        if (!getBone().isSelected()) {
            logger.log(Level.WARNING, "Rename failed: no bone selected.",
                    MyString.quote(newName));
            success = false;

        } else if (newName.equals(SelectedSkeleton.noBone)
                || newName.isEmpty()) {
            logger.log(Level.WARNING, "Rename failed: {0} is a reserved name.",
                    MyString.quote(newName));
            success = false;

        } else if (getSkeleton().hasBone(newName)) {
            logger.log(Level.WARNING,
                    "Rename failed: a bone named {0} already exists.",
                    MyString.quote(newName));
            success = false;

        } else {
            Bone selectedBone = getBone().get();
            History.autoAdd();
            success = MySkeleton.setName(selectedBone, newName);
        }

        if (success) {
            Maud.getModel().getMap().renameBone(oldName, newName);
            setEdited("rename bone");
        }

        return success;
    }

    /**
     * Rename the selected spatial.
     *
     * @param newName new name (not null)
     * @return true if successful, otherwise false
     */
    public boolean renameSpatial(String newName) {
        Validate.nonNull(newName, "spatial name");

        boolean success;
        if (newName.isEmpty()) {
            logger.log(Level.WARNING, "Rename failed: {0} is a reserved name.",
                    MyString.quote(newName));
            success = false;

        } else if (hasSpatial(newName)) {
            logger.log(Level.WARNING,
                    "Rename failed: a spatial named {0} already exists.",
                    MyString.quote(newName));
            success = false;

        } else {
            Spatial selectedSpatial = getSpatial().find();

            History.autoAdd();
            selectedSpatial.setName(newName);
            success = true;
            setEdited("rename spatial");
        }

        return success;
    }

    /**
     * Rename the selected user-data key.
     *
     * @param newKey new key name (not null)
     */
    public void renameUserKey(String newKey) {
        Validate.nonNull(newKey, "new key");

        Spatial sp = getSpatial().find();
        String oldKey = getUserData().getKey();
        Object data = sp.getUserData(oldKey);

        History.autoAdd();
        sp.setUserData(oldKey, null);
        sp.setUserData(newKey, data);
        setEdited("rename user-data key");

        getUserData().selectKey(newKey);
    }

    /**
     * Replace the specified animation with a new one. TODO rename replace
     *
     * @param oldAnimation (not null)
     * @param newAnimation (not null)
     * @param eventDescription description for the edit history (not null)
     */
    void replaceAnimation(Animation oldAnimation, Animation newAnimation,
            String eventDescription) {
        assert oldAnimation != null;
        assert newAnimation != null;
        assert eventDescription != null;

        AnimControl animControl = getAnimControl().find();

        History.autoAdd();
        animControl.removeAnim(oldAnimation);
        animControl.addAnim(newAnimation);
        LoadedAnimation loaded = getAnimation();
        float duration = loaded.getDuration();
        if (loaded.getTime() > duration) {
            loaded.setTime(duration); // keep track time in range
        }
        setEdited(eventDescription);
    }

    /**
     * Add a re-targeted animation to the C-G model.
     *
     * @param newAnimationName name for the resulting animation (not null)
     */
    public void retargetAndAdd(String newAnimationName) {
        Validate.nonNull(newAnimationName, "new animation name");

        Cgm source = Maud.getModel().getSource();
        Animation sourceAnimation = source.getAnimation().getAnimation();
        Skeleton sourceSkeleton = source.getSkeleton().find();
        Skeleton targetSkeleton = getSkeleton().find();
        SkeletonMapping effectiveMap = Maud.getModel().getMap().effectiveMap();
        TweenTransforms techniques = Maud.getModel().getTweenTransforms();
        Animation retargeted = TrackEdit.retargetAnimation(sourceAnimation,
                sourceSkeleton, targetSkeleton, effectiveMap, techniques,
                newAnimationName);

        float duration = retargeted.getLength();
        assert duration >= 0f : duration;

        addAnimation(retargeted);
    }

    /**
     * Alter whether the selected S-G control applies to its spatial's local
     * translation.
     *
     * @param newSetting true&rarr;apply to local, false&rarr;apply to world
     */
    public void setApplyPhysicsLocal(boolean newSetting) {
        Control modelSgc = getSgc().get();
        if (MyControl.canApplyPhysicsLocal(modelSgc)) {
            boolean oldSetting = MyControl.isApplyPhysicsLocal(modelSgc);
            if (oldSetting != newSetting) {
                History.autoAdd();
                MyControl.setApplyPhysicsLocal(modelSgc, newSetting);

                Spatial controlled = getSgc().getControlled();
                List<Integer> treePosition = findSpatial(controlled);
                PhysicsControl pc = (PhysicsControl) modelSgc;
                int pcPosition = PhysicsUtil.pcToPosition(controlled, pc);
                SceneView sceneView = getSceneView();
                sceneView.setApplyPhysicsLocal(treePosition, pcPosition,
                        newSetting);

                if (newSetting) {
                    setEdited("enable local physics");
                } else {
                    setEdited("disable local physics");
                }
            }
        }
    }

    /**
     * Alter the batch hint of the selected spatial.
     *
     * @param newHint new value for batch hint (not null)
     */
    public void setBatchHint(Spatial.BatchHint newHint) {
        Validate.nonNull(newHint, "batch hint");

        Spatial modelSpatial = getSpatial().find();
        Spatial.BatchHint oldHint = modelSpatial.getLocalBatchHint();
        if (oldHint != newHint) {
            History.autoAdd();
            modelSpatial.setBatchHint(newHint);
            // scene view not updated
            setEdited("change batch hint");
        }
    }

    /**
     * Alter the cull hint of the selected spatial.
     *
     * @param newHint new value for cull hint (not null)
     */
    public void setCullHint(Spatial.CullHint newHint) {
        Validate.nonNull(newHint, "cull hint");

        Spatial modelSpatial = getSpatial().find();
        Spatial.CullHint oldHint = modelSpatial.getLocalCullHint();
        if (oldHint != newHint) {
            History.autoAdd();
            modelSpatial.setCullHint(newHint);
            getSceneView().setCullHint(newHint);
            setEdited("change cull hint");
        }
    }

    /**
     * Alter whether the selected geometry ignores its transform.
     *
     * @param newSetting true&rarr;ignore transform, false&rarr;apply transform
     */
    public void setIgnoreTransform(boolean newSetting) {
        Spatial modelSpatial = getSpatial().find();
        if (modelSpatial instanceof Geometry) {
            Geometry geometry = (Geometry) modelSpatial;
            boolean oldSetting = geometry.isIgnoreTransform();
            if (oldSetting != newSetting) {
                History.autoAdd();
                geometry.setIgnoreTransform(newSetting);
                getSceneView().setIgnoreTransform(newSetting);
                if (newSetting) {
                    setEdited("ignore transform");
                } else {
                    setEdited("stop ignoring transform");
                }
            }
        }
    }

    /**
     * Alter all keyframes in the selected bone track.
     *
     * @param times array of keyframe times (not null, not empty)
     * @param translations array of keyframe translations (not null)
     * @param rotations array of keyframe rotations (not null)
     * @param scales array of keyframe scales (may be null)
     */
    void setKeyframes(float[] times, Vector3f[] translations,
            Quaternion[] rotations, Vector3f[] scales) {
        assert times != null;
        assert times.length > 0 : times.length;
        assert translations != null;
        assert rotations != null;

        BoneTrack boneTrack = getTrack().find();

        History.autoAdd();
        if (scales == null) {
            boneTrack.setKeyframes(times, translations, rotations);
        } else {
            boneTrack.setKeyframes(times, translations, rotations, scales);
        }
        setEdited("replace keyframes");
    }

    /**
     * Relocate the selected physics object.
     *
     * @param newLocation (not null, unaffected)
     */
    public void setPhysicsLocation(Vector3f newLocation) {
        Validate.nonNull(newLocation, "new location");

        getObject().setLocation(newLocation);
        setEditedPhysicsPosition();
    }

    /**
     * Reorient the selected physics object.
     *
     * @param newOrientation (not null, unaffected)
     */
    public void setPhysicsOrientation(Quaternion newOrientation) {
        Validate.nonNull(newOrientation, "new orientation");

        getObject().setOrientation(newOrientation);
        setEditedPhysicsPosition();
    }

    /**
     * Alter the render-queue bucket of the selected spatial.
     *
     * @param newBucket new value for queue bucket (not null)
     */
    public void setQueueBucket(RenderQueue.Bucket newBucket) {
        Validate.nonNull(newBucket, "new bucket");

        Spatial modelSpatial = getSpatial().find();
        RenderQueue.Bucket oldBucket = modelSpatial.getLocalQueueBucket();
        if (oldBucket != newBucket) {
            History.autoAdd();
            modelSpatial.setQueueBucket(newBucket);
            getSceneView().setQueueBucket(newBucket);
            setEdited("change render-queue bucket");
        }
    }

    /**
     * Alter the specified parameter of the selected rigid body. TODO move the
     * meat to SelectedPhysics class
     *
     * @param parameter which parameter to alter (not null)
     * @param newValue new parameter value
     */
    public void setRigidBodyParameter(RigidBodyParameter parameter,
            float newValue) {
        Validate.nonNull(parameter, "parameter");

        PhysicsCollisionObject object = getObject().find();
        if (object instanceof PhysicsRigidBody) {
            PhysicsRigidBody prb = (PhysicsRigidBody) object;

            History.autoAdd();
            Vector3f vector;
            switch (parameter) {
                case AngularDamping:
                    prb.setAngularDamping(newValue);
                    break;
                case AngularSleep:
                    prb.setAngularSleepingThreshold(newValue);
                    break;
                case Friction:
                    prb.setFriction(newValue);
                    break;
                case GravityX:
                    vector = prb.getGravity();
                    vector.x = newValue;
                    prb.setGravity(vector);
                    break;
                case GravityY:
                    vector = prb.getGravity();
                    vector.y = newValue;
                    prb.setGravity(vector);
                    break;
                case GravityZ:
                    vector = prb.getGravity();
                    vector.z = newValue;
                    prb.setGravity(vector);
                    break;
                case LinearDamping:
                    prb.setLinearDamping(newValue);
                    break;
                case LinearSleep:
                    prb.setLinearSleepingThreshold(newValue);
                    break;
                case Mass:
                    prb.setMass(newValue);
                    break;
                case Restitution:
                    prb.setRestitution(newValue);
                    break;
                default:
                    throw new IllegalArgumentException();
            }

            String eventDescription = String.format(
                    "set %s of rigid body to %f", parameter, newValue);
            setEdited(eventDescription);
        }
    }

    /**
     * Alter whether the selected S-G control is enabled.
     *
     * @param newSetting true&rarr;enable, false&rarr;disable
     */
    public void setSgcEnabled(boolean newSetting) {
        Control modelSgc = getSgc().get();
        if (MyControl.canDisable(modelSgc)) {
            boolean oldSetting = MyControl.isEnabled(modelSgc);
            if (oldSetting != newSetting) {
                History.autoAdd();
                MyControl.setEnabled(modelSgc, newSetting);
                if (modelSgc instanceof PhysicsControl) {
                    Spatial controlled = getSgc().getControlled();
                    List<Integer> treePosition = findSpatial(controlled);
                    PhysicsControl pc = (PhysicsControl) modelSgc;
                    int pcPosition = PhysicsUtil.pcToPosition(controlled, pc);

                    SceneView sceneView = getSceneView();
                    sceneView.setPhysicsControlEnabled(treePosition, pcPosition,
                            newSetting);
                }
                if (newSetting) {
                    setEdited("enable control");
                } else {
                    setEdited("disable control");
                }
            }
        }
    }

    /**
     * Alter the shadow mode of the selected spatial.
     *
     * @param newMode new value for shadow mode (not null)
     */
    public void setShadowMode(RenderQueue.ShadowMode newMode) {
        Validate.nonNull(newMode, "new mode");

        Spatial modelSpatial = getSpatial().find();
        RenderQueue.ShadowMode oldMode = modelSpatial.getLocalShadowMode();
        if (oldMode != newMode) {
            History.autoAdd();
            modelSpatial.setShadowMode(newMode);
            getSceneView().setMode(newMode);
            String description = String.format(
                    "change spatial's shadow mode to %s", newMode);
            setEdited(description);
        }
    }

    /**
     * Alter the specified parameter of the selected physics collision shape.
     *
     * @param parameter which parameter to alter (not null)
     * @param newValue new parameter value
     */
    public void setShapeParameter(ShapeParameter parameter, float newValue) {
        Validate.nonNull(parameter, "parameter");

        SelectedShape shape = getShape();
        assert shape.canSetParameter(parameter);
        float oldValue = shape.getParameterValue(parameter);
        if (newValue != oldValue) {
            History.autoAdd();
            shape.setParameter(parameter, newValue);
            if (parameter.equals(ShapeParameter.Margin)) {
                String description = String.format(
                        "change shape's margin to %f", newValue);
                setEdited(description);
            } else {
                setEditedShapeSize();
            }
        }
    }

    /**
     * Alter the local rotation of the selected spatial.
     *
     * @param rotation (not null, unaffected)
     */
    public void setSpatialRotation(Quaternion rotation) {
        Validate.nonNull(rotation, "rotation");

        Spatial selectedSpatial = getSpatial().find();
        selectedSpatial.setLocalRotation(rotation);
        setEditedSpatialTransform();
    }

    /**
     * Alter the local scale of the selected spatial.
     *
     * @param scale (not null, unaffected)
     */
    public void setSpatialScale(Vector3f scale) {
        Validate.nonNull(scale, "scale");
        Validate.positive(scale.x, "x scale");
        Validate.positive(scale.y, "y scale");
        Validate.positive(scale.z, "z scale");

        Spatial selectedSpatial = getSpatial().find();
        selectedSpatial.setLocalScale(scale);
        setEditedSpatialTransform();
    }

    /**
     * Alter the local translation of the selected spatial.
     *
     * @param translation (not null, unaffected)
     */
    public void setSpatialTranslation(Vector3f translation) {
        Validate.nonNull(translation, "translation");

        Spatial selectedSpatial = getSpatial().find();
        selectedSpatial.setLocalTranslation(translation);
        setEditedSpatialTransform();
    }

    /**
     * Alter the selected user data.
     *
     * @param valueString string representation of the new value (not null)
     */
    public void setUserData(String valueString) {
        Validate.nonNull(valueString, "value string");

        String key = getUserData().getKey();
        Spatial sp = getSpatial().find();
        Object data = getSpatial().getUserData(key);

        History.autoAdd();
        if (data instanceof Boolean) {
            boolean valueBoolean = Boolean.parseBoolean(valueString);
            sp.setUserData(key, valueBoolean);

        } else if (data instanceof Float) {
            float valueFloat = Float.parseFloat(valueString);
            sp.setUserData(key, valueFloat);

        } else if (data instanceof Integer) {
            int valueInteger = Integer.parseInt(valueString);
            sp.setUserData(key, valueInteger);

        } else if (data instanceof Long) {
            long valueLong = Long.parseLong(valueString);
            sp.setUserData(key, valueLong);

        } else if (data instanceof String) {
            sp.setUserData(key, valueString);
        }
        setEdited("alter user data");
    }

    /**
     * Toggle the bounds type of the selected geometry.
     */
    public void toggleBoundType() {
        SelectedSpatial ss = getSpatial();
        if (ss.isGeometry()) {
            History.autoAdd();
            ss.toggleBoundType();
            setEdited("alter bound type");
        }
    }

    /**
     * Write the C-G model to the filesystem at the specified base path.
     *
     * @param baseFilePath file path without any extension (not null, not empty)
     * @return true if successful, otherwise false
     */
    public boolean writeToFile(String baseFilePath) {
        Validate.nonEmpty(baseFilePath, "base file path");

        String filePath = baseFilePath + ".j3o";
        File file = new File(filePath);
        BinaryExporter exporter = BinaryExporter.getInstance();

        boolean success = true;
        try {
            exporter.save(rootSpatial, file);
        } catch (IOException exception) {
            success = false;
        }

        filePath = file.getAbsolutePath();
        filePath = filePath.replaceAll("\\\\", "/");

        if (success) {
            String af = assetFolderForWrite();
            if (baseFilePath.startsWith(af)) {
                assetRootPath = af;
                baseAssetPath = MyString.remainder(baseFilePath, af);
            } else if (baseFilePath.endsWith(baseAssetPath)
                    && !baseAssetPath.isEmpty()) {
                assetRootPath = MyString.removeSuffix(baseFilePath,
                        baseAssetPath);
            } else {
                assetRootPath = "";
                baseAssetPath = "";
            }
            if (baseAssetPath.startsWith("/")) {
                baseAssetPath = MyString.remainder(baseAssetPath, "/");
            }

            extension = "j3o";
            String eventDescription = "write model to " + filePath;
            setPristine(eventDescription);
            logger.log(Level.INFO, "Wrote model to file {0}",
                    MyString.quote(filePath));
        } else {
            logger.log(Level.SEVERE,
                    "I/O exception while writing model to file {0}",
                    MyString.quote(filePath));
        }

        return success;
    }
    // *************************************************************************
    // LoadedCgm methods

    /**
     * Create a deep copy of this object.
     *
     * @return a new object, equivalent to this one
     * @throws CloneNotSupportedException if the superclass isn't cloneable
     */
    @Override
    public EditableCgm clone() throws CloneNotSupportedException {
        EditableCgm clone = (EditableCgm) super.clone();
        return clone;
    }

    /**
     * Invoked after successfully loading a C-G model.
     *
     * @param cgmRoot (not null)
     */
    @Override
    protected void postLoad(Spatial cgmRoot) {
        assert cgmRoot != null;

        String eventDescription = "load model named " + MyString.quote(name);
        setPristine(eventDescription);

        repair(cgmRoot);

        super.postLoad(cgmRoot);
    }
    // *************************************************************************
    // private methods

    /**
     * Determine the default asset folder for writing the C-G model to the
     * filesystem.
     *
     * @return absolute filesystem path (not null, not empty)
     */
    private String assetFolderForWrite() {
        String result = assetRootPath;
        if (result.isEmpty() || result.endsWith(".jar")
                || result.endsWith(".zip")) {
            result = ActionApplication.getWrittenAssetDirPath();
        }

        return result;
    }

    /**
     * Delete all "extra" spatials among a node's descendents. Note: recursive!
     *
     * @param subtree subtree to traverse (not null)
     * @param attachmentsNodes collection of attachments nodes (not null,
     * unaffected)
     */
    private void deleteExtraSpatials(Node subtree,
            Collection<Spatial> attachmentsNodes) {
        assert subtree != null;
        assert attachmentsNodes != null;

        List<Spatial> childList = subtree.getChildren();
        int numChildren = childList.size();
        Spatial[] children = childList.toArray(new Spatial[numChildren]);
        for (Spatial child : children) {
            if (MaudUtil.isExtra(child, attachmentsNodes)) {
                List<Integer> position = findSpatial(child);
                int index = subtree.detachChild(child);
                assert index != -1;
                getSceneView().deleteSubtree(position);
            }
        }

        for (Spatial child : subtree.getChildren()) {
            if (child instanceof Node) {
                deleteExtraSpatials((Node) child, attachmentsNodes);
            }
        }
    }

    /**
     * Repair minor issues with a C-G model, such as repetitious keyframes and
     * tracks without a keyframe at t=0.
     *
     * @param cgmRoot model to correct (not null)
     */
    private void repair(Spatial cgmRoot) {
        int numTracksZfed = 0;
        int numTracksRred = 0;

        List<AnimControl> animControls;
        animControls = MySpatial.listControls(cgmRoot, AnimControl.class, null);
        for (AnimControl animControl : animControls) {
            Collection<String> names = animControl.getAnimationNames();
            for (String animationName : names) {
                Animation anim = animControl.getAnim(animationName);
                numTracksZfed += TrackEdit.zeroFirst(anim);
                numTracksRred += TrackEdit.removeRepeats(anim);
            }
        }

        if (numTracksZfed > 0) {
            String description = "zeroed the time of the 1st keyframe in ";
            if (numTracksZfed == 1) {
                description += "one track";
            } else {
                description += String.format("%d tracks", numTracksZfed);
            }
            setEdited(description);
        }

        if (numTracksRred > 0) {
            String description = "removed repeat keyframe(s) from ";
            if (numTracksRred == 1) {
                description += "one track";
            } else {
                description += String.format("%d tracks", numTracksRred);
            }
            setEdited(description);
        }
    }

    /**
     * Increment the count of unsaved edits.
     *
     * @param eventDescription description of causative event (not null)
     */
    private void setEdited(String eventDescription) {
        assert eventDescription != null;

        ++editCount;
        continousEditState = "";
        History.addEvent(eventDescription);
    }

    /**
     * If not a continuation of the previous physics-position edit, update the
     * edit count.
     */
    private void setEditedPhysicsPosition() {
        String newState = "pp" + getObject().getName();
        if (!newState.equals(continousEditState)) {
            History.autoAdd();
            ++editCount;
            continousEditState = newState;
            History.addEvent("reposition physics");
        }
    }

    /**
     * If not a continuation of the previous shape-size edit, update the edit
     * count.
     */
    private void setEditedShapeSize() {
        String newState = "ss" + getShape().toString();
        if (!newState.equals(continousEditState)) {
            History.autoAdd();
            ++editCount;
            continousEditState = newState;
            History.addEvent("resize shape");
        }
    }

    /**
     * If not a continuation of the previous spatial-transform edit, update the
     * edit count.
     */
    private void setEditedSpatialTransform() {
        String newState = "st" + getSpatial().toString();
        if (!newState.equals(continousEditState)) {
            History.autoAdd();
            ++editCount;
            continousEditState = newState;
            History.addEvent("transform spatial");
        }
    }

    /**
     * Mark the C-G model as pristine.
     *
     * @param eventDescription description of causative event (not null)
     */
    private void setPristine(String eventDescription) {
        editCount = 0;
        continousEditState = "";
        History.addEvent(eventDescription);
    }
}
