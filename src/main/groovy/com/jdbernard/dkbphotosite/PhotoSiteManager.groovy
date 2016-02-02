package com.jdbernard.dkbphotosite

import ca.odell.glazedlists.BasicEventList
import ca.odell.glazedlists.swing.AutoCompleteSupport
import com.jdbernard.util.LightOptionParser
import groovy.beans.Bindable
import groovy.swing.SwingBuilder
import java.awt.GridBagConstraints as GBC
import java.util.regex.Pattern
import javax.imageio.ImageIO
import javax.swing.JComboBox
import javax.swing.JFrame
import javax.swing.JSplitPane
import javax.swing.JTextField
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel
import net.miginfocom.swing.MigLayout
import org.apache.commons.codec.digest.DigestUtils
import org.yaml.snakeyaml.Yaml

public class PhotoSiteManager {

  public static final String VERSION = "1.0"

  private static final Yaml yaml = new Yaml()

  def config

  // GUI Data (Model)
  @Bindable Category rootCategory
  @Bindable Image currentImage
  @Bindable Category currentCategory

  final static String IMAGE_CARD = "Image View";
  final static String CATEGORY_CARD = "Category View";

  // GUI Elements (View)
  SwingBuilder swing = new SwingBuilder()
  JFrame rootFrame
  JTree categoryTree

  // Image card elements.
  ScalableImagePanel scaledImage
  JTextField commonNameTF
  JTextField scientificNameTF
  JComboBox categoryCB
  AutoCompleteSupport categoryACS

  public static void main(String[] args) {
    def cli = [
      'h': [longName: 'help'],
      'v': [longName: 'version'],
      'c': [longName: 'config', arguments: 1]]

    def opts = LightOptionParser.parseOptions(cli, args)

    def exitErr = { String msg ->
      System.err.println "PhotoSiteManager: ${msg}"
      System.exit(1) }

    def configFile = opts.c ? new File(opts.c[0]) : new File('site-configuration.yaml')

    if (!configFile.exists() || !configFile.isFile())
      exitErr "${configFile.canonicalPath} does not exist or is not a file."

    def config
    try { config = configFile.withInputStream { is -> yaml.load(is) } }
    catch (all) { exitErr "Cannot load config file: ${all.localizedMessage}" }

    def psm = new PhotoSiteManager(config)
    psm.show()

  }

  public PhotoSiteManager(def config) {
    this.config = config

    config.imageFilenamePattern = Pattern.compile(/(?i)^.+\.(/ +
      config.imageFileExtensions.join('|') + /)$/)

    // TODO: change to use stored metadata.
    rootCategory = Category.newWithMetaInDirname(config, new File(config.albumsDirectory), null)
    initGui(rootCategory)
  }

  // GUI Definition (View)
  private def initGui(Category rootCateogory) {
    swing.edtBuilder {
      this.rootFrame = frame(title: "DKB Photo Site Manager v${VERSION}",
            /*iconImage: iconImage('/photo-site-manager-32x32.png').image,
            iconImages: [iconImage('/photo-site-manager-16x16.png').image,
                         iconImage('/photo-site-manager-32x32.png').image,
                         iconImage('/photo-site-manager-64x64.png').image],*/
            preferredSize: [1024, 768], pack: true,
            layout: new MigLayout("ins 0, fill"),
            defaultCloseOperation: JFrame.EXIT_ON_CLOSE) {

        splitPane(
          orientation: JSplitPane.HORIZONTAL_SPLIT,
          dividerLocation: 400,
          oneTouchExpandable: true,
          constraints: "grow") {

          scrollPane(constraints: "left") {
            tcr = new DefaultTreeCellRenderer()
            this.categoryTree = tree(
              cellRenderer: tcr,
              dragEnabled: true,
              model: new DefaultTreeModel(categoryToNode(rootCategory)),
              valueChanged: { evt ->
                def selectedObject = evt?.newLeadSelectionPath?.
                  lastPathComponent?.userObject

                if (!selectedObject) return;
                else if (selectedObject instanceof Image)
                  this.selectImage(selectedObject)
                else if (selectedObject instanceof Category)
                  this.selectCategory(selectedObject)
              },
              mouseReleased: { evt ->
                if (this.currentImage) this.commonNameTF.requestFocus()
              }
            )
          }

          editingPanel = panel(constraints: "right") {
            cardLayout()

            panel(constraints: IMAGE_CARD,
              layout: new MigLayout("wrap 2, ins 10",
                "[grow 66, right] 10 [grow , fill]",
                "")) {

              this.scaledImage = widget(new ScalableImagePanel(),
                constraints: "grow, span 2")

              label("Common Name: ")
              this.commonNameTF = textField()

              label("Scientific Name: ")
              this.scientificNameTF = textField()

              label("Category: ")
              this.categoryCB = comboBox()
              this.categoryACS = AutoCompleteSupport.install(
                this.categoryCB, this.getCategoryList(this.rootCategory))

              hbox(constraints: "dock south") {
                hglue()
                button(label: "Cancel")
                hstrut(10)
                button(label: "Save Changes")
              }
            }
          }
        }
      }
    }
  }

  // GUI Actions (Controller)
  private DefaultMutableTreeNode categoryToNode(Category cat) {
    DefaultMutableTreeNode node = new DefaultMutableTreeNode(cat)
    cat.subcategories.each { subcat -> node.add(categoryToNode(subcat)) }
    cat.images.each { image -> node.add(new DefaultMutableTreeNode(image)) }
    return node
  }

  private BasicEventList<String> getCategoryList(Category category) {
    BasicEventList<String> list = new BasicEventList<>();

    def catQueue = new LinkedList<Category>()
    catQueue.add(category)

    while (!catQueue.isEmpty()) {
      def cat = catQueue.remove()
      list.add(cat.name)
      catQueue.addAll(cat.subcategories)
    }

    return list;
  }

  private void selectImage(Image image) {
    currentImage = image
    currentCategory = image.parent

    final def imageData = ImageIO.read(image.file)
    swing.doLater {
      scaledImage.image = imageData
      commonNameTF.text = image.commonName
      scientificNameTF.text = image.scientificName
      categoryCB.selectedItem = image.parent.name
    }
  }

  private void selectCategory(Category category) {
    currentImage = null
    currentCategory = category
  }

  private void show() { rootFrame.show() }
}
