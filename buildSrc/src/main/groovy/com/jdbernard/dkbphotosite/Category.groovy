package com.jdbernard.dkbphotosite

public class Category {

    public def siteConfig
    public Category root
    public Category parent = null
    public String name
    public String description
    public String relativePathToRoot

    public List<Category> subcategories = []
    public List<Image> images = []

    public Category(def siteConfig, File dir, Category parent) {
        File descFile = new File(dir, "description.txt")

        this.siteConfig = siteConfig
        this.parent = parent
        this.name = dir.name
        this.description = descFile.exists() ? descFile.text : ""
        this.relativePathToRoot = (parent?.relativePathToRoot ?: "") + "../"
        this.root = parent ? parent.root : this

        this.images = dir.listFiles(
            { f -> !f.isDirectory() && f.name ==~ siteConfig.imageFilenamePattern }
                as FileFilter)
            .collect { new Image(it, this) }

        this.subcategories = dir.listFiles( { f -> f.isDirectory() } as FileFilter)
            .collect { new Category(siteConfig, it, this) }
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
