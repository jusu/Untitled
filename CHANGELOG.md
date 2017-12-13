# Changelog

##v43
- Menu font set to plain style in windows
- Arrows up/down change note selection when search field is focused

##v42
- Fixed visual bug on Retina Macbooks when multiple notes are selected
- Fixed ui focus when clearing search input with backspace (now search input stays focused)
- Changed window title to "Elephant"

##v41
- Support macOS-style full screen
- Now cleaning out some old, unused cache files from .imagecache folder

##v40
- Draw sharper images and PDFs on Retina Macbooks
- Note|Note count - menu item
- Clearing search goes back to notebook/note prior search
- New App icon

##v39
- Added "charset" setting to optionally use a character set different from system character set. For example, "UTF-8"
- Draw sharper background tiles on Retina Macbooks

##v38
(v38 had an issue with character sets)

##v37
- Added "defaultFiletype" setting. Use "md" to create new notes in markdown format. Default is "txt".
- Reverted automatic removal of "md" from note title

##v36
- Markdown notes are saved as 'filename.md' instead of 'filename.md.txt'
- Editor will automatically remove .md from note title when saving a markdown note
- Fixed unusual hang with unfortunate search term

##v35
- Fixed wrong ui state for right panel when moving multiple notes and notebook is left empty.
- Fixed clicking X in searchfield, now works same as ESC

##v34
- Fixed undo handling when moving and then editing notes

##v33
- Added text encryption/decryption via menu items: Edit | Encrypt Selection to Clipboard / Decrypt to Clipboard

##v32
- Added searching inside attachments. Most file types are supported (office docs, pdfs, etc)
- Now using Apache PdfBox for better PDF support
- Faster multithreaded Pdf rendering
- Added ability to Undo note deletion/moving
- App closes properly when last ElephantWindow is closed
- Fixed Tag view drawing bug when >= 100 notes on a tag
- Single note window now uses note title as window title

##v31
- Fixed some key handling issues when separate note windows open

