import std/[logging, os, sets, strutils, tables]
import cliutils, docopt, tempdir, zero_functional
import private/[cliconstants, generate, models, parse]

from std/sequtils import toSeq

proc awsS3Cp(src, tgt: string, workingDir = "") =
  let s3CmdResult = execWithOutput(
    command = "aws",
    args = ["s3", "cp", src, tgt],
    workingDir = workingDir)

  if s3CmdResult.exitCode != QuitSuccess:
    let cmdFull = "aws s3 cp " & src & " " & tgt
    warn "[" & cmdFull & "] failed."
    debug "[" & cmdFull & "] stdout: " & s3CmdResult.output
    debug "[" & cmdFull & "] stderr: " & s3CmdResult.error

proc fetchRemoteMetadata(targetBucket: string, tgtDir: string): SiteMetadata =
  let s3Path = "s3://" & targetBucket / "site-metadata.json"
  let localPath = tgtDir / "site-metadata.json"

  awsS3Cp(src = s3Path, tgt = localPath, workingDir = tgtDir)

  if not fileExists(localPath):
    raise newException(IOError,
      "Expected the local copy of the remote site metadata to be at '" &
      localPath & "' but there is not file there.")

  return parseSiteMetadataFromJson(localPath)

proc pushLocalMetadata(localDir, targetBucket: string, meta: SiteMetadata) =
  let s3Path = "s3://" & targetBucket / "site-metadata.json"
  let localPath = localDir / "site-metadata.json"

  debug "Pushing metadata file to S3: aws s3 cp " & localPath & " " & s3Path
  awsS3Cp(src = localPath, tgt = s3Path)

proc pushImages(targetBucket: string, images: seq[ImageNode]) =
  for image in images:
    let s3Path = "s3://" & targetBucket & "/webroot/img/" & image.filename
    let localPath = image.originalFilepath

    debug "Pushing image to S3: aws s3 cp " & localPath & " " & s3Path
    awsS3Cp(src = localPath, tgt = s3Path)

proc pushPages(
    localMeta, remoteMeta: SiteMetadata,
    localDir, targetBucket: string) =

  info "Pushing new and updated pages to S3."
  let localPaths = localMeta.indexPathsAndHashes
  var updatedPaths: seq[string] = @[]

  for (relPath, hash) in localPaths:
    if remoteMeta.categoryHashes.contains(hash): continue

    let s3Path = "s3://" & targetBucket & "/webroot/" & relPath
    let localPath = localDir / relPath / "index.html"

    debug "Pushing page to S3: aws s3 cp " & localPath & " " & s3Path
    awsS3Cp(src = localPath, tgt = s3Path)
    updatedPaths.add(relPath)

  if localMeta.scriptHash != remoteMeta.scriptHash:
    let scriptPath = localDir / SCRIPT_FILENAME
    let s3Path = "s3://" & targetBucket & "/webroot/" & SCRIPT_FILENAME

    debug "Pushing page to S3: aws s3 cp " & scriptPath & " " & s3Path
    awsS3Cp(src = scriptPath, tgt = s3Path)
    updatedPaths.add("/" & SCRIPT_FILENAME)

  if localMeta.styleHash != remoteMeta.styleHash:
    let stylePath = localDir / STYLE_FILENAME
    let s3Path = "s3://" & targetBucket & "/webroot/" & STYLE_FILENAME
    debug "Pushing page to S3: aws s3 cp " & stylePath & " " & s3Path
    awsS3Cp(src = stylePath, tgt = s3Path)
    updatedPaths.add("/" & STYLE_FILENAME)

  info "Requesting cache invalidation from CloudFront"
  let args = @["cloudfront", "create-invalidation",
               "--distribution-id", CLOUDFRONT_DISTRIBUTION_ID,
               "--paths", "/index.html"] & (updatedPaths[1..^1] --> map("/" & it))

  debug "aws " & args.join(" ")
  let invalidateResult = execWithOutput(command = "aws", args = args)

  if invalidateResult.exitCode != QuitSuccess:
    warn "Cloudfront cache invalidation failed."
    debug "Cloudfront cache invalidation stdout: " & invalidateResult.output
    debug "Cloudfront cache invalidation stderr: " & invalidateResult.error

proc writeImages(srcDir, tgtDir: string, images: seq[ImageNode]) =
  for image in images:
    let tgtPath = tgtDir / "img" / image.filename
    debug "Copying image: " & image.originalFilepath & " --> " & tgtPath
    copyFile(image.originalFilepath, tgtPath)

when isMainModule:
 let workingDir = createTempDirectory("dkbphoto")
 var retainLocalDir = false

 try:
  let consoleLogger = newConsoleLogger(
    levelThreshold=lvlInfo,
    fmtStr="dkb_photo_site - $levelname: ",
    useStderr = true)
  logging.addHandler(consoleLogger)

  # Parse arguments
  let args = docopt(USAGE, version = DPS_VERSION)

  if args["--debug"]:
    retainLocalDir = true
    consoleLogger.levelThreshold = lvlDebug

  if args["--echo-args"]: stderr.writeLine($args)

  createDir(workingDir / "local/webroot")
  let srcDir = $args["<srcDir>"]
  let tgtDir = workingDir / "local/webroot"

  info "Analyzing files at " & srcDir
  var localMetadata = analyzeRoot(srcDir)

  generateSite(localMetadata, tgtDir)
  writeFile(workingDir / "local/site-metadata.json", $localMetadata)

  if args["local"]:
    createDir(tgtDir / "img")
    info "Writing files to local temporary directory: " & tgtDir
    writeImages(srcDir, tgtDir, toSeq(localMetadata.images.values))
    retainLocalDir = true

  elif args["publish"]:
    createDir(workingDir / "remote")

    let targetBucket = $args["<targetBucket>"]
    let remoteMetadata = fetchRemoteMetadata(targetBucket, workingDir / "remote")

    let newImages =
      (toSeq(toHashSet(toSeq(localMetadata.images.keys)) -
       toHashSet(toSeq(remoteMetadata.images.keys)))) -->
      map(localMetadata.images[it])

    pushImages(targetBucket, newImages)
    pushPages(localMetadata, remoteMetadata, tgtDir, targetBucket)
    pushLocalMetadata(workingDir / "local", targetBucket, localMetadata)

 except:
  debug getCurrentException().getStackTrace()
  fatal getCurrentExceptionMsg()
  quit(QuitFailure)
 finally:
  if not retainLocalDir: removeDir(workingDir)
