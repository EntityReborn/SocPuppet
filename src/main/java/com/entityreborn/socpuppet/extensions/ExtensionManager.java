package com.entityreborn.socpuppet.extensions;

import com.entityreborn.socbot.events.PrivmsgEvent;
import com.entityreborn.socpuppet.extensions.annotations.SocBotPlugin;
import com.entityreborn.socpuppet.extensions.annotations.Trigger;
import com.laytonsmith.PureUtilities.ClassLoading.ClassDiscovery;
import com.laytonsmith.PureUtilities.ClassLoading.ClassMirror.ClassMirror;
import com.laytonsmith.PureUtilities.ClassLoading.DynamicClassLoader;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Layton
 */
public class ExtensionManager {

    private static final Map<URL, ExtensionTracker> trackers = new HashMap<>();
    private static final DynamicClassLoader dcl = new DynamicClassLoader();
    private static final List<File> locations = new ArrayList<>();

    /**
     * Initializes the extension manager. This operation is not necessarily
     * required, and must be guaranteed to not run more than once per
     * ClassDiscovery object.
     *
     * @param cd the ClassDiscovery to use for loading files.
     */
    public static void Initialize(ClassDiscovery cd) {
        //Look in the given locations for jars, add them to our class discovery, then initialize everything
        List<File> toProcess = new ArrayList<>();
        
        for (File location : locations) {
            if (location.isDirectory()) {
                for (File f : location.listFiles()) {
                    if (f.getName().endsWith(".jar")) {
                        try {
                            toProcess.add(f.getCanonicalFile());
                        } catch (IOException ex) {
                            Logger.getLogger(ExtensionManager.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            } else if (location.getName().endsWith(".jar")) {
                try {
                    toProcess.add(location.getCanonicalFile());
                } catch (IOException ex) {
                    Logger.getLogger(ExtensionManager.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

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
            
            //First, load it with our custom class loader
            dcl.addJar(jar);
            cd.addDiscoveryLocation(jar);

            System.out.println("Loaded " + file.getAbsolutePath());
        }

        for (ClassMirror<AbstractExtension> extmirror : cd.getClassesWithAnnotationThatExtend(SocBotPlugin.class, AbstractExtension.class)) {
            Extension plugin;

            Class<AbstractExtension> extcls = extmirror.loadClass(dcl, true);
            URL url = ClassDiscovery.GetClassContainer(extcls);
            System.out.println("URL: " + url);
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
            System.out.println("Loaded " + trig.name() + " from " + trig.plugin());
        }

        cd.setDefaultClassLoader(dcl);
    }

    public static Map<URL, ExtensionTracker> getTrackers() {
        return Collections.unmodifiableMap(trackers);
    }

    public static void Startup() {
        for (ExtensionTracker tracker : trackers.values()) {
            tracker.startup();
        }
    }

    public static void Shutdown() {
        for (ExtensionTracker tracker : trackers.values()) {
            tracker.shutdown();
        }
    }

    public static void AddDiscoveryLocation(File file) {
        try {
            locations.add(file.getCanonicalFile());
        } catch (IOException ex) {
            Logger.getLogger(ExtensionManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
