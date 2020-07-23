package com.gopal.cdd

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.soundcloud.android.crop.Crop
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File


class MainActivity : AppCompatActivity() {
   private  val REQUEST_PERMISSION=123
   // private val GAL_ACCESS=124
    private val REQUEST_IMAGE=124
    private val PICK_IMAGE=124
    private var select:String="output.tflite"
    private  var imageUri:Uri?=null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
            if(ContextCompat.checkSelfPermission(this.applicationContext,Manifest.permission.CAMERA)==PackageManager.PERMISSION_DENIED){
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.CAMERA),REQUEST_PERMISSION)
            }}
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M&&ContextCompat.checkSelfPermission(this.applicationContext,Manifest.permission.WRITE_EXTERNAL_STORAGE)==PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(this,arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),REQUEST_PERMISSION)

        }

        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M&&ContextCompat.checkSelfPermission(this.applicationContext,Manifest.permission.READ_EXTERNAL_STORAGE)==PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),REQUEST_PERMISSION)
        }
      main_background.setBackgroundResource(R.drawable.front)
        camera.setOnClickListener {
                openCamera();
        }
        gal.setOnClickListener {

                openGallery()

        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode ==REQUEST_PERMISSION) {
            if (!(grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Toast.makeText(
                    applicationContext,
                    "This application needs read, write, and camera permissions to run. Application now closing.",
                    Toast.LENGTH_LONG
                )
                System.exit(0)
            }
    }}

    private fun openGallery() {
     val i= Intent()
        i.type="image/*"
        i.action=Intent.ACTION_GET_CONTENT
   startActivityForResult(Intent.createChooser(i,"Select Image"),PICK_IMAGE)

    }

    private fun openCamera() {
        // Define the file-name to save photo taken by Camera activity

        var values=ContentValues()
        values.put(MediaStore.Images.Media.TITLE,"NEW PIC")
        values.put(MediaStore.Images.Media.DESCRIPTION,"FROM CAMERA")
       imageUri=contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values)
        val int=Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        int.putExtra(MediaStore.EXTRA_OUTPUT,imageUri)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
        startActivityForResult(int,REQUEST_IMAGE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode==REQUEST_IMAGE&&resultCode== Activity.RESULT_OK){
              val source_uri = imageUri
              val dest_uri =
                  Uri.fromFile(File(cacheDir, "cropped"))
              Crop.of(source_uri, dest_uri).asSquare().start(this)

        }
        else if(requestCode==PICK_IMAGE&&resultCode== Activity.RESULT_OK&&data!=null&&data.data!=null)
        {
            imageUri = data.data
            val source_uri = imageUri
            val dest_uri =
                Uri.fromFile(File(cacheDir, "cropped"))
            Crop.of(source_uri, dest_uri).asSquare().start(this)
        }
      else if(requestCode==Crop.REQUEST_CROP&&resultCode== Activity.RESULT_OK){
            imageUri=Crop.getOutput(data)
            val i=Intent(this,Classify::class.java)
            i.putExtra("IMAGE_URI",imageUri)
            i.putExtra("SELECT",select)
            startActivity(i)
        }
    }
}
