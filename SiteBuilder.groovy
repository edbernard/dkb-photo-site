import org.yaml.snakeyaml.Yaml
import java.util.regex.Pattern
import org.apache.commons.codec.digest.DigestUtils

public class SiteBuilder {

    public static def loadConfig(File configFile) {
        Yaml yaml = new Yaml()
        def config = configFile.withInputStream { is -> yaml.load(is) }

        config.imageFilenamePattern = Pattern.compile(/(?i)^.+\.(/ +
            config.imageFileExtensions.join('|') + /)$/)

        return config;
    }

    public static def buildTree(def siteConfig, File rootDir) {

        if (!rootDir.isDirectory()) {
            throw new IOException("The root directory (configured as '" +
                rootDir.canonicalPath + "') is not a directory.");
        }

        return buildCategory(siteConfig, rootDir, null);
    }

    public static def buildCategory(def siteConfig, File dir, def parent) {

        File descFile = new File(dir, "description.txt")
        def node = [
            siteConfig: siteConfig,
            parent: parent,
            title: dir.name, 
            description: descFile.exists() ? descFile.text : "",
            relativePathToRoot: (parent?.relativePathToRoot ?: "") + "../",
        ]

        node.root = parent ? parent.root : node

        node.images = dir.listFiles(
            { f -> !f.isDirectory() && f.name ==~ siteConfig.imageFilenamePattern }
                as FileFilter)
            .collect { buildImage(it, node) }

        node.subcategories = dir.listFiles( { f -> f.isDirectory() } as FileFilter)
            .collect { buildCategory(siteConfig, it, node) }

        return node
    }

    public static def buildImage(File imageFile, def parent) {
        def nameParts = (imageFile.name.replaceAll(/\.\w+$/, '').split('!'))
            .reverse() as LinkedList

        return imageFile.withInputStream { is -> [
            parent: parent,
            file: imageFile,
            filename: imageFile.name,
            commonName: nameParts.poll() ?: "",
            scientificName: nameParts.poll() ?: "",
            originalFileName: nameParts.poll() ?: "",
            order: nameParts.poll() ?: "",
            md5Hex: DigestUtils.md5Hex(is)
        ]}
    }
}
