package com.example.mewsick;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class Accueil extends AppCompatActivity
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_accueil);

		findViewById(R.id.Sphinx).setOnClickListener(v ->
		{
			Intent intent = new Intent(Accueil.this, PocketSphinx.class);
			startActivity(intent);
		});

		findViewById(R.id.Android).setOnClickListener((View.OnClickListener) v ->
		{
			Intent intent = new Intent(Accueil.this, AndroidSpeech.class);
			startActivity(intent);
		});
	}
}
