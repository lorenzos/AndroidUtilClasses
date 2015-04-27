package com.lorenzostanco.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;

/**
 * JSON web service client.
 * Starts requests, read responses and manages event listeners.
 */
public class JSONWebService {

	public final static int READ_BUFFER_SIZE = 4 * 1024;
	public final static int DEFAULT_TIMEOUT = 60000;
	
	// Connection timeout
	private int timeout = DEFAULT_TIMEOUT;
	
	// Current connection
	private HttpURLConnection connection = null;
	private AsyncTask<Void, Void, Object> asyncTask;
	
	// Optional request body, if not NULL a POST request will be used
	private String requestBody = null;

	// Listeners
	private List<EventListener> eventListeners;

	/** Initializes client */
	public JSONWebService() {
		this.eventListeners = new ArrayList<EventListener>();
	}

	/** Initializes client and sets connection timeout
	 * @param timeout Timeout in milliseconds */
	public JSONWebService(int timeout) {
		this();
		this.setTimeout(timeout);
	}

	/** Initializes client with events */
	public JSONWebService(EventListener eventListener) {
		this();
		this.addEventListener(eventListener);
	}
	
	/** Cancel current connection, if any */
	public void cancel() {
		if (this.asyncTask != null) this.asyncTask.cancel(true);
		if (this.isRunning()) {
			String url = this.connection.getURL().toString();
			this.connection.disconnect();
			this.connection = null;
			for (EventListener l : this.eventListeners) l.onCancel(url);
		}
	}

	/** Returns true if client is running a request */
	public boolean isRunning() {
		return this.connection != null;
	}

	/** Adds an event listener
	 * @return This object, for chaining */
	public JSONWebService addEventListener(EventListener l) {
		this.eventListeners.add(l);
		return this;
	}

	/** Sets request timeout
	 * @param timeout Timeout in milliseconds
	 * @return This object, for chaining */
	public JSONWebService setTimeout(int timeout) {
		this.timeout = timeout;
		return this;
	}

	/** Sets the request body, a POST request will be then used
	 * @return This object, for chaining */
	public JSONWebService setRequestBody(String body) {
		this.requestBody = body;
		return this;
	}
	
	/** Clears the request body
	 * @return This object, for chaining */
	public JSONWebService clearRequestBody() {
		this.requestBody = null;
		return this;
	}

	/** Performs request on URL. Used by other public request() methods */
	public void request(final String url) {
		
		// Cancel any running request
		this.cancel();
		
		// Run an asynchronous task in background
		this.asyncTask = new AsyncTask<Void, Void, Object>() {
			
			// Before running request, fire onRequest event
			protected void onPreExecute() {
				for (EventListener l : eventListeners) l.onRequest(url);
			};

			// Background task
			@Override protected Object doInBackground(Void... params) {
				try {
					
					// Open connection and get string
					connection = (HttpURLConnection) new URL(url).openConnection();
					final String response = requestStringSyncFromConnection(connection, requestBody, timeout);
					
					// On success, result is the JSON
					return new JSONObject(response.toString());
					
				} catch (Exception e) {
					
					// On errors, result is the exception
					return e;
					
				}
			}
			
			// After running background task...
			protected void onPostExecute(Object result) {
				if (this.isCancelled()) return;
				
				// Request is complete, errors or not!
				for (EventListener l : eventListeners) l.onComplete(url);
				
				// Close connection
				if (connection != null) {
					connection.disconnect();
					connection = null;
				}
				
				// JSON read?
				if (result instanceof JSONObject) {
					
					// Cast JSON response
					JSONObject response = (JSONObject)result;
					
					// Is error?
					if (response.optBoolean("error", false)) {
						for (EventListener l : eventListeners) l.onError(url, 
							response.optString("errorCode", "unknown_error"), 
							response.optString("errorMessage", "(unknown error)")
						);
						
					// Success?
					} else {
						for (EventListener l : eventListeners) l.onSuccess(url, response);
					}
					
				}
				
				// Exception?
				if (result instanceof Exception) {
					Exception e = (Exception)result;
					final String errorCode = result instanceof SocketException || result instanceof UnknownHostException || result instanceof SocketTimeoutException ? "connection_error" : "unknown_error";
					for (EventListener l : eventListeners) l.onError(url, errorCode, e.getClass().getSimpleName() + ": " + e.getMessage());
				}
				
			};
			
		};
		
		// Go
		this.asyncTask.execute();
		
	}

