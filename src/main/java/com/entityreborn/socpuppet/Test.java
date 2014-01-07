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

package com.entityreborn.socpuppet;

import com.entityreborn.socpuppet.extensions.ExtensionCache;
import com.laytonsmith.PureUtilities.ClassLoading.ClassDiscovery;
import com.laytonsmith.PureUtilities.ClassLoading.ClassDiscoveryCache;
import java.io.File;
import java.net.URL;

/**
 *
 * @author Jason Unger <entityreborn@gmail.com>
 */
public class Test {
    public static void main(String[] args) {
        ClassDiscoveryCache cache = new ClassDiscoveryCache(null);
        ClassDiscovery cd = new ClassDiscovery();
        cd.setClassDiscoveryCache(cache);
        
        URL thisurl = ClassDiscovery.GetClassContainer(Test.class);
        cd.addDiscoveryLocation(thisurl);
        
        File extcache = new File("extcache");
        extcache.mkdirs();
        
        ExtensionCache u = new ExtensionCache();
        u.addUpdateLocation(new File("../SPFactoids/target/SPFactoids-0.0.0-SNAPSHOT.jar"));
        u.addUpdateLocation(new File("../SPGroovy/target/SPGroovy-0.0.0-SNAPSHOT.jar"));
        u.update(cd, extcache);
        u.cleanup(extcache);
    }
}
