package com.lorenzostanco.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import android.os.AsyncTask;

/**
 * Generic, abstract, web service client.
 * Starts requests, read responses and manages event listeners.
 * 
 * <pre>
 * url = Uri.parse("http://www.example.com/ws/").buildUpon();
 * url.appendQueryParameter("foo", "bar");
 * 
 * ws = new Request.JSON();
 * ws.addEventListener(new Request.EventListener&lt;JSONObject&gt;() {
 * 	public void onSuccess(String url, JSONObject response) { 
 * 		Log.i("WS", "Received: " + response.toString());
 * 	}
 * });
 * ws.send(url.build().toString());
 * </pre>
 */
public abstract class Request<T> {

	public final static int READ_BUFFER_SIZE = 4 * 1024;
	public final static int DEFAULT_TIMEOUT = 60000;
	
	// Connection timeout
	protected int timeout = DEFAULT_TIMEOUT;
	
	// Current connection
	protected HttpURLConnection connection = null;
	private AsyncTask<Void, Void, Object> asyncTask;
	
	// Optional request body, if not NULL a POST request will be used
	protected String requestBody = null;
	
	// Optional request headers
	protected Map<String, String> requestHeaders;

	// Listeners
	protected List<IEventListener<T>> eventListeners;

	/** Initializes the client */
	public Request() {
		this.eventListeners = new ArrayList<IEventListener<T>>();
		this.requestHeaders = new HashMap<String, String>();
	}
	
	/** Adds an event listener
	 * @return This object, for chaining */
	public Request<T> addEventListener(IEventListener<T> l) {
		this.eventListeners.add(l);
		return this;
	}

	/** Sets request timeout
	 * @param timeout Timeout in milliseconds
	 * @return This object, for chaining */
	public Request<T> setTimeout(int timeout) {
		this.timeout = timeout;
		return this;
	}

	/** Sets the request headers
	 * @return This object, for chaining */
	public Request<T> setRequestHeaders(Map<String, String> headers) {
		this.requestHeaders.clear();
		this.requestHeaders.putAll(headers);
		return this;
	}

	/** Sets the request body, a POST request will be then used
	 * @return This object, for chaining */
	public Request<T> setRequestBody(String body) {
		this.requestBody = body;
		return this;
	}

	/** Sets the request body from any object (using <code>.toString()</code>), a POST request will be then used
	 * @return This object, for chaining */
	public Request<T> setRequestBody(Object body) {
		this.setRequestBody(body.toString());
		return this;
	}
	
	/** Clears the request headers
	 * @return This object, for chaining */
	public Request<T> clearRequestHeaders() {
		this.requestHeaders.clear();
		return this;
	}

	/** Clears the request body
	 * @return This object, for chaining */
	public Request<T> clearRequestBody() {
		this.requestBody = null;
		return this;
	}

	/** Performs request on URL. Used by other public request() methods */
	public void send(final String url) {
		
		// Cancel any running request
		this.cancel();
		
		// Run an asynchronous task in background
		this.asyncTask = new AsyncTask<Void, Void, Object>() {
			
			// Before running request, fire onRequest event
			protected void onPreExecute() {
				for (IEventListener<T> l : eventListeners) l.onRequest(url);
			};

			// Background task
			@Override protected Object doInBackground(Void... params) {
				try {
					final Object response = requestInBackground(url);
					disconnect();
					return response;
				} catch (Exception e) {
					disconnect();
					return e; // On errors, result is the exception
				}
			}
			
			// After running background task...
			protected void onPostExecute(Object result) {
				if (this.isCancelled()) return;
				
				// Request is complete, errors or not!
				for (IEventListener<T> l : eventListeners) l.onComplete(url);
				
				// Exception?
				if (result instanceof Exception) {
					Exception e = (Exception)result;
					final String errorCode = result instanceof SocketException || result instanceof UnknownHostException || result instanceof SocketTimeoutException ? "connection_error" : "unknown_error";
					for (IEventListener<T> l : eventListeners) l.onError(url, errorCode, e.getClass().getSimpleName() + ": " + e.getMessage());
					
				} else {
					postExecute(url, result);
				}
				
			};
			
		};
		
		// Go
		this.asyncTask.execute();
		
	}

	/** Cancel current connection, if any */
	public void cancel() {
		if (this.asyncTask != null) this.asyncTask.cancel(true);
		if (this.isRunning()) {
			String url = this.connection.getURL().toString();
			disconnect();
			for (IEventListener<T> l : this.eventListeners) l.onCancel(url);
		}
	}

	/** Returns true if client is running a request */
	public boolean isRunning() {
		return this.connection != null;
	}

