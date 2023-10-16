package com.example.filedownloaderHatori

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.HandlerCompat
import com.example.filedownloaderHatori.databinding.ActivityMainBinding
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val directoryPath =
        Environment.getExternalStorageDirectory().getPath() + "/hatori_picture"

    companion object {
        private const val REQUEST_GALLERY_TAKE = 2
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ダイアログを表示
        displayDialog()
        // URLから画像をダウンロード
        binding.startDownload.setOnClickListener { v: View? ->
            val urlString = binding.URLInputField.getText().toString()
            downloadImage(urlString)
        }
        // ギャラリーへ遷移
        binding.toGallery.setOnClickListener { v: View? -> toGallery() }
        // ドキュメントへ遷移
        binding.toDocument.setOnClickListener { v: View? -> toDocument() }
        // 画像とテキストをclear
        binding.clear.setOnClickListener { v: View? -> clear() }
    }

    private fun displayDialog() {
        // 既存ディレクトリの有無を調べる
        if (File(directoryPath).isDirectory) return

        val builder = AlertDialog.Builder(this).apply {
            setMessage("ストレージへのアクセス許可")
            setPositiveButton("許可する") { _, _ ->
                createDirectory()
            }
            setNegativeButton("許可しない") { _, _ ->
                android.os.Process.killProcess(android.os.Process.myPid());
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
        } catch (error: SecurityException) {
            // ファイルに書き込み用のパーミッションが無い場合など
            error.printStackTrace()
        } catch (error: IOException) {
            // 何らかの原因で誤ってディレクトリを2回作成してしまった場合など
            error.printStackTrace()
        } catch (error: Exception) {
            error.printStackTrace()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun downloadImage(urlString: String) {
        // Singleの別スレッドを立ち上げる
        Executors.newSingleThreadExecutor().execute {
            try {
                val url = URL(urlString)
                val urlConnection = url.openConnection() as HttpURLConnection
                urlConnection.readTimeout = 10000
                urlConnection.connectTimeout = 20000
                urlConnection.requestMethod = "GET"
                // リダイレクトを自動で許可しない設定
                urlConnection.instanceFollowRedirects = false
                val bitmap = BitmapFactory.decodeStream(urlConnection.inputStream)
                // 別スレッド内での処理を管理し実行する
                HandlerCompat.createAsync(mainLooper).post {
                    Toast.makeText(applicationContext, "画像をダウンロードしました", Toast.LENGTH_LONG).show()
                    // 画像をImageViewに表示
                    binding.image.setImageBitmap(bitmap)
                }
                // データ保存のフォーマット
                val dateFormat = SimpleDateFormat("yyyyMMdd_HH:mm:ss");
                val currentDate: String = dateFormat.format(Date());
                // JPEG形式で保存
                val file = File(directoryPath, "$currentDate.jpeg")
                FileOutputStream(file).use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                }
            } catch (e: IOException) {
                HandlerCompat.createAsync(mainLooper).post {
                    Toast.makeText(applicationContext, "画像をダウンロード出来ませんでした", Toast.LENGTH_LONG)
                        .show()
                }
                e.printStackTrace()
            }
        }
    }

    private fun toGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_GALLERY_TAKE)
    }

    private fun toDocument() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.type = "*/*"
        startActivityForResult(intent, REQUEST_GALLERY_TAKE)
    }

    private fun clear() {
        binding.URLInputField.setText("")
        binding.image.setImageDrawable(null)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_GALLERY_TAKE -> {
                if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_GALLERY_TAKE) {
                    binding.image.setImageURI(data?.data)
                }
            }
        }
    }
}