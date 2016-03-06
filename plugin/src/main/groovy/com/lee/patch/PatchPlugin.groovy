package com.lee.patch

import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.IOUtils
import org.apache.tools.ant.taskdefs.condition.Os
import org.objectweb.asm.Type

import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

import static com.lee.patch.Utils.*;
import static com.lee.patch.PatchConstants.*;

import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.api.Plugin

class PatchPlugin implements Plugin<Project> {

    PatchExtension patchExtension
    Logger log

    void apply(Project project) {
        project.extensions.create("patch", PatchExtension);

        project.afterEvaluate {
            patchExtension = project.extensions.findByName("patch") as PatchExtension
            log = new Logger(level: patchExtension.logLevel, tag: "PatchPlugin")
            patchExtension.excludeClasses << Type.getType(patchExtension.hackType).getClassName()
            log.i("level:${patchExtension.logLevel}," +
                    "hacktype:${patchExtension.hackType}," +
                    "makeTag:${patchExtension.makeTag}," +
                    "excludeClasses:${Arrays.toString(patchExtension.excludeClasses.toArray())}" +
                    "excludePackages:${Arrays.toString(patchExtension.excludePackages.toArray())}")

            project.android.applicationVariants.each { variant ->
                def variantName = "${variant.name}"
                def variantNameCap = "${variantName.capitalize()}"
                def dexTask
                def proguardTask
                def beforeGradleV1_4 = true
                dexTask = project.tasks.findByName("dex${variantNameCap}")
                if (dexTask) {
                    /**
                     * gradle 1.3 or before
                     */
                    proguardTask = project.tasks.findByName("proguard${variantNameCap}")
                } else {
                    /**
                     * Since gradle v1.4.0-beta2
                     * @see <a href="http://tools.android.com/tech-docs/new-build-system/transform-api"></a>
                     */
                    beforeGradleV1_4 = false
                    dexTask = project.tasks.findByName("transformClassesWithDexFor${variantNameCap}")
                    proguardTask = project.tasks.findByName("transformClassesAndResourcesWithProguardFor${variantNameCap}")
                }
                if (dexTask) {
                    /**
                     * if make patch and mapping file exists, try to apply the mapping file to proguard task
                     */
                    Map<String, String> fileMD5Map = [:]
                    String patchDirStr = "${project.projectDir}/${PATCH_DIR_NAME}/${variantName}"
                    proguardTask?.doFirst {
                        if (!patchExtension.makeTag) {
                            File mapping = FileUtils.getFile(mapToLocalPath("${patchDirStr}/${MAPPING_FILE_NAME}"))
                            if (mapping.exists()) {
                                // Need Test: gradle 1.4 or above may Failed, please check sourceCode to find the api or change to proguard manually
                                if (beforeGradleV1_4) {
                                    proguardTask.applymapping(mapping)
                                    log.d("apply mapping before v1_4 : ${mapping.getAbsolutePath()}")
                                } else {
                                    proguardTask.getTransform().applyMapping(mapping)
                                    log.d("apply mapping after v1_4 : ${mapping.getAbsolutePath()}")
                                }
                            }
                        }
                    }

                    String patchOutputs = "${project.buildDir}/intermediates/${PATCH_DIR_NAME}/${variantName}"
                    dexTask.doFirst {
                        File patchDir = FileUtils.getFile(mapToLocalPath(patchDirStr));
                        if (patchDir.exists()) {
                            if (patchExtension.makeTag) {
                                // delete the old mapping.txt and hash.txt
                                FileUtils.cleanDirectory(patchDir)
                            } else {
                                // parse hash.txt to fileMd5Map
                                File hash = FileUtils.getFile(mapToLocalPath("${patchDirStr}/${HASH_FILE_NAME}"))
                                if (hash.exists()) {
                                    hash.eachLine {
                                        def datas = it.split(HASH_FILE_SEPARATOR)
                                        if (datas.length == 2) {
                                            fileMD5Map.put(datas[0], datas[1])
                                        }
                                    }
                                    log.d("read hash : ${hash.getAbsolutePath()}")
                                }
                            }
                        } else {
                            FileUtils.forceMkdir(patchDir)
                            log.d("create PatchDir : ${patchDirStr}")
                        }

                        Set excludeClasses = []
                        patchExtension.excludeClasses.each {
                            if (it.endsWith(".class")) {
                                excludeClasses << it.substring(0, it.lastIndexOf("."))
                            } else {
                                excludeClasses << it
                            }
                        }
                        File tagMapping = FileUtils.getFile(mapToLocalPath("${project.buildDir}/outputs/mapping/${variant.dirName}/mapping.txt"))
                        if (tagMapping.exists()) {
                            // change the excludeClasses and excludePackages to mapping one
                            tagMapping.eachLine {
                                if (null != it
                                        && it.endsWith(":")
                                        && !it.startsWith("android.support")) {
                                    String[] map = it.split("->");
                                    if (map.size() == 2) {
                                        String className = map[0].trim()
                                        String mapClassName = map[1].substring(0, map[1].lastIndexOf(":")).trim()
                                        if (excludeClasses.contains(className)) {
                                            excludeClasses.remove(className)
                                            excludeClasses << mapClassName
                                        }
                                        String packageName = className.substring(0, className.lastIndexOf("."))
                                        if (patchExtension.excludePackages.contains(packageName)) {
                                            patchExtension.excludePackages.remove(packageName)
                                            patchExtension.excludePackages << mapClassName.substring(0, mapClassName.lastIndexOf("."))
                                        }
                                    }
                                }
                            }

                            // copy mapping.txt to patchDir
                            FileUtils.copyFile(tagMapping, FileUtils.getFile(mapToLocalPath("${patchDirStr}/${MAPPING_FILE_NAME}")))
                            if (patchExtension.makeTag) {
                                log.d("copy mapping : from ${tagMapping.getAbsolutePath()} to ${patchDirStr}/${MAPPING_FILE_NAME}")
                            }
                        }
                        patchExtension.excludeClasses = []
                        excludeClasses.each {
                            patchExtension.excludeClasses << "${it}.class".toString()
                        }
                        log.d("after mapping : excludeClasses:${Arrays.toString(patchExtension.excludeClasses.toArray())}, " +
                                "excludePackages:${Arrays.toString(patchExtension.excludePackages.toArray())}")

                        /**
                         * 1、Hack classes with hackType
                         * 2、If make tag, compute the md5
                         * 3、If make patch, detect the different classes
                         */
                        Set handledClassSet = []
                        if (beforeGradleV1_4) {
                            inputs.files.files.each {
                                def path = it.absolutePath
                                if (path.endsWith(".jar")) {
                                    // 处理jar包
                                    processJar(it, fileMD5Map, patchOutputs, handledClassSet)
                                }
                            }
                            inputs.files.files.each {
                                def path = it.absolutePath
                                // 处理class
                                if (path.endsWith(".class")) {
                                    processClass(variantName, it, fileMD5Map, patchOutputs, handledClassSet)
                                }
                            }
                        } else {
                            Set<File> files = []
                            inputs.files.files.each {
                                listAllFiles(it, files)
                            }
                            files.each {
                                def path = it.absolutePath
                                if (path.endsWith(".jar")) {
                                    // 处理jar包
                                    processJar(it, fileMD5Map, patchOutputs, handledClassSet)
                                }
                            }
                            files.each {
                                def path = it.absolutePath
                                if (path.endsWith(".class")) {
                                    // 处理class
                                    processClass(variantName, it, fileMD5Map, patchOutputs, handledClassSet)
                                }
                            }
                        }

                        if (patchExtension.makeTag) {
                            // storage the hash file
                            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("${patchDirStr}/${HASH_FILE_NAME}")))
                            for (Map.Entry<String, String> entry : fileMD5Map) {
                                writer.writeLine("${entry.getKey()}${HASH_FILE_SEPARATOR}${entry.getValue()}")
                            }
                            closeSafely(writer)
                            log.d("storage hash file : ${patchDirStr}/${HASH_FILE_NAME}")
                        }
                    }

                    if (!patchExtension.makeTag) {
                        // if make patch, dex the different classes
                        dexTask.doLast {
                            File patchDir = FileUtils.getFile(mapToLocalPath(patchOutputs))
                            if (patchDir.exists() && patchDir.listFiles().size()) {
                                def sdkDir
                                Properties properties = new Properties()
                                File localProps = project.rootProject.file("local.properties")
                                if (localProps.exists()) {
                                    properties.load(localProps.newDataInputStream())
                                    sdkDir = properties.getProperty("sdk.dir")
                                } else {
                                    sdkDir = System.getenv("ANDROID_HOME")
                                }
                                if (sdkDir) {
                                    String cmd = "${sdkDir}/build-tools/${project.android.buildToolsVersion}/dx${Os.isFamily(Os.FAMILY_WINDOWS) ? '.bat' : ''}"
                                    File outDir = FileUtils.getFile(mapToLocalPath("${project.buildDir}/outputs/${PATCH_DIR_NAME}/${variantName}"))
                                    if (!outDir.exists()) {
                                        FileUtils.forceMkdir(outDir)
                                    }
                                    OutputStream stdout = new ByteArrayOutputStream()
                                    project.exec {
                                        commandLine "${cmd}",
                                                '--dex',
                                                "--output=${mapToLocalPath("${outDir.absolutePath}/${PATCH_FILE_NAME}")}",
                                                "${patchDir.absolutePath}"
                                        standardOutput = stdout
                                    }
                                    def error = stdout.toString().trim()
                                    if (error) {
                                        println "dex error:" + error
                                    } else {
                                        log.i("patch file : ${outDir.absolutePath}/${PATCH_FILE_NAME}")
                                    }
                                } else {
                                    throw new IllegalArgumentException('$ANDROID_HOME is not defined')
                                }
                            }
                        }
                    }
                } else {
                    log.i("Make 'Tag' or 'Patch' only when build with proguard.")
                }
            }
        }
    }

    private void processJar(File jar, Map<String, String> fileMd5Map, String patchOutputs, Set handledClassSet) {
        if (jar.exists()) {
            try {
                log.d("process jar : ${jar.absolutePath}")
                JarFile jarFile = new JarFile(jar)
                File tempJar = FileUtils.getFile(jar.getParent(), "${jar.getName()}_temp.jar")
                JarOutputStream jo = new JarOutputStream(new FileOutputStream(tempJar))
                Enumeration<JarEntry> entries = jarFile.entries()
                while (entries.hasMoreElements()) {
                    JarEntry originEntry = entries.nextElement()
                    String entryName = originEntry.getName()
                    ZipEntry newEntry = new ZipEntry(entryName)
                    InputStream i = jarFile.getInputStream(originEntry)
                    jo.putNextEntry(newEntry)
                    byte[] buf = null;
                    if (entryName.endsWith(".class")) {
                        String packageName = entryName.substring(0, entryName.lastIndexOf(File.separator))
                        if (!handledClassSet.contains(entryName)
                                && !patchExtension.excludeClasses.contains(entryName.replaceAll(File.separator, "."))
                                && !patchExtension.excludePackages.contains(packageName.replaceAll(File.separator, "."))
                                && !entryName.contains("android/support/")) {
                            buf = hackClass(i, patchExtension.hackType)
                            if (null != buf) {
                                log.d("hack class success : ${entryName}")
                                String md5 = DigestUtils.md5Hex(buf)
                                if (patchExtension.makeTag) {
                                    fileMd5Map.put(entryName, md5)
                                } else {
                                    String tagMd5 = fileMd5Map.get(entryName)
                                    if (!tagMd5 || !tagMd5.equals(md5)) {
                                        log.d("detect patch file : ${entryName}")
                                        File dstFile = FileUtils.getFile(mapToLocalPath("${patchOutputs}/${entryName}"))
                                        if (!dstFile.exists()) {
                                            FileUtils.forceMkdir(FileUtils.getFile(dstFile.getParent()))
                                            dstFile.createNewFile()
                                        }
                                        FileOutputStream co = new FileOutputStream(dstFile)
                                        co.write(buf)
                                        closeSafely(co)
                                    }
                                }
                                handledClassSet << entryName
                            } else {
                                log.d("hack class fail : ${entryName}")
                                closeSafely(i)
                                i = jarFile.getInputStream(originEntry)
                                buf = IOUtils.toByteArray(i)
                            }
                        } else {
                            buf = IOUtils.toByteArray(i)
                        }
                    } else {
                        buf = IOUtils.toByteArray(i)
                    }
                    jo.write(buf)
                    jo.closeEntry()
                    closeSafely(i)
                }
                closeSafely(jo)
                jarFile.close()

                if (jar.exists()) {
                    jar.delete()
                }
                tempJar.renameTo(jar)
            } catch (Exception e) {
                log.d(e)
            }
        }
    }

    private void processClass(String variantName, File classFile, Map<String, String> fileMd5Map, String patchOutputs, Set handledClassSet) {
        if (classFile.exists()) {
            try {
                String path = classFile.absolutePath
                String entryName = path.substring(path.indexOf(variantName) + variantName.length() + 1, path.length())
                String packageName = entryName.substring(0, entryName.lastIndexOf(File.separator))
                String className = classFile.name
                byte[] buf = null;
                InputStream i = new FileInputStream(classFile)
                if (!handledClassSet.contains(entryName)
                        && !entryName.contains("android/support/")
                        && !patchExtension.excludeClasses.contains(entryName.replaceAll(File.separator, "."))
                        && !patchExtension.excludePackages.contains(packageName.replaceAll(File.separator, "."))
                        && !className.startsWith("R")
                        && !className.startsWith("BuildConfig")) {
                    buf = hackClass(i, patchExtension.hackType)
                    if (null != buf) {
                        log.d("hack class success : ${entryName}")
                        String md5 = DigestUtils.md5Hex(buf)
                        if (patchExtension.makeTag) {
                            fileMd5Map.put(entryName, md5)
                        } else {
                            String tagMd5 = fileMd5Map.get(entryName)
                            if (!tagMd5 || !tagMd5.equals(md5)) {
                                log.d("detect patch file : ${entryName}")
                                File dstFile = FileUtils.getFile(mapToLocalPath("${patchOutputs}/${entryName}"))
                                if (!dstFile.exists()) {
                                    FileUtils.forceMkdir(FileUtils.getFile(dstFile.getParent()))
                                    dstFile.createNewFile()
                                }
                                FileOutputStream co = new FileOutputStream(dstFile)
                                co.write(buf)
                                closeSafely(co)
                            }
                        }
                        handledClassSet << entryName
                    } else {
                        log.d("hack class fail : ${entryName}(size=${buf?.size()})")
                        closeSafely(i)
                        i = new FileInputStream(classFile)
                        buf = IOUtils.toByteArray(i)
                    }
                } else {
                    buf = IOUtils.toByteArray(i)
                }
                File tempFile = FileUtils.getFile(classFile.getParent(), UUID.randomUUID().toString() + ".tmp")
                FileOutputStream o = new FileOutputStream(tempFile)
                o.write(buf)
                closeSafely(i)
                closeSafely(o)
                if (classFile.exists()) {
                    classFile.delete()
                }
                tempFile.renameTo(classFile)
            } catch (Exception e) {
                log.d(e)
            }
        }
    }
}
