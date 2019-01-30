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

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.bullet.collision.shapes.ConeCollisionShape;
import com.jme3.bullet.collision.shapes.CylinderCollisionShape;
import com.jme3.bullet.collision.shapes.GImpactCollisionShape;
import com.jme3.bullet.collision.shapes.HullCollisionShape;
import com.jme3.bullet.collision.shapes.MeshCollisionShape;
import com.jme3.bullet.collision.shapes.MultiSphere;
import com.jme3.bullet.collision.shapes.SimplexCollisionShape;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.bullet.collision.shapes.infos.ChildCollisionShape;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import jme3utilities.Validate;
import jme3utilities.math.MyMath;
import jme3utilities.math.MyVector3f;
import jme3utilities.minie.MyShape;
import maud.ParseUtil;
import maud.PhysicsUtil;
import maud.model.History;
import maud.model.option.ShapeParameter;
import maud.view.scene.SceneView;

/**
 * The selected physics shape in the Maud application.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class SelectedShape implements JmeCloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(SelectedShape.class.getName());
    // *************************************************************************
    // fields

    /**
     * C-G model containing the selected shape (set by {@link #setCgm(Cgm)})
     */
    private Cgm cgm = null;
    /**
     * editable C-G model, if any, containing the selected shape (set by
     * {@link #setCgm(Cgm)})
     */
    private EditableCgm editableCgm = null;
    /**
     * id of the selected shape, or -1L for none
     */
    private long selectedId = -1L;
    // *************************************************************************
    // new methods exposed

    /**
     * Create a compound shape having the selected shape as its only child. The
     * new shape replaces the child shape in every collision object that uses
     * it.
     * <p>
     * The child shape cannot itself be a compound shape.
     */
    public void addParent() {
        CollisionShape child = find();
        if (child != null && !(child instanceof CompoundCollisionShape)) {
            CompoundCollisionShape parent = new CompoundCollisionShape();
            Vector3f location = new Vector3f();
            parent.addChildShape(child, location);

            replaceInObjects(parent,
                    "replace collision shape with a compound shape");
        }
    }

    /**
     * Test whether the specified parameter can be set to the specified value.
     *
     * @param parameter which parameter (not null)
     * @return true if settable, otherwise false
     */
    public boolean canSet(ShapeParameter parameter) {
        Validate.nonNull(parameter, "parameter");

        if (!isSelected()) {
            return false;
        }
        CollisionShape shape = find();

        boolean box = shape instanceof BoxCollisionShape;
        boolean capsule = shape instanceof CapsuleCollisionShape;
        boolean cone = shape instanceof ConeCollisionShape;
        boolean cylinder = shape instanceof CylinderCollisionShape;
        boolean sphere = shape instanceof SphereCollisionShape;

        boolean result;
        switch (parameter) {
            case HalfExtentX:
            case HalfExtentY:
            case HalfExtentZ:
                result = box || cylinder; // TODO height axis of cone/cyl
                break;
            case Height:
            case Radius:
                result = box || capsule || cone || cylinder || sphere;
                break;
            case Margin:
                result = !sphere && !capsule;
                break;
            case ScaleX:
            case ScaleY:
            case ScaleZ:
                result = false; // TODO
                break;
            case ScaledVolume:
                result = false; // TODO
                break;
            default:
                throw new IllegalArgumentException(parameter.toString());
        }

        return result;
    }

    /**
     * Copy the scale of the shape. TODO remove?
     *
     * @param storeResult (modified if not null)
     * @return a scale vector (either storeResult or a new instance)
     */
    public Vector3f copyScale(Vector3f storeResult) {
        if (storeResult == null) {
            storeResult = new Vector3f();
        }

        CollisionShape shape = find();
        shape.getScale(storeResult);

        assert MyVector3f.isAllNonNegative(storeResult);
        return storeResult;
    }

    /**
     * Count the children in a compound shape.
     *
     * @return count (&ge;0)
     */
    public int countChildren() {
        int count = 0;
        CollisionShape shape = find();
        if (shape instanceof CompoundCollisionShape) {
            CompoundCollisionShape ccs = (CompoundCollisionShape) shape;
            List<ChildCollisionShape> children = ccs.getChildren();
            count = children.size();
        }

        assert count >= 0 : count;
        return count;
    }

    /**
     * Count the vertices used to generate the shape.
     *
     * @return count (&ge;0)
     */
    public int countGeneratorVertices() {
        int count = 0;
        CollisionShape shape = find();
        if (shape instanceof CapsuleCollisionShape) {
            count = 2;
        } else if (shape instanceof GImpactCollisionShape) {
            GImpactCollisionShape giShape = (GImpactCollisionShape) shape;
            count = giShape.countMeshVertices();
        } else if (shape instanceof HullCollisionShape) {
            HullCollisionShape hullShape = (HullCollisionShape) shape;
            count = hullShape.countMeshVertices();
        } else if (shape instanceof MeshCollisionShape) {
            MeshCollisionShape meshShape = (MeshCollisionShape) shape;
            count = meshShape.countMeshVertices();
        } else if (shape instanceof MultiSphere) {
            MultiSphere multiSphereShape = (MultiSphere) shape;
            count = multiSphereShape.countSpheres();
        } else if (shape instanceof SimplexCollisionShape) {
            SimplexCollisionShape simplexShape = (SimplexCollisionShape) shape;
            count = simplexShape.countMeshVertices();
        } else if (shape instanceof SphereCollisionShape) {
            count = 1;
        }

        assert count >= 0 : count;
        return count;
    }

    /**
     * Describe the shape.
     *
     * @return a brief description of the shape, or "" if none selected
     */
    public String describe() {
        String result = "";
        CollisionShape shape = find();
        if (shape != null) {
            result = MyShape.describe(shape);
        }

        return result;
    }

    /**
     * Access the selected shape.
     *
     * @return the pre-existing instance, or null if not found
     */
    CollisionShape find() {
        PhysicsSpace space = cgm.getSceneView().getPhysicsSpace();
        Map<Long, CollisionShape> map = PhysicsUtil.shapeMap(space);
        CollisionShape result = map.get(selectedId);

        return result;
    }

    /**
     * Determine the axis index of the shape. TODO rename axisIndex()
     *
     * @return 0&rarr;X, 1&rarr;Y, 2&rarr;Z, -1&rarr;doesn't have an axis
     */
    public int getAxisIndex() {
        CollisionShape shape = find();
        int result = -1;
        if (shape != null) {
            result = MyShape.axisIndex(shape);
        }

        return result;
    }

    /**
     * Read the Bullet id of the selected shape. TODO rename id()
     *
     * @return id, or -1L if none selected
     */
    public long getId() {
        return selectedId;
    }

    /**
     * Read the type of the selected shape. TODO rename type()
     *
     * @return abbreviated class name, or "" if none selected
     */
    public String getType() {
        String type = "";
        CollisionShape shape = find();
        if (shape != null) {
            type = MyShape.describeType(shape);
        }

        assert type != null;
        return type;
    }

    /**
     * Read the specified parameter of the shape. TODO rename value()
     *
     * @param parameter which parameter to read (not null)
     * @return parameter value (&ge;0) or NaN if not applicable
     */
    public float getValue(ShapeParameter parameter) {
        Validate.nonNull(parameter, "parameter");

        float result = Float.NaN;
        if (isSelected()) {
            CollisionShape shape = find();
            if (shape instanceof CompoundCollisionShape) {
                switch (parameter) {
                    case HalfExtentX:
                    case HalfExtentY:
                    case HalfExtentZ:
                    case Height:
                    case Radius:
                        return Float.NaN;
                }
            }
            switch (parameter) {
                case HalfExtentX:
                case HalfExtentY:
                case HalfExtentZ:
                    Vector3f halfExtents = MyShape.halfExtents(shape, null);
                    if (parameter == ShapeParameter.HalfExtentX) {
                        result = halfExtents.x;
                    } else if (parameter == ShapeParameter.HalfExtentY) {
                        result = halfExtents.y;
                    } else if (parameter == ShapeParameter.HalfExtentZ) {
                        result = halfExtents.z;
                    }
                    break;

                case Height:
                    result = MyShape.height(shape);
                    break;
                case Margin:
                    result = shape.getMargin();
                    break;
                case Radius:
                    result = MyShape.radius(shape);
                    break;
                case ScaleX:
                    result = shape.getScale(null).x;
                    break;
                case ScaleY:
                    result = shape.getScale(null).y;
                    break;
                case ScaleZ:
                    result = shape.getScale(null).z;
                    break;
                case ScaledVolume:
                    result = MyShape.volume(shape);
                    break;
                default:
                    throw new IllegalArgumentException(parameter.toString());
            }
        }

        assert Float.isNaN(result) || result >= 0f : result;
        return result;
    }

    /**
     * Calculate the half extents of the selected shape.
     *
     * @param storeResult (modified if not null)
     * @return half extents on the local axes in world units (either storeResult
     * or a new instance)
     */
    public Vector3f halfExtents(Vector3f storeResult) {
        CollisionShape shape = find();
        storeResult = MyShape.halfExtents(shape, storeResult);

        return storeResult;
    }

    /**
     * Find the index of the shape among all shapes in the C-G model in ID
     * order.
     *
     * @return index (&ge;0)
     */
    public int index() {
        List<Long> ids = listShapeIds();
        int index = ids.indexOf(selectedId);

        assert index >= 0 : index;
        return index;
    }

    /**
     * Test whether the shape is a compound shape.
     *
     * @return true if compound, otherwise false
     */
    public boolean isCompound() {
        CollisionShape shape = find();
        boolean result = shape instanceof CompoundCollisionShape;

        return result;
    }

    /**
     * Test whether a collision shape is selected.
     *
     * @return true if selected, otherwise false
     */
    public boolean isSelected() {
        CollisionShape shape = find();
        boolean result;
        if (shape == null) {
            result = false;
        } else {
            result = true;
        }

        return result;
    }

    /**
     * Enumerate the children of a compound shape.
     *
     * @param prefix (not null. may be empty)
     * @return a new list of descriptions
     */
    public List<String> listChildNames(String prefix) {
        Validate.nonNull(prefix, "prefix");

        List<String> result;
        CollisionShape shape = find();
        if (shape instanceof CompoundCollisionShape) {
            CompoundCollisionShape compound = (CompoundCollisionShape) shape;
            List<ChildCollisionShape> children = compound.getChildren();
            int count = children.size();
            result = new ArrayList<>(count);
            for (int childIndex = 0; childIndex < count; childIndex++) {
                ChildCollisionShape child = children.get(childIndex);
                String description = MyShape.describe(child.getShape());
                if (description.startsWith(prefix)) {
                    result.add(description);
                }
            }
        } else {
            result = new ArrayList<>(0);
        }

        return result;
    }

    /**
     * Resize the shape by the specified factors without altering its scale. Has
     * no effect on compound shapes. TODO implement for compound shapes
     *
     * @param factors size factor to apply each local axis (not null,
     * unaffected)
     */
    public void resize(Vector3f factors) {
        Validate.nonNull(factors, "factors");

        if (!MyVector3f.isScaleIdentity(factors) && !isCompound()) {
            Vector3f he = halfExtents(null);
            he.multLocal(factors);
            setHalfExtents(he);
            String shapeName = find().toString();
            editableCgm.getEditState().setEditedShapeSize(shapeName);
        }
    }

    /**
     * Select the identified shape.
     *
     * @param shapeId which shape
     */
    public void select(long shapeId) {
        SceneView sceneView = cgm.getSceneView();
        Map<Long, CollisionShape> map = sceneView.shapeMap();
        assert map.containsKey(shapeId) : shapeId;
        selectedId = shapeId;
    }

    /**
     * Select the described shape.
     *
     * @param description the shape's description (not null, not empty)
     */
    public void select(String description) {
        long id = ParseUtil.parseShapeId(description);
        select(id);
    }

    /**
     * Select the 1st child shape of the compound shape.
     */
    public void selectFirstChild() {
        CollisionShape parent = find();
        if (parent instanceof CompoundCollisionShape) {
            CompoundCollisionShape ccs = (CompoundCollisionShape) parent;
            List<ChildCollisionShape> children = ccs.getChildren();
            if (!children.isEmpty()) {
                ChildCollisionShape child = children.get(0);
                selectedId = child.getShape().getObjectId();
            }
        }
    }

    /**
     * Select the next shape in the C-G model in cyclic ID order.
     */
    public void selectNext() {
        List<Long> ids = listShapeIds();
        int index = ids.indexOf(selectedId);
        if (index != -1) {
            int numObjects = ids.size();
            int newIndex = MyMath.modulo(index + 1, numObjects);
            selectedId = ids.get(newIndex);
        }
    }

    /**
     * Deselect the selected shape, if any.
     */
    public void selectNone() {
        selectedId = -1L;
    }

    /**
     * Select the previous shape in the C-G model in cyclic ID order.
     */
    public void selectPrevious() {
        List<Long> ids = listShapeIds();
        int index = ids.indexOf(selectedId);
        if (index != -1) {
            int numObjects = ids.size();
            int newIndex = MyMath.modulo(index - 1, numObjects);
            selectedId = ids.get(newIndex);
        }
    }

    /**
     * Alter the value of specified parameter of the shape.
     *
     * @param parameter which parameter to alter (not null)
     * @param newValue new value for the parameter (&ge;0)
     */
    void set(ShapeParameter parameter, float newValue) {
        assert parameter != null;
        assert newValue >= 0f : newValue;
        assert isSelected();

        CollisionShape shape = find();
        Vector3f halfExtents;
        switch (parameter) {
            case HalfExtentX:
                halfExtents = MyShape.halfExtents(shape, null);
                halfExtents.x = newValue;
                setHalfExtents(halfExtents);
                break;

            case HalfExtentY:
                halfExtents = MyShape.halfExtents(shape, null);
                halfExtents.y = newValue;
                setHalfExtents(halfExtents);
                break;

            case HalfExtentZ:
                halfExtents = MyShape.halfExtents(shape, null);
                halfExtents.z = newValue;
                setHalfExtents(halfExtents);
                break;

            case Height:
                setHeight(newValue);
                break;

            case Margin:
                shape.setMargin(newValue);
                break;

            case Radius:
                setRadius(newValue);
                break;

            case ScaleX:
            case ScaleY:
            case ScaleZ:
            // TODO for kinematic controls, alter via the spatial

            default:
                throw new IllegalArgumentException(parameter.toString());
        }
    }

    /**
     * Alter which C-G model contains the selected shape. (Invoked only during
     * initialization and cloning.)
     *
     * @param newCgm (not null, aliases created)
     */
    void setCgm(Cgm newCgm) {
        assert newCgm != null;
        assert newCgm.getShape() == this;

        cgm = newCgm;
        if (newCgm instanceof EditableCgm) {
            editableCgm = (EditableCgm) newCgm;
        } else {
            editableCgm = null;
        }
    }

    /**
     * Replace the shape with new shape that has different half extents.
     *
     * @param newHalfExtents (not null, all elements non-negative)
     */
    void setHalfExtents(Vector3f newHalfExtents) {
        assert newHalfExtents != null;
        assert MyVector3f.isAllNonNegative(newHalfExtents) : newHalfExtents;

        CollisionShape shape = find();
        CollisionShape newShape = MyShape.setHalfExtents(shape, newHalfExtents);
        if (newShape != null) {
            replaceForResize(newShape);
        }
    }

    /**
     * Alter the specified parameter of the selected physics collision shape.
     *
     * @param parameter which parameter to alter (not null)
     * @param newValue new parameter value
     */
    public void setParameter(ShapeParameter parameter, float newValue) {
        Validate.nonNull(parameter, "parameter");

        assert canSet(parameter);
        float oldValue = getValue(parameter);
        if (newValue != oldValue) {
            if (parameter.equals(ShapeParameter.Margin)) {
                if (newValue > 0f) {
                    History.autoAdd();
                    set(parameter, newValue);
                    String description = String.format(
                            "change shape's margin to %f", newValue);
                    editableCgm.getEditState().setEdited(description);
                }
            } else {
                set(parameter, newValue);
                String shapeName = find().toString();
                editableCgm.getEditState().setEditedShapeSize(shapeName);
            }
        }
    }

    /**
     * Calculate the transform of the shape.
     *
     * @param storeResult (modified if not null)
     * @return world transform (either storeResult or a new instance)
     */
    public Transform transform(Transform storeResult) {
        if (storeResult == null) {
            storeResult = new Transform();
        }

        SelectedObject selectedObject = cgm.getObject();
        if (selectedObject.usesShape(selectedId)) {
            selectedObject.transform(storeResult);
            // further transform if part of a compound shape
        } else {
            Set<Long> userSet = userSet();
            int numUsers = userSet.size();
            if (numUsers == 1) {
                Long[] userIds = new Long[1];
                userSet.toArray(userIds);
                long userId = userIds[0];

                PhysicsSpace space = cgm.getSceneView().getPhysicsSpace();
                PhysicsCollisionObject objectUser
                        = PhysicsUtil.findObject(userId, space);
                if (objectUser != null) {
                    PhysicsUtil.transform(objectUser, storeResult);
                } else {
                    CollisionShape shapeUser
                            = PhysicsUtil.findShape(userId, space);
                    CompoundCollisionShape compound
                            = (CompoundCollisionShape) shapeUser;
                    List<ChildCollisionShape> children = compound.getChildren();

                    Transform parent = new Transform();
                    for (ChildCollisionShape child : children) {
                        long id = child.getShape().getObjectId();
                        if (id == userId) {
                            parent.setTranslation(child.getLocation(null));
                            Quaternion rot = parent.getRotation();
                            rot.fromRotationMatrix(child.getRotation(null));
                        }
                    }
                    storeResult.combineWithParent(parent);
                }

            } else {
                /*
                 * shape has multiple users, or none
                 */
                storeResult.loadIdentity();
            }
        }

        return storeResult;
    }

    /**
     * Enumerate all collision objects and compound shapes that reference the
     * selected shape.
     *
     * @return a new set of ids of objects/shapes
     */
    public Set<Long> userSet() {
        PhysicsSpace space = cgm.getSceneView().getPhysicsSpace();
        Set<Long> result = PhysicsUtil.userSet(selectedId, space);

        return result;
    }
    // *************************************************************************
    // JmeCloneable methods

    /**
     * Don't use this method; use a {@link com.jme3.util.clone.Cloner} instead.
     *
     * @return never
     * @throws CloneNotSupportedException always
     */
    @Override
    public SelectedShape clone() throws CloneNotSupportedException {
        super.clone();
        throw new CloneNotSupportedException("use a cloner");
    }

    /**
     * Callback from {@link com.jme3.util.clone.Cloner} to convert this
     * shallow-cloned instance into a deep-cloned one, using the specified
     * cloner and original to resolve copied fields.
     *
     * @param cloner the cloner currently cloning this control
     * @param original the control from which this control was shallow-cloned
     */
    @Override
    public void cloneFields(Cloner cloner, Object original) {
    }

    /**
     * Create a shallow clone for the JME cloner.
     *
     * @return a new instance
     */
    @Override
    public SelectedShape jmeClone() {
        try {
            SelectedShape clone = (SelectedShape) super.clone();
            return clone;
        } catch (CloneNotSupportedException exception) {
            throw new RuntimeException(exception);
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Enumerate all shapes in the C-G model in ascending ID order.
     *
     * @return a new list of shape IDs
     */
    private List<Long> listShapeIds() {
        SceneView sceneView = cgm.getSceneView();
        Map<Long, CollisionShape> map = sceneView.shapeMap();
        List<Long> result = new ArrayList<>(map.keySet());
        Collections.sort(result);

        return result;
    }

    /**
     * Replace the selected shape with a resized shape.
     *
     * @param newShape replacement shape (not null, not a compound shape)
     */
    private void replaceForResize(CollisionShape newShape) {
        assert newShape != null;
        assert !(newShape instanceof CompoundCollisionShape);

        PhysicsSpace space = cgm.getSceneView().getPhysicsSpace();
        CollisionShape shape = find();
        PhysicsUtil.replaceInObjects(space, shape, newShape);
        PhysicsUtil.replaceInCompounds(space, shape, newShape);

        editableCgm.getEditState().replaceForResize(shape, newShape);

        long newShapeId = newShape.getObjectId();
        selectedId = newShapeId;
    }

    /**
     * Replace the selected shape with a new shape, but only in objects, not in
     * compound shapes.
     *
     * @param newShape replacement shape (not null)
     * @param eventDescription description for the edit history (not null, not
     * empty)
     */
    private void replaceInObjects(CollisionShape newShape,
            String eventDescription) {
        assert newShape != null;
        assert eventDescription != null;
        assert !eventDescription.isEmpty();

        CollisionShape shape = find();
        PhysicsSpace space = cgm.getSceneView().getPhysicsSpace();

        History.autoAdd();
        PhysicsUtil.replaceInObjects(space, shape, newShape);
        editableCgm.getEditState().setEdited(eventDescription);
        long newShapeId = newShape.getObjectId();
        selectedId = newShapeId;
    }

    /**
     * Replace the shape with a new shape that has a different height.
     *
     * @param newHeight (&ge;0)
     */
    private void setHeight(float newHeight) {
        assert newHeight >= 0f : newHeight;

        CollisionShape shape = find();
        CollisionShape newShape = MyShape.setHeight(shape, newHeight);
        replaceForResize(newShape);
    }

    /**
     * Replace the shape with a new shape that has a different radius.
     *
     * @param newRadius (&ge;0)
     */
    private void setRadius(float newRadius) {
        assert newRadius >= 0f : newRadius;

        CollisionShape shape = find();
        CollisionShape newShape = MyShape.setRadius(shape, newRadius);
        replaceForResize(newShape);
    }
}
