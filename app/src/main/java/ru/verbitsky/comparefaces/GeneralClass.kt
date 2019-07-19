package ru.verbitsky.comparefaces

import android.Manifest
import android.app.Activity
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File

open class GeneralClass : AppCompatActivity() { //Класс со основными методами для работы с камерой и галереей


    private val FINAL_TAKE_PHOTO = 1
    private val FINAL_CHOOSE_PHOTO = 2
    private var imageUri: Uri? = null
    var imagePath: String? = null
    private val PERMISSION_REQUEST_CODE_CAMERA: Int = 1
    private val PERMISSION_REQUEST_CODE_GALLERY: Int = 2

    fun getBase64(imagePath: String?): String { //Получаем base64 картинки + сжимаем ее
        val bitmapImage = BitmapFactory.decodeFile(imagePath)
        val nh = (bitmapImage.height * (600.0 / bitmapImage.width)).toInt()
        val scaled = Bitmap.createScaledBitmap(bitmapImage, 600, nh, true)
        val baos = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 40, baos)
        val b = baos.toByteArray()
        val base64String = b.encodeBase64ToString()
        return base64String
    }


    fun checkPersmission(context: Context): Boolean {
        return (ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(context,
            android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)&& ContextCompat.checkSelfPermission(context,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    fun requestPermission(requestCode: Int, activity: Activity) {
        ActivityCompat.requestPermissions(activity, arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ), requestCode)
    }



    fun takePicture() {
        val directory = this.filesDir
        val outputImage = File(externalCacheDir, "output_image.jpg")
        if(outputImage.exists()) {
            outputImage.delete()
        }
        outputImage.createNewFile()
        imagePath = outputImage.absolutePath
        imageUri = if(Build.VERSION.SDK_INT >= 24){

            FileProvider.getUriForFile(
                this,
                "ru.verbitsky.comparefaces.fileprovider", outputImage
            )
        } else {
            Uri.fromFile(outputImage)
        }

        val intent = Intent("android.media.action.IMAGE_CAPTURE")
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        startActivityForResult(intent, FINAL_TAKE_PHOTO)
    }



    fun openAlbum(){
        val intent = Intent("android.intent.action.GET_CONTENT")
        intent.type = "image/*"
        startActivityForResult(intent, FINAL_CHOOSE_PHOTO)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            && grantResults[1] == PackageManager.PERMISSION_GRANTED && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
            when(requestCode) {
                PERMISSION_REQUEST_CODE_CAMERA -> takePicture()
                PERMISSION_REQUEST_CODE_GALLERY -> openAlbum()
            }
        } else {
            Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
        }

    }


    fun handleImageOn(data: Intent?):String?{
        var imagePath: String? = null
        val uri = data!!.data
        if (DocumentsContract.isDocumentUri(this, uri)){
            val docId = DocumentsContract.getDocumentId(uri)
            if ("com.android.providers.media.documents" == uri.authority){
                val id = docId.split(":")[1]
                val selsetion = MediaStore.Images.Media._ID + "=" + id
                imagePath = imagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selsetion)
            }
            else if ("com.android.providers.downloads.documents" == uri.authority){
                val contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), java.lang.Long.valueOf(docId))
                imagePath = imagePath(contentUri, null)
            }
        }
        else if ("content".equals(uri.scheme, ignoreCase = true)){
            imagePath = imagePath(uri, null)
        }
        else if ("file".equals(uri.scheme, ignoreCase = true)){
            imagePath = uri.path
        }
        this.imagePath = imagePath
        return imagePath
    }



    fun imagePath(uri: Uri?, selection: String?): String {
        var path: String? = null
        val cursor = contentResolver.query(uri, null, selection, null, null )
        if (cursor != null){
            if (cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA))
            }
            cursor.close()
        }
        return path!!
    }

    fun imagePathToBitmap(imagePath: String?):Bitmap?{
        if (imagePath != null) {
            return BitmapFactory.decodeFile(imagePath)
        }
        else {
            Toast.makeText(this, "Failed to get image", Toast.LENGTH_SHORT).show()
            return null
        }
    }

}