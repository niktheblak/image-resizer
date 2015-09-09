image-resizer
=============

An image thumbnail generation system that is built on Scala and Akka.

- Provides an image broker actor that fetches the requested image, resizes it, caches it to a Facebook Haystack-style cache and returns the cached version on subsequent requests
- Scalable
- Capable of handling massive amounts of image traffic

## Installation

Clone the repository

    $ git clone https://niktheblak@bitbucket.org/niktheblak/image-resizer.git

## Usage

Start the service with SBT

    $ sbt run

Select `org.ntb.imageresizer.SprayBootstrap` from the options presented by SBT.

After this the service exposes a web API at http://localhost:8080/resize. The API takes the following parameters:

| Name   | Type    | Required | Description                                   |
|--------|-------- |----------|-----------------------------------------------|
| source | String  | x        | URL of the source image                       |
| size   | Integer | x        | Desired size of the image                     |
| format | String  |          | Desired image format (`jpeg`, `png` or `gif`) |

For example:

    http://localhost:8080/resize?source=https://channelcloud-a.akamaihd.net/mobilevideopanel_2c9314d9aa5f4c4a_364x618_29ada748cb59b5dd.JPEG&size=800

Try changing the `size` parameter for different results.

Shut down the service by pressing `ENTER` on the command prompt window.

## License

Copyright © 2015 Niko Korhonen
