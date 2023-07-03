package com.khtn.zone.utils

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.webkit.MimeTypeMap
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.ImageViewTarget
import com.canhub.cropper.CropImage
import com.canhub.cropper.CropImageView
import com.google.android.material.imageview.ShapeableImageView
import com.khtn.zone.MyApplication
import com.khtn.zone.R
import com.khtn.zone.custom.dialog.DialogImageResourceSheet
import com.khtn.zone.custom.dialog.SheetListener
import com.khtn.zone.model.SetSticker
import com.khtn.zone.model.Sticker
import java.io.*
import kotlin.random.Random


object ImageUtils {
    const val FROM_GALLERY = 116
    const val TAKE_PHOTO = 111
    const val REQUEST_IMAGE_CAMERA_PERMISSION = 23

    val IMAGE_CAMERA_PERMISSION = arrayOf(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.CAMERA
    )

    private var photoUri: Uri? = null

    fun askImageCameraPermission(context: Fragment) {
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

    fun loadUserImage(
        imageView: ShapeableImageView,
        imgUrl: String
    ){
        Glide.with(imageView.context)
            .load(imgUrl)
            .placeholder(R.drawable.ic_other_user)
            .error(R.drawable.ic_other_user)
            .into(imageView)
    }

    fun loadSingleSticker(
        imageView: ImageView,
        progressBar: ProgressBar,
        sticker: Sticker
    ) {
        try {
            progressBar.showView()
            val width = (MyApplication.getMaxWidth().pxToDp - imageView.context.resources.getDimension(R.dimen.dp40)) / 5

            imageView.layoutParams.width = width.dpToPx.toInt()

            Glide.with(imageView.context)
                .load(sticker.url)
                .placeholder(R.drawable.holder_sticker)
                .error(R.drawable.holder_sticker)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(p0: GlideException?, p1: Any?, p2: com.bumptech.glide.request.target.Target<Drawable>?, p3: Boolean): Boolean {
                        return false
                    }

                    override fun onResourceReady(p0: Drawable?, p1: Any?, p2: com.bumptech.glide.request.target.Target<Drawable>?, p3: DataSource?, p4: Boolean): Boolean {
                        if (sticker.type == ImageTypeConstants.GIF) {
                            (p0 as GifDrawable).setLoopCount(1)
                            p0.registerAnimationCallback(object : Animatable2Compat.AnimationCallback() {
                                override fun onAnimationEnd(drawable: Drawable) {
                                    progressBar.hideView()
                                }
                            })
                        } else progressBar.hideView()

                        return false
                    }
                })
                .into(imageView)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadSetSticker(
        imageView: ImageView,
        progressBar: ProgressBar,
        setSticker: SetSticker
    ) {
        try {
            progressBar.showView()

            Glide.with(imageView.context)
                .load(setSticker.image)
                .placeholder(R.drawable.holder_sticker)
                .error(R.drawable.holder_sticker)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(p0: GlideException?, p1: Any?, p2: com.bumptech.glide.request.target.Target<Drawable>?, p3: Boolean): Boolean {
                        return false
                    }

                    override fun onResourceReady(p0: Drawable?, p1: Any?, p2: com.bumptech.glide.request.target.Target<Drawable>?, p3: DataSource?, p4: Boolean): Boolean {
                        if (setSticker.type == ImageTypeConstants.GIF) {
                            (p0 as GifDrawable).setLoopCount(1)
                            p0.registerAnimationCallback(object : Animatable2Compat.AnimationCallback() {
                                override fun onAnimationEnd(drawable: Drawable) {
                                    progressBar.hideView()
                                }
                            })
                        } else progressBar.hideView()

                        return false
                    }
                })
                .into(imageView)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Suppress("KotlinConstantConditions")
    fun loadMsgImage(
        imgView: ImageView,
        url: String,
        imageType: String,
        isGiftSticker: Boolean = false
    ) {
        try {
            val layoutParam = imgView.layoutParams
            when (imageType) {
                ImageTypeConstants.GIF -> {
                    Glide.with(imgView.context)
                        .asGif()
                        .load(url)
                        .placeholder(R.drawable.gif)
                        .error(R.drawable.gif)
                        .into(object: ImageViewTarget<GifDrawable>(imgView) {
                            override fun setResource(resource: GifDrawable?) {
                                val height = resource?.firstFrame?.height
                                val width = resource?.firstFrame?.width

                                height?.let {
                                    width?.let {
                                        val w = imgView.context.applicationContext.resources.displayMetrics.widthPixels.pxToDp *
                                                if (height/width <= 1.2f) 4/5 else 1/2
                                        layoutParam.width = w
                                        layoutParam.height = height*w/width
                                    }
                                }.run {
                                    layoutParam.width = 0
                                    layoutParam.height = 0
                                }
                                imgView.layoutParams = layoutParam
                                imgView.setImageDrawable(resource)
                            }
                        })
                }

                ImageTypeConstants.STICKER -> {
                    Glide.with(imgView.context)
                        .load(url)
                        .placeholder(R.drawable.holder_sticker)
                        .error(R.drawable.holder_sticker)
                        .listener(object : RequestListener<Drawable> {
                            override fun onLoadFailed(p0: GlideException?, p1: Any?, p2: com.bumptech.glide.request.target.Target<Drawable>?, p3: Boolean): Boolean {
                                return false
                            }

                            override fun onResourceReady(p0: Drawable?, p1: Any?, p2: com.bumptech.glide.request.target.Target<Drawable>?, p3: DataSource?, p4: Boolean): Boolean {
                                if (isGiftSticker)
                                        (p0 as GifDrawable).setLoopCount(1)

                                return false
                            }
                        })
                        .into(imgView)
                }

                else -> {
                    Glide.with(imgView.context)
                        .asBitmap()
                        .load(url)
                        .placeholder(R.drawable.ic_gal_pholder)
                        .error(R.drawable.ic_gal_pholder)
                        .into(object: ImageViewTarget<Bitmap>(imgView) {
                            override fun setResource(resource: Bitmap?) {
                                val height = resource?.height
                                val width = resource?.width

                                height?.let {
                                    width?.let {
                                        val w = imgView.context.applicationContext.resources.displayMetrics.widthPixels.pxToDp *
                                                if (height/width <= 1.2f) 4/5 else 1/2
                                        layoutParam.width = w
                                        layoutParam.height = height*w/width
                                    }
                                }.run {
                                    layoutParam.width = 0
                                    layoutParam.height = 0
                                }
                                imgView.layoutParams = layoutParam
                                imgView.setImageBitmap(resource)
                            }
                        })
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkStoragePermission(context: Fragment): Boolean {
        return Utils.checkPermission(
            context = context,
            permissions = IMAGE_CAMERA_PERMISSION,
            reqCode = REQUEST_IMAGE_CAMERA_PERMISSION
        )
    }

    fun showCameraOptions(context: Fragment) {
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

    @Suppress("DEPRECATION")
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
            context.toast(context.getString(R.string.permission_error))
    }

    @SuppressLint("Range")
    fun getUriPath(
        context: Context,
        uri: Uri,
        vararg data: String
    ): String? {
        return if (uri.toString().contains(providerPath(context))) uri.path else if (isGoogleOldPhotosUri(uri)) uri.lastPathSegment else if (isGoogleNewPhotosUri(uri)
            || isPicasaPhotoUri(uri))
            copyFile(context, uri, *data)
        else {
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
            return if (extension.isNullOrEmpty()) actual else extension
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