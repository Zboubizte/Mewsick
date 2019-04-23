<?php

if (isset($_POST['cmd']))
{
	$commande = $_POST['cmd'];
	$rep = array();
	$tab = explode(' ', trim($commande));

	error_log($commande);

	//*************//
	//*** PAUSE ***//
	//*************//
	if (comp($tab[0], "pause") OR
		comp($tab[0], "stop") OR 
			(count($tab) == 3 AND 
			 comp($tab[0], "met") AND
			 comp($tab[1], "en") AND
			 comp($tab[2], "pause")))
	{
		$rep['cmd'] = "stop";
	}
	//************//
	//*** PLAY ***//
	//************//
	else if (comp($tab[0], "joue") OR
			 comp($tab[0], "reprendre") OR
			 comp($tab[0], "écoute"))
	{
		$rep['cmd'] = "play";

		if (count($tab) > 1 AND
				(comp($tab[0], "joue") OR
				comp($tab[0], "écoute")))
		{
			if (count($tab) > 3 AND
			  ((comp($tab[1], "le") AND
				comp($tab[2], "morceau")) OR
			   (comp($tab[1], "la") AND
				comp($tab[2], "chanson"))))
			{
				$rep['arg']['artiste'] = "";
				$rep['arg']['album'] = "";
				$rep['arg']['morceau'] = getString($tab, 2);
			}
			else if (count($tab) > 2 AND
					 comp($tab[1], "l'album") OR
					 comp($tab[1], "lalbum"))
			{
				$rep['arg'] = analyserAlbum($tab);
			}
			else if (count($tab) > 2 AND
					 comp($tab[1], "l'artiste") OR
					 comp($tab[1], "lartiste"))
			{
				$rep['arg']['artiste'] = getString($tab, 1);
				$rep['arg']['album'] = "";
				$rep['arg']['morceau'] = "";
			}
			else
			{
				$rep['arg'] = analyser($tab);
			}
		}
	}
	//************//
	//*** NEXT ***//
	//************//
	else if (comp($commande, "suivant"))
	{
		if (count($tab) == 1 AND
			comp($tab[0], "suivant"))
		{
			$rep['cmd'] = "next";
		}
		else if (count($tab) == 2 AND
				 comp($tab[1], "suivant") AND
				 	(comp($tab[0], "morceau") OR
				 	 comp($tab[0], "chanson")))
		{
			$rep['cmd'] = "next";
		}
		else
		{
			$rep['cmd'] = "null";
		}
	}
	//************//
	//*** PRED ***//
	//************//
	else if (comp($commande, "précédent"))
	{
		if (count($tab) == 1 AND
			comp($tab[0], "précédent"))
		{
			$rep['cmd'] = "pred";
		}
		else if (count($tab) == 2 AND
				 comp($tab[1], "précédent") AND
				 	(comp($tab[0], "morceau") OR
				 	 comp($tab[0], "chanson")))
		{
			$rep['cmd'] = "pred";
		}
		else
		{
			$rep['cmd'] = "null";
		}
	}
	//************//
	//*** MOVE ***//
	//************//
	else if (comp($tab[0], "avancer") OR
			 comp($tab[0], "revenir") OR
			 comp($tab[0], "reculer") OR
			 comp($tab[0], "aller"))
	{
		$continuer = true;

		if (count($tab) == 1)
		{
			if (comp($tab[0], "avancer"))
			{
				$rep['cmd'] = "move";
				$rep['arg'] = 10;
			}
			else if (comp($tab[0], "revenir") OR
					 comp($tab[0], "reculer"))
			{
				$rep['cmd'] = "move";
				$rep['arg'] = -10;
			}
		}
		else if (count($tab) >= 3)
		{
			if (comp($tab[1], "à"))
			{
				$rep['cmd'] = "moveto";
			}
			else if (comp($tab[1], "de"))
			{
				$rep['cmd'] = "move";
			}
			else if (comp($tab[1], "au") AND
					 comp($tab[2], "début"))
			{
				$rep['cmd'] = "moveto";
				$rep['arg'] = 0;
				$continuer = false;
			}
			else
			{
				$rep['cmd'] = "null";
			}

			if ($continuer == true)
			{
				$rep['arg'] = toDuree($tab);
			}

				if ($rep['cmd'] == "move" AND 
						(comp($tab[0], "revenir") OR
						 comp($tab[0], "reculer")))
					$rep['arg'] = - $rep['arg'];
		}
	}
	//**************//
	//*** VOLUME ***//
	//**************//
	else if (comp($commande, "volume") OR
			 comp($commande, "son"))
	{
		$rep['cmd'] = "vol";

		if (comp($tab[0], "monte") AND
			comp($tab[1], "le"))
		{
			$rep['arg'] = "+";
		}
		else if (comp($tab[0], "baisse") AND
				 comp($tab[1], "le"))
		{
			$rep['arg'] = "-";
		}
		else
		{
			$rep['arg'] = "null";
		}
	}
	else if (comp($commande, "fête"))
	{
		$rep['cmd'] = "vol";
		$rep['arg'] = "++";
	}
	else
		$rep['cmd'] = "null";

	echo json_encode($rep);
}

