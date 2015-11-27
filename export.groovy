import groovy.sql.Sql
import groovy.util.XmlSlurper
import java.nio.file.Files

exportPropFile = new File('export.properties')

config = new Properties()

if (!exportPropFile.exists()) 
    println "Unable to find 'export.properties' file. Expect errors."
else exportPropFile.withInputStream { is -> config.load(is); }

xmlSlurper = new XmlSlurper()

connect = { -> return Sql.newInstance(
    config['db.connString'], config['db.username'],
    config['db.password'], config['db.driverName']) }

getGalleryFolders = { row ->
    def oldPath = new File(config['old.root'], row.path)
    def newPath = new File(config['new.root'], row.title)

    if (!newPath.exists()) {
        newPath.mkdirs()
        new File(newPath, 'description.txt').text = htmlDecode(row.galdesc)
    }

    return [old: oldPath, new: newPath];
}

htmlDecode = { text ->
    return xmlSlurper.parseText("<span>${text.replaceAll(/\\&/, '&')}</span>")
}

processGallery = { sql, galRow ->
    println "Processing gallery: ${htmlDecode(galRow.title)}"

    folders = getGalleryFolders(galRow)

    sql.eachRow("select * from wp_ngg_pictures where galleryid=${galRow.gid}") { p ->
        try {
            File oldFile = new File(folders.old, p.filename)
            File newFile = new File(folders.new, 
                "${p.sortorder.toString().padLeft(3,"0")}!${p.filename}!${htmlDecode(p.description)}!${htmlDecode(p.alttext)}.jpg")

            println "${oldFile.name} -> ${newFile.name}"
            Files.copy(oldFile.toPath(), newFile.toPath())
        } catch (Exception e) {
            println "Error: ${e.getClass()}: ${e.localizedMessage}"
        }
    }
}
