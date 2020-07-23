package com.gopal.cdd

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_classify.*
import org.tensorflow.lite.Interpreter
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.util.*

class Classify : AppCompatActivity() {
   private val RESULT_TO_SHOW=3
    private val IMAGE_MEAN=128
    private val IMAGE_STD=128.0f
    private val tfliteOption= Interpreter.Options()
   private lateinit var tflite:Interpreter
   lateinit var listLabels:List<String>
    private var imgData: ByteBuffer? = null
    private var labelProbArray: Array<FloatArray>? = null
    private var topLables: Array<String>? = null
    private var topConfidence: Array<String>? = null
    private val DIM_IMG_SIZE_X = 256//255;//224;//299
    private val DIM_IMG_SIZE_Y = 256 //255;//224;//299
    private val DIM_PIXEL_SIZE = 3
    private var intValues: IntArray?=null
  private  var Select:String?=null
    private val sortedLabels =
        PriorityQueue<Map.Entry<String, Float>>(
            RESULT_TO_SHOW,
                object: Comparator<Map.Entry<String?, Float?>?> {
                    override fun compare(
                        o1: Map.Entry<String?, Float?>?,
                        o2: Map.Entry<String?, Float?>?
                    ): Int {
                        return o1!!.value!!.compareTo(o2!!.value!!)
                    }

                })
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_classify)
      Select= intent.getStringExtra("SELECT")
        intValues = IntArray(DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y)
       tflite= Interpreter(LoadModelFile(),tfliteOption)
        listLabels=LoadLabelsList()
      imgData= ByteBuffer.allocate(4*DIM_IMG_SIZE_Y*DIM_IMG_SIZE_X*DIM_PIXEL_SIZE)
       imgData?.order(ByteOrder.nativeOrder())
        labelProbArray = Array(1) { FloatArray(listLabels.size) }

        topLables = Array(RESULT_TO_SHOW){ String()}
        topConfidence = Array(RESULT_TO_SHOW){String()}
      back_button.setOnClickListener{
          startActivity(Intent(this,MainActivity::class.java))
      }
       classify_image.setOnClickListener{
           val bitmap_orig = (selected_image.drawable as BitmapDrawable).bitmap
           var x=255f
           var y=255f
           val bitmap: Bitmap = getResizedBitmap(bitmap_orig, x, y)
           convertBitmapToByteBuffer(bitmap)
               tflite.run(imgData, labelProbArray)
           printLabel()
       }
        val uri=intent.getParcelableExtra<Uri>("IMAGE_URI")
        var bitmap=MediaStore.Images.Media.getBitmap(contentResolver,uri)
         selected_image.setImageBitmap(bitmap)

    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap) {
     if(imgData==null)
         return
       imgData?.rewind()
        bitmap.getPixels(
            intValues,
            0,
            bitmap.width,
            0,
            0,
            bitmap.width,
            bitmap.height
        )
        // loop through all pixels
        var pixel = 0
        for (i  in 0..DIM_IMG_SIZE_X) {
            for (j in 0..DIM_IMG_SIZE_Y) {
            val va = intValues!![pixel++]
                imgData!!.putFloat((((va shr 16 )and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                imgData!!.putFloat((((va shr 8) and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                imgData!!.putFloat(((va and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
    }}

    }

    private fun printLabel(){


        for (i in listLabels.indices) {
                sortedLabels.add(
                    AbstractMap.SimpleEntry(
                        listLabels.get(i),
                        labelProbArray!![0][i]
                    )
                )

            if (sortedLabels.size >RESULT_TO_SHOW) {
                sortedLabels.poll()
            }
        }
        val size = sortedLabels.size
        for (i in 0 until size) {
            val label =
                sortedLabels.poll()
            topLables!![i] = label.key
            topConfidence!![i] = String.format("%.0f%%", label.value * 100)
        }

        // set the corresponding textviews with the results
        label1.text = "1. " + topLables!![2]
        label2.text = "2. " + topLables!![1]
        label3.text = "3. " + topLables!![0]
        Confidence1.text = topConfidence!![2]
        Confidence2.text = topConfidence!![1]
        Confidence3.text = topConfidence!![0]
    }

    private fun getResizedBitmap(bitmapOrig: Bitmap, dimImgSizeX: Float, dimImgSizeY: Float): Bitmap {
        val width: Int = bitmapOrig.width
        val height: Int = bitmapOrig.height
        val scaleWidth = dimImgSizeX  / width
        val scaleHeight = dimImgSizeY / height
        val matrix = Matrix()
        matrix.postScale(scaleWidth, scaleHeight)
        return Bitmap.createBitmap(
            bitmapOrig, 0, 0, width, height, matrix, false
        )
    }

    private fun LoadLabelsList(): List<String> {
        val labelList: MutableList<String> =
            ArrayList()
        val reader =
            BufferedReader(InputStreamReader(this.assets.open("labels.txt")))
        var line: String?=reader.readLine()
        while (line!= null) {
            labelList.add(line)
            line=reader.readLine()
        }
        reader.close()
        return labelList
    }

    private fun LoadModelFile(): ByteBuffer {
     val fd=this.assets.openFd(Select!!)
    val inputStream=FileInputStream(fd.fileDescriptor)
        val fc=inputStream.channel
        val startOffset=fd.startOffset
        val dl=fd.declaredLength
       val va= fc.map(FileChannel.MapMode.READ_ONLY,startOffset,dl)
       return va
    }
}
