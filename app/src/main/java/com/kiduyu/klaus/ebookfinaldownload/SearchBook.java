package com.kiduyu.klaus.ebookfinaldownload;

import android.content.Context;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.kiduyu.klaus.ebookfinaldownload.adapters.BookAdapter;
import com.kiduyu.klaus.ebookfinaldownload.models.BookInfo;
import com.kiduyu.klaus.ebookfinaldownload.models.DownloadLink;
import com.kiduyu.klaus.ebookfinaldownload.utils.DownloadEpub;
import com.kiduyu.klaus.ebookfinaldownload.utils.DownloadUtils;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import okhttp3.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SearchBook extends AppCompatActivity {
    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    private static final String BASE_URL = "https://oceanofpdf.com/page/1/?s=";
    private static final int DEFAULT_TIMEOUT = 15000;
    private static final int MAX_PARALLEL_PAGES = 5;

    private static final int STORAGE_PERMISSION_CODE = 101;

    private OkHttpClient client;
    private ExecutorService pageExecutor;
    private Handler mainHandler;

    // UI Elements
    private EditText searchInput;
    private EditText numBooksInput;
    private Button searchButton;
    private TextView statusText;
    private TextView bookCountText;
    private ProgressBar progressBar;
    private RecyclerView booksRecyclerView;
    private LinearLayout emptyStateLayout;

    // Adapter
    private BookAdapter bookAdapter;
    private List<BookInfo> booksList;

    private AtomicInteger booksFound;
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
    DownloadEpub downloadEpub = new DownloadEpub(this);
    /**
     * Get a random user agent string
     */
    public static String getRandomUserAgent() {
        return USER_AGENTS[random.nextInt(USER_AGENTS.length)];
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_search_book);

        initializeViews();
        initializeExecutors();
        setupRecyclerView();
        setupListeners();
        checkAndRequestPermissions();

        this.mainHandler = new Handler(Looper.getMainLooper());
        this.booksFound = new AtomicInteger(0);
    }

    private void checkAndRequestPermissions() {
        downloadEpub.checkAndRequestPermissions();
    }

    private void setupListeners() {
        searchButton.setOnClickListener(v -> {
            // Hide keyboard when Search is clicked
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(searchInput.getWindowToken(), 0);
            }
            String query = searchInput.getText().toString().trim();
            //searchInput.setText();
            String numBooksStr = numBooksInput.getText().toString().trim();

            if (query.isEmpty()) {
                Toast.makeText(this, "Please enter a search query", Toast.LENGTH_SHORT).show();
                return;
            }

            Integer numBooks = null;
            if (!numBooksStr.isEmpty()) {
                try {
                    numBooks = Integer.parseInt(numBooksStr);
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Invalid number of books", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startSearch(query, numBooks);
            }
        });

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startSearch(String searchQuery, Integer firstNBooks) {
        searchButton.setEnabled(false);
        progressBar.setVisibility(ProgressBar.VISIBLE);
        booksFound.set(0);

        // Clear previous results
        bookAdapter.clearBooks();
        emptyStateLayout.setVisibility(View.GONE);
        booksRecyclerView.setVisibility(View.VISIBLE);

        updateStatus("Starting search for: " + searchQuery);

        new Thread(() -> {
            try {
                searchAndPrintBooksParallel(searchQuery, null, null, null, false, firstNBooks);
            } catch (Exception e) {
                updateStatus("Error: " + e.getMessage());
                e.printStackTrace();
            } finally {
                mainHandler.post(() -> {
                    searchButton.setEnabled(true);
                    progressBar.setVisibility(ProgressBar.GONE);

                    // Show empty state if no books found
                    if (booksList.isEmpty()) {
                        booksRecyclerView.setVisibility(View.GONE);
                        emptyStateLayout.setVisibility(View.VISIBLE);
                    }
                });
            }
        }).start();
    }

    public void searchAndPrintBooksParallel(String searchQuery, Integer startPage,
                                            Integer stopPage, Integer maxPages,
                                            boolean firstOnly, Integer firstNBooks) throws Exception {

        String encodedQuery = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            encodedQuery = URLEncoder.encode(searchQuery, StandardCharsets.UTF_8);
        }
        String fullUrl = BASE_URL + encodedQuery;

        int lastPage = getLastPage(fullUrl);
        updateStatus("Detected " + lastPage + " pages");

        int start = (startPage != null) ? startPage : 1;
        int stop = (stopPage != null) ? stopPage : lastPage;
        if (maxPages != null) {
            stop = Math.min(start + maxPages - 1, stop);
        }

        updateStatus("Searching pages " + start + " to " + stop);

        // Create a queue for book processing
        BlockingQueue<BookTask> bookQueue = new LinkedBlockingQueue<>();
        CountDownLatch latch = new CountDownLatch(stop - start + 1);

        // Submit page crawling tasks in parallel
        for (int page = start; page <= stop; page++) {
            final int currentPage = page;
            final String currentQuery = encodedQuery;

            pageExecutor.submit(() -> {
                try {
                    crawlPage(currentPage, currentQuery, bookQueue, firstOnly, firstNBooks);
                } catch (Exception e) {
                    updateStatus("Error on page " + currentPage + ": " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        // Process books as they come in
        boolean searchComplete = false;
        while (!searchComplete || !bookQueue.isEmpty()) {
            BookTask task = bookQueue.poll(1, TimeUnit.SECONDS);

            if (task != null) {
                processBookInfo(task);

                // Check if we've reached the limit
                if (firstNBooks != null && booksFound.get() >= firstNBooks) {
                    break;
                }
            }

            // Check if all pages have been crawled
            searchComplete = latch.getCount() == 0;
        }

        updateStatus("✅ Search complete! Found " + booksFound.get() + " books");
    }


    private void processBookInfo(BookTask task) {
        try {
            String downloadDir = "";
            BookInfo bookInfo = getBookInfo(task.bookUrl);
            List<DownloadLink>  downloadLink=bookInfo.getDownloadLinks();
            String result = DownloadUtils.fetchAndDownload(downloadLink, client, bookInfo, 3);
            if (result != null) {
                Log.e("TAG", "downloadLink: "+downloadLink);
                Log.e("TAG", "processBookInfo: "+result);
                bookInfo.setDownlink(result);

            }



            if (bookInfo != null) {
                int count = booksFound.incrementAndGet();

                // Add book to adapter on main thread
                mainHandler.post(() -> {
                    bookAdapter.addBook(bookInfo);
                    updateBookCount(count);
                    updateStatus("Found: " + bookInfo.getTitle());
                });
            }
        } catch (Exception e) {
            updateStatus("Error processing book: " + e.getMessage());
        }
    }

    private BookInfo getBookInfo(String bookUrl) {
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

            // Extract book details
            Element entryContent = doc.selectFirst("div.entry-content");
            if (entryContent != null) {
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

                    DownloadLink link = new DownloadLink();
                    link.setId(idInput.attr("value"));
                    link.setFilename(filename);
                    link.setFormat(fileExt);

                    info.addDownloadLink(link);
                }
            }

            Thread.sleep(1500);
            return info;

        } catch (Exception e) {
            updateStatus("Error extracting book info: " + e.getMessage());
            return null;
        }
    }


    private void initializeExecutors() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS)
                .readTimeout(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS)
                .followRedirects(true)
                .connectionPool(new ConnectionPool(10, 5, TimeUnit.MINUTES))
                .build();

        this.pageExecutor = Executors.newFixedThreadPool(MAX_PARALLEL_PAGES);
    }

    private void setupRecyclerView() {
        booksList = new ArrayList<>();
        bookAdapter = new BookAdapter(this, booksList);
        booksRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        booksRecyclerView.setAdapter(bookAdapter);
    }

    private void initializeViews() {
        searchInput = findViewById(R.id.searchInput);
        numBooksInput = findViewById(R.id.numBooksInput);
        searchButton = findViewById(R.id.searchButton);
        statusText = findViewById(R.id.statusText);
        bookCountText = findViewById(R.id.downloadCountText);
        progressBar = findViewById(R.id.progressBar);
        booksRecyclerView = findViewById(R.id.booksRecyclerView);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
    }

    private void crawlPage(int page, String encodedQuery, BlockingQueue<BookTask> bookQueue,
                           boolean firstOnly, Integer firstNBooks) throws Exception {
        String pageUrl = "https://oceanofpdf.com/page/" + page + "/?s=" + encodedQuery;

        Document doc = fetchPage(pageUrl);
        if (doc == null) return;

        Elements articles = doc.select("article");

        if (firstOnly && !articles.isEmpty()) {
            articles = new Elements(articles.get(0));
        } else if (firstNBooks != null) {
            int remaining = firstNBooks - booksFound.get();
            if (remaining <= 0) return;
            articles = new Elements(articles.subList(0, Math.min(remaining, articles.size())));
        }

        for (Element article : articles) {
            if (firstNBooks != null && booksFound.get() >= firstNBooks) {
                break;
            }

            Element header = article.selectFirst("header.entry-header");
            if (header == null) continue;

            Element aTag = header.selectFirst("a.entry-title-link[href]");
            if (aTag == null) continue;

            Element postmetainfo = article.selectFirst("div.postmetainfo");
            if (postmetainfo == null) continue;

            // Language filtering
            if (!isEnglish(postmetainfo)) {
                continue;
            }

            String bookUrl = aTag.attr("href");
            String bookTitle = aTag.text();
            BookTask task = new BookTask(bookUrl, bookTitle, page);
            bookQueue.offer(task);
        }

        Thread.sleep(2000); // Rate limiting
    }
    private boolean isEnglish(Element postmetainfo) {
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

    private int getLastPage(String url) throws Exception {
        Document doc = fetchPage(url);
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

    private Document fetchPage(String url) {
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
            updateStatus("[!] Failed to fetch " + url + ": " + e.getMessage() + "\n");
        }
        return null;
    }

    private void updateStatus(String message) {
        mainHandler.post(() -> statusText.setText(message));
    }

    private void updateBookCount(int count) {
        mainHandler.post(() -> bookCountText.setText("Books Found: " + count));
    }

    // Helper classes
    private static class BookTask {
        String bookUrl;
        String bookTitle;
        int pageNumber;

        BookTask(String bookUrl, String bookTitle, int pageNumber) {
            this.bookUrl = bookUrl;
            this.bookTitle = bookTitle;
            this.pageNumber = pageNumber;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pageExecutor != null && !pageExecutor.isShutdown()) {
            pageExecutor.shutdown();
        }
    }
}