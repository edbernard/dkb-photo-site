package com.jdbernard.dkbphotosite

import javax.swing.tree.TreeNode
import groovy.beans.Bindable

/**
 * Data class for image categories.
 */
@Bindable
public class Category {

  public def siteConfig
  public Category root
  public Category parent = null
  public String name
  public String description
  public String relativePathToRoot
  public TreeNode treeNode

  public List<Category> subcategories = []
  public List<Image> images = []

  public static Category fromStorageForm(def siteConfig, def storedForm,
    Category parent) {

    Category cat = new Category(
      siteConfig: siteConfig,
      parent: parent,
      name: storedForm.name,
      description: storedForm.description,
      relativePathToRoot: (parent?.relativePathToRoot ?: "") + "../")

    cat.images = storedForm.images.collect { Image.fromStorageForm(it, cat) }

    cat.subcategories = storedForm.subcategories.collect {
      Category.fromStorageForm(siteConfig, it, cat) }

    return cat
  }

  public def toStorageForm() {
    return [
      name: this.name,
      description: this.description,
      images: this.images.collect { it.toStorageForm() },
      subcategories: this.subcategories.collect { it.toStorageForm() }]
  }

  public Category findCategory(String categoryName) {
    if (this.name == categoryName) return this
    if (this.subcategories.size() == 0) return null
    return this.subcategories.collect { it.findCategory(categoryName) }.find()
  }

  public List<Image> findImages(String md5Hex) {
    return this.images.findAll { it.md5Hex == md5Hex } ?:
      this.subcategories.collect { it.findImages(md5Hex) }.flatten().findAll()
  }

  public boolean isDescendantOf(Category cat) {
    for (def cur = this; cur != null; cur = cur.parent) {
      if (cur == cat) return true; }
    return false;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder()
    sb.append(this.name)

    if (subcategories || images) sb.append(": ")
    if (subcategories) sb.append(subcategories.size()).append(" subcategories")
    if (subcategories && images) sb.append(" & ")
    if (images) sb.append(images.size()).append(" images")

    return sb.toString()
  }

  @Deprecated
  public static Category newWithMetaInDirname(def siteConfig, File dir,
    Category parent) {

    File descFile = new File(dir, "description.txt")

    Category cat = new Category(
      siteConfig: siteConfig,
      parent: parent,
      name: dir.name,
      description: descFile.exists() ? descFile.text : "",
      relativePathToRoot: (parent?.relativePathToRoot ?: "") + "../")

    cat.root = parent ? parent.root : cat

    cat.images = dir.listFiles(
      { f -> !f.isDirectory() && f.name ==~ siteConfig.imageFilenamePattern }
        as FileFilter)
      .collect { Image.newWithMetaInFilename(it, cat) }.sort { it.order }

    cat.subcategories = dir.listFiles( { f -> f.isDirectory() } as FileFilter)
        .collect { Category.newWithMetaInDirname(siteConfig, it, cat) }

    return cat
  }


}
