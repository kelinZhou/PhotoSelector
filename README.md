# PhotoSelector [![](https://jitpack.io/v/kelinZhou/PhotoSelector.svg)](https://jitpack.io/#kelinZhou/PhotoSelector)
自定义相册图片、视频视频选择，提供简单好用的自定义相册图片视频选择工具。

## 简介
打造最简单易用的自定义相册库，提供图片选择、视频选择、自动去重、图片视频预览、调用系统相机拍照、调用系统相机录像等功能。
**该库基于Androidx，没有迁移到Androidx的同学可能无法使用，请谅解。**


#### 主要功能：

1. 图片和视频选择。支持只选择图片、只选择视频、同时选择图片和视频。

2. 图片视频预览。可直接预览本地或网络上的图片和视频。视频播放采用的是Google的ExoPlayer。

3. 调起相机拍照。

4. 调起相机录像。

#### 主要特点：

1.自带Android6.0以上的动态权限校验，开发者无需关心权限问题。

2.Callback的调用方式，使用PhotoSelector打开图片库选择图片或视频后可直接在回调中拿到结果，逻辑更清晰代码更简洁，一行代码即可实现图片选择的功能。

3.支持同时选择多张图片或多个视频(可手动设置最大个数，默认为9个)。

4.自动去重，自动回显已经选中了的图片或视频，防止选择相同的资源(默认支持:默认使用当前Activity或则Fragment的hashCode作为id,也可手动设置id，可手动设置关闭去重,id为-1即可)。

## 更新


### [3.3.2] - 2024-9-14
#### Fixed
- 修复图片预览功能如果预览图片过多会崩溃的Bug。

### [3.3.1] - 2024-6-11
#### Fixed
- 修复图片未全部加载成功时退出页面会导致崩溃的Bug。

### [3.3.0] - 2024-6-6
#### Changed
- 优化图片过多时的加载卡顿问题。
- 优化同时设置maxSize和maxDuration时maxDuration无法生效的问题(如设置了maxDuration则不再校验视频文件的size，视频文件优先已maxDuration为准)。

### [3.2.1] - 2024-5-16
#### Fixed
- 修复ClassCaseException。

### [3.2.0] - 2024-4-18
#### Added
- Coil图片加载框架
#### Removed
- Glide图片加载框架
#### Changed
- 升级OkPermission框架。
- 将Glide图片加载框架替换为Coil。
- ImageLoader为Coil的核心，用户需要在初始化之前调用代码为Coil设置ImageLoader，例如`Coil.setImageLoader(imageLoader)`或`Coil.setImageLoader(imageLoaderFactory)`
- 也可以直接让Application实现`ImageLoaderFactory`接口并重写`newImageLoader`方法，例如下面的代码： 
```kotlin
class App : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
       //初始化PhotoSelector。
        PhotoSelector.init(this,"${packageName}.fileProvider", true, albumTakePictureEnable = false)
    }
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                //添加GIF支持
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
                //添加视频加载为图片的支持
                add(VideoFrameDecoder.Factory())
            }
            .build()
    }
}
```

### [3.1.0] - 2024-1-12
#### Added 
- 适配Android13
#### Changed
- 升级ExoPlayer到2.18.5
- 升级Kotlin版本到1.9.22
- 升级AGP到8.2

### 3.0.0 适配Android12，支持系统相册以及支持过滤文件大小(系统相册暂不支持)。

### 2.0.4 修复相册为空时会角标越界的问题。

### 2.0.2 修复部分机型可能会在打开相册后闪退的Bug。

### 2.0.1 为相册内拍照提供开关，支持关闭相册内拍照/录像功能。

### 2.0.0 从2.0.0开始图片、视频选择页面可以直接拍摄。

### 1.0.4 支持选择视频时的最大时长限制。

### 1.0.3 适配AndroidQ，变更默认的图片存储位置，适配AndroidQ通知相册刷新。

### 1.0.2 优化Activity的传值方式，尽量避免传值丢失的问题发生。

### 1.0.1 更新权限库，修复在Fragment中使用时有可能会产生Bug的问题。

### 1.0.0 优化使用修复Bug。
1. 修复相册视频列表中因视频文件太小或出现0kb文件而导致闪退的Bug。
2. 优化单选时的处理，具体如下：
    * 当 maxLength == 1 时用户点击选择框自动结束选择，并返回(通过回调)用户选择的结果。
    * 当 maxLength == 1 时底部的完成按钮变成标签样式。
    * 对所有openXXXSelector方法增加openXXXSelectorSingle方法，改方法的回调中只会返回一个Photo对象，不再返回List<Photo>，使用起来更加方便。
