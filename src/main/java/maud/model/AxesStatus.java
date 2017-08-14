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

import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Validate;

/**
 * The status of the visible coordinate axes in scene views.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class AxesStatus implements Cloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            AxesStatus.class.getName());
    // *************************************************************************
    // fields

    /**
     * flag to enable depth test for visibility of the axes
     */
    private boolean depthTestFlag = false;
    /**
     * line width for the axes (in pixels, &ge;1)
     */
    private float lineWidth = 4f;
    /**
     * which set of axes is active (either "bone", "model", "none", "spatial",
     * or "world")
     */
    private String mode = "bone";
    // *************************************************************************
    // new methods exposed

    /**
     * Read the depth test flag.
     *
     * @return true to enable test, otherwise false
     */
    public boolean getDepthTestFlag() {
        return depthTestFlag;
    }

    /**
     * Read the width of each line.
     *
     * @return width (in pixels, &ge;1)
     */
    public float getLineWidth() {
        assert lineWidth >= 1f : lineWidth;
        return lineWidth;
    }

    /**
     * Read the current mode string.
     *
     * @return mode string (not null)
     */
    public String getMode() {
        assert mode != null;
        return mode;
    }

    /**
     * Alter the depth-test flag.
     *
     * @param newState true &rarr; enable depth test, false &rarr; no depth test
     */
    public void setDepthTestFlag(boolean newState) {
        this.depthTestFlag = newState;
    }

    /**
     * Alter the width.
     *
     * @param width line width for axes (in pixels, &ge;1)
     */
    public void setLineWidth(float width) {
        Validate.inRange(width, "width", 1f, Float.MAX_VALUE);
        this.lineWidth = width;
    }

    /**
     * Alter the display mode.
     *
     * @param modeName name of new axes display mode (not null)
     */
    public void setMode(String modeName) {
        Validate.nonNull(modeName, "mode name");

        switch (modeName) {
            case "bone":
            case "model":
            case "none":
            case "spatial":
            case "world":
                mode = modeName;
                break;
            default:
                logger.log(Level.SEVERE, "mode name={0}", modeName);
                throw new IllegalArgumentException("invalid mode name");
        }
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
    public AxesStatus clone() throws CloneNotSupportedException {
        AxesStatus clone = (AxesStatus) super.clone();
        return clone;
    }
}
