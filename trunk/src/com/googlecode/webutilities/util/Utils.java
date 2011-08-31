/*
 * Copyright 2010-2011 Rajendra Patil
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.googlecode.webutilities.util;

import javax.servlet.ServletContext;
import java.io.*;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

import static com.googlecode.webutilities.common.Constants.*;

/**
 * Common Utilities provider class
 *
 * @author rpatil
 * @version 1.0
 */
public final class Utils {

    private static final Logger LOGGER = Logger.getLogger(Utils.class.getName());

    private static final String FINGERPRINT_SEPARATOR = "_wu_";

    /**
     * @param string       string representation of a int which is to be parsed and read from
     * @param defaultValue in case parsing fails or string is null, returns this default value
     * @return int parsed value or the default value in case parsing failed
     */
    public static int readInt(String string, int defaultValue) {
        int returnValue;
        try {
            returnValue = Integer.parseInt(string);
        } catch (Exception e) {
            returnValue = defaultValue;
        }
        return returnValue;
    }


    /**
     * @param string       string representation of a long which is to be parsed and read from
     * @param defaultValue in case parsing fails or string is null, returns this default value
     * @return long parsed value or the default value in case parsing failed
     */
    public static long readLong(String string, long defaultValue) {
        long returnValue;
        try {
            returnValue = Long.parseLong(string);
        } catch (Exception e) {
            returnValue = defaultValue;
        }
        return returnValue;
    }

    /**
     * @param string       string representation of a boolean (true or false) which is to be parsed and read from
     * @param defaultValue in case string is null or does not contain valid boolean, returns this default value
     * @return boolean parsed value or the default value
     */

    public static boolean readBoolean(String string, boolean defaultValue) {

        if (string == null || !string.toLowerCase().matches("^true|t|on|1|y|false|f|off|0|n$")) {
            return defaultValue;
        } else {
            return string.toLowerCase().matches("^true|t|on|1|y$");
        }

    }

    /**
     * @param string       string
     * @param defaultValue in case string is null or empty
     * @return String parsed value or the default value
     */

    public static String readString(String string, String defaultValue) {

        if (string == null || string.equals("")) {
            return defaultValue;
        } else {
            return string;
        }

    }

    /**
     * @param requestURI the URL string
     * @return extension .css or .js etc.
     */
    public static String detectExtension(String requestURI) { //!TODO case sensitivity? http://server/context/path/a.CSS
        String requestURIExtension = null;
        if (requestURI.endsWith(EXT_JS)) {
            requestURIExtension = EXT_JS;
        } else if (requestURI.endsWith(EXT_JSON)) {
            requestURIExtension = EXT_JSON;
        } else if (requestURI.endsWith(EXT_CSS)) {
            requestURIExtension = EXT_CSS;
        }
        return requestURIExtension;
    }

    /**
     * @param filePath - path of the file, whose mime is to be detected
     * @return contentType - mime type of the file
     */
    public static String selectMimeByFile(String filePath) {
        if (filePath == null) return null;
        if (filePath.toLowerCase().endsWith(EXT_JS)) {
            return MIME_JS;
        } else if (filePath.toLowerCase().endsWith(EXT_CSS)) {
            return MIME_CSS;
        } else if (filePath.toLowerCase().endsWith(EXT_JSON)) {
            return MIME_JSON;
        }
        String guess = URLConnection.guessContentTypeFromName(filePath);
        return guess != null ? guess : MIME_OCTET_STREAM;
    }

    /**
     * @param extensionOrFile - .js or .css etc. of full file path in case of image files
     * @return - mime like text/javascript or text/css etc.
     */
    public static String selectMimeForExtension(String extensionOrFile) {
        if (EXT_JS.equals(extensionOrFile)) {
            return MIME_JS;
        } else if (EXT_CSS.equals(extensionOrFile)) {
            return MIME_CSS;
        } else if (EXT_JSON.equals(extensionOrFile)) {
            return MIME_JSON;
        } else {
            return Utils.selectMimeByFile(extensionOrFile);
        }
    }

