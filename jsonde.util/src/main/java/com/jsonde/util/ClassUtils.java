package com.jsonde.util;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Commenti Javadoc
 * @author gabriele
 *
 */
public class ClassUtils {

	/**
	 * Stringa CONSTRUCTOR_METHOD_NAME
	 */
    public static final String CONSTRUCTOR_METHOD_NAME = "<init>";
    
    /**
     * String STATIC_CONSTRUCTOR_METHOD_NAME
     */
    public static final String STATIC_CONSTRUCTOR_METHOD_NAME = "<clinit>";
    private static final String EMPTY_STRING = "";

    public static String getFullyQualifiedName(String className) {
        if (null == className) return EMPTY_STRING;
        return className.replace('/', '.');
    }

    public static String getInternalClassName(String className) {
        if (null == className) return EMPTY_STRING;
        return className.replace('.', '/');
    }

    public static String convertClassNameToResourceName(String className) {
        if (null == className) return EMPTY_STRING;
        return className.replace('.', '/') + ".class";
    }

    public static Set<String> getPackagesFromClassPath() throws IOException {

        String classPath = System.getProperty("java.class.path");
        String pathSeparator = System.getProperty("path.separator");

        Set<String> packages = new HashSet<String>();
        
        File classPathFile = null;
        JarFile jarFile = null;
        return packages;

    }

    public static Set<String> getPackagesFromDirectory(File directory) {
        return getPackagesFromDirectory(directory, directory);
    }

    private static Set<String> getPackagesFromDirectory(File rootDirectory, File directory) {

        Set<String> packages = new HashSet<String>();

        String rootDirectoryFileName = rootDirectory.getAbsolutePath();

        for (File file : directory.listFiles()) {

            if (file.isDirectory()) {
                packages.addAll(getPackagesFromDirectory(rootDirectory, file));
            } else if (file.getName().endsWith(".class")) {

                String directoryFileName = directory.getAbsolutePath();

                String packageName =
                        directoryFileName.
                                substring(rootDirectoryFileName.length() + 1).
                                replaceAll("/", ".");

                packages.add(packageName);

            }

        }

        return packages;

    }

}