	/** Closes the connection */
	private void disconnect() {
		if (connection != null) {
			connection.disconnect();
			connection = null;
		}
	}
	
	/** Makes the HTTP request in a sync-way
	 * @throws Exception In case of any network error
	 * @return An object as the result of the HTTP request */
	protected abstract Object requestInBackground(final String url) throws Exception;
	
	/** Consumes the HTTP request in a sync-way, firing onSuccess() or onError() on listeners */
	protected abstract void postExecute(final String url, Object result);
	
	/** Read an URL to get a String in a sync way.
	 * This method is not used by class itself, it's intended to be an utility method
	 * @param requestHeaders The request headers
	 * @param requestBody The request body, if not null a POST request will be then used */
	public static String requestStringSync(final String url, final Map<String, String> requestHeaders, final String requestBody) throws IOException {
		return requestStringSync(url, requestHeaders, requestBody, DEFAULT_TIMEOUT);
	}
	
	/** Read an URL to get a String in a sync way.
	 * This method is not used by class itself, it's intended to be an utility method
	 * @param requestHeaders The request headers
	 * @param requestBody The request body, if not null a POST request will be then used
	 * @param timeout Timeout in milliseconds */
	public static String requestStringSync(final String url, final Map<String, String> requestHeaders, final String requestBody, final int timeout) throws IOException {
		final HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection(); // Open connection
		return requestStringSyncFromConnection(connection, requestHeaders, requestBody, timeout);
	}

	/** Read an URL to get a String in a sync way, needs an already opened connection. */
	private static String requestStringSyncFromConnection(final HttpURLConnection connection, final Map<String, String> requestHeaders, final String requestBody, final int timeout) throws IOException {
	
		// Setup connection
		connection.setConnectTimeout(timeout);
		connection.setReadTimeout(timeout);
		if (requestHeaders != null) for (String header : requestHeaders.keySet()) {
			connection.setRequestProperty(header, requestHeaders.get(header));
		}
		
		// Optional request body
		if (requestBody != null) {
			connection.setDoOutput(true);
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
			out.write(requestBody);
			out.flush();
			out.close();
		} else {
			connection.setDoOutput(false);
			connection.setRequestMethod("GET");
		}

		// Read input stream
		InputStream in = connection.getInputStream();
		InputStreamReader inr = new InputStreamReader(in, "UTF-8");
		int read = 0;
		char[] buffer = new char[READ_BUFFER_SIZE];
		StringBuffer stringBuffer = new StringBuffer();
		while ((read = inr.read(buffer)) > 0) stringBuffer.append(buffer, 0, read);

		// On success, result is the response string
		inr.close();
		return stringBuffer.toString();

	}

	/** Listener interface for web service events */
	public interface IEventListener<T> {
		
		/** Before web request is sent */
		public void onRequest(String url);
		
		/** When web request is canceled */
		public void onCancel(String url);
		
		/** After web request completed, with or without errors */
		public void onComplete(String url);
		
		/** After web request completed successfully and the "error" condition is FALSE or not set */
		public void onSuccess(String url, T response);
		
		/** After web request completed with errors, on connection errors or if the "error" condition is set and TRUE */
		public void onError(String url, String code, String message);
		
	}

	/** Simple implementation for a web service events listener, every method do nothing */
	public static class EventListener<T> implements IEventListener<T> { 
		@Override public void onRequest(String url) { }
		@Override public void onCancel(String url) { }
		@Override public void onComplete(String url) { }
		@Override public void onSuccess(String url, T response) { }
		@Override public void onError(String url, String code, String message) { } 
	}

	/** 
	 * Concrete implementation for a web service which outputs a JSON object 
	 * @see Request
	 */
	public static class JSON extends Request<JSONObject> {

		/** Initializes the client */
		public JSON() {
			super();
		}
		
		@Override protected Object requestInBackground(final String url) throws Exception {
			
			// Open connection and get string
			connection = (HttpURLConnection) new URL(url).openConnection();
			final String response = Request.requestStringSyncFromConnection(connection, requestHeaders, requestBody, timeout);
			
			// On success, result is the JSON
			return new JSONObject(response.toString());
			
		}

		@Override protected void postExecute(final String url, Object result) {
			
			// Cast JSON response
			JSONObject response = (JSONObject)result;
			
			// Is error?
			if (response.optBoolean("error", false)) {
				for (IEventListener<JSONObject> l : eventListeners) l.onError(url, 
					response.optString("error_code", "unknown_error"), 
					response.optString("error_message", "(unknown error)")
				);
				
			// Success?
			} else {
				for (IEventListener<JSONObject> l : eventListeners) l.onSuccess(url, response);
			}
			
		}
		
