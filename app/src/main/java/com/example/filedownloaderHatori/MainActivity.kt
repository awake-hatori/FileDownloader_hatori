package com.example.filedownloaderHatori

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.HandlerCompat
import com.example.filedownloaderHatori.databinding.ActivityMainBinding
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    companion object {
        private const val REQUEST_GALLERY_TAKE = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ダイアログを表示
        displayDialog()
        // URLから画像検索
            binding.startDownload.setOnClickListener { v: View? ->
            val stringUrl = binding.URLInputField.getText().toString()
            downloadImage(stringUrl)
        }
        // ギャラリーへ遷移
        binding.toGallery.setOnClickListener{v:View?->
            toGallery()
        }
        binding.toDocument.setOnClickListener{v:View?->
            toDocument()
        }
        // 画像とテキストをclear
        binding.clear.setOnClickListener { v: View? ->
            clear()
        }
    }

    private fun downloadImage(urlSt: String) {
        // Singleの別スレッドを立ち上げる
        Executors.newSingleThreadExecutor().execute {
            try {
                val url = URL(urlSt)
                val urlCon = url.openConnection() as HttpURLConnection
                // タイムアウト設定
                urlCon.readTimeout = 10000
                urlCon.connectTimeout = 20000
                // リクエストメソッド
                urlCon.requestMethod = "GET"
                // リダイレクトを自動で許可しない設定
                urlCon.instanceFollowRedirects = false
                val bitmap = BitmapFactory.decodeStream(urlCon.inputStream)
                // 別スレッド内での処理を管理し実行する
                HandlerCompat.createAsync(mainLooper).post {
                    Toast.makeText(applicationContext,"画像を取得できました",Toast.LENGTH_LONG).show()
                    binding.image.setImageBitmap(bitmap)
                }
                // 内部ストレージのディレクトリを指定するオブジェクトを取得
                val context: Context = applicationContext
                val file = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "sample_image.jpeg")
                // JPEG形式で保存
                FileOutputStream(file).use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                }
            } catch (e: IOException) {
                HandlerCompat.createAsync(mainLooper).post {
                    Toast.makeText(applicationContext,"画像を取得できませんでした",Toast.LENGTH_LONG).show()
                }
                e.printStackTrace()
            }
        }
    }

    private fun toGallery(){
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_GALLERY_TAKE)
    }

    private fun toDocument(){
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.type = "*/*"
        startActivityForResult(intent, REQUEST_GALLERY_TAKE)
    }

    // onActivityResultにイメージ設定
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode){
            REQUEST_GALLERY_TAKE -> {
                if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_GALLERY_TAKE){
                    binding.image.setImageURI(data?.data)
                }
            }
        }
    }

    private fun displayDialog(){
        val builder = AlertDialog.Builder(this).apply{
            setMessage("ストレージへのアクセス許可")
            setPositiveButton("許可する") { _, _ ->
                // 何もしない
            }
            setNegativeButton("許可しない") { _, _ ->
                // プロセスを終了
                android.os.Process.killProcess(android.os.Process.myPid());
            }
        }
        builder.create().apply {
            setCancelable(false)
        }.show()
    }

    private fun clear(){
        binding.URLInputField.setText("")
        binding.image.setImageDrawable(null)
    }
}