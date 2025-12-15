# OceanOfPdf App - [![Build APK](https://github.com/kiduyu-klaus/ebook-reader-with-folioreader/actions/workflows/gradle-release.yml/badge.svg)](https://github.com/kiduyu-klaus/ebook-reader-with-folioreader/actions/workflows/gradle-release.yml)

A modern, feature-rich ebook reader application for Android that allows users to search, download, and read EPUB books from OceanOfPDF with an intuitive Material Design interface.

## 📱 Features

### Core Functionality
- **Book Search & Download**: Search and download EPUB books directly from OceanOfPDF
- **EPUB Reader**: Built-in reader using FolioReader library with customizable settings
- **Library Management**: Organize and manage your personal book collection
- **Favorites System**: Mark books as favorites for quick access
- **Genre Browsing**: Browse books by genre/category
- **Author Search**: Search books by specific authors
- **Listopia Collections**: Access curated book collections
- **New Releases**: Stay updated with latest book releases
- **Cover Extraction**: Automatically extracts and displays book cover images
- **Book Metadata**: Displays title, author, language, series info, and file size

### User Interface
- **Modern Material Design**: Clean, contemporary UI with gradient themes
- **Navigation Drawer**: Easy navigation between different sections
- **Fragment-based Architecture**: Smooth transitions between screens
- **Empty State Handling**: Helpful messages when no content is available
- **Progress Indicators**: Visual feedback during downloads and searches
- **Swipe to Refresh**: Pull-to-refresh on book lists
- **Smart Pagination**: Load books page by page with prefetching

### Advanced Features
- **Parallel Processing**: Multi-threaded downloading and scraping for faster performance
- **Smart Search**: Filter and sort books by name, date, or size
- **Language Filtering**: Automatically filters for English books
- **Download Link Validation**: Verifies download links before presenting to user
- **Caching System**: Page caching for faster browsing
- **Storage Management**: Cache clearing and storage information
- **Auto Update Checker**: Checks for new app versions from GitHub
- **Dark Mode Support**: Coming soon - UI prepared for dark theme
- **Permission Handling**: Proper runtime permission management for Android 11+

## 🏗️ Architecture

### Project Structure
```
app/src/main/java/com/kiduyu/klaus/ebookfinaldownload/
├── adapters/           # RecyclerView adapters
│   ├── BookAdapter.java
│   ├── BookListAdapter.java
│   ├── DownloadLinkAdapter.java
│   ├── GenreAdapter.java
│   └── ListopiaAdapter.java
├── fragments/          # App fragments
│   ├── HomeFragment.java
│   ├── SearchFragment.java
│   ├── SearchAuthorFragment.java
│   ├── MyBooksFragment.java
│   ├── FavoritesFragment.java
│   ├── CategoriesFragment.java
│   ├── GenreBooksFragment.java
│   ├── ListopiaFragment.java
│   ├── NewReleasesFragment.java
│   ├── SettingsFragment.java
│   └── RecentFragment.java
├── models/            # Data models
│   ├── BookInfo.java
│   ├── BookItem.java
│   ├── DownloadLink.java
│   ├── Genre.java
│   └── Listopia.java
├── utils/             # Utility classes
│   ├── DownloadUtils.java
│   ├── DownloadEpub.java
│   ├── EpubCoverExtractor.java
│   ├── ProgressDialog.java
│   └── FileAccessPermissionHelper.java
├── MainActivity.java
├── ReadBook.java
└── SearchBook.java
```

### Key Components

#### Activities
- **MainActivity**: Main container with navigation drawer and fragment management
- **ReadBook**: EPUB reader interface using FolioReader
- **SearchBook**: Standalone search activity (legacy)

#### Fragments
- **HomeFragment**: Dashboard with quick actions and library stats
- **SearchFragment**: Search and download books by title/keyword
- **SearchAuthorFragment**: Search books by author name
- **MyBooksFragment**: Display all downloaded books
- **FavoritesFragment**: Show favorited books
- **CategoriesFragment**: Browse all available genres
- **GenreBooksFragment**: View books in a specific genre with pagination
- **ListopiaFragment**: Browse curated book collections
- **NewReleasesFragment**: Latest book releases with infinite scroll
- **SettingsFragment**: App settings and preferences

#### Data Models
- **BookInfo**: Detailed book information from OceanOfPDF
- **BookItem**: Local book file information
- **DownloadLink**: Download link metadata with validation
- **Genre**: Genre/category information
- **Listopia**: Curated book collection metadata

#### Utilities
- **EpubCoverExtractor**: Extracts cover images and metadata from EPUB files
- **DownloadUtils**: Handles book searching, downloading, and scraping with OkHttp
- **DownloadEpub**: Permission handling and download management

## 🔧 Technologies & Libraries

