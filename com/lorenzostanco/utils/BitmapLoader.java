package com.lorenzostanco.utils;

import java.io.IOException;
import java.io.InputStream;

import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

/**
 * Carica in memoria immagini da file e risorse nel modo più efficiente possibile.
 */
@SuppressWarnings("unused") public class BitmapLoader {

	public final static boolean LOG = true;
	public final static String TAG = "BITMAP-LOADER";

	/** 
	 * Carica un'immagine da file alla massima dimensione possibile (senza subsample).
	 */
	public static Bitmap load(String path) {
		return load(path, 0, 0);
	}

	/** 
	 * Carica un'immagine dalle risorse alla massima dimensione possibile (senza subsample).
	 */
	public static Bitmap load(Resources res, int resId) {
		return load(res, resId, 0, 0);
	}

	/** 
	 * Carica un'immagine da file. Se le dimensioni del contenitore non sono date,
	 * l'immagine verà caricata alla massima dimensione possibile (senza subsample).
	 * @param destW Larghezza del contenitore dell'immagine, 0 se non si conosce
	 * @param destH Altezza del contenitore dell'immagine, 0 se non si conosce
	 */
	public static Bitmap load(String path, int destW, int destH) {
		final int[] size = getSize(path);
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inScaled = false;
		options.inSampleSize = calculateSampleSize(size[0], size[1], destW, destH);
		if (LOG) Log.i(TAG, String.format("Decoding from file, original size is (%d %d), output is downsampled by %d to fit (%d %d)", size[0], size[1], options.inSampleSize, destW, destH));
		return BitmapFactory.decodeFile(path, options);
	}

	/** 
	 * Carica un'immagine dalle risorse. Se le dimensioni del contenitore non sono date,
	 * l'immagine verà caricata alla massima dimensione possibile (senza subsample).
	 * @param destW Larghezza del contenitore dell'immagine, 0 se non si conosce
	 * @param destH Altezza del contenitore dell'immagine, 0 se non si conosce
	 */
	public static Bitmap load(Resources res, int resId, int destW, int destH) {
		final int[] size = getSize(res, resId);
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inScaled = false;
		options.inSampleSize = calculateSampleSize(size[0], size[1], destW, destH);
		if (LOG) Log.i(TAG, String.format("Decoding from resources, original size is (%d %d), output is downsampled by %d to fit (%d %d)", size[0], size[1], options.inSampleSize, destW, destH));
		return BitmapFactory.decodeResource(res, resId, options);
	}

	/** 
	 * Carica un'immagine dagli assets. Se le dimensioni del contenitore non sono date,
	 * l'immagine verà caricata alla massima dimensione possibile (senza subsample).
	 * @param path Path all'interno degli assets
	 * @param destW Larghezza del contenitore dell'immagine, 0 se non si conosce
	 * @param destH Altezza del contenitore dell'immagine, 0 se non si conosce
	 */
	public static Bitmap load(AssetManager assets, String path, int destW, int destH) {
		final int[] size = getSize(assets, path);
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inScaled = false;
		options.inSampleSize = calculateSampleSize(size[0], size[1], destW, destH);
		if (LOG) Log.i(TAG, String.format("Decoding from assets, original size is (%d %d), output is downsampled by %d to fit (%d %d)", size[0], size[1], options.inSampleSize, destW, destH));
		try {
			final InputStream is = assets.open(path);
			final Bitmap ret = BitmapFactory.decodeStream(is, null, options);
			is.close();
			return ret;
		} catch (IOException _) {
			return null;
		}
	}

	/**
	 * Ritorna le dimensioni (larghezza e altezza) di un'immagine da file
	 */
	public static int[] getSize(String path) {
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(path, options);
		return new int[] { options.outWidth, options.outHeight };
	}

	/**
	 * Ritorna le dimensioni (larghezza e altezza) di un'immagine dalle risorse
	 */
	public static int[] getSize(Resources res, int resId) {
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeResource(res, resId, options);
		return new int[] { options.outWidth, options.outHeight };
	}

	/**
	 * Ritorna le dimensioni (larghezza e altezza) di un'immagine dagli assets
	 */
	public static int[] getSize(AssetManager assets, String path) {
		try {
			final BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			final InputStream is = assets.open(path);
			BitmapFactory.decodeStream(is, null, options);
			is.close();
			return new int[] { options.outWidth, options.outHeight };
		} catch (IOException _) {
			return new int[] { 0, 0 };
		}
	}
	
	/** 
	 * Calcola il migliore sample size (in potenze di 2) per caricare una immagine grande
	 * (di dimensioni sourceWH) in un contenitore piccolo (di dimensioni destWH). Una delle 
	 * dimensioni del contenitore può essere passata 0, verrà calcolata in base al caso peggiore. 
	 */
	private static int calculateSampleSize(final int sourceW, final int sourceH, int destW, int destH) {
		int inSampleSize = 1;
		
		// Se una delle dimensioni di destinazione non è data, calcolala
		if (destW <= 0 && destH <= 0) return inSampleSize;
		if (destW <= 0) destW = Math.round(destH * ((float)sourceW / (float)sourceH));
		if (destH <= 0) destH = Math.round(destW / ((float)sourceW / (float)sourceH));
		
		// Se l'immagine sorgente è più grande del contenitore
		if (sourceW > destW || sourceH > destH) {
			
			// Calculate ratios of height and width to requested height and width
			final int ratioW = (int)Math.floor((float)sourceW / (float)destW);
			final int ratioH = (int)Math.floor((float)sourceH / (float)destH);
			
			// Choose the smallest ratio as inSampleSize value, this will guarantee a final 
			// image with both dimensions larger than or equal to the requested height and width.
			inSampleSize = ratioW < ratioH ? ratioW : ratioH;
			
		}
		
		return inSampleSize < 1 ? 1 : inSampleSize;
		
	}
	
}