    //!TODO might have problems, need to test or replace with something better
    public static String buildProperPath(String parentPath, String relativePathFromParent) {
        if (relativePathFromParent == null) return null;
        if (parentPath != null) {
            parentPath = parentPath.trim();
        }

        if (relativePathFromParent.startsWith("./")) {
            relativePathFromParent = relativePathFromParent.replaceFirst("(./)+", "");
        }

        String path;

        if (relativePathFromParent.startsWith("/")) { //absolute
            path = relativePathFromParent;
        } else if (relativePathFromParent.startsWith("../")) {
            while (relativePathFromParent.startsWith("../")) {
                relativePathFromParent = relativePathFromParent.replaceFirst("../", "");
                if (relativePathFromParent.startsWith("./")) {
                    relativePathFromParent = relativePathFromParent.replaceFirst("./", "");
                }
                parentPath = (parentPath == null || parentPath.equals("/")) ? "/" : getParentPath(parentPath);
            }
            path = parentPath + File.separator + relativePathFromParent;
        } else {
            path = parentPath + File.separator + relativePathFromParent;
        }

        return path.replaceAll("(/|\\./)+", "$1");
    }

    /**
     * Calculates simple hash using file size and last modified time.
     *
     * @param resourceRealPath - file path, whose has to be calculated
     * @return - hash string as lastmodified#size
     */
    private static String simpleHashOf(String resourceRealPath) {
        if (resourceRealPath == null) return null;
        File resource = new File(resourceRealPath);
        if (!resource.exists()) return null;
        long lastModified = resource.lastModified();
        long size = resource.length();
        return String.format("%s#%s", lastModified, size);
    }

    /**
     * Split multiple resources with comma eg. if URL is http://server/context/js/a,b,c.js
     * then a.js, b.js and c.js have to be processed and merged together.
     * <p/>
     * b and c can be absolute paths or relative (relative to previous resource) too.
     * <p/>
     * eg.
     * <p/>
     * http://server/context/js/a,/js/libs/b,/js/yui/c.js - absolutes paths for all OR
     * http://server/context/js/a,/js/libs/b,../yui/c.js - relative path used for c.js (relative to b) OR
     * http://server/context/js/a,/js/libs/b,./c.js OR - b & c are in same directory /js/libs
     *
     * @param contextPath request Context Path
     * @param requestURI  requestURI
     * @return Set of resources to be processed
     */
    public static List<String> findResourcesToMerge(String contextPath, String requestURI) {

        String extension = Utils.detectExtension(requestURI);

        if (extension == null) {
            extension = "";
        }

        requestURI = requestURI.replace(contextPath, "").replace(extension, "");//remove the context path & ext. will become /path/subpath/a,b,/anotherpath/c

        String[] resourcesPath = requestURI.split(",");

        List<String> resources = new ArrayList<String>();

        String currentPath = "/"; //default

        for (String filePath : resourcesPath) {

            String path = Utils.buildProperPath(currentPath, filePath) + extension;
            if (filePath == null) continue;

            currentPath = getParentPath(path);
            if (!resources.contains(path)) {
                resources.add(path);
            }
        }
        return resources;
    }

