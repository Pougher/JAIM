### TODO OVERNIGHT
1) build button class
2) detect buildings
3) get json
4) focus purely on python with a similar json structure

### Run the program
Do: `make && make run`
To recompile: `make`.

## Using Homebrew
Run
```
brew install sdl2 maven openjdk
```
and
```
export PATH="/opt/homebrew/bin:$PATH" >> ~/.zshrc
```

### RENDERING API!!!!!
Custom graphics library which does all of the low level work
using a library called SDL. The API is
structured in the following way:


`ac.warwick.core.Renderer`: This is the "renderer". Give it an
object and it will be rendered on the next frame. This is done by
queuing a resource to be rendered with the
`<renderer>.enqueue_resource(<resource>)` method. The default renderer
for the program is owned by the `App`. Each renderable will be given an ID,
which can be used to remove it from the rendering queue (making it
disappear from the screen). If you want to remove a resource, do
`<renderer>.dequeue_resource(<resource-id>)`. **Another thing - the rendering
queue is persistent - a resource added on one frame will continue to be rendered
on the next frame**.

`ac.warwick.core.Window`: A wrapper for the window handle. That's it!

### RENDERABLES
Under this approach we are using renderables. These
are little objects which implement rendering/update behaviour, which can be
added to the renderer for drawing. Currently we have
- `Bitmap`: Renders a bitmap texture to the screen at specified coordinates. This
  bitmap texture has a specific size (image dimensions in pixels).
  Bitmaps also have an $x$ and $y$
  position, controlling where they are drawn on the screen. You can set the
  pixel data inside of a bitmap by directly accessing the bitmap pixel buffer
  `<bitmap>.pixels`. After you have modified the pixels in the pixel buffer,
  you can update the buffer using the `send_to_gpu` method. This changes the
  physical texture in video memory.
