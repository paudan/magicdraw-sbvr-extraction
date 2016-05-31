package org.ktu.transformations.uml2sbvr.transform;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import vepsem.ChildFirstClassLoader;
import vepsem.PluginUtils;

public class CustomClassLoader {

    private static List<URL> urls = new ArrayList<>();
    private static ClassLoader loader = null;

    public static ClassLoader getClassLoader() {
        File QVT_dir = new File(PluginUtils.getInstance().getPluginLibDir());
        File EMFECORE_dir = new File(PluginUtils.getInstance().getPluginLibDir() + "EMFECORE/");
        File EMFUML_dir = new File(PluginUtils.getInstance().getPluginLibDir() + "EMFUML/");
        addJar(QVT_dir);
        addJar(EMFECORE_dir);
        addJar(EMFUML_dir);
        loader = new ChildFirstClassLoader(urls.toArray(new URL[]{}));
        return loader;
    }

    public static void addJar(File dir) {

        try {
            ArrayList<File> files = new ArrayList<>(Arrays.asList(dir.listFiles(new FilenameFilter() {
                        public boolean accept(File dir, String filename) {
                            return filename.endsWith(".jar");
                        }
                    })));
            for (File file : files)
                urls.add(file.toURI().toURL());

        } catch (MalformedURLException e) {
            Logger.getLogger(CustomClassLoader.class.getName()).log(Level.SEVERE, null, e);
        }

    }

    public static Boolean executeTransformation(String prjInput, String mDirection) {
        try {
            Class<?> myClass = getClassLoader().loadClass("org.ktu.transformations.uml2sbvr.transform.TransformationEngine");
            Method mainMethod = myClass.getMethod("transform", String.class, String.class);
            return (Boolean) mainMethod.invoke(null, new Object[]{prjInput, mDirection});
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | ClassNotFoundException ex) {
            Logger.getLogger(CustomClassLoader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

}
