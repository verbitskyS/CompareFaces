package ru.verbitsky.comparefaces

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.MediaRecorder
import android.opengl.Visibility
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.speech.RecognitionListener
import android.speech.RecognitionService
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import kotlinx.android.synthetic.main.activity_main.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.lang.Exception
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

open class MainActivity : GeneralClass(), RecognitionListener {

    private val FINAL_TAKE_PHOTO = 1
    private val FINAL_CHOOSE_PHOTO = 2
    private val PERMISSION_REQUEST_CODE_CAMERA: Int = 1
    private val PERMISSION_REQUEST_CODE_GALLERY: Int = 2
    var photoNumber:Int = 1
    var nomerVoice:Int = 1
    var img1 = ""
    var img2 = ""
    var primaryProgressStatus = 0
    var secondaryHandler: Handler? = Handler()
    var stopProgress = false
    var isPhoto1 = true
    var isPhoto2 = true
    var isVoice1 = false
    var isVoice2 = false
    lateinit var  speech:SpeechRecognizer
    lateinit var recognizerIntent: Intent
    private var output: String? = null
    private var mediaRecorder: MediaRecorder? = null
    private var state: Boolean = false
    private val textForSpeech = "самоучитель чешского языка"
    var text = ""


    private var lastTouchDown: Long = 0 //для регистрации быстрого клика для onTouchListener
    private val CLICK_ACTION_THRESHHOLD = 200

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        voice1.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                nomerVoice = 1
                output = Environment.getExternalStorageDirectory().absolutePath + "/recording1.mp3"
                return onTouchButton(v, event, progressVoice1, 1)
            }
        })


        speech = SpeechRecognizer.createSpeechRecognizer(this)
        speech.setRecognitionListener(this)
        recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,"ru")
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,this.getPackageName())
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH)
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,this.getPackageName())
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)

        voice2.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                nomerVoice = 2
                output = Environment.getExternalStorageDirectory().absolutePath + "/recording2.mp3"
                return onTouchButton(v, event, progressVoice2, 2)
            }
        })

        face1.setOnClickListener {
            photoNumber = 1
            showDialogChoise()
        }
        face2.setOnClickListener {
            photoNumber = 2
            showDialogChoise()
        }

        compare.setOnClickListener {
            if((isPhoto1)and(isPhoto2)and(isVoice1)and(isVoice2)) {
                img1 = bitmapToBase64(face1.drawable.toBitmap())
                img2 = bitmapToBase64(face2.drawable.toBitmap())
                val bytes1 = File(Environment.getExternalStorageDirectory().absolutePath + "/recording1.mp3").readBytes()
                val vc1 = bytes1.encodeBase64ToString()
                val bytes2 = File(Environment.getExternalStorageDirectory().absolutePath + "/recording2.mp3").readBytes()
                val vc2 = bytes2.encodeBase64ToString()
                val len = (img1.length + img2.length + vc1.length + vc2.length + 20).toString()
                val s = len + " &@# " + img1 + " &@# " + img2 + " &@# " + vc1 + " &@# " + vc2
                //val s = len + " &@# " + img1 + " &@# " + img2
                compare.setText("Wait...")
                val task = Task(this).execute(s)
            }else Toast.makeText(this@MainActivity, "Что-то не сделано!", Toast.LENGTH_LONG).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == Activity.RESULT_OK) {
            val firebaseML = FirebaseML()
            if(photoNumber==1) {
                firebaseML.imageView = face1
                firebaseML.choise_photo = choise_photo1
                firebaseML.progressBar = progressBar1
            }else{
                firebaseML.imageView = face2
                firebaseML.choise_photo = choise_photo2
                firebaseML.progressBar = progressBar2
            }
            when (requestCode) {
                FINAL_TAKE_PHOTO ->
                    if (resultCode == Activity.RESULT_OK) {
                        if(imagePath!=null) firebaseML.MLFaces(this, imagePathToBitmap(imagePath))

                    }
                FINAL_CHOOSE_PHOTO ->
                    if (resultCode == Activity.RESULT_OK) {
                        if(handleImageOn(data) !=null) firebaseML.MLFaces(this, imagePathToBitmap(handleImageOn(data)))
                    }
            }
        }
    }

     private fun showDialogChoise() {
        var dialogs = Dialog(this@MainActivity)
        dialogs.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialogs.setContentView(R.layout.custom_dialog)

        val buttonCamera = dialogs.findViewById(R.id.btCamera) as Button
        val buttonGallery = dialogs.findViewById(R.id.btGallery) as Button
        buttonCamera.setOnClickListener {
            if (checkPersmission(this@MainActivity)) takePicture() else requestPermission(PERMISSION_REQUEST_CODE_CAMERA, this@MainActivity)
            dialogs.dismiss()
        }

        buttonGallery.setOnClickListener{
            if (checkPersmission(this@MainActivity)) openAlbum() else requestPermission(PERMISSION_REQUEST_CODE_GALLERY, this@MainActivity)
            dialogs.dismiss()
        }

        dialogs.show()

    }


    private fun startRecording() {
        try {
            mediaRecorder = MediaRecorder()
            mediaRecorder?.setAudioSource(MediaRecorder.AudioSource.MIC)
            mediaRecorder?.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mediaRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mediaRecorder?.setOutputFile(output)

            mediaRecorder?.prepare()
            mediaRecorder?.start()
            state = true
           // Toast.makeText(this, "Recording started!", Toast.LENGTH_SHORT).show()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun stopRecording() {
        try {
            if (state) {
                mediaRecorder?.stop()
                mediaRecorder?.release()
                state = false
                //Toast.makeText(this, "Stopped and Saved", Toast.LENGTH_LONG).show()
            } else {
                // Toast.makeText(this, "You are not recording right now!", Toast.LENGTH_SHORT).show()
            }
        }catch (e:Exception){
            state = false
            primaryProgressStatus = 0 //чтобы остановить поток
            stopProgress = true
            when(nomerVoice){ //разблокируем нажатие другой кнопки
                1->{
                    isVoice1 = false
                    progressVoice1.progress = 0
                }
                2->{
                    isVoice2 = false
                    progressVoice2.progress = 0
                }
            }
        }

    }

    private fun onTouchButton(v:View?, event:MotionEvent?, progressBar:ProgressBar, nomer:Int):Boolean{
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchDown = System.currentTimeMillis()
                lockCompare()
                stopProgress = false
                when(nomer){ //блокируем нажатие другой кнопки
                    1->{
                        voice2.setEnabled(false)
                        isVoice1 = true
                    }
                    2->{
                        voice1.setEnabled(false)
                        isVoice2 = true
                    }
                }
                if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    val permissions = arrayOf(android.Manifest.permission.RECORD_AUDIO, android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                    ActivityCompat.requestPermissions(this, permissions,0)
                } else{
                    startRecording()
                    speech.startListening(recognizerIntent)
                    primaryProgressStatus = 0
                    progressBar.progress = 0
                    Thread(Runnable {
                        while (primaryProgressStatus < 100) {
                            if(!stopProgress) {
                                primaryProgressStatus += 1
                                try {
                                    Thread.sleep(100)
                                } catch (e: InterruptedException) {
                                    e.printStackTrace()
                                }
                                secondaryHandler?.post {
                                    if (!stopProgress) progressBar.progress = primaryProgressStatus
                                }
                            }else{
                                primaryProgressStatus=100
                            }
                        }
                    }).start()
                }
                return true
            }
            MotionEvent.ACTION_UP  -> {
                if (System.currentTimeMillis() - lastTouchDown < CLICK_ACTION_THRESHHOLD) {
                    Toast.makeText(this@MainActivity, "Click", Toast.LENGTH_LONG).show()
                    lockCompare()
                    primaryProgressStatus = 0 //чтобы остановить поток
                    stopProgress = true
                    progressBar.progress = 0
                    when (nomer) { //разблокируем нажатие другой кнопки
                        1 -> voice2.setEnabled(true)
                        2 -> voice1.setEnabled(true)
                    }
                    speech.cancel()

                }else {
                    stopRecording()
                    speech.stopListening()
                    primaryProgressStatus = 100 //чтобы остановить поток
                    progressBar.progress = 100
                    when (nomer) { //разблокируем нажатие другой кнопки
                        1 -> voice2.setEnabled(true)
                        2 -> voice1.setEnabled(true)
                    }
                }
            }
        }
        return v?.onTouchEvent(event) ?: true
    }

    private fun bitmapToBase64(bitmap:Bitmap):String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        val byteArray = byteArrayOutputStream.toByteArray ()
        return byteArray.encodeBase64ToString()
    }


    override fun onResults(results: Bundle?) {
        val matches = results!!.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        matches.forEach{
            it.toLowerCase()
        }
        Toast.makeText(this@MainActivity, matches[0], Toast.LENGTH_LONG).show()
        if(!(textForSpeech in matches)){
            primaryProgressStatus = 0 //чтобы остановить поток
            stopProgress = true
            Toast.makeText(this@MainActivity, "Неправильный текст, попробуйте снова!", Toast.LENGTH_LONG).show()
            when(nomerVoice){ //разблокируем нажатие другой кнопки
                1->{
                    isVoice1 = false
                    progressVoice1.progress = 0
                }
                2->{
                    isVoice2 = false
                    progressVoice2.progress = 0
                }
            }
        }
        text = ""
        unLockCompare()
    }

    override fun onError(error: Int) {
        primaryProgressStatus = 0 //чтобы остановить поток
        stopProgress = true
        when(nomerVoice){ //разблокируем нажатие другой кнопки
            1->{
                isVoice1 = false
                progressVoice1.progress = 0
            }
            2->{
                isVoice2 = false
                progressVoice2.progress = 0
            }
        }
        Toast.makeText(this@MainActivity, "Ошибка!", Toast.LENGTH_LONG).show()
    }
    override fun onEndOfSpeech(){}
    override fun onBeginningOfSpeech(){}
    override fun onEvent(eventType: Int, params: Bundle?){}
    override fun onPartialResults(partialResults: Bundle?){}
    override fun onBufferReceived(buffer: ByteArray?){}
    override fun onRmsChanged(rmsdB: Float){}
    override fun onReadyForSpeech(params: Bundle?){}

    fun unLockCompare(){
        if((isPhoto1)and(isPhoto2)and(isVoice1)and(isVoice2)) {
            compare.text = "СРАВНИТЬ"
           // compare.textSize = (10.toPx()).toFloat()
        }

    }

    fun lockCompare(){
        compare.text = "самоучитель чешского языка"
    }

    fun Int.toPx():Int = (this*Resources.getSystem().displayMetrics.density).toInt()

}


