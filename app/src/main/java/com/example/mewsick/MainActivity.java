package com.example.mewsick;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.Image;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.jackandphantom.blurimage.BlurImage;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;

import static edu.cmu.pocketsphinx.SpeechRecognizerSetup.defaultSetup;

public class MainActivity extends Activity implements RecognitionListener
{
	private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;

	private SpeechRecognizer recognizer;
	private String NGRAM_SEARCH = "salut";
	private boolean enEcoute = false;

	TextView artisteAlbum;
	TextView morceau;
	TextView tempsActuel;
	TextView tempsTotal;
	ImageView albumArt;
	ImageView enregistrer;
	SeekBar progress;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		artisteAlbum = findViewById(R.id.artisteAlbum);
		morceau = findViewById(R.id.morceau);
		tempsActuel = findViewById(R.id.tempsActuel);
		tempsTotal = findViewById(R.id.tempsTotal);
		albumArt = findViewById(R.id.albumArt);
		enregistrer = findViewById(R.id.enregistrer);
		progress = findViewById(R.id.progress);

		int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO);

		if (permissionCheck != PackageManager.PERMISSION_GRANTED)
		{
			ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
			return;
		}

		enregistrer.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				if (!enEcoute)
				{
					ecouter();
				}
				else
				{
					stop();
				}
			}
		});

		BlurImage.with(getApplicationContext())
				.load(R.drawable.placeholder)
				.intensity(100)
				.Async(true)
				.into((ImageView) findViewById(R.id.backgroundImg));

		new SetupTask(this).execute();
	}

	private static class SetupTask extends AsyncTask<Void, Void, Exception>
	{
		WeakReference<MainActivity> activityReference;

		SetupTask(MainActivity activity)
		{
			this.activityReference = new WeakReference<>(activity);
		}

		@Override
		protected Exception doInBackground(Void... params)
		{
			try
			{
				Assets assets = new Assets(activityReference.get());
				File assetDir = assets.syncAssets();
				activityReference.get().setupRecognizer(assetDir);
			}
			catch (IOException e)
			{
				return e;
			}

			return null;
		}

		@Override
		protected void onPostExecute(Exception result)
		{
			if (result != null)
				System.out.println(result.getMessage());
			else
				activityReference.get().stop();
		}
	}

	private void setupRecognizer(File assetDir) throws IOException
	{
		recognizer = defaultSetup()
					.setAcousticModel(new File(assetDir, "ptm"))
					.setDictionary(new File(assetDir, "fr.dict"))
					.getRecognizer();

		recognizer.addListener(this);

		recognizer.addNgramSearch(NGRAM_SEARCH, new File(assetDir, "fr-small.lm.bin"));
	}

	private void ecouter()
	{
		System.out.println("J'ECOUTE");
		enregistrer.setAlpha(0.2f);
		enEcoute = true;
		recognizer.startListening(NGRAM_SEARCH, 1500);
	}

	private void stop()
	{
		System.out.println("FIN");
		enregistrer.setAlpha(1f);
		enEcoute = false;
		recognizer.stop();
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull  int[] grantResults)
	{
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);

		if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO)
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
				new SetupTask(this).execute();
			else
				finish();
	}

	@Override public void onBeginningOfSpeech() { }
	@Override public void onEndOfSpeech() { }
	@Override public void onError(Exception e) { }
	@Override public void onPartialResult(Hypothesis hypothesis) { }

	@Override public void onTimeout()
	{
		stop();
	}

	@Override public void onResult(Hypothesis hypothesis)
	{
		if (hypothesis != null)
		{
			System.out.println(hypothesis.getHypstr());
			((TextView) findViewById(R.id.commande)).setText(hypothesis.getHypstr());
		}
	}
}
