package c3r38r170;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;

import java.util.LinkedList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import java.awt.image.BufferedImage;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.Component;

import javax.imageio.ImageIO;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JWindow;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JFrame;

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
	private LinkedList<String> sounds = new LinkedList<>();// check how I managed sounds on the battery app
	private LinkedList<BufferedImage> sprites = new LinkedList<>();
	private LinkedList<LinkedList<AnimationStep>> walkingCycle = new LinkedList<>();
	private LinkedList<AnimationStep> idleCycle = new LinkedList<>();
	private Behaviour behaviour;
	private boolean canBeClicked;
	private LinkedList<Event> events = new LinkedList<>();
	private double finalSpeed;
	private double acceleration;

	// auxiliar properties
	private double currentSpeed;
	private double currentAngle;
	private Point destination;
	private boolean stopped = true;
	private int[] usedWnH = new int[2];

	private BufferedImage currentImage;
	private double[] coordenates = { 0, 0 };
	static private Dimension screenSize;
	private int height, width;
	private LinkedList<AnimationStep> currentWalkingAnimation;
	private long miliseconds = 0;

	//meta
	static private LinkedList<Jjittai> Jjittais = new LinkedList<>();
	private boolean frozen=false;
	private static JPanel main;
	private static ImageIcon worker=new ImageIcon(Jjittai.class.getResource("/worker.png"));
	private Point screen;
	private Point my;
	private boolean initializeWithMenu;

	public static void main(String[] args) {
		screenSize = Toolkit.getDefaultToolkit().getScreenSize();
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
					Jjittais.add(new Jjittai(out.toString("UTF-8"), zip));
				}
			} catch (Exception e) {
				JOptionPane.showMessageDialog(null, "Error: "+e.getMessage(), "ERROR", JOptionPane.ERROR);
			}

		}
		if (Jjittais.size() == 0){
			JOptionPane.showMessageDialog(null, "No se ha encontrado ningún Jjittai.", "Lo sentimos.", JOptionPane.INFORMATION_MESSAGE);
			System.exit(0);// avisar antes JFrame+DISPOSE_ON_CLOSE || JOptionPane advertencia
		}else if (Jjittais.size() == 1&&!Jjittais.get(0).initializeWithMenu){
			//if(!canBeClicked)canBeClicked=true;
			Jjittais.get(0).summon();
		}else { // show menu
			JFrame jittaisManager = new JFrame();
			jittaisManager.setMinimumSize(new Dimension(300,0));
			jittaisManager.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			jittaisManager.setTitle("Jjittai's Manager");
			jittaisManager.setIconImage(worker.getImage());
			main = new JPanel();
			jittaisManager.setContentPane(new JScrollPane(main));
			main.setLayout(new BoxLayout(main,BoxLayout.Y_AXIS));
			for (Jjittai jittai : Jjittais) {
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
			jittaisManager.setLocation((int)(screenSize.getWidth()-50-jittaisManager.getWidth()),(int)(screenSize.getHeight()-50-jittaisManager.getHeight()));

			jittaisManager.setVisible(true);
		}
	}

	public Jjittai(String JSONString, ZipFile zip) throws JSONException, IOException {
		// mandatory
		JSONObject jittai = new JSONObject(JSONString);
		name = jittai.getString("name");
		if (name.isEmpty())
			throw new JSONException("El nombre no puede estar vacío.");
		JSONArray sprites = jittai.getJSONArray("sprites");
		if (sprites.length() == 0)
			throw new JSONException("Tiene que haber por lo menos un sprite.");
		for (int i = 0, len = sprites.length(); i < len; i++)
			this.sprites.add(spriteMaker(sprites.getJSONObject(i), zip));
		
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
			this.idleCycle.add(new AnimationStep(idleCycle.getJSONObject(i)));

		initializeWithMenu=jittai.optBoolean("initializeWithMenu",false);

		// optional
		finalSpeed = jittai.optInt("speed", 10);
		acceleration = jittai.optInt("acceleration", 10);
		canBeClicked = behaviour == Behaviour.TOTALLY_IDLE ? true : jittai.optBoolean("canBeClicked", false);
		JSONArray sounds = jittai.optJSONArray("sounds");
		if (sounds != null)
			for (int i = 0, len = sounds.length(); i < len; i++)
				this.sounds.add(sounds.getString(i));
		JSONArray events = jittai.optJSONArray("events");
		if (events != null){
			int probabilitySum=0;
			for (int i = 0, len = events.length(); i < len; i++){
				Event disposable=new Event(events.getJSONObject(i));
				this.events.add(disposable);
				probabilitySum+=disposable.probability;
				if(probabilitySum>100)
					throw new JSONException("La suma de las probabilidades debe ser menor a 100.");
			}
		}
		
		if(behaviour==Behaviour.TOTALLY_IDLE){
			addMouseListener(new MouseAdapter(){
				@Override
				public void mousePressed(MouseEvent e){
					if(e.getButton()==MouseEvent.BUTTON1){
						screen=new Point(e.getLocationOnScreen());
						my=getLocation();
					}
				}
				@Override
				public void mouseReleased(MouseEvent e){
					if(e.getButton()==MouseEvent.BUTTON1&&frozen)
						startIdle();
				}
			});
			addMouseMotionListener(new MouseMotionListener(){

				@Override
				public void mouseDragged(MouseEvent e) {
					if(!frozen)
						frozen=true;
					setPosition(my.x+(e.getXOnScreen() - screen.x), my.y+(e.getYOnScreen() - screen.y));
				}

				@Override
				public void mouseMoved(MouseEvent e) {}
				
			});
		}
		addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(MouseEvent e){
				if(e.getButton()==MouseEvent.BUTTON3){
					frozen=true;
					int opcion=JOptionPane.showConfirmDialog(null, "¿Desea matar a "+name+"?", "Matar", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, worker);
					if(opcion==0){
						if(Jjittais.size()>1){
							kill();
							for(Component checkBox:main.getComponents()){
								JCheckBox JcheckBox=(JCheckBox)checkBox;
								if(JcheckBox.getText().equals(name))
									JcheckBox.setSelected(false);
							}
						}else System.exit(0);
					}else startIdle();
				}
			}
		});
		setBackground(new Color(0, 0, 0, 0));
		setAlwaysOnTop(true);
		setContentPane(new JLabel());
	}

	public void kill(){
		frozen=true;
		setVisible(false);
	}

	public void summon() {
		setPosition(randomInt(screenSize.width - width), randomInt(screenSize.height - height));
		setImage(this.idleCycle.get(0).sprite);
		setVisible(true);
		if(!canBeClicked)
			try{
				HWND hwnd = new HWND();
				hwnd.setPointer(Native.getComponentPointer(this));
				//WinDef.HWND hwnd = hwnd;
				int wl = User32.INSTANCE.GetWindowLong(hwnd, WinUser.GWL_EXSTYLE);
				wl = wl | WinUser.WS_EX_LAYERED | WinUser.WS_EX_TRANSPARENT;
				User32.INSTANCE.SetWindowLong(hwnd, WinUser.GWL_EXSTYLE, wl);
			}catch(Exception e){}

		// start activity//TODO
		switch (behaviour) {
		case CHASE_POINTER:
			// FORGET EVENTAS
			// nah, itd depend on being quite

			// walking, constantly, every second
			// idle near mouse pointer
			break;
		case TOTALLY_IDLE:
			startIdle();
			break;
		case WHIMSICAL:

			// onclick, onkeytap; recalculate route, and walk
			// in between, idle
			break;
		case CHILL_AROUND:
			startIdle();
			break;
		}
	}

	private void walkTo(int x, int y) {
		destination = new Point(x, y);
		int DeltaY = y - getY();
		int DeltaX = x - getX();
		double partitions = 2 * Math.PI / walkingCycle.size();
		currentAngle = Math.atan2(DeltaY, DeltaX);
		double degrees = Math.PI / 2 + partitions / 2 + currentAngle;
		if (degrees < 0)
			degrees += 2 * Math.PI;
		currentWalkingAnimation = walkingCycle.get((int) Math.floor(degrees / partitions));
		usedWnH[0] = width;
		usedWnH[1] = height;
		stopped = false;
		walkingStep();
		showWalkingStep(0);
	}

	private void walkingStep() {
		if(!frozen){
			if (currentSpeed != finalSpeed) {
				currentSpeed += acceleration / 10;
				if (currentSpeed > finalSpeed)
					currentSpeed = finalSpeed;
			}
			setPosition(coordenates[0] + Math.cos(currentAngle) * currentSpeed,coordenates[1] + Math.sin(currentAngle) * currentSpeed);
			Point displacedDest = new Point((int) (destination.getX() + (usedWnH[0] - width) / 2),
					(int) (destination.getY() + (usedWnH[1] - height) / 2));
			if (getX() - 4 < displacedDest.getX() && displacedDest.getX() < getX() + 4 + width
					&& getY() - 4 < displacedDest.getY() && displacedDest.getY() < getY() + 4 + height) {
				currentSpeed = 0;
				stopped = true;
				startIdle();
			} else setTimeout(() -> walkingStep(), 100);
		}
	}

	private void showWalkingStep(int step) {
		if (!stopped&&!frozen) {
			AnimationStep currentStep = currentWalkingAnimation.get(step);
			setImage(currentStep.sprite);
			setTimeout(() ->showWalkingStep(step == currentWalkingAnimation.size() - 1 ?0 : step + 1),Math.round(currentStep.duration * 1000));
		}
	}

	private static void setTimeout(Runnable runnable, long delay) {
		new Thread(() -> {
			try {
				Thread.sleep(delay);
				runnable.run();
			} catch (InterruptedException e){}
    }).start();
	}

	private void playEvent() {
		if(events.size()==0)
			if(behaviour==Behaviour.CHILL_AROUND&&Math.random()>0.6)
				randomWalk();
			else startIdle();
		else{
			int currentEvent=0;
			double measure=Math.random()*(behaviour==Behaviour.CHILL_AROUND?1.5:1);
			double possibility=0;
			while(measure>possibility&&currentEvent<events.size())
				possibility+=events.get(currentEvent++).probability/100.0;
			if(measure<possibility){
				Event chosenEvent=events.get(currentEvent-1);
				int[] reps=chosenEvent.repetitions;
				LinkedList<AnimationStep> chosenAnimation=chosenEvent.animation;
				animationStep(chosenAnimation, chosenAnimation.size()*(reps[0]+(reps.length==1?0:randomInt(reps[1]-reps[0]))));
			}else if(behaviour==Behaviour.CHILL_AROUND && measure>1)
				randomWalk();
			else startIdle();
		}
	}

	private void randomWalk(){
		walkTo(randomInt(screenSize.width-this.width),randomInt(screenSize.height-this.height));
	}

	private int randomInt(int range){
		return (int)Math.round(Math.random()*range);
	}

	private void animationStep(LinkedList<AnimationStep> animation, int step){
		if(!frozen){
			int size=animation.size(),
				remainder=step%size;
			AnimationStep thisStep=animation.get(remainder==0?0:size-remainder);
			setImage(thisStep.sprite);
			if(step > 0)
				setTimeout(()->animationStep(animation,step-1),Math.round(thisStep.duration*1000));
			else startIdle();
		}
	}
	
	private void startIdle() {
		if(frozen)
			frozen=false;
		miliseconds=10+randomInt(120000);// TODO Activity variable? masybe too
		if (idleCycle.size() == 1) {
			setImage(idleCycle.get(0).sprite);
			setTimeout(()->playEvent(), miliseconds);
		} else idleStep(0);
	}

	private void idleStep(int step) {
		if(!frozen){
			AnimationStep thisStep=idleCycle.get(step);
			int time=(int)Math.round(thisStep.duration*1000);
			miliseconds-=time;
			setImage(thisStep.sprite);
			if(miliseconds>=0)
				setTimeout(()->idleStep(step==idleCycle.size()-1?0:step+1),time);
			else playEvent();
		}
	}

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

	static private BufferedImage spriteMaker(JSONObject rawSprite,ZipFile zip)throws JSONException, IOException{
		BufferedImage file=ImageIO.read(zip.getInputStream(zip.getEntry(rawSprite.getString("file"))));
		return file.getSubimage(
			rawSprite.optInt("x", 0)
			,rawSprite.optInt("y", 0)
			,rawSprite.optInt("width", file.getWidth())
			,rawSprite.optInt("height", file.getHeight()));
	}

	private class Event{
		private String name;
		private int probability;
		private int[] repetitions;
		private LinkedList<AnimationStep> animation=new LinkedList<>();

		public Event(JSONObject rawEvent) throws JSONException{
			JSONArray animationsArray=rawEvent.getJSONArray("animation");
			if(animationsArray.length()==0)
				throw new JSONException("La animación debe constar de al menos un sprite.");
			for(int i=0,len=animationsArray.length();i<len;i++)
				animation.add(new AnimationStep(animationsArray.getJSONObject(i)));
			name=rawEvent.getString("name");
			probability=rawEvent.getInt("probability");
			JSONArray rawRepetitions=rawEvent.optJSONArray("repetitionInfo");
			if(rawRepetitions==null||rawRepetitions.length()==0){
				repetitions=new int[1];
				repetitions[0]=1;
			}else{
				repetitions=new int[2];
				repetitions[0]=rawRepetitions.getInt(0);
				repetitions[1]=rawRepetitions.optInt(1,0);
				if(repetitions[1]<repetitions[0]){
					int temp=repetitions[0];
					repetitions=new int[1];
					repetitions[0]=temp;
				}
			}
		}
	}

	private class AnimationStep{
		private double duration;
		private int sprite;
		private int sound;
		
		public AnimationStep(JSONObject rawObject) throws JSONException {
			duration=rawObject.optDouble("duration",0);
			sprite=rawObject.getInt("sprite");
			sound=rawObject.optInt("sound", -1);
		}
	}

	private enum Behaviour{
		TOTALLY_IDLE
		,CHASE_POINTER
		,WHIMSICAL
		,CHILL_AROUND
	}

}