package com.jdbernard.dkbphotosite

import org.apache.commons.codec.digest.DigestUtils
import groovy.beans.Bindable

@Bindable
public class Image {

    public Category parent
    public File file
    public String filename
    public String commonName
    public String scientificName
    public String originalFileName
    public String order
    public String md5Hex

    public static Image newWithMetaInFilename(File imageFile, Category parent) {
      def nameParts = (imageFile.name.replaceAll(/\.\w+$/, '').split('!'))
        .reverse() as LinkedList

      return new Image(
        parent: parent,
        file: imageFile,
        filename: imageFile.name,
        commonName: nameParts.poll() ?: "",
        scientificName: nameParts.poll() ?: "",
        originalFileName: nameParts.poll() ?: "",
        order: nameParts.poll() ?: "",
        md5Hex: imageFile.withInputStream { DigestUtils.md5Hex(it) })
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
