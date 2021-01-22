/*
 * Copyright (c) 2019 Razeware LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * Notwithstanding the foregoing, you may not use, copy, modify, merge, publish,
 * distribute, sublicense, create a derivative work, and/or sell copies of the
 * Software in any work that is designed, intended, or marketed for pedagogical or
 * instructional purposes related to programming, coding, application development,
 * or information technology.  Permission for such use, copying, modification,
 * merger, publication, distribution, sublicensing, creation of derivative works,
 * or sale is expressly withheld.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */


package com.raywenderlich.memeify

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider.getUriForFile
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_take_picture.*
import java.io.File


class TakePictureActivity : Activity(), View.OnClickListener {

    private var selectedPhotoPath: Uri? = null

    private var pictureTaken: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_take_picture)

        pictureImageview.setOnClickListener(this)
        enterTextButton.setOnClickListener(this)

        checkReceivedIntent()

    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.pictureImageview -> {
                var items: Any
                if (isDeviceSupportCamera()) {
                    items = arrayOfNulls<CharSequence>(2)
                    items[0] = "Camera"
                    items[1] = "Gallery"
                } else {
                    items = arrayOfNulls<CharSequence>(1)
                    items[0] = "Gallery"
                }

                val alertDialog = AlertDialog.Builder(this)
                alertDialog.setTitle("Add Image")
                alertDialog.setItems(items) { _, item ->
                    if (items[item]!! == "Camera") {
                        takePictureWithCamera()
                    } else if (items[item]!! == "Gallery") {
                        takePictureFromGallery()
                    }
                }
                alertDialog.show()
            }
            R.id.enterTextButton -> moveToNextScreen()
            else -> println("No case satisfied")
        }
    }

    private fun isDeviceSupportCamera(): Boolean {
        return packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)
    }

    private fun takePictureFromGallery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                requestPermissions(permissions, GALLERY_PERMISSION_CODE)
            } else {
                chooseImageGallery();
            }
        } else {
            chooseImageGallery();
        }
    }

    //https://stackoverflow.com/questions/30719047/android-m-check-runtime-permission-how-to-determine-if-the-user-checked-nev
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == GALLERY_PERMISSION_CODE) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted, yay!
                Toaster.show(this, R.string.permissions_granted)
            } else {

                val showRationale = shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)
                Log.i("Hello", "showRationale :: $showRationale")
                if (showRationale) {
                    // now, user has denied permission (but not permanently!)
                    Toaster.show(this, R.string.permissions_please)
                } else {
                    // now, user has denied permission permanently!
                    val snackBar = Snackbar.make(rootContainer,
                            "You have previously declined this permission.\n" + "You must approve this permission in \"Permissions\" in the app settings on your device.",
                            Snackbar.LENGTH_LONG
                    ).setAction("Settings") {
                        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + BuildConfig.APPLICATION_ID)))
                    }
                    val snackBarView: View = snackBar.view
                    val textView = snackBarView.findViewById<View>(com.google.android.material.R.id.snackbar_text) as TextView
                    textView.maxLines = 5 //Or as much as you need
                    snackBar.show()
                }
            }
        }
    }

    private fun chooseImageGallery() {
        Toast.makeText(this, "Pick Image from gallery", Toast.LENGTH_SHORT).show()
    }

    private fun takePictureWithCamera() {
        // 1
        val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        // 2
        val imagePath = File(filesDir, "images")
        val newFile = File(imagePath, "default_image.jpg")
        if (newFile.exists()) {
            newFile.delete()
        } else {
            newFile.parentFile.mkdirs()
        }
        selectedPhotoPath = getUriForFile(this, BuildConfig.APPLICATION_ID + ".fileprovider", newFile)

        // 3
        captureIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, selectedPhotoPath)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            captureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        } else {
            val clip = ClipData.newUri(contentResolver, "A photo", selectedPhotoPath)
            captureIntent.clipData = clip
            captureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }

        startActivityForResult(captureIntent, TAKE_PHOTO_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == TAKE_PHOTO_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            setImageViewWithImage()
        }
    }

    private fun setImageViewWithImage() {
        val photoPath: Uri = selectedPhotoPath ?: return
        pictureImageview.post {
            val pictureBitmap = BitmapResizer.shrinkBitmap(
                    this@TakePictureActivity,
                    photoPath,
                    pictureImageview.width,
                    pictureImageview.height
            )
            pictureImageview.setImageBitmap(pictureBitmap)
        }
        lookingGoodTextView.visibility = View.VISIBLE
        pictureTaken = true
    }

    private fun moveToNextScreen() {
        if (pictureTaken) {
            val nextScreenIntent = Intent(this, EnterTextActivity::class.java).apply {
                putExtra(IMAGE_URI_KEY, selectedPhotoPath)
                putExtra(BITMAP_WIDTH, pictureImageview.width)
                putExtra(BITMAP_HEIGHT, pictureImageview.height)
            }

            startActivity(nextScreenIntent)
        } else {
            Toaster.show(this, R.string.select_a_picture)
        }
    }

    private fun checkReceivedIntent() {
        val imageReceivedIntent = intent
        val intentAction = imageReceivedIntent.action
        val intentType = imageReceivedIntent.type

        if (Intent.ACTION_SEND == intentAction && intentType != null) {
            if (intentType.startsWith(MIME_TYPE_IMAGE)) {
                selectedPhotoPath = imageReceivedIntent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                setImageViewWithImage()
            }
        }
    }

    companion object {
        private const val MIME_TYPE_IMAGE = "image/"
        private const val TAKE_PHOTO_REQUEST_CODE = 1
        private const val GALLERY_PERMISSION_CODE = 1001;
    }
}
