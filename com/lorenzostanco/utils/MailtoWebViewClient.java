package com.lorenzostanco.utils;

import android.content.Context;
import android.content.Intent;
import android.net.MailTo;
import android.net.Uri;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Attach this web client to a WebView to make "mailto:" links work:
 * {@code myWebView.setWebViewClient(new MailtoWebViewClient());}
 */
@SuppressWarnings("unused") public class MailtoWebViewClient extends WebViewClient {
	
	@Override public boolean shouldOverrideUrlLoading(final WebView view, final String url) {
		if (url.startsWith("mailto:")) {
			final MailTo mt = MailTo.parse(url);
			final Intent i = newEmailIntent(view.getContext(), mt.getTo(), mt.getSubject(), mt.getBody(), mt.getCc());
			view.getContext().startActivity(i);
		} else {
			view.getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
		}
		return true;
	}
	
	public static Intent newEmailIntent(final Context context, final String address, final String subject, final String body, final String cc) {
		final Intent intent = new Intent(Intent.ACTION_SEND);
		intent.putExtra(Intent.EXTRA_EMAIL, new String[] { address });
		intent.putExtra(Intent.EXTRA_TEXT, body);
		intent.putExtra(Intent.EXTRA_SUBJECT, subject);
		intent.putExtra(Intent.EXTRA_CC, cc);
		intent.setType("message/rfc822");
		return intent;
	}
	
}
