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
 * Stephen Gold's name may not be used to endorse or promote products
 derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL STEPHEN GOLD BE LIABLE FOR ANY
 DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package maud.dialog;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import de.lessvoid.nifty.controls.Button;
import de.lessvoid.nifty.controls.TextField;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.elements.render.TextRenderer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.nifty.DialogController;

/**
 * Controller for a text-entry dialog box used to select an asset path.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class AssetDialog implements DialogController {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            AssetDialog.class.getName());
    // *************************************************************************
    // fields

    /**
     * manager for loading assets
     */
    final private AssetManager manager;
    /**
     * list of accepted extensions (not null, no elements null)
     */
    final private List<String> extensions = new ArrayList<>(4);
    /**
     * description of the commit action (not null, not empty, should fit the
     * button -- about 8 or 9 characters)
     */
    final private String commitDescription;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a controller with the specified commit description and list
     * of extensions.
     *
     * @param description (not null)
     * @param extList list of accepted extensions (not null)
     * @param assetManager the asset manager (not null)
     */
    public AssetDialog(String description, Collection<String> extList,
            AssetManager assetManager) {
        assert description != null;
        assert !description.isEmpty();
        assert extList != null;
        assert assetManager != null;

        commitDescription = description;
        extensions.addAll(extList);
        manager = assetManager;
    }
    // *************************************************************************
    // DialogController methods

    /**
     * Test whether "commit" actions are allowed.
     *
     * @param dialogElement (not null)
     * @return true if allowed, otherwise false
     */
    @Override
    public boolean allowCommit(Element dialogElement) {
        String proposedAssetPath = getPath(dialogElement);
        if (hasExtension(proposedAssetPath) && exists(proposedAssetPath)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Callback to update the dialog box prior to rendering. (Invoked once per
     * render pass.)
     *
     * @param dialogElement (not null)
     * @param elapsedTime time interval between render passes (in seconds,
     * &ge;0)
     */
    @Override
    public void update(Element dialogElement, float elapsedTime) {
        String commitLabel, feedbackMessage;
        String proposedAssetPath = getPath(dialogElement);
        if (hasExtension(proposedAssetPath)) {
            if (exists(proposedAssetPath)) {
                commitLabel = commitDescription;
                feedbackMessage = "";
            } else {
                commitLabel = "";
                feedbackMessage = String.format("asset doesn't exist");
            }
        } else {
            commitLabel = "";
            String list = joinExtensions();
            feedbackMessage = String.format("needs extension: %s",
                    MyString.quote(list));
        }

        Button commitButton = dialogElement.findNiftyControl("#commit",
                Button.class);
        commitButton.setText(commitLabel);

        Element feedbackElement = dialogElement.findElementById("#feedback");
        TextRenderer renderer = feedbackElement.getRenderer(TextRenderer.class);
        renderer.setText(feedbackMessage);
    }
    // *************************************************************************
    // private methods

    /**
     * Test whether an asset exists at the specified path.
     *
     * @param assetPath (not null)
     * @return true if one exists, otherwise false
     */
    private boolean exists(String assetPath) {
        assert assetPath != null;

        AssetKey key = new AssetKey(assetPath);
        /*
         * Temporarily hush asset-manager warnings about missing resources.
         */
        Logger damLogger = Logger.getLogger(AssetManager.class.getName());
        Level savedLevel = damLogger.getLevel();
        damLogger.setLevel(Level.SEVERE);

        AssetInfo info = manager.locateAsset(key);
        damLogger.setLevel(savedLevel);

        if (info == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Read the text field.
     *
     * @param dialogElement (not null)
     * @return a text string (not null)
     */
    private String getPath(Element dialogElement) {
        assert dialogElement != null;

        TextField textField = dialogElement.findNiftyControl("#textfield",
                TextField.class);
        String path = textField.getRealText();

        assert path != null;
        return path;
    }

    /**
     * Test whether the specified asset path has one of the required extensions.
     *
     * @param assetPath path to test (not null)
     * @return true if it end with an extension on the list, otherwise false
     */
    private boolean hasExtension(String assetPath) {
        assert assetPath != null;

        boolean match = false;
        for (String ext : extensions) {
            if (assetPath.endsWith(ext)) {
                match = true;
                break;
            }
        }

        return match;
    }

    /**
     * Concatenate the extensions together into a single string.
     *
     * @return a string containing all legal extensions
     */
    private String joinExtensions() {
        StringBuilder buffer = new StringBuilder(40);
        boolean first = true;
        for (String ext : extensions) {
            if (first) {
                first = false;
            } else {
                buffer.append("/");
            }
            buffer.append(ext);
        }
        String result = buffer.toString();

        return result;
    }
}
