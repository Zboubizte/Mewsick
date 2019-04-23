#ifndef _MORCEAUI_HPP_
#define _MORCEAUI_HPP_

#include <Ice/Ice.h>
#include "Streaming.h"

using namespace std;
using namespace Middleware;

class MorceauI : public Morceau
{
	public:
		MorceauI();
		MorceauI(int, string, string, string, string);
		
		string decrire();
		int getPiste();
		string getTitre();
		string getArtiste();
		string getAlbum();
		string getLocalisation();
};

#endif