    /**
     * @param resources      - list of resources paths
     * @param sinceTime      - long value to compare against
     * @param servletContext - servlet context
     * @return true if any of the resources is modified since given time, false otherwise
     */
    public static boolean isAnyResourceModifiedSince(List<String> resources, long sinceTime, ServletContext servletContext) {
        for (String resourcePath : resources) {
            resourcePath = servletContext.getRealPath(resourcePath);
            if (resourcePath == null) continue;
            File resource = new File(resourcePath);
            long lastModified = resource.lastModified();
            if (lastModified > sinceTime) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param resources      - list of resources paths
     * @param servletContext - servlet context
     * @return long - maximum of last modified values of the resources
     */
    public static long getLastModifiedFor(List<String> resources, ServletContext servletContext) {
        long lastModified = 0;
        for (String resourcePath : resources) {
            resourcePath = servletContext.getRealPath(resourcePath);
            if (resourcePath == null) continue;
            File resource = new File(resourcePath);
            long resourceLastModified = resource.lastModified();
            if (resourceLastModified > lastModified) {
                lastModified = resourceLastModified;
            }
        }
        return lastModified;
    }

    /**
     * @param resources      - list of resources
     * @param requestETag    - request ETag from If-None-Match header
     * @param actualETag     - current ETag of a resource
     * @param servletContext - servlet context
     * @return true if any resource ETag is modified, false otherwise.
     */
    public static boolean isAnyResourceETagModified(List<String> resources, String requestETag, String actualETag, ServletContext servletContext) {
        if (actualETag == null && requestETag != null) {
            actualETag = buildETagForResources(resources, servletContext);
        }
        if (requestETag != null && actualETag != null) {
            requestETag = requestETag.replace("-gzip", "");//might have been added by gzip filter
            return !requestETag.equals(actualETag);
        }
        return true;
    }


    /**
     * @param resourcesRelativePath - list of resources
     * @param context               - servlet context
     * @return - String as ETag calculated using simple hash based on size and last modified of all resources
     */
    public static String buildETagForResources(List<String> resourcesRelativePath, ServletContext context) {
        String hashForETag = "";
        for (String relativePath : resourcesRelativePath) {
            String hash = buildETagForResource(relativePath, context);
            hashForETag = hashForETag + (hash != null ? hash : "");
        }
        return hashForETag.length() > 0 ? (resourcesRelativePath.size() > 2 ? hexDigestString(hashForETag.getBytes()) : hashForETag) : null;
    }

    /**
     * @param cssFilePath
     * @param imgFilePath
     * @return
     */
    public static boolean updateReferenceMap(String cssFilePath, String imgFilePath) {
        if (imgFilePath != null) {
            File imgFile = new File(imgFilePath);
            List<String> referencesList = CSS_IMG_REFERENCES.get(cssFilePath);
            if (imgFile.isFile() && imgFile.exists()) {
                if (referencesList == null) {
                    referencesList = new LinkedList<String>();
                    referencesList.add(imgFilePath);
                    CSS_IMG_REFERENCES.put(cssFilePath, referencesList);
                }
                if (!referencesList.contains(imgFilePath)) {
                    referencesList.add(imgFilePath);
                }
                File cssFile = new File(cssFilePath);
                if (cssFile.lastModified() < imgFile.lastModified()) { //means img got modified after css
                    //so touch css file
                    cssFile.setLastModified(new Date().getTime());
                    return true;
                }
            } else if (referencesList != null) {
                referencesList.remove(imgFilePath);
            }
        }
        return false;
    }

    public static boolean isProtocolURL(String url) {
        if (url == null || url.trim().length() == 0) return false;
        return url.matches("^[a-z0-9\\+\\.\\-]+:.*$");
    }

    /**
     * @param relativePath - relative path of res
     * @param context      - servlet context
     * @return ETag string
     */
    public static String buildETagForResource(String relativePath, ServletContext context) {
        String hashForETag = ":";
        String realPath = context.getRealPath(relativePath);
        if (realPath == null) return null;
        File realFile = new File(realPath);
        if (!realFile.isFile() || !realFile.exists()) return null;
        if (realPath.endsWith(EXT_CSS)) { // check if any image references by this css has been modified or not
            long cssLastModified = realFile.lastModified();

            List<String> referencedImages = CSS_IMG_REFERENCES.get(realPath);

            if (referencedImages != null) {
                for (String referenceImage : referencedImages) {
                    File imgFile = new File(referenceImage);
                    if (imgFile.isFile() && imgFile.exists()) {
                        if (cssLastModified < imgFile.lastModified()) { //means ref img got modified after css
                            //so touch css file
                            realFile.setLastModified(new Date().getTime());
                            break;
                        }
                    }
                }

            } else {

                BufferedReader bufferedReader;
                try {
                    bufferedReader = new BufferedReader(new FileReader(realPath));
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        Matcher matcher = CSS_IMG_URL_PATTERN.matcher(line);
                        while (matcher.find()) {
                            String refImgPath = matcher.group(1);
                            if (!Utils.isProtocolURL(refImgPath)) { //ignore absolute protocol paths
                                String resolvedImgPath = refImgPath;
                                if (!refImgPath.startsWith("/")) {
                                    resolvedImgPath = Utils.buildProperPath(Utils.getParentPath(realPath), refImgPath);
                                }

                                if (updateReferenceMap(realPath, resolvedImgPath)) {
                                    break;
                                }

                            }
                        }
                    }
                } catch (FileNotFoundException ex) {
                    LOGGER.warning("File not found." + ex);
                } catch (IOException ex) {
                    LOGGER.warning("Failed to read/touch " + realPath + ". ex:" + ex);
                }
            }
        }
        String hash = Utils.simpleHashOf(realPath);
        hashForETag = hashForETag + (hash != null ? ":" + hash : "");
        return hashForETag.length() > 0 ? hexDigestString(hashForETag.getBytes()) : null;
    }

    /**
     * @param headerDateString - from request header
     * @return Date object after reading from header string
     */
    public static Date readDateFromHeader(String headerDateString) {

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATE_PATTERN_HTTP_HEADER, DEFAULT_LOCALE_US);
        try {
            return simpleDateFormat.parse(headerDateString);
        } catch (Exception e) {
            LOGGER.warning("Date parsing using HTTP header pattern failed.");
        }

        //try another rfc1123
        simpleDateFormat = new SimpleDateFormat(DATE_PATTERN_RFC_1123, DEFAULT_LOCALE_US);
        try {
            return simpleDateFormat.parse(headerDateString);
        } catch (Exception e) {
            LOGGER.warning("Date parsing using RFC_1123 pattern failed.");
        }

        //try another rfc1036
        simpleDateFormat = new SimpleDateFormat(DATE_PATTERN_RFC_1036, DEFAULT_LOCALE_US);
        try {
            return simpleDateFormat.parse(headerDateString);
        } catch (Exception e) {
            LOGGER.warning("Date parsing using RFC_1036 pattern failed.");
        }

        //try another ansi
        simpleDateFormat = new SimpleDateFormat(DATE_PATTERN_ANSI_C, DEFAULT_LOCALE_US);
        try {
            return simpleDateFormat.parse(headerDateString);
        } catch (Exception e) {
            LOGGER.warning("Date is not even ANSI C pattern.");
        }

        return null;
    }

