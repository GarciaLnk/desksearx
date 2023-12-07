# DeskSearX <img align="right" src="src/main/resources/com/garcialnk/desksearx/package/desksearx.png" alt="Logo generated by DALLE-3" width="128" height="128">

DeskSearX is a desktop search tool built with JavaFX, Apache Lucene, and Apache Tika for modern, efficient file searching on your personal computer.

## Features

- Index and search text documents, PDFs, emails, and more.
- Real-time file system monitoring for index updates.
- Multilingual search support with language-specific analyzers.
- Sleek user interface with MaterialFX styling.

## Prerequisites

- Java JDK 21 or higher.
- Gradle (to manage dependencies and run the build).

## Building

Clone the repository and navigate to the project directory:

```bash
git clone https://github.com/GarciaLnk/desksearx
cd desksearx
```

Build the project using Gradle:

```bash
./gradlew build
```

Run the application:

```bash
./gradlew run
```

## Usage

1. Start DeskSearX.
2. Open the settings and enter the directory paths you want to index.
3. Type your search query.
4. Press 'Search' to retrieve your results.