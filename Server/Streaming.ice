["java:package:com.example.mewsick"]
module Middleware
{
	class Morceau
	{
		int piste;
		string titre;
		string artiste;
		string album;
		string localisation;
	};

	sequence<Morceau> Morceaux;

	interface Streaming
	{
		bool ajouter(int piste, string titre, string artiste, string album, string localisation);
		bool supprimer(string titre, string artiste);
		Morceaux rechercher(string titre, string artiste, string album);
		Morceaux getMorceaux();
	};
};