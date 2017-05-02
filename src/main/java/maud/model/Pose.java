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

import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.Validate;
import maud.Maud;

/**
 * The displayed pose in the Maud application.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class Pose {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            Pose.class.getName());
    // *************************************************************************
    // fields

    /**
     * user transforms which describe the pose, one for each bone
     */
    final private List<Transform> currentPose = new ArrayList<>(30);
    // *************************************************************************
    // new methods exposed

    /**
     * Copy the user transform of the indexed bone in the pose.
     *
     * @param boneIndex which bone to copy
     * @return a new instance
     */
    public Transform copyBoneTransform(int boneIndex) {
        Transform result = currentPose.get(boneIndex);
        result = result.clone();

        return result;
    }

    /**
     * Pose the CG model per the loaded animation.
     */
    public void poseSkeleton() {
        int boneCount = Maud.model.countBones();
        for (int boneIndex = 0; boneIndex < boneCount; boneIndex++) {
            Transform transform = currentPose.get(boneIndex);
            Maud.model.animation.boneTransform(boneIndex, transform);
        }

        Maud.viewState.poseSkeleton(currentPose);
    }

    /**
     * Reset the pose to bind pose.
     */
    public void resetPose() {
        int boneCount = Maud.model.countBones();
        currentPose.clear();
        for (int boneIndex = 0; boneIndex < boneCount; boneIndex++) {
            Transform transform = new Transform();
            currentPose.add(transform);
        }

        Maud.viewState.poseSkeleton(currentPose);
    }

    /**
     * Alter the user rotation of the indexed bone.
     *
     * @param boneIndex which bone to rotate
     * @param rotation (not null, unaffected)
     */
    public void setBoneRotation(int boneIndex, Quaternion rotation) {
        Validate.nonNull(rotation, "rotation");

        Transform boneTransform = currentPose.get(boneIndex);
        boneTransform.setRotation(rotation);

        Maud.viewState.poseSkeleton(currentPose);
    }

    /**
     * Alter the user scale of the indexed bone.
     *
     * @param boneIndex which bone to scale
     * @param scale (not null, unaffected)
     */
    public void setBoneScale(int boneIndex, Vector3f scale) {
        Validate.nonNull(scale, "scale");

        Transform boneTransform = currentPose.get(boneIndex);
        boneTransform.setScale(scale);

        Maud.viewState.poseSkeleton(currentPose);
    }

    /**
     * Alter the user translation of the indexed bone.
     *
     * @param boneIndex which bone to translate
     * @param translation (not null, unaffected)
     */
    public void setBoneTranslation(int boneIndex, Vector3f translation) {
        Validate.nonNull(translation, "translation");

        Transform boneTransform = currentPose.get(boneIndex);
        boneTransform.setTranslation(translation);

        Maud.viewState.poseSkeleton(currentPose);
    }
}
