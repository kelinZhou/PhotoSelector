package com.kelin.photoselectordemo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.net.toFile
import com.kelin.photoselector.PhotoSelector
import com.kelin.photoselector.model.PhotoImpl
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnTakePhoto.setOnClickListener {
            PhotoSelector.takePhoto(this){
                PhotoSelector.openPicturePreviewPage(this, listOf(PhotoImpl(it.absolutePath)))
            }
        }

        btnTakeVideo.setOnClickListener {
            PhotoSelector.takeVideo(this){
                PhotoSelector.openPicturePreviewPage(this, listOf(PhotoImpl(it.absolutePath)))
            }
        }

        btnPhotoSelect.setOnClickListener {
            PhotoSelector.openPhotoSelector(this) { photos ->
                if (photos.isEmpty()) {
                    Toast.makeText(this, "选择已被取消", Toast.LENGTH_SHORT).show()
                } else {
                    ImageListActivity.start(this, *photos.map { it.uri }.toTypedArray())
                }
            }
        }

        btnVideoSelect.setOnClickListener {
            PhotoSelector.openVideoSelector(this) { photos ->
                if (photos.isEmpty()) {
                    Toast.makeText(this, "选择已被取消", Toast.LENGTH_SHORT).show()
                } else {
                    ImageListActivity.start(this, *photos.map { it.uri }.toTypedArray())
                }
            }
        }

        btnPhotoAndVideoSelect.setOnClickListener {
            PhotoSelector.openPictureSelector(this) { photos ->
                if (photos.isEmpty()) {
                    Toast.makeText(this, "选择已被取消", Toast.LENGTH_SHORT).show()
                } else {
                    ImageListActivity.start(this, *photos.map { it.uri }.toTypedArray())
                }
            }
        }

        btnPreviewPictures.setOnClickListener {
            PhotoSelector.openPicturePreviewPage(
                this, listOf(
                    PhotoImpl("http://test-cloud-yxholding-com.oss-cn-shanghai.aliyuncs.com/yx-users/13509/16121382-7c1e-4a29-9a3d-83cea2d89613.mp4"),
                    PhotoImpl("https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1594798038139&di=fa8ec9fefaa0a63f691f61b97a7973c5&imgtype=0&src=http%3A%2F%2Fimg0.imgtn.bdimg.com%2Fit%2Fu%3D2811584385%2C4107951140%26fm%3D214%26gp%3D0.jpg"),
                    PhotoImpl("http://test-cloud-yxholding-com.oss-cn-shanghai.aliyuncs.com/yx-users/20425/81c9502a-e8a3-4ffb-8830-5dbea04a3f58.mp4"),
                    PhotoImpl("http://test-cloud-yxholding-com.oss-cn-shanghai.aliyuncs.com/yx-users/20425/81c9502a-e8a3-4ffb-8830-5dbea04a323rvsd3f58.mp4"),
                    PhotoImpl("https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1594798037700&di=13c506d61f14c08e9b28a189118133ab&imgtype=0&src=http%3A%2F%2Fattach.bbs.miui.com%2Fforum%2F201205%2F07%2F200343cx0b5wwqdp0wbdb3.jpg"),
                    PhotoImpl("https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1594798037699&di=a6b5909d028e1b6f11aaa37429f4ca67&imgtype=0&src=http%3A%2F%2Fgss0.baidu.com%2F-vo3dSag_xI4khGko9WTAnF6hhy%2Fzhidao%2Fpic%2Fitem%2Fa8014c086e061d954f51db5679f40ad162d9ca4d.jpg"),
                    PhotoImpl("http://test-cloud-yxholding-com.oss-cn-shanghai.aliyuncs.com/yx-users/20425/786910ff-e5b4-4081-97cc-b732ededc46c.mp4"),
                    PhotoImpl("https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1594798037700&di=7b4c89fdcb5a1ede4f13ca642d743907&imgtype=0&src=http%3A%2F%2Fb-ssl.duitang.com%2Fuploads%2Fitem%2F201511%2F28%2F20151128182741_Mywkf.png")
                )
            )
        }
    }
}