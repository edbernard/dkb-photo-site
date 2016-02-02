package com.jdbernard.dkbphotosite

import groovy.beans.Bindable

@Bindable
public class Category {

    public def siteConfig
    public Category root
    public Category parent = null
    public String name
    public String description
    public String relativePathToRoot

    public List<Category> subcategories = []
    public List<Image> images = []

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
        .collect { Image.newWithMetaInFilename(it, cat) }

      cat.subcategories = dir.listFiles( { f -> f.isDirectory() } as FileFilter)
          .collect { Category.newWithMetaInDirname(siteConfig, it, cat) }

      return cat
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

}
