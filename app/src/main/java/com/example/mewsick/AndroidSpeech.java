package com.example.mewsick;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.speech.RecognizerIntent;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mewsick.Middleware.Morceau;
import com.example.mewsick.Middleware.StreamingPrx;
import com.jackandphantom.blurimage.BlurImage;
import com.zeroc.Ice.Communicator;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import wseemann.media.FFmpegMediaMetadataRetriever;

import static com.zeroc.Ice.Util.initialize;

public class AndroidSpeech extends Activity
{
	private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;
	private boolean enEcoute = false, paused = false;

	private TextView artisteAlbum, morceau, tempsActuel, tempsTotal, pistes;
	private ImageView albumArt, enregistrer;
	private SeekBar progress;

	private int duree, dureePassee;
	private String titre, artiste, album;
	private Bitmap image;

	private AudioManager audioManager = null;
	private MediaPlayer mp = null;
	private FFmpegMediaMetadataRetriever mmr = null;
	private Handler durationHandler = null;

	private StreamingPrx streamer = null;
	private String ip = "192.168.0.5";
	private String port = "12345";
	private Communicator communicator = null;

	private Morceau[] playlist = null;
	private int index = 0, indexMax = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_lecteur);

		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy);

		connexion(ip, port);

		if (streamer == null)
			Toast.makeText(getApplicationContext(),"Le serveur de données ne répond pas !", Toast.LENGTH_LONG).show();

		if (httpPostRequest("http://192.168.0.5:8080/server.php", "").equals(""))
			Toast.makeText(getApplicationContext(),"Le serveur d'analyse ne répond pas !", Toast.LENGTH_LONG).show();

		durationHandler = new Handler();

		verifierPermissions();
		lierInterface();
		initMedia();
		chargerPlaylist(streamer.getMorceaux());
		pause();

		enregistrer.setOnClickListener(v ->
		{
			if (!enEcoute)
				ecouter();
			else
				stop();
		});
	}

	private void connexion(String ip, String port)
	{
		try
		{
			communicator = initialize();
			communicator.getProperties().setProperty("Ice.Package.Middleware", "com.example.mewsick");
			communicator.getProperties().setProperty("Ice.Default.Package", "com.example.mewsick");
			com.zeroc.Ice.ObjectPrx base = communicator.stringToProxy("ServeurStreaming:default -h " + ip + " -p " + port);
			streamer = StreamingPrx.checkedCast(base);

			if (streamer == null)
				throw new Error("Invalid proxy");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
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
		pistes = findViewById(R.id.pistes);
	}

	private void initMedia()
	{
		audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		mmr = new FFmpegMediaMetadataRetriever();
	}

	private void changerCouleurs()
	{
		List<int[]> result = new ArrayList<>();
		try
		{
			result = MMCQ.compute(image, 2);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		int[] dominant = result.get(0);
		int[] accentuation = result.get(1);

		Window window = this.getWindow();
		window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
		window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
		window.setStatusBarColor(Color.rgb(accentuation[0], accentuation[1], accentuation[2]));

		progress.getProgressDrawable().setColorFilter(Color.rgb(dominant[0], dominant[1], dominant[2]), PorterDuff.Mode.MULTIPLY);
		progress.getThumb().setColorFilter(Color.rgb(dominant[0], dominant[1], dominant[2]), PorterDuff.Mode.SRC_IN);
	}

	private void charger(int num)
	{
		if (mp != null)
		{
			mp.reset();
			mp.release();
			mp = null;
		}

		mmr.setDataSource(playlist[num].localisation);

		titre = mmr.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_TITLE);
		artiste = mmr.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_ARTIST).toUpperCase();
		album = mmr.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_ALBUM).toUpperCase();

		byte[] data = mmr.getEmbeddedPicture();
		image = BitmapFactory.decodeByteArray(data, 0, data.length);

		artisteAlbum.setText(String.format("%s - %s", artiste, album));
		morceau.setText(titre);
		albumArt.setImageBitmap(image);

		changerCouleurs();
		blurBG();

		try
		{
			mp = new MediaPlayer();
			mp.setDataSource(playlist[num].localisation);
			mp.prepare();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		duree = mp.getDuration();
		tempsTotal.setText(String.format("%01d:%02d", (mp.getDuration() / 1000) / 60, (mp.getDuration() / 1000) % 60));
		progress.setMax(mp.getDuration());
		progress.setClickable(false);
		pistes.setText(String.format("%d/%d", index + 1, indexMax));

		mp.start();

		mp.setOnCompletionListener(mp ->
		{
			next();
			jouer();
		});
	}

	private void chargerPlaylist(Morceau [] m)
	{
		playlist = null;
		playlist = m;
		index = 0;
		indexMax = playlist.length;

		charger(index);
	}

	private void blurBG()
	{
		BlurImage.with(getApplicationContext())
				 .load(image)
				 .intensity(20)
				 .Async(true)
				 .into(findViewById(R.id.backgroundImg));
	}

	private void stop()
	{
		enregistrer.setAlpha(1f);
		enEcoute = false;
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

		Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
		intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Je vous écoute...");

		try
		{
			startActivityForResult(intent, 666);
		}
		catch (ActivityNotFoundException a)
		{
			Toast.makeText(getApplicationContext(),"Oups il manque un truc !", Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		stop();

		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == 666 && resultCode == RESULT_OK && null != data)
		{
			ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

			String commande = result.get(0);

			String rep = httpPostRequest("http://" + ip + ":8080/server.php", commande);

			JSONParser parser = new JSONParser();
			JSONObject json;

			try
			{
				json = (JSONObject) parser.parse(rep);

				faireAction(json);
			}
			catch (ParseException e)
			{
				e.printStackTrace();
			}
		}
		else if (paused)
		{
			jouer();
			paused = false;
		}
	}

	private void jouer()
	{
		paused = false;
		mp.start();
		progress.setProgress(mp.getCurrentPosition());
		durationHandler.postDelayed(updateSeekBarTime, 100);
	}

	private void jouer(JSONObject jo)
	{
		String artiste = "", album = "", morceau = "";

		if (jo.get("artiste") != null)
			artiste = (String) jo.get("artiste");
		if (jo.get("album") != null)
			album = (String) jo.get("album");
		if (jo.get("morceau") != null)
			morceau = (String) jo.get("morceau");

		Morceau[] m = streamer.rechercher(morceau, artiste, album);

		if (m.length > 0)
		{
			chargerPlaylist(m);
			mp.start();
			progress.setProgress(mp.getCurrentPosition());
			durationHandler.postDelayed(updateSeekBarTime, 100);
		}
		else
			Toast.makeText(getApplicationContext(),"Je ne trouve pas ce morceau !", Toast.LENGTH_LONG).show();
	}

	private void pause()
	{
		mp.pause();
	}

	private void next()
	{
		if (index + 1 < indexMax)
			index++;
		else
			index = 0;

		charger(index);
	}

	private void pred()
	{
		if (index - 1 >= 0)
			index--;
		else
			index = indexMax - 1;

		charger(index);
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

	private void partyMode()
	{
		audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
	}

	private void bouger(int f)
	{
		int temps = f * 1000;

		if (mp.getCurrentPosition() + temps <= duree && mp.getCurrentPosition() + temps > 0)
			mp.seekTo(mp.getCurrentPosition() + temps);
		else if (mp.getCurrentPosition() + temps > duree)
			mp.seekTo(duree);
		else if (mp.getCurrentPosition() + temps <= 0)
			mp.seekTo(0);
	}

	private void bougerA(int f)
	{
		int temps = Math.abs(f * 1000);

		if (temps <= duree && temps >= 0)
			mp.seekTo(temps);
		else if (temps > duree)
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

	/* https://stackoverflow.com/questions/38408121/how-to-send-http-post-request-from-android */
	public static String httpPostRequest(String url, String cmd)
	{
		String response = "";
		BufferedReader reader = null;
		HttpURLConnection conn = null;

		try
		{
			URL urlObj = new URL(url);

			conn = (HttpURLConnection) urlObj.openConnection();
			conn.setRequestMethod("POST");
			conn.setDoOutput(true);
			OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());

			String data = "&" + URLEncoder.encode("cmd", "UTF-8") + "=" + URLEncoder.encode(cmd, "UTF-8");

			wr.write(data);
			wr.flush();

			reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			StringBuilder sb = new StringBuilder();
			String line;

			while ((line = reader.readLine()) != null)
			{
				sb.append(line).append("\n");
			}

			response = sb.toString();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				reader.close();

				if (conn != null)
					conn.disconnect();
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}

		return response;
	}

	private void faireAction(JSONObject json)
	{
		String action;

		if ((action = (String) json.get("cmd")) != null)
		{
			switch (action)
			{
				case "stop":
					break;

				case "play":
					if (json.get("arg") != null)
						jouer((JSONObject) json.get("arg"));
					else
						jouer();

					break;

				case "next":
					next();
					jouer();
					break;

				case "pred":
					pred();
					jouer();
					break;

				case "move":
					if (json.get("arg") != null)
						bouger(((Long) json.get("arg")).intValue());

					if (paused)
						jouer();

					break;

				case "moveto":
					if (json.get("arg") != null)
						bougerA(((Long) json.get("arg")).intValue());

					if (paused)
						jouer();

					break;

				case "vol":
					if (Objects.equals(json.get("arg"), "+"))
						monterVolume();
					else if (Objects.equals(json.get("arg"), "-"))
						baisserVolume();
					else if (Objects.equals(json.get("arg"), "++"))
						partyMode();

					if (paused)
						jouer();

					break;

				default:
					Toast.makeText(getApplicationContext(),"Désolé je n'ai pas compris", Toast.LENGTH_SHORT).show();

					if (paused)
						jouer();

					break;
			}
		}
	}
}