	/** Read an URL to get a JSON object in a sync way.
	 * This method is not used by class itself, it's intended to be an utility method
	 * @param requestBody The request body, if not null a POST request will be then used */
	public static JSONObject requestJSONObjectSync(final String url, final String requestBody) throws IOException, JSONException {
		return requestJSONObjectSync(url, requestBody, DEFAULT_TIMEOUT);
	}

	/** Read an URL to get a JSON object in a sync way.
	 * This method is not used by class itself, it's intended to be an utility method
	 * @param requestBody The request body, if not null a POST request will be then used
	 * @param timeout Timeout in milliseconds */
	public static JSONObject requestJSONObjectSync(final String url, final String requestBody, int timeout) throws IOException, JSONException {
		return new JSONObject(requestStringSync(url, requestBody, timeout));
	}

	/** Read an URL to get a JSON array in a sync way.
	 * This method is not used by class itself, it's intended to be an utility method
	 * @param requestBody The request body, if not null a POST request will be then used */
	public static JSONArray requestJSONArraySync(final String url, final String requestBody) throws IOException, JSONException {
		return requestJSONArraySync(url, requestBody, DEFAULT_TIMEOUT);
	}

	/** Read an URL to get a JSON array in a sync way.
	 * This method is not used by class itself, it's intended to be an utility method
	 * @param requestBody The request body, if not null a POST request will be then used
	 * @param timeout Timeout in milliseconds */
	public static JSONArray requestJSONArraySync(final String url, final String requestBody, final int timeout) throws IOException, JSONException {
		return new JSONArray(requestStringSync(url, requestBody, timeout));
	}

	/** Read an URL to get a String in a sync way.
	 * This method is not used by class itself, it's intended to be an utility method
	 * @param requestBody The request body, if not null a POST request will be then used */
	public static String requestStringSync(final String url, final String requestBody) throws IOException {
		return requestStringSync(url, requestBody, DEFAULT_TIMEOUT);
	}
	
	/** Read an URL to get a String in a sync way.
	 * This method is not used by class itself, it's intended to be an utility method
	 * @param requestBody The request body, if not null a POST request will be then used
	 * @param timeout Timeout in milliseconds */
	public static String requestStringSync(final String url, final String requestBody, final int timeout) throws IOException {
		final HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection(); // Open connection
		return requestStringSyncFromConnection(connection, requestBody, timeout);
	}

	/** Read an URL to get a String in a sync way, needs an already opened connection. */
	private static String requestStringSyncFromConnection(final HttpURLConnection connection, final String requestBody, final int timeout) throws IOException {
	
		// Setup connection
		connection.setConnectTimeout(timeout);
		connection.setReadTimeout(timeout);
		
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

		// On success, result is the JSON
		inr.close();
		return stringBuffer.toString();

	}

	/** Listener interface for JSON web service events */
	public interface EventListener {
		
		/** Before web request is sent */
		public void onRequest(String url);
		
		/** When web request is canceled */
		public void onCancel(String url);
		
		/** After web request completed, with or without errors */
		public void onComplete(String url);
		
		/** After web request completed successfully and "error" JSON key is FALSE or not set */
		public void onSuccess(String url, JSONObject response);
		
		/** After web request completed with errors, on connection errors or if "error" JSON key is set and TRUE */
		public void onError(String url, String code, String message);
		
	}

	/** Simple implementation for a JSON web service events listener, every method do nothing */
	public static class SimpleEventListener implements EventListener { 
		@Override public void onRequest(String url) { }
		@Override public void onCancel(String url) { }
		@Override public void onComplete(String url) { }
		@Override public void onSuccess(String url, JSONObject response) { }
		@Override public void onError(String url, String code, String message) { } 
	}

}
