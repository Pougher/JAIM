# IM WHACKING IT OH YEAH SO GOOD
67 jarona

Do you want to run the program? I hope so. Do `make && make run`
Recompilation? `make`.

## Homebrew is not shady
Run
```
brew install sdl2 maven openjdk
```
and
```
export PATH="/opt/homebrew/bin:$PATH" >> ~/.zshrc
```

### RENDERING API!!!!!
![](res/netanyahu.png)
hi guys; I've written a graphics library which does all of the low level crap
with a library called SDL (which is pretty cool). Right now, the API is
structured in the following way:


`ac.warwick.core.Renderer`: This is the "renderer". Just give it a renderable
object and it will shove it on the screen on the next frame. This is done by
queuing a resource to be rendered with the
`<renderer>.enqueue_resource(<resource>)` method. The default renderer
for the program is owned by the `App`. Each renderable will be given an ID,
which can be used to remove the renderable from the rendering queue (making it
disappear from the screen). If you want to remove a resource, just do
`<renderer>.dequeue_resource(<resource-id>)`. **Another thing - the rendering
queue is persistent - a resource added on one frame will continue to be rendered
on the next frame**.

`ac.warwick.core.Window`: Just a wrapper for the window handle.
It doesn't really do anything outside of that

### RENDERABLES???
I'm stupid and couldn't think of a better approach so I made renderables. These
are little objects which implement some rendering/update behaviour, which can be
added to the renderer for drawing. Currently we have
- `Bitmap`: Renders a bitmap texture to the screen at some coordinates. This
  bitmap texture has a specific size (image dimensions in pixels).
  Bitmaps also have an $x$ and $y$
  position, controlling where they are drawn on the screen. You can set the
  pixel data inside of a bitmap by directly accessing the bitmap pixel buffer
  `<bitmap>.pixels`. After you have modified the pixels in the pixel buffer,
  you can update the buffer using the `send_to_gpu` method. This changes the
  physical texture in video memory blah blah.
