import std/[json, jsonutils, nre, options, os, sets, strutils, tables, unicode]

type
  SiteMetadata* = ref object
    images*: TableRef[string, ImageNode]
    categoryHashes*: HashSet[string]
    rootCategory*: CategoryNode
    scriptHash*, styleHash*: string

  CategoryNode* = ref object
    childCategories*: seq[CategoryNode]
    coverImageFilename*: Option[string]
    images*: seq[ImageNode]
    description*: Option[string]
    hash*, name*, relPath*, slug*: string

  ImageNode* = ref object
    hash*, filename*, name*, originalFilepath*: string

func slugify*(name: string): string =
  result = name.
    replace(re"[\[\]!@#$%^&*(){}\/\\|;:'""]+", "_").
    replace(' ', '_').
    toLower

const IMG_EXTS = [ "jpg", "png", "bmp", "svg", "jpeg", "gif" ]

func isImage*(path: string): bool =
  result = IMG_EXTS.contains(path.splitFile.ext[1..^1].toLower)

proc parseSiteMetadataFromJson*(path: string): SiteMetadata =
  fromJson[SiteMetadata](result, parseFile(path))

proc `$`*(meta: SiteMetadata): string = $toJson(meta)

func indexPathsAndHashes(c: CategoryNode): seq[tuple[path, hash: string]] =
  result = @[(path: c.relPath, hash: c.hash)]
  for child in c.childCategories: result &= child.indexPathsAndHashes

func indexPathsAndHashes*(m: SiteMetadata): seq[tuple[path, hash: string]] =
  return m.rootCategory.indexPathsAndHashes
