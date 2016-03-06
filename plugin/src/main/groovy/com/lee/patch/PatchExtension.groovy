package com.lee.patch

class PatchExtension {
    /**
     * Log Level
     * 0 : Debug
     * 1 : Info
     * 2 : Error
     */
    int logLevel = 1

    /**
     * the classes which never take part in hacking and patching
     * Must contain application class, because we inject hack.apk when application create
     */
    HashSet<String> excludeClasses = []

    /**
     * the classes which never take part in hacking and patching
     * Must contain com.lee.patch, we can not hack the patch lib.
     */
    HashSet<String> excludePackages = []

    /**
     * the class type which will inject to the classes before dex
     * which must be the class in hack.apk
     */
    String hackType

    /**
     * If true, copy mapping file and compute hash file
     * If false, compute classes which is different from the tag, and patch them into patch.apk
     */
    boolean makeTag = false
}