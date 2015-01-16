#!/usr/bin/env python

#
# toElephant.py -- given a file as argument, create an empty note in Elephant and insert file as attachment.
#

import sys, os

# Modify 'target' to point to your Inbox
target="/Users/jusu/Desktop/Elephant/Inbox"

f = sys.argv[1]

# Disk:Users:... -> /Users/...
idx = f.find(":")
if idx >= 0:
    f = "/" + f[idx+1:].replace(":", "/")

base=os.path.basename(f)

# text file? just move it over, no attachments.
ext = os.path.splitext(f)[1]
if ext in ['.txt', '.rtf']:
    cmd="/bin/mv -n \"{0}\" \"{1}/{2}\"".format(f, target, base)
    os.system(cmd);
    sys.exit(0)

# something else - create empty note + attach it
attachments="{0}/{1}.txt.attachments".format(target, base)

cmd1="/bin/mkdir \"{0}\"".format(attachments)
cmd2="/bin/mv -n \"{0}\" \"{1}\"".format(f, attachments)
cmd3="/usr/bin/touch \"{0}/{1}.txt\"".format(target, base)

os.system(cmd1)
os.system(cmd2)
os.system(cmd3)
