package com.jdbernard.dkbphotosite

import javax.swing.tree.TreeNode
import org.apache.commons.codec.digest.DigestUtils
import groovy.beans.Bindable

@Bindable
public class Image {

  public Category parent
  public File file
  public String filename
  public String commonName
  public String scientificName
  public String md5Hex
  public TreeNode treeNode

  @Deprecated public String originalFileName
  @Deprecated public String order

  @Deprecated
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

  public static Image fromStorageForm(def storedForm, Category parent) {
    return new Image(
      parent: parent,
      commonName: storedForm.commonName,
      scientificName: storedForm.scientificName,
      md5Hex: storedForm.md5Hex)
  }

  public def toStorageForm() {
    return [
      commonName: this.commonName,
      scientificName: this.scientificName,
      md5Hex: this.md5Hex ]
  }

  public String toString() {
    StringBuilder sb = new StringBuilder()

    sb.append(scientificName ?: "")
    if (scientificName && commonName) sb.append(" - ")
    sb.append(commonName ?: "")
  }
}
