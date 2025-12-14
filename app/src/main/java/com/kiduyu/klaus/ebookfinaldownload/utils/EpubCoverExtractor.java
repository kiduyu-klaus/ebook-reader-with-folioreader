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
     * Extracts the book title from an EPUB file
     * @param epubPath Path to the EPUB file
     * @return Book title, or filename if extraction failed
     */
    public static String extractBookTitle(String epubPath) {
        try {
            File epubFile = new File(epubPath);
            if (!epubFile.exists()) {
                Log.e(TAG, "EPUB file does not exist: " + epubPath);
                return getFileNameWithoutExtension(epubPath);
            }

            ZipFile zipFile = new ZipFile(epubFile);
            String opfPath = findOpfFile(zipFile);

            if (opfPath != null) {
                String title = extractTitleFromOpf(zipFile, opfPath);
                zipFile.close();

                if (title != null && !title.trim().isEmpty()) {
                    return title.trim();
                }
            }

            zipFile.close();
            // Fallback to filename without extension
            return getFileNameWithoutExtension(epubPath);

        } catch (Exception e) {
            Log.e(TAG, "Error extracting book title", e);
            return getFileNameWithoutExtension(epubPath);
        }
    }

    /**
     * Extracts the book author from an EPUB file
     * @param epubPath Path to the EPUB file
     * @return Book author, or "Unknown" if extraction failed
     */
    public static String extractBookAuthor(String epubPath) {
        try {
            File epubFile = new File(epubPath);
            if (!epubFile.exists()) {
                Log.e(TAG, "EPUB file does not exist: " + epubPath);
                return "Unknown";
            }

            ZipFile zipFile = new ZipFile(epubFile);
            String opfPath = findOpfFile(zipFile);

            if (opfPath != null) {
                String author = extractAuthorFromOpf(zipFile, opfPath);
                zipFile.close();

                if (author != null && !author.trim().isEmpty()) {
                    return author.trim();
                }
            }

            zipFile.close();
            return "Unknown";

        } catch (Exception e) {
            Log.e(TAG, "Error extracting book author", e);
            return "Unknown";
        }
    }

    /**
     * Extracts complete book metadata from an EPUB file
     * @param epubPath Path to the EPUB file
     * @return EpubMetadata object containing title, author, etc.
     */
    public static EpubMetadata extractMetadata(String epubPath) {
        EpubMetadata metadata = new EpubMetadata();

        try {
            File epubFile = new File(epubPath);
            if (!epubFile.exists()) {
                metadata.title = getFileNameWithoutExtension(epubPath);
                return metadata;
            }

            ZipFile zipFile = new ZipFile(epubFile);
            String opfPath = findOpfFile(zipFile);

            if (opfPath != null) {
                extractAllMetadataFromOpf(zipFile, opfPath, metadata);
            }

            zipFile.close();

            // Set defaults if not found
            if (metadata.title == null || metadata.title.trim().isEmpty()) {
                metadata.title = getFileNameWithoutExtension(epubPath);
            }
            if (metadata.author == null || metadata.author.trim().isEmpty()) {
                metadata.author = "Unknown";
            }

        } catch (Exception e) {
            Log.e(TAG, "Error extracting metadata", e);
            metadata.title = getFileNameWithoutExtension(epubPath);
            metadata.author = "Unknown";
        }

        return metadata;
    }

    /**
     * Helper method to get filename without extension
     */
    private static String getFileNameWithoutExtension(String filePath) {
        File file = new File(filePath);
        String name = file.getName();
        int lastDot = name.lastIndexOf('.');
        if (lastDot > 0) {
            return name.substring(0, lastDot);
        }
        return name;
    }

    /**
     * Extracts title from OPF file
     */
    private static String extractTitleFromOpf(ZipFile zipFile, String opfPath) {
        try {
            ZipEntry opfEntry = zipFile.getEntry(opfPath);
            if (opfEntry == null) return null;

            InputStream inputStream = zipFile.getInputStream(opfEntry);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(inputStream);

            // Look for title in metadata
            NodeList titleNodes = doc.getElementsByTagName("dc:title");
            if (titleNodes.getLength() > 0) {
                String title = titleNodes.item(0).getTextContent();
                inputStream.close();
                return title;
            }

            inputStream.close();
        } catch (Exception e) {
            Log.e(TAG, "Error extracting title from OPF", e);
        }
        return null;
    }

    /**
     * Extracts author from OPF file
     */
    private static String extractAuthorFromOpf(ZipFile zipFile, String opfPath) {
        try {
            ZipEntry opfEntry = zipFile.getEntry(opfPath);
            if (opfEntry == null) return null;

            InputStream inputStream = zipFile.getInputStream(opfEntry);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(inputStream);

            // Look for creator/author in metadata
            NodeList creatorNodes = doc.getElementsByTagName("dc:creator");
            if (creatorNodes.getLength() > 0) {
                String author = creatorNodes.item(0).getTextContent();
                inputStream.close();
                return author;
            }

            inputStream.close();
        } catch (Exception e) {
            Log.e(TAG, "Error extracting author from OPF", e);
        }
        return null;
    }

    /**
     * Extracts all metadata from OPF file
     */
    private static void extractAllMetadataFromOpf(ZipFile zipFile, String opfPath, EpubMetadata metadata) {
        try {
            ZipEntry opfEntry = zipFile.getEntry(opfPath);
            if (opfEntry == null) return;

            InputStream inputStream = zipFile.getInputStream(opfEntry);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(inputStream);

            // Extract title
            NodeList titleNodes = doc.getElementsByTagName("dc:title");
            if (titleNodes.getLength() > 0) {
                metadata.title = titleNodes.item(0).getTextContent();
            }

            // Extract author/creator
            NodeList creatorNodes = doc.getElementsByTagName("dc:creator");
            if (creatorNodes.getLength() > 0) {
                metadata.author = creatorNodes.item(0).getTextContent();
            }

            // Extract publisher
            NodeList publisherNodes = doc.getElementsByTagName("dc:publisher");
            if (publisherNodes.getLength() > 0) {
                metadata.publisher = publisherNodes.item(0).getTextContent();
            }

            // Extract language
            NodeList languageNodes = doc.getElementsByTagName("dc:language");
            if (languageNodes.getLength() > 0) {
                metadata.language = languageNodes.item(0).getTextContent();
            }

            // Extract description
            NodeList descriptionNodes = doc.getElementsByTagName("dc:description");
            if (descriptionNodes.getLength() > 0) {
                metadata.description = descriptionNodes.item(0).getTextContent();
            }

            // Extract date
            NodeList dateNodes = doc.getElementsByTagName("dc:date");
            if (dateNodes.getLength() > 0) {
                metadata.publishDate = dateNodes.item(0).getTextContent();
            }

            inputStream.close();
        } catch (Exception e) {
            Log.e(TAG, "Error extracting all metadata from OPF", e);
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

    /**
     * Data class to hold EPUB metadata
     */
    public static class EpubMetadata {
        public String title;
        public String author;
        public String publisher;
        public String language;
        public String description;
        public String publishDate;

        public EpubMetadata() {
            this.title = "";
            this.author = "Unknown";
            this.publisher = "";
            this.language = "";
            this.description = "";
            this.publishDate = "";
        }

        @Override
        public String toString() {
            return "EpubMetadata{" +
                    "title='" + title + '\'' +
                    ", author='" + author + '\'' +
                    ", publisher='" + publisher + '\'' +
                    ", language='" + language + '\'' +
                    '}';
        }
    }
}