    public static String forHeaderDate(long time) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATE_PATTERN_HTTP_HEADER, DEFAULT_LOCALE_US);
        return simpleDateFormat.format(time);
    }

    public static String hexDigestString(byte[] data) {
        MessageDigest md5Digest = null;
        try {
            md5Digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ex) {
            LOGGER.warning("Unable to use MD5 for digesting." + ex);
        }
        if (md5Digest != null) {
            data = md5Digest.digest(data);
        }
        final char[] HEX_CHARS = "0123456789abcdef".toCharArray();
        char[] hex = new char[2 * data.length];
        for (int i = 0; i < data.length; ++i) {
            hex[2 * i] = HEX_CHARS[(data[i] & 0xF0) >>> 4];
            hex[2 * i + 1] = HEX_CHARS[data[i] & 0x0F];
        }
        return new String(hex);
    }

    /**
     * @param fingerPrint hex digest
     * @param url
     * @return url with fingerprint
     */
    public static String addFingerPrint(String fingerPrint, String url) {
        if (fingerPrint != null) {
            int li = url.lastIndexOf(".");
            url = url.substring(0, li) + FINGERPRINT_SEPARATOR + fingerPrint + url.substring(li);
        }
        return url;
    }

    /**
     * @param url Finger Printed URL
     * @return Non Finger Printed URL
     */
    public static String removeFingerPrint(String url) {
        int from = url.indexOf(FINGERPRINT_SEPARATOR);
        if (from <= 0) return url;
        int to = url.lastIndexOf(".");
        return url.substring(0, from) + url.substring(to);
    }

    /**
     * Fast get parent directory
     * // * @param path
     *
     * @return
     */
    public static String getParentPath(String path) {
        if (path == null) return null;
        path = path.trim();
        if (path.endsWith("/") && path.length() > 1) {
            path = path.substring(0, path.length() - 1);
        }
        int lastIndex = path.lastIndexOf("/");
        if (path.length() > 1 && lastIndex > 0) {
            return path.substring(0, lastIndex);
        }
        return "/";
    }

    /**
     * This method accept string tokens and return the concatenated logger message.
     *
     * @param messages
     * @return
     */
    public static String buildLoggerMessage(Object... messages) {
        StringBuilder strBuilder = new StringBuilder();
        for (int i = 0; i < messages.length; i++) {
            strBuilder.append(messages[i]);
        }
        return strBuilder.toString();
    }

    private Utils() {
    } //non instantiable

    public static void main(String[] args) {
        System.out.println(Utils.selectMimeByFile("/Users/rpatil/Documents/workspace/ARCC/js/unittest.js"));
    }
}
