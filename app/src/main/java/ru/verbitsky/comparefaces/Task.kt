package ru.verbitsky.comparefaces

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.widget.TextView
import android.widget.Toast
import java.io.IOError
import java.io.InputStream
import java.io.OutputStream
import java.lang.Exception
import java.net.Socket

class Task(context: Context) : AsyncTask<String, Void, String>() {
    val context = context
    var Stroka: String = ""
    var soc:Socket? = null
    var writer: OutputStream? = null
    var input: InputStream? = null

    override fun doInBackground(vararg params: String): String? {
        try{
            soc = Socket("192.168.1.41", 6454)
            if(soc !=null) {
                writer = soc?.getOutputStream()
                if (writer != null) {
                    writer!!.write(params[0].toByteArray())
                    writer!!.flush()
                }
                input = soc!!.getInputStream()

                while (true) {
                    val s = input!!.read().toChar()
                    if (s != '*') Stroka += s else break
                }
            }
        }catch(e: Exception){
            e.printStackTrace()
          }catch (e: IOError) {
             e.printStackTrace()
          } finally {
            if(writer!=null) {
                writer?.close()
            }
            if(input!=null){
                input?.close()
            }
            soc?.close()
        }
        return Stroka
    }

    override fun onPostExecute(result: String) {
        super.onPostExecute(result)
        if(!result.equals("error")) {
            val intent: Intent = Intent(context, Result::class.java)
            intent.putExtra("info", result)
            context.startActivity(intent)
        }else{
            Toast.makeText(context, "Error! There is no face on the picture!", Toast.LENGTH_LONG)
        }
    }
}