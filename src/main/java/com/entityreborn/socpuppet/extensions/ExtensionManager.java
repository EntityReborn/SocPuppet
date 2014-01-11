package com.entityreborn.socpuppet.extensions;

import com.entityreborn.socpuppet.extensions.annotations.SocBotPlugin;
import com.entityreborn.socpuppet.extensions.annotations.Trigger;
import com.laytonsmith.PureUtilities.ClassLoading.ClassDiscovery;
import com.laytonsmith.PureUtilities.ClassLoading.ClassDiscoveryCache;
import com.laytonsmith.PureUtilities.ClassLoading.ClassMirror.AnnotationMirror;
import com.laytonsmith.PureUtilities.ClassLoading.ClassMirror.ClassMirror;
import com.laytonsmith.PureUtilities.ClassLoading.DynamicClassLoader;
import com.laytonsmith.PureUtilities.Common.OSUtils;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Layton
 */
public class ExtensionManager {
    private static ExtensionManager instance;
    private final Map<URL, ExtensionTracker> extensions = new HashMap<>();
    private final List<File> locations = new ArrayList<>();

    public static ExtensionManager Get() {
        if (instance == null) {
            instance = new ExtensionManager();
        }
        
        return instance;
    }
    
    public void cache(File extCache, File cacheDir) {
        // We will only cache on Windows, as Linux doesn't natively lock
        // files that are in use. Windows prevents any modification, making
        // it harder for server owners on Windows to update the jars.
        boolean onWindows = (OSUtils.GetOS() == OSUtils.OS.WINDOWS);

        if (!onWindows) {
            return;
        }

        // Using System.out here instead of the logger as the logger doesn't
        // immediately print to the console.
        System.out.println("[SocPuppet] Caching extensions...");

        // Create the directory if it doesn't exist.
        extCache.mkdirs();

        // Try to delete any loose files in the cache dir, so that we
        // don't load stuff we aren't supposed to. This is in case the shutdown
        // cleanup wasn't successful on the last run.
        for (File f : extCache.listFiles()) {
            try {
                Files.delete(f.toPath());
            } catch (IOException ex) {
                Logger.getLogger(ExtensionManager.class.getName()).log(Level.WARNING,
                        "[SocPuppet] Could not delete loose file "
                        + f.getAbsolutePath() + ": " + ex.getMessage());
            }
        }

        // The cache, cd and dcl here will just be thrown away.
        // They are only used here for the purposes of discovering what a given 
        // jar has to offer.
        ClassDiscoveryCache cache = new ClassDiscoveryCache(cacheDir);
        DynamicClassLoader dcl = new DynamicClassLoader();
        ClassDiscovery cd = new ClassDiscovery();

        cd.setClassDiscoveryCache(cache);
        cd.addDiscoveryLocation(ClassDiscovery.GetClassContainer(ExtensionManager.class));

        //Look in the given locations for jars, add them to our class discovery.
        List<File> toProcess = new ArrayList<File>();

        for (File location : locations) {
            toProcess.addAll(getFiles(location));
        }

        // Load the files into the discovery mechanism.
        for (File file : toProcess) {
            if (!file.canRead()) {
                continue;
            }

            URL jar;
            try {
                jar = file.toURI().toURL();
            } catch (MalformedURLException ex) {
                Logger.getLogger(ExtensionManager.class.getName()).log(Level.SEVERE, null, ex);
                continue;
            }

            dcl.addJar(jar);
            cd.addDiscoveryLocation(jar);
        }

        cd.setDefaultClassLoader(dcl);

        // Loop thru the found lifecycles, copy them to the cache using the name
        // given in the lifecycle. If more than one jar has the same internal
        // name, the filename will be given a number.
        Set<File> done = new HashSet<>();
        Map<String, Integer> namecount = new HashMap<>();

        // First, cache new lifecycle style extensions. They will be renamed to
        // use their internal name.
        for (ClassMirror<AbstractExtension> extmirror
                : cd.getClassesWithAnnotationThatExtend(
                        SocBotPlugin.class, AbstractExtension.class)) {
            AnnotationMirror plug = extmirror.getAnnotation(SocBotPlugin.class);

            URL plugURL = extmirror.getContainer();

            // Get the internal name that this extension exposes.
            if (plugURL != null && plugURL.getPath().endsWith(".jar")) {
                File f;

                try {
                    f = new File(plugURL.toURI());
                } catch (URISyntaxException ex) {
                    Logger.getLogger(ExtensionManager.class.getName()).log(
                            Level.SEVERE, null, ex);
                    continue;
                }

                // Skip extensions that originate from commandhelpercore.
                if (plugURL.equals(ClassDiscovery.GetClassContainer(ExtensionManager.class))) {
                    done.add(f);
                }

                // Skip files already processed.
                if (done.contains(f)) {
                    Logger.getLogger(ExtensionManager.class.getName()).log(Level.WARNING,
                            f.getAbsolutePath() + " contains more than one extension"
                            + " descriptor. Bug someone about it!");

                    continue;
                }

                done.add(f);

                String name = plug.getValue("value").toString();

                // Just in case we have two plugins with the same internal name,
                // lets track and rename them using a number scheme.
                if (namecount.containsKey(name.toLowerCase())) {
                    int i = namecount.get(name.toLowerCase());
                    name += "-" + i;
                    namecount.put(name.toLowerCase(), i++);

                    Logger.getLogger(ExtensionManager.class.getName()).log(Level.WARNING,
                            f.getAbsolutePath() + " contains a duplicate internally"
                            + " named extension (" + name + "). Bug someone"
                            + " about it!");
                } else {
                    namecount.put(name.toLowerCase(), 1);
                }

                // Rename the jar to use the plugin's internal name and 
                // copy it into the cache.
                File newFile = new File(extCache, name.toLowerCase() + ".jar");

                try {
                    Files.copy(f.toPath(), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException ex) {
                    Logger.getLogger(ExtensionManager.class.getName()).log(
                            Level.SEVERE, "Could not copy '" + f.getName()
                            + "' to cache: " + ex.getMessage());
                }
            }
        }

        System.out.println("[SocPuppet] Extension caching complete.");

        // Shut down the original dcl to "unlock" the processed jars.
        // The cache and cd instances will just fall into oblivion.
        dcl.destroy();

        // Explicit call. Without this, jar files won't actually get unlocked on
        // Windows. Of course, this is hit and miss, but that's fine; we tried.
        System.gc();
    }

    /**
     * Process the given location for any jars. If the location is a jar, add it
     * directly. If the location is a directory, look for jars in it.
     *
     * @param location file or directory
     * @return
     */
    private List<File> getFiles(File location) {
        List<File> toProcess = new ArrayList<>();

        if (location.isDirectory()) {
            for (File f : location.listFiles()) {
                if (f.getName().endsWith(".jar")) {
                    try {
                        // Add the trimmed absolute path.
                        toProcess.add(f.getCanonicalFile());
                    } catch (IOException ex) {
                        Logger.getLogger(ExtensionManager.class.getName()).log(
                                Level.SEVERE, null, ex);
                    }
                }
            }
        } else if (location.getName().endsWith(".jar")) {
            try {
                // Add the trimmed absolute path.
                toProcess.add(location.getCanonicalFile());
            } catch (IOException ex) {
                Logger.getLogger(ExtensionManager.class.getName()).log(
                        Level.SEVERE, null, ex);
            }
        }

        return toProcess;
    }

    /**
     * Initializes the extension manager. This operation is not necessarily
     * required, and must be guaranteed to not run more than once per
     * ClassDiscovery object.
     *
     * @param cd the ClassDiscovery to use for loading files.
     * @param extCache
     */
    public void initialize(File extCache, ClassDiscovery cd) {
        // Look in the given locations for jars, add them to our class discovery,
        // then initialize everything
        DynamicClassLoader dcl = new DynamicClassLoader();
        List<File> toProcess = new ArrayList<>();
        toProcess.addAll(getFiles(extCache));

        for (File file : toProcess) {
            if (!file.canRead()) {
                continue;
            }

            URL jar;
            try {
                jar = file.toURI().toURL();
            } catch (MalformedURLException ex) {
                Logger.getLogger(ExtensionManager.class.getName()).log(
                        Level.SEVERE, null, ex);
                continue;
            }

            //First, load it with our custom class loader
            dcl.addJar(jar);
            cd.addDiscoveryLocation(jar);
        }

        cd.setDefaultClassLoader(dcl);

        for (ClassMirror<AbstractExtension> extmirror
                : cd.getClassesWithAnnotationThatExtend(
                        SocBotPlugin.class, AbstractExtension.class)) {
            Extension plugin;

            Class<? extends AbstractExtension> extcls = extmirror.loadClass(dcl, true);
            URL url = ClassDiscovery.GetClassContainer(extcls);

            try {
                plugin = extcls.newInstance();
            } catch (InstantiationException | IllegalAccessException ex) {
                //Error, but skip this one, don't throw an exception ourselves, just log it.
                Logger.getLogger(ExtensionManager.class.getName()).log(Level.SEVERE,
                        "Could not instantiate " + extcls.getName() + ": " + ex.getMessage());
                continue;
            }

            if (!extensions.containsKey(url)) {
                extensions.put(url, new ExtensionTracker(url, cd, dcl));
            }

            extensions.get(url).addExtension(plugin);
        }

        for (ClassMirror<AbstractTrigger> extmirror : cd.getClassesWithAnnotationThatExtend(Trigger.class, AbstractTrigger.class)) {
            AbstractTrigger trig;

            Class<AbstractTrigger> extcls = extmirror.loadClass(dcl, true);
            URL url = ClassDiscovery.GetClassContainer(extcls);

            try {
                trig = extcls.newInstance();
            } catch (InstantiationException | IllegalAccessException ex) {
                //Error, but skip this one, don't throw an exception ourselves, just log it.
                Logger.getLogger(ExtensionManager.class.getName()).log(Level.SEVERE,
                        "Could not instantiate " + extcls.getName() + ": " + ex.getMessage());
                continue;
            }

            if (!extensions.containsKey(url)) {
                extensions.put(url, new ExtensionTracker(url, cd, dcl));
            }

            extensions.get(url).addTrigger(trig);
        }
    }

    public Map<URL, ExtensionTracker> getTrackers() {
        return Collections.unmodifiableMap(extensions);
    }

    /**
     * Get an extension tracker by name.
     *
     * @param name
     * @return
     */
    public ExtensionTracker getExtensionTracker(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }

        for (ExtensionTracker trk : extensions.values()) {
            if (name.trim().equalsIgnoreCase(trk.identifier.trim())) {
                return trk;
            }
        }

        return null;
    }

    /**
     * Get an extension tracker by URL.
     *
     * @param url
     * @return
     */
    public ExtensionTracker getExtensionTracker(URL url) {
        return extensions.get(url);
    }

    /**
     * Get an extension tracker via class.
     *
     * @param possible
     * @return
     */
    public ExtensionTracker getExtensionTracker(Class possible) {
        URL url = ClassDiscovery.GetClassContainer(possible);

        return getExtensionTracker(url);
    }

    public void addDiscoveryLocation(File file) {
        try {
            locations.add(file.getCanonicalFile());
        } catch (IOException ex) {
            Logger.getLogger(ExtensionManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void startup() {
        for (ExtensionTracker ext : extensions.values()) {
            try {
                ext.startup();
            } catch (Throwable t) {
                Logger.getLogger(ExtensionManager.class.getName()).log(
                        Level.SEVERE, "Error while starting " + ext.getIdentifier(), t);
            }
        }
    }
    
    public void shutdown() {
        for (ExtensionTracker ext : extensions.values()) {
            try {
                ext.shutdown();
            } catch (Throwable t) {
                Logger.getLogger(ExtensionManager.class.getName()).log(
                        Level.SEVERE, "Error while shutting down " + ext.getIdentifier(), t);
            }
        }
    }
}
