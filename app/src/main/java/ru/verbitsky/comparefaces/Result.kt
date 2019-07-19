package ru.verbitsky.comparefaces

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import kotlinx.android.synthetic.main.result.*
import androidx.appcompat.app.AppCompatActivity

class Result : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.result)
        val info = 100-intent.getStringExtra("info").toFloat()*100
        resultText.text = "Процент сходства: " + info + "%"

    }

    fun parser(info:String):List<String>{
        return info.split(" &@# ")
    }

    fun base64toBitmap(base64str: String): Bitmap {
        val bytes = base64str.decodeBase64ToByteArray()
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }
}