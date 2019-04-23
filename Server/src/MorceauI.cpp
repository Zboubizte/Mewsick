#include "../include/MorceauI.hpp"

MorceauI::MorceauI() : Morceau() {}

MorceauI::MorceauI(int _piste, string _titre, string _artiste, string _album, string _localisation) : Morceau(_piste, _titre, _artiste, _album, _localisation) {}

string MorceauI::decrire()
{
	return artiste + " - " + titre + " (" + album + ")";
}

int MorceauI::getPiste()
{
	return piste;
}

string MorceauI::getTitre()
{
	return titre;
}

string MorceauI::getArtiste()
{
	return artiste;
}

string MorceauI::getAlbum()
{
	return album;
}

string MorceauI::getLocalisation()
{
	return localisation;
}