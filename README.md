image-resizer
=============

An image thumbnail generation system that is built on Scala and Akka. More information coming.

- Exposes an image broker actor that fetches the requested image, resizes it, caches it to an in-memory cache and returns the cached version on subsequent requests
- Scalable
- Capable of handling massive amounts of image traffic
