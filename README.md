# IM WHACKING IT OH YEAH SO GOOD
67


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
