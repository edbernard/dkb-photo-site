import sys
import os.path
import json
import time

from importlib_metadata import metadata

# Building the metadata file.
# 1. Scan the directory of files.
# 2. Compare the files in the directory with the metadata in S3.
#    2a. If there is no metadata on S3, create a new metadata file.
# 3. Upload any new files to S3.
# 4. Update the metadata file to include all files in the directory

# Building the pages
# 1. Read the metadata file (this will give you a tree data structure)

# 1a. Read the metadata file path from our first argument.
if len(sys.argv) != 2:
    print("this script expects 1 argument: path to the metadata file, quitting", file=sys.stderr)
    quit(1)
    
metadata_file_path = sys.argv[1]

# 1b. Check to see if the file exists.
if not os.path.exists(metadata_file_path):
    # 1ba. If the file doesn't exist, then print an error and quit.
    print("file doesn't exist, quitting", file=sys.stderr)
    quit(1)

# 1c. Read the file and parse it into our tree structure.
with open(metadata_file_path, 'r') as metadata_file:
    # this block
    metadata = json.load(metadata_file)

# 2. Walk the tree using depth first search.
# For now, we're cheating, and just expecting the metadata itself to be a page_node

# 3. For every node in the tree (except for leaves) create a page on the website
def generate_category_page(page_node):
    print("The category name is: ", page_node["category_name"], file=sys.stderr)
    print("The category description is: ", page_node["description"], file=sys.stderr)
    print("There are ", len(page_node["child_categories"]), " child categories in this one.", file=sys.stderr)
    html_text = """
<!DOCTYPE html>
<html>
    <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1, minimum-scale=1">
        <link rel="stylesheet" href="/dkb-photo-site.css">
    </head>
    <body>
        <script>document.body.classList.add("loading");</script>
        <div class="loading-splash">\n"""
    
    html_text += "          <h1><span>David K. Bernard</span> <span>|</span> <span>" + page_node["category_name"] + "</span></h1>"
    html_text += """
        </div>
        <header>
          <h1>
            <a href=".."><span>David K. Bernard</span></a>
            <span>|</span>\n"""
    html_text += '            <a href="."><span>' + page_node["category_name"] + '</span></a>\n'
    html_text += """          </h1>"""
    html_text += '          <div class="description">' + page_node["description"] + '</div>'
    html_text += """        </header>

        <!-- Catagories -->
        <div class="categories">\n\n"""
        
    for child in page_node["child_categories"]:
        html_text += '          <a href="' + child["slug"] + '">\n'
        html_text += '              <img src="' + child["slug"] + '/' + child["cover_photo"] + '">\n'
        html_text += '              <h2 class="category-title">' + child["category_name"] + '</h2>\n'
        html_text += '          </a>\n\n'

    html_text += """        </div>

        <footer>Created by Elijah Bernard</footer>
        <script type="application/javascript">
          window.onload = function() {
            document.body.classList.remove('loading');
          }
        </script>
        <script type="application/javascript" src="dkb-photo-site.js"></script>
    </body>
</html>
"""
    return html_text


print(generate_category_page(metadata))


# n = len(sys.argv)
# print("Total arguments passed:", n)

# print("\nName of Python Script", sys.argv[0])

# print("\nArguments passed:", end = " ")
# for i in range(1, n):
#    print(sys.argv[i], end = " ")

# Sum = 0
# for i in range(1, n):
#    Sum += int(sys.argv[i])

#    print ("\n\nResult:", Sum)
