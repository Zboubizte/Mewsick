package com.example.mewsick;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
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
	private boolean enEcoute = false, paused = false;

	private TextView artisteAlbum, morceau, tempsActuel, tempsTotal, texteCommande;
	private ImageView albumArt, enregistrer;
	private SeekBar progress;

	private int morceauActuel, duree, dureePassee;
	private String titre, artiste, album;
	private Bitmap image;

	private AudioManager audioManager = null;
	private MediaPlayer mp = null;
	private MediaMetadataRetriever mmr = null;
	private Handler durationHandler = null;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		morceauActuel = R.raw.strawberry_girls_betelgeuse;
		durationHandler = new Handler();

		verifierPermissions();
		lierInterface();
		initMedia();
		charger(morceauActuel);

		enregistrer.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				if (!enEcoute)
					ecouter();
				else
					stop();
			}
		});

		new SetupTask(this).execute();
	}

	private void blurBG()
	{
		BlurImage.with(getApplicationContext())
				 .load(image)
				 .intensity(20)
				 .Async(true)
				 .into((ImageView) findViewById(R.id.backgroundImg));
	}

	private void verifierPermissions()
	{
		int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO);

		if (permissionCheck != PackageManager.PERMISSION_GRANTED)
			ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
	}

	private void lierInterface()
	{
		artisteAlbum = findViewById(R.id.artisteAlbum);
		morceau = findViewById(R.id.morceau);
		tempsActuel = findViewById(R.id.tempsActuel);
		tempsTotal = findViewById(R.id.tempsTotal);
		albumArt = findViewById(R.id.albumArt);
		enregistrer = findViewById(R.id.enregistrer);
		progress = findViewById(R.id.progress);
		texteCommande = findViewById(R.id.commande);
	}

	private void initMedia()
	{
		audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		mmr = new MediaMetadataRetriever();
	}

	private void charger(int fichier)
	{
		if (mp != null)
			mp.reset();

		final AssetFileDescriptor afd = getResources().openRawResourceFd(fichier);

		mp = MediaPlayer.create(this, fichier);
		mmr.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());

		titre = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
		artiste = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST).toUpperCase();
		album = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM).toUpperCase();
		duree = mp.getDuration();

		byte[] data = mmr.getEmbeddedPicture();
		image = BitmapFactory.decodeByteArray(data, 0, data.length);

		artisteAlbum.setText(String.format("%s - %s", artiste, album));
		morceau.setText(titre);
		albumArt.setImageBitmap(image);
		tempsTotal.setText(String.format("%01d:%02d", (mp.getDuration() / 1000) / 60, (mp.getDuration() / 1000) % 60));

		progress.setMax(mp.getDuration());
		progress.setClickable(false);

		blurBG();
	}

	private void jouer()
	{
		mp.start();
		progress.setProgress(mp.getCurrentPosition());
		durationHandler.postDelayed(updateSeekBarTime, 100);
	}

	private void pause()
	{
		mp.pause();
	}

	private void next()
	{
		if (morceauActuel == R.raw.strawberry_girls_betelgeuse)
			morceauActuel = R.raw.queen_dont_stop_me_now;
		else
			morceauActuel = R.raw.strawberry_girls_betelgeuse;

		charger(morceauActuel);
	}

	private void monterVolume()
	{
		audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND);
		audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND);
		audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND);
		audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND);
		audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND);
		audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND);
	}

	private void baisserVolume()
	{
		audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_PLAY_SOUND);
		audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_PLAY_SOUND);
		audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_PLAY_SOUND);
		audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_PLAY_SOUND);
		audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_PLAY_SOUND);
		audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_PLAY_SOUND);
	}

	private void avancer()
	{
		if (mp.getCurrentPosition() + 10000 <= duree)
			mp.seekTo(mp.getCurrentPosition() + 10000);
		else
			mp.seekTo(duree);
	}

	private Runnable updateSeekBarTime = new Runnable()
	{
		public void run()
		{
			dureePassee = mp.getCurrentPosition();
			progress.setProgress(dureePassee);
			tempsActuel.setText(String.format("%01d:%02d", (mp.getCurrentPosition() / 1000) / 60, (mp.getCurrentPosition() / 1000) % 60));

			durationHandler.postDelayed(this, 100);
		}
	};

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
					.setDictionary(new File(assetDir, "customdict.dict"))
					.setBoolean("-allphone_ci", true)
					.setFloat("-lw", 2.0)
					.setFloat("-beam", 1e-20)
					.setFloat("-pbeam", 1e-20)
					.setFloat("-vad_threshold", 3.0)
					.setBoolean("-remove_noise", true)
					.getRecognizer();

		recognizer.addListener(this);

		recognizer.addNgramSearch(NGRAM_SEARCH, new File(assetDir, "fr-small.lm.bin"));
	}

	private void ecouter()
	{
		if (mp.isPlaying())
		{
			pause();
			paused = true;
		}

		enregistrer.setAlpha(0.2f);
		enEcoute = true;
		recognizer.startListening(NGRAM_SEARCH, 1500);
	}

	private void stop()
	{
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

		if (paused)
		{
			jouer();
			paused = false;
		}
	}

	@Override public void onResult(Hypothesis hypothesis)
	{
		if (hypothesis != null)
		{
			String commande = hypothesis.getHypstr();

			if (commande.contains("pause") || commande.contains("stop"))
				texteCommande.setText("PAUSE");
			else if (commande.contains("jouer") || commande.contains("reprendre"))
			{
				jouer();
				texteCommande.setText("JOUER");
			}
			else if (commande.contains("suivant"))
			{
				next();
				jouer();
				texteCommande.setText("SUIVANT");
			}
			else if (commande.contains("avancer"))
			{
				avancer();

				if (paused)
					jouer();

				texteCommande.setText("AVANCER");
			}
			else if (commande.contains("volume") || commande.contains("son"))
			{
				if (commande.contains("monter"))
				{
					monterVolume();
					texteCommande.setText("VOLUME +");
				}
				else if (commande.contains("baisser"))
				{
					baisserVolume();
					texteCommande.setText("VOLUME +");
				}

				if (paused)
					jouer();
			}
			else
			{
				if (paused)
					jouer();
				texteCommande.setText(commande);
			}

			paused = false;
		}
	}
}
