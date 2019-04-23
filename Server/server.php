<?php

if (isset($_POST['cmd']))
{
	$commande = $_POST['cmd'];
	$rep = array();
	$coupe = explode(' ', trim($commande));

	error_log($commande);

	//*************//
	//*** PAUSE ***//
	//*************//
	if (strpos($coupe[0], "pause") !== false OR
		strpos($coupe[0], "stop") !== false OR 
			(count($coupe) == 3 AND 
			 strpos($coupe[0], "met") !== false AND
			 strpos($coupe[1], "en") !== false AND
			 strpos($coupe[2], "pause") !== false))
	{
		$rep['cmd'] = "stop";
	}
	//************//
	//*** PLAY ***//
	//************//
	else if (strpos($coupe[0], "joue") !== false OR
			 strpos($coupe[0], "reprendre") !== false OR
			 strpos($coupe[0], "écoute") !== false)
	{
		$rep['cmd'] = "play";

		if (count($coupe) > 1 AND
				(strpos($coupe[0], "joue") !== false OR
				strpos($coupe[0], "écoute") !== false))
		{
			if (count($coupe) > 3 AND
			  ((strpos($coupe[1], "le") !== false AND
				strpos($coupe[2], "morceau") !== false) OR
			   (strpos($coupe[1], "la") !== false AND
				strpos($coupe[2], "chanson") !== false)))
			{
				$rep['arg']['artiste'] = "";
				$rep['arg']['album'] = "";
				$rep['arg']['morceau'] = getString($coupe, 2);
			}
			else if (count($coupe) > 2 AND
					 strpos($coupe[1], "l'album") !== false OR
					 strpos($coupe[1], "lalbum") !== false)
			{
				$rep['arg']['artiste'] = "";
				$rep['arg']['album'] = getString($coupe, 1);
				$rep['arg']['morceau'] = "";
			}
			else if (count($coupe) > 2 AND
					 strpos($coupe[1], "l'artiste") !== false OR
					 strpos($coupe[1], "lartiste") !== false)
			{
				$rep['arg']['artiste'] = getString($coupe, 1);
				$rep['arg']['album'] = "";
				$rep['arg']['morceau'] = "";
			}
			else
			{
				$rep['arg'] = analyser($coupe);
			}
		}
	}
	//************//
	//*** NEXT ***//
	//************//
	else if (strpos($commande, "suivant") !== false)
	{
		if (count($coupe) == 1 AND
			strpos($coupe[0], "suivant") !== false)
		{
			$rep['cmd'] = "next";
		}
		else if (count($coupe) == 2 AND
				 strpos($coupe[1], "suivant") !== false AND
				 	(strpos($coupe[0], "morceau") !== false OR
				 	 strpos($coupe[0], "chanson") !== false))
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
	else if (strpos($commande, "précédent") !== false)
	{
		if (count($coupe) == 1 AND
			strpos($coupe[0], "précédent") !== false)
		{
			$rep['cmd'] = "pred";
		}
		else if (count($coupe) == 2 AND
				 strpos($coupe[1], "précédent") !== false AND
				 	(strpos($coupe[0], "morceau") !== false OR
				 	 strpos($coupe[0], "chanson") !== false))
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
	else if (strpos($coupe[0], "avancer") !== false OR
			 strpos($coupe[0], "revenir") !== false OR
			 strpos($coupe[0], "reculer") !== false OR
			 strpos($coupe[0], "aller") !== false)
	{
		$continuer = true;

		if (count($coupe) == 1)
		{
			if (strpos($coupe[0], "avancer") !== false)
			{
				$rep['cmd'] = "move";
				$rep['arg'] = 10;
			}
			else if (strpos($coupe[0], "revenir") !== false OR
					 strpos($coupe[0], "reculer") !== false)
			{
				$rep['cmd'] = "move";
				$rep['arg'] = -10;
			}
		}
		else if (count($coupe) >= 3)
		{
			if (strpos($coupe[1], "à") !== false)
			{
				$rep['cmd'] = "moveto";
			}
			else if (strpos($coupe[1], "de") !== false)
			{
				$rep['cmd'] = "move";
			}
			else if (strpos($coupe[1], "au") !== false AND
					 strpos($coupe[2], "début") !== false)
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
				$rep['arg'] = toDuree($coupe);
			}

				if ($rep['cmd'] == "move" AND 
						(strpos($coupe[0], "revenir") !== false OR
						 strpos($coupe[0], "reculer") !== false))
					$rep['arg'] = - $rep['arg'];
		}
	}
	//**************//
	//*** VOLUME ***//
	//**************//
	else if (strpos($commande, "volume") !== false OR
			 strpos($commande, "son") !== false)
	{
		$rep['cmd'] = "vol";

		if (strpos($coupe[0], "monte") !== false AND
			strpos($coupe[1], "le") !== false)
		{
			$rep['arg'] = "+";
		}
		else if (strpos($coupe[0], "baisse") !== false AND
				 strpos($coupe[1], "le") !== false)
		{
			$rep['arg'] = "-";
		}
		else
		{
			$rep['arg'] = "null";
		}
	}
	else if (strpos($commande, "fête") !== false)
	{
		$rep['cmd'] = "vol";
		$rep['arg'] = "++";
	}
	else
		$rep['cmd'] = "null";

	echo json_encode($rep);
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
			if (strpos($tab[1], "minute") !== false)
				$mult = 60;
			else if (strpos($tab[1], "seconde") !== false)
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

?>