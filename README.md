# EBook Reader - [![Build APK](https://github.com/kiduyu-klaus/ebook-reader-with-folioreader/actions/workflows/gradle-release.yml/badge.svg)](https://github.com/kiduyu-klaus/ebook-reader-with-folioreader/actions/workflows/gradle-release.yml)

A modern, feature-rich ebook reader application for Android that allows users to search, download, and read EPUB books with an intuitive Material Design interface.

## 📱 Features

### Core Functionality
- **Book Search & Download**: Search and download EPUB books from Ocean of PDF
- **EPUB Reader**: Built-in reader using FolioReader library with customizable settings
- **Library Management**: Organize and manage your personal book collection
- **Favorites System**: Mark books as favorites for quick access
- **Cover Extraction**: Automatically extracts and displays book cover images
- **Book Metadata**: Displays title, author, language, and file size information

### User Interface
- **Modern Material Design**: Clean, contemporary UI with gradient themes
- **Navigation Drawer**: Easy navigation between different sections
- **Fragment-based Architecture**: Smooth transitions between screens
- **Empty State Handling**: Helpful messages when no content is available
- **Progress Indicators**: Visual feedback during downloads and searches

### Advanced Features
- **Parallel Downloads**: Multi-threaded downloading for faster performance
- **Smart Search**: Filter and sort books by name, date, or size
- **Storage Management**: Cache clearing and storage information
- **Dark Mode Support**: Coming soon - UI prepared for dark theme
- **Permission Handling**: Proper runtime permission management for Android 11+

## 🏗️ Architecture

### Project Structure
```
app/src/main/java/com/kiduyu/klaus/ebookfinaldownload/
├── adapters/           # RecyclerView adapters
│   ├── BookAdapter.java
│   ├── BookListAdapter.java
│   └── DownloadLinkAdapter.java
├── fragments/          # App fragments
│   ├── HomeFragment.java
│   ├── SearchFragment.java
│   ├── MyBooksFragment.java
│   ├── FavoritesFragment.java
│   ├── SettingsFragment.java
│   ├── CategoriesFragment.java
│   └── RecentFragment.java
├── models/            # Data models
│   ├── BookInfo.java
│   ├── BookItem.java
│   └── DownloadLink.java
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
- **SearchFragment**: Search and download books
- **MyBooksFragment**: Display all downloaded books
- **FavoritesFragment**: Show favorited books
- **SettingsFragment**: App settings and preferences

#### Data Models
- **BookInfo**: Detailed book information from search results
- **BookItem**: Local book file information
- **DownloadLink**: Download link metadata

#### Utilities
- **EpubCoverExtractor**: Extracts cover images and metadata from EPUB files
- **DownloadUtils**: Handles book searching and downloading with OkHttp
- **DownloadEpub**: Permission handling and download management

## 🔧 Technologies & Libraries

### Core Dependencies
- **Android SDK**: API 23+ (Android 6.0 Marshmallow and above)
- **Target SDK**: 36
- **Language**: Java 17

### Major Libraries
```gradle
// UI Components
implementation 'com.google.android.material:material:1.x.x'
implementation 'androidx.constraintlayout:constraintlayout:2.x.x'
implementation 'androidx.cardview:cardview:1.x.x'
implementation 'androidx.recyclerview:recyclerview:1.x.x'

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

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/ebook-reader.git
   cd ebook-reader
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
- `READ_EXTERNAL_STORAGE` (API < 33)
- `READ_MEDIA_IMAGES` (API 33+)
- `READ_MEDIA_VIDEO` (API 33+)
- `READ_MEDIA_AUDIO` (API 33+)
- `POST_NOTIFICATIONS` (API 33+)

## 📖 Usage

### Searching for Books
1. Navigate to the **Search Books** section
2. Enter your search query (e.g., "The Alchemist")
3. Optionally specify the number of books to retrieve
4. Tap **Search Books**
5. Browse results and tap **Read Book** to download and open

### Managing Your Library
1. Go to **My Books** to see all downloaded books
2. Tap on a book to open it
3. Use the favorite icon to mark books as favorites
4. Use the delete button to remove books from your library

### Reading Books
- Tap any book to open it in the reader
- Swipe or tap to navigate pages
- Access highlighting and note-taking features
- Adjust reading settings from the reader menu

### Settings & Preferences
- **Dark Mode**: Toggle dark theme (applied on restart)
- **Auto Download**: Automatically save downloaded books
- **WiFi Only**: Restrict downloads to WiFi connections
- **EPUB Only**: Filter to show only EPUB format books
- **Clear Cache**: Free up storage space
- **Clear History**: Remove search and reading history

## 🎨 UI/UX Features

### Material Design
- Gradient backgrounds for visual appeal
- Rounded corners and elevated cards
- Consistent color scheme with Material palette
- Smooth animations and transitions

### User Feedback
- Loading indicators during operations
- Toast messages for quick confirmations
- Empty state screens with helpful messages
- Progress bars for downloads

### Navigation
- Bottom-aligned FAB for quick actions
- Navigation drawer with organized menu
- Breadcrumb-style navigation
- Back button handling

## 🔐 Security & Privacy

- **No data collection**: App doesn't collect or share personal data
- **Local storage**: All books stored locally on device
- **No analytics**: No tracking or usage analytics
- **Secure downloads**: Uses HTTPS for all network requests

## 🐛 Known Issues & Limitations

1. **Storage Permissions**: Android 11+ requires "Manage All Files" permission for some operations
2. **Download Source**: Currently limited to Ocean of PDF
3. **Format Support**: Only EPUB format is fully supported
4. **Search Limitations**: Depends on source website availability
5. **Language Filter**: Currently filters for English books only

## 🛠️ Development

### Building from Source
```bash
./gradlew assembleDebug
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

## 📝 Future Enhancements

- [ ] PDF format support
- [ ] Cloud backup and sync
- [ ] Reading statistics and goals
- [ ] Social features (reviews, ratings)
- [ ] Multiple download sources
- [ ] Enhanced search filters
- [ ] Reading lists and collections
- [ ] Night mode improvements
- [ ] Text-to-speech integration
- [ ] Bookmarks and annotations sync

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🙏 Acknowledgments

- **FolioReader**: EPUB reader implementation
- **OkHttp**: Networking library
- **Glide**: Image loading and caching
- **Jsoup**: HTML parsing for web scraping
- **Material Design**: UI/UX guidelines and components

## 📧 Contact

For questions, suggestions, or issues, please open an issue on GitHub or contact the maintainer.

## ⚠️ Disclaimer

This application is for educational purposes only. Users are responsible for ensuring they have the right to download and read any content accessed through this application. The developers do not host, distribute, or endorse any copyrighted material.

---

**Version**: 1.0.0  
**Last Updated**: December 2024  
**Minimum Android Version**: 6.0 (API 23)  
**Target Android Version**: 14+ (API 36)
