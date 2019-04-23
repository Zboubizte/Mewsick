#include "../include/StreamingI.hpp"

StreamingI::StreamingI() : Streaming(), ip("")
{
	chargerMorceaux();
}

StreamingI::StreamingI(string _ip, string _port) : Streaming(), ip(_ip), portserv(_port)
{
	chargerMorceaux();
}

bool StreamingI::ajouter(int piste, string titre, string artiste, string album, string localisation, const ::Ice::Current&)
{
	return ajouter(piste, titre, artiste, album, localisation);
}

bool StreamingI::ajouter(int piste, string titre, string artiste, string album, string localisation)
{
	MorceauI * m = new MorceauI(piste, titre, artiste, album, localisation);

		if (m != nullptr)
		{
			cout << endl << "Ajout de : " << m -> decrire() << endl;

			morceaux.emplace_back(m);

			return true;
		}
		else
			return false;
}

bool StreamingI::supprimer(string titre, string artiste, const ::Ice::Current&)
{
	Morceaux::iterator it;

	if (!(titre == "" || artiste == ""))
		for (it = morceaux.begin(); it != morceaux.end(); ++it)
		{
			string _titre = it -> get() -> titre,
				   _artiste = it -> get() -> artiste;

			if (comparer(_titre, titre) && comparer(_artiste, artiste))
			{
				cout << endl << "Suppression de " <<  getMorceauI(it -> get()) -> decrire() << endl;
				morceaux.erase(it);
				return true;
			}
		}

	return false;
}

Morceaux StreamingI::rechercher(string titre, string artiste, string album, const ::Ice::Current&)
{
	cout << endl << "Recherche de [Artiste: " << artiste << "][Titre: " << titre << "][Album: " << album << "]" << endl;
	Morceaux m;
	Morceaux::iterator it;
	
	for (it = morceaux.begin(); it != morceaux.end(); ++it)
	{
		string _titre = it -> get() -> titre,
			   _artiste = it -> get() -> artiste,
			   _album = it -> get() -> album;

		if (comparer(_titre, titre) && comparer(_artiste, artiste) && comparer(_album, album))
		{
			m.push_back(*it);
			cout << "  Ajout à la recherche de " << getMorceauI(it -> get()) -> decrire() << endl;
		}
	}

	return m;
}

Morceaux StreamingI::getMorceaux(const ::Ice::Current&)
{
	return morceaux;
}

MorceauI * StreamingI::getMorceauI(Morceau * m)
{
	return dynamic_cast<MorceauI *>(m);
}

bool StreamingI::comparer(string fichier, string recherche)
{
	transform(fichier.begin(), fichier.end(), fichier.begin(), ::tolower);
	transform(recherche.begin(), recherche.end(), recherche.begin(), ::tolower);

	return fichier.find(recherche) != string::npos;
}

void StreamingI::chargerMorceaux()
{
	struct dirent * lecture;
	DIR * rep;

	rep = opendir("tracks");

	while (lecture = readdir(rep))
	{
		string nom = string(lecture -> d_name), 
			   token;

		if (nom.compare(".") != 0 && nom.compare("..") != 0)
		{
			string * res = new string[4];

			// { [artiste], [album], [piste], [localisation] }
			// nom = titre du morceau

			res[3] = "http://192.168.0.5:8080/tracks/" + nom;

			remplacer(res[3], " ", "%20");

			nom = nom.substr(0, nom.size() - 4);

			size_t pos = 0;
			int i = 0;

			while ((pos = nom.find(" --- ")) != std::string::npos)
			{
				token = nom.substr(0, pos);
				res[i++] = token;
				nom.erase(0, pos + 5);
			}

			ajouter(stoi(res[2]), nom, res[0], res[1], res[3]);
		}
	}
}

void StreamingI::remplacer(string & s, string search, string replace)
{
	for (size_t pos = 0; ; pos += replace.length())
	{
		pos = s.find(search, pos);
		
		if (pos == string::npos)
			break;

		s.erase(pos, search.length());
		s.insert(pos, replace);
	}
}