function analyserAlbum($tab)
{
	array_shift($tab);
	array_shift($tab);
	
	$rep = array();

	$decount = 0;

	for ($i = 0; $i < count($tab); $i++)
	{
		if ($tab[$i] == "de")
		{
			if ($i + 1 < count($tab) AND $tab[$i + 1] == "l'artiste")
			{
				$tab[$i] = "dlartiste";
				unset($tab[$i + 1]);
				$tab = array_values($tab);
				$decount++;
			}

			if ($tab[$i] == "de")
			{
				if ($decount == 0)
					$tab[$i] = "dlartiste";
				else if ($decount == 1)
					$tab[$i] = "dlalbum";

				$decount++;
			}
		}
	}

	$type = 0;

	$rep['morceau'] = "";
	$rep['artiste'] = "";
	$rep['album'] = "";

	for ($i = 0; $i < count($tab); $i++)
	{
		if ($tab[$i] == "dlartiste")
		{
			$type = 1;
			$i++;
		}

		if ($type == 0)
		{
			if ($rep['album'] != "")
				$rep['album'] .= " ";

			$rep['album'] .= $tab[$i];
		}
		else if ($type == 1)
		{
			if ($rep['artiste'] != "")
				$rep['artiste'] .= " ";

			$rep['artiste'] .= $tab[$i];
		}
	}

	return $rep;
}

function analyser($tab)
{
	array_shift($tab);
	$rep = array();

	$decount = 0;

	for ($i = 0; $i < count($tab); $i++)
	{
		if ($tab[$i] == "de")
		{
			if ($i + 1 < count($tab) AND $tab[$i + 1] == "l'artiste")
			{
				$tab[$i] = "dlartiste";
				unset($tab[$i + 1]);
				$tab = array_values($tab);
				$decount++;
			}

			if ($i + 1 < count($tab) AND $tab[$i + 1] == "l'album")
			{
				$tab[$i] = "dlalbum";
				unset($tab[$i + 1]);
				$tab = array_values($tab);
				$decount++;
			}

			if ($tab[$i] == "de")
			{
				if ($decount == 0)
					$tab[$i] = "dlartiste";
				else if ($decount == 1)
					$tab[$i] = "dlalbum";

				$decount++;
			}
		}
	}

	$type = 0;

	$rep['morceau'] = "";
	$rep['artiste'] = "";
	$rep['album'] = "";

	for ($i = 0; $i < count($tab); $i++)
	{
		if ($tab[$i] == "dlartiste")
		{
			$type = 1;
			$i++;
		}
		else if ($tab[$i] == "dlalbum")
		{
			$type = 2;
			$i++;
		}

		if ($type == 0)
		{
			if ($rep['morceau'] != "")
				$rep['morceau'] .= " ";

			$rep['morceau'] .= $tab[$i];
		}
		else if ($type == 1)
		{
			if ($rep['artiste'] != "")
				$rep['artiste'] .= " ";

			$rep['artiste'] .= $tab[$i];
		}
		else if ($type == 2)
		{
			if ($rep['album'] != "")
				$rep['album'] .= " ";

			$rep['album'] .= $tab[$i];
		}
	}

	return $rep;
}

function getString($tab, $it)
{
	$rep = "";

	for ($i = 0; $i < $it + 1; $i++)
		array_shift($tab);

	for ($i = 0; $i < count($tab); $i++)
	{
		if ($rep != "")
				$rep .= " ";

			$rep .= $tab[$i];
	}

	return $rep;
}

function toDuree($tab)
{
	$val = 0;
	$mult = 0;

	array_shift($tab);
	array_shift($tab);

	for ($i = 0; $i < count($tab); $i++)
		if ($tab[$i] == "et")
			unset($tab[$i]);

	$tab = array_values($tab);

	if (count($tab) >= 1)
	{
		if ($tab[0] == "une") $tab[0] = 1;

		$val = intval($tab[0]);

		if (count($tab) >= 2)
		{
			if (comp($tab[1], "minute"))
				$mult = 60;
			else if (comp($tab[1], "seconde"))
				$mult = 1;

			$val *= $mult;

			if (count($tab) >= 3)
			{
				$val += intval($tab[2]);
			}
		}
	}

	return $val;
}

function comp($a, $b)
{
	return strpos($a, $b) !== false;
}

?>