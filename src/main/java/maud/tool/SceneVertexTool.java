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
package maud.tool;

import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import de.lessvoid.nifty.controls.Slider;
import java.util.logging.Logger;
import jme3utilities.nifty.BasicScreenController;
import jme3utilities.nifty.WindowController;
import maud.Maud;
import maud.model.Cgm;
import maud.model.SelectedVertex;
import maud.model.option.scene.VertexOptions;

/**
 * The controller for the "Scene Vertex Tool" window in Maud's editor screen.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class SceneVertexTool extends WindowController {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            SceneVertexTool.class.getName());
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized controller.
     *
     * @param screenController
     */
    SceneVertexTool(BasicScreenController screenController) {
        super(screenController, "sceneVertexTool", false);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Update the MVC model based on the sliders.
     */
    void onSliderChanged() {
        VertexOptions options = Maud.getModel().getScene().getVertex();

        ColorRGBA color = Maud.gui.readColorBank("sv");
        options.setColor(color);

        float pointSize = Maud.gui.readSlider("svPointSize");
        options.setPointSize(pointSize);
    }

    /**
     * Update the visualization based on the MVC model.
     *
     * @param cgm which C-G model's view to update (not null)
     */
    void updateVisualizer(Cgm cgm) {
        Spatial spatial = cgm.getSceneView().getVertexSpatial();

        SelectedVertex vertex = cgm.getVertex();
        if (vertex.isSelected()) {
            Vector3f worldLocation = vertex.worldLocation(null);
            spatial.setLocalTranslation(worldLocation);

            Geometry geometry = (Geometry) spatial;
            Material material = geometry.getMaterial();

            VertexOptions options = Maud.getModel().getScene().getVertex();
            ColorRGBA color = options.copyColor(null);
            material.setColor("Color", color);
            float pointSize = options.getPointSize();
            material.setFloat("PointSize", pointSize);

            spatial.setCullHint(Spatial.CullHint.Never);
        } else {
            spatial.setCullHint(Spatial.CullHint.Always);
        }
    }
    // *************************************************************************
    // WindowController methods

    /**
     * Callback to update this window prior to rendering. (Invoked once per
     * render pass.)
     *
     * @param elapsedTime time interval between render passes (in seconds,
     * &ge;0)
     */
    @Override
    public void update(float elapsedTime) {
        super.update(elapsedTime);
        Maud.gui.setIgnoreGuiChanges(true);
        VertexOptions options = Maud.getModel().getScene().getVertex();

        ColorRGBA color = options.copyColor(null);
        Maud.gui.setColorBank("sv", color);

        Slider slider = Maud.gui.getSlider("svPointSize");
        float pointSize = options.getPointSize();
        slider.setValue(pointSize);
        pointSize = Math.round(pointSize);
        Maud.gui.updateSliderStatus("svPointSize", pointSize, " pixels");

        Maud.gui.setIgnoreGuiChanges(false);
    }
}
