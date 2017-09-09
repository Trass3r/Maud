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

import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import com.jme3.asset.ModelKey;
import com.jme3.scene.Spatial;
import com.jme3.scene.plugins.bvh.BVHAnimData;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MySpatial;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.ui.Locators;
import maud.CheckLoaded;
import maud.Maud;
import maud.Util;
import maud.model.option.SceneBones;

/**
 * MVC model for a computer-graphics (CG) model load slot in the Maud
 * application: keeps track of where it was loaded from, and provides access to
 * related MVC model state.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class LoadedCgm extends Cgm {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            LoadedCgm.class.getName());
    // *************************************************************************
    // fields

    /**
     * absolute filesystem path to asset location, or "" if unknown, or null if
     * no CG model loaded
     */
    protected String assetLocation = null;
    /**
     * asset path less extension, or "" if unknown, or null if no CG model
     * loaded
     */
    protected String baseAssetPath = null;
    /**
     * extension of the asset path, or "" if unknown, or null if no CG model
     * loaded
     */
    protected String extension = null;
    /**
     * name of the CG model, or null if no CG model loaded
     */
    protected String name = null;
    // *************************************************************************
    // new methods exposed

    /**
     * Read the asset location of the loaded CG model.
     *
     * @return absolute filesystem path, or "" if not known (not null)
     */
    public String getAssetLocation() {
        assert assetLocation != null;
        return assetLocation;
    }

    /**
     * Read the asset path of the loaded CG model, less extension.
     *
     * @return base path, or "" if not known (not null)
     */
    public String getAssetPath() {
        assert baseAssetPath != null;
        return baseAssetPath;
    }

    /**
     * Read the extension of the loaded CG model.
     *
     * @return extension (not null)
     */
    public String getExtension() {
        assert extension != null;
        return extension;
    }

    /**
     * Read the name of the loaded CG model.
     *
     * @return name, or "" if not known (not null)
     */
    public String getName() {
        assert name != null;
        return name;
    }

    /**
     * Unload the loaded CG model, if any, and load from the specified asset in
     * the specified location.
     *
     * @param rootPath absolute filesystem path to the asset
     * directory/folder/JAR/ZIP (not null, not empty)
     * @param assetPath path to the asset to load (not null, not empty)
     * @return true if successful, otherwise false
     */
    public boolean loadAsset(String rootPath, String assetPath) {
        Validate.nonEmpty(rootPath, "root path");
        Validate.nonEmpty(assetPath, "asset path");

        Locators.save();
        Locators.useFilesystem(rootPath);
        boolean diagnose = Maud.getModel().getMisc().getDiagnoseLoads();
        Spatial loaded = loadFromAsset(assetPath, false, diagnose);
        Locators.restore();

        if (loaded == null) {
            return false;
        } else {
            assetLocation = rootPath;
            postLoad(loaded);
            return true;
        }
    }

    /**
     * Unload the current CG model, if any, and load the named one (typically
     * from the classpath).
     *
     * @param cgmName which CG model to load (not null, not empty)
     * @return true if successful, otherwise false
     */
    public boolean loadNamed(String cgmName) {
        String folderName = cgmName;
        String fileName;
        switch (cgmName) {
            case "Boat":
                fileName = "boat.j3o";
                break;
            case "Buggy":
                fileName = "Buggy.j3o";
                break;
            case "Elephant":
                fileName = "Elephant.mesh.xml";
                break;
            case "Ferrari":
                fileName = "Car.scene";
                break;
            case "HoverTank":
                fileName = "Tank2.mesh.xml";
                break;
            case "Jaime":
                fileName = "Jaime.j3o";
                break;
            case "MhGame":
                fileName = "MhGame.mesh.xml";
                break;
            case "MonkeyHead":
                fileName = "MonkeyHead.mesh.xml";
                break;
            case "Ninja":
                fileName = "Ninja.mesh.xml";
                break;
            case "Oto":
                fileName = "Oto.mesh.xml";
                break;
            case "Puppet":
                fileName = "Puppet.xbuf";
                break;
            case "Sign Post":
                fileName = "Sign Post.mesh.xml";
                break;
            case "Sword":
                fileName = "Sword.mesh.xml";
                folderName = "Sinbad";
                break;
            case "Sinbad":
                fileName = "Sinbad.mesh.xml";
                break;
            case "SpaceCraft":
                fileName = "Rocket.mesh.xml";
                break;
            case "Teapot":
                fileName = "Teapot.obj";
                break;
            case "Tree":
                fileName = "Tree.mesh.xml";
                break;

            default:
                String message = String.format("unknown asset name: %s",
                        MyString.quote(cgmName));
                throw new IllegalArgumentException(message);
        }

        String assetPath = String.format("Models/%s/%s", folderName, fileName);
        boolean diagnose = Maud.getModel().getMisc().getDiagnoseLoads();
        Spatial loaded = loadFromAsset(assetPath, false, diagnose);
        if (loaded == null) {
            return false;
        } else {
            name = cgmName;
            postLoad(loaded);
            return true;
        }
    }
    // *************************************************************************
    // protected methods

    /**
     * Invoked after successfully loading a CG model.
     *
     * @param cgmRoot the newly loaded CGM (not null)
     */
    protected void postLoad(Spatial cgmRoot) {
        assert cgmRoot != null;

        CheckLoaded.cgm(cgmRoot);
        rootSpatial = cgmRoot.clone();
        getSceneView().loadCgm(cgmRoot);
        updateSceneWireframe();
        /*
         * Reset the selected bone/spatial and also the loaded animation.
         */
        getBone().deselect();
        getSpatial().postLoad();
        getAnimation().loadBindPose();

        if (countAnimations() == 1) {
            List<String> names = listRealAnimationsSorted();
            String animationName = names.get(0);
            getAnimation().load(animationName);
        }

        SceneBones sceneBones;
        if (MySpatial.countVertices(cgmRoot) == 0) {
            sceneBones = SceneBones.All;
        } else {
            sceneBones = SceneBones.InfluencersOnly;
        }
        Maud.getModel().getScene().getSkeleton().setBones(sceneBones);
    }
    // *************************************************************************
    // Cgm methods

    /**
     * Create a deep copy of this object.
     *
     * @return a new object, equivalent to this one
     * @throws CloneNotSupportedException if a field isn't cloneable
     */
    @Override
    public LoadedCgm clone() throws CloneNotSupportedException {
        LoadedCgm clone = (LoadedCgm) super.clone();
        return clone;
    }

    /**
     * Unload the CG model.
     */
    public void unload() {
        LoadedCgm target = Maud.getModel().getTarget();
        assert this != target; // not allowed to unload target

        assetLocation = null;
        baseAssetPath = null;
        extension = null;
        name = null;

        super.unload();
    }
    // *************************************************************************
    // private methods

    /**
     * Quietly load a CG model asset from persistent storage without adding it
     * to the scene. If successful, set {@link #baseAssetPath}.
     *
     * @param assetPath (not null)
     * @param useCache true to look in the asset manager's cache, false to force
     * a fresh load from persistent storage
     * @param diagnose true&rarr;messages to console, false&rarr;no messages
     * @return an orphaned spatial, or null if the asset had errors
     */
    private Spatial loadFromAsset(String assetPath, boolean useCache,
            boolean diagnose) {
        AssetManager assetManager = Locators.getAssetManager();
        Locators.save();
        /*
         * Load the CG model.
         */
        String ext;
        Spatial loaded;
        if (assetPath.endsWith(".bvh")) {
            AssetKey<BVHAnimData> key = new AssetKey<>(assetPath);
            ext = key.getExtension();
            if (!useCache) {
                /*
                 * Delete the key from the asset manager's cache in order
                 * to force a fresh load from persistent storage.
                 */
                assetManager.deleteFromCache(key);
            }
            loaded = Util.loadBvhAsset(assetManager, key, diagnose);

        } else {
            ModelKey key = new ModelKey(assetPath);
            ext = key.getExtension();
            if (!useCache) {
                /*
                 * Delete the key from the asset manager's cache in order
                 * to force a fresh load from persistent storage.
                 */
                assetManager.deleteFromCache(key);
            }
            Locators.registerDefault();
            List<String> assetFolders;
            assetFolders = Maud.getModel().getLocations().listAll();
            Locators.register(assetFolders);

            loaded = Util.loadCgmAsset(assetManager, key, diagnose);
        }
        if (loaded == null) {
            logger.log(Level.SEVERE, "Failed to load model from asset {0}",
                    MyString.quote(assetPath));
        } else {
            logger.log(Level.INFO, "Loaded model from asset {0}",
                    MyString.quote(assetPath));

            if (this == Maud.getModel().getTarget() && isLoaded()) {
                History.autoAdd();
            }
            extension = ext;
            int extLength = extension.length();
            if (extLength == 0) {
                baseAssetPath = assetPath;
            } else {
                int pathLength = assetPath.length() - extLength - 1;
                baseAssetPath = assetPath.substring(0, pathLength);
            }
            assetLocation = Locators.getRootPath();
            name = loaded.getName();
        }

        Locators.restore();
        return loaded;
    }
}
