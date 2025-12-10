package com.kiduyu.klaus.ebookfinaldownload.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class EpubCoverExtractor {
    private static final String TAG = "EpubCoverExtractor";

    /**
     * Extracts the cover image from an EPUB file and saves it to cache directory
     * @param context Application context
     * @param epubPath Path to the EPUB file
     * @return Path to the extracted cover image, or null if extraction failed
     */
    public static String extractCoverImage(Context context, String epubPath) {
        try {
            File epubFile = new File(epubPath);
            if (!epubFile.exists()) {
                Log.e(TAG, "EPUB file does not exist: " + epubPath);
                return null;
            }

            // Create cache directory for covers
            File coversDir = new File(context.getCacheDir(), "book_covers");
            if (!coversDir.exists()) {
                coversDir.mkdirs();
            }

            // Generate cover file name based on epub file name
            String coverFileName = epubFile.getName().replace(".epub", "") + "_cover.jpg";
            File coverFile = new File(coversDir, coverFileName);

            // If cover already exists, return it
            if (coverFile.exists()) {
                return coverFile.getAbsolutePath();
            }

            // Extract cover from EPUB
            ZipFile zipFile = new ZipFile(epubFile);
            String coverImagePath = findCoverImagePath(zipFile);

            if (coverImagePath != null) {
                ZipEntry coverEntry = zipFile.getEntry(coverImagePath);
                if (coverEntry != null) {
                    InputStream inputStream = zipFile.getInputStream(coverEntry);

                    // Decode bitmap
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    inputStream.close();

                    if (bitmap != null) {
                        // Save bitmap to cache
                        FileOutputStream outputStream = new FileOutputStream(coverFile);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
                        outputStream.flush();
                        outputStream.close();
                        bitmap.recycle();

                        zipFile.close();
                        return coverFile.getAbsolutePath();
                    }
                }
            }

            zipFile.close();
            Log.w(TAG, "Could not find cover image in EPUB: " + epubPath);
            return null;

        } catch (Exception e) {
            Log.e(TAG, "Error extracting cover image", e);
            return null;
        }
    }

    /**
     * Finds the cover image path within the EPUB (ZIP) file
     */
    private static String findCoverImagePath(ZipFile zipFile) {
        try {
            // First, try to find cover from OPF file
            String opfPath = findOpfFile(zipFile);
            if (opfPath != null) {
                String coverFromOpf = findCoverFromOpf(zipFile, opfPath);
                if (coverFromOpf != null) {
                    return coverFromOpf;
                }
            }

            // Fallback: look for common cover image names
            return findCoverByCommonNames(zipFile);

        } catch (Exception e) {
            Log.e(TAG, "Error finding cover image path", e);
            return null;
        }
    }

    /**
     * Finds the OPF (Open Packaging Format) file in the EPUB
     */
    private static String findOpfFile(ZipFile zipFile) {
        try {
            // Check container.xml for OPF location
            ZipEntry containerEntry = zipFile.getEntry("META-INF/container.xml");
            if (containerEntry != null) {
                InputStream inputStream = zipFile.getInputStream(containerEntry);
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.parse(inputStream);

                NodeList rootfiles = doc.getElementsByTagName("rootfile");
                if (rootfiles.getLength() > 0) {
                    Element rootfile = (Element) rootfiles.item(0);
                    String fullPath = rootfile.getAttribute("full-path");
                    inputStream.close();
                    return fullPath;
                }
                inputStream.close();
            }

            // Fallback: look for .opf files
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.getName().endsWith(".opf")) {
                    return entry.getName();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error finding OPF file", e);
        }
        return null;
    }

    /**
     * Extracts cover image path from OPF file
     */
    private static String findCoverFromOpf(ZipFile zipFile, String opfPath) {
        try {
            ZipEntry opfEntry = zipFile.getEntry(opfPath);
            if (opfEntry == null) return null;

            InputStream inputStream = zipFile.getInputStream(opfEntry);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(inputStream);

            // Get the directory of the OPF file
            String opfDir = "";
            int lastSlash = opfPath.lastIndexOf('/');
            if (lastSlash != -1) {
                opfDir = opfPath.substring(0, lastSlash + 1);
            }

            // Look for cover in metadata
            NodeList metaItems = doc.getElementsByTagName("meta");
            String coverId = null;
            for (int i = 0; i < metaItems.getLength(); i++) {
                Element meta = (Element) metaItems.item(i);
                if ("cover".equals(meta.getAttribute("name"))) {
                    coverId = meta.getAttribute("content");
                    break;
                }
            }

            // Find the actual image file using cover id
            if (coverId != null) {
                NodeList items = doc.getElementsByTagName("item");
                for (int i = 0; i < items.getLength(); i++) {
                    Element item = (Element) items.item(i);
                    if (coverId.equals(item.getAttribute("id"))) {
                        String href = item.getAttribute("href");
                        inputStream.close();
                        return opfDir + href;
                    }
                }
            }

            // Alternative: look for items with "cover" in id or properties
            NodeList items = doc.getElementsByTagName("item");
            for (int i = 0; i < items.getLength(); i++) {
                Element item = (Element) items.item(i);
                String id = item.getAttribute("id").toLowerCase();
                String properties = item.getAttribute("properties").toLowerCase();
                String mediaType = item.getAttribute("media-type");

                if ((id.contains("cover") || properties.contains("cover-image")) &&
                        mediaType.startsWith("image/")) {
                    String href = item.getAttribute("href");
                    inputStream.close();
                    return opfDir + href;
                }
            }

            inputStream.close();
        } catch (Exception e) {
            Log.e(TAG, "Error extracting cover from OPF", e);
        }
        return null;
    }

    /**
     * Fallback method: searches for common cover image file names
     */
    private static String findCoverByCommonNames(ZipFile zipFile) {
        String[] commonNames = {
                "cover.jpg", "cover.jpeg", "cover.png",
                "Cover.jpg", "Cover.jpeg", "Cover.png",
                "COVER.jpg", "COVER.jpeg", "COVER.png"
        };

        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            String name = entry.getName();

            // Check if filename contains "cover" and is an image
            if (name.toLowerCase().contains("cover") &&
                    (name.endsWith(".jpg") || name.endsWith(".jpeg") ||
                            name.endsWith(".png") || name.endsWith(".gif"))) {
                return name;
            }

            // Check common names
            for (String commonName : commonNames) {
                if (name.endsWith(commonName)) {
                    return name;
                }
            }
        }
        return null;
    }

    /**
     * Deletes cached cover image for a specific EPUB
     */
    public static void deleteCachedCover(Context context, String epubPath) {
        try {
            File epubFile = new File(epubPath);
            File coversDir = new File(context.getCacheDir(), "book_covers");
            String coverFileName = epubFile.getName().replace(".epub", "") + "_cover.jpg";
            File coverFile = new File(coversDir, coverFileName);

            if (coverFile.exists()) {
                coverFile.delete();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error deleting cached cover", e);
        }
    }
}