/*
 * The MIT License
 *
 * Copyright 2013 Jason Unger <entityreborn@gmail.com>.
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

import com.entityreborn.socbot.events.PrivmsgEvent;
import com.entityreborn.socpuppet.extensions.annotations.Trigger;
import com.laytonsmith.PureUtilities.ClassLoading.ClassDiscovery;
import java.lang.annotation.Annotation;
import java.net.URL;

/**
 *
 * @author Jason Unger <entityreborn@gmail.com>
 */
public abstract class AbstractTrigger {

    public abstract void exec(PrivmsgEvent event, String trigger, String args);

    public String name() {
        for (Annotation a : getClass().getAnnotations()) {
            if (a instanceof Trigger) {
                Trigger e = (Trigger) a;

                return e.value();
            }
        }

        return "<unknown>";
    }

    public String plugin() {
        URL url = ClassDiscovery.GetClassContainer(getClass());

        ExtensionTracker tracker = ExtensionManager.getTrackers().get(url);
        if (tracker != null) {
            return tracker.getIdentifier();
        }

        return "<unknown>";
    }

    public abstract String docs();
}
