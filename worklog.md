DKB Photo Site
========================================

Current: trying to figure out how I want
to generate the HTML.

Remaining To Do
----------------------------------------

* Rsync-ish upload (diff and only upload
  new content. Check file hashes and
  rename files that have only changed
  names).

* Configuration for server/upload.

Structure For Traversal
----------------------------------------

    String visit(rootNode, currentNode)

    Node: {
      parent: Node,
      title: String,
      description: String,
      subcategories: Node[],
      files: PhotoNode,
      relativePathToRoot: String
    }

_fold in category properties from the
properties file_
