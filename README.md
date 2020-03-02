AndroidUtilClasses
==================

A collection of tiny utility classes for Android development.


Included classes
----------------


### [`BitmapLoader`](com/lorenzostanco/utils/BitmapLoader.java)

Sync static functions that load `Bitmap` objects, optionally with automatic subsampling
calculated from desired minimum width and height. Can also just return bitmap size.
Supports various sources:

 * file path as string
 * resources
 * assets

This class is an implementation of this [official training guide](http://developer.android.com/training/displaying-bitmaps/load-bitmap.html).


### [`MailtoWebViewClient`](com/lorenzostanco/utils/MailtoWebViewClient.java)

Attach this web client to a web view to make *mailto:* links work:

	myWebView.setWebViewClient(new MailtoWebViewClient());


### [`LockableViewPager`](com/lorenzostanco/utils/LockableViewPager.java)

Extends `ViewPager` to let you disable paging/swiping:

	myLockableViewPager.setPagingEnabled(false);

All credits for this class goes to [Santiago L. Valdarrama](https://blog.svpino.com/2011/08/29/disabling-pagingswiping-on-android).


### [`ProgressOverlay`](com/lorenzostanco/utils/ProgressOverlay.java)

Simulates a progress overlay creating a `ProgressDialog` with no borders, frame nor text:

	overlay = ProgressOverlay.show(this);
	/* ... */
	overlay.dismiss();


### [`Request`](com/lorenzostanco/utils/Request.java)

A generic, abstract, web service client that uses `HttpURLConnection` inside an `AsyncTask`. It starts requests, read responses and manages event listeners. Two concrete implementations are provided as nested classes: `Request.JSON` and `Request.XML`: both extend the base client, implementing response parsing and error checking.

	url = Uri.parse("http://www.example.com/ws/").buildUpon();
	url.appendQueryParameter("foo", "bar");

	ws = new Request.JSON();
	ws.addEventListener(new Request.EventListener<JSONObject>() {
		public void onSuccess(String url, JSONObject response) { 
			Log.i("WS", "Received: " + response.toString());
		}
	});
	ws.send(url.build().toString());

Inspired by the great [Mootools](http://mootools.net/) [`Request`](http://mootools.net/core/docs/1.5.1/Request/Request) class.


### [`ToastQueue`](com/lorenzostanco/utils/Request.java)

Enqueue toasts in order to avoid Toast overlapping occurring in Android 8.1+. See <https://issuetracker.google.com/issues/79159357>.

	ToastQueue.enqueue(this, "Text", Toast.LENGTH_LONG);
	ToastQueue.enqueue(Toast.makeText(this, "Text", Toast.LENGTH_LONG));


Contribute
----------

If you think a class of your can be useful to all of us, feel free to share it forking
this repo and making a pull request.

I will gladly accept contributions also if they are just fixes or refinements.


License
-------

Classes are licensed under the **[MIT License](LICENSE)**. Just do what you want with this code.

