AndroidUtilClasses
==================

A growing collection of tiny utility classes for Android development.


Included classes
----------------


### `JSONWebService`

An `AsyncTask` wrapper to start requests to JSON web services and
easily listen for events such as:

 * request started;
 * request cancelled;
 * request completed;
 * success (JSON object received and correctly parsed);
 * error (IO problems, invalid JSON or `{ error: true }` in the received JSON object);

Inspired by the great [Mootools](http://mootools.net/) [`Request.JSON`](http://mootools.net/core/docs/1.5.1/Request/Request.JSON) class.


### `BitmapLoader`

Sync static functions that load `Bitmap` objects, optionally with automatic subsampling
calculated from desired minimum width and height. Can also just return bitmap size.
Supports various sources:

 * file path as string;
 * resources;
 * assets.

This class is an implementation of this [official training guide](http://developer.android.com/training/displaying-bitmaps/load-bitmap.html).


Contribute
----------

If you think a class of your can be useful to all of us, feel free to share it forking
this repo and making a pull request.

I will gladly accept contributions also if they are just fixes or refinements.


License
-------

Classes are licensed under the **[MIT License](http://choosealicense.com/licenses/mit)**. Just do what you want with this code.

