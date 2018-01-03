package com.jdbernard.dkbphotosite

import ca.odell.glazedlists.BasicEventList
import ca.odell.glazedlists.swing.AutoCompleteSupport
import com.jdbernard.util.LightOptionParser
import com.jdblabs.file.treediff.TreeDiff
import com.jdblabs.file.treediff.DirAnalysis
import groovy.beans.Bindable
import groovy.io.FileType
import groovy.swing.SwingBuilder
import java.awt.CardLayout
import java.awt.Color
import java.awt.event.KeyEvent
import java.awt.GridBagConstraints as GBC
import java.awt.KeyboardFocusManager
import java.util.regex.Pattern
import javax.imageio.ImageIO
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JSplitPane
import javax.swing.JTextArea
import javax.swing.JTextField
import javax.swing.JTextPane
import javax.swing.JTree
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath
import net.miginfocom.swing.MigLayout
import org.apache.commons.codec.digest.DigestUtils
import org.yaml.snakeyaml.Yaml

public class PhotoSiteManager {

  public static final String VERSION = "1.1.0"

  private static final Yaml yaml = new Yaml()

  def config
  File imageDatabaseFile
  SiteBuilder siteBuilder

  // GUI Data (Model)
  @Bindable Category rootCategory
  Map<String, Image> imagesByMd5
  @Bindable Image currentImage
  @Bindable Category currentCategory
  @Bindable Integer currentImageIdx

  final static String LOADING_CARD = "Loading View";
  final static String IMAGE_CARD = "Image View";
  final static String CATEGORY_CARD = "Category View";

  // GUI Elements (View)
  SwingBuilder swing = new SwingBuilder()
  JFrame rootFrame
  JTree categoryTree
  DefaultTreeModel categoryTreeModel
  def rightPanel
  CardLayout rightLayout
  JButton saveButton, cancelButton, previewButton, publishButton

  // Loading card elements
  JTextPane loadingTextPane
  SimpleAttributeSet successMsgStyle
  SimpleAttributeSet errorMsgStyle
  SimpleAttributeSet warnMsgStyle

  // Image card elements.
  ScalableImagePanel scaledImage
  JTextField commonNameTF
  JTextField scientificNameTF
  JComboBox categoryCB
  AutoCompleteSupport categoryACS

  // Category card elements
  JTextArea categoryDescriptionTA

  enum MsgType { Success, Error, Normal, Warning }

  public static void main(String[] args) {
    def cli = [
      'h': [longName: 'help'],
      'v': [longName: 'version'],
      'c': [longName: 'config', arguments: 1],
      'i': [longName: 'image-database', arguments: 1],
      'F': [longName: 'use-file-meta-format']]

    def opts = LightOptionParser.parseOptions(cli, args)

    if (opts.v) {
      println "DKB Photo Site Manager v$VERSION"
      System.exit(0) }

    def exitErr = { String msg ->
      System.err.println "PhotoSiteManager: ${msg}"
      System.exit(1) }

    def configFile = opts.c ? new File(opts.c[0]) : new File('site-configuration.yaml')

    if (!configFile.exists() || !configFile.isFile())
      exitErr "${configFile.canonicalPath} does not exist or is not a file."

    def config
    try { config = configFile.withInputStream { is -> yaml.load(is) } }
    catch (all) { exitErr "Cannot load config file: ${all.localizedMessage}" }

    File imageDatabaseFile = opts.i ? new File(opts.i[0]) :
      new File('image-database.yaml')

    if (!opts.F && (!imageDatabaseFile.exists() || !imageDatabaseFile.isFile()))
      exitErr "${imageDatabaseFile.canonicalPath} does not exist or is not a file."

    new PhotoSiteManager(config, imageDatabaseFile, opts.F as boolean)
  }

