# PhotoSelector
自定义相册图片、视频视频选择，提供简单好用的自定义相册图片视频选择工具。

## 简介
打造最简单易用的自定义相册库，提供图片选择、视频选择、自动去重、图片视频预览、调用系统相机拍照、调用系统相机录像等功能。
**该库给予Androidx，没有迁移到Androidx的同学可能无法使用，请谅解。**


#### 主要功能：

1. 图片和视频选择。支持只选择图片、只选择视频、同时选择图片和视频。

2. 图片视频预览。可直接预览本地或网络上的图片和视频。视频播放采用的是系统的播放功能。

3. 调起相机拍照。

4. 调起相机录像。

#### 主要特点：

1.自带Android6.0以上的动态权限校验，开发者无需关心权限问题。

2.Callback的调用方式，使用PhotoSelector打开图片库选择图片或视频后可直接在回调中拿到结果，逻辑更清晰代码更简洁，一行代码即可实现图片选择的功能。

3.支持同时选择多张图片或多个视频(可手动设置最大个数，默认为9个)。

4.自动去重，自动回显已经选中了的图片或视频，防止选择相同的资源(默认支持，可手动设置)。


## 体验

## 下载
###### 第一步：添加 JitPack 仓库到你项目根目录的 gradle 文件中。
```groovy
allprojects {
    repositories {
        //...
        maven { url 'https://jitpack.io' }
    }
}
```
###### 第二步：添加这个依赖。
```groovy
dependencies {
    implementation 'com.github.kelinZhou:PhotoSelector:0.5.0'
}
```
## 效果图

## 使用

1. 拍摄照片
```kotlin
PhotoSelector.takePhoto(context){
    if (it != null) {
        //do something…
    } else {
        Toast.makeText(applicationContext, "拍照失败", Toast.LENGTH_SHORT).show()
    }
}
```

2. 拍摄视频
```kotlin
PhotoSelector.takeVideo(context){
    if (it != null) {
        //do something…
    } else {
        Toast.makeText(applicationContext, "录像失败", Toast.LENGTH_SHORT).show()
    }
}
```

3. 选择图片
```kotlin
PhotoSelector.openPhotoSelector(context) { photos ->
    if (photos.isEmpty()) {
        Toast.makeText(context, "选择已被取消", Toast.LENGTH_SHORT).show()
    } else {
        //do something…
    }
}
```

4. 选择视频
```kotlin
PhotoSelector.openVideoSelector(context) { photos ->
    if (photos.isEmpty()) {
        Toast.makeText(context, "选择已被取消", Toast.LENGTH_SHORT).show()
    } else {
        //do something…
    }
}
```
5. 选择图片和视频
```kotlin
PhotoSelector.openPictureSelector(context) { photos ->
    if (photos.isEmpty()) {
        Toast.makeText(context, "选择已被取消", Toast.LENGTH_SHORT).show()
    } else {
        //do something…
    }
}
```
6. 图片或视频预览
```kotlin
PhotoSelector.openPicturePreviewPage(
    context, listOf(
        PhotoImpl("https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1594798038139&di=fa8ec9fefaa0a63f691f61b97a7973c5&imgtype=0&src=http%3A%2F%2Fimg0.imgtn.bdimg.com%2Fit%2Fu%3D2811584385%2C4107951140%26fm%3D214%26gp%3D0.jpg"),
        PhotoImpl("https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1594798037700&di=13c506d61f14c08e9b28a189118133ab&imgtype=0&src=http%3A%2F%2Fattach.bbs.miui.com%2Fforum%2F201205%2F07%2F200343cx0b5wwqdp0wbdb3.jpg"),
        PhotoImpl("https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1594798037699&di=a6b5909d028e1b6f11aaa37429f4ca67&imgtype=0&src=http%3A%2F%2Fgss0.baidu.com%2F-vo3dSag_xI4khGko9WTAnF6hhy%2Fzhidao%2Fpic%2Fitem%2Fa8014c086e061d954f51db5679f40ad162d9ca4d.jpg"),
        PhotoImpl("https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1594798037700&di=7b4c89fdcb5a1ede4f13ca642d743907&imgtype=0&src=http%3A%2F%2Fb-ssl.duitang.com%2Fuploads%2Fitem%2F201511%2F28%2F20151128182741_Mywkf.png")
    )
)
```

* * *
### License
```
Copyright 2016 kelin410@163.com

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```