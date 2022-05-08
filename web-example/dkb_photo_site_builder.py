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
if len(sys.argv) != 3:
    print("this script expects 2 arguments: path to the metadata file, " +
        "and the root directory for the generated site, quitting", file=sys.stderr)
    quit(1)
    
metadata_file_path = sys.argv[1]
target_directory_root = sys.argv[2]

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
def generate_category_page(page_node, target_directory):
    # 2a. Check if the target directory exists, if not, create it
    if not os.path.isdir(target_directory):
        os.makedirs(target_directory)
    
    # 2b. Generate the HTML content
    category_page_html = generate_category_page_html(page_node)
    
    # 2c. Save the HTML content to the 'index.html' file in the target directory.
    # https://python.readthedocs.io/en/stable/library/functions.html#open
    with open (target_directory + '/index.html', 'w') as index_file:
        index_file.write(category_page_html)
        
    # 2d. Generate all the child categories
    for child in page_node["child_categories"]:
        generate_category_page(child, target_directory + "/" + child["slug"])


# 3. For every node in the tree (except for leaves) create a page on the website
def generate_category_page_html(page_node):
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

        <!-- Photos -->
        <div class="photos">\n\n"""

    for photo_url in page_node["photos"]:
        html_text += '<img src="' + photo_url + '">'

    html_text += """
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


generate_category_page(metadata, target_directory_root)


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
