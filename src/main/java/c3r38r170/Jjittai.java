package c3r38r170;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import java.awt.image.BufferedImage;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Component;
import java.awt.MouseInfo;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JWindow;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JFrame;

import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;
import org.jnativehook.mouse.NativeMouseEvent;
import org.jnativehook.mouse.NativeMouseListener;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.Native;

class Jjittai extends JWindow {
	private static final long serialVersionUID = 1L;

	// main properties
	private String name;
	private LinkedList<Clip> sounds = new LinkedList<>();// check how I managed sounds on the battery app
	private LinkedList<BufferedImage> sprites = new LinkedList<>();
	private LinkedList<LinkedList<AnimationStep>> walkingCycle = new LinkedList<>();
	private LinkedList<AnimationElement> idleCycle = new LinkedList<>();
	private Behaviour behaviour;
	private double angularSpeed;
	private boolean canBeClicked;
	private LinkedList<Event> events = new LinkedList<>();
	private double finalSpeed;
	private double acceleration;
	private int[] timeBetweenEvents= new int[2];

	// auxiliar properties
	private double currentSpeed;
	private boolean stopped;
	private boolean isAlreadyUntouchable=false;
	private BufferedImage currentImage;
	private int height, width;
	private LinkedList<AnimationStep> currentWalkingAnimation;
	private long miliseconds = 0;
	private LinkedList<StoppedAnimation> animationStack = new LinkedList<>();
	private LinkedList<Integer> regularEvents = new LinkedList<>();
	private Map<String,Integer> triggerEvents=new LinkedHashMap<>();
	
	private double[] coordenates = { 0, 0 };
	private Point destination;
	private int[] usedWnH = new int[2];
	private double targetAngle;
	private double currentAngle;
	
	private Runnable customActionAfterAnimation;
	
	private LinkedList<AnimationElement> currentAnimation = new LinkedList<>();
	private String currentEventName;
	private int currentAnimationSize;
	private int currentAnimationID=0;
	
	//meta
	static private Dimension screenSize=Toolkit.getDefaultToolkit().getScreenSize();;
	static private LinkedList<Jjittai> instances = new LinkedList<>();
	private boolean frozen=false;
	private static JPanel main;
	private static ImageIcon worker=new ImageIcon(Jjittai.class.getResource("/worker.png"));
	private boolean initializeWithMenu;

	public static void main(String[] args) {
		File dir = new File(".");
		File[] filesList = dir.listFiles(new FileFilter() {
			@Override
			public boolean accept(File currFile) {
				return currFile.getName().toLowerCase().endsWith(".zip") && currFile.isFile();
			}
		});
		for (File file : filesList) {
			try (ZipFile zip = new ZipFile(file)) {
				String zipName = file.getName();
				ZipEntry JSONFile = zip.getEntry(zipName.substring(0, zipName.lastIndexOf(".")) + ".json");
				if (JSONFile != null) {
					InputStream JSONStream = zip.getInputStream(JSONFile);
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					byte[] buffer = new byte[4096];
					for (int n; (n = JSONStream.read(buffer)) != -1;)
						out.write(buffer, 0, n);
					instances.add(new Jjittai(out.toString("UTF-8"), zip));
				}
			} catch (IOException e){
				JOptionPane.showMessageDialog(null, "Error de imagen: "+e.getMessage(), "ERROR", JOptionPane.ERROR_MESSAGE);
			}catch (JSONException e){
				JOptionPane.showMessageDialog(null, "Error de JSON: "+e.getMessage(), "ERROR", JOptionPane.ERROR_MESSAGE);
			} catch (LineUnavailableException | UnsupportedAudioFileException e) {
				JOptionPane.showMessageDialog(null, "Error de sonido: "+e.getMessage(), "ERROR", JOptionPane.ERROR_MESSAGE);
			}
			
		}
		if (instances.size() == 0){
			JOptionPane.showMessageDialog(null, "No se ha encontrado ningún Jjittai.", "Lo sentimos.", JOptionPane.INFORMATION_MESSAGE);
			System.exit(0);
		}else if (instances.size() == 1&&!instances.get(0).initializeWithMenu)
			instances.get(0).summon();
		else {
			JFrame jittaisManager = new JFrame();
			jittaisManager.setMinimumSize(new Dimension(300,0));
			jittaisManager.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			jittaisManager.setTitle("Jjittai's Manager");
			jittaisManager.setIconImage(worker.getImage());
			main = new JPanel();
			jittaisManager.setContentPane(new JScrollPane(main));
			main.setLayout(new BoxLayout(main,BoxLayout.Y_AXIS));
			for (Jjittai jittai : instances) {
				JCheckBox jittaiCheckBox = new JCheckBox(jittai.name);
				jittaiCheckBox.addItemListener(new ItemListener(){
					@Override
					public void itemStateChanged(ItemEvent e) {
						if(jittaiCheckBox.isSelected())
							jittai.summon();
						else jittai.kill();
					}
				});
				main.add(jittaiCheckBox);
			}
			jittaisManager.pack();
			if(jittaisManager.getWidth()<300)
				jittaisManager.setSize(300,jittaisManager.getHeight());
			jittaisManager.setLocation((int)(screenSize.width-50-jittaisManager.getWidth()),(int)(screenSize.height-50-jittaisManager.getHeight()));
			
			jittaisManager.setVisible(true);
		}
	}

