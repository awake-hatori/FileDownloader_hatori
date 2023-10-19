package com.example.filedownloader_coroutine

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.HandlerCompat
import androidx.core.view.isInvisible
import com.example.filedownloader_coroutine.databinding.ActivityMainBinding
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date

class MainActivity : AppCompatActivity() {  
    private lateinit var binding: ActivityMainBinding
    private val directoryPath =
        Environment.getExternalStorageDirectory().path + "/hatori_picture"

    companion object {
        private const val REQUEST_IMAGE_TAKE = 2
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // プログレスバーを非表示
        binding.progressbar.isInvisible = true
        // ダイアログを表示
        displayDialog()
        // URLから画像をダウンロード
        binding.startDownload.setOnClickListener {
            val urlString = binding.URLInputField.text.toString()
            downloadImage(urlString)
        }
        // ギャラリーへ遷移
        binding.toGallery.setOnClickListener { toGallery() }
        // ドキュメントへ遷移
        binding.toDocument.setOnClickListener { toDocument() }
        // 画像とテキストをclear
        binding.clear.setOnClickListener { clear() }
    }

    private fun displayDialog() {
        // 既存ディレクトリの有無を調べる
        if (File(directoryPath).isDirectory) return
        // ダイアログ出力
        val builder = AlertDialog.Builder(this).apply {
            setMessage("ストレージへのアクセス許可")
            setPositiveButton("許可する") { _, _ ->
                createDirectory()
            }
            setNegativeButton("許可しない") { _, _ ->
                android.os.Process.killProcess(android.os.Process.myPid())
            }
        }
        builder.create().apply {
            // キャンセル操作を無効
            setCancelable(false)
        }.show()
    }

    private fun createDirectory() {
        try {
            // 既存ディレクトリの有無を調べる
            val myDirectory = File(directoryPath)
            if (!myDirectory.isDirectory) myDirectory.mkdir()
        } catch (e: SecurityException) {
            // ファイルに書き込み用のパーミッションが無い場合など
            e.printStackTrace()
        } catch (e: IOException) {
            // 何らかの原因で誤ってディレクトリを2回作成してしまった場合など
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("SimpleDateFormat")
    @OptIn(DelicateCoroutinesApi::class)
    @RequiresApi(Build.VERSION_CODES.O)
    private fun downloadImage(urlString: String) = runBlocking {
        binding.progressbar.isInvisible = false
        GlobalScope.launch {
            try {
                val url = URL(urlString)
                val urlConnection =
                    withContext(Dispatchers.IO) {
                        url.openConnection()
                    } as HttpURLConnection
                urlConnection.readTimeout = 10000
                urlConnection.connectTimeout = 20000
                urlConnection.requestMethod = "GET"
                // リダイレクトを自動で許可しない設定
                urlConnection.instanceFollowRedirects = false
                val bitmap = BitmapFactory.decodeStream(urlConnection.inputStream)
                // 別スレッド内での処理を管理し実行する
                HandlerCompat.createAsync(mainLooper).post {
                    Toast.makeText(applicationContext, "画像をダウンロードしました", Toast.LENGTH_LONG).show()
                    binding.progressbar.isInvisible = true
                    // 画像をImageViewに表示
                    binding.image.setImageBitmap(bitmap)
                }
                // データ保存のフォーマット
                val dateFormat = SimpleDateFormat("yyyyMMdd_HH:mm:ss")
                val currentDate: String = dateFormat.format(Date())
                // JPEG形式で保存
                val file = File(directoryPath, "$currentDate.jpeg")
                withContext(Dispatchers.IO) {
                    FileOutputStream(file).use { stream ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                    }
                }
            } catch (e: IOException) {
                HandlerCompat.createAsync(mainLooper).post {
                    Toast.makeText(
                        applicationContext,
                        "画像をダウンロード出来ませんでした",
                        Toast.LENGTH_LONG
                    ).show()
                    binding.progressbar.isInvisible = true
                }
                e.printStackTrace()
            }
        }
    }

    private fun toGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_IMAGE_TAKE)
    }

    @SuppressLint("IntentReset")
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private fun toDocument() {
        val test = Uri.fromFile(File(directoryPath))
        val uri = Uri.parse(test.toString())
        Log.d("Log", "$uri")
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT, uri)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_IMAGE_TAKE)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_IMAGE_TAKE -> {
                if (resultCode == Activity.RESULT_OK && requestCode.equals(REQUEST_IMAGE_TAKE)) {
                    // 遷移先の画面から画像データを取得して表示
                    binding.image.setImageURI(data?.data)
                    Toast.makeText(applicationContext, "画像を取得しました", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun clear() {
        binding.URLInputField.setText("")
        binding.image.setImageDrawable(null)
    }
}