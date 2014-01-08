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

import com.laytonsmith.PureUtilities.ClassLoading.DynamicClassLoader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