##v30
- Fixed occasional broken images with markdown notes
- Mac: No longer forces dedicated graphics with machines that use graphics switching.
  (Mac version now builds with forked appbundler from https://bitbucket.org/infinitekind/appbundler)

##v29
- PDFs are rendered at better quality when needed
- Scrollbars now look the part in all platforms
- Some memory was not collected when closing note windows

##v28
- Added View | Hide Sidebar
- Added Note | Open Note in Separate Window. Also works by doubleclicking a note in Note List
- Added File | Close

##v27
- Fixed CTRL+ keymapping  (increase text size) for US key layouts.

##v26
- Fixed bug affecting copy-paste from rtf-note to plain text note
- Added fontScale setting for scaling fonts. Set to less than one to shrink and more than one to enlarge.
- Added "pastePlaintext" setting to always paste text in plain text, ignoring styling. Default is false.

##v25
- Fixed note scrolling bug with html notes using frameset redirection
- Added possibility to permanently delete note from Trash.
- Save/Load maximized state of window in settings
- Merged: Added external Stylesheet for HtmlPane #71
- Merged: exportJar.xml cleanup
- Added disabling note previews by notebook. Add empty file ".disablePreview" to notebook folder to disable note previews for that folder.

##v24
- NoteEditor: fixed going to edit mode by clicking empty note area with short markdown notes
- NoteList: scrolling speed doubled from 5 to 10, matching NoteEditor scroll speed
- Fixed dropping multiple files - attachments appeared on same line
- Added text (stdin/stdout) interface for search queries
- Suppressed pdf rendering errors from com.sun.pdfview

##v23
- Added markdown table support (from Pegdown parser)
- Added markdown FENCED_CODE_BLOCKS
- Added markdown DEFINITION_LISTS
- Added "allowFilenameChars" setting to allow custom characters in note filenames
- Added View | Back and View | Forward menu options

##v22
- Fixed invisible editor when editing markdown notes

##v21

with many contributors, see Github https://github.com/jusu/Elephant/graphs/contributors

- Added 'autoBullet' setting to toggle automatically extending lists on enter
- Search words are highlighted in matching notes
- Added search by date
- Added View | Recent Notes menu option
- Added Note | Word Count menu option
- Added notebook trash button
- Fixed missing date from markdown preview
- Fixed Cmd-N/Cmd-A losing editor changes

##v20

with contributions from Oliver Kopp, thanks!
Contributors are listed in https://github.com/jusu/Elephant/graphs/contributors

- "Add Notebook to Shortcuts" command under Note menu
- Bullet list handling: automatically insert bullet char *+- when making lists
- Automatic list indenting with Tab and Shift-Tab
- Enabled delete key to delete a note, notebook and tag
- Added File - Switch Note Location
- Convert plain links to markdown links
- HOME key jumps to beginning of text in line instead of beginning of line

##v19

- Fixed cut/copy/paste menu items on win and linux (menu items were disabled)
- Delete tag: use backspace to delete selected tag in tags view
- Delete notebook: use backspace to delete selected notebook in notebooks view
(notebook directory along with any notes is moved to Trash)
- Note -> Add Note to Shortcuts (finally!)
- Edit -> Save Search to Shortcuts
- Drag shortcuts with mouse to rearrange, drag right to remove

##v18

- Fixed some unwanted borders on linux/ubuntu
- Fixed opening file attachments on some windows machines
- Fixed taskbar icon on windows
- Help menu shows version number

##v17

- Snippet View (change from View menu)

##v16

- Moving attachments around the note (just drag the image/attachment to new position)
- Double-click opens image attachments with default app

##v15

- Multiple note selection with moving, tagging, trashing
- Snappier note loading with large image attachments or pdfs
- Fixed opening file attachments on enex->markdown converted notes on windows
- json files (settings, shortcuts, ...) are written with indentation

##v14

- Fixed HTML notes not always being scrollable

##v13

- Show HTML files as read-only notes, uses WebKit for rendering. Use "Save Page As" on Chrome to save full webpages directly to an Elephant folder. Export as HTML from other apps to save a read-only copy in Elephant.
- Added Format - Style - Strikethrough
- Fixed changing font size when no selection
- Fix for opening attachment links after using https://github.com/zerobase/enml2md for enex -> markdown import.

##v12

- Use Lucene for search indexing when over 2k notes (override with "useLucene=1/0" in settings). Upper note limit now in the zillions.
- New Tag - button

##v11

- Remade notebook chooser for jumping/moving. Looks great.

##v10

- Shortcuts support tags and saved searches. add "search:today" for a search shortcut, "search:tag:todo" for a tag.
- Small visual fixes

##v9

- Improvements for syncing scenarios
- Some visual fixes for linux

##v8

- Style commands work as expected with markdown
- Note card shows markdown rendering
- Fixed notebook refreshing on linux

##v7

- Initial markdown support
- Fixed attachments 'drifting' in note text, sometimes adding extra whitespace

##v6

- Inline display for PDFs, yay!
- Shortcuts survive note moving / renaming

##v5

- Initial Tag view
- Fixed issues with multiple Elephant windows
- "defaultNotebook" setting in main settings to set def notebook (no UI yet)
- Now watching filesystem for changes and refreshing notebooks when a file is dropped on a notebook folder
- Now requires Java 1.7 or newer due to watched folders
- (Mac) Hit space to quicklook first file attachment when browsing notes

##v4

- Search is indexed/optimized for about 10,000 notes
- Preserve text styles when copy-pasting
- Can add tags to notes and search by tags
- Search with "tag:cats" or "t:cats" to limit results by tag
- Search with "notebook:inbox" or "nb:inbox" to limit results by notebook name
- Note preview brushup

##v3

- fixed trashing a note from "All Notes" list
- added Edit | Undo/Redo
- added Format | Style menu with a few styles and text size options. Some text needs to be selected when changing text size
- Applying styles will save the note in rft format
- Added Format | Make Plain Text to turn the note back to plain text

##v2

- all metadata is stored under one ".meta" folder
- attached images are scaled to editor width
- note list thumbnails have images
- recent notes fixed when moving notes

