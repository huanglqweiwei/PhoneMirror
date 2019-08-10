package com.hlq.mobile.utils;

import com.android.ddmlib.*;

import java.io.*;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Tools {
    private static final String TAG = "Tools";
    public static final ExecutorService sExecutor = Executors.newCachedThreadPool();
    private static String LOCAL_PATH = null;

    public static final String LIBS_PATH_NAME = "phoneMirrorLibs";
    public static final String SINGLETOUCH_APK = "singletouch.apk";

    public static String getLocalPath() {
        if (LOCAL_PATH != null) {
            return LOCAL_PATH;
        }
        String userDir = System.getProperty("user.dir");
        if (userDir != null) {
            if (new File(userDir,"phone-mirror.jar").exists()) {
                LOCAL_PATH = userDir;
                return userDir;
            }
        }
        URL url = Tools.class.getProtectionDomain().getCodeSource().getLocation();
        String filePath = null;
        try {
            filePath = URLDecoder.decode(url.getPath(), "utf-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (filePath != null) {
            filePath = new File(filePath).getAbsolutePath();
            if (filePath.endsWith(".jar")) {
                filePath = filePath.substring(0, filePath.lastIndexOf(File.separator));
            } else {
                int endIndex = filePath.indexOf("PhoneMirror");
                if (endIndex >= 0) {
                    filePath = filePath.substring(0, endIndex + 11);
                }
            }
        }
        LOCAL_PATH = filePath;
        return filePath;
    }

    public static boolean isEmpty(String src) {
        return src == null || src.trim().length() == 0;
    }

    public static String executeShellCommand(IDevice device, String cmd) {
        CollectingOutputReceiver receiver = new CollectingOutputReceiver();
        try {
            device.executeShellCommand(cmd, receiver,10000);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.d(TAG, "executeShellCommand : " + cmd );
        return receiver.getOutput();
    }

    public static String getAdbPath(){
        String userDir = getLocalPath();
        if (userDir != null) {
            Properties config = getConfigProperties(userDir);
            if (config != null) {
                String adbPath = config.getProperty("adb.path");
                if (adbPath != null && !adbPath.isEmpty()) {
                    return adbPath;
                }
            }
        }
        Log.d(TAG,"*****************************\nadb路径默认使用jar包同路径下的 ：android_tools/bradb.exe\n" +
                "自定义配置方法：同路径下创建config.properties文件，配置属性：adb.path=adb绝对路径\n*********************************");
        String os = System.getProperty("os.name");
        Log.d(TAG,"os = " + os);
        //TODO:linux
        String adb = "bradb.exe";
        if (os != null && os.startsWith("Mac")) {
            adb = "adb";
        }
        return userDir + File.separator + "android_tools" + File.separator + adb;
    }

    private static Properties getConfigProperties(String userDir) {
        File propFile = new File(userDir, "config.properties");
        if (propFile.exists()) {
            FileInputStream inStream = null;
            try {
                inStream = new FileInputStream(propFile);
                Properties properties = new Properties();
                properties.load(inStream);
                return properties;
            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                if (inStream != null) {
                    try {
                        inStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return null;
    }

    public static String getMinicapLibPath(){
        return getLocalPath() + File.separator + LIBS_PATH_NAME + File.separator + "minicapLib" + File.separator;
    }

    public static void unzipMinicapLib(){
        unzipResLib("minicapLib.zip");
    }

    public static void unzipResLib(String resName){
        String userDir = getLocalPath();
        Log.d(TAG,"unzipResLib : " + userDir + ",resName = " + resName);
        InputStream inputStream = Tools.class.getClassLoader().getResourceAsStream(resName);
        if (inputStream != null) {
            File libFile = new File(userDir, resName);
            copyFile(inputStream,libFile);
            String phoneMirrorLibsPath = userDir + File.separator + LIBS_PATH_NAME;
            ZipFile zipFile = null;
            try {
                zipFile = new ZipFile(libFile);
                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                File file;
                while (entries.hasMoreElements()){
                    ZipEntry entry = entries.nextElement();
                    file = new File(phoneMirrorLibsPath, entry.getName());
                    if (entry.isDirectory()) {
                        if (!file.isDirectory()) {
                            file.mkdirs();
                        }
                    } else {
                        copyFile(zipFile.getInputStream(entry),file);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                if (zipFile != null) {
                    try {
                        zipFile.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (libFile.exists()) {
                    libFile.delete();
                }
            }
        }
    }

    public static void copySingleTouchApk(File apkFile){
        InputStream inputStream = Tools.class.getClassLoader().getResourceAsStream(SINGLETOUCH_APK);
        if (inputStream != null) {
            File dir = apkFile.getParentFile();
            if (dir != null && !dir.exists()) {
                dir.mkdir();
            }
            copyFile(inputStream,apkFile);
        }
    }

    public static void copyFile(InputStream inputStream,File outFile){
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(outFile);
            int len;
            byte[] bytes = new byte[8092];
            while ((len = inputStream.read(bytes)) != -1){
                out.write(bytes,0,len);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


}
