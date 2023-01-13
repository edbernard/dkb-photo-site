import std/[logging, options, os, sets, sha1]
import zero_functional
import ./cliconstants, ./models

const SCRIPT_CONTENTS = readFile("src/main/webroot/" & SCRIPT_FILENAME)
const STYLE_CONTENTS = readFile("src/main/webroot/" & STYLE_FILENAME)

proc generateCategoryPageHtml(cat: CategoryNode, ancestors: seq[CategoryNode]): string =
  result = """
<!DOCTYPE html>
<html>
  <head>
    <meta charset="utf-8">
    <meta name="viewport"
          content="width=device-width, initial-scale=1, minimum-scale=1">
    <link rel="stylesheet" href='/""" & STYLE_FILENAME & """'>
    <script type="application/javascript" src='/""" & SCRIPT_FILENAME & """'></script>
    <title>""" & cat.name & """</title>
  </head>
  <body>
    <div class="loading-splash">
      <h1>
        <span>David K. Bernard</span>
        <span>|</span>
        <span>""" & cat.name & """</span>
      </h1>
      <div class="loading-bar"><hr /></div>
    </div>
    <header>
      <div>
        <h1>
          <a href="."><span>""" & cat.name & """</span></a>
        </h1>
        <h2 class="breadcrumbs">"""

  for a in ancestors:
    result &= """
          <a href='/""" & a.relPath & "'>" & a.name & "</a>"

  result &= """
          <a href='/""" & cat.relPath & "'>" & cat.name & """</a>
        </h2>
      </div>"""

  if cat.description.isSome: result &= """
      <div class="description">""" & cat.description.get & """</div>"""

  result &= """
    </header>

    <!-- Catagories -->
    <div class="categories">"""

  for child in cat.childCategories:
    result &= """
      <a href='/""" & child.relPath & """'>
        <div class="loading-photo">
          <div class="loading-photo-1"></div>
          <div class="loading-photo-2"></div>
          <div class="loading-photo-3"></div>
        </div>"""

    if child.coverImageFilename.isSome:
      result &= "\p        <img src='/img/" & child.coverImageFilename.get & "'>"
    elif child.images.len > 0:
      result &= "\p        <img src='/img/" & child.images[0].filename & "'>"

    result &= """
        <h2 class="category-title">""" &  child.name & """</h2>
      </a>

"""

  result &= """
    </div>

    <!-- Photos -->
    <div class="photos">
"""

  for i in cat.images:
    result &= """
      <a href='/img/""" & i.filename & """'>
        <div class="loading-photo">
          <div class="loading-photo-1"></div>
          <div class="loading-photo-2"></div>
          <div class="loading-photo-3"></div>
        </div>
        <img src='/img/""" & i.filename & """'>
        <h2 class="photo-title">""" & i.name & """</h2>
      </a>"""

  result &= """
    </div>
    <footer>Created by Elijah and Jonathan Bernard</footer>
    <script type="application/javascript">
      /*
      window.onload = function() {
        document.body.classList.remove('loading');
      }
      */
    </script>
  </body>
</html>
"""

proc generateCategory*(
    cat: CategoryNode,
    rootPath: string,
    ancestors: seq[CategoryNode] = @[]): seq[string] =

  let catPath = rootPath / cat.relPath
  if not dirExists(catPath): createDir(catPath)

  debug "Generating and writing html for " & cat.name & " at " & catPath
  let pageHtml = generateCategoryPageHtml(cat, ancestors)
  let pagePath = catPath / "index.html"
  pagePath.writeFile(pageHtml)

  cat.hash = $secureHashFile(pagePath)
  result = @[cat.hash]

  for child in cat.childCategories:
    result &= generateCategory(child, rootPath, ancestors & @[cat])

proc generateSite*(meta: SiteMetadata, tgtDir: string) =
  info "Generating site to temporary folder: " & tgtDir
  meta.categoryHashes = toHashSet(generateCategory(meta.rootCategory, tgtDir))

  info "Writing out static assets to temporary folder: " & tgtDir
  writeFile(tgtDir / SCRIPT_FILENAME, SCRIPT_CONTENTS)
  writeFile(tgtDir / STYLE_FILENAME, STYLE_CONTENTS)

  meta.scriptHash = $secureHashFile(tgtDir / SCRIPT_FILENAME)
  meta.styleHash = $secureHashFile(tgtDir / STYLE_FILENAME)
