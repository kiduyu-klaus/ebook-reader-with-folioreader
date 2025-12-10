package com.kiduyu.klaus.ebookfinaldownload.utils;





import com.kiduyu.klaus.ebookfinaldownload.models.BookInfo;
import com.kiduyu.klaus.ebookfinaldownload.models.DownloadLink;

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
    public static String fetchAndDownload(List<DownloadLink>  payload, OkHttpClient client,
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
            epubLink = payload.get(0);;
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
}
