import std/[logging, nre, options, os, parsecfg, sha1, strutils, tables]
import zero_functional
import ./models

from std/sequtils import toSeq

func parseName(name: string): string =
  let namePat = re"^(\d+[a-z]? )?(.*)$"
  return name.match(namePat).get.captures[1]

proc parseImage(imagePath: string): ImageNode =
  debug "Processing image " & imagePath
  let pathParts = imagePath.splitFile
  let hash = $secureHashFile(imagePath)
  result = ImageNode(
    filename: hash & pathParts.ext,
    hash: hash,
    originalFilepath: imagePath,
    name: pathParts.name.parseName)

proc parseDirectory(
    folderPath: string,
    parent: Option[CategoryNode] = none[CategoryNode]()
  ): tuple[category: CategoryNode, images: seq[ImageNode]] =

  let name = folderPath.splitFile.name
  let slug = name.parseName.slugify
  debug "Processing directory " & folderPath

  result = (
    category: CategoryNode(
      childCategories: @[],
      coverImageFilename: none[string](),
      description: none[string](),
      images: @[],
      hash: "",
      name: name.parseName,
      relPath:
        if parent.isSome: parent.get.relPath / slug
        else: "",
      slug: slug),
    images: @[])

  for k in walkDir(folderPath):
    if k.kind == pcDir and not k.path.lastPathPart.startsWith("."):
      let child = parseDirectory(k.path, some(result.category))
      result.category.childCategories.add(child.category)
      result.images &= child.images

    elif k.kind == pcFile and k.path.isImage:
      let imageNode = parseImage(k.path)
      result.images.add(imageNode)
      result.category.images.add(imageNode)

  let folderCfgPath = folderPath / "folder-config.ini"
  if fileExists(folderCfgPath):
    let folderCfg = loadConfig(folderCfgPath)

    if not folderCfg.getSectionValue("", "description").isEmptyOrWhitespace:
      result.category.description = some(folderCfg.getSectionValue("", "description"))

    if not folderCfg.getSectionValue("", "cover photo").isEmptyOrWhitespace:
      let cfgValue = folderPath / folderCfg.getSectionValue("", "cover photo")
      let fileName = walkFiles(cfgValue & "*").toSeq

      if fileName.len > 0:
        let coverImage = parseImage(fileName[0])
        result.images.add(coverImage)
        result.category.coverImageFilename = some(coverImage.filename)
      else: warn "The folder config at '" & folderCfgPath & "' lists '" &
        cfgValue & "' as a cover photo, but I can't find any file " &
        "corresponing to the pattern '" & cfgValue & "*'"

proc analyzeRoot*(path: string): SiteMetadata =
  if not dirExists(path):
    raise newException(IOError,
      "cannot find the source directory (" & path & ")")

  let (rootCat, images) = parseDirectory(path)
  let imageTable: TableRef[string, ImageNode] = newTable(images --> map((it.hash, it)))

  result = SiteMetadata(
    images: imageTable,
    rootCategory: rootCat)

  result.rootCategory.name = "David K. Bernard Photography"
