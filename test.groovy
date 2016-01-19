import com.jdbernard.dkbphotosite.*
import org.yaml.snakeyaml.Yaml
import java.util.regex.Pattern

yaml = new Yaml()

configFile = new File('../site-configuration.yaml')
configFile.withInputStream { config = yaml.load(it) } ?
    "Config loaded." : "Failed to load the configuration."

config.imageFilenamePattern = Pattern.compile(/(?i)^.+\.(/ + config.imageFileExtensions.join('|') + /)$/)

rootDir = new File(config.albumsDirectory)

rootCat = new Category(config, rootDir, null)

siteBuilder = new SiteBuilder(new FileReader('../template/html/index.html'), config)