	public Jjittai(String JSONString, ZipFile zip) throws JSONException, IOException, LineUnavailableException, UnsupportedAudioFileException {
		// mandatory
		JSONObject jittai = new JSONObject(JSONString);
		name = jittai.getString("name");
		if (name.isEmpty())
			throw new JSONException("El nombre no puede estar vacío.");
		JSONArray sprites = jittai.getJSONArray("sprites");
		if (sprites.length() == 0)
			throw new JSONException("Tiene que haber por lo menos un sprite.");
		Map<String,BufferedImage> originalImages=new LinkedHashMap<>();
		for (int i = 0, len = sprites.length(); i < len; i++){
			JSONObject rawSprite=sprites.getJSONObject(i);
			String imageName=rawSprite.getString("file");
			BufferedImage file;
			if(originalImages.containsKey(imageName))
				file=originalImages.get(imageName);
			else{
				file=ImageIO.read(zip.getInputStream(zip.getEntry(imageName)));
				originalImages.put(imageName,file);
			}
			this.sprites.add(file.getSubimage(
				rawSprite.optInt("x", 0)
				,rawSprite.optInt("y", 0)
				,rawSprite.optInt("width", file.getWidth())
				,rawSprite.optInt("height", file.getHeight())));
		}
			
		switch (jittai.optInt("behaviour", 0)) {
		case 0:
			behaviour = Behaviour.CHILL_AROUND;
			break;
		case 1:
			behaviour = Behaviour.TOTALLY_IDLE;
			break;
		case 2:
			behaviour = Behaviour.CHASE_POINTER;
			break;
		case 3:
			behaviour = Behaviour.WHIMSICAL;
			break;
		case 4:
			behaviour = Behaviour.CHILL_AROUND;
			break;
		default:
			behaviour = Behaviour.CHILL_AROUND;
			break;
		}
		
		if(behaviour!=Behaviour.TOTALLY_IDLE){
			JSONArray walkingCycle = jittai.getJSONArray("walkingCycle");
			if (walkingCycle.length() < 4)
				throw new JSONException("Tiene que haber mínimo 4 ciclos.");
			for (int i = 0, len = walkingCycle.length(); i < len; i++) {
				LinkedList<AnimationStep> tempList = new LinkedList<>();
				JSONArray tempArray = walkingCycle.getJSONArray(i);
				for (int j = 0, leng = tempArray.length(); j < leng; j++)
					tempList.add(new AnimationStep(tempArray.getJSONObject(j)));
				this.walkingCycle.add(tempList);
			}
		}
		JSONArray idleCycle = jittai.getJSONArray("idleCycle");
		if (idleCycle.length() == 0)
			throw new JSONException("Tiene que haber al menos un sprite de idle.");
		for (int i = 0, len = idleCycle.length(); i < len; i++)
			this.idleCycle.add(stepOrLoop(idleCycle.getJSONObject(i)));
		
		//hidden
		initializeWithMenu=jittai.optBoolean("initializeWithMenu",false);
		
		// optional
		finalSpeed = jittai.optInt("speed", 10);
		acceleration = jittai.optInt("acceleration", 10);
		canBeClicked = behaviour == Behaviour.TOTALLY_IDLE ? true : jittai.optBoolean("canBeClicked", true);
		angularSpeed=Math.PI/180*jittai.optDouble("angularSpeed",180);
		JSONArray timeBetweenEvents=jittai.optJSONArray("timeBetweenEvents");
		if(timeBetweenEvents==null){
			this.timeBetweenEvents[0]=10;
			this.timeBetweenEvents[1]=110;
		}else{
			int minimum=timeBetweenEvents.optInt(0,10);
			if(minimum<1)
				this.timeBetweenEvents[0]=1;
			else this.timeBetweenEvents[0]=minimum;
			int maximum=timeBetweenEvents.optInt(1,110);
			if(maximum<minimum)
				this.timeBetweenEvents[1]=0;
			else this.timeBetweenEvents[1]=maximum-minimum;
		}
		JSONArray sounds = jittai.optJSONArray("sounds");
		if (sounds != null)
			for (int i = 0, len = sounds.length(); i < len; i++){
				Clip clip=AudioSystem.getClip();
				clip.open(AudioSystem.getAudioInputStream(new BufferedInputStream(zip.getInputStream(zip.getEntry(sounds.getString(i))))));
				this.sounds.add(clip);
			}
		JSONArray events = jittai.optJSONArray("events");
		if (events != null){
			int probabilitySum=0;
			for (int i = 0, len = events.length(); i < len; i++){
				Event disposable=new Event(events.getJSONObject(i));
				this.events.add(disposable);
				if(disposable.triggers.size()>0)
					for(String trigger:disposable.triggers){
						if(triggerEvents.containsKey(trigger))
							throw new JSONException("Se han encontrado múltiples evento para el mismo trigger.");
						triggerEvents.put(trigger,this.events.size()-1);
					}
				else{
					regularEvents.add(this.events.size()-1);
					probabilitySum+=disposable.probability;
					if(probabilitySum>100)
						throw new JSONException("La suma de las probabilidades debe ser menor o igual a 100.");
				}
			}
		}
		
		// TODO document this chunk
		
		if(behaviour==Behaviour.TOTALLY_IDLE){
			MouseAdapter epicAdapter=new MouseAdapter(){
				private Point originalClick,originalPosition;
				@Override
				public void mousePressed(MouseEvent e){
					if(e.getButton()==MouseEvent.BUTTON1){
						originalClick=new Point(e.getLocationOnScreen());
						originalPosition=getLocation();
					}
				}
				@Override
				public void mouseReleased(MouseEvent e){
					if(e.getButton()==MouseEvent.BUTTON1&&frozen)
						startIdle();// TODO just resume, make a resume method on about life and death fms
				}
				@Override
				public void mouseDragged(MouseEvent e) {
					if(!frozen)
						frozen=true;
					setPosition(originalPosition.x+(e.getXOnScreen() - originalClick.x), originalPosition.y+(e.getYOnScreen() - originalClick.y));
				}
			};
			addMouseListener(epicAdapter);
			addMouseMotionListener(epicAdapter);
		}
		if(behaviour==Behaviour.WHIMSICAL)
			try {
				GlobalScreen.registerNativeHook();
				GlobalScreen.addNativeMouseListener(new NativeMouseListener(){
					
					@Override
					public void nativeMousePressed(NativeMouseEvent arg0) {
						if(frozen)
							return;
						//randomWalk(); // TODO made for debugging, uncomment
					}
					
					@Override
					public void nativeMouseClicked(NativeMouseEvent arg0) {}
					@Override
					public void nativeMouseReleased(NativeMouseEvent arg0) {}
				});
				GlobalScreen.addNativeKeyListener(new NativeKeyListener(){
					
					boolean released=true;
					
					@Override
					public void nativeKeyTyped(NativeKeyEvent nativeEvent) {}
					
					@Override
					public void nativeKeyPressed(NativeKeyEvent nativeEvent) {
						if(frozen || !released)
							return;
						randomWalk();
						released=false;
					}
					@Override
					public void nativeKeyReleased(NativeKeyEvent nativeEvent) {
						released=true;
					}
				});
				
			} catch (NativeHookException e) {}
		if(canBeClicked)
			addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(MouseEvent e){
					if(e.getButton()==MouseEvent.BUTTON3)
						askAboutKilling();
				}
			});
		else try{
			GlobalScreen.registerNativeHook();
			GlobalScreen.addNativeMouseListener(new NativeMouseListener(){
				
				@Override
				public void nativeMousePressed(NativeMouseEvent arg0) {
					if(frozen)
						return;
					if(arg0.getButton()==MouseEvent.BUTTON2 && touchingMouse())
						askAboutKilling();
				}
				@Override
				public void nativeMouseClicked(NativeMouseEvent arg0) {}
				@Override
				public void nativeMouseReleased(NativeMouseEvent arg0) {}
			});
		}catch(Exception e){System.err.println(e.getMessage());}
		
		//stop native hook log spam, I THINK
		Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
		logger.setLevel(Level.WARNING);
		logger.setUseParentHandlers(false);
		
		setBackground(new Color(0, 0, 0, 0));
		setAlwaysOnTop(true);
		setContentPane(new JLabel());
	}

	//tool methods

	private void setPosition(double x, double y){
		coordenates[0]=x;
		coordenates[1]=y;
		setLocation((int)Math.round(coordenates[0]),(int)Math.round(coordenates[1]));
	}

	private void setImage(int sprite){
		currentImage=sprites.get(sprite);
		width=currentImage.getWidth();
		height=currentImage.getHeight();
		if(width!=getWidth()||height!=getHeight()){
			setPosition(coordenates[0]+(getWidth()-width)/2, coordenates[1]+(getHeight()-height)/2);
			setSize(width, height);
		}
		((JLabel) getContentPane()).setIcon(new ImageIcon(currentImage));
	}

	private int randomInt(int range){
		return (int)Math.round(Math.random()*range);
	}
	
	private int randomIntFromArray(int[] interval){
		return interval[0]+(interval.length==1?0:randomInt(interval[1]-interval[0]));
	}
	
	private static void setTimeout(Runnable runnable, long delay) {
		new Thread(() -> {
			try {
				Thread.sleep(delay);
				runnable.run();
			} catch (InterruptedException e){}
		}).start();
	}

	private void randomWalk(){
		walkTo(randomInt(screenSize.width-width),randomInt(screenSize.height-height));
	}

	private void walkTo(Point p){
		walkTo((int)p.getX(),(int)p.getY());
	}

	private boolean touchingMouse(){
		Point mouseLoc=MouseInfo.getPointerInfo().getLocation();
		int mX=(int)mouseLoc.getX(),mY=(int)mouseLoc.getY();
		return (getX()-3<mX&&mX<getX()+width+3 && getY()-3<mY&&mY<getY()+height+3);
	}
	
	private AnimationElement stepOrLoop(JSONObject inQuestion) throws JSONException {
		return inQuestion.has("isLoop") && inQuestion.getBoolean("isLoop")? new AnimationLoop(inQuestion): new AnimationStep(inQuestion);
	}
	
	private int[] getRepetitionsArray(JSONArray rawReps) throws JSONException { // TODO try to apply to timeBetweenEvents, maybe add default
		if(rawReps==null||rawReps.length()==0){
			int[] reps={1};
			return reps;
		}else{
			int[] reps={
				rawReps.getInt(0)
				,rawReps.optInt(1,0)
			};
			if(reps[1]<=reps[0]){
				int temp=reps[0];
				reps=new int[1];
				reps[0]=temp;
			}
			return reps;
		}
	}
	
	//about life and death
	
	public void summon() { // TODO add onsummon
		stopped=true;
		boolean summonAnimation=false;
		setPosition(Math.random()*(screenSize.width - width), Math.random()*(screenSize.height - height));
		LinkedList<AnimationElement> firstSeen=idleCycle;
			//triggerEvents.containsKey("summon")?triggerEvents.get("summon").animation:;
		if(triggerEvents.containsKey("summon")){
			Event onsummon=events.get(triggerEvents.get("summon"));
			if(Math.random()<onsummon.probability/100){
				summonAnimation=true;
				firstSeen=onsummon.animation;
			}
		}
		int i=0;
		while(firstSeen.get(i).isLoop)
			i++;
		setImage(((AnimationStep)firstSeen.get(i)).sprite);
		setVisible(true);
		
		//Windows, can't be clicked, TODO check if it has to be applied every time or what, ... it works
		//if(!canBeClicked && !isAlreadyUntouchable){
		if(!canBeClicked){
			HWND hwnd = new HWND();
			hwnd.setPointer(Native.getComponentPointer(this));
			int wl = User32.INSTANCE.GetWindowLong(hwnd, WinUser.GWL_EXSTYLE);
			wl = wl | WinUser.WS_EX_LAYERED | WinUser.WS_EX_TRANSPARENT;
			User32.INSTANCE.SetWindowLong(hwnd, WinUser.GWL_EXSTYLE, wl);
			//isAlreadyUntouchable=true;
		}
		// start activity
		if(frozen)
			frozen=false;
		Runnable action=null;
		switch (behaviour) {
		case CHASE_POINTER:
			action=()->chaseMouse();
			break;
		case WHIMSICAL:
		case TOTALLY_IDLE:
		case CHILL_AROUND:
			action=()->startIdle();
			break;
		}
		if(summonAnimation)
			startEvent(events.get(triggerEvents.get("summon")),action);
		else action.run();
	}

	public void kill(){
		lookForAnimation("kill",()->{
			frozen=true;
			stopped=false;
			setVisible(false);
		});
	}

	public void askAboutKilling(){
		frozen=true;
		JFrame parentFrame=new JFrame();
		int opcion=JOptionPane.showConfirmDialog(parentFrame, "¿Desea matar a "+name+"?", "Matar", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, worker);
		parentFrame.requestFocus();
		parentFrame.dispose();
		if(opcion==0){
			if(instances.size()>1){
				kill();
				for(Component checkBox:main.getComponents()){
					JCheckBox JcheckBox=(JCheckBox)checkBox;
					if(JcheckBox.getText().equals(name))
						JcheckBox.setSelected(false);
				}
			}else lookForAnimation("kill",()->System.exit(0));
		}else startIdle();
	}
	
	//actual working logic

	private void chaseMouse(){
		if(frozen)
			return;
		if(!touchingMouse())
			walkTo(MouseInfo.getPointerInfo().getLocation());//TODO 	que asco
		setTimeout(()->chaseMouse(),1000);
	}

	private void startIdle() {
		if(frozen)
			frozen=false;
		miliseconds=Math.round((timeBetweenEvents[0]+Math.random()*timeBetweenEvents[1])*1000);
		if (idleCycle.size() == 1 && !idleCycle.get(0).isLoop) {// 
			setImage(((AnimationStep)idleCycle.get(0)).sprite);
			setTimeout(()->playEvent(), miliseconds);
		}else{
			currentAnimation=idleCycle;
			currentAnimationSize=currentAnimation.size();
			idleStep(currentAnimation.size());
		}
	}
	
	private void idleStep(int step) {
		if(frozen||!stopped)
			return;
			int modulo=step%currentAnimationSize;
		AnimationElement thisElement=currentAnimation.get(
			modulo==0?0:currentAnimationSize-modulo
		);
		if(thisElement.isLoop){
			animationStack.push(new StoppedAnimation(currentAnimation,step==0?currentAnimationSize:step-1));// TODO again, make this a method somewhere, maybe join both?
			AnimationLoop thisLoop=(AnimationLoop)thisElement;
			currentAnimation=thisLoop.loop;
			idleStep(randomIntFromArray(thisLoop.repetitions)*currentAnimationSize);
		}else{
			AnimationStep thisStep=(AnimationStep)thisElement;
			int time=(int)Math.round(thisStep.duration*1000);
			miliseconds-=time;
			setImage(thisStep.sprite);
			if(animationStack.size()>0 && step==0){
				StoppedAnimation previousAnimation=animationStack.pop();
				currentAnimation=previousAnimation.animation;
				setTimeout(()->idleStep(previousAnimation.resumingStep),time);
			}else if(animationStack.size()>0 || miliseconds>=0)
				setTimeout(()->idleStep(step==1?currentAnimationSize:step-1),time);//(step==currentAnimation.size()-1?0:step+1),time);
			else playEvent();
		}
	}

	private void playEvent() {
		if(!stopped)
			return;
		if(regularEvents.size()==0)
			if(behaviour==Behaviour.CHILL_AROUND&&Math.random()>0.6)
				randomWalk();
			else startIdle();
		else{
			int currentEvent=0;
			double measure=Math.random()*(behaviour==Behaviour.CHILL_AROUND?1.5:1);
			double possibility=0;
			int eventsSize=regularEvents.size();
			while(measure>possibility&&currentEvent<eventsSize)
				possibility+=events.get(regularEvents.get(currentEvent++)).probability/100.0;
			if(measure<possibility){
				Event chosenEvent=events.get(regularEvents.get(currentEvent-1));
				lookForAnimation(
					"before-"+chosenEvent.name
					,()->startEvent(chosenEvent,()->startIdle())
				);
			}else if(behaviour==Behaviour.CHILL_AROUND && measure>1)
				randomWalk();
			else startIdle();
		}
	}
	
	private void lookForAnimation(String trigger,Runnable doAfter){
		if(triggerEvents.containsKey(trigger))// TODO try all defaults (SIN PROBAR:after-Walking,  KILL,before-animation, after-animation)
			startEvent(events.get(triggerEvents.get(trigger)), doAfter);
		else doAfter.run();
	}
	
	public void startEvent(Event chosenEvent,Runnable actionAfterAnimation){
		if(currentEventName==chosenEvent.name)  
			return;
		int[] reps=chosenEvent.repetitions;
		currentAnimation=chosenEvent.animation;
		currentAnimationSize=currentAnimation.size();
		currentEventName=chosenEvent.name;
		customActionAfterAnimation=actionAfterAnimation;
		animationStack.clear();
		giveAnimationStep(currentAnimation.size()*randomIntFromArray(reps),++currentAnimationID);
	}
	
	private void giveAnimationStep(/*LinkedList<AnimationElement> chosenAnimation,*/ int step,int ID){
		//System.out.println(currentEventName+" "+currentAnimationID+" =? "+ID+" step:"+step); // TODO remove BOTH once you are sure
		if(frozen||!stopped||currentAnimationID!=ID)
			return;
		int size=currentAnimation.size(),
			remainder=step%size;
		AnimationElement thisElement=currentAnimation.get(remainder==0?0:size-remainder);
		
		if(thisElement.isLoop){
			animationStack.push(new StoppedAnimation(currentAnimation, step-1));
			AnimationLoop thisLoop = (AnimationLoop)thisElement;
			currentAnimation=thisLoop.loop;
			int[] reps=thisLoop.repetitions;
			giveAnimationStep(currentAnimation.size()*randomIntFromArray(reps),ID);// TODO apply this utility
		}else{
			AnimationStep thisStep=(AnimationStep)thisElement;
			//System.out.println(thisStep.sprite);
			setImage(thisStep.sprite);
			//sound
			if(thisStep.sound!=-1){
				Clip thisClip=sounds.get(thisStep.sound);
				thisClip.start();
				setTimeout(()->stopClip(thisClip),thisClip.getMicrosecondLength()/1000);
			}
			
			long timeout=Math.round(thisStep.duration*1000);
			if(step > 1)
				setTimeout(()->giveAnimationStep(step-1,ID),timeout);
			else if(animationStack.size()>0){
				StoppedAnimation resumingAnimation;
				do
					resumingAnimation=animationStack.pop();
				while(resumingAnimation.resumingStep==0);
				currentAnimation=resumingAnimation.animation;
				int resumingStep=resumingAnimation.resumingStep;
				setTimeout(()->giveAnimationStep(resumingStep,ID), timeout);
			}else{
				setTimeout(()->{
					currentEventName=null;
					lookForAnimation("after-"+currentEventName,customActionAfterAnimation);
				},timeout);
			}
		}
	}

	private void stopClip(Clip clipToStop){
		clipToStop.stop();
		clipToStop.setMicrosecondPosition(0);
	}
	
	private void walkTo(int x, int y) {
		destination = new Point(x, y);
		int DeltaY = y - getY();
		int DeltaX = x - getX();
		targetAngle = Math.atan2(DeltaY, DeltaX);
		double partitions = 2 * Math.PI / walkingCycle.size();
		double degrees = Math.PI / 2 + partitions / 2 + targetAngle;
		if (degrees < 0)
			degrees += 2 * Math.PI;
		currentWalkingAnimation = walkingCycle.get((int) Math.floor(degrees / partitions));
		usedWnH[0] = width;
		usedWnH[1] = height;
		if(stopped){
			lookForAnimation("before-Walking", ()->{
				currentAngle=targetAngle;
				stopped = false;
				walkingStep();
				showWalkingStep(0);
			});
		}
	}

	private void walkingStep() {
		if(frozen)
			return;
		if (currentSpeed != finalSpeed) {
			currentSpeed += acceleration / 10;
			if (currentSpeed > finalSpeed)
				currentSpeed = finalSpeed;
		}
		if(currentAngle!=targetAngle){
			double resta =currentAngle-targetAngle;
			switch(Math.abs(resta)>Math.PI?2:0 + resta>0?1:0){
			case 0://resta menor a 180 ochenta (sin conversion), targetAngle es mayor que currentAngle
				currentAngle+=angularSpeed/10;
				if(currentAngle>targetAngle)
					currentAngle=targetAngle;
				break;
			case 1://resta menor a 180 ochenta (sin conversion), currentAngle es mayor que targetAngle
				currentAngle-=angularSpeed/10;
				if(currentAngle<targetAngle)
					currentAngle=targetAngle;
				break;
			case 2://resta mayor a 180 ochenta (potencial conversion), currentAngle es mayor que targetAngle
				currentAngle+=angularSpeed/10;
				if(currentAngle>=Math.PI*2){
					currentAngle-=Math.PI*2;
					if(currentAngle>targetAngle)
						currentAngle=targetAngle;
				}
				break;
			case 3://resta mayor a 180 ochenta (potencial conversion), targetAngle es mayor que currentAngle
				currentAngle-=angularSpeed/10;
				if(currentAngle<=0){
					currentAngle+=Math.PI*2;
					if(currentAngle<targetAngle)
						currentAngle=targetAngle;
				}
				break;
			}
			targetAngle = Math.atan2(destination.y - getY(), destination.x - getX());
			double partitions = 2 * Math.PI / walkingCycle.size();
			double degrees = Math.PI / 2 + partitions / 2 + currentAngle;
			if (degrees < 0)
				degrees += 2 * Math.PI;
			if(degrees>2*Math.PI)
				degrees-=2*Math.PI;
			currentWalkingAnimation = walkingCycle.get((int) Math.floor(degrees / partitions));
			//recalculateAngle();
		}
		setPosition(coordenates[0] + Math.cos(currentAngle) * currentSpeed,coordenates[1] + Math.sin(currentAngle) * currentSpeed);
		Point displacedDest = new Point(
			(int)(destination.x + (usedWnH[0] - width) / 2),
			(int)(destination.y + (usedWnH[1] - height) / 2)
		);
		if ((
				getX()-3 < displacedDest.x && displacedDest.x < getX() + width+3
				&& getY()-3 < displacedDest.y && displacedDest.y < getY() + height+3
		)||(behaviour==Behaviour.CHASE_POINTER && touchingMouse())){
			currentSpeed = 0;
			stopped = true;
			lookForAnimation("after-Walking", ()->startIdle());
		}else setTimeout(() -> walkingStep(), 100);
	}
	
	
	private void recalculateAngle(){
		
	}
	
	private void showWalkingStep(int step) {
		if (stopped||frozen)
			return;
		AnimationStep currentStep = currentWalkingAnimation.get(step);
		setImage(currentStep.sprite);
		setTimeout(() ->showWalkingStep(step == currentWalkingAnimation.size() - 1 ?0 : step + 1),Math.round(currentStep.duration * 1000));
	}

	//inner classes

	private class Event{
		private String name;
		private int probability;
		private int[] repetitions;
		private LinkedList<AnimationElement> animation=new LinkedList<>();
		private LinkedList<String> triggers=new LinkedList<>();

		public Event(JSONObject rawEvent) throws JSONException{
			JSONArray animationsArray=rawEvent.getJSONArray("animation");
			if(animationsArray.length()==0)
				throw new JSONException("La animación debe constar de al menos un sprite.");
			for(int i=0,len=animationsArray.length();i<len;i++)
				animation.add(stepOrLoop(animationsArray.getJSONObject(i)));
			name=rawEvent.getString("name");
			probability=rawEvent.optInt("probability",100);
			repetitions=getRepetitionsArray(rawEvent.optJSONArray("repetitionInfo"));
			String trigger=rawEvent.optString("trigger",null);
			if(trigger==null){
				JSONArray triggers=rawEvent.optJSONArray("triggers");
				if(triggers!=null)
					for(int i=0,len=triggers.length();i<len;i++)
						this.triggers.add(triggers.getString(i));
			}else triggers.add(trigger);
		}
	}
	
	
	private abstract class AnimationElement{
		private boolean isLoop=false;
	}
	
	private class AnimationLoop extends AnimationElement{
		private LinkedList<AnimationElement> loop= new LinkedList<>();
		private int[] repetitions;
		
		public AnimationLoop(JSONObject rawLoop) throws JSONException {
			super.isLoop=true;
			JSONArray rawAnimation=rawLoop.getJSONArray("loop");
			for(int i=0,len=rawAnimation.length();i<len;i++)
				loop.add(stepOrLoop(rawAnimation.getJSONObject(i)));
			repetitions=getRepetitionsArray(rawLoop.optJSONArray("repetitions"));
		}
		
	}
	
	
	private class AnimationStep extends AnimationElement{
		private double duration;
		private int sprite;
		private int sound;
		
		public AnimationStep(JSONObject rawObject) throws JSONException {
			duration=rawObject.optDouble("duration",0);
			sprite=rawObject.getInt("sprite");
			sound=rawObject.optInt("sound", -1);
		}
	}
	
	private class StoppedAnimation{
		
		public LinkedList<AnimationElement> animation;
		public int resumingStep;
		
		public StoppedAnimation(LinkedList<AnimationElement> animation,int resumingStep){
			this.animation=animation;
			this.resumingStep=resumingStep;
		}
	}

	private enum Behaviour{
		TOTALLY_IDLE
		,CHASE_POINTER
		,WHIMSICAL
		,CHILL_AROUND
	}
	
	/*
		Falta:
			
			triggers
				ondeath
				onsummon
				onstartwalking
			cambiar gradualmente el angulo segun energia?
	*/
	
}