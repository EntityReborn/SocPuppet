/*
 * The MIT License
 *
 * Copyright 2014 Jason Unger <entityreborn@gmail.com>.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.entityreborn.socpuppet.extensions;

import com.entityreborn.socpuppet.extensions.annotations.SocBotPlugin;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class that facilitates copying extensions from one location to another
 * in the interest of loading from a cache, to get around Windows locking
 * files when loaded. This will probably not be used on Linux hosts.
 * 
 * @author Jason Unger <entityreborn@gmail.com>
 */
public class ExtensionCache {
    private final List<File> locations = new ArrayList<>();
    private final DynamicClassLoader dcl = new DynamicClassLoader();
    
    public void update(ClassDiscovery cd, File extcache) {
        //Look in the given locations for jars, add them to our class discovery.
        List<File> toProcess = new ArrayList<>();
        
        // Allow for a location to point to a directory /or/ a file.
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
        
        for (ClassMirror<AbstractExtension> extmirror : cd.getClassesWithAnnotationThatExtend(SocBotPlugin.class, AbstractExtension.class)) {
            AnnotationMirror plug = extmirror.getAnnotation(SocBotPlugin.class);
            
            URL plugURL = extmirror.getContainer();
            
            if (plugURL != null && plugURL.getPath().endsWith(".jar")) {
                String name = plug.getValue("value").toString();
                File f;
                
                try {
                    f = new File(plugURL.toURI());
                } catch (URISyntaxException ex) {
                    Logger.getLogger(ExtensionCache.class.getName()).log(Level.SEVERE, null, ex);
                    continue;
                }
                
                if (done.contains(f)) {
                    continue;
                }
                
                done.add(f);
                
                // Just in case we have two plugins with the same internal name,
                // lets track and rename them.
                if (namecount.containsKey(name.toLowerCase())) {
                    int i = namecount.get(name.toLowerCase());
                    name += "-" + i;
                    namecount.put(name.toLowerCase(), i++);
                } else {
                    namecount.put(name.toLowerCase(), 1);
                }
                
                // Rename the jar to use the plugin's internal name.
                File newFile = new File(extcache, name.toLowerCase() + ".jar");
                
                try {
                    Files.copy(f.toPath(), newFile.toPath(), REPLACE_EXISTING);
                    newFile.deleteOnExit();
                } catch (IOException ex) {
                    Logger.getLogger(ExtensionCache.class.getName()).log(Level.SEVERE, null, ex);
                    continue;
                }
            }
        }
    }
    
    public void addUpdateLocation(File file) {
        try {
            locations.add(file.getCanonicalFile());
        } catch (IOException ex) {
            Logger.getLogger(ExtensionManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void cleanup(File extcache) {
        if (extcache == null || !extcache.isDirectory()) {
            return;
        }
        
        for (File f : extcache.listFiles()) {
            f.delete();
        }
    }
}
