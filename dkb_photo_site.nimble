# Package

version       = "0.1.0"
author        = "Jonathan Bernard"
description   = "David K. Bernard Photography Site Publisher"
license       = "Proprietary"
srcDir        = "src/main/nim"
bin           = @["dkb_photo_site"]


# Dependencies

requires @["nim >= 1.6.6", "docopt", "tempdir", "zero_functional"]
#requires "https://git.jdb-software.com/jdb/console_progress"
requires "https://git.jdb-software.com/jdb/nim-cli-utils"
