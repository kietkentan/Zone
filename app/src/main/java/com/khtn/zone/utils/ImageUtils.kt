@file:Suppress("DEPRECATION")

package com.khtn.zone.utils

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.webkit.MimeTypeMap
import android.widget.ImageView
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.canhub.cropper.CropImage
import com.canhub.cropper.CropImageView
import com.khtn.zone.R
import com.khtn.zone.custom.dialog.DialogImageResourceSheet
import com.khtn.zone.custom.dialog.SheetListener
import java.io.*
import kotlin.random.Random


object ImageUtils {
    private const val FROM_GALLERY = 116
    private const val TAKE_PHOTO = 111

    private var photoUri: Uri? = null

    fun askPermission(context: Fragment) {
        if (checkStoragePermission(context))
            showCameraOptions(context)
    }

    fun loadUserImage(
        imageView: ImageView,
        imgUrl: String
    ){
        Glide.with(imageView.context)
            .load(imgUrl)
            .placeholder(R.drawable.ic_other_user)
            .error(R.drawable.ic_other_user)
            .into(imageView)
    }

    private fun checkStoragePermission(context: Fragment): Boolean {
        return Utils.checkPermission(
            context, Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
        )
    }

    private fun showCameraOptions(context: Fragment) {
        photoUri = null
        val builder = DialogImageResourceSheet.newInstance(Bundle())
        builder.addListener(object : SheetListener {
            override fun selectedItem(index: Int) {
                when (index) {
                    ImageResourceSheetOptions.CAMERA -> takePhoto(context.requireActivity())
                    ImageResourceSheetOptions.GALLERY -> chooseGallery(context.requireActivity())
                    ImageResourceSheetOptions.CANCEL -> return
                }
            }
        })
        builder.show(context.childFragmentManager, "")
    }

    fun chooseGallery(context: Activity) {
        try {
            val intent = Intent(Intent.ACTION_PICK)
            intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
            context.startActivityForResult(intent, FROM_GALLERY)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun takePhoto(context: Activity) {
        val fileName = "Snap_" + System.currentTimeMillis() / 1000 + ".jpg"
        openCameraIntent(context, MediaStore.ACTION_IMAGE_CAPTURE, fileName, TAKE_PHOTO)
    }

    @SuppressLint("QueryPermissionsNeeded")
    @Suppress("SameParameterValue")
    private fun openCameraIntent(
        context: Activity,
        action: String,
        fileName: String,
        reqCode: Int
    ) {
        try {
            val intent = Intent(action)
            if (intent.resolveActivity(context.packageManager) != null) {
                val file = File(createImageFolder(context, ""), fileName)
                photoUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                    FileProvider.getUriForFile(context, providerPath(context), file)
                else Uri.fromFile(file)
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                context.startActivityForResult(intent, reqCode)
                context.overridePendingTransition(
                    android.R.anim.fade_in,
                    android.R.anim.fade_out
                )
            } else
                context.toast(context.getString(R.string.camera_not_available))
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    fun cropImage(
        context: Activity,
        data: Intent?,
        squareCrop: Boolean = true
    ) {
        val imgUri: Uri? = getPhotoUri(data)
        imgUri?.let {
            val cropImage = CropImage.activity(imgUri)
                .setOutputCompressFormat(Bitmap.CompressFormat.JPEG)
                .setGuidelines(CropImageView.Guidelines.ON)
                .setNoOutputImage(false)
            if (squareCrop)
                cropImage.setAspectRatio(1, 1)
            cropImage.start(context)
        }
    }

    fun getCroppedImage(data: Intent?): Uri? {
        try {
            val result = CropImage.getActivityResult(data)
            return result?.originalUri
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun getPhotoUri(data: Intent?): Uri? {
        return if (data == null || data.data == null) photoUri else data.data
    }

    private fun createImageFolder(
        context: Context,
        path: String
    ): String? {
        val folderPath = context.getExternalFilesDir("")
            ?.absolutePath + "/" + context.getString(R.string.app_name)
        try {
            val file = File("$folderPath/$path")
            if (!file.exists())
                file.mkdirs()
            return file.absolutePath
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        return folderPath
    }

    private fun providerPath(context: Context): String {
        return context.packageName + ".fileprovider"
    }

    fun onImagePerResult(
        context: Fragment,
        vararg result: Int
    ) {
        if (Utils.isPermissionOk(*result))
            showCameraOptions(context)
        else
            context.toast(context.getString(R.string.file_p_error))
    }

    @SuppressLint("Range")
    fun getUriPath(
        context: Context,
        uri: Uri,
        vararg data: String
    ): String? {
        return if (uri.toString()
                .contains(providerPath(context))
        ) uri.path else if (isGoogleOldPhotosUri(uri)) uri.lastPathSegment else if (isGoogleNewPhotosUri(
                uri
            ) || isPicasaPhotoUri(uri)
        ) copyFile(context, uri, *data) else {
            val result: String?
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            if (cursor == null) result = uri.path else {
                cursor.moveToFirst()
                result = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DATA))
                cursor.close()
            }
            result ?: ""
        }
    }

    private fun isGoogleOldPhotosUri(uri: Uri): Boolean {
        return "com.google.android.apps.photos.content" == uri.authority
    }

    private fun isGoogleNewPhotosUri(uri: Uri): Boolean {
        return "com.google.android.apps.photos.contentprovider" == uri.authority
    }

    private fun isPicasaPhotoUri(uri: Uri?): Boolean {
        return (uri != null && !TextUtils.isEmpty(uri.authority)
                && (uri.authority!!.startsWith("com.android.gallery3d")
                || uri.authority!!.startsWith("com.google.android.gallery3d")))
    }

    private fun copyFile(
        context: Context,
        uri: Uri,
        vararg data: String
    ): String {
        var filePath: String
        var inputStream: InputStream? = null
        var outStream: BufferedOutputStream? = null
        try {
            val extension = getExtension(context, uri, data[1])
            inputStream = context.contentResolver.openInputStream(uri)
            val extDir = context.externalCacheDir
            if (extDir == null || inputStream == null) return ""
            filePath = (extDir.absolutePath + "/" + data[0]
                    + "_" + Random.nextInt(100) + extension)
            outStream = BufferedOutputStream(FileOutputStream(filePath))
            val buf = ByteArray(2048)
            var len: Int
            while (inputStream.read(buf).also { len = it } > 0) {
                outStream.write(buf, 0, len)
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            filePath = ""
        } finally {
            try {
                inputStream?.close()
                outStream?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return filePath
    }

    private fun getExtension(
        context: Context,
        uri: Uri,
        actual: String
    ): String {
        try {
            val extension: String? = if (uri.scheme != null && uri.scheme == ContentResolver.SCHEME_CONTENT) {
                val mime = MimeTypeMap.getSingleton()
                mime.getExtensionFromMimeType(context.contentResolver.getType(uri))
            } else MimeTypeMap.getFileExtensionFromUrl(
                Uri.fromFile(uri.path?.let { File(it) }).toString()
            )
            return if (extension == null || extension.isEmpty()) actual else extension
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        return actual
    }

    fun loadGalleryImage(
        url: String,
        imageView: ImageView
    ) {
        Glide.with(imageView.context)
            .load(url)
            .placeholder(R.drawable.ic_gallery_place_holder)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .error(R.drawable.ic_broken_image)
            .into(imageView)
    }
}