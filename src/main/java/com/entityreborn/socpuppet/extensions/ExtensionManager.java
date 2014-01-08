package com.entityreborn.socpuppet.extensions;

import com.entityreborn.socpuppet.extensions.annotations.SocBotPlugin;
import com.entityreborn.socpuppet.extensions.annotations.Trigger;
import com.laytonsmith.PureUtilities.ClassLoading.ClassDiscovery;
import com.laytonsmith.PureUtilities.ClassLoading.ClassMirror.AnnotationMirror;
import com.laytonsmith.PureUtilities.ClassLoading.ClassMirror.ClassMirror;
import com.laytonsmith.PureUtilities.ClassLoading.DynamicClassLoader;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
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

    private static final Map<URL, ExtensionTracker> trackers = new HashMap<>();
    private static final List<File> locations = new ArrayList<>();
    private static File extCache;
    
    public static void Cache(ClassDiscovery cd, File extcache) {
        cd.addDiscoveryLocation(ClassDiscovery.GetClassContainer(ExtensionManager.class));
        DynamicClassLoader dcl = new DynamicClassLoader();
        extCache = extcache;
        
        //Look in the given locations for jars, add them to our class discovery.
        List<File> toProcess = new ArrayList<>();
        
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
        
        // Begin caching
        extcache.mkdirs();
        
        // Loop thru the found lifecycles, copy them to the cache using the name
        // given in the lifecycle. If more than one jar has the same internal
        // name, the filename will be given a number.
        
        Set<File> done = new HashSet<>();
        Map<String, Integer> namecount = new HashMap<>();
        
        for (ClassMirror<AbstractExtension> extmirror : 
                cd.getClassesWithAnnotationThatExtend(
                        SocBotPlugin.class, AbstractExtension.class)) {
            AnnotationMirror plug = extmirror.getAnnotation(SocBotPlugin.class);
            
            URL plugURL = extmirror.getContainer();
            
            if (plugURL != null && plugURL.getPath().endsWith(".jar")) {
                String name = plug.getValue("value").toString();
                File f;
                
                try {
                    f = new File(plugURL.toURI());
                } catch (URISyntaxException ex) {
                    Logger.getLogger(ExtensionCache.class.getName()).log(
                            Level.SEVERE, null, ex);
                    continue;
                }
                
                if (done.contains(f)) {
                    continue;
                }
                
                done.add(f);
                
                // Just in case we have two plugins with the same internal name,
                // lets track and rename them using a number scheme.
                if (namecount.containsKey(name.toLowerCase())) {
                    int i = namecount.get(name.toLowerCase());
                    name += "-" + i;
                    namecount.put(name.toLowerCase(), i++);
                } else {
                    namecount.put(name.toLowerCase(), 1);
                }
                
                // Rename the jar to use the plugin's internal name and 
                // copy it into the cache.
                File newFile = new File(extcache, name.toLowerCase() + ".jar");
                
                try {
                    Files.copy(f.toPath(), newFile.toPath(), REPLACE_EXISTING);
                    newFile.deleteOnExit();
                } catch (IOException ex) {
                    Logger.getLogger(ExtensionManager.class.getName()).log(
                        Level.SEVERE, "Could not copy '" + f.getName()
                                + "' to cache: " + ex.getMessage());
                    continue;
                }
            }
        }
        
        // Shut down the original dcl to "unlock" the processed jars.
        dcl.destroy();
    }
    
    private static List<File> getFiles(File location) {
        List<File> toProcess = new ArrayList<>();
        
        if (location.isDirectory()) {
            for (File f : location.listFiles()) {
                if (f.getName().endsWith(".jar")) {
                    try {
                        toProcess.add(f.getCanonicalFile());
                    } catch (IOException ex) {
                        Logger.getLogger(ExtensionManager.class.getName()).log(
                                Level.SEVERE, null, ex);
                    }
                }
            }
        } else if (location.getName().endsWith(".jar")) {
            try {
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
     * @param extcache
     */
    public static void Initialize(ClassDiscovery cd) {
        //Look in the given locations for jars, add them to our class discovery,
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

        for (ClassMirror<AbstractExtension> extmirror : 
                cd.getClassesWithAnnotationThatExtend(
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

            if (!trackers.containsKey(url)) {
                trackers.put(url, new ExtensionTracker(url, cd, dcl));
            }

            trackers.get(url).addPlugin(plugin);
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

            if (!trackers.containsKey(url)) {
                trackers.put(url, new ExtensionTracker(url, cd, dcl));
            }

            trackers.get(url).addTrigger(trig);
        }
    }
    
    public static ExtensionTracker getTrackerByName(String name) {
        for (ExtensionTracker trk : trackers.values()) {
            if (trk.getIdentifier().equalsIgnoreCase(name)) {
                return trk;
            }
        }
        
        return null;
    }

    public static Map<URL, ExtensionTracker> getTrackers() {
        return Collections.unmodifiableMap(trackers);
    }

    public static void Startup() {
        for (ExtensionTracker tracker : trackers.values()) {
            tracker.startup();
        }
    }
    
    public static void Reload() {
        
    }

    public static void Shutdown() {
        for (ExtensionTracker tracker : trackers.values()) {
            tracker.shutdown();
        }
        
        // Actually unload extension code from memory.
        for (ExtensionTracker tracker : trackers.values()) {
            tracker.unload();
        }
        
        trackers.clear();
        
        System.gc();
    }

    public static void AddDiscoveryLocation(File file) {
        try {
            locations.add(file.getCanonicalFile());
        } catch (IOException ex) {
            Logger.getLogger(ExtensionManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static void Unload(String extension) {
        ExtensionTracker trk = getTrackerByName(extension);
        
        if (trk == null) {
            return;
        }
        
        trk.unload();
        
        trackers.remove(trk.getLocation());
        System.gc();
    }
}