### Core Dependencies
- **Android SDK**: API 23+ (Android 6.0 Marshmallow and above)
- **Target SDK**: 36
- **Language**: Java 17
- **Build System**: Gradle

### Major Libraries
```gradle
// UI Components
implementation 'com.google.android.material:material:1.x.x'
implementation 'androidx.constraintlayout:constraintlayout:2.x.x'
implementation 'androidx.cardview:cardview:1.x.x'
implementation 'androidx.recyclerview:recyclerview:1.x.x'
implementation 'androidx.swiperefreshlayout:swiperefreshlayout:1.2.0'

// EPUB Reader
implementation project(':folioreader')

// Image Loading
implementation 'com.github.bumptech.glide:glide:4.x.x'
annotationProcessor 'com.github.bumptech.glide:compiler:4.x.x'

// Networking
implementation 'com.squareup.okhttp3:okhttp:4.x.x'
implementation 'io.github.lizhangqu:coreprogress:1.x.x'

// HTML Parsing
implementation 'org.jsoup:jsoup:1.21.2'

// JSON Processing
implementation 'com.fasterxml.jackson.core:jackson-core:2.x.x'
implementation 'com.fasterxml.jackson.core:jackson-annotations:2.x.x'
implementation 'com.fasterxml.jackson.core:jackson-databind:2.x.x'
```

## 🚀 Getting Started

### Prerequisites
- Android Studio Arctic Fox or later
- JDK 17
- Android SDK with API level 23+
- FolioReader library (included as module)

### Installation

