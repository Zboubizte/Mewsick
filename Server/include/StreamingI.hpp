#ifndef _STREAMINGI_HPP_
#define _STREAMINGI_HPP_

#include <iostream>
#include <algorithm>
#include <dirent.h>
#include "Streaming.h"
#include "MorceauI.hpp"

using namespace std;
using namespace Middleware;

class StreamingI : public Streaming
{
	public:
		StreamingI();
		StreamingI(string _ip, string _port);
		virtual bool ajouter(int piste, string titre, string artiste, string album, string localisation, const ::Ice::Current&);
		virtual bool supprimer(string titre, string artiste, const ::Ice::Current&);
		virtual Morceaux rechercher(string titre, string artiste, string album, const ::Ice::Current&);
		virtual Morceaux getMorceaux(const ::Ice::Current&);

	private:
		string ip, portserv;
		Morceaux morceaux;

		void chargerMorceaux();
		bool morceauExiste(string titre);
		MorceauI * getMorceauI(Morceau * m);
		bool comparer(string, string);
		virtual bool ajouter(int piste, string titre, string artiste, string album, string localisation);
		void remplacer(string &, string, string);
};

#endif