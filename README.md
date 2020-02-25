# Jjittai
Java jittai manager

Hello,
I love Aneko and have it installed since a couple of years ago.
Researching about it, I discovered that there existed the possibility of extending it, so I tried.
Unforunately, I couldn't work with Android Studio and there was almost no documentation of the process of making skins I could find.
Fast forward a couple of years, here I am with a little program to parse files and make little desktop pets, easy to make so that everyone can have (and make) it's own.

The program looks for .zip files in it's folder, looks for a JSON inside and then parses the images files it contains.

The JSON structure should be as follows:

```
{
	//mandatory properties
	"name":"Example"//cannot be empty
 	,"sprites":[//at least one
		{
			"file":"file.png" //mandatory
			,"x":0 //optional DEFAULT: 0
     			,"y":0 //optional DEFAULT: 0
     			,"width":10 //optional DEFAULT: the image width
			,"height":10 //optional DEFAULT: the image height
		}
	]
	,"idleCycle":[ //array of animation steps
		{
			"duration":0.1 //mandatory, in seconds
			,"sprite":0 //mandatory, index in sprites array
		}
	]
  
	//optional properties
	,"speed":10 //optional DEFAULT: 10pix/second
	,"canBeClicked":true //optional DEFAULT: true
	,"behaviour":1
	/*
		optional DEFAULT: 4
		1:totally idle, doesn't walk but displays events
		2:chase pointer
		3:whimsical, just like current Aneko, it starts walking in a random direction every click or keypress
		4:chill around, like totally idle, but ocasionally walks

		future implementation:
		annoy, it would prefer critical screen places to be
		more!
	*/
	,"walkingCycle":[ //array of arrays of animation steps
	//if its behaviour is not chill around, you need at least 4 cycles
		[
			{
				"duration":0.1
				,"sprite":0
			}
		]
	]
	,"events":[ //array of event objects
		{
			"name":"event1" //mandatory
			,"probability":50 //mandatory 100 based
			,"repetitionInfo":[1,5] //optional, DEFAULT: [1]
			//the event animation will repeat between index 0 and index 1 times, or just index 0 if ther's no index 1 or it's less than index 0
			,"animation":[ //mandatory array of animation steps
				{
					"duration":0.1
					,"sprite":0
				}
			]
		}
	]
}

```

## To be added
Possibility of playing sounds. (very soon)

Add events triggers. (not that soon)

A property to define how often events should happen. (as soon as I find a good name for it)

## Known Issues
The killing message may appear on the background.

canBeClicked:false only works on Windows.

The CHASE_CURSOR behaviour may miss the cursor by less than a pixel, so it starts "bouncing" back and forth to find it.

The CHASE_CURSOR behaviour is unpredictable if you play over the jittai.