#### Option 1: Download Pre-built APK
1. Go to the [Releases](https://github.com/kiduyu-klaus/ebook-reader-with-folioreader/releases) page
2. Download the latest `ebook-reader-vX.X-release-signed.apk`
3. Install on your Android device (enable "Install from Unknown Sources" if needed)

#### Option 2: Build from Source

1. **Clone the repository**
   ```bash
   git clone https://github.com/kiduyu-klaus/ebook-reader-with-folioreader.git
   cd ebook-reader-with-folioreader
   ```

2. **Open in Android Studio**
    - Launch Android Studio
    - Select "Open an existing project"
    - Navigate to the cloned directory

3. **Sync Gradle**
    - Wait for Gradle to sync dependencies
    - Resolve any dependency conflicts if prompted

4. **Run the app**
    - Connect an Android device or start an emulator
    - Click the "Run" button or press Shift+F10

### Configuration

#### Storage Location
Books are stored in the app's external files directory:
```
/storage/emulated/0/Android/data/com.kiduyu.klaus.ebookfinaldownload/files/
```

#### Permissions Required
- `INTERNET` - Download books from OceanOfPDF
- `ACCESS_NETWORK_STATE` - Check network connectivity
- `ACCESS_WIFI_STATE` - WiFi-only download feature
- `READ_EXTERNAL_STORAGE` (API < 33)
- `WRITE_EXTERNAL_STORAGE` - Save downloaded books
- `MANAGE_EXTERNAL_STORAGE` (API 30+)
- `READ_MEDIA_IMAGES` (API 33+)
- `FOREGROUND_SERVICE` - Background downloads
- `FOREGROUND_SERVICE_DATA_SYNC` (API 34+)
- `POST_NOTIFICATIONS` (API 33+) - Download completion notifications

## 📖 Usage

### Searching for Books
1. Navigate to the **Search Books** section
2. Enter your search query (e.g., "The Alchemist")
3. Optionally specify the number of books to retrieve
4. Tap **Search Books**
5. Browse results and tap **Read** to download and open

### Searching by Author
1. Go to **Search Authors**
2. Enter author's first and last name (e.g., "Paulo Coelho")
3. Optionally limit number of results
4. Tap **Search**
5. Browse all books by that author

### Browsing by Genre
1. Navigate to **Browse Genres**
2. Use the search box to filter genres
3. Tap on any genre to see books in that category
4. Use Previous/Next buttons to paginate through results

### Exploring Collections
1. Go to **Listopia Books**
2. Browse curated collections
3. Tap on a collection to see books
4. Download books directly from collections

### Checking New Releases
1. Navigate to **New Releases**
2. Scroll to browse latest books
3. Pull down to refresh
4. Tap "Load More" for additional releases

### Managing Your Library
1. Go to **My Books** to see all downloaded books
2. Tap on a book to open it
3. Use the favorite icon to mark books as favorites
4. Use the delete button to remove books from your library
5. Filter by format (All/EPUB/PDF)
6. Sort by name, date, or size

### Reading Books
- Tap any book to open it in the reader
- Swipe or tap to navigate pages
- Access highlighting and note-taking features
- Adjust reading settings from the reader menu
- Resume where you left off

### Settings & Preferences
- **Dark Mode**: Toggle dark theme (applied on restart)
- **Auto Download**: Automatically save downloaded books
- **WiFi Only**: Restrict downloads to WiFi connections
- **Notifications**: Enable/disable download notifications
- **EPUB Only**: Filter to show only EPUB format books
- **Clear Cache**: Free up storage space
- **Clear History**: Remove search and reading history

## 🎨 UI/UX Features

### Material Design
- Gradient backgrounds for visual appeal
- Rounded corners and elevated cards
- Consistent color scheme with Material palette
- Smooth animations and transitions
- Floating Action Buttons for quick actions

### User Feedback
- Loading indicators during operations
- Toast messages for quick confirmations
- Empty state screens with helpful messages
- Progress bars for downloads and page loading
- Swipe-to-refresh on lists

### Navigation
- Bottom-aligned FAB for quick actions
- Navigation drawer with organized menu
- Breadcrumb-style navigation
- Back button handling
- Fragment-based navigation

### Performance Optimizations
- Page prefetching for smooth browsing
- Image caching with Glide
- Parallel book processing
- Connection pooling
- Rate limiting to prevent server overload

## 🔐 Security & Privacy

- **No data collection**: App doesn't collect or share personal data
- **Local storage**: All books stored locally on device
- **No analytics**: No tracking or usage analytics
- **Secure downloads**: Uses HTTPS for all network requests
- **Open source**: Full source code available for inspection
- **No ads**: Completely ad-free experience

## 🐛 Known Issues & Limitations

1. **Storage Permissions**: Android 11+ requires "Manage All Files" permission for some operations
2. **Download Source**: Currently limited to OceanOfPDF
3. **Format Support**: Only EPUB format is fully supported
4. **Search Limitations**: Depends on OceanOfPDF website availability
5. **Language Filter**: Currently filters for English books only
6. **Rate Limiting**: Excessive requests may be temporarily blocked by the source
7. **Cover Images**: Some books may not have cover images available

## 🛠️ Development

### Building from Source
```bash
# Debug build
./gradlew assembleDebug

# Release build (requires signing)
./gradlew assembleRelease
```

### Running Tests
```bash
./gradlew test
./gradlew connectedAndroidTest
```

### Code Style
- Follow standard Java conventions
- Use meaningful variable and method names
- Comment complex logic
- Keep methods focused and concise
- Maximum line length: 120 characters

### Continuous Integration
The project uses GitHub Actions for:
- Automatic APK building on push to main
- Signed release APK generation
- Version management via VERSION file
- Automated GitHub Releases

## 📈 Future Enhancements

- [ ] PDF format support with PDF reader
- [ ] Cloud backup and sync across devices
- [ ] Reading statistics and goals
- [ ] Social features (reviews, ratings, recommendations)
- [ ] Multiple download sources
- [ ] Enhanced search filters (year, rating, pages)
- [ ] Reading lists and custom collections
- [ ] Full dark mode implementation
- [ ] Text-to-speech integration
- [ ] Bookmarks and annotations sync
- [ ] Offline mode indicator
- [ ] Advanced reader customization
- [ ] Translation feature
- [ ] Series tracking

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

### Contribution Guidelines
- Write clear commit messages
- Update documentation as needed
- Add tests for new features
- Follow existing code style
- Test on multiple Android versions

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🙏 Acknowledgments

- **OceanOfPDF**: Source for ebook downloads
- **FolioReader**: EPUB reader implementation
- **OkHttp**: Networking library by Square
- **Glide**: Image loading and caching by Bumptech
- **Jsoup**: HTML parsing for web scraping
- **Material Design**: UI/UX guidelines and components by Google

## 📧 Contact & Support

- **Issues**: [GitHub Issues](https://github.com/kiduyu-klaus/ebook-reader-with-folioreader/issues)
- **Discussions**: [GitHub Discussions](https://github.com/kiduyu-klaus/ebook-reader-with-folioreader/discussions)
- **Email**: support@oceanofpdf-app.com (if available)

## ⚠️ Disclaimer

This application is for educational purposes only. Users are responsible for ensuring they have the right to download and read any content accessed through this application. The developers do not host, distribute, or endorse any copyrighted material. This app simply provides an interface to access publicly available content from OceanOfPDF.

**Respect copyright laws in your jurisdiction. Only download books that are in the public domain or for which you have the legal right to access.**

---

**App Name**: OceanOfPdf App  
**Version**: 1.1  
**Last Updated**: December 2024  
**Minimum Android Version**: 6.0 (API 23)  
**Target Android Version**: 14+ (API 36)  
**Package Name**: com.kiduyu.klaus.ebookfinaldownload

## 📸 Screenshots

[Coming Soon - Add screenshots of the app in action]

## 🌟 Star History

[![Star History Chart](https://api.star-history.com/svg?repos=kiduyu-klaus/ebook-reader-with-folioreader&type=Date)](https://star-history.com/#kiduyu-klaus/ebook-reader-with-folioreader&Date)

---

Made with ❤️ for book lovers everywhere