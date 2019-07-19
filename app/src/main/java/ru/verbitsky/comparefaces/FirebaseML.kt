package ru.verbitsky.comparefaces

import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.Image
import android.view.Gravity
import android.view.View
import android.view.Window
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.scale
import com.google.android.gms.tasks.OnFailureListener
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.android.synthetic.main.activity_main.*
import java.io.ByteArrayOutputStream

class FirebaseML: MainActivity() {
    lateinit var  imageView : ImageView
    lateinit var choise_photo:TextView
    lateinit var progressBar:ProgressBar
    private var list = mutableListOf<Bitmap>()
    private var ammountOfRows = 1
    private var listRows = mutableListOf<LinearLayout>()
    private var iterRows = 0



     fun  MLFaces(context: Context, bitmap : Bitmap?){
         choise_photo.visibility = View.INVISIBLE
         progressBar.visibility = View.VISIBLE
        val bitmapScaled : Bitmap = bitmap!!.scale(600, (600*(bitmap!!.height.toFloat()/bitmap.width.toFloat())).toInt())
        val scaleWidth:Float = bitmap.width.toFloat()/bitmapScaled.width.toFloat()
        val scaleHeight:Float = bitmap.height.toFloat()/bitmapScaled.height.toFloat()

        val image = FirebaseVisionImage.fromBitmap(bitmapScaled)
        val detector = FirebaseVision.getInstance().visionFaceDetector

        val result = detector.detectInImage(image)
            .addOnSuccessListener { faces ->

                faces.forEach {
                    var x = (it.boundingBox.left*scaleWidth).toInt() - (it.boundingBox.width()*scaleWidth*(0.2)).toInt()
                    var y = (it.boundingBox.top*scaleHeight).toInt()
                    var width = (it.boundingBox.right*scaleWidth).toInt() - x + (it.boundingBox.width()*scaleWidth*(0.2)).toInt()
                    var height =  (it.boundingBox.bottom*scaleHeight).toInt() - y + (it.boundingBox.height()*scaleHeight*(0.5)).toInt()
                    y = y - (it.boundingBox.height()*scaleHeight*(0.35)).toInt()
                    if(x<0) x=1
                    if(y<0) y=1
                    if(y+height>(bitmap.height).toInt()) height = bitmap.height - y
                    if(x+width>(bitmap.width).toInt()) width = bitmap.width-x

                    list.add(Bitmap.createBitmap(bitmap, x, y, width, height))
                }
                runOnUiThread {
                    when(list.size) {
                        0-> {
                            when(photoNumber){
                                1->isPhoto1 = false
                                2->isPhoto2 = false
                            }
                            Toast.makeText(context, "Ошибка! На фотографии нет лица!", Toast.LENGTH_SHORT).show()
                            imageView.setImageResource(0)
                            choise_photo.visibility = View.VISIBLE
                            progressBar.visibility = View.INVISIBLE
                        }
                        1-> showPhoto(list[0])
                        else-> {
                            createDialog(context)
                            progressBar.visibility = View.INVISIBLE
                        }

                    }
                }
            }
            .addOnFailureListener(
                object : OnFailureListener {
                    override fun onFailure(e: Exception) {
                        Toast.makeText(context, "Failed to get image", Toast.LENGTH_SHORT).show()
                    }
                })

    }

    private fun createDialog(context:Context){
        var dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.custom_dialog_picture)
        var tableLayout = dialog.findViewById(R.id.table) as TableLayout
        if(list.size>4){
            ammountOfRows = list.size/3 + if (list.size%3==0) 0 else 1
        }else{
            ammountOfRows = list.size/2 + if (list.size%2==0) 0 else 1
        }

        var params : LinearLayout.LayoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(10,10,10,10)

        for(i in 0..ammountOfRows){
            var tableRow = LinearLayout(context)
            tableRow.layoutParams = params
            tableRow.gravity = Gravity.CENTER
            tableLayout.addView(tableRow)
            listRows.add(tableRow)
        }

        list.forEachIndexed{index, photo ->

            var imageBackground = LinearLayout(context)
            var image = CircleImageView(context)
            var width = 400
            when(list.size){
                1-> width = 600
                in 2..4-> width = 300
                else-> width = 200
            }
            var paramsBack: LinearLayout.LayoutParams = LinearLayout.LayoutParams(width, width)
            var paramsImage: LinearLayout.LayoutParams = LinearLayout.LayoutParams(width-5, width-5)
            paramsBack.setMargins(20, 10, 20, 10)

            imageBackground.layoutParams = paramsBack
            imageBackground.gravity = Gravity.CENTER
            imageBackground.setBackgroundResource(R.drawable.circle_shape)
            image.layoutParams = paramsImage
            imageBackground.addView(image)
            listRows[iterRows].addView(imageBackground)
            if((list.size > 4) and ((index+1)%3==0)) iterRows++ else if((list.size <= 4) and ((index+1)%2==0)) iterRows++
            imageBackground.setElevation((5.0).toFloat())
            image.setImageBitmap(photo)
            dialog.show()
            image.setOnClickListener {
                showPhoto(photo)
                dialog.dismiss()
            }
        }
    }

    private fun showPhoto(bitmap : Bitmap?){
        imageView.setImageBitmap(bitmap)
        progressBar.visibility = View.INVISIBLE
        when(photoNumber) {
            1 -> isPhoto1 = true
            2 -> isPhoto2 = true
        }
        unLockCompare()
    }

}