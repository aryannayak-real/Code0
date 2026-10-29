package com.example.data.repository

import com.example.data.model.GitRepoItem
import com.example.data.model.GitStatusSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

class GitHubRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
) {
    suspend fun validateToken(token: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("https://api.github.com/user")
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "Code0-Android-Studio")
                .build()

            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("GitHub auth error: HTTP ${resp.code} (${resp.message})")
                    )
                }
                val body = resp.body?.string() ?: ""
                val json = JSONObject(body)
                val username = json.getString("login")
                Result.success(username)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchUserRepos(token: String): Result<List<GitRepoItem>> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("https://api.github.com/user/repos?sort=updated&per_page=40")
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "Code0-Android-Studio")
                .build()

            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(Exception("Failed to load repos: HTTP ${resp.code}"))
                }
                val body = resp.body?.string() ?: "[]"
                val arr = JSONArray(body)
                val repos = mutableListOf<GitRepoItem>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    repos.add(parseRepo(obj))
                }
                Result.success(repos)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchPublicRepos(query: String, token: String? = null): Result<List<GitRepoItem>> = withContext(Dispatchers.IO) {
        try {
            val builder = Request.Builder()
                .url("https://api.github.com/search/repositories?q=$query&sort=stars&per_page=20")
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "Code0-Android-Studio")

            if (!token.isNullOrBlank()) {
                builder.header("Authorization", "Bearer $token")
            }

            client.newCall(builder.build()).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(Exception("Search failed: HTTP ${resp.code}"))
                }
                val body = resp.body?.string() ?: "{}"
                val items = JSONObject(body).optJSONArray("items") ?: JSONArray()
                val repos = mutableListOf<GitRepoItem>()
                for (i in 0 until items.length()) {
                    repos.add(parseRepo(items.getJSONObject(i)))
                }
                Result.success(repos)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchBranches(owner: String, repo: String, token: String? = null): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val builder = Request.Builder()
                .url("https://api.github.com/repos/$owner/$repo/branches")
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "Code0-Android-Studio")

            if (!token.isNullOrBlank()) {
                builder.header("Authorization", "Bearer $token")
            }

            client.newCall(builder.build()).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(Exception("Failed to fetch branches: HTTP ${resp.code}"))
                }
                val body = resp.body?.string() ?: "[]"
                val arr = JSONArray(body)
                val branches = mutableListOf<String>()
                for (i in 0 until arr.length()) {
                    branches.add(arr.getJSONObject(i).getString("name"))
                }
                Result.success(branches)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Download complete repository archive (Zipball) from GitHub API and unpack into targetDir,
     * stripping the top-level container directory that GitHub includes (e.g. `owner-repo-hash/`).
     * Preserves exact nested directory hierarchy!
     */
    suspend fun downloadAndExtractRepository(
        owner: String,
        repo: String,
        branch: String,
        token: String?,
        targetDir: File,
        onProgress: ((String) -> Unit)? = null
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            onProgress?.invoke("Connecting to GitHub...")
            val ref = branch.ifBlank { "main" }
            val builder = Request.Builder()
                .url("https://api.github.com/repos/$owner/$repo/zipball/$ref")
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "Code0-Android-Studio")

            if (!token.isNullOrBlank()) {
                builder.header("Authorization", "Bearer $token")
            }

            client.newCall(builder.build()).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("Repository download failed: HTTP ${resp.code} (${resp.message})")
                    )
                }

                val bodyStream = resp.body?.byteStream()
                    ?: return@withContext Result.failure(Exception("Empty response body"))

                onProgress?.invoke("Extracting repository files...")
                targetDir.mkdirs()

                var extractedFileCount = 0
                ZipInputStream(bodyStream).use { zis ->
                    var entry = zis.nextEntry
                    var rootPrefix: String? = null

                    while (entry != null) {
                        val name = entry.name
                        if (rootPrefix == null) {
                            val firstSlash = name.indexOf('/')
                            if (firstSlash != -1) {
                                rootPrefix = name.substring(0, firstSlash + 1)
                            }
                        }

                        val relPath = if (rootPrefix != null && name.startsWith(rootPrefix)) {
                            name.removePrefix(rootPrefix)
                        } else {
                            name
                        }

                        if (relPath.isNotEmpty()) {
                            val outFile = File(targetDir, relPath)
                            if (entry.isDirectory) {
                                outFile.mkdirs()
                            } else {
                                outFile.parentFile?.mkdirs()
                                FileOutputStream(outFile).use { fos ->
                                    zis.copyTo(fos)
                                }
                                extractedFileCount++
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }

                onProgress?.invoke("Extracted $extractedFileCount files successfully")
                Result.success(extractedFileCount)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Inspect local project files and detect modified / untracked files
     */
    suspend fun getLocalGitStatus(projectDir: File, branch: String): GitStatusSummary = withContext(Dispatchers.IO) {
        val untracked = mutableListOf<String>()
        val modified = mutableListOf<String>()

        projectDir.walkTopDown().filter { it.isFile }.forEach { file ->
            val rel = file.relativeTo(projectDir).path
            // If file is newly added or modified
            if (!rel.startsWith(".git")) {
                untracked.add(rel)
            }
        }

        GitStatusSummary(
            branch = branch,
            modifiedFiles = modified,
            untrackedFiles = untracked.take(50),
            deletedFiles = emptyList()
        )
    }

    /**
     * Push commit via GitHub API
     * Updates/creates files in the remote repository using authenticated GitHub contents API.
     */
    suspend fun pushCommitToGitHub(
        owner: String,
        repo: String,
        branch: String,
        token: String,
        commitMessage: String,
        filesToPush: List<Pair<String, String>>
    ): Result<String> = withContext(Dispatchers.IO) {
        if (token.isBlank()) {
            return@withContext Result.failure(Exception("GitHub personal access token required for push"))
        }

        try {
            var successCount = 0
            for ((path, content) in filesToPush) {
                // Fetch file SHA if it exists
                var existingSha: String? = null
                val checkReq = Request.Builder()
                    .url("https://api.github.com/repos/$owner/$repo/contents/$path?ref=$branch")
                    .header("Authorization", "Bearer $token")
                    .header("Accept", "application/vnd.github.v3+json")
                    .header("User-Agent", "Code0-Android-Studio")
                    .build()

                client.newCall(checkReq).execute().use { checkResp ->
                    if (checkResp.isSuccessful) {
                        val body = checkResp.body?.string() ?: "{}"
                        existingSha = JSONObject(body).optString("sha", null)
                    }
                }

                // Base64 encode content
                val encodedContent = android.util.Base64.encodeToString(
                    content.toByteArray(Charsets.UTF_8),
                    android.util.Base64.NO_WRAP
                )

                val payload = JSONObject().apply {
                    put("message", commitMessage)
                    put("content", encodedContent)
                    put("branch", branch)
                    if (existingSha != null) {
                        put("sha", existingSha)
                    }
                }

                val jsonMedia = "application/json; charset=utf-8".toMediaTypeOrNull()
                val putBody = payload.toString().toRequestBody(jsonMedia)

                val putReq = Request.Builder()
                    .url("https://api.github.com/repos/$owner/$repo/contents/$path")
                    .header("Authorization", "Bearer $token")
                    .header("Accept", "application/vnd.github.v3+json")
                    .header("User-Agent", "Code0-Android-Studio")
                    .put(putBody)
                    .build()

                client.newCall(putReq).execute().use { putResp ->
                    if (putResp.isSuccessful) {
                        successCount++
                    } else {
                        return@withContext Result.failure(
                            Exception("Failed pushing $path: HTTP ${putResp.code} (${putResp.message})")
                        )
                    }
                }
            }

            Result.success("Pushed $successCount file(s) to $owner/$repo on branch '$branch'")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseRepo(obj: JSONObject): GitRepoItem {
        return GitRepoItem(
            id = obj.getLong("id"),
            name = obj.getString("name"),
            fullName = obj.getString("full_name"),
            description = obj.optString("description", null),
            isPrivate = obj.getBoolean("private"),
            htmlUrl = obj.getString("html_url"),
            defaultBranch = obj.optString("default_branch", "main"),
            stars = obj.optInt("stargazers_count", 0),
            updatedAt = obj.optString("updated_at", "")
        )
    }
}