		/** Read an URL to get a JSON object in a sync way.
		 * This method is not used by class itself, it's intended to be an utility method
		 * @param requestHeaders The request headers
		 * @param requestBody The request body, if not null a POST request will be then used */
		public static JSONObject requestJSONObjectSync(final String url, final Map<String, String> requestHeaders, final String requestBody) throws IOException, JSONException {
			return requestJSONObjectSync(url, requestHeaders, requestBody, DEFAULT_TIMEOUT);
		}

		/** Read an URL to get a JSON object in a sync way.
		 * This method is not used by class itself, it's intended to be an utility method
		 * @param requestHeaders The request headers
		 * @param requestBody The request body, if not null a POST request will be then used
		 * @param timeout Timeout in milliseconds */
		public static JSONObject requestJSONObjectSync(final String url, final Map<String, String> requestHeaders, final String requestBody, int timeout) throws IOException, JSONException {
			return new JSONObject(requestStringSync(url, requestHeaders, requestBody, timeout));
		}

		/** Read an URL to get a JSON array in a sync way.
		 * This method is not used by class itself, it's intended to be an utility method
		 * @param requestHeaders The request headers
		 * @param requestBody The request body, if not null a POST request will be then used */
		public static JSONArray requestJSONArraySync(final String url, final Map<String, String> requestHeaders, final String requestBody) throws IOException, JSONException {
			return requestJSONArraySync(url, requestHeaders, requestBody, DEFAULT_TIMEOUT);
		}

		/** Read an URL to get a JSON array in a sync way.
		 * This method is not used by class itself, it's intended to be an utility method
		 * @param requestHeaders The request headers
		 * @param requestBody The request body, if not null a POST request will be then used
		 * @param timeout Timeout in milliseconds */
		public static JSONArray requestJSONArraySync(final String url, final Map<String, String> requestHeaders, final String requestBody, final int timeout) throws IOException, JSONException {
			return new JSONArray(requestStringSync(url, requestHeaders, requestBody, timeout));
		}
		
	}
	

	/** 
	 * Concrete implementation for a web service which outputs a XML document
	 * @see Request
	 */
	public static class XML extends Request<Document> {

		/** Initializes the client */
		public XML() {
			super();
		}
		
		@Override protected Object requestInBackground(final String url) throws Exception {
			
			// Open connection and get string
			connection = (HttpURLConnection) new URL(url).openConnection();
			final String response = Request.requestStringSyncFromConnection(connection, requestHeaders, requestBody, timeout);
			
			// On success, result is the XML document
			final StringReader responseReader = new StringReader(response);
			final Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(responseReader));
			responseReader.close();
			return document;
			
		}

		@Override protected void postExecute(final String url, Object result) {
			
			// Cast JSON response
			Document response = (Document)result;
			
			// Is error?
			final Element responseElement = response.getDocumentElement();
			if (responseElement.hasAttribute("error") && responseElement.getAttribute("error").equals("1")) {
				for (IEventListener<Document> l : eventListeners) l.onError(url, 
					responseElement.hasAttribute("error_code") ? responseElement.getAttribute("error_code") : "unknown_error",
					responseElement.hasAttribute("error_message") ? responseElement.getAttribute("error_message") : "(unknown error)"
				);
				
			// Success?
			} else {
				for (IEventListener<Document> l : eventListeners) l.onSuccess(url, response);
			}
			
		}

		/** Read an URL to get a XML document in a sync way.
		 * This method is not used by class itself, it's intended to be an utility method
		 * @param requestHeaders The request headers
		 * @param requestBody The request body, if not null a POST request will be then used */
		public static Document requestXMLDocumentSync(final String url, final Map<String, String> requestHeaders, final String requestBody) throws Exception {
			return requestXMLDocumentSync(url, requestHeaders, requestBody, DEFAULT_TIMEOUT);
		}

		/** Read an URL to get a XML document in a sync way.
		 * This method is not used by class itself, it's intended to be an utility method
		 * @param requestHeaders The request headers
		 * @param requestBody The request body, if not null a POST request will be then used
		 * @param timeout Timeout in milliseconds */
		public static Document requestXMLDocumentSync(final String url, final Map<String, String> requestHeaders, final String requestBody, int timeout) throws Exception {
			final String response = requestStringSync(url, requestHeaders, requestBody, timeout);
			final StringReader responseReader = new StringReader(response);
			final Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(responseReader));
			responseReader.close();
			return document;
		}

	}
	
}
