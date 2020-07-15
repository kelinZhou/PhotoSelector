package com.kelin.photoselector.model


/**
 * **描述:** Picture的包装器。
 *
 * **创建人:** kelin
 *
 * **创建时间:** 2020/7/9 4:23 PM
 *
 * **版本:** v 1.0.0
 */
internal data class PictureWrapper(
    /**
     * 文件所在的路径。
     */
    val picture: Picture,
    /**
     * 是否被选中了。
     */
    var isSelected: Boolean = false
) {
    /**
     * 流水号。
     */
    var no: Int? = null
}