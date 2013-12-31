package com.entityreborn.socpuppet.extensions.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Trigger {

    /**
     * The name of the extension.
     *
     * @return String
     */
    String value();
}
