package com.example.data

import android.content.Context
import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException

data class BackupPayload(
    val accounts: List<Account>,
    val expenses: List<Expense>,
    val incomes: List<Income>,
    val safeWithdrawals: List<SafeWithdrawal> = emptyList(),
    val backupTime: Long = System.currentTimeMillis(),
    val deviceName: String = android.os.Build.MODEL
)

object GoogleDriveHelper {
    private const val TAG = "GoogleDriveHelper"
    private val client = OkHttpClient()
    
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private val adapter = moshi.adapter(BackupPayload::class.java)

    /**
     * Serializes local data to a JSON string.
     */
    fun serializeData(accounts: List<Account>, expenses: List<Expense>, incomes: List<Income>, safeWithdrawals: List<SafeWithdrawal>): String {
        val payload = BackupPayload(accounts, expenses, incomes, safeWithdrawals)
        return adapter.toJson(payload)
    }

    /**
     * Deserializes backup JSON to models.
     */
    fun deserializeData(jsonString: String): BackupPayload? {
        return try {
            adapter.fromJson(jsonString)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to deserialize backup", e)
            null
        }
    }

    /**
     * Backs up data to Google Drive.
     * If the token is empty/simulated, saves to a mock cloud file (local cache shared preferences) 
     * so that the user can immediately test backup/restore in the browser preview.
     */
    fun backupToDrive(
        context: Context,
        accounts: List<Account>,
        expenses: List<Expense>,
        incomes: List<Income>,
        safeWithdrawals: List<SafeWithdrawal> = emptyList(),
        accessToken: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        val jsonContent = serializeData(accounts, expenses, incomes, safeWithdrawals)
        
        // Check for simulated/sandbox environment
        if (accessToken.isEmpty() || accessToken.startsWith("simulated_")) {
            // Simulated backup
            val sharedPref = context.getSharedPreferences("google_drive_mock", Context.MODE_PRIVATE)
            sharedPref.edit()
                .putString("mock_backup_file", jsonContent)
                .putLong("mock_backup_time", System.currentTimeMillis())
                .apply()
            
            // Log for debugging
            Log.d(TAG, "Simulated backup complete: ${jsonContent.length} bytes saved.")
            onResult(true, "تم النسخ الاحتياطي السحابي بنجاح (بيئة محاكاة)")
            return
        }

        // Real Google Drive integration
        val mediaType = "application/json; charset=utf-8".toMediaType()
        Thread {
            try {
                // 1. Search for existing "ledger_app_backup.json"
                val searchUrl = "https://www.googleapis.com/drive/v3/files?q=name='ledger_app_backup.json' and trashed=false"
                val searchRequest = Request.Builder()
                    .url(searchUrl)
                    .header("Authorization", "Bearer $accessToken")
                    .get()
                    .build()

                client.newCall(searchRequest).execute().use { response ->
                    if (!response.isSuccessful) {
                        onResult(false, "حدث خطأ أثناء البحث عن نسخة احتياطية: ${response.code}")
                        return@Thread
                    }

                    val responseBody = response.body?.string() ?: ""
                    val jsonResponse = JSONObject(responseBody)
                    val filesArray = jsonResponse.optJSONArray("files")
                    var fileId: String? = null

                    if (filesArray != null && filesArray.length() > 0) {
                        fileId = filesArray.getJSONObject(0).optString("id")
                    }

                    if (fileId != null) {
                        // 2. Overwrite existing file with PATCH
                        val patchUrl = "https://www.googleapis.com/upload/drive/v3/files/$fileId?uploadType=media"
                        val patchRequest = Request.Builder()
                            .url(patchUrl)
                            .header("Authorization", "Bearer $accessToken")
                            .patch(jsonContent.toRequestBody(mediaType))
                            .build()

                        client.newCall(patchRequest).execute().use { patchResponse ->
                            if (patchResponse.isSuccessful) {
                                onResult(true, "تم تحديث النسخة الاحتياطية على Google Drive بنجاح")
                            } else {
                                onResult(false, "فشل تحديث النسخة الاحتياطية: ${patchResponse.code}")
                            }
                        }
                    } else {
                        // 3. Create a new file with POST metadata + media
                        // First create metadata
                        val metadataUrl = "https://www.googleapis.com/drive/v3/files"
                        val metaJson = JSONObject().apply {
                            put("name", "ledger_app_backup.json")
                            put("mimeType", "application/json")
                        }.toString()

                        val metaRequest = Request.Builder()
                            .url(metadataUrl)
                            .header("Authorization", "Bearer $accessToken")
                            .post(metaJson.toRequestBody("application/json".toMediaType()))
                            .build()

                        client.newCall(metaRequest).execute().use { metaResponse ->
                            if (!metaResponse.isSuccessful) {
                                onResult(false, "فشل إنشاء ملف النسخ الاحتياطي: ${metaResponse.code}")
                                return@Thread
                            }

                            val newFileId = JSONObject(metaResponse.body?.string() ?: "{}").optString("id")
                            if (newFileId.isEmpty()) {
                                onResult(false, "لم يتم الحصول على معرف ملف جديد")
                                return@Thread
                            }

                            // Now upload content
                            val uploadUrl = "https://www.googleapis.com/upload/drive/v3/files/$newFileId?uploadType=media"
                            val uploadRequest = Request.Builder()
                                .url(uploadUrl)
                                .header("Authorization", "Bearer $accessToken")
                                .patch(jsonContent.toRequestBody(mediaType))
                                .build()

                            client.newCall(uploadRequest).execute().use { uploadResponse ->
                                if (uploadResponse.isSuccessful) {
                                    onResult(true, "تم رفع نسخة احتياطية جديدة لـ Google Drive بنجاح")
                                } else {
                                    onResult(false, "فشل رفع محتوى النسخة الاحتياطية: ${uploadResponse.code}")
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in backupToDrive", e)
                onResult(false, "فشل الاتصال بـ Google Drive: ${e.message}")
            }
        }.start()
    }

    /**
     * Restores data from Google Drive.
     */
    fun restoreFromDrive(
        context: Context,
        accessToken: String,
        onResult: (Boolean, BackupPayload?, String?) -> Unit
    ) {
        // Simulated sandbox environment
        if (accessToken.isEmpty() || accessToken.startsWith("simulated_")) {
            val sharedPref = context.getSharedPreferences("google_drive_mock", Context.MODE_PRIVATE)
            val jsonContent = sharedPref.getString("mock_backup_file", null)
            
            if (jsonContent != null) {
                val payload = deserializeData(jsonContent)
                if (payload != null) {
                    onResult(true, payload, "تم العثور على نسخة احتياطية (محاكاة) بنجاح")
                } else {
                    onResult(false, null, "فشل تحليل بيانات النسخة الاحتياطية")
                }
            } else {
                onResult(false, null, "لم يتم العثور على أي نسخ احتياطية مسجلة مسبقاً في حسابك")
            }
            return
        }

        // Real Google Drive download
        Thread {
            try {
                // 1. Search for existing "ledger_app_backup.json"
                val searchUrl = "https://www.googleapis.com/drive/v3/files?q=name='ledger_app_backup.json' and trashed=false"
                val searchRequest = Request.Builder()
                    .url(searchUrl)
                    .header("Authorization", "Bearer $accessToken")
                    .get()
                    .build()

                client.newCall(searchRequest).execute().use { response ->
                    if (!response.isSuccessful) {
                        onResult(false, null, "خطأ أثناء البحث عن النسخة الاحتياطية: ${response.code}")
                        return@Thread
                    }

                    val responseBody = response.body?.string() ?: ""
                    val jsonResponse = JSONObject(responseBody)
                    val filesArray = jsonResponse.optJSONArray("files")
                    var fileId: String? = null

                    if (filesArray != null && filesArray.length() > 0) {
                        fileId = filesArray.getJSONObject(0).optString("id")
                    }

                    if (fileId != null) {
                        // 2. Download contents of file
                        val downloadUrl = "https://www.googleapis.com/drive/v3/files/$fileId?alt=media"
                        val downloadRequest = Request.Builder()
                            .url(downloadUrl)
                            .header("Authorization", "Bearer $accessToken")
                            .get()
                            .build()

                        client.newCall(downloadRequest).execute().use { downloadResponse ->
                            if (downloadResponse.isSuccessful) {
                                val content = downloadResponse.body?.string() ?: ""
                                val payload = deserializeData(content)
                                if (payload != null) {
                                    onResult(true, payload, "تم تحميل نسخة احتياطية بنجاح")
                                } else {
                                    onResult(false, null, "بيانات النسخة الاحتياطية غير صالحة")
                                }
                            } else {
                                onResult(false, null, "فشل تنزيل ملف النسخة الاحتياطية: ${downloadResponse.code}")
                            }
                        }
                    } else {
                        onResult(false, null, "لم يتم العثور على أي ملف نسخ احتياطي بالاسم المحدد على السحابة")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in restoreFromDrive", e)
                onResult(false, null, "فشل الاتصال بالخادم: ${e.message}")
            }
        }.start()
    }
}
