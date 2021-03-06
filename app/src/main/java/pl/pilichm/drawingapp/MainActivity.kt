package pl.pilichm.drawingapp

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.os.AsyncTask
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.get
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dialog_brush_size.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {
    private var mImageButtonCurrentPaint: ImageButton? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawing_view.setSizeForBrush(20.toFloat())

        mImageButtonCurrentPaint = ll_paint_colors[1] as ImageButton
        mImageButtonCurrentPaint!!.setImageDrawable(
            ContextCompat.getDrawable(this, R.drawable.pallet_selected))

        setBrushSizeButton()
        setGalleryButton()
        setRedoUndoButtons()
        setSaveButton()
        setRandomColor()
    }


    private fun setBrushSizeButton(){
        ib_brush.setOnClickListener{
            showBrushSizeChooserDialog()
        }
    }

    private fun setGalleryButton(){
        ib_gallery.setOnClickListener {
            if (isStorageAllowed()) {
                val pickPhoto = Intent(
                    Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                )
                startActivityForResult(pickPhoto, GALLERY)
            } else {
                requestStoragePermission()
            }
        }
    }

    private fun setRedoUndoButtons(){
        ib_undo.setOnClickListener {
            drawing_view.modifyPaths(true)
        }

        ib_redo.setOnClickListener {
            drawing_view.modifyPaths(false)
        }
    }

    private fun setSaveButton(){
        ib_save.setOnClickListener {
            if (isStorageAllowed()){
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Choose filename")

                val textInput = EditText(this)
                textInput.setText("DrawingApp_${System.currentTimeMillis()/1000}")
                textInput.inputType = InputType.TYPE_CLASS_TEXT
                builder.setView(textInput)

                builder.setPositiveButton("Ok"){
                        _, _ ->
                    BitmapAsyncTask(getBitmapFromView(fl_drawing_view_container),
                        textInput.text.toString()).execute()
                }

                builder.setNegativeButton("Cancel"){
                        dialog, _ -> dialog.cancel()
                }

                builder.show()
            } else {
                requestStoragePermission()
            }
        }
    }

    private fun setRandomColor(){
        ib_color_random.background = Color.rgb(
            (1..255).random(), (1..255).random(), (1..255).random()
        ).toDrawable()
    }

    private fun showBrushSizeChooserDialog(){
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush size:")

        val smallBrush = brushDialog.findViewById<ImageButton>(R.id.ib_small_brush)
        val mediumBrush = brushDialog.findViewById<ImageButton>(R.id.ib_medium_brush)
        val largeBrush = brushDialog.findViewById<ImageButton>(R.id.ib_large_brush)
        val sbBrushSize = brushDialog.findViewById<SeekBar>(R.id.sb_brush_size)

        smallBrush.setOnClickListener{
            drawing_view.setSizeForBrush(10.toFloat())
            brushDialog.dismiss()
        }

        mediumBrush.setOnClickListener{
            drawing_view.setSizeForBrush(20.toFloat())
            brushDialog.dismiss()
        }

        largeBrush.setOnClickListener{
            drawing_view.setSizeForBrush(30.toFloat())
            brushDialog.dismiss()
        }

        sbBrushSize.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {}

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seek: SeekBar) {
                drawing_view.setSizeForBrush(seek.progress.toFloat())
            }
        })

        brushDialog.show()
    }

    fun paintClicked(view: View){
        if (view != mImageButtonCurrentPaint){
            val imageButton = view as ImageButton
            val colorTag = imageButton.tag.toString()
            drawing_view.setColor(colorTag)
            imageButton.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallet_selected))
            mImageButtonCurrentPaint!!.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallet_normal))
            mImageButtonCurrentPaint = imageButton
        }
    }

    private fun requestStoragePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ).toString()
            )
        ) {
        }

        ActivityCompat.requestPermissions(
            this, arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ),
            STORAGE_PERMISSION_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode== STORAGE_PERMISSION_CODE){
            if (grantResults.isNotEmpty()&&grantResults[0]==PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun isStorageAllowed(): Boolean =
        PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode== Activity.RESULT_OK){
            if (requestCode== GALLERY){
                try {
                    if (data!!.data!=null){
                        iv_background.visibility = View.VISIBLE
                        iv_background.setImageURI(data.data)
                    } else {
                        Toast.makeText(this,
                            "Something wrong with the image", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception){
                    e.printStackTrace()
                }
            }
        }
    }

    private fun getBitmapFromView(view: View): Bitmap {
        val returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        val bgDrawable = view.background
        if (bgDrawable!=null){
            bgDrawable.draw(canvas)
        } else {
            canvas.drawColor(Color.WHITE)
        }
        view.draw(canvas)
        return  returnedBitmap
    }

    private inner class BitmapAsyncTask(val bitmap: Bitmap, val fileName: String):
        AsyncTask<Any, Void, String>(){

        private lateinit var mProgressBar: Dialog

        override fun onPreExecute() {
            super.onPreExecute()
            showProgressDialog()
        }

        override fun doInBackground(vararg params: Any?): String {
            var result = ""
            if (bitmap!=null){
                try {
                    val bytes = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)
                    val file = File(externalCacheDir!!.absoluteFile.toString()
                            + File.separator + fileName + ".png")
                    val fos = FileOutputStream(file)
                    fos.write(bytes.toByteArray())
                    fos.close()
                    result = file.absolutePath
                } catch (e: Exception){
                    result = ""
                    e.printStackTrace()
                }
            }
            return result
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            cancelProgressDialog()
            if (!result.isNullOrEmpty()){
                Toast.makeText(this@MainActivity, "File saved $result", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this@MainActivity, "Error saving file", Toast.LENGTH_LONG).show()
            }
            MediaScannerConnection.scanFile(this@MainActivity, arrayOf(result), null){
                    _, uri -> val shareIntent = Intent()
                shareIntent.action = Intent.ACTION_SEND
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
                shareIntent.type = "image/png"
                startActivity(Intent.createChooser(shareIntent, "Share"))
            }

        }

        private fun showProgressDialog(){
            mProgressBar = Dialog(this@MainActivity)
            mProgressBar.setContentView(R.layout.dialog_custom_progress)
            mProgressBar.show()
        }

        private fun cancelProgressDialog(){
            mProgressBar.dismiss()
        }
    }

    companion object {
        private const val STORAGE_PERMISSION_CODE = 1
        private const val GALLERY = 2
    }
}