3. 增加了英文系统的国际化适配。

### 0.9.3 修复拍照后没有默认返回压缩后的图片的Bug。

### 0.9.2 修复由于在某些情况下onComposeFinished方法没有被调用而导致按下完成按钮后一直处于loading的状态的Bug。

### 0.9.1 变更PhotoSelectorActivity的包路径，变更后再清单文件注册方式如下：。
```xml
<activity android:name="com.kelin.photoselector.PhotoSelectorActivity"
          android:configChanges="screenSize|orientation"/>
```

### 0.9.0 将Activity替换为Fragment并增加视频播放功能(使用Google的ExoPlayer)。
    
    1.完成重构，将所有功能的具体实现都移至Fragment，并舍弃PhotoPreviewActivity。这样做的好处就是即使以后再扩展新的页面，使用者也无需再清单文件中增加新的Activity。
    
    2.基于Google的ExoPlayer库增加视频播放功能，支持网络视频和本地视频。原来调用系统播放视频的功能依然保留。调用方式如下：
```kotlin
//使用Google的ExoPlayer播放视频。
PhotoSelector.playVideo(activity, uri)
//使用Android系统的播放视频功能播放视频。
PhotoSelector.playVideoWithSystem(activity, uri)
```

### 0.8.1
优化压缩之后的处理逻辑以及优化代码。
    
    1.解决选择图片时如果在选中之后立即点击完成按钮将会有可能导致调用者无法拿到压缩后的图片的问题。
   
    2.优化代码逻辑，更加稳定。

### 0.8.0
优化图片预览交互体检增加自动压缩功能，取消0.7.1中的自动旋转的配置。

   1. 优化代码逻辑，使其更加稳定。
   
   2. 图片预览时用户可以单击图片退出预览，优化交互体验。
   
   3. 增加自动压缩功能，您可以在init方法中进行配置是否需要自动压缩，同时取消了自动旋转的配置，开启自动压缩则会同时开启自动旋转。开启方式如下：
```kotlin
PhotoSelector.init(this,"${packageName}.fileProvider", true)
```

### 0.7.1
解决部分机型拍照后会图片会旋转的问题，解决办法为选择图片或拍照时自动纠正图片角度。自动纠正功能默认不会开启，如要手动开启需要在init方法中设置，例如：
```kotlin
PhotoSelector.init(this,"${packageName}.fileProvider", true)
```

### 0.7.0
优化去重逻辑，拍照或拍视频后再打开自定义相册会默认勾选(依然是由id决定)

### 0.6.0 
PhotoSelector 增加 removeSelected 方法。

    1. 增加从外部取消选中的功能，使得使用者可以在选择完毕后调用方法对已选中的进行取消选中的操作(应对不同的使用场景)。
    2. 修复图片预览时因使用了错误的缩放模式而导致的图片显示不正常的Bug。

## 体验
[点击下载](http://d.firim.vip/cspz)或扫码下载DemoApk

![DemoApk](materials/apk_download.png)

## 下载
###### 第一步：添加 JitPack 仓库到你项目根目录的 gradle 文件中。
```groovy
allprojects {
    repositories {
        //...省略N行代码
        maven { url 'https://jitpack.io' }
    }
}
```
###### 第二步：app的gradle中添加这个依赖。
```groovy
dependencies {
    implementation 'com.github.kelinZhou:PhotoSelector:${last version here!}'
}
```
###### 第三步：打开Java8支持。
如果尚未启用，则需要在所有 build.gradle文件中打开Java 8支持，由于视频播放是使用的ExoPlayer库所以必须要打开Java8的支持。方法是在以下android部分添加以下内容：
```groovy
android {
    //...省略N行代码
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
}
```

## 效果图
![相册](materials/PhotoSelector_Album.png)
![视频播放](materials/PhotoSelector_PlayVideo.png)

## 使用
###### 第一步：添加PhotoSelectorActivity到你的清单文件中。
```xml
<activity android:name="com.kelin.photoselector.PhotoSelectorActivity"
          android:configChanges="screenSize|orientation"/>
```
###### 第二步：AndroidManifest.xml清单文件中添加PhotoSelector所需要的权限。
```xml
<!--网络权限，如果你需要预览网络图片或视频则必须添加改权限-->
<uses-permission android:name="android.permission.INTERNET" />
<!--相机权限，拍照、录像时的必要权限-->
<uses-permission android:name="android.permission.CAMERA" />
<!--读取外部存储权限，PhotoSelector库中所有功能都会使用到的权限-->
<uses-permission
    android:name="android.permission.READ_EXTERNAL_STORAGE"
    android:maxSdkVersion="32" />
<!--写入外部存储权限，PhotoSelector库中所有功能都会使用到的权限-->
<uses-permission
    android:name="android.permission.WRITE_EXTERNAL_STORAGE"
    android:maxSdkVersion="32"
    tools:ignore="ScopedStorage" />
<!--访问照片权限-->
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
<!--访问视频权限-->
<uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
```

**完成上面两步后就可以开始使用了。整个库的核心类就一个```PhotoSelector``。你只需要使用它的相应方法就能完成相应功能。具体Api如下：**

**注意：**在华为的HarmonyOS中可能会出现无法成功拉起相机的情况：如果出现则在`AndroidManifest.xml`清单文件中加入以下代码：
```xml
<manifest
    ...>
    <queries>
        <package android:name="com.huawei.camera" />  //启用华为相机
    </queries>
</manifest>
```

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
使用自定义相册
```kotlin
PhotoSelector.withSelectorAlbum(activity, AlbumType.PHOTO) { 
   selectId = activity.hashCode() //设置过滤条件，当前Activity中不能选择重复的图片，仅自定义相册支持。
   maxSize = 5F //设置过滤大于5M的图片，仅自定义相册支持。
   selectAll(9){ photos ->
      if (photos.isEmpty()) {
         Toast.makeText(context, "选择已被取消", Toast.LENGTH_SHORT).show()
      } else {
         //do something…
      }
   }
}
```
使用系统相册
```kotlin
PhotoSelector.withSysAlbum(activity, AlbumType.PHOTO) {
   selectAll(9){ photos ->
      if (photos.isEmpty()) {
         Toast.makeText(context, "选择已被取消", Toast.LENGTH_SHORT).show()
      } else {
         //do something…
      }
   }
}
```

4. 选择视频
使用自定义相册
```kotlin
PhotoSelector.withSelectorAlbum(activity, AlbumType.VIDEO) {
   maxDuration = 30  //过滤所有视频长度超过30秒的视频，仅自定义相册支持。
   maxSize = 20 //设置过滤大于20M的视频，仅自定义相册支持。
   selectAll(9){ photos ->
       if (photos.isEmpty()) {
          Toast.makeText(context, "选择已被取消", Toast.LENGTH_SHORT).show()
       } else {
          //do something…
       }
    }
}
```
使用系统相册
```kotlin
PhotoSelector.withSysAlbum(activity, AlbumType.VIDEO) {
   selectAll(9){ photos ->
      if (photos.isEmpty()) {
         Toast.makeText(context, "选择已被取消", Toast.LENGTH_SHORT).show()
      } else {
         //do something…
      }
   }
}
```
5. 选择图片和视频
图片和视频同时选择时与仅选择图片或视频是的使用方法一致，仅需要把`AlbumType`设置为`AlbumType.PHOTO_VIDEO`即可。

6. 单选
很多使用选择图片仅需要单选，单选时不要调用`selectAll`方法，而是调用`select`方法，下面已选择图片举例：

使用自定义相册
```kotlin
PhotoSelector.withSelectorAlbum(activity, AlbumType.PHOTO) { 
   select{ photo ->
      if (photo == null) {
         Toast.makeText(context, "选择已被取消", Toast.LENGTH_SHORT).show()
      } else {
         //do something…
      }
   }
}
```
使用系统相册
```kotlin
PhotoSelector.withSysAlbum(activity, AlbumType.PHOTO) { 
   select{ photo ->
      if (photo == null) {
         Toast.makeText(context, "选择已被取消", Toast.LENGTH_SHORT).show()
      } else {
         //do something…
      }
   }
}
```

6. 图片或视频预览，支持本地图片已经网络图片。
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
7. 播放视频，支持网络视频以及本地视频。
```kotlin
PhotoSelector.playVideo(activity, "url or filePath")
```
8. 使用系统自带的播放功能播放视频，支持网络视频以及本队视频。
```kotlin
PhotoSelector.playVideoWithSystem(activity, "url or filePath")
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