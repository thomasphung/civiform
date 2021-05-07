import EasyMDE = require("EasyMDE")   // Markdown rich-text editor

new EasyMDE({
  autoDownloadFontAwesome: false,
  element: document.getElementById("block-description-textarea"),
  initialValue: "# EasyMDE \n Go ahead, play around with the editor! Be sure to check out **bold**, *italic* and ~~strikethrough~~ styling, [links](https://google.com) and all the other features. You can type the Markdown syntax, use the toolbar, or use shortcuts like `ctrl-b` or `cmd-b`."
});