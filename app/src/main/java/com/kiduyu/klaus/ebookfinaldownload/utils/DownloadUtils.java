package com.kiduyu.klaus.ebookfinaldownload.utils;





import static android.content.Context.POWER_SERVICE;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.provider.Settings;
import android.net.Uri;
import android.os.PowerManager;
import android.util.Log;

import com.kiduyu.klaus.ebookfinaldownload.MainActivity;
import com.kiduyu.klaus.ebookfinaldownload.models.BookInfo;
import com.kiduyu.klaus.ebookfinaldownload.models.DownloadLink;
import com.kiduyu.klaus.ebookfinaldownload.models.Listopia;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import okhttp3.*;

public class DownloadUtils {
    private static final String TAG = "DownloadUtils";
    private static final String FETCH_URL = "https://oceanofpdf.com/Fetching_Resource.php";
    private static final int CHUNK_SIZE = 65536; // 64KB
    private static final String[] USER_AGENTS = {
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:121.0) Gecko/20100101 Firefox/121.0",
            "Mozilla/5.0 (Android 13; Mobile; rv:109.0) Gecko/120.0 Firefox/120.0",
            "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.6099.144 Mobile Safari/537.36"
    };

    private static final Random random = new Random();
    private static final ExecutorService chunkExecutor = Executors.newFixedThreadPool(4);
    Context context;
    public DownloadUtils(Context context) {
        this.context=context;
    }


    public boolean isBatteryOptimized(Context context) {
        PowerManager powerManager = (PowerManager) context.getSystemService(POWER_SERVICE);
        String packageName = context.getPackageName();
        // returns true if the app is currently on the exemption (ignore) list
        return !powerManager.isIgnoringBatteryOptimizations(packageName);
    }

    public void showBatteryOptimizationDialog() {
        if (isBatteryOptimized(context)) { // Check if optimized (not ignoring)
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Enable Battery Optimization")
                    .setMessage("For optimal battery life, we recommend enabling battery optimization for this app. This might restrict background functions.")
                    .setPositiveButton("Open Settings", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Intent to open the general battery optimization settings list
                            Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                            try {
                                context.startActivity(intent);
                            } catch (ActivityNotFoundException e) {
                                // Handle case where the specific settings activity is not found
                                // You might want to direct the user to the main settings page instead
                                Intent generalSettingsIntent = new Intent(Settings.ACTION_SETTINGS);
                                context.startActivity(generalSettingsIntent);
                            }
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .setCancelable(false); // User must choose an action

            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    /**
     * Get a random user agent string
     */
    public static String getRandomUserAgent() {
        return USER_AGENTS[random.nextInt(USER_AGENTS.length)];
    }

    public static String cleanTitle(String title) {
        return title.replaceAll("\\s*\\(.*?\\)", "").trim();
    }

    /**
     * Get download forms from a book page
     * Returns list of form data (id and filename) for downloading
     */
    public  String fetchAndDownload(List<DownloadLink>  payload, OkHttpClient client,
                                          BookInfo bookInfo, int maxRetries) {
        DownloadLink epubLink = null;
        for (DownloadLink link : payload) {
            if (link.getFilename().toLowerCase().endsWith(".epub")) {
                epubLink = link;
                break;
            }
        }

        if (epubLink == null) {
            System.out.println("❌ No EPUB download link found for this book.");
            return null;
        }

        System.out.println("\n[+] Requesting resource for " + epubLink.getFilename() + "...");

        try {
            // Step 1: POST to fetch resource
            FormBody formBody = new FormBody.Builder()
                    .add("id", epubLink.getId())
                    .add("filename", epubLink.getFilename())
                    .build();

            Request postRequest = new Request.Builder()
                    .url(FETCH_URL)
                    .post(formBody)
                    .header("User-Agent", getRandomUserAgent())
                    .build();

            Response postResponse = client.newCall(postRequest).execute();
            if (!postResponse.isSuccessful() || postResponse.body() == null) {
                System.out.println("❌ POST request failed");
                return null;
            }

            String responseText = postResponse.body().string();

            // Step 2: Extract redirect URL
            Pattern pattern = Pattern.compile("https://fs\\d+\\.oceanofpdf\\.com/[^\\s\"']+");
            Matcher matcher = pattern.matcher(responseText);

            if (!matcher.find()) {
                System.out.println("[!] No redirect URL found. Response preview:");
                System.out.println(responseText.substring(0, Math.min(500, responseText.length())));
                return null;
            }

            String redirectUrl = matcher.group(0);

            // Step 3: HEAD request with retries
            Response headResponse = null;
            for (int attempt = 0; attempt < maxRetries; attempt++) {
                try {
                    Request headRequest = new Request.Builder()
                            .url(redirectUrl)
                            .head()
                            .header("User-Agent", getRandomUserAgent())
                            .build();

                    headResponse = client.newCall(headRequest).execute();
                    if (headResponse.isSuccessful()) {
                        return redirectUrl;
                    }
                } catch (IOException e) {
                    if (attempt < maxRetries - 1) {
                        int waitTime = 5 * (int) Math.pow(2, attempt);
                        System.out.println("❌ HEAD request failed (attempt " + (attempt + 1) +
                                "/" + maxRetries + "): " + e.getMessage());
                        System.out.println("   Retrying in " + waitTime + " seconds...");
                        Thread.sleep(waitTime * 1000L);
                    } else {
                        System.out.println("❌ HEAD request failed after " + maxRetries + " attempts");
                        return null;
                    }
                }
            }

            if (headResponse == null || !headResponse.isSuccessful()) {
                System.out.println("❌ HEAD request validation failed");
                return null;
            }

            // Step 4: Validate content-disposition
            String contentDisposition = headResponse.header("content-disposition", "");
            if (contentDisposition.contains("attachment") && contentDisposition.contains("filename=")) {
                bookInfo.getDownloadLinks().get(0).setDownlink(redirectUrl);
                return redirectUrl;
            } else {
                System.out.println("❌ No valid downloadable attachment in headers.");
                return null;
            }

        } catch (Exception e) {
            System.err.println("❌ Download failed: " + e.getMessage());
            return null;
        }
    }

    private static double extractSizeInMB(String sizeText) {
        if (sizeText == null || sizeText.isEmpty()) {
            return 0;
        }

        Pattern pattern = Pattern.compile("(\\d+\\.?\\d*)\\s*(MB|GB|KB)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(sizeText.toUpperCase());

        if (matcher.find()) {
            double value = Double.parseDouble(matcher.group(1));
            String unit = matcher.group(2);

            switch (unit) {
                case "KB":
                    return value / 1024;
                case "MB":
                    return value;
                case "GB":
                    return value * 1024;
            }
        }

        return 0;
    }


    public boolean isEnglish(Element postmetainfo) {
        Elements strongTags = postmetainfo.select("strong");
        for (Element strong : strongTags) {
            if (strong.text().contains("Language:")) {
                String language = strong.nextSibling() != null ?
                        Objects.requireNonNull(strong.nextSibling()).toString().trim().toLowerCase() : "";
                return language.equals("english");
            }
        }
        return true;
    }

    public int getLastPage(String url,OkHttpClient client) throws Exception {
        Document doc = fetchPage(url, client);
        if (doc == null) return 1;

        Element pagination = doc.selectFirst("div.archive-pagination.pagination");
        if (pagination == null) return 1;

        int maxPage = 1;
        Elements links = pagination.select("a[href]");
        for (Element link : links) {
            link.select("span").remove();
            String text = link.text().trim();
            try {
                int pageNum = Integer.parseInt(text);
                maxPage = Math.max(maxPage, pageNum);
            } catch (NumberFormatException ignored) {}
        }

        Thread.sleep(3000);
        return maxPage;
    }

    public Document fetchPage(String url, OkHttpClient client) {
        try {
            Request request = new Request.Builder()
                    .url(url)
                    .header("User-Agent", getRandomUserAgent())
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Referer", "https://www.google.com/")
                    .build();

            Response response = client.newCall(request).execute();
            if (response.isSuccessful() && response.body() != null) {
                String html = response.body().string();
                return Jsoup.parse(html);
            }
        } catch (IOException e) {
            Log.d(TAG, "[!] Failed to fetch " + url + ": " + e.getMessage() + "\n");
        }
        return null;
    }


    public BookInfo getBookInfo(String bookUrl, OkHttpClient client) {
        try {
            Request request = new Request.Builder()
                    .url(bookUrl)
                    .header("User-Agent", getRandomUserAgent())
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Referer", "https://www.google.com/")
                    .build();

            Response response = client.newCall(request).execute();

            if (!response.isSuccessful() || response.body() == null) {
                return null;
            }

            String html = response.body().string();
            Document doc = Jsoup.parse(html);

            BookInfo info = new BookInfo();
            info.setBookUrl(bookUrl);
            info.setDownlink(bookUrl);

            // Extract book cover image
            Element entryContent = doc.selectFirst("div.entry-content");
            if (entryContent != null) {
                // Try to find the book cover image
                Element imgTag = entryContent.selectFirst("img.aligncenter");
                if (imgTag != null) {
                    String imgUrl = imgTag.attr("src");
                    if (imgUrl == null || imgUrl.isEmpty()) {
                        // Try data-src for lazy loaded images
                        imgUrl = imgTag.attr("data-src");
                    }
                    if (imgUrl != null && !imgUrl.isEmpty()) {
                        info.setBookimg(imgUrl);
                        Log.d(TAG, "Found book cover: " + imgUrl);
                    }
                }

                // If no image found with aligncenter, try any img in entry-content
                if (info.getBookimg() == null) {
                    Elements allImages = entryContent.select("img");
                    for (Element img : allImages) {
                        String src = img.attr("src");
                        if (src == null || src.isEmpty()) {
                            src = img.attr("data-src");
                        }
                        // Check if it's a book cover (usually contains PDF-EPUB or book title)
                        if (src != null && !src.isEmpty() &&
                                (src.contains("media.oceanofpdf.com") &&
                                        !src.contains("button") &&
                                        !src.contains("donate") &&
                                        !src.contains("sharing"))) {
                            info.setBookimg(src);
                            Log.d(TAG, "Found book cover (alternative): " + src);
                            break;
                        }
                    }
                }

                // Extract book details from ul tag
                Element ulTag = entryContent.selectFirst("ul");
                if (ulTag != null) {
                    Elements liElements = ulTag.select("li");
                    for (Element li : liElements) {
                        Element strong = li.selectFirst("strong");
                        if (strong != null) {
                            String text = strong.text().trim();
                            String value = li.text().replace(text, "").trim();

                            if (text.contains("Full Book Name")) {
                                info.setTitle(value);
                            } else if (text.contains("Author")) {
                                info.setAuthor(value);
                            } else if (text.contains("Language")) {
                                info.setLanguage(value);
                            } else if (text.contains("PDF File Size")) {
                                info.setPdfSize(value);
                            } else if (text.contains("EPUB File Size")) {
                                info.setEpubSize(value);
                            }
                        }
                    }
                }
            }

            // Extract download forms
            Elements forms = doc.select("form[action=https://oceanofpdf.com/Fetching_Resource.php]");
            for (Element form : forms) {
                Element idInput = form.selectFirst("input[name=id]");
                Element filenameInput = form.selectFirst("input[name=filename]");

                if (idInput != null && filenameInput != null) {
                    String filename = filenameInput.attr("value");
                    String fileExt = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();

                    if (fileExt.equals("epub")) {
                        DownloadLink link = new DownloadLink();
                        link.setId(idInput.attr("value"));
                        link.setFilename(filename);
                        link.setFormat(fileExt);

                        info.addDownloadLink(link);
                    }
                }
            }

            Thread.sleep(1500);
            return info;

        } catch (Exception e) {
            Log.d(TAG, "Error extracting book info: " + e.getMessage());
            return null;
        }
    }


    /**
     * Fetch and download a book given payload data
     * Returns the path to the downloaded file or null if failed
     */
    public static String fetchAndDownload(Map<String, String> payload, OkHttpClient client,
                                          String downloadDir, int maxRetries, String baseDir) {
        System.out.println("\n[+] Requesting resource for " + payload.get("filename") + "...");

        try {
            // Step 1: POST to fetch resource
            FormBody formBody = new FormBody.Builder()
                    .add("id", payload.get("id"))
                    .add("filename", payload.get("filename"))
                    .build();

            Request postRequest = new Request.Builder()
                    .url(FETCH_URL)
                    .post(formBody)
                    .header("User-Agent", getRandomUserAgent())
                    .build();

            Response postResponse = client.newCall(postRequest).execute();
            if (!postResponse.isSuccessful() || postResponse.body() == null) {
                System.out.println("❌ POST request failed");
                return null;
            }

            String responseText = postResponse.body().string();

            // Step 2: Extract redirect URL
            Pattern pattern = Pattern.compile("https://fs\\d+\\.oceanofpdf\\.com/[^\\s\"']+");
            Matcher matcher = pattern.matcher(responseText);

            if (!matcher.find()) {
                System.out.println("[!] No redirect URL found.");
                return null;
            }

            String redirectUrl = matcher.group(0);

            // Step 3: HEAD request with exponential backoff retry
            Response headResponse = null;
            for (int attempt = 0; attempt < maxRetries; attempt++) {
                try {
                    Request headRequest = new Request.Builder()
                            .url(redirectUrl)
                            .head()
                            .header("User-Agent", getRandomUserAgent())
                            .build();

                    headResponse = client.newCall(headRequest).execute();
                    if (headResponse.isSuccessful()) {
                        break;
                    }
                } catch (IOException e) {
                    if (attempt < maxRetries - 1) {
                        int waitTime = 5 * (int) Math.pow(2, attempt);
                        System.out.println("❌ HEAD request failed (attempt " + (attempt + 1) +
                                "/" + maxRetries + "): " + e.getMessage());
                        System.out.println("   Retrying in " + waitTime + " seconds...");
                        Thread.sleep(waitTime * 1000);
                    } else {
                        System.out.println("❌ HEAD request failed after " + maxRetries + " attempts");
                        return null;
                    }
                }
            }

            if (headResponse == null || !headResponse.isSuccessful()) {
                System.out.println("❌ HEAD request validation failed");
                return null;
            }

            // Step 4: Validate content-disposition
            String contentDisposition = headResponse.header("content-disposition", "");
            if (contentDisposition.contains("attachment") && contentDisposition.contains("filename=")) {
                long fileSize = Long.parseLong(headResponse.header("content-length", "0"));

                // Use parallel download for large files (> 5MB)
                if (fileSize > 5 * 1024 * 1024) {
                    return downloadEpubParallel(redirectUrl, client, downloadDir, fileSize);
                } else {
                    return downloadEpubFast(redirectUrl, client, downloadDir);
                }
            } else {
                System.out.println("❌ No valid downloadable attachment in headers.");
                return null;
            }

        } catch (Exception e) {
            System.err.println("❌ Download failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Parallel chunk download for large files
     */
    private static String downloadEpubParallel(String epubLink, OkHttpClient client,
                                               String downloadDir, long totalSize) {
        try {
            File dir = new File(downloadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // Extract filename
            Request headRequest = new Request.Builder()
                    .url(epubLink)
                    .head()
                    .header("User-Agent", getRandomUserAgent())
                    .build();

            Response headResponse = client.newCall(headRequest).execute();
            String filename = extractFilename(headResponse);
            if (filename == null) return null;

            File savePath = new File(dir, filename);

            // Skip if exists
            if (savePath.exists()) {
                System.out.println("⏭️  File already exists: " + savePath.getAbsolutePath());
                return savePath.getAbsolutePath();
            }

            // Calculate chunk ranges
            int numChunks = 4;
            long chunkSize = totalSize / numChunks;
            List<Future<byte[]>> chunkFutures = new ArrayList<>();

            System.out.println("📥 Starting parallel download: " + filename +
                    " (" + (totalSize / 1024 / 1024) + " MB)");

            for (int i = 0; i < numChunks; i++) {
                final long start = i * chunkSize;
                final long end = (i == numChunks - 1) ? totalSize - 1 : (start + chunkSize - 1);

                Future<byte[]> future = chunkExecutor.submit(() -> downloadChunk(epubLink, client, start, end));
                chunkFutures.add(future);
            }

            // Combine chunks
            try (FileOutputStream fos = new FileOutputStream(savePath)) {
                for (Future<byte[]> future : chunkFutures) {
                    byte[] chunk = future.get();
                    fos.write(chunk);
                }
            }

            System.out.println("✅ Parallel download complete: " + savePath.getAbsolutePath());
            return savePath.getAbsolutePath();

        } catch (Exception e) {
            System.err.println("❌ Parallel download failed: " + e.getMessage());
            return null;
        }
    }

    private static byte[] downloadChunk(String url, OkHttpClient client, long start, long end) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", getRandomUserAgent())
                .header("Range", "bytes=" + start + "-" + end)
                .build();

        Response response = client.newCall(request).execute();
        if (response.isSuccessful() && response.body() != null) {
            return response.body().bytes();
        }
        throw new IOException("Failed to download chunk " + start + "-" + end);
    }

    /**
     * Standard fast download for smaller files
     */
    private static String downloadEpubFast(String epubLink, OkHttpClient client, String downloadDir) {
        try {
            File dir = new File(downloadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // HEAD request for metadata
            Request headRequest = new Request.Builder()
                    .url(epubLink)
                    .head()
                    .header("User-Agent", getRandomUserAgent())
                    .build();

            Response headResponse = client.newCall(headRequest).execute();
            String filename = extractFilename(headResponse);
            if (filename == null) return null;

            File savePath = new File(dir, filename);

            // Skip if exists
            if (savePath.exists()) {
                System.out.println("⏭️  File already exists: " + savePath.getAbsolutePath());
                return savePath.getAbsolutePath();
            }

            long totalSize = Long.parseLong(headResponse.header("content-length", "0"));

            // Download file
            Request downloadRequest = new Request.Builder()
                    .url(epubLink)
                    .header("User-Agent", getRandomUserAgent())
                    .build();

            Response downloadResponse = client.newCall(downloadRequest).execute();
            if (!downloadResponse.isSuccessful() || downloadResponse.body() == null) {
                System.out.println("❌ Download request failed");
                return null;
            }

            try (InputStream inputStream = downloadResponse.body().byteStream();
                 FileOutputStream outputStream = new FileOutputStream(savePath)) {

                byte[] buffer = new byte[CHUNK_SIZE];
                long downloaded = 0;
                int bytesRead;

                System.out.println("📥 Downloading: " + filename);
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    downloaded += bytesRead;

                    // Progress indicator
                    if (totalSize > 0 && downloaded % (CHUNK_SIZE * 10) == 0) {
                        int progress = (int) ((downloaded * 100) / totalSize);
                        System.out.print("\rProgress: " + progress + "%");
                    }
                }
                System.out.println();
            }

            System.out.println("✅ Download complete: " + savePath.getAbsolutePath());
            return savePath.getAbsolutePath();

        } catch (Exception e) {
            System.err.println("❌ Download failed: " + e.getMessage());
            return null;
        }
    }

    private static String extractFilename(Response response) {
        String contentDisposition = response.header("content-disposition", "");
        Pattern pattern = Pattern.compile("filename=\"?([^\"]+)\"?");
        Matcher matcher = pattern.matcher(contentDisposition);

        if (matcher.find()) {
            String filename = matcher.group(1).replace("\"", "");
            return new File(filename).getName();
        }

        System.out.println("❌ No valid filename in headers");
        return null;
    }

    public static void shutdown() {
        chunkExecutor.shutdown();
        try {
            if (!chunkExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                chunkExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            chunkExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    // Helper class for file information
    private static class FileInfo {
        String fullBookName = "Unknown";
        double pdfSizeMB = 0;
        double epubSizeMB = 0;
    }

    // Add these methods to your DownloadUtils class

    /**
     * Fetch all Listopia categories from the main Listopia page
     * @param client OkHttpClient instance
     * @return List of Listopia objects containing category information
     */
    public List<Listopia> getAllListopia(OkHttpClient client) {
        List<Listopia> listopiaList = new ArrayList<>();
        String listopiaUrl = "https://oceanofpdf.com/listopia/";

        try {
            Log.d(TAG, "Fetching Listopia categories from: " + listopiaUrl);
            Document doc = fetchPage(listopiaUrl, client);

            if (doc == null) {
                Log.e(TAG, "Failed to fetch Listopia page");
                return listopiaList;
            }

            // Select all subcategory sections
            Elements subcategorySections = doc.select("div.subcategory-section");
            Log.d(TAG, "Found " + subcategorySections.size() + " Listopia categories");

            for (Element section : subcategorySections) {
                try {
                    // Get the category link and title
                    Element linkElement = section.selectFirst("div.subcategory-link a");
                    if (linkElement == null) continue;

                    String url = linkElement.attr("href");
                    String fullTitle = linkElement.text().trim();

                    // Extract book count from title (e.g., "Category Name (553)")
                    int bookCount = 0;
                    String title = fullTitle;

                    if (fullTitle.contains("(") && fullTitle.contains(")")) {
                        int startIdx = fullTitle.lastIndexOf("(");
                        int endIdx = fullTitle.lastIndexOf(")");
                        if (startIdx > 0 && endIdx > startIdx) {
                            title = fullTitle.substring(0, startIdx).trim();
                            String countStr = fullTitle.substring(startIdx + 1, endIdx).trim();
                            try {
                                bookCount = Integer.parseInt(countStr);
                            } catch (NumberFormatException e) {
                                Log.w(TAG, "Could not parse book count: " + countStr);
                            }
                        }
                    }

                    // Get thumbnail from first book in the list
                    String thumbnailUrl = null;
                    Element bookList = section.selectFirst("div.book-list");
                    if (bookList != null) {
                        Element firstBook = bookList.selectFirst("div.book");
                        if (firstBook != null) {
                            Element imgElement = firstBook.selectFirst("img");
                            if (imgElement != null) {
                                thumbnailUrl = imgElement.attr("data-src");
                                if (thumbnailUrl == null || thumbnailUrl.isEmpty()) {
                                    thumbnailUrl = imgElement.attr("src");
                                }
                            }
                        }
                    }

                    Listopia listopia = new Listopia(title, url, bookCount, thumbnailUrl);
                    listopiaList.add(listopia);

                    Log.d(TAG, "Added Listopia: " + title + " (" + bookCount + " books)");

                } catch (Exception e) {
                    Log.e(TAG, "Error parsing Listopia category", e);
                }
            }

            Log.d(TAG, "Total Listopia categories found: " + listopiaList.size());
            Thread.sleep(2000); // Rate limiting

        } catch (Exception e) {
            Log.e(TAG, "Error fetching Listopia categories", e);
        }

        return listopiaList;
    }

    /**
     * Fetch books from a specific Listopia category
     * @param listopiaUrl URL of the Listopia category
     * @param client OkHttpClient instance
     * @return List of BookInfo objects
     */
    public List<BookInfo> getBooksFromListopia(String listopiaUrl, OkHttpClient client) {
        List<BookInfo> books = new ArrayList<>();

        try {
            Log.d(TAG, "Fetching books from Listopia: " + listopiaUrl);

            // Get the last page number
            int lastPage = getLastPage(listopiaUrl, client);
            Log.d(TAG, "Total pages in Listopia: " + lastPage);

            // Fetch books from all pages
            for (int page = 1; page <= lastPage; page++) {
                String pageUrl = page == 1 ? listopiaUrl : listopiaUrl + "page/" + page + "/";

                Document doc = fetchPage(pageUrl, client);
                if (doc == null) continue;

                // Select book elements
                Elements bookElements = doc.select("div.subcategory-section div.book-list div.book");
                Log.d(TAG, "Found " + bookElements.size() + " books on page " + page);

                for (Element bookElement : bookElements) {
                    try {
                        Element bookCover = bookElement.selectFirst("div.book-cover a");
                        if (bookCover == null) continue;

                        String bookUrl = bookCover.attr("href");
                        if (bookUrl == null || bookUrl.isEmpty()) continue;

                        // Get book image
                        String imageUrl = null;
                        Element imgElement = bookCover.selectFirst("img");
                        if (imgElement != null) {
                            imageUrl = imgElement.attr("data-src");
                            if (imageUrl == null || imageUrl.isEmpty()) {
                                imageUrl = imgElement.attr("src");
                            }
                        }

                        // Fetch full book info
                        BookInfo bookInfo = getBookInfo(bookUrl, client);
                        if (bookInfo != null) {
                            // Use the image from Listopia if book info doesn't have one
                            if ((bookInfo.getBookimg() == null || bookInfo.getBookimg().isEmpty())
                                    && imageUrl != null && !imageUrl.isEmpty()) {
                                bookInfo.setBookimg(imageUrl);
                            }
                            books.add(bookInfo);
                            Log.d(TAG, "Added book: " + bookInfo.getTitle());
                        }

                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing book element", e);
                    }
                }

                Thread.sleep(2000); // Rate limiting between pages
            }

            Log.d(TAG, "Total books fetched from Listopia: " + books.size());

        } catch (Exception e) {
            Log.e(TAG, "Error fetching books from Listopia", e);
        }

        return books;
    }

    /**
     * Fetch books from a specific Listopia with a limit
     * @param listopiaUrl URL of the Listopia category
     * @param client OkHttpClient instance
     * @param maxBooks Maximum number of books to fetch (null for all)
     * @return List of BookInfo objects
     */
    public List<BookInfo> getBooksFromListopia(String listopiaUrl, OkHttpClient client, Integer maxBooks) {
        List<BookInfo> books = new ArrayList<>();
        ExecutorService bookExecutor = Executors.newFixedThreadPool(4); // For parallel book fetching

        try {
            int lastPage = getLastPage(listopiaUrl, client);
            Log.d(TAG, "Total pages in Listopia: " + lastPage);

            // Fetch books from pages until we reach the limit
            outerLoop:
            for (int page = 1; page <= lastPage; page++) {
                Log.d(TAG, "Fetching books from Listopia: " + listopiaUrl + "page/" + page);

                Document doc = fetchPage(listopiaUrl + "page/" + page, client);
                if (doc == null) continue;

                // Select book elements
                Elements bookElements = doc.select("article");
                Log.d(TAG, "Found " + bookElements.size() + " books on page " + page);
                List<String> bookUrls = new ArrayList<>();

                for (Element article : bookElements) {
                    // Check if we've reached the limit
                    if (maxBooks != null && books.size() >= maxBooks) {
                        break outerLoop;
                    }

                    Element header = article.selectFirst("header.entry-header");
                    if (header == null) continue;

                    Element aTag = header.selectFirst("a.entry-title-link[href]");
                    if (aTag == null) continue;

                    Element postmetainfo = article.selectFirst("div.postmetainfo");
                    if (postmetainfo == null) continue;

                    if (!isEnglish(postmetainfo)) {
                        continue;
                    }

                    bookUrls.add(aTag.attr("href"));
                }

                // Fetch book info in parallel
                List<Future<BookInfo>> futures = new ArrayList<>();
                for (String bookUrl : bookUrls) {
                    // Check again if we've reached the limit before submitting more tasks
                    if (maxBooks != null && books.size() >= maxBooks) {
                        break;
                    }

                    Future<BookInfo> future = bookExecutor.submit(() -> {
                        try {
                            BookInfo bookInfo = getBookInfo(bookUrl, client);

                            if (bookInfo != null) {
                                List<DownloadLink> downloadLink = bookInfo.getDownloadLinks();

                                if (downloadLink != null && !downloadLink.isEmpty()) {
                                    String result = fetchAndDownload(downloadLink, client, bookInfo, 3);
                                    if (result != null) {
                                        Log.d(TAG, "downloadLink: " + downloadLink);
                                        Log.d(TAG, "processBookInfo: " + result);
                                        bookInfo.setDownlink(result);
                                        downloadLink.get(0).setDownlink(result);
                                    }
                                }
                            }

                            return bookInfo;
                        } catch (Exception e) {
                            Log.e(TAG, "Error fetching book info for: " + bookUrl, e);
                            return null;
                        }
                    });

                    futures.add(future);
                }

                // Collect results
                for (Future<BookInfo> future : futures) {
                    try {
                        BookInfo bookInfo = future.get(15, TimeUnit.SECONDS);
                        if (bookInfo != null) {
                            books.add(bookInfo);

                            // Check if we've reached the limit after adding
                            if (maxBooks != null && books.size() >= maxBooks) {
                                break outerLoop;
                            }
                        }
                    } catch (TimeoutException e) {
                        Log.e(TAG, "Timeout getting book info from future", e);
                        future.cancel(true);
                    } catch (Exception e) {
                        Log.e(TAG, "Error getting book info from future", e);
                    }
                }

                Thread.sleep(2000); // Rate limiting between pages
            }

        } catch (Exception e) {
            Log.e(TAG, "Error fetching books from Listopia", e);
        } finally {
            // Clean up executor
            bookExecutor.shutdown();
            try {
                if (!bookExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                    bookExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                bookExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        return books;
    }
}
