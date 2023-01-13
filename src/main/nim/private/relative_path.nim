import std/[os, sequtils, strutils]

proc relativePath*(ancestor, child: string): string =
  ## Given a ancestor path and a child path, assuming the child path is
  ## contained within the ancestor path, return the relative path from the
  ## ancestor to the child.

  let ancestorPath = ancestor.expandFilename.split({DirSep, AltSep})
  let childPath = child.expandFilename.split({DirSep, AltSep})

  # If the ancestor path is longer it cannot contain the child path and we
  # cannot construct a relative path without backtracking.
  if (ancestorPath.len > childPath.len): return ""

  # Compare the ancestor and child path up until the end of the ancestor path.
  var idx = 0
  while idx < ancestorPath.len and ancestorPath[idx] == childPath[idx]: idx += 1

  # If we stopped before reaching the end of the ancestor path it must be that
  # the paths do not match. The ancestor cannot contain the child and we cannot
  # build a relative path without backtracking.
  if idx != ancestorPath.len: return ""
  return foldl(@["."] & childPath[idx..childPath.high], joinPath(a, b))
