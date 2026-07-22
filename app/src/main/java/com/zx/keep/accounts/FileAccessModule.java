package com.zx.keep.accounts;

import android.os.Environment;
import android.util.Base64;
import com.zx.keep.accounts.security.AuditLogger;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

public class FileAccessModule {
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
    private static String allowedRoot;

    private static String getAllowedRoot() {
        if (allowedRoot == null) {
            File extDir = Environment.getExternalStorageDirectory();
            allowedRoot = extDir != null ? extDir.getAbsolutePath() : "/storage/emulated/0";
        }
        return allowedRoot;
    }

    private static final String[] BLOCKED_PREFIXES = {
            "/data/data/", "/data/user/", "/system/", "/proc/",
            "/sys/", "/dev/", "/etc/", "/root/"
    };

    public String listFiles(String path) {
        File file = resolvePath(path);
        if (file == null || !file.exists() || !file.isDirectory()) {
            return jsonError("Directory not found or not accessible: " + path);
        }
        try {
            File[] files = file.listFiles();
            JSONArray jsonArray = new JSONArray();
            if (files != null) {
                for (File f : files) {
                    JSONObject item = new JSONObject();
                    item.put("name", f.getName());
                    item.put("path", f.getAbsolutePath());
                    item.put("type", f.isDirectory() ? "directory" : "file");
                    item.put("size", f.length());
                    item.put("lastModified", dateFormat.format(new Date(f.lastModified())));
                    item.put("readable", f.canRead());
                    item.put("writable", f.canWrite());
                    jsonArray.put(item);
                }
            }
            JSONObject result = new JSONObject();
            result.put("success", true);
            result.put("data", jsonArray);
            return result.toString();
        } catch (Exception e) {
            return jsonError(e.getMessage());
        }
    }

    public String readFile(String path) {
        File file = resolvePath(path);
        if (file == null || !file.exists() || !file.isFile()) {
            return jsonError("File not found or not accessible: " + path);
        }
        AuditLogger.logFileOp("read", path);
        try {
            byte[] bytes = readAllBytes(file);
            JSONObject result = new JSONObject();
            result.put("success", true);
            result.put("data", new String(bytes, "UTF-8"));
            return result.toString();
        } catch (Exception e) {
            return jsonError(e.getMessage());
        }
    }

    public String readBinaryFile(String path) {
        File file = resolvePath(path);
        if (file == null || !file.exists() || !file.isFile()) {
            return jsonError("File not found or not accessible: " + path);
        }
        AuditLogger.logFileOp("readBinary", path);
        try {
            String base64 = Base64.encodeToString(readAllBytes(file), Base64.NO_WRAP);
            JSONObject result = new JSONObject();
            result.put("success", true);
            result.put("data", base64);
            return result.toString();
        } catch (Exception e) {
            return jsonError(e.getMessage());
        }
    }

