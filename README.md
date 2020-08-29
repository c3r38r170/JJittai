# JJittai
Java jittai manager

Hello,
I love ANeko and have it installed since a couple of years ago.
Researching about it, I discovered that there existed the possibility of extending it, so I tried.
Unforunately, I couldn't work with Android Studio and there was almost no documentation of the process of making skins I could find.
Fast forward a couple of years, here I am with a little program to parse files and make little desktop pets, easy to make so that everyone can have (and make) it's own.

The program looks for .zip files in it's folder, looks for a JSON inside and then parses the images files it contains.

The JSON structure should be as follows:

```
{
	//mandatory properties
	
	"name":"Example"
	,"sprites":[//at least one
		//sprite example
		{
			"file":"file.png" //mandatory
			,"x":0 //optional, DEFAULT: 0
			,"y":0 //optional, DEFAULT: 0
			,"width":10 //optional, DEFAULT: the image width
			,"height":10 //optional, DEFAULT: the image height
		}
	]
	,"idleCycle":[ //array of animation elements
		//animation step example:
		{
			"sprite":0 //mandatory, index in sprites array
			,"duration":0.1 //optional, DEFAULT: 0.001 in seconds, cannot be less
			,"sound":0 //optional, DEFAULT: -1 sound to play at this step, just .wav was tested and works, mp3 still don't
		}
	]

	//optional properties
	
	,"speed":10 // DEFAULT: 10 (pix/second)
	,"canBeClicked":true // DEFAULT: true
	,"behaviour":1
		/*
			DEFAULT: 4

			1:totally idle, doesn't walk but displays events
			2:chase pointer
			3:whimsical, just like current Aneko, it starts walking in a random direction every click or keypress
			4:chill around, like totally idle, but ocasionally walks

			future implementation:
			annoy, it would prefer critical screen places to be
			more!
		*/
	,"angularSpeed": //DEFAULT: 90 (degrees/second)
		//determines how fast or slow can a Jittai turn around, only useful in behaviours 2 and 3
	,"timeBetweenEvents":[10,120] // DEFAULT: [10,120] minimum and maximum idle time, in whole seconds
		// if the minimum is less than 1, it defaults to 1
		// if the maximum is less than the minimum, it defaults to the minimum
	,"sounds":[ // array of sound file names 
		"sound.wav"
	]
	,"walkingCycle":[ //array of arrays of animation steps
		//if its behaviour is not chill around (4), you need at least 4 cycles
		[
			{
				"sprite":0
				,"duration":0.1
				,"sound":0
			}
		]
	]
	,"events":[ //array of event objects
		{
			"name":"event1" //mandatory
			,"probability":50 //optional, DEFAULT: 100
				//the default is for triggered events to be able to ommit the probability property, you should define it for non-triggered events as a sum of more than 100 on regular events is not allowed
			,"trigger":"after-" //optional, no default
			,"repetitionInfo":[1,5] //optional, DEFAULT: [1]
				//the event animation will repeat between index 0 and index 1 times, or just index 0 if there's no index 1 or it's less than index 0
			,"animation":[ //mandatory array of animation elements
				{
					"sprite":0
					,"duration":0.1
					,"sound":0
				}
				//animation loop example:
				{
					"isLoop":true //mandatory, to tell the software this is not a simple animation step
					"repetitionInfo":[2,4] //optional, DEFAULT [1], but there's no point on not defining it
					"loop":[ //mandatory array of animation elements
						{
							"sprite":0
							,"duration":0.1
							,"sound":0
						}
					]
				}
			]
			//arrays of element must not have empty loops
		}
	]
}

```

## To do

Add more events triggers. (not that soon)

Support more file formats. (webp, jsonc, mp3...)

A proper documentation. (when I get my own website)

A website to make JJittai creation more user-friendly. (see above)

Support for probability on animation loops.

Named sprites.

Maybe define general sprite size.

## Known Issues

The killing message may appear on the background.

canBeClicked:false only works on Windows. Or at least it should.

The CHASE_CURSOR behaviour may miss the cursor by less than a pixel, so it starts "bouncing" back and forth to find it.

The CHASE_CURSOR behaviour is unpredictable if you play with your pointer over the jittai.