  public PhotoSiteManager(def config, File imageDatabaseFile,
    boolean seedFromFilesystem) {

    this.config = config
    this.imageDatabaseFile = imageDatabaseFile

    initGui()
    show()

    File templateFile = new File(config.pageTemplate)
    if (!templateFile.exists()) {
      showMessage("Unable to find the page template file (configured as " +
        "'${config.pageTemplate}').\nPreview and publish functionality will " +
        "not work without a valid page template.", MsgType.Error) }

    this.siteBuilder = new SiteBuilder(new FileReader(templateFile), config)

    config.imageFilenamePattern = Pattern.compile(/(?i)^.+\.(/ +
      config.imageFileExtensions.join('|') + /)$/)

    if (seedFromFilesystem) {
      File albumsDir = new File(config.albumsDirectory)
      if (!config.albumsDirectory || !albumsDir.exists() ||
          !albumsDir.isDirectory()) {
          showMessage("Cannot find the albums directory. Configured value " +
            "was \"${config.albumsDirectory}\" but this is not a valid " +
            "directory.", MsgType.Error) }

      else  {
        imageDatabaseFile.createNewFile()

        showMessage("Analyzing album directory (${config.albumsDirectory}) for image information...")
        rootCategory = Category.newWithMetaInDirname(config, albumsDir, null) }
    }

    else {
      showMessage("Loading image and category information...")
      def storedRoot = imageDatabaseFile.withInputStream { yaml.load(it) }
      rootCategory = Category.fromStorageForm(config, storedRoot, null)

      showMessage("Scanning images under ${config.albumsDirectory} for new " +
        "or changed files...")
      imagesByMd5 = scanImages()
    }

    updateTreeRoot()
    showMessage(">> Ready.\n", MsgType.Success)
  }

  private Map<String, Image> scanImages() {
    Map<String, Image> images = new HashMap<>()

    new File(config.albumsDirectory).eachFileRecurse(FileType.FILES) { f ->
      if (!(f.name ==~ config.imageFilenamePattern)) return

      // Get the hash of the file
      String md5Hex = f.withInputStream { DigestUtils.md5Hex(it) }

      // Try to find an image entry for that hash
      def image = rootCategory.findImage(md5Hex)

      if (!image) {
        def uncategorized = rootCategory.findCategory("Uncategorized")
        if (!uncategorized) {
          uncategorized = new Category(
            parent: rootCategory,
            name: "Uncategorized",
            description: "Photos that have not yet been categorized.",
            subcategories: [],
            images: [])
          rootCategory.subcategories << uncategorized
        }

        image = new Image(
          parent: uncategorized,
          commonName: f.name,
          scientificName: "")

        uncategorized.images << image
      }

      image.file = f
      image.filename = f.name
      images[md5Hex] = image
    }

    return images
  }

  // GUI Definition (View)
  private def initGui() {

    successMsgStyle = new SimpleAttributeSet()
    warnMsgStyle = new SimpleAttributeSet()
    errorMsgStyle = new SimpleAttributeSet()

    StyleConstants.setForeground(successMsgStyle, Color.BLUE);
    StyleConstants.setForeground(warnMsgStyle, Color.ORANGE);
    StyleConstants.setForeground(errorMsgStyle, Color.RED);
    StyleConstants.setBold(successMsgStyle, true);
    StyleConstants.setBold(warnMsgStyle, true);
    StyleConstants.setBold(errorMsgStyle, true);

    // Build main frame
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
            this.categoryTreeModel = new DefaultTreeModel(
                new DefaultMutableTreeNode("Loading..."))
            this.categoryTree = tree(
              cellRenderer: tcr,
              dragEnabled: true,
              enabled: false,
              model: this.categoryTreeModel,
              valueChanged: { evt ->
                def selectedObject = evt?.newLeadSelectionPath?.
                  lastPathComponent?.userObject

                if (!selectedObject) return;
                else if (selectedObject instanceof Image)
                  this.selectImage(selectedObject)
                else if (selectedObject instanceof Category)
                  this.selectCategory(selectedObject)
              }
            )
          }