    public String writeFile(String path, String content) {
        File file = resolvePath(path);
        if (file == null) {
            return jsonError("Path not allowed: " + path);
        }
        AuditLogger.logFileOp("write", path);
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            writeAllBytes(file, content.getBytes("UTF-8"));
            JSONObject result = new JSONObject();
            result.put("success", true);
            result.put("path", file.getAbsolutePath());
            return result.toString();
        } catch (Exception e) {
            return jsonError(e.getMessage());
        }
    }

    public String writeBinaryFile(String path, String base64Data) {
        File file = resolvePath(path);
        if (file == null) {
            return jsonError("Path not allowed: " + path);
        }
        AuditLogger.logFileOp("writeBinary", path);
        try {
            byte[] data = Base64.decode(base64Data, Base64.DEFAULT);
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            writeAllBytes(file, data);
            JSONObject result = new JSONObject();
            result.put("success", true);
            result.put("path", file.getAbsolutePath());
            return result.toString();
        } catch (Exception e) {
            return jsonError(e.getMessage());
        }
    }

    public String deleteFile(String path) {
        File file = resolvePath(path);
        if (file == null || !file.exists()) {
            return jsonError("File not found: " + path);
        }
        AuditLogger.logFileOp("delete", path);
        boolean success = deleteRecursive(file);
        try {
            JSONObject result = new JSONObject();
            result.put("success", success);
            return result.toString();
        } catch (Exception e) {
            return jsonError(e.getMessage());
        }
    }

    public String createDirectory(String path) {
        File file = resolvePath(path);
        if (file == null) {
            return jsonError("Path not allowed: " + path);
        }
        AuditLogger.logFileOp("mkdir", path);
        boolean success = file.mkdirs();
        try {
            JSONObject result = new JSONObject();
            result.put("success", success);
            result.put("path", file.getAbsolutePath());
            return result.toString();
        } catch (Exception e) {
            return jsonError(e.getMessage());
        }
    }

    public String exists(String path) {
        File file = resolvePath(path);
        boolean exists = file != null && file.exists();
        try {
            JSONObject result = new JSONObject();
            result.put("success", true);
            result.put("data", exists);
            return result.toString();
        } catch (Exception e) {
            return jsonError(e.getMessage());
        }
    }

    public String getFileInfo(String path) {
        File file = resolvePath(path);
        if (file == null || !file.exists()) {
            return jsonError("File not found: " + path);
        }
        try {
            JSONObject info = new JSONObject();
            info.put("name", file.getName());
            info.put("path", file.getAbsolutePath());
            info.put("size", file.length());
            info.put("type", file.isDirectory() ? "directory" : "file");
            info.put("lastModified", dateFormat.format(new Date(file.lastModified())));
            info.put("readable", file.canRead());
            info.put("writable", file.canWrite());
            info.put("executable", file.canExecute());
            info.put("hidden", file.isHidden());
            JSONObject result = new JSONObject();
            result.put("success", true);
            result.put("data", info);
            return result.toString();
        } catch (Exception e) {
            return jsonError(e.getMessage());
        }
    }

    public String copyFile(String src, String dst) {
        File srcFile = resolvePath(src);
        File dstFile = resolvePath(dst);
        if (srcFile == null || !srcFile.exists()) {
            return jsonError("Source file not found: " + src);
        }
        if (dstFile == null) {
            return jsonError("Destination path not allowed: " + dst);
        }
        AuditLogger.logFileOp("copy", src + " -> " + dst);
        try {
            File parent = dstFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            copyFileStream(srcFile, dstFile);
            JSONObject result = new JSONObject();
            result.put("success", true);
            return result.toString();
        } catch (Exception e) {
            return jsonError(e.getMessage());
        }
    }

    public String moveFile(String src, String dst) {
        File srcFile = resolvePath(src);
        File dstFile = resolvePath(dst);
        if (srcFile == null || !srcFile.exists()) {
            return jsonError("Source file not found: " + src);
        }
        if (dstFile == null) {
            return jsonError("Destination path not allowed: " + dst);
        }
        AuditLogger.logFileOp("move", src + " -> " + dst);
        try {
            File parent = dstFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            copyFileStream(srcFile, dstFile);
            srcFile.delete();
            JSONObject result = new JSONObject();
            result.put("success", true);
            return result.toString();
        } catch (Exception e) {
            return jsonError(e.getMessage());
        }
    }

    public String getStorageRoots() {
        try {
            JSONArray roots = new JSONArray();
            File extDir = Environment.getExternalStorageDirectory();
            if (extDir != null) {
                JSONObject ext = new JSONObject();
                ext.put("path", extDir.getAbsolutePath());
                ext.put("label", "External Storage");
                ext.put("type", "external");
                roots.put(ext);
            }
            JSONObject sandbox = new JSONObject();
            sandbox.put("path", "/data/data/com.zx.keep.accounts/files");
            sandbox.put("label", "App Sandbox");
            sandbox.put("type", "internal");
            roots.put(sandbox);
            JSONObject result = new JSONObject();
            result.put("success", true);
            result.put("data", roots);
            return result.toString();
        } catch (Exception e) {
            return jsonError(e.getMessage());
        }
    }

    private byte[] readAllBytes(File file) throws Exception {
        FileInputStream fis = null;
        ByteArrayOutputStream bos = null;
        try {
            fis = new FileInputStream(file);
            bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int len;
            while ((len = fis.read(buf)) != -1) {
                bos.write(buf, 0, len);
            }
            return bos.toByteArray();
        } finally {
            closeQuietly(fis);
            closeQuietly(bos);
        }
    }

    private void writeAllBytes(File file, byte[] data) throws Exception {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            fos.write(data);
            fos.flush();
        } finally {
            closeQuietly(fos);
        }
    }

    private void copyFileStream(File src, File dst) throws Exception {
        FileInputStream fis = null;
        FileOutputStream fos = null;
        FileChannel srcChannel = null;
        FileChannel dstChannel = null;
        try {
            fis = new FileInputStream(src);
            fos = new FileOutputStream(dst);
            srcChannel = fis.getChannel();
            dstChannel = fos.getChannel();
            srcChannel.transferTo(0, srcChannel.size(), dstChannel);
        } finally {
            closeQuietly(dstChannel);
            closeQuietly(srcChannel);
            closeQuietly(fos);
            closeQuietly(fis);
        }
    }

    private void closeQuietly(Closeable c) {
        if (c != null) {
            try { c.close(); } catch (Exception ignored) {}
        }
    }

    private File resolvePath(String path) {
        if (path == null || path.isEmpty()) return null;
        File file = new File(path);
        try {
            String canonicalPath = file.getCanonicalPath();
            String absolutePath = file.getAbsolutePath();
            for (String prefix : BLOCKED_PREFIXES) {
                if (canonicalPath.startsWith(prefix) || absolutePath.startsWith(prefix)) {
                    AuditLogger.logSecurityBlock("Path Blocked", path);
                    return null;
                }
            }
            String root = getAllowedRoot();
            if (canonicalPath.startsWith(root)) {
                return new File(canonicalPath);
            }
            if (canonicalPath.startsWith(root + "/Android/data/com.zx.keep.accounts")) {
                return new File(canonicalPath);
            }
            if (canonicalPath.startsWith("/storage/")) {
                String suffix = canonicalPath.substring(19);
                if (suffix.startsWith("/")) {
                    File resolved = new File(root + suffix);
                    if (resolved.getCanonicalPath().startsWith(root)) {
                        return resolved;
                    }
                }
            }
        } catch (Exception e) {
            AuditLogger.logError("FileAccess", "resolvePath: " + e.getMessage());
        }
        return null;
    }

    private boolean deleteRecursive(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        return file.delete();
    }

    private String jsonError(String msg) {
        try {
            JSONObject result = new JSONObject();
            result.put("success", false);
            result.put("error", msg);
            return result.toString();
        } catch (Exception ignored) {
            return "{\"success\":false,\"error\":\"" + msg.replace("\"", "\\\"") + "\"}";
        }
    }
}
