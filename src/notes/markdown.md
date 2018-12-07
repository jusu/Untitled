## Markdown support

Here's how it works:

  1. Files ending with ".md" or ".md.txt" are markdown-enabled
  1. To create a new markdown note, end the title with .md and save
  1. After saving as markdown, .md can be removed from the title
  1. The note is shown in all rich text glory
  1. Click on the note to edit it. Editing always happens in plain text / markdown mode
  1. Click the note card or editor background to go back to rich display

## Click here to edit,

## Click the note card or editor background to render.

### Tips

- Try dropping images or attachments to a markdown note (when editing).

- <u>Try the styling commands!</u> _Select_ some text and hit **cmd-b** / **cmd-i** / **cmd-u** / **cmd-+-** !

- Inline html might be useful as well:

<table style="width:50%">
  <tr>
    <td>Jill</td>
    <td>Smith</td> 
    <td>50</td>
  </tr>
  <tr>
    <td>Eve</td>
    <td>Jackson</td> 
    <td>94</td>
  </tr>
</table>

[Markdown syntax cheatsheet](https://github.com/adam-p/markdown-here/wiki/Markdown-Cheatsheet)

You can also use file:// links to point to filesystem:

file:///Users

Links can use environment variables:

file://${HOME}/

search:// works as a shortcut to Elephant's search field:

search://@Inbox

search://some+notes

search://#

Last one searches for notes with any tag.
