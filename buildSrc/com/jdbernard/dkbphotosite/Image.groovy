package com.jdbernard.dkbphotosite

import org.apache.commons.codec.digest.DigestUtils

public class Image {

    public Category parent
    public File file
    public String filename
    public String commonName
    public String scientificName
    public String originalFileName
    public String order
    public String md5Hex

    public Image(File imageFile, Category parent) {
        def nameParts = (imageFile.name.replaceAll(/\.\w+$/, '').split('!'))
            .reverse() as LinkedList

        this.parent = parent
        this.file = imageFile
        this.filename = imageFile.name
        this.commonName = nameParts.poll() ?: ""
        this.scientificName = nameParts.poll() ?: ""
        this.originalFileName = nameParts.poll() ?: ""
        this.order = nameParts.poll() ?: ""
        imageFile.withInputStream {is -> this.md5Hex = DigestUtils.md5Hex(is) }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder()

        sb.append(order ?: "")
        if (order && (scientificName || commonName)) sb.append(": ")
        sb.append(scientificName ?: "")
        if ((order || scientificName) && commonName) sb.append(" - ")
        sb.append(commonName ?: "")
    }
}
