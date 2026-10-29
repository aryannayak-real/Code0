package com.example.data.repository

import android.content.Context
import com.example.data.local.ProjectDao
import com.example.data.model.FileSearchResult
import com.example.data.model.ProjectEntity
import com.example.data.model.ProjectFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ProjectRepository(
    private val context: Context,
    private val projectDao: ProjectDao
) {
    val projectsFlow: Flow<List<ProjectEntity>> = projectDao.getAllProjectsFlow()

    private val projectsRoot: File by lazy {
        File(context.filesDir, "code0_projects").apply {
            if (!exists()) mkdirs()
        }
    }

    suspend fun getProject(id: String): ProjectEntity? = withContext(Dispatchers.IO) {
        projectDao.getProjectById(id)
    }

    suspend fun createProject(
        name: String,
        templateType: String,
        description: String = ""
    ): ProjectEntity = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val projectDir = File(projectsRoot, id).apply { mkdirs() }

        when (templateType) {
            "Android / Kotlin" -> populateAndroidStarter(projectDir, name)
            "Python CLI" -> populatePythonStarter(projectDir, name)
            "Web (HTML/CSS/JS)" -> populateWebStarter(projectDir, name)
            "TypeScript / Node" -> populateTypeScriptStarter(projectDir, name)
            else -> populateEmptyStarter(projectDir, name)
        }

        val detectedType = detectProjectType(projectDir)
        val (count, size) = calculateProjectStats(projectDir)

        val project = ProjectEntity(
            id = id,
            name = name.ifBlank { "Untitled Project" },
            directoryPath = projectDir.absolutePath,
            projectType = detectedType,
            description = description.ifBlank { "Created with $templateType template" },
            createdAt = System.currentTimeMillis(),
            lastModifiedAt = System.currentTimeMillis(),
            fileCount = count,
            totalSize = size
        )

        projectDao.insertProject(project)
        project
    }

    suspend fun deleteProject(project: ProjectEntity) = withContext(Dispatchers.IO) {
        val dir = File(project.directoryPath)
        if (dir.exists()) {
            dir.deleteRecursively()
        }
        projectDao.deleteProject(project)
    }

    suspend fun loadProjectFiles(projectDir: File): List<ProjectFile> = withContext(Dispatchers.IO) {
        if (!projectDir.exists()) return@withContext emptyList()
        scanDirectory(projectDir, projectDir)
    }

    private fun scanDirectory(dir: File, rootDir: File): List<ProjectFile> {
        val files = dir.listFiles()?.sortedWith(
            compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase() }
        ) ?: return emptyList()

        return files.map { file ->
            val relPath = file.relativeTo(rootDir).path
            ProjectFile(
                name = file.name,
                path = file.absolutePath,
                relativePath = relPath,
                isDirectory = file.isDirectory,
                size = if (file.isDirectory) 0L else file.length(),
                lastModified = file.lastModified(),
                extension = file.extension,
                children = if (file.isDirectory) scanDirectory(file, rootDir) else emptyList()
            )
        }
    }

    suspend fun readFileContent(filePath: String): String = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (!file.exists() || file.isDirectory) return@withContext ""
        file.readText()
    }

    suspend fun writeFileContent(filePath: String, content: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            file.parentFile?.mkdirs()
            file.writeText(content)
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun createFile(projectDir: File, relativePath: String, content: String = ""): Boolean = withContext(Dispatchers.IO) {
        try {
            val target = File(projectDir, relativePath)
            target.parentFile?.mkdirs()
            target.writeText(content)
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun createDirectory(projectDir: File, relativePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val target = File(projectDir, relativePath)
            target.mkdirs()
        } catch (_: Exception) {
            false
        }
    }

    suspend fun deleteFileOrFolder(targetPath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val target = File(targetPath)
            if (target.isDirectory) target.deleteRecursively() else target.delete()
        } catch (_: Exception) {
            false
        }
    }

    suspend fun renameFile(oldPath: String, newName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val oldFile = File(oldPath)
            val newFile = File(oldFile.parentFile, newName)
            oldFile.renameTo(newFile)
        } catch (_: Exception) {
            false
        }
    }

    suspend fun searchProjectFiles(
        projectDir: File,
        query: String,
        caseSensitive: Boolean = false,
        maxResults: Int = 200
    ): List<FileSearchResult> = withContext(Dispatchers.IO) {
        if (query.isBlank() || !projectDir.exists()) return@withContext emptyList()
        val results = mutableListOf<FileSearchResult>()

        // Ignored directories to maintain speed and safety
        val ignoredDirs = setOf(".git", ".gradle", "build", "node_modules", ".idea", ".bin")

        projectDir.walkTopDown()
            .onEnter { dir -> !ignoredDirs.contains(dir.name) }
            .filter { it.isFile }
            .forEach { file ->
                if (results.size >= maxResults) return@forEach
                // Only scan text-like files under 1MB
                if (file.length() in 1..1_000_000L && isLikelyTextFile(file)) {
                    try {
                        var lineIndex = 0
                        file.useLines { lines ->
                            for (line in lines) {
                                lineIndex++
                                val matchIdx = if (caseSensitive) {
                                    line.indexOf(query)
                                } else {
                                    line.indexOf(query, ignoreCase = true)
                                }
                                if (matchIdx != -1) {
                                    results.add(
                                        FileSearchResult(
                                            filePath = file.absolutePath,
                                            relativePath = file.relativeTo(projectDir).path,
                                            fileName = file.name,
                                            lineNumber = lineIndex,
                                            lineContent = line.trim(),
                                            matchStartIndex = matchIdx,
                                            matchLength = query.length
                                        )
                                    )
                                    if (results.size >= maxResults) break
                                }
                            }
                        }
                    } catch (_: Exception) {}
                }
            }
        results
    }

    private fun isLikelyTextFile(file: File): Boolean {
        val ext = file.extension.lowercase()
        val textExtensions = setOf(
            "kt", "kts", "java", "py", "js", "ts", "jsx", "tsx", "html", "htm", "css", "scss",
            "json", "xml", "md", "txt", "yaml", "yml", "sh", "properties", "gradle", "toml",
            "sql", "php", "c", "cpp", "h", "hpp", "rs", "go", "rb", "env"
        )
        return ext in textExtensions || ext.isBlank()
    }

    fun calculateProjectStats(projectDir: File): Pair<Int, Long> {
        var count = 0
        var totalBytes = 0L
        if (projectDir.exists()) {
            projectDir.walkTopDown().forEach {
                if (it.isFile) {
                    count++
                    totalBytes += it.length()
                }
            }
        }
        return Pair(count, totalBytes)
    }

    suspend fun updateProjectStats(projectId: String) = withContext(Dispatchers.IO) {
        val project = projectDao.getProjectById(projectId) ?: return@withContext
        val (count, size) = calculateProjectStats(File(project.directoryPath))
        projectDao.updateProject(
            project.copy(
                fileCount = count,
                totalSize = size,
                lastModifiedAt = System.currentTimeMillis()
            )
        )
    }

    fun detectProjectType(projectDir: File): String {
        if (!projectDir.exists()) return "General"
        val hasGradle = File(projectDir, "build.gradle.kts").exists() || File(projectDir, "build.gradle").exists()
        val hasManifest = File(projectDir, "AndroidManifest.xml").exists() ||
                File(projectDir, "app/src/main/AndroidManifest.xml").exists()
        if (hasGradle && hasManifest) return "Android / Kotlin"
        if (File(projectDir, "requirements.txt").exists() || File(projectDir, "main.py").exists() || File(projectDir, "pyproject.toml").exists()) return "Python"
        if (File(projectDir, "tsconfig.json").exists()) return "TypeScript"
        if (File(projectDir, "package.json").exists()) return "JavaScript / Node"
        if (File(projectDir, "index.html").exists()) return "Web (HTML/CSS)"
        if (File(projectDir, "CMakeLists.txt").exists()) return "C / C++"
        if (File(projectDir, "pom.xml").exists()) return "Java / Maven"
        return "General Project"
    }

    suspend fun exportProjectZip(project: ProjectEntity, targetZipFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val sourceDir = File(project.directoryPath)
            if (!sourceDir.exists()) return@withContext false

            ZipOutputStream(FileOutputStream(targetZipFile)).use { zos ->
                sourceDir.walkTopDown().forEach { file ->
                    if (file.isFile) {
                        val relPath = file.relativeTo(sourceDir).path
                        val entry = ZipEntry(relPath)
                        zos.putNextEntry(entry)
                        FileInputStream(file).use { fis -> fis.copyTo(zos) }
                        zos.closeEntry()
                    }
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    // Template initializers
    private fun populateAndroidStarter(dir: File, appName: String) {
        val safeName = appName.replace(Regex("[^a-zA-Z0-9]"), "")
        val pkg = "com.code0.${safeName.lowercase().ifBlank { "app" }}"

        File(dir, "README.md").writeText(
            "# $appName\n\nAndroid application created in Code0 mobile studio.\n"
        )
        File(dir, "app/src/main/AndroidManifest.xml").apply {
            parentFile?.mkdirs()
            writeText(
                """<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application
        android:label="$appName"
        android:theme="@android:style/Theme.Material.Light.NoActionBar">
        <activity
            android:name="$pkg.MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>"""
            )
        }

        File(dir, "app/src/main/java/${pkg.replace('.', '/')}/MainActivity.kt").apply {
            parentFile?.mkdirs()
            writeText(
                """package $pkg

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Text(text = "Welcome to $appName created with Code0!")
            }
        }
    }
}
"""
            )
        }

        File(dir, "build.gradle.kts").writeText(
            """plugins {
    id("com.android.application") version "8.7.0" apply false
    id("org.jetbrains.kotlin.android") version "2.0.20" apply false
}
"""
        )
        File(dir, "app/build.gradle.kts").writeText(
            """plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "$pkg"
    compileSdk = 35

    defaultConfig {
        applicationId = "$pkg"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
}
"""
        )
    }

    private fun populatePythonStarter(dir: File, name: String) {
        File(dir, "README.md").writeText("# $name\n\nPython utility created in Code0.\n")
        File(dir, "main.py").writeText(
            """#!/usr/bin/env python3
\"\"\"
$name - Created with Code0 mobile studio
\"\"\"

def greet(target: str = "World") -> str:
    return f"Hello, {target}! Ready to code."

def main():
    print(greet("$name"))

if __name__ == "__main__":
    main()
"""
        )
        File(dir, "test_main.py").writeText(
            """import unittest
from main import greet

class TestMain(unittest.TestCase):
    def test_greet(self):
        self.assertEqual(greet("Code0"), "Hello, Code0! Ready to code.")

if __name__ == "__main__":
    unittest.main()
"""
        )
        File(dir, "requirements.txt").writeText("# Add your python dependencies here\n")
    }

    private fun populateWebStarter(dir: File, name: String) {
        File(dir, "README.md").writeText("# $name\n\nWeb project created in Code0.\n")
        File(dir, "index.html").writeText(
            """<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>$name</title>
    <link rel="stylesheet" href="styles.css">
</head>
<body>
    <main class="container">
        <h1>Welcome to $name</h1>
        <p>Built with Code0 Mobile Coding Studio</p>
        <button id="actionBtn">Click Me</button>
        <p id="output"></p>
    </main>
    <script src="app.js"></script>
</body>
</html>
"""
        )
        File(dir, "styles.css").writeText(
            """body {
    margin: 0;
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
    background: #12161A;
    color: #F1F5F9;
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 100vh;
}
.container {
    text-align: center;
    padding: 2rem;
    background: #1E293B;
    border-radius: 12px;
    box-shadow: 0 4px 12px rgba(0,0,0,0.4);
}
button {
    background: #00E676;
    color: #0F172A;
    border: none;
    padding: 10px 20px;
    font-weight: bold;
    border-radius: 8px;
    cursor: pointer;
}
"""
        )
        File(dir, "app.js").writeText(
            """document.getElementById('actionBtn').addEventListener('click', () => {
    document.getElementById('output').innerText = 'Code0 active at ' + new Date().toLocaleTimeString();
});
"""
        )
    }

    private fun populateTypeScriptStarter(dir: File, name: String) {
        File(dir, "README.md").writeText("# $name\n\nTypeScript project in Code0.\n")
        File(dir, "package.json").writeText(
            """{
  "name": "${name.lowercase().replace(" ", "-")}",
  "version": "1.0.0",
  "description": "Created with Code0",
  "main": "dist/index.js",
  "scripts": {
    "build": "tsc",
    "start": "node dist/index.js",
    "test": "echo \"Running tests...\" && exit 0"
  },
  "dependencies": {},
  "devDependencies": {
    "typescript": "^5.0.0"
  }
}
"""
        )
        File(dir, "tsconfig.json").writeText(
            """{
  "compilerOptions": {
    "target": "ES2022",
    "module": "commonjs",
    "outDir": "./dist",
    "rootDir": "./src",
    "strict": true,
    "esModuleInterop": true
  }
}
"""
        )
        File(dir, "src/index.ts").apply {
            parentFile?.mkdirs()
            writeText(
                """export function startApp(): string {
    return "Code0 TypeScript Service running.";
}

console.log(startApp());
"""
            )
        }
    }

    private fun populateEmptyStarter(dir: File, name: String) {
        File(dir, "README.md").writeText("# $name\n\nEmpty project initialized in Code0.\n")
    }
}