          vbox(constraints: "right") {
            this.rightPanel = panel() {
              this.rightLayout = cardLayout()

              vbox(constraints: LOADING_CARD) {
                scrollPane() {
                  this.loadingTextPane = textPane(editable: false)
                }
              }

              panel(constraints: IMAGE_CARD,
                layout: new MigLayout("wrap 2, ins 10",
                  "[grow 66, right] 10 [grow , fill]",
                  "")) {

                this.scaledImage = widget(new ScalableImagePanel(),
                  constraints: "grow, span 2")

                label("Common Name: ")
                this.commonNameTF = textField(
                  actionPerformed: this.&advanceFocus,
                  keyReleased: { e ->
                    if (e.keyCode == KeyEvent.VK_ESCAPE)
                      this.selectImage(this.currentImage) })

                label("Scientific Name: ")
                this.scientificNameTF = textField(
                  actionPerformed: this.&advanceFocus,
                  keyReleased: { e ->
                    if (e.keyCode == KeyEvent.VK_ESCAPE)
                      this.selectImage(this.currentImage) })

                label("Category: ")
                this.categoryCB = comboBox()
                this.categoryCB.editor.actionPerformed =
                  this.&saveChanges

              }

              panel(constraints: CATEGORY_CARD,
                layout: new MigLayout("ins 10, wrap 1", "[fill, grow]",
                  "[] 10 [grow, fill]")) {
                label("Category Description: ")

                scrollPane() {
                  this.categoryDescriptionTA = textArea(lineWrap: true,
                    wrapStyleWord: true)
                }
              }
            }

            hbox() {
              this.previewButton = button(label: "Preview", enabled: false)
              hstrut(10)
              this.publishButton = button(label: "Publish", enabled: false)
              hglue()
              this.cancelButton = button(label: "Cancel", enabled: false,
                actionPerformed: this.&cancelChanges)
              hstrut(10)
              this.saveButton = button(label: "Save Changes", enabled: false,
                actionPerformed: this.&saveChanges)
            }
          }
        }
      }
    }
  }

  private void updateTreeRoot() {
    swing.edt {
      this.previewButton.enabled = true
      this.publishButton.enabled = true
      this.categoryTreeModel = new DefaultTreeModel(makeNodeForCategory(this.rootCategory))
      this.categoryTree.model = this.categoryTreeModel
      this.categoryACS = AutoCompleteSupport.install(
        this.categoryCB, this.createCategoryList(rootCategory))
      this.categoryTree.enabled = true
    }
  }

  // GUI Actions (Controller)

  /// Given a image category, create a TreeNode that represents it in the UI.
  private DefaultMutableTreeNode makeNodeForCategory(Category cat) {
    DefaultMutableTreeNode node = new DefaultMutableTreeNode(cat)
    cat.treeNode = node
    cat.subcategories.each { subcat -> node.add(makeNodeForCategory(subcat)) }
    cat.images.each { image ->
      image.treeNode = new DefaultMutableTreeNode(image)
      node.add(image.treeNode) }
    return node
  }

  private BasicEventList<String> createCategoryList(Category category) {
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

  private void showMessage(String message, MsgType msgType = MsgType.Normal) {
    swing.edt {
      saveButton.enabled = false
      cancelButton.enabled = false
      rightLayout.show(rightPanel, LOADING_CARD)
      def doc = loadingTextPane.styledDocument
      def style = null
      switch (msgType) {
        case MsgType.Success: style = successMsgStyle; break
        case MsgType.Warning: style = warnMsgStyle; break
        case MsgType.Error: style = errorMsgStyle; break
        default: break
      }
      doc.insertString(doc.length, message + "\n", style)
    }
  }

  private void selectImage(Image image) {
    saveButton.enabled = true
    cancelButton.enabled = true
    rightLayout.show(rightPanel, IMAGE_CARD)
    currentImage = image
    currentCategory = image.parent
    currentImageIdx = currentCategory.images.indexOf(currentImage)

    final def imageData = (image.file && image.file.exists()) ?
      ImageIO.read(image.file) : null

    if (!image.file) {
      println """\
Missing image file:
  md5: ${image.md5Hex}
  commonName: ${image.commonName}
  scientificName: ${image.scientificName}
""" }

    swing.edt {
      scaledImage.image = imageData
      commonNameTF.text = image.commonName
      scientificNameTF.text = image.scientificName
      categoryCB.selectedItem = image.parent.name
    }
  }

  private void saveChanges(def evt) {
    if (currentImage) {
      currentImage.commonName = commonNameTF.text
      currentImage.scientificName = scientificNameTF.text
      changeImageCategory(currentImage, categoryCB.selectedItem)
      categoryTreeModel.nodeChanged(currentImage.treeNode)
      nextImage()
    }

    else currentCategory.description = categoryDescriptionTA.text

    imageDatabaseFile.withWriter { writer ->
      yaml.dump(rootCategory.toStorageForm(), writer) }
  }

  private void cancelChanges(def evt) {
    if (currentImage) selectImage(currentImage)
    else selectCategory(currentCategory)
  }

  private void showPreview() {

    TreeDiff treediff = new TreeDiff(in: System.in, out: System.out, quiet: true)

    File stagingDir = new File(config.stagingDirectory)
    showMessage("Generating preview: writing to '${stagingDir.canonicalPath}'")

    // TODO: working here (cleanup when done)
    // 1. Create staging directory if needed.
    if (!stagingDir.exists()) {
      showMessage("Staging directory did not exist, creating...")

      if (!stagingDir.mkdirs()) {
        showMessage("Unable to generate preview: failed to create directory " +
          "'${stagingDir.canonicalPath}'.")
        return } }

    // 2. Analyze staging directory
    DirAnalysis stagingAnalysis = treediff.analyzeDir(stagingDir, null)

    // 3. Organize metadata into treediff-compatible DirAnalysis 

    // 4. Move links that point to continued files but are in the wrong paths

    // 5. Create symlinks for files not present in the staging directory

    // 6. Delete symlinks for files that no longer exist

    // 7. Generate pages into staging directory.

    // 8. Start Jetty (if not already running), point at staging directory.

    // 9. Open a browser pointing to the Jetty instance

  }

  private void advanceFocus(def e) {
    KeyboardFocusManager.currentKeyboardFocusManager.focusNextComponent()
  }

  private void selectCategory(Category category) {
    currentImage = null
    currentCategory = category
    saveButton.enabled = true
    cancelButton.enabled = true

    swing.edt {
      rightLayout.show(rightPanel, CATEGORY_CARD)
      categoryDescriptionTA.text = currentCategory.description
    }
  }

  private void nextImage() {
    int nextIdx = currentImageIdx
    if (currentCategory.images.contains(currentImage)) nextIdx += 1

    nextIdx = Math.min(currentCategory.images.size() - 1, nextIdx)
    Image image = currentCategory.images[nextIdx]

    swing.edt {
      categoryTree.selectionPath = new TreePath(image.treeNode.path)
      commonNameTF.requestFocus()
    }
  }

  private void changeImageCategory(Image image, String newCategoryName) {
    def newCategory = rootCategory.findCategory(newCategoryName)
    if (!newCategory || newCategory == image.parent) return

    image.parent.images.remove(image)
    newCategory.images.add(image)

    categoryTreeModel.removeNodeFromParent(image.treeNode)
    newCategory.treeNode.add(image.treeNode)
    image.parent = newCategory
  }

  private void publish() {
    // TODO: working here 2

    // 1. Analyze the target directory.
    showMessage("Analyzing existing site...")

    // 2. Organize local metadata into treediff-compatible DirAnalysis

    // 3. Find differences.

    // 4. Move files present under different paths

    // 5. Upload files present locally not present on remote

    // 6. Delete files present on remote but not locally


  }

  private void show() { rootFrame.show() }
}
