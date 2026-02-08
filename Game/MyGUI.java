package Game;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import javax.swing.*;
import javax.swing.event.*;

public class MyGUI extends JPanel {

	private static final long serialVersionUID = 1L;
	private final MyGUI self; //so the gui can be referenced by actionlisteners, since "this" refers to the listener

	//GUI components
	private JEditorPane editor;
	private JScrollPane scroll;
	private JTextField input;
	private JButton back;
	
	//story components
	private static WeightedGraph<Story> story; //contains all possible text and options
	private static Story state; //player's current place in the story
	private static boolean IS_DOCTOR=false;
	
	//used by randint
	private static ArrayList<Integer> NUMBERS; 
	private static ArrayList<Integer> CHOICES;
	
	static //initialises the story
	{
		NUMBERS= new ArrayList<Integer>();
		for (int i=1; i<=5; i++)
		{
			NUMBERS.add(i);
		}
		CHOICES=new ArrayList<Integer>();
		for (int i=1; i<=3; i++)
		{
			CHOICES.add(i);
		}
		story=new WeightedGraph<Story>();
		Story reference=MakeAct1();
		Story[] branches=MakeAct2(reference);
		MakeAct3(branches);
	}
	
	private static int RandInt(boolean doctor) //for randomising dinosaur fight choices, for extra chaos lol
	{
		Random random=new Random();
		if (doctor==false) //normal selection
		{
			if (NUMBERS.isEmpty())
			{
				for (int i=1; i<=5; i++)
				{
					NUMBERS.add(i);
				}
			}
			int index=random.nextInt(NUMBERS.size()); //random int from 0 to size-1
			int toret=NUMBERS.get(index);
			NUMBERS.remove(index);
			return toret;
		}
		else //doctor narrows it down to 3 choices
		{
			if (CHOICES.isEmpty())
			{
				for (int i=1; i<=3; i++)
				{
					CHOICES.add(i);
				}
			}
			int index=random.nextInt(CHOICES.size()); //random int from 0 to size-1
			int toret=CHOICES.get(index);
			CHOICES.remove(index);
			return toret;
		}
	}

	public MyGUI () 
	{
		self=this;
		
		this.setLayout(new BorderLayout());
		
        //System.out.println("Java version: "+System.getProperty("java.version"));
		
		//editorpane displays all the game's text
		editor=new JEditorPane();
		editor.setContentType("text/html");
		editor.setFont(new Font("Monospaced", Font.PLAIN, 14));
		editor.setEditable(false); 
		editor.setHighlighter(null);
		editor.setCaret(null);
		editor.setPreferredSize(getPreferredSize());
		
		//initialise the GUI's text with story's intro
		this.updateGUI();
		
		//enables link functionality for footnotes
		editor.addHyperlinkListener(new HyperlinkListener() 
		{
			@Override
			public void hyperlinkUpdate(HyperlinkEvent e) 
			{
				if (e.getEventType()==HyperlinkEvent.EventType.ACTIVATED)
				{
					try 
					{
						if (e.getDescription().endsWith(".html")&&e.getDescription().startsWith("http")==false) //is a footnote
						{
							editor.setPage(getClass().getResource(e.getDescription()));
							//System.out.println(getClass().getResource(e.getDescription()));
							//back button replaces input field while not in story; no need for it while viewing footnotes
							self.remove(input);
							self.add(back, BorderLayout.SOUTH); 
							self.revalidate();
							self.repaint();
						}
						else //is a website link; for accessing works cited
						{
							Desktop.getDesktop().browse(new URI(e.getURL().toString()));
						}
					}
					catch (Exception error) 
					{
						System.out.println("Error. Broken link: "+e.getDescription());
					} 
				}
			}
		}
		);
		
		//put editorpane inside a scrollpane to enable scrolling if needed
		scroll=new JScrollPane(editor);
		scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		
		//create area for player to type choice
		input=new JTextField();
		//ensures that when player presses enter, input is gathered and processed
		input.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e) //only actionevent in textfield should be enter key
			{
				String choice=input.getText();
				input.setText(""); //clear input field
				self.parseChoice(choice);
			}
		}
		);
		
		//to be used by the hyperlinklistener
		back=new JButton("Go Back");
		back.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed (ActionEvent e) //when button is pressed
			{
				//remove back button and bring back input field
				self.remove(back);
				self.add(input, BorderLayout.SOUTH); 
				self.revalidate();
				self.repaint();
				//then go back to the previous page
				self.updateGUI(); 
			}
		}
		);
		
		//for some reason the scroll bar starts at the bottom of the page by default; this forces it to the top
		//must be invoked later or it doesn't work
		SwingUtilities.invokeLater(() -> scroll.getVerticalScrollBar().setValue(0));

		//add the text and input area to the GUI
		this.add(scroll, BorderLayout.CENTER);
		this.add(input, BorderLayout.SOUTH);
	}
	
	//displays the next story text in the GUI, with the choices displayed beneath
	private void updateGUI ()
	{
		HashMap<Integer, Story> choices=story.getChoices(state);
		String append="<br><br>"; //since editorpane is set to display html
		for (Integer i: choices.keySet()) //choices always displayed in bold, to show they're special
		{
			String title=choices.get(i).getTitle();
			String result=parseRestriction(title); //check if choice is restricted to doctor or lawyer
			if (result!=null) //display altered choice text
			{
				append+="<strong>"+i+". "+result+"</strong><br>";
			}
			else //unrestricted choice; display it normally
			{
				append+="<strong>"+i+". "+title+"</strong><br>";
			}
		}
		editor.setText(state.getText()+append);
	}
	
	//given the title of a choice, determine whether it's restricted to doctor/lawyer or not
	private String parseRestriction (String title)
	{
		if (title.indexOf(")")!=-1) //is a  character specific choice
		{
			//System.out.println("Character specifc choice found.");
			int end=title.indexOf(")");
			String sub=title.substring(0, end+1);
			if (IS_DOCTOR==true) //player is doctor
			{
				//System.out.println("Player is doctor");
				if (sub.equals("(Requires Doctor)")) //choice unlocked
				{
					title=title.substring(end+2); 
					//System.out.println("Choice unlocked");
				}
				else //choice locked
				{
					title=sub; 
					//System.out.println("Choice locked");
				}
			}
			else //player is lawyer
			{
				//System.out.println("Player is lawyer");
				if (sub.equals("(Requires Doctor)")) //choice locked
				{
					title=sub; 
					//System.out.println("Choice locked");
				}
				else //choice unlocked
				{
					title=title.substring(end+2); 
					//System.out.println("Choice unlocked");
				}
			}
			return title; //either return the requirement, or return the choice if requirement is met
		}
		else
		{
			return null;
		}
	}
	
	//When the player gives input, try to match it to a valid choice and updateGUI with the new page
	private void parseChoice (String chose)
	{
		Integer num=null;
		try
		{
			num=Integer.valueOf(chose);
		}
		catch (NumberFormatException e)
		{
			return; //do nothing because player typed gibberish; wait for another input
		}
		//try to change state to the page/choice the player chose
		HashMap<Integer, Story> options=story.getChoices(state);
		if (options.containsKey(num))
		{
			Story temp=options.get(num); 
			//do logic valdiation first
			String name=parseRestriction(temp.getTitle());
			if (name==null||name.indexOf("(")==-1) //only update story if choice wasn't restricted
			{
				state=temp;
				this.updateGUI(); //then update page to reflect next part of the story
				SwingUtilities.invokeLater(() -> scroll.getVerticalScrollBar().setValue(0)); //force scrollbar to the top again
				if (state.getChoice()!=null) //something important is happening
				{
					if (state.getChoice().equals("IS_DOCTOR")) 
					{
						IS_DOCTOR=true;
					}
				}
			}
		}
		//if the number typed isn't a valid choice, do nothing and wait for a valid input
	}
	
	private static Story MakeAct1() //meet the characters and do worldbuilding
	{
		//methods return references to the last point in the story; needed to add edges between it and the next parts
		Story reference=MakeExposition(); 
		reference=MeetMacomb(reference);
		reference=MeetNeary(reference);
		reference=MeetMuldrewAlex(reference);
		return reference;
	}
	
	private static Story[] MakeAct2(Story start) //main plot; survive the dinosaurs
	{
		Story[] split=MakeArgument(start); //0 is muldrew path; 1 is macomb path
		//fight a pterosaur; possibility of death
		Story[] muld=MakeMuldrewPath(split[0]); //three outcomes; first two are good and last has muldrew dead
		Story[] mac=MakeMacombPath(split[1]); //three outcomes; first two are good and last has macomb dead
		//must fight dilophosaurus; equal chance of dying
		Story[] muld2=MakeMuldrewPath2(muld[0], muld[1]); //three outcomes; first two are good and last has muldrew dead
		Story[] alex=MakeAlexPath(muld[2]); //three outcomes; first two are good and last has alex dead
		Story[] mac2=MakeMacombPath2(mac[0], mac[1]); //three outcomes; first two are good and last has macomb dead
		Story[] solo=MakeSoloPath(mac[2]); //only two outcomes are player survival
		//if macomb or muldrew died to dilophosaurus
		Story alexAgain=MakeNewAlexPath(muld2[2]); //merge with regular alex path after minor differences
		Story bridge=MakeNewSoloPath(mac2[2]); //merge with regular solo path after minor differences
		//run from allosaurus and end the act
		Story muld3=MakeMuldrewPath3(muld2[0], muld2[1]); //further deaths no longer possible
		Story[] alex2=MakeAlexPath2(alex[0], alex[1], alexAgain); //alex lives in first and dies in second
		Story shadow=MakeDeathPath(alex[2]); //further deaths no longer possible
		Story mac3=MakeMacombPath3(mac2[0], mac2[1]); //further deaths no longer possible
		Story solo2=MakeSoloPath2(solo[0], solo[1], bridge); //further deaths no longer possible
		//the 6 paths lead to one of three outcomes
		//first two are good; middle two are okay; last two are bad
		Story[] toret=new Story[] {muld3, mac3, alex2[0], solo2, shadow, alex2[1]}; 
		return toret;
	}
	
	private static void MakeAct3(Story[] paths) //escape the last two dinosaurs, then meet hammett and leave
	{
		Story muld=MakeMuldrewEscape(paths[0]); 
		Story mac=MakeMacombEscape(paths[1]); //will eventually merge with above to form good ending
		Story alex=MakeAlexEscape(paths[2]); //power escape
		Story powerless=MakePowerlessEscape(paths[3]);
		Story death=MakeDeathEscape(paths[4], paths[5]);
		//four endings based on who died
		MakeGoodEnding(muld, mac); //no deaths
		MakePowerEnding(alex); //only muldrew dead
		MakePowerlessEnding(powerless); //only macomb dead
		MakeBadEnding(death); //both muldrew and alex dead
	}
	
	/*private static Story Name(Story start) //template
	{
		StringBuilder string=new StringBuilder(600); //STUPID BUG MAKES CLICKING LINK TWICE NOT WORK FIX
		string.append("");
		string.append("");
		string.append("");
		string.append("");
		string.append("");
		string.append("");
		string.append("");
		string.append("");
		string.append("");
		string.append("");
		string.append("");
		string.append("");
		string.append("");
		string.append("");
		string.append("");
		
		//same main text regardless of choice; choice only adds flavour
		StringBuilder prime=new StringBuilder(600);
		prime.append("");
		prime.append("");
		prime.append("");
		prime.append("");
		prime.append("");
		prime.append("");
		prime.append("");
		prime.append("");
		prime.append("");
		prime.append("");
		prime.append("");
		prime.append("");
		
		string.delete(0, string.length());
		string.append("");
		string.append("");
		string.append("");
		string.append("");
		string.append("");
		string.append("");
		string.append("");
		string.append("");
		string.append("");
		string.append("");
		string.append("");
		string.append("");
		string.append("");
		string.append("");
		string.append("");
		
		string.delete(0, string.length());
		string.append("");
		string.append("");
		string.append("");
		string.append("");
		string.append("");
		string.append("");
		string.append("");
		string.append("");
		string.append("");
		string.append("");
		string.append("");
		string.append("");
		string.append("");
		string.append("");
		string.append("");
		
		string.delete(0, string.length());
		string.append("");
		string.append("");
		string.append("");
		string.append("");
		string.append("");
		string.append("");
		string.append("");
		string.append("");
		string.append("");
		string.append("");
		string.append("");
		string.append("");
		string.append("");
		string.append("");
		string.append("");
		
		return null;
	}*/
	
	private static Story MakeExposition() //character selection plus the park's ad
	{
		//using string builder solely because it's easier to read than repeated concatenation
		StringBuilder string=new StringBuilder(600);
		string.append("Jack Hammett, CEO of NextGen, is on a quest to bring the dinosaurs back.");
		string.append("	Everyone said it was impossible, but he didn’t become the world’s first trillionaire by");
		string.append("	listening to people with less money than him. Now, after 7 years, he claims to have succeeded in");
		string.append("	filling his private island with cloned dinosaurs, and plans to open it to the public as a theme park.");
		string.append("	But first, he wants an expert’s opinion on the place, which is why he’s holding a private tour of it ");
		string.append("and sent an invite to you, a…");
		string.append("<br><br>(Type the number of your choice in the box below)");
		
		Story intro= new Story("Intro", string.toString());
		story.addVertex(intro);
		state=intro; //this is the start of the story; the first page
				
		StringBuilder prime=new StringBuilder(600);
		//same continuing text regardless of choice
		prime.append("<br><br>As unbelievable as his claim sounds, you accept Hammett’s invitation anyway. Worst case scenario, ");
		prime.append("you're getting paid to take an all expenses paid trip to a tropical island, and who wouldn't want that? ");
		prime.append("After dressing for the warm weather you were told to expect, you get on the luxurious private jet ");
		prime.append("Hammett has sent for you. The only thing you're allowed to bring with you is your phone, and only after ");
		prime.append("you sign 20 pages of waivers and non-disclosure agreements. Shortly after you begin your flight to the ");
		prime.append("island, the flatscreen television across from you begins to play a test commercial for the park.");
		
		string.replace(0, string.length(),"You are Kelly Sutter, a paleontologist and one of the world’s leading experts");
		string.append(" on the Jurassic period,");
		string.append("	able to identify plants and animals from the period with only a single look (a skill that makes you very fun at parties).");
		string.append("	You are the paragon of your field, the envy of your peers.");
		string.append("	If Hammett can get your approval of the park, he knows it’ll be equivalent to an endorsement from the entire scientific community.");
		
		Story chooseDoc=new Story("Doctor", string.toString()+prime.toString(), "IS_DOCTOR");
		
		string.replace(0, string.length(), "You are Aaron Brandt, an ex-JAG officer and one of the most respected lawyers in the nation,");
		string.append(" able to recite hundreds of laws from memory (a skill that’s helped you win every case you’ve ever taken). ");
		string.append("You are the paragon of your field, the envy of your peers. If Hammett can get your approval of the park, he knows");
		string.append(" its legal standing (and thus marketability) will be rock solid.");
		
		Story chooseLaw=new Story("Lawyer", string.toString()+prime.toString());
		
		//player chooses their character
		story.addVertex(chooseDoc);
		story.addEdge(intro, chooseDoc, 1);
		story.addVertex(chooseLaw);
		story.addEdge(intro, chooseLaw, 2);
		
		string.replace(0, string.length(),"The screen shows an aerial view of Isla Soleada, the lush tropical island ");
		string.append("where Hammett has built his park. A calm male voice begins to speak over drums playing in the background. ");
		string.append("“Take a trip to NextGen's Jurassic Preserve(™), and experience the world as it was 200 million years ago.”");
		string.append("<br><br>The man continues, sounding oddly proud of his next words, as if he helped build the park.");
		string.append(" “Trek through authentic prehistoric forests.” The camera zooms in on an aerial view of the forest, ");
		string.append("giving you a closer look at the densely packed trees, which are tall enough to completely block ");
		string.append("your view of the ground. Then the view switches to a ground-level shot of the forest’s canopy, ");
		string.append("slowly panning around while a layer of strings joins the drums. <br><br>You can see countless large ");
		string.append("shrubs, along with some smaller trees, all leafy and green. The dirt they’re growing in has what ");
		string.append("looks like pine cones scattered across it. You can’t help but notice the complete lack of flowers ");
		string.append("or grass, the only visible colours being varying shades of green and brown. But despite this ");
		string.append("<a href=\"flora.html\">oddity</a>, the scene is still quite tranquil looking.");
		
		//regardless of prior choice, watch the ad
		Story AD1=new Story("Continue", string.toString());
		story.addVertex(AD1);	
		story.addEdge(chooseDoc, AD1, 1);
		story.addEdge(chooseLaw, AD1, 1);
		
		string.replace(0, string.length(),"“Get an exclusive look at hundreds of reborn species,” the man says, ");
		string.append("while the next shot is shown. You see a small group of three—two men and a boy in a baseball hat ");
		string.append("who couldn’t be older than ten–in the forest, being led by a man dressed like a stereotypical safari guide, ");
		string.append("in khaki shorts, a polo shirt, and a pith hat. Surprisingly, only the boy looks like he’s enjoying himself, ");
		string.append("while the two men with him—an overweight one and a thinner one—respectively look annoyed and bored. ");
		string.append("Before you can wonder why the actors look so unenthusiastic, the camera pans to show what the group is watching.");
		string.append("<br><br>It’s a strange creature that looks like some kind of mutant bird. It’s the size of a raven, with a beak and ");
		string.append("black feathers, yet also has sharp teeth and a feathered tail <a href=\"birds.html\">uncharacteristic of birds</a>. ");
		string.append("Then you witness a quick montage of several other shots, while the music picks up in intensity. ");
		string.append("You see a lobster with antennae as long as its body, crawling around the bottom of a lake; ");
		string.append("a squid with spiky arms, hunting a nautilus; and at nighttime, a flying squirrel gliding from a tree ");
		string.append("in pursuit of its insect prey. Although the <a href=\"fauna.html\">unusual array of animals</a> is interesting, ");
		string.append("you can’t help but wonder where the dinosaurs are. As if on cue, the montage ends and the man speaks again.");
		
		Story AD2=new Story("Continue", string.toString());
		story.addVertex(AD2);
		story.addEdge(AD1, AD2, 1);
		
		string.delete(0, string.length());
		string.append("“And witness the return of the dinosaurs,” he says. You think he sounds a bit overdramatic, ");
		string.append("especially with the music reaching its climax, until you see the ensuing shots.<br><br>");
		string.append("First, you see what looks like a featherless, reptilian looking pelican with arms, soaring through ");
		string.append("the cloudless sky above the island. “Pterosaurs,” the man says, echoing what you were already ");
		string.append("thinking. It seems Hammett is trying to leave no doubt in the audience’s minds about what they’re seeing.");
		string.append("<br><br>Then, you see a man in scuba gear, swimming up close to a creature twice his size, that literally ");
		string.append("looks like the Loch Ness monster. “Plesiosaurs”, the man says, almost clarifies, as if aware of how the scene appears.");
		string.append("<br><br>The penultimate shot is of a dinosaur you recognise instantly, as would anyone. “Stegosauruses,” ");
		string.append("says the man, completing your thought. A <a href=\"reptiles.html\">stegosaurus</a> the size of an elephant is in ");
		string.append("the forest, grazing on what looks like ferns. Once you see it, any doubt about Hammett’s outrageous claims ");
		string.append("is erased from your mind; even a trillionaire couldn’t make CGI look that real.<br><br>");
		string.append("The commercial then ends by zooming out, again showing you that same aerial view of Isla Soleada. ");
		string.append("“Witness the rebirth of the Jurassic for yourself, only at NextGen’s Jurassic Preserve (™).” The ");
		string.append("park’s logo appears when the man speaks, superimposed onto the shot. It’s a red circle with the ");
		string.append("silhouette of the front half of a stegosaurus inside of it, with the words “Jurassic Preserve” ");
		string.append("displayed below it in large blocky letters. “You have to see it to believe it,” he concludes, and the ");
		string.append("screen suddenly turns off.");
		
		Story AD3=new Story("Continue", string.toString());
		story.addVertex(AD3);
		story.addEdge(AD2, AD3, 1);
		
		return AD3;
	}
	
	private static Story MeetMacomb(Story start) //arrive at island and talk to Macomb
	{
		StringBuilder string=new StringBuilder(600);
		string.append("You look out the window, and see that you’ve already arrived at your destination. Either the ");
		string.append("commercial was longer than you realised, or the jet must’ve flown very fast.<br><br>");
		string.append("Your view of the island looks the exact same as the ending shot from the commercial, though ");
		string.append("you can’t make out any dinosaurs from here. A long landing platform extends from the edge of the ");
		string.append("otherwise primeval looking island, and the jet sets down.<br><br>");
		string.append("A few moments later, the pilot lowers the jet’s door for you, the rungs in it serving as stairs. ");
		string.append("She informs you that she has other trips to make, but will be back in the evening to escort you off ");
		string.append("the island. Once you’ve safely exited, the door closes shut and the jet takes off, leaving you to ");
		string.append("take a look at your surroundings.");
		
		Story exit= new Story("Continue", string.toString());
		story.addVertex(exit);
		story.addEdge(start, exit, 1);
		
		string.delete(0, string.length());
		string.append("The first thing you notice is how <a href=\"climate.html\">warm</a> it is. Even though it's only spring ");
		string.append("time, it feels like the middle of summer, and you can feel yourself starting to sweat already.<br><br>");
		string.append("The only sign you’re at the park’s entrance are the tall stone arches in front of you, which have ");
		string.append("the park’s name emblazoned on them in big orange letters. Between the arches is the start of a");
		string.append("trail through the forest, but you can’t see where it leads.<br><br>");
		string.append("Before going anywhere, you look around to see if anyone was sent to greet you, and see nothing ");
		string.append("but trees and sporadically placed solar panels. Then you notice a small building hidden among ");
		string.append("the trees to your right, painted to blend in with its surroundings. You decide to walk towards it, ");
		string.append("hoping to find someone. <br><br>");
		string.append("You’ve barely taken more than a few steps when the door flings open and an oddly familiar ");
		string.append("looking thin man rushes out to meet you. He’s around your height, with messy dark hair, glasses, ");
		string.append("and a black shirt with a very low v-neck. He grins when he sees you looking, and extends his ");
		string.append("hand for you to shake.<br><br>");
		string.append("“Ivan Macomb,” he says. “Chief Technology Officer of Jurassic Preserve (™).” He says the ");
		string.append("trademark out loud.");
		
		Story meet=new Story("Continue", string.toString());
		story.addVertex(meet);
		story.addEdge(exit, meet, 1);
		
		StringBuilder prime=new StringBuilder(600);
		//same continuing text regardless of handshake
		prime.append("He eyes you briefly, then says “You must be the expert Hammett told us about. Looks like you ");
		prime.append("got the memo on the dress code.” He lowers his voice and adds “And I must say, you fill out ");
		prime.append("those clothes very nicely,” giving you a wink.");
		
		//choice 1
		string.delete(0, string.length());
		string.append("You shake his hand, finding his grip surprisingly firm.<br><br>");
		
		Story shake=new Story("Shake his hand", string.toString()+prime.toString());
		story.addVertex(shake);
		story.addEdge(meet, shake, 1);
		
		//choice 2
		string.delete(0, string.length());
		string.append("He raises an eyebrow, but lowers his hand without comment.<br><br>");
		
		Story noshake=new Story("Don't shake his hand", string.toString()+prime.toString());
		story.addVertex(noshake);
		story.addEdge(meet, noshake, 2);
		
		//regardless of reaction, begin tour
		prime.delete(0, prime.length());
		prime.append("“Anyway, we should start the tour then,” Macomb says. He walks over to the arches and ");
		prime.append("stands under them before spreading out his arms and saying theatrically “Welcome to Jurassic Preserve!”");
		prime.append("<br><br>The moment is undercut by the fact that you two are still the only ones there, standing alone at ");
		prime.append("the entrance to a giant forest, which now that you think of it, is oddly quiet, devoid of any ");
		prime.append("birdsong or sounds of movement.<br><br>");
		prime.append("Macomb notices your lack of reaction and smiles, amused. “Sorry, but I’m contractually ");
		prime.append("obligated to say that.”<br><br>");
		prime.append("“Really?” you ask.<br><br>");
		prime.append("He laughs. “No, not really. I just really wanted to say that for some reason. Now come on, let’s ");
		prime.append("get going.”");
		
		//choices are same regardless of previous choice
		//choice 1
		string.delete(0, string.length());
		string.append("“I’m not interested,” you say bluntly.<br><br>");
		string.append("Macomb takes your rejection in stride, saying with an easygoing smile “Hey, I was just speaking ");
		string.append("my mind. I didn’t mean to give you the wrong impression.”<br><br>");
		
		Story reject=new Story("Shut him down", string.toString()+prime.toString());
		story.addVertex(reject);
		story.addEdge(shake, reject, 1);
		story.addEdge(noshake, reject, 1);
		
		//choice 2
		string.delete(0, string.length());
		string.append("Openly taking a closer look at his low-cut shirt, you notice that his unexpectedly toned chest is ");
		string.append("glistening with sweat from the heat. You also lower your voice, and say “I could say the same thing about you, ");
		string.append("Mr. Macomb.”<br><br>");
		string.append("Macomb grins, and says “Oh, we’re going to get along just fine. But please, drop the ‘mister’. ");
		string.append("Mr. Macomb is my father.”<br><br>");
		
		Story flirt=new Story("Flirt back", string.toString()+prime.toString());
		story.addVertex(flirt);
		story.addEdge(shake, flirt, 2);
		story.addEdge(noshake, flirt, 2);
		
		//choice 3
		string.delete(0, string.length());
		string.append("Ignoring his compliment, you say “Yes, I’m the expert who’s here to tour the park.” You stress ");
		string.append("those last two words, implicitly reminding him to focus.<br><br>");
		string.append("Macomb clears his throat and nods awkwardly, understanding your meaning. “Right, of course.”<br><br>");
		
		Story ignore=new Story("Focus on the tour", string.toString()+prime.toString());
		story.addVertex(ignore);
		story.addEdge(shake, ignore, 3);
		story.addEdge(noshake, ignore, 3);

		string.delete(0, string.length());
		string.append("Macomb starts walking down the trail, and you follow him, noticing how quiet the forest is. When you ");
		string.append("look around, you don’t see much beyond some insects and a few small reptiles, all of which stay ");
		string.append("out of your way. However, there are also lamp posts periodically placed along the trail, with solar panels ");
		string.append("attached to them. Clearly Hammett expects to offer tours during the nighttime.<br><br>");
		string.append("Macomb glances back at you and notices you looking around. He smirks and asks “You’re wondering ");
		string.append("where the dinosaurs are, aren’t you? We don’t let them roam free, obviously. They’re dinosaurs ");
		string.append("after all.”<br><br>");
		string.append("“‘We’?” you ask. That was the second time he’s alluded to other people, yet he’s the only person ");
		string.append("you’ve even seen on the island so far.<br><br>");
		string.append("He responds with amusement. “What, you thought I was the only guy here? I’m not going to ");
		string.append("show you the whole island all by myself. We’re going to meet the others, then the real tour will start.”");
		string.append("<br><br>The two of you fall into silence as you continue walking, and you figure you have time to ask ");
		string.append("him another question.");
		
		Story walk=new Story("Continue", string.toString());
		story.addVertex(walk);
		story.addEdge(reject, walk, 1);
		story.addEdge(flirt, walk, 1);
		story.addEdge(ignore, walk, 1);
		
		//then player has 4 different conversation combinations possible, or can skip ahead to next part	
		//player can ask up to 2 questions before main story continues
		string.delete(0, string.length());
		string.append("“Pay no attention to the man behind the curtain,” Macomb quips, before looking back at you and ");
		string.append("explaining.<br><br>");
		string.append("“It’s the control tower, for landings. You know, since the only way on or off the island is by ");
		string.append("plane. Since Hammett insists on making this place as ‘authentically Jurassic’ as possible, he ");
		string.append("camouflaged the control tower, since there’s no way to build one from sticks and stones.” He ");
		string.append("doesn’t sound like he very much appreciates Hammett’s dedication.<br<br>");
		string.append("“Is there something wrong with that?” you ask him.<br><br>");
		string.append("Macomb rolls his eyes. “There wouldn’t be, if he hadn’t also refused to build a normal runway. ");
		string.append("That retractable one we have took twice as long to build.”");
		
		Story Qshack=new Story("“What was that weird shack you were in earlier?”", string.toString());
		story.addVertex(Qshack);
		story.addEdge(walk, Qshack, 1);
		
		string.delete(0, string.length());
		string.append("Macomb laughs when he hears your question. “Hey, that’s what I said to Hammett. I have ");
		string.append("a very important job to do, but he needed someone with good looks and a charming personality ");
		string.append("to put his VIP at ease.”<br><br>");
		string.append("He sees the look you’re giving him, and smiles, shrugging. “Heh. Seriously though, we drew straws ");
		string.append("for it. I lost.”<br><br>");
		string.append("Confused, you ask him “Why were you even in the drawing if your job is so important?”<br><br>");
		string.append("Macomb explains with another shrug, “We all have important jobs, but Hammett wanted ");
		string.append("someone familiar with the island to greet you. He didn’t care who, so we decided to draw for it to ");
		string.append("make it fair.” You get the feeling that this isn’t Macomb’s first time getting the short straw.");
		
		Story Qjob=new Story("“Why is my tour guide the ‘Chief Technology Officer’?”", string.toString());
		story.addVertex(Qjob);
		story.addEdge(walk, Qjob, 2);
		
		string.delete(0, string.length());
		string.append("You and Macomb finally reach the end of the lengthy path through the forest, which ends in a ");
		string.append("large clearing. In the middle of it is a large building that looks like a museum with a dome-like roof, ");
		string.append("though it’s made out of wood and stone instead of marble. In contrast to its primitive materials, the building's ");
		string.append("roof is almost completely covered with solar panels. There’s a lake in front of the building, and ");
		string.append("at the edges of the clearing, there are several more paths leading deeper into the forest, each labelled with ");
		string.append("signs that are too far away for you to read. There are also more solar powered lamp posts spread across the clearing.<br><br>");
		string.append("Macomb turns to you. “This is the Visitor’s Center. Pretty nice, huh? I designed it myself. It’s ");
		string.append("100% made of Jurassic period materials. Except for the functioning toilets and stuff, obviously.” ");
		string.append("He seems genuinely proud of the way it turned out, especially given Hammett’s constraints.<br><br>");
		string.append("“It’s even better on the inside. Come on.” You follow him closer to the building, and an irate ");
		string.append("voice suddenly calls out from behind you. “Macomb!”<br><br>");
		string.append("You both turn around and see an overweight man running towards you, stumbling and wheezing, ");
		string.append("clearly out of breath. The first thing you notice about him is his garish Hawaiian shirt, which is ");
		string.append("bright blue, and covered with green lilypads that have pink flamingos perched on them. Like ");
		string.append("Macomb, he has glasses and dark hair, though his hair is much neater. And like Macomb, he ");
		string.append("looks oddly familiar to you. For some reason, he's holding a bright red electric guitar.<br><br>");
		string.append("Macomb is unfazed by the man’s glare, and says flippantly “Neary, can’t you see we have ");
		string.append("company? Hammett’s esteemed guest is here. You should at least say hello before you start ");
		string.append("yelling at me again.”");
		
		Story end=new Story("End the conversation", string.toString());
		story.addVertex(end);
		story.addEdge(walk, end, 3);
		story.addEdge(Qshack, end, 3);
		story.addEdge(Qjob, end, 3);
		
		string.delete(0, string.length());
		string.append("“What’s so bad about that?” you ask him, not understanding his annoyance.<br><br>");
		string.append("“Because,” he says, trying to make you understand, “I had to design it, I had to order the ");
		string.append("materials, and I had to oversee the contractors who built it. And that’s on top of having to design ");
		string.append("the park, which obviously, I haven’t finished yet.” His annoyance seems to stem from him not ");
		string.append("having enough time to do everything, rather than any discontent with his job.<br><br>");
		string.append("You recall the minimalistic looking entrance to the park, nothing more than those arches the two ");
		string.append("of you walked through. Given Macomb’s personality, it’s pretty obvious that the completed ");
		string.append("design is meant to have more flair.<br><br>");
		string.append("“Can’t you delegate some of the work to someone else?” you ask, figuring the Chief Technology ");
		string.append("Officer must have subordinates to rely on.<br><br>");
		string.append("Macomb unexpectedly laughs. “Yeah, right. As if either of them knows the first thing about ");
		string.append("architecture or engineering. Even if they did, they’re just as busy as I am.”<br><br>");
		string.append("You notice his use of the word ‘either’, and are further confused. But before you can say ");
		string.append("anything about it, you arrive at your destination.");
		
		Story Qproblem=new Story("Ask him what the big deal is", string.toString());
		story.addVertex(Qproblem);
		story.addEdge(Qshack, Qproblem, 1);
		story.addEdge(Qproblem, end, 1);
		
		string.delete(0, string.length());
		string.append("“It doesn’t sound like you like Hammett too much,” you surmise.<br><br>");
		string.append("Macomb sighs a little. “Don’t get me wrong, he’s kind of a perfectionist, and really demanding. ");
		string.append("It can be exhausting sometimes. But I already knew all that when I took this job, and I love a ");
		string.append("good challenge. Being in charge of the design for an island full of dinosaurs was too good to pass up.”");
		string.append("<br><br>Macomb grins. “Besides, I’d do almost anything for this kind of money. I don’t get much time ");
		string.append("off, but nothing is more relaxing than spending a few hours in my yacht.”<br><br>");
		string.append("He sees the surprise on your face, and his grin widens. “You know, maybe you could join me some time.”");
		string.append("<br><br>Before you can respond to him, you finally arrive at your destination.");
		
		Story Qboss=new Story("Ask him if he likes his boss", string.toString());
		story.addVertex(Qboss);
		story.addEdge(Qshack, Qboss, 2);
		story.addEdge(Qjob, Qboss, 2);
		story.addEdge(Qboss, end, 1);
		
		string.delete(0, string.length()); //ask how jobs are equal, from qjob to this is 1
		string.append("You decide to get him to clarify, and say “The Chief Technology Officer is the same level of ");
		string.append("importance as everyone else? Hammett really couldn’t get anyone less busy to show me around?”<br><br>");
		string.append("Macomb shakes his head. “Like I said, Hammett didn’t care which one of us did it. He doesn’t, ");
		string.append("uh…” He pauses, trying to think of how to phrase his next words. “He doesn’t micromanage.”<br><br>");
		string.append("“That doesn’t make any sense. Couldn’t you have just gotten one of your assistants to do it?” ");
		string.append("You assume that the Chief Technology Officer could afford to spare one of his subordinates for a day.<br><br>");
		string.append("Macomb unexpectedly laughs. “I appreciate the fact that you think I’m in charge, but don’t let ");
		string.append("the other two hear you say that, or they’ll think I put you up to it.”<br><br>");
		string.append("You notice his mention of ‘the other two’, and are confused by his apparent downplaying of his ");
		string.append("Chief Officer rank. But before you can say anything about it, you arrive at your destination.");
		
		Story Qequal=new Story("Ask him how everyone’s jobs can be equally important", string.toString());
		story.addVertex(Qequal);
		story.addEdge(Qjob, Qequal, 1);
		story.addEdge(Qequal, end, 1);
		
		return end;
	}
	
	private static Story MeetNeary(Story start) //resolve neary and macomb's argument and talk to them (or not)
	{
		StringBuilder string=new StringBuilder(600);
		string.delete(0, string.length());
		string.append("The irate Neary refuses, barely even looking at you. Still panting, he holds up the guitar and ");
		string.append("huffs “How many times have I told you to clean up after yourself when you leave the substation?”<br><br>");
		string.append("Macomb is unfazed by Neary’s surliness, probably because he’s already used to it. “Neary, ");
		string.append("you’re literally the janitor,” he deadpans.<br><br>");
		string.append("Neary curses. “Macomb, I almost tripped on your stupid guitar again. Do you want to risk ");
		string.append("another accident?!”<br><br>");
		string.append("Macomb rolls his eyes at the electric guitar Neary is still holding. “She’s literally bright red; how ");
		string.append("can you even miss her? Besides, you should know to look out for her by now. What is this, the ninth ");
		string.append("time we’ve had this argument?”<br><br>");
		string.append("Neary huffs, looking as if he’s rapidly approaching his limit. “It doesn’t matter how bright it is if ");
		string.append("you leave it right by the door! Besides, you didn’t even unplug the cable, and I almost tripped ");
		string.append("over that too. I shouldn’t have to look out for your stupid guitar because it shouldn’t even be in ");
		string.append("the substation in the first place!”<br><br>");
		string.append("Macomb is finally starting to look annoyed. “That substation is my workplace Neary, not yours. I ");
		string.append("can do whatever I want in there. If you can’t get used to that, maybe you should just stop coming by.”");
		string.append("<br><br>You can tell that this argument is not going well, and probably won’t get any better unless you intervene.");
		
		Story argument=new Story("Continue", string.toString());
		story.addVertex(argument);
		story.addEdge(start, argument, 1);
		
		//5 choices for player; choice 1
		string.delete(0, string.length());
		string.append("You clear your throat to gain their attention, then say “Hello. Remember me? I’m the expert Hammett invited ");
		string.append("to tour the island.” You put emphasis on that last part of the sentence.<br><br>");
		string.append("Both men seem to finally remember that you’re here, and go quiet. Neary takes a look at you for ");
		string.append("the first time, and flushes from embarrassment, mumbling “Ah, hi.” After an awkward pause, he ");
		string.append("holds up the guitar before either you or Macomb can say anything, and speaks stiffly. “Excuse");
		string.append("me, I need to return this. Enjoy the tour.”<br><br>");
		string.append("He shoots Macomb a glare, as if to say “this isn’t over”, then scurries off, going back to the path ");
		string.append("he arrived from, and moving surprisingly fast for such a large man. You can just barely make out ");
		string.append("the words on the path’s sign: “Substation”, with “EMPLOYEES ONLY” written below it in big ");
		string.append("red letters.<br><br>");
		string.append("Now you and Macomb are alone in the clearing once again.<br><br>");
		string.append("Macomb looks at you, uncharacteristically awkward, and says “Let’s just forget that happened, ");
		string.append("okay?” He starts walking inside the Visitor’s Center, and you follow.");

		Story ignore=new Story("Focus on the tour", string.toString());
		story.addVertex(ignore);
		story.addEdge(argument, ignore, 1);
		
		//choice 2
		string.delete(0, string.length());
		string.append("“It’s Neary, right? Would it kill you to just check the floor before you walk in? That isn't ");
		string.append("too difficult,” you tell him.<br><br>");
		string.append("Macomb nods at you, clearly appreciating the support.<br><br>");
		string.append("Neary makes no attempt to hide his indignation. “What? This is common sense, and Macomb ");
		string.append("knows it. That substation controls all the park’s power. If any accident happens in there, it’d ");
		string.append("cause an island-wide blackout again! What kind of expert are you, anyway?”<br><br>");
		string.append("“Well—” you start before being cut off by Macomb.<br><br>");
		string.append("“Sorry, Neary. But it’s two against one,” he says, unable to hide a hint of vindication.<br><br>");
		string.append("Neary sputters, then huffs “Fine, whatever. You win, Macomb. I’ll put your stupid guitar back, ");
		string.append("but don’t blame me if this causes an accident someday.”<br><br>");
		string.append("Then he scurries off, going back to the path he arrived from, moving surprisingly fast for such a ");
		string.append("large man. You can just barely make out the words on the path’s sign: “Substation”, with ");
		string.append("“EMPLOYEES ONLY” written below it in big red letters.<br><br>");
		string.append("Now you and Macomb are alone in the clearing once again.");
		
		Story defendMacomb=new Story("Side with Macomb", string.toString());
		story.addVertex(defendMacomb);
		story.addEdge(argument, defendMacomb, 2);
		
		//choice 3
		string.delete(0, string.length());
		string.append("“Macomb, would it kill you to put your guitar away when you leave your workspace? Or at least ");
		string.append("not leave it by the door?” you ask him.<br><br>");
		string.append("Neary makes no attempt to hide his smugness. “Well Macomb, sounds like it’s two against one.”<br><br>");
		string.append("Macomb looks at you, almost disappointed, but quickly hides it and mutters “Alright, fine. You ");
		string.append("win, Neary. I’ll be more careful from now on. Happy? Now give me back Sarah and I’ll clean up ");
		string.append("the substation for you.”<br><br>");
		string.append("Neary shakes his head, finally calming down. He manages to sound less smug when he speaks ");
		string.append("this time. “No, I’ll handle it. I have to go back anyway since I left Lewis behind.” Macomb rolls ");
		string.append("his eyes at the mention of Lewis, but nods.<br><br>");
		string.append("Neary looks at you for the first time and smiles with a mixture of gratitude and pride, saying “Hi. ");
		string.append("Donald Neary, head scientist.” He extends his hand for you to shake.");
		
		Story defendNeary=new Story("Side with Neary", string.toString());
		story.addVertex(defendNeary);
		story.addEdge(argument, defendNeary, 3);
		
		//choice 5
		string.delete(0, string.length());
		string.append("Neary chuckles mockingly. “Oh sure, and then how many days will it take for you to beg me to ");
		string.append("come back and clean up after you? We both know it won’t even be two weeks before you can’t ");
		string.append("see over the heaps of ramen cups, you slob.”<br><br>");
		string.append("Macomb looks like he knows Neary is right, but refuses to back down. “You wanna bet on that, smart guy?”");
		string.append("<br><br>“Fine. How does $10,000 sound?” Neary asks, clearly thinking it’s a sure thing based on his ");
		string.append("smug tone and the shocking amount of money he’s offering.<br><br>");
		string.append("The two of them shake hands, Macomb looking irritated and stubborn, and Neary looking smug ");
		string.append("and almost greedy. Then they seem to remember that you’re there, and Macomb clears his throat ");
		string.append("awkwardly while Neary shifts in place, still holding Macomb’s guitar.<br><br>");
		string.append("“Uh, sorry about that,” Macomb says. “This is Donald Neary, our head scientist.” He gestures to ");
		string.append("Neary, trying to act as if they hadn’t just been arguing a minute ago.<br><br>");
		string.append("Neary takes a look at you for the first time, and flushes from embarrassment, mumbling “Ah, hi.” ");
		string.append("After an awkward pause, he holds up the guitar before either you or Macomb can say anything, ");
		string.append("and speaks stiffly. “Excuse me, I need to return this. Enjoy the tour.”<br><br>");
		string.append("Then he scurries off, going back to the path he arrived from, moving surprisingly fast for such a ");
		string.append("large man. You can just barely make out the words on the path’s sign: “Substation”, with ");
		string.append("“EMPLOYEES ONLY” written below it in big red letters.<br><br>");
		string.append("Now you and Macomb are alone in the clearing once again.<br><br>");
		string.append("Macomb looks at you, uncharacteristically awkward, and says “Let’s just forget that happened, ");
		string.append("okay?” He starts walking inside the Visitor’s Center, and you follow.");
		
		Story silence=new Story("Don't intervene", string.toString());
		story.addVertex(silence);
		story.addEdge(argument, silence, 5);
		
		//choice 4; lawyer only
		string.delete(0, string.length());
		string.append("You clear your throat to gain their attention, then say “OSHA Occupational Health and Safety ");
		string.append("Standards, part 1910, section 37, subsection a, paragraph 3. Quote. Exit routes must be free and ");
		string.append("unobstructed. No materials or equipment may be placed, either permanently or temporarily, ");
		string.append("within the exit route. End quote.”<br><br>");
		string.append("Both men are staring at you like you just started speaking in a foreign language, but you aren’t ");
		string.append("done yet. “Macomb, it sounds like the substation only has a single entrance, or else Neary ");
		string.append("would’ve taken another door.” Neary nods, grinning gleefully as he belatedly understands that ");
		string.append("you’re siding with him.<br><br>");
		string.append("You find yourself slipping into lawyer mode out of habit. “If the entrance and the exit are the ");
		string.append("same, Macomb, your guitar would be considered an ‘obstruction’, and shouldn’t be there even if ");
		string.append("the workplace is ‘yours’. Serious violations of OSHA standards can incur fines of up to $16,550, ");
		string.append("and willful violations can incur fines of up to $165,514. Consider this a warning. Now any ");
		string.append("violation after this point would be considered willful.”<br><br>");
		string.append("Macomb appears to be struggling to decide whether he’s more impressed or annoyed by you. ");
		string.append("Neary is cackling ungraciously.");
		
		Story scold=new Story("(Requires Lawyer) Scold Macomb for being irresponsible", string.toString());
		story.addVertex(scold);
		story.addEdge(argument, scold, 4);
		
		//two further lawyer exclusive choices
		string.delete(0, string.length());
		string.append("Disapproving of his actions too, you turn your serious lawyer face on Neary.<br><br>");
		string.append("“Neary, if this was a court of law, you’d be held in contempt,” you tell him, and he freezes ");
		string.append("mid-cackle, clearly caught off guard.<br><br>");
		string.append("“The United States Code of Law, Title 18, Section 401. Quote. A court of the United States shall ");
		string.append("have power to punish by fine or imprisonment, or both, at its discretion, such contempt of its ");
		string.append("authority. End quote. This includes—quote—misbehaviour of any person in its presence. End ");
		string.append("quote. I don’t think bursting out into laughter is appropriate behaviour, do you?”<br><br>");
		string.append("Neary is staring at you, and can only sputter. Macomb has decided you’re more impressive than ");
		string.append("annoying, and is trying to hide a smile.<br><br>");
		string.append("Macomb answers Neary’s unasked question, and introduces you with a faint hint of amusement ");
		string.append("in his voice. “Neary, this is Aaron Brandt, the legal expert Hammett invited to tour the island.” ");
		string.append("He puts emphasis on “legal expert”.<br><br>");
		string.append("You nod at him and he narrows his eyes for a moment before nodding back, the both of you calming down together.");
		
		Story doubleScold=new Story("Scold Neary too", string.toString());
		story.addVertex(doubleScold);
		story.addEdge(scold, doubleScold, 1);
		
		string.delete(0, string.length());
		string.append("You remain silent, allowing Neary to continue laughing at Macomb. Neary eventually catches his ");
		string.append("breath and manages to speak, sounding smug. “Well Macomb, sounds like it’s two against one.”<br><br>");
		string.append("Macomb has decided you’re more annoying than impressive, and sighs. “Alright, fine. You win, Neary. ");
		string.append("I’ll be more careful from now on. Happy? Now give me back Sarah and I’ll clean up the ");
		string.append("substation for you.”<br><br>");
		string.append("Neary shakes his head, finally calming down. He manages to sound less smug when he speaks ");
		string.append("this time. “No, I’ll handle it. I have to go back anyway since I left Lewis behind.” Macomb rolls ");
		string.append("his eyes at the mention of Lewis, but nods.<br><br>");
		string.append("Neary looks at you for the first time and smiles with a mixture of gratitude and pride, saying “Hi. ");
		string.append("Donald Neary, head scientist.” He extends his hand for you to shake.");
		
		Story noScold=new Story("Say nothing", string.toString());
		story.addVertex(noScold);
		story.addEdge(scold, noScold, 2);
		
		//after scolding macomb or siding with neary, another choice
		string.delete(0, string.length());
		string.append("You shake Neary’s hand, feeling traces of sweat on his palm as his thick hand envelops yours. ");
		string.append("Before you can speak, Neary enthuses “Actually, I already know exactly who you are. We’ve had ");
		string.append("a bet going for months on who Hammet was going to invite, and you were my pick.” He sounds ");
		string.append("proud to have been the one to make the correct prediction.<br><br>");
		string.append("Speaking as if letting you in on a secret, yet making no attempt to prevent Macomb from ");
		string.append("overhearing, Neary says “By the way, Macomb over here thought Hammett was going to invite a ");
		string.append("mathematician! Can you believe that? As if a mathematician could have anything useful to say ");
		string.append("about dinosaurs!” He chuckles in disbelief.<br><br>");
		string.append("Macomb gives Neary an unamused look. “Yeah, yeah, laugh it up. I’ll pay you after the tour’s ");
		string.append("over, okay?” Then he looks at you. “Speaking of, I’m going to head inside the Center now. Come ");
		string.append("in when you’re done here.”<br><br>");
		string.append("Now you and Neary are alone in the clearing.");
		
		Story shake=new Story("Shake Neary's hand", string.toString());
		story.addVertex(shake);
		story.addEdge(noScold, shake, 1);
		story.addEdge(defendNeary, shake, 1);
		
		string.delete(0, string.length());
		string.append("You refuse to shake Neary’s hand, and his face darkens into a frown when he realises that he ");
		string.append("mistook your previous actions for support. “Well, nice meeting you too. Enjoy your tour,” he ");
		string.append("says, unable to hide a hint of sarcasm despite his obvious annoyance. Macomb gives you a ");
		string.append("puzzled look at your rudeness to Neary, but says nothing.<br><br>");
		string.append("Neary turns around and heads back down the path he arrived from, moving surprisingly fast for ");
		string.append("such a large man. You can just barely make out the words on the path’s sign: “Substation”, with ");
		string.append("“EMPLOYEES ONLY” written below it in big red letters.<br><br>");
		string.append("You and Macomb are alone in the clearing once again.<br><br>");
		string.append("Macomb looks at you, uncharacteristically awkward, and says “Let’s just forget that happened, ");
		string.append("okay?” He starts walking inside the Visitor’s Center, and you follow.");

		Story noShake=new Story("Don't shake Neary's hand", string.toString());
		story.addVertex(noShake);
		story.addEdge(noScold, noShake, 2);
		story.addEdge(defendNeary, noShake, 2);
		
		//if neary left, can ask macomb questions
		string.delete(0, string.length());
		string.append("“So, what was that all about?” you ask Macomb, unable to let the argument go unacknowledged.<br><br>");
		string.append("Macomb looks sheepish, though it’s hard to tell if it’s because he regrets his words to Neary, or if ");
		string.append("he’s just embarrassed you witnessed it. “Uh, it’s not that big of a deal. We…do that sometimes. ");
		string.append("Well, a lot actually.”<br><br>");
		string.append("“You mean you two argue about your guitar a lot, or you two argue a lot in general?” you ask.<br><br>");
		string.append("Macomb chuckles a little, his embarrassment starting to subside. “Both, actually. Neary and I’ve ");
		string.append("known each other for a long time, and we tend to get on each others’ nerves a lot. Though it’s ");
		string.append("usually about less important stuff, like whether Star Wars is better than Star Trek.” He murmurs ");
		string.append("“Star Wars is totally better, by the way.”");

		Story macombConvo=new Story("Ask Macomb about the argument", string.toString());
		story.addVertex(macombConvo);
		story.addEdge(defendMacomb, macombConvo, 1);
		
		//can end questioning at any time
		string.delete(0, string.length());
		string.append("You clear your throat and say “By the way, I still haven’t seen any dinosaurs yet.”<br><br>");
		string.append("Macomb nods at you and says “Right, we better get back on schedule. Hammett will kill me if ");
		string.append("we don’t finish the tour before he comes by tonight.” He starts walking inside the Visitor's ");
		string.append("Center, and you follow.");
		
		Story macombEnd=new Story ("Focus on the tour", string.toString());
		story.addVertex(macombEnd);
		story.addEdge(macombConvo, macombEnd, 3);
		story.addEdge(defendMacomb, macombEnd, 2);
		
		//macomb question 1
		string.delete(0, string.length());
		string.append("“Why do you argue about your guitar so much anyway?” you ask him.<br><br>");
		string.append("Macomb sighs softly and puts his hand on the back of his neck, looking away from you as he ");
		string.append("talks. “Neary doesn’t really understand my connection with Sarah, and just sees her as an ");
		string.append("obstacle to clean around, but playing her helps me think, okay? I’ve had her since I was a ");
		string.append("teenager, and I’ve had some of my best ideas thanks to her.” He gestures vaguely at one of the ");
		string.append("signs in the distance, whose text you can’t make out. “You know, in engineering school, they don’t ");
		string.append("exactly teach you how to design holding pens for dinosaurs.”<br><br>");
		string.append("“‘She’? You named your guitar Sarah?” you ask.<br><br>");
		string.append("Macomb shrugs. “Like I said, I’ve had her since I was a teenager. I’ve always been a B.B. King ");
		string.append("fan, so I wanted to name my guitar too. I just liked the sound of the name Sarah.”");

		Story askGuitar=new Story("Ask Macomb about his guitar", string.toString());
		story.addVertex(askGuitar);
		story.addEdge(macombConvo, askGuitar, 1);
		story.addEdge(askGuitar, macombEnd, 3);
		
		string.delete(0, string.length());
		string.append("“B.B. King? Never heard of him,” you say, completely serious.<br><br>");
		string.append("Macomb gives you a half-joking scoff. “You’ve never heard of the King of the Blues before? ");
		string.append("He’s only a Rock and Roll Hall of Famer who played dozens of concerts every year for 70 ");
		string.append("years!” He says this as if it’s something everyone should know.<br><br>");
		string.append("“You really need to get out more. All work and no play makes Jack a dull boy,” he says dryly, ");
		string.append("referencing your dedication to your field.<br><br>");
		string.append("Before you can retort, Macomb suddenly curses, looking as if he just remembered something ");
		string.append("important. He looks at you sheepishly and says “I almost forgot, we’ve barely even started the ");
		string.append("tour. We have to get back on schedule or Hammett will kill me. But I promise we’ll have time for ");
		string.append("more questions at the end. Come on.” He motions for you to follow as starts walking inside the ");
		string.append("Visitor's Center, and you do.");

		Story askBB=new Story("Ask who B.B. King is", string.toString());
		story.addVertex(askBB);
		story.addEdge(askGuitar, askBB, 1);
		
		string.delete(0, string.length());
		string.append("“Since you were a teenager? Then you must be good at it, right?” you say.<br><br>");
		string.append("Macomb smiles coyly. “Well, I dabble, really. But I’ve picked up a thing or two over the years, ");
		string.append("and I’d like to think I’m pretty decent.” Despite his faux-humble tone, you can tell he is ");
		string.append("genuinely proud of his abilities.<br><br>");
		string.append("Curious, you ask “Are you saying you’re self taught?”<br><br>");
		string.append("Macomb’s smile widens a bit. “Yeah, I guess I kind of am. I haven’t gone to guitar lessons since ");
		string.append("I was 17, but I’ve definitely learned a lot since then.” He shrugs, as if it isn’t that big of a deal.");
		string.append("<br><br>Before you can respond, Macomb suddenly curses, looking as if he just remembered something ");
		string.append("important. He looks at you sheepishly and says “I almost forgot, we’ve barely even started the ");
		string.append("tour. We have to get back on schedule or Hammett will kill me. But I promise we’ll have time for ");
		string.append("more questions at the end. Come on.” He motions for you to follow as starts walking inside the ");
		string.append("Visitor's Center, and you do.");
		
		Story askSkill=new Story("Ask if he's good at playing the guitar", string.toString());
		story.addVertex(askSkill);
		story.addEdge(askGuitar, askSkill, 2);
		
		//macomb question 2
		string.delete(0, string.length());
		string.append("Curious, you ask him “You and Neary are friends?”<br><br>");
		string.append("Macomb laughs. “Yeah, I know. Muldrew says we live a ‘cat and dog life’. It’s not as bad as it ");
		string.append("looks though, promise. The only reason we fight so much is because we’re so close.”<br><br>");
		string.append("“How long have you known each other?” you ask, inferring that Muldrew is another of his coworkers.<br><br>");
		string.append("He pauses for a moment. “13 years now, I think. Almost twice as long as I’ve known Muldrew. ");
		string.append("We’ve been through a lot together. Maybe this is corny for me to say–and don’t you dare tell him ");
		string.append("I said it–but Neary feels like the brother I never had.”<br><br>");
		string.append("After another pause, he becomes a bit self conscious, saying with a sheepish smile “Sorry, I don’t know why ");
		string.append("I’m telling you all of this. I guess it’s obvious that this is my first time playing tour guide.”");
		
		Story askFriends=new Story("Ask Macomb if he's friends with Neary", string.toString());
		story.addVertex(askFriends);
		story.addEdge(macombConvo, askFriends, 2);
		story.addEdge(askFriends, macombEnd, 3);
		
		string.delete(0, string.length());
		string.append("You smile at him and say “Don’t worry, this is my first time being a tourist at an island full of ");
		string.append("dinosaurs. We’re both new at this.”<br><br>");
		string.append("Macomb smiles back, with a hint of genuine affection in his eyes. “Yeah, I guess so. The fact that ");
		string.append("you’re a pretty good listener probably helps too.”<br><br>");
		string.append("Before you can respond, Macomb suddenly facepalms, looking as if he just remembered ");
		string.append("something important. He groans a little and says “Wait, the dinosaurs! I almost forgot, we’ve ");
		string.append("barely even started the tour. We have to get back on schedule or Hammett will kill me. But I ");
		string.append("promise we’ll have time for more questions at the end. Come on.” He motions for you to follow ");
		string.append("as starts walking inside the Visitor's Center, and you do.");
		
		Story nice=new Story("Be kind", string.toString());
		story.addVertex(nice);
		story.addEdge(askFriends, nice, 1);
		
		string.delete(0, string.length());
		string.append("“Oh? And here I thought it was normal for tour guides to give out their whole life stories during the ");
		string.append("tour.” You couldn't sound any more sarcastic if you tried.<br><br>");
		string.append("Macomb looks more sheepish but still manages to retort with a playful scoff “Hey, you were the ");
		string.append("one who kept asking me questions. I was just being polite.”<br><br>");
		string.append("You are unable to deny that he has a point.<br><br>");
		string.append("Macomb smirks a little. “Don’t worry, I promise you’ll see some dinosaurs before the end of the ");
		string.append("day. I just need to show you around the Visitor’s Center first, Hammett’s orders. Come on.” He ");
		string.append("motions for you to follow as starts walking inside the Visitor's Center, and you do.");
		
		Story rude=new Story("Be sarcastic", string.toString());
		story.addVertex(rude);
		story.addEdge(askFriends, rude, 2);
		
		//if macomb left, can ask neary questions
		string.delete(0, string.length());
		string.append("“So, what was that all about?” you ask Neary, unwilling to let the argument go unacknowledged.<br><br>");
		string.append("Neary scoffs indignantly, as if the answer is self-evident. “Macomb was being lazy and refusing ");
		string.append("to clean up after himself, as usual. He acts like I have nothing better to do than clean.” Despite ");
		string.append("the exasperation in his tone, there’s an odd undercurrent of softness to it.<br><br>");
		string.append("Confused, you tell him “But I thought Macomb said you were the janitor.”<br><br>");
		string.append("Neary scowls, annoyed, and the softness in his voice disappears. “I’m not the janitor, I’m the ");
		string.append("head scientist! I did not get my PhD so I could be called a janitor!” This is clearly a sore spot for him.");
		string.append("<br><br>His response hasn’t cleared up your confusion at all, and you can’t keep the disbelief out of your ");
		string.append("voice. “So…you’re a scientist, but you’re in charge of cleaning?” You avoid using the word janitor again.");
		string.append("<br><br>“Yes, I am,” Neary huffs, sounding almost bitter. He mutters “Hammett’s a trillionaire and he ");
		string.append("won’t even pay for a janitor.” Then he tries to calm down and says “I don’t want to talk about it,” ");
		string.append("but can’t keep the petulance out of his tone.<br><br>");
		string.append("Despite the fact that Neary’s response raises more questions than it answers, you can tell that it’s ");
		string.append("a sensitive topic for him, and drop it.");
		
		Story nearyConvo=new Story("Ask Neary about the argument", string.toString());
		story.addVertex(nearyConvo);
		story.addEdge(shake, nearyConvo, 1);
		
		//can end questioning at any time
		string.delete(0, string.length());
		string.append("Suddenly, Neary’s phone vibrates with an incoming text, and he pulls it out of his pocket to read it.");
		string.append("<br><br>“Is that something from Hammet?” you ask.<br><br>");
		string.append("Neary puts his phone away, suddenly tensing up. “Oh, yeah. That’s exactly what it is. I have to ");
		string.append("go now. Enjoy the tour,” he says, sounding overly casual, almost as if trying to hide something.<br><br>");
		string.append("Before you can ask any further questions, Neary turns around and heads back down the path he ");
		string.append("came from, moving surprisingly fast for such a large man. You can just barely make out the ");
		string.append("words on the path’s sign: “Substation”, with “EMPLOYEES ONLY” written below it in big red letters.<br><br>");
		string.append("With nothing else to be said, you head inside the Visitor’s Center.");

		Story nearyEnd=new Story("End the conversation", string.toString());
		story.addVertex(nearyEnd);
		story.addEdge(nearyConvo, nearyEnd, 3);
		story.addEdge(shake, nearyEnd, 2);
		
		//neary question 1
		string.delete(0, string.length());
		string.append("“So, I take it you and Macomb don’t get along very well?” you posit.<br><br>");
		string.append("Neary grunts. “Not always.” He seems to interpret your comment as an insinuation, and frowns, ");
		string.append("sounding almost defensive. “But that doesn’t mean we aren’t friends.”<br><br>");
		string.append("“Then why do you fight so much? Macomb said that was the ninth time you had that argument,” you ask.");
		string.append("<br><br>Neary’s frown deepens, looking as if he’s uncomfortable admitting this. “Macomb and I have ");
		string.append("known each other for 13 years, okay? We're close, so of course we argue all the time.”<br><br>");
		string.append("After a brief pause, he mutters “Anyway, it’s what he gets for making me put up with his ");
		string.append("sloppiness,” but there’s no real bitterness in his words, his irritated tone serving as a transparent ");
		string.append("attempt to mask his affection.");

		Story askEnemies=new Story("Ask Neary if he's friends with Macomb", string.toString());
		story.addVertex(askEnemies);
		story.addEdge(nearyConvo, askEnemies, 1);
		story.addEdge(askEnemies, nearyEnd, 3);
		
		string.delete(0, string.length());
		string.append("You remember something Neary said earlier, and ask “Wait, who is Lewis? You said you left him ");
		string.append("behind somewhere?”<br><br>");
		string.append("Neary suddenly bursts into laughter, and for once, he sounds completely genuine. Looking at you ");
		string.append("with pure amusement, he explains “Lewis is my coffee mug. I left it in the substation.”<br><br>");
		string.append("“Why did you name your mug?” you ask him, staring at him in obvious confusion.<br><br>");
		string.append("Neary smirks when he sees your reaction, and speaks sardonically. “Why didn’t you ask ");
		string.append("Macomb why he named his guitar? What’s wrong with me naming my mug? I’ve had it for ");
		string.append("almost a decade, so it’s important to me.” It’s clear he gave Lewis its name to make fun of ");
		string.append("Macomb’s decision to name his guitar Sarah.<br><br>");
		string.append("“Well, why did Macomb name his guitar then?” you ask, ignoring his sarcasm for now.<br><br>");
		string.append("Neary barely hesitates to respond, scoffing “Because he’s weird. I don’t care if some musician he ");
		string.append("likes did it, it’s stupid to call an inanimate object a ‘she’.” He sounds genuinely annoyed by this.");
		
		Story askLewis=new Story("Ask Neary who Lewis is", string.toString());
		story.addVertex(askLewis);
		story.addEdge(askEnemies, askLewis, 1);
		story.addEdge(askLewis, nearyEnd, 1);
		
		string.delete(0, string.length());
		string.append("You decide to ask Neary how he likes his job, considering that he gets to work with dinosaurs. ");
		string.append("“So, what’s it like working at Jurassic Preserve anyway?”<br><br>");
		string.append("Neary chuckles, unable to hold back a hint of bitterness. “Oh, it’s amazing. Tight deadlines, an ");
		string.append("overbearing boss, and I get to make full use of my PhD with a bucket and a mop!”<br><br>");
		string.append("“If it’s that bad, why don’t you just leave?” you question him.<br><br>");
		string.append("Neary rolls his eyes. “If I could have, I would have, but where else can I clone and study ");
		string.append("dinosaurs?” He sighs and reluctantly admits in a softer tone. “Besides, I can't really complain ");
		string.append("since the company’s not too bad.”<br><br>");
		string.append("“Speaking of which, what’s it like working with dinosaurs anyway?” you ask, remembering you ");
		string.append("haven’t even seen one yet.<br><br>");
		string.append("Neary looks more thoughtful, and responds honestly “It’s the opportunity I’ve been waiting for ");
		string.append("all my life.” He says with a mixture of pride and annoyance “If Hammett ever releases me from ");
		string.append("his non-disclosure agreement, I’m guaranteed a spot in the history books.”");
		
		Story askJob=new Story("Ask Neary how he likes his job", string.toString());
		story.addVertex(askJob);
		story.addEdge(askEnemies, askJob, 2);
		story.addEdge(askJob, nearyEnd, 1);
		
		//neary question 2 (doctor only)
		string.delete(0, string.length());
		string.append("“So, tell me, scientist to scientist, how did you do it? How did you manage to clone dinosaurs?!” ");
		string.append("you ask, finally asking a question you’ve wanted answered even before you stepped foot on the island.<br><br>");
		string.append("Neary smiles, looking proud, but says reluctantly “Sorry, but I can’t tell you. It’s a <a href=\"cloning.html\">trade ");
		string.append("secret</a>.” He sounds like he’s genuinely struggling to hold back from bragging about it.<br><br>");
		string.append("“Come on, can’t you tell me anything? At least how you managed to get intact DNA?” you ask, ");
		string.append("wanting to know how he managed to make the biggest scientific discovery in decades.<br><br>");
		string.append("Neary sighs and speaks more firmly, but sounds genuinely sympathetic. “Sorry, but no. I signed a ");
		string.append("non-disclosure agreement.” Then he mutters “Hammett would kill me if I told anyone.”<br><br>");
		string.append("“You don’t mean…actually killed, do you?” you hesitantly ask, almost subconsciously lowering ");
		string.append("your voice even though you two are the only ones around.<br><br>");
		string.append("You know that the idea sounds ridiculous, but at the same time, you’ve heard the same rumours ");
		string.append("everyone else has. Everyone knows that Biosynch’s CEO was found dead in his penthouse days ");
		string.append("after he announced his opposition to InGen’s merger, though his death was ruled a suicide, and ");
		string.append("the note the police found was a perfect match for his handwriting. But it’s hard to look past the ");
		string.append("fact that the merger’s success was what catapulted Hammett into his position as the world’s first ");
		string.append("trillionaire…<br><br>");
		string.append("Neary looks at you, his face suddenly unreadable and his tone difficult to decipher. “There are a ");
		string.append("lot of people out there who would pay good money for the knowledge I have.” Something shifts ");
		string.append("in his eyes when he mentions money. It almost looks like greed? Or maybe guilt? You can’t quite ");
		string.append("tell, but it doesn’t escape you that he didn’t answer your question.");

		Story askClone=new Story("(Requires Doctor) Ask Neary how he managed to clone dinosaurs", string.toString());
		story.addVertex(askClone);
		story.addEdge(nearyConvo, askClone, 2);
		story.addEdge(askClone, nearyEnd, 1);	
		
		//if neither left can ask both questions
		string.delete(0, string.length());
		string.append("Now that the argument is over, you ask the two “So, what was that all about?”, unwilling to let ");
		string.append("the argument go unacknowledged.<br><br>");
		string.append("Neary harrumphs and says nothing. Macomb sighs and averts his gaze, looking guilty. “Look, ");
		string.append("I’m sorry for being so difficult, but there’s a reason.” He’s looking at neither of you, but is ");
		string.append("speaking to both of you. “Sarah helps me focus, okay? She…she reminds me of home.”<br><br>");
		string.append("Neary blinks in surprise, his former annoyance all gone after Macomb’s admission. “You never ");
		string.append("told me that before.”<br><br>");
		string.append("“I didn’t think it was a big deal?” Macomb confesses, sounding sheepish.<br><br>");
		string.append("Now it’s Neary’s turn to look guilty, but he hides it with a mask of annoyance, mumbling “If I ");
		string.append("knew it meant that much to you, I wouldn’t have gotten so annoyed…”<br><br>");
		string.append("You can see that you’re missing something here, given the sudden shift in both of their attitudes, ");
		string.append("but can also see that the two are clearly still in the middle of reconciling.");

		Story askBoth=new Story("Ask Macomb and Neary about the argument", string.toString());
		story.addVertex(askBoth);
		story.addEdge(doubleScold, askBoth, 1);
		
		//can end questioning at any time		
		string.delete(0, string.length());
		string.append("Suddenly, Neary’s phone vibrates with an incoming text, and he pulls it out of his pocket to read it.");
		string.append("<br><br>“Is that something from Hammet?” you ask.<br><br>");
		string.append("Neary quickly puts his phone away, suddenly tensing up. “Oh, yeah. That’s exactly what it is. I have to ");
		string.append("go now. Enjoy the tour,” he says, sounding overly casual, almost as if trying to hide something.<br><br>");
		string.append("Before you can ask any further questions, Neary turns around to leave, but a confused Macomb ");
		string.append("asks him “Wait, don’t you want to help with the tour?”<br><br>");
		string.append("Neary doesn’t turn around when he speaks, and his shoulders suddenly seem oddly tense. ");
		string.append("“You’re going to be in the Visitor’s Center for a while, right?”<br><br>");
		string.append("“Uh, yeah. You know, you can just put Sarah in the gift shop and I’ll get her after the tour,” says ");
		string.append("Macomb, still confused by his friend’s sudden egress.<br><br>");
		string.append("Neary says curtly “Just stay in the Center, okay? I’ll be back. I’m sorry.”<br><br>");
		string.append("He heads back down the path he came from, moving surprisingly fast for such a large man. You ");
		string.append("can just barely make out the words on the path’s sign: “Substation”, with “EMPLOYEES ");
		string.append("ONLY” written below it in big red letters.<br><br>");
		string.append("Macomb frowns and mutters “That was weird. He usually grumbles non-stop when Hammett ");
		string.append("texts him out of the blue like that.”<br><br>");
		string.append("He runs his hand through his hair and faces you. “Anyway, I guess we should continue the tour ");
		string.append("now. We haven’t even shown you the dinosaurs yet. Come on.” He starts walking into the ");
		string.append("Visitor’s Center and waves at you to follow, which you do.");
		
		Story endBoth=new Story("End the conversation", string.toString());
		story.addVertex(endBoth);
		story.addEdge(askBoth, endBoth, 3);
		story.addEdge(doubleScold, endBoth, 2);
		
		//dual question 1
		string.delete(0, string.length());
		string.append("You interrupt the awkwardness by innocently asking Macomb “Did something happen to your house?”<br><br>");
		string.append("Macomb looks at you, and answers with a hint of amusement. “This isn’t about my penthouse, ");
		string.append("it’s about the fact that I’ve barely even left the island since I got here.”<br><br>");
		string.append("Neary’s lack of reaction implies that he already knew about Macomb’s homesickness, so you ask ");
		string.append("them both “Wait, how long have you guys been here?”<br><br>");
		string.append("“7 years,” Macomb says, as if it’s the most natural thing in the world. Neary nods, affirming that ");
		string.append("he’s been on Isla Soleada for the same amount of time.<br><br>");
		string.append("Then Neary scoffs at you, noticing the look on your face. “What, did you think this was just some 9-5 job?”");
		string.append("<br><br>“Hammett’s real serious about information security. We all basically live here, and probably will ");
		string.append("until the park finally opens,” Macomb says with a nonchalant shrug.");

		Story askHouse=new Story("Ask about the sudden change in mood", string.toString());
		story.addVertex(askHouse);
		story.addEdge(askBoth, askHouse, 1);
		story.addEdge(askHouse, endBoth, 3);
		
		//this is what happens when I fall behind schedule lol: false choice
		string.delete(0, string.length());
		string.append("“All of you have lived on the island for years? But the National Labour Relations Act—” you ");
		string.append("start, but are quickly interrupted by an amused Macomb.<br><br>");
		string.append("“Whoa, slow down there, Mr. Professional. This isn’t a courtroom. I know you’re a super lawyer ");
		string.append("and all, but I’m pretty sure this is completely legal,” he drawls.<br><br>");
		string.append("“Even if it wasn’t, Hammett has billions of dollars worth of lawyers on his side. Not even you ");
		string.append("could challenge that,” Neary snorts.<br><br>");
		string.append("“You’re okay with this?” you ask, still surprised. You can’t imagine being away from your home ");
		string.append("for 7 years, even if it was to work with dinosaurs.<br><br>");
		string.append("They both nod simultaneously. “It’s something you get used to quickly,” sighs Neary. Then he ");
		string.append("adds “And it’s worth the chance to make scientific history,” sounding firm in his belief.<br><br>");
		string.append("“Besides, Hammett made sure we had ‘informed consent’ before we signed our contracts, so it’s ");
		string.append("not like this was a surprise to any of us,” Macomb says, using the legal term. He pauses for a ");
		string.append("moment, then offhandedly adds “Plus, you know, there’s the fact that he pays us enough to make ");
		string.append("us all multi-millionaires.”");

		Story surprise=new Story("Express surprise", string.toString());
		story.addVertex(surprise);
		story.addEdge(askHouse, surprise, 1);
		story.addEdge(surprise, endBoth, 1);
		
		string.delete(0, string.length());
		string.append("“All of you have lived on the island for years? But the National Labour Relations Act—” you ");
		string.append("start, but are quickly interrupted by an amused Macomb.<br><br>");
		string.append("“Whoa, slow down there, Mr. Professional. This isn’t a courtroom. I know you’re a super lawyer ");
		string.append("and all, but I’m pretty sure this is completely legal,” he drawls.<br><br>");
		string.append("“Even if it wasn’t, Hammett has billions of dollars worth of lawyers on his side. Not even you ");
		string.append("could challenge that,” Neary snorts.<br><br>");
		string.append("“You’re okay with this?” you ask, still in disbelief. To you, living on a literal island is just as bad ");
		string.append("as living in a company town.<br><br>");
		string.append("They both nod simultaneously. “It’s something you get used to quickly,” sighs Neary. Then he ");
		string.append("adds “And it’s worth the chance to make scientific history,” sounding firm in his belief.<br><br>");
		string.append("“Besides, Hammett made sure we had ‘informed consent’ before we signed our contracts, so it’s ");
		string.append("not like this was a surprise to any of us,” Macomb says, using the legal term. He pauses for a ");
		string.append("moment, then offhandedly adds “Plus, you know, there’s the fact that he pays us enough to make ");
		string.append("us all multi-millionaires.”");
		
		Story disbelief=new Story("Express disbelief", string.toString());
		story.addVertex(disbelief);
		story.addEdge(askHouse, disbelief, 2);
		story.addEdge(disbelief, endBoth, 1);
		
		//dual question 2
		string.delete(0, string.length());
		string.append("You decide not to interrupt their moment of vulnerability, and listen as the two men continue ");
		string.append("their conversation.<br><br>");
		string.append("Macomb sighs and admits to Neary “Well, if I’m being honest, I wasn’t sure you’d even get it. I ");
		string.append("haven’t heard you complain about living on the island once.”<br><br>");
		string.append("Neary scoffs and crosses his arms to draw attention away from the hint of guilt on his face. ");
		string.append("“That’s only because it’s nothing compared to the working conditions under Hammett.” He ");
		string.append("pauses, sounding as if he’s struggling to get the words out. “But that doesn’t mean I wouldn’t ");
		string.append("have cared if you told me.”<br><br>");
		string.append("Macomb sighs again, putting a hand on the back of his neck. “Yeah, I should've known better. Sorry. Can ");
		string.append("we…compromise then? I have to keep Sarah by the door because that’s where the outlet is, but I ");
		string.append("can…I dunno, put up a sign or something?” He extends his other hand for Neary to shake.<br><br>");
		string.append("Neary scoffs again, but there’s no annoyance in it this time, feigned or otherwise. Still holding ");
		string.append("Sarah, Neary shakes Macomb’s hand with his free hand. “Fine.” After a pause, he says with a ");
		string.append("smirk. “You should’ve just designed the substation better. Who puts an outlet next to a door?”<br><br>");
		string.append("Macomb smirks back. “I told you Sarah helps me focus. If your dinosaur incubators hadn’t been ");
		string.append("hogging all the power, I could’ve come up with something better.”<br><br>");
		string.append("Neary scoffs, but is clearly holding back a smile. “And if you’d had the solar panels installed on ");
		string.append("schedule, there would’ve been more power to use.”<br><br>");
		string.append("Despite their argument before, you can clearly see now that Macomb and Neary are more than ");
		string.append("just antagonistic coworkers; they’re actually surprisingly close friends.");
		
		Story listen=new Story ("Don't interrupt them", string.toString());
		story.addVertex(listen);
		story.addEdge(askBoth, listen, 2);
		story.addEdge(listen, endBoth, 3);
		
		//no false choice here lol
		string.delete(0, string.length());
		string.append("“You guys are friends?!” you blurt out, not expecting the sudden shift in their attitudes.<br><br>");
		string.append("Both men look at you. Macomb looks amused, but Neary rolls his eyes, as if your question was ");
		string.append("dumb. “Did you think we hated each other?” Neary sneers, as if the idea is completely preposterous.<br><br>");
		string.append("“Well, you were just yelling at each other a few minutes ago,” you say, confused by their ");
		string.append("apparent lack of short term memory.<br><br>");
		string.append("“Friends fight sometimes, what are you gonna do?” Macomb says, slapping Neary on the back ");
		string.append("perhaps harder than necessary, making Neary flinch and glare at Macomb.<br><br>");
		string.append("“Besides,” Macomb starts. “It’s a small island. Even if I wanted to, it’s hard to hate someone ");
		string.append("after working so closely with them for 7 years.” However, his tone and Neary’s casual nod of agreement ");
		string.append("suggest that they never hated each other to begin with.");

		Story shock=new Story ("Express surprise", string.toString());
		story.addVertex(shock);
		story.addEdge(listen, shock, 1);
		story.addEdge(shock, endBoth, 1);
		
		string.delete(0, string.length());
		string.append("“You two sure made up quickly. When were you going to mention you were friends?” you ask. ");
		string.append("Both men look at you. Macomb looks amused, but Neary rolls his eyes, as if your question was ");
		string.append("dumb. “Did you think we hated each other?” Neary sneers, as if the idea is completely preposterous.<br><br>");
		string.append("“Oh, I’m sorry. When you ran up to Macomb and started yelling at him, was I supposed to think ");
		string.append("it was a lover’s spat?” you retort.<br><br>");
		string.append("Neary scowls, but Macomb responds before he can, smirking. “You sure didn’t look or sound ");
		string.append("very friendly, Neary. Can you blame him for getting a bad impression?”<br><br>");
		string.append("Neary just grunts in annoyance, unable to think of a way to defend himself against both of you.");

		Story doubt=new Story("Express disbelief", string.toString());
		story.addVertex(doubt);
		story.addEdge(listen, doubt, 2);
		story.addEdge(doubt, endBoth, 1);
		
		//regardless of how argument was resolved, must end up going inside center
		string.delete(0, string.length());
		string.append("Once inside, you can instantly feel that the building has air conditioning, finally providing you with ");
		string.append("some relief from the island’s heat. When stepping through the double doored entrance, you are ");
		string.append("greeted by the sight of a massive dinosaur skeleton, explaining the need for a domed roof. There are twin ");
		string.append("spiral staircases on either side of it, leading to a balcony that provides a nice view of the lake ");
		string.append("outside. To your left there’s a sign that says “Souvenirs”, right above a small but empty shop. To ");
		string.append("your right is a corridor that bends so you can’t make out where it leads, but presumably leads further into the Center. ");
		string.append("And of course, the building’s interior is made out of stone and wood, just like the exterior.<br><br>");
		string.append("You notice Macomb is standing by the skeleton. Once he sees you, he gives you a nod and clears his throat. ");
		string.append("“As you can see, in front of us we have the skeleton of a currently unidentified sauropod. Sauropods were a ");
		string.append("type of massive herbivorous dinosaur. The biggest sauropods were the size of four story buildings, ");
		string.append("and even the smallest ones were the size of giraffes.” He sounds like he’s ");
		string.append("reading from a notecard, and when you look over at him, you can see that he is, ");
		string.append("holding one drawn from a stack you can see sticking out of his pocket.");
		Story tour=new Story("Continue the tour", string.toString());
		story.addVertex(tour);
		//forced into instantly ending section if argument ended badly
		story.addEdge(ignore, tour, 1);
		story.addEdge(silence, tour, 1);
		story.addEdge(noShake, tour, 1);
		//go inside once done asking Macomb questions
		story.addEdge(macombEnd, tour, 1);
		story.addEdge(askBB, tour, 1);
		story.addEdge(askSkill, tour, 1);
		story.addEdge(nice, tour, 1);
		story.addEdge(rude, tour, 1);
		//same for Neary
		story.addEdge(nearyEnd, tour, 1);
		//same for both
		story.addEdge(endBoth, tour, 1);
		
		return tour; 
	}
	
	private static Story MeetMuldrewAlex(Story start) //speak to muldrew and alex before power goes out
	{
		//optional sauropod infodump
		StringBuilder string=new StringBuilder(600); 
		string.append("“<a href=\"sauropods.html\">Sauropods</a>’ height and length prevented them from living in Jurassic ");
		string.append("forests, so they were mainly concentrated in the arid, desert-like areas, outside the forests. ");
		string.append("Their long necks let them eat leaves and twigs from the tops of trees, where no other dinosaur could reach, ");
		string.append("and their size prevented them from having natural predators, making them a kind of ‘apex herbivore’.”<br><br>");
		string.append("You can’t tell if Macomb is deliberately making himself sound dull to be amusing, or if it’s the ");
		string.append("side effect of him trying too hard to enunciate. Either way, he seems focused on his task as he ");
		string.append("takes out the next notecard to read off of.<br><br>");
		string.append("“Scientists believe sauropods’ long necks allowed them to gather food without moving, making ");
		string.append("them burn minimal energy. And their large size meant it took nutrients a long time to circulate ");
		string.append("through their bodies, giving them a slow metabolism, meaning they needed less food than their ");
		string.append("size would suggest. Combined with the fact that larger animals are more difficult to prey upon, ");
		string.append("natural selection led to them using their spare energy to grow to huge sizes.” Macomb flips over ");
		string.append("the notecard, and you can see that you underestimated how long this would take.<br><br>");
		string.append("“Even though their size made their movement slow—and prevented them from running if they ");
		string.append("were attacked—they made up for this vulnerability by travelling in packs, much like modern-day ");
		string.append("elephants. They were only vulnerable if separated—usually due to natural disaster or illness—in ");
		string.append("which case agile predators like allosauruses could easily wear them down. And like most other ");
		string.append("reptiles, sauropods laid eggs. Somewhat similar to sea turtles, they would lay one to two dozen ");
		string.append("eggs at a time, burying them in the earth to incubate, and abandoning them to survive on their ");
		string.append("own when they hatched. Unlike adult sauropods, both sauropod babies and eggs were small and ");
		string.append("fully vulnerable to predators.”<br><br>With that, Macomb goes for his pocket. You can’t immediately ");
		string.append("tell if he’s putting away his notecards or pulling out more, but it no longer matters once you hear ");
		string.append("a high pitched voice call out from your right.");
		
		Story listen=new Story("Keep listening", string.toString());
		story.addVertex(listen);
		story.addEdge(start, listen, 1);
		
		string.delete(0, string.length());
		string.append("You stop Macomb from droning on, staring at him and being completely blunt. “Is this seriously ");
		string.append("going to be the tour? You’re going to show me skeletons and read off notecards when there’s ");
		string.append("literally real dinosaurs right outside?<br><br>");
		string.append("Macomb winces and raises his hands placatingly. “Whoa, okay, okay. Sorry, I didn’t know you ");
		string.append("were so, uh, eager to see them.”<br><br>");
		string.append("He awkwardly pockets his notecard and sheepishly explains “Hammett said I was supposed to ");
		string.append("give you the full tour, like you were a normal visitor. He doesn’t just want feedback on the ");
		string.append("dinosaurs, he wants to know what you think of the park as a whole, you know? We all put a lot of ");
		string.append("effort into making it.”<br><br>");
		string.append("Then Macomb lets out a soft sigh. “But I guess it’s not such a big deal if we skip ahead to the ");
		string.append("dinosaurs. Maybe I can show you around the Center at the end.” As he speaks, one of his notecards falls ");
		string.append("out of his pocket and lands near you. He quickly retrieves it and puts it back, but not before you get a");
		string.append("glimpse of the text, enough to see that it was all hand-written.");
		
		Story interrupt=new Story("Interrupt him", string.toString());
		story.addVertex(interrupt);
		story.addEdge(start, interrupt, 2);
		
		//meet alex
		StringBuilder prime=new StringBuilder(600);
		prime.append("You hear a shout of  “Uncle Ivan!”, clearly the words of a child, but too high pitched to tell if it’s ");
		prime.append("a boy or girl. The call rings out from the corridor that presumably leads deeper inside the Center, ");
		prime.append("and a second later, a young boy emerges from it. As with Macomb and Neary, he looks bizarrely ");
		prime.append("familiar to you even though you have no idea who he is. He appears to be no older than ten, wearing a t-shirt, ");
		prime.append("shorts, and a baseball hat that mostly hides his dirty blonde hair.<br><br>");
		prime.append("Macomb’s face immediately lights up when he sees the boy, responding “Hey, Alex. You’re just ");
		prime.append("in time to join us. We were just starting the tour.” He gestures towards you and says “This is the ");
		prime.append("special guest everyone was talking about. Why don’t you introduce yourself?”<br><br>");
		prime.append("Alex looks at you curiously, as if trying to figure out what’s so special about you. Then he ");
		prime.append("cautiously says “Hi, I’m Alex.”");
		
		Story meetAlex=new Story("Continue", prime.toString());
		story.addVertex(meetAlex);
		story.addEdge(listen, meetAlex, 1);
		
		//false choice but it's justified
		string.delete(0, string.length());
		string.append("“I—” you start, but are suddenly interrupted by a high pitched voice calling out from your right.<br><br>");
		
		Story sorry=new Story("Apologise", string.toString()+prime.toString());
		story.addVertex(sorry);
		story.addEdge(interrupt, sorry, 2);
		
		Story notSorry=new Story("Agree", sorry);
		story.addVertex(notSorry);
		story.addEdge(interrupt, notSorry, 1);
		
		//introduce self to alex or not
		prime.delete(0, prime.length());
		prime.append("“He’s Hammett’s grandson.” A low yet smooth voice you’ve heard before calls out from the corridor, and you ");
		prime.append("look over to see yet another familiar looking person walking towards you, an older man in a polo shirt and ");
		prime.append("khakis. Even without the pith hat, you manage to identify him as the tour guide from the commercial ");
		prime.append("you watched, as he’s wearing the exact same clothing.<br><br>");
		prime.append("The man sees the recognition in your eyes and nods at you as he walks over to Alex’s side. ");
		prime.append("“Roland Muldrew; I’m the zookeeper here.”");
		
		string.delete(0, string.length());
		string.append("You introduce yourself to Alex with a nod. “I’m Aaron Brandt, here to assess the park’s legality.”<br><br>");
		string.append("Alex frowns in confusion. “Why would it be illegal?”<br><br>");
		string.append("Even though you could probably talk about all its potential legal issues for hours, you try to ");
		string.append("phrase your answer in the simplest way possible. “Dinosaurs can be dangerous if they aren’t ");
		string.append("taken care of properly. And even if they are, there are a lot of laws that control how animals are ");
		string.append("supposed to be treated. I’m here to make sure they’re all being followed so everyone is safe.”<br><br>");
		string.append("“Oh, they aren’t dangerous. Uncle Roland is around them all the time and they never hurt him,” he says confidently.");
		string.append("<br><br>“Uncle…Roland?” you ask, now confused. “Uncle Ivan” hadn’t once mentioned having a sibling ");
		string.append("as one of his coworkers. Come to think of it…you look between Macomb and Alex, failing to see any resemblance whatsoever.");
		string.append("<br><br>Macomb understands your implication, and is about to clarify, but is interrupted.<br><br>");
		
		Story introLaw=new Story("(Requires Lawyer) Introduce yourself", string.toString()+prime.toString());
		story.addVertex(introLaw);
		story.addEdge(meetAlex, introLaw, 1);
		story.addEdge(sorry, introLaw, 1);
		story.addEdge(notSorry, introLaw, 1);
		
		string.delete(0, string.length());
		string.append("You introduce yourself to Alex with a nod. “Hi, Alex. I’m Kelly. I study dinosaurs for a living, ");
		string.append("so I’m here to see if the ones on this island are real or not.”<br><br>");
		string.append("Alex smiles as if you just said something silly. “But they are real! I see them all the time. Didn’t ");
		string.append("you see the commercial?”<br><br>");
		string.append("Something about Alex is bugging you, not just the fact that Macomb didn’t tell you anything ");
		string.append("about him, but his apparent familiarity with the park, which isn’t even open to the public yet. ");
		string.append("Then you look between Macomb and Alex, failing to see any resemblance whatsoever. “‘All the ");
		string.append("time?’ How often does your uncle take you to his work?”<br><br>");
		string.append("Macomb understands your implication, and is about to clarify, but is interrupted.<br><br>");

		Story introDoc=new Story("(Requires Doctor) Introduce yourself", string.toString()+prime.toString());
		story.addVertex(introDoc);
		story.addEdge(meetAlex, introDoc, 2);
		story.addEdge(sorry, introDoc, 2);
		story.addEdge(notSorry, introDoc, 2);
		
		string.delete(0, string.length());
		string.append("You don’t even acknowledge Alex, looking right over him at Macomb, and barely noticing when Alex frowns ");
		string.append("at your dismissiveness. “What’s up with the random kid?” you ask Macomb, as if Alex isn’t there.<br><br>");

		Story introIgnore=new Story("Ignore the boy", string.toString()+prime.toString());
		story.addEdge(meetAlex, introIgnore, 3);
		story.addEdge(sorry, introIgnore, 3);
		story.addEdge(notSorry, introIgnore, 3);
		
		//meet muldrew
		string.delete(0, string.length());
		string.append("Macomb nods at Muldrew in greeting. “Late as always, eh Muldrew?”<br><br>");
		string.append("Muldrew responds evenly, not missing a beat. “Doesn’t look like I missed much, mate. You ");
		string.append("haven’t even left the lobby.”<br><br>");
		string.append("Macomb adopts an expression that’s a mixture of amusement and sheepishness. “We’re a little ");
		string.append("bit behind schedule, that’s all. Neary…ran into us.”<br><br>");
		string.append("Alex interjects, asking “Is Uncle Donnie going to tour the park with us too?”<br><br>");
		string.append("Muldrew answers before Macomb can. “He said he needed to work, remember?” Muldrew’s ");
		string.append("voice remains calm, but softens ever so slightly when he talks to Alex, who is disappointed by ");
		string.append("the reminder but nods when he remembers.<br><br>");
		string.append("“Which is really weird, because you’d think he’d jump at the chance to get a day off,” Macomb ");
		string.append("muses. “The guy’s always complaining about being overworked, then suddenly goes off on his ");
		string.append("own saying he wants to work more…”<br><br>");
		string.append("Looking at Muldrew and listening to him talk more, you suddenly recognise his voice as the ");
		string.append("narrator from the commercial. And once you do, something clicks in your mind and you can’t ");
		string.append("help but ask about it.");

		Story meetMuldrew=new Story("Continue", string.toString());
		story.addEdge(introLaw, meetMuldrew, 1);
		story.addEdge(introDoc, meetMuldrew, 1);
		story.addEdge(introIgnore, meetMuldrew, 1);
		
		string.delete(0, string.length());
		string.append("You only saw them for a few seconds before your attention was drawn to the dinosaurs, but you ");
		string.append("can remember it clearly now. A thin man, a fat man, and a boy in a baseball hat, standing with ");
		string.append("Muldrew. It perfectly explains why these new faces all looked so familiar to you.<br><br>");
		string.append("Macomb nods, his lip curving upwards. “Yeah, you only now noticed that?”<br><br>");
		string.append("“It was only a two second shot!” you retort. Additionally, you recall that in the commercial, he ");
		string.append("was dressed much more conservatively than he is now.<br><br>");
		string.append("“And it still took almost half an hour to film,” Muldrew notes dryly. “Neary kept complaining, ");
		string.append("and Macomb here kept provoking him,” he explains.<br><br>");
		string.append("“And Grandpa got so mad, he almost said a bad word,” Alex giggles.");

		Story familiar=new Story("“Wait, weren’t all of you guys in the commercial?”", string.toString());
		story.addEdge(meetMuldrew, familiar, 1);
		
		string.delete(0, string.length());
		string.append("“Why didn’t Hammett just hire extras then? You and Neary didn’t look too happy in the ");
		string.append("commercial,” you say to Macomb, recalling how respectively bored and annoyed they look.<br><br>");
		string.append("“It’s a test commercial, not the final version,” Macomb replies. “Besides, you think that man ");
		string.append("would ever hire actors when he could just make us do it? He’s such a—” he hesitates and glances ");
		string.append("at Alex, then finishes lamely. “Uh, he’s too ‘frugal’ for that.”<br><br>");
		string.append("Muldrew gives you a different reason. “Hammett had practical concerns too. He doesn’t want ");
		string.append("any outsiders seeing the dinosaurs until he’s ready to open the park.”<br><br>");
		string.append("“And why is that?” you ask.<br><br>");
		string.append("Muldrew is silent for a moment, as if considering how much to say. “He doesn’t want anyone ");
		string.append("spying on him. No one is allowed on the island but us three and Alex, and now you,” he says bluntly.");
		string.append("<br><br>“‘Three’? You mean you, Macomb, and Neary are the only people working on the island?!” you ");
		string.append("say, completely shocked.<br><br>");
		string.append("Alex gives you a confused look, and Macomb looks at you blankly, neither understanding your surprise. ");
		string.append("“Yeah? Were you expecting a fourth?” asks Macomb.");

		Story revelation=new Story("Ask more about the commercial", string.toString());
		story.addEdge(familiar, revelation, 1);
		
		string.delete(0, string.length());
		string.append("“How—” you are quickly interrupted by Muldrew, who appears unfazed by your question.<br><br>");
		string.append("“Excuse my colleague. He’s worked for Hammett so long, he doesn’t think it’s weird anymore,” ");
		string.append("Muldrew says, giving you a perceptive glance. You notice he’s visibly older than either Macomb ");
		string.append("or Neary; neither of them could be older than 40, but Muldrew looks like he couldn’t be any younger ");
		string.append("than 50, despite being in better shape than you are.<br><br>");
		string.append("“Hammett started building this place 7 years ago, right?” you ask, still trying to make sense of ");
		string.append("this revelation.<br><br>");
		string.append("“Yeah, for my third birthday,” Alex innocently chimes in.<br><br>");
		string.append("Macomb laughs when he sees your stare, knowing exactly what you’re thinking. “That’s right, ");
		string.append("Hammett dumped billions of dollars into cloning dinosaurs just so he could make his grandson ");
		string.append("happy. Perk of being a trillionaire.”");
		
		//false choice for comedic effect
		Story bombshell1=new Story("“How is that even possible?”", string.toString());
		Story bombshell2=new Story("“How do you get around?”", bombshell1);
		Story bombshell3=new Story("“How many hours do you work per day?”", bombshell1);
		Story bombshell4=new Story("(Requires Lawyer) “How is that even legal?”", bombshell1);
		Story bombshell5=new Story("(Requires Doctor) “How can three people handle so many dinosaurs?”", bombshell1);
		story.addEdge(revelation, bombshell1, 1);
		story.addEdge(revelation, bombshell2, 2);
		story.addEdge(revelation, bombshell3, 3);
		story.addEdge(revelation, bombshell4, 4);
		story.addEdge(revelation, bombshell5, 5);
		
		//talk to Muldrew
		string.delete(0, string.length());
		string.append("You decide the logistics of the island is the more pressing issue, and ask them “How can the ");
		string.append("three of you possibly manage an entire island of dinosaurs?”<br><br>");
		string.append("Muldrew grunts softly. “That’s my job. I supervise the other two. Or else they’d have killed each ");
		string.append("other by now.”<br><br>");
		string.append("You speak before Macomb can. “No, I meant how can the three of you physically manage? I did ");
		string.append("my research before coming here; Isla Soleada is as big as Manhattan.”<br><br>");
		string.append("Muldrew is unfazed, still speaking in the same calm tone. “And which parts of the island have dinosaurs?”");
		string.append("<br><br>“Hint: they aren’t just roaming around,” Macomb mock whispers to you.");

		Story askMuldrew=new Story("Ask Macomb and Muldrew about their jobs", string.toString());
		story.addEdge(bombshell1, askMuldrew, 1);
		story.addEdge(bombshell2, askMuldrew, 1);
		story.addEdge(bombshell3, askMuldrew, 1);
		story.addEdge(bombshell4, askMuldrew, 1);
		story.addEdge(bombshell5, askMuldrew, 1);
		
		prime.delete(0, prime.length());
		prime.append("Muldrew’s lip quirks ever so slightly upwards. “Just messing with you a bit, kid.”<br><br>");
		prime.append("“‘Kid’?” is all you can say at his sudden nickname for you.<br><br>");
		prime.append("Muldrew lets out a low chuckle. “No offence, but you look barely half my age.”<br><br>");
		prime.append("“How old are you, exactly?” you hesitantly ask him. If it wasn’t for his light grey hair and the ");
		prime.append("faint wrinkles on his face, his muscles and sharp grey eyes would almost be enough to make you ");
		prime.append("think he was your age.<br><br>");
		prime.append("“Fifty-seven,” Muldrew responds without hesitation. You actually are almost half his age.");
		
		string.delete(0, string.length());
		string.append("“You keep the dinosaurs in enclosures, right?” you say, even though you all know that wasn’t what you meant.");
		string.append("<br><br>“Ding, ding! We have a winner,” Macomb quips.<br><br>");
		string.append("Muldrew nods at you with a slight hint of approval while Alex keeps silently watching. “Exactly. ");
		string.append("The free roaming animals are harmless and manage themselves. Neary and I take care of the ");
		string.append("dinosaurs and ecosystem, and Macomb handles the planning and building.”<br><br>");
		string.append("“You couldn’t have said that in the first place?” you ask.<br><br>");

		Story serious=new Story("Answer the question", string.toString()+prime.toString());
		story.addEdge(askMuldrew, serious, 1);
		
		string.delete(0, string.length());
		string.append("You speak more firmly now. “I know you keep the dinosaurs in enclosures, and you know what I ");
		string.append("meant. Stop sidestepping the question and answer already.”<br><br>");
		string.append("Macomb frowns a little, but Muldrew inclines his head at you and gets to the point while Alex ");
		string.append("keeps silently watching. “Neary and I take care of the dinosaurs and ecosystem, and Macomb ");
		string.append("handles the planning and building.”<br><br>");
		string.append("“You couldn’t have said that in the first place?” you mutter.<br><br>");

		Story annoyed=new Story("Get annoyed", string.toString()+prime.toString());
		story.addEdge(askMuldrew, annoyed, 2);
		
		string.delete(0, string.length());
		string.append("You sigh in exasperation at how absurd all of this is. “You know what, never mind. I don’t even ");
		string.append("want to know anymore.”<br><br>");
		string.append("Macomb grins at you, clearly amused, and Muldrew just nods, completely unfazed by your ");
		string.append("annoyance. Alex just looks at you innocently, not understanding what the problem is.<br><br>");
		string.append("Macomb says to Muldrew “Well, as long as you two are here, you might as well join the tour. ");
		string.append("You know more about dinosaurs than me, anyway.”<br><br>");
		string.append("Muldrew is about to ask Alex if he’s up for it, but stops when he sees the look on Alex’s face. ");
		string.append("Alex is practically glowing with excitement, as if this is his first time getting to look around the island.");
		string.append("<br><br>“Sure, mate,” Muldrew says to Macomb.");

		Story quit=new Story("Give up on trying to understand", string.toString());
		story.addEdge(bombshell1, quit, 3);
		story.addEdge(bombshell2, quit, 3);
		story.addEdge(bombshell3, quit, 3);
		story.addEdge(bombshell4, quit, 3);
		story.addEdge(bombshell5, quit, 3);	
		story.addEdge(askMuldrew, quit, 3);
		story.addEdge(serious, quit, 3);
		story.addEdge(annoyed, quit, 3);
		
		//can ask muldrew one of two more questions
		string.delete(0, string.length());
		string.append("“So, are you in such good shape because you walk everywhere?” you ask Muldrew in a not so ");
		string.append("subtle attempt to learn more, recalling that you haven’t seen a single car or bike on the island.<br><br>");
		string.append("“No, I’m in shape because I work out daily,” he says dryly.<br><br>");
		string.append("“Hammett does make us walk everywhere though. Because bikes aren’t ‘historically accurate’,” ");
		string.append("Macomb mutters. “The three of us had to argue with him for days before he even let us have porta-potties.”");
		string.append("<br><br>Remembering all the paths in the clearing outside, you ask “If you walk everywhere, how do you ");
		string.append("keep an eye on all the dinosaurs?”<br><br>");
		string.append("Muldrew answers seriously this time. “Macomb made an app. There are cameras in all the ");
		string.append("enclosures and automated feeding systems. If anything happens, I can run there within five ");
		string.append("minutes, assuming I can’t fix it with my phone.”<br><br>");
		string.append("“It’s really cool,” Alex chimes in. “Uncle Roland let me feed Allen once.”<br><br>");
		string.append("“One of the allosauruses,” Macomb explains before you can ask.");
		
		Story askDetails=new Story("Press Muldrew and Macomb for details on their jobs", string.toString());
		story.addEdge(serious, askDetails, 1);
		story.addEdge(annoyed, askDetails, 1);
		
		string.delete(0, string.length());
		string.append("You take a closer look at Muldrew, noting the effortlessly confident way he holds himself and ");
		string.append("unflinchingly returns your gaze, and ask him “You’re a veteran, aren’t you?”<br><br>");
		string.append("Muldrew nods, replying “From the Army. Was lucky enough to be a dog handler for all my 20 ");
		string.append("years. Even got stationed in the UK for a year, picked up a bit of slang.” He looks you over ");
		string.append("again, then asks “Which branch are you from?”<br><br>");
		string.append("“The Marines,” you respond. “I was a JAG Officer for 4 years.” You aren’t surprised that he ");
		string.append("figured out you were also ex-military, considering that you brought up the topic.<br><br>");
		string.append("“Ah. Got sick of the taste of crayons?” Muldrew asks with a completely straight face.<br><br>");
		string.append("“Actually, I found a better job as a private prosecutor. I wanted more of a challenge,” you say, ");
		string.append("holding his unwavering gaze with one of your own.<br><br>");
		string.append("“Get a room; there’s kids around,” Macomb drawls, unable to hide an amused smirk.<br><br>Alex is ");
		string.append("looking between you two, trying to figure out what a JAG Officer is, but not wanting to interrupt ");
		string.append("your staring competition.");
		
		Story askMilitary=new Story ("(Requires Lawyer) Ask Muldrew if he's ex-military", string.toString());
		story.addEdge(serious, askMilitary, 2);
		story.addEdge(annoyed, askMilitary, 2);
		
		//then ask him one of two final questions
		string.delete(0, string.length());
		string.append("“How did Hammett get you to be his zookeeper anyway?” you ask Muldrew.<br><br>");
		string.append("He just shrugs indifferently. “I still don’t know how he found me. I had just taken a job as a ");
		string.append("safari guide when he contacted me.” After a pause, he adds “I presume it’s because I’m ");
		string.append("ex-military. I’ve dealt with worse than hungry dinosaurs, and I know how to keep a secret.”<br><br>");
		string.append("“You knew you were going to be handling dinosaurs?” you ask.<br><br>");
		string.append("Muldrew shakes his head. “Hammett only mentioned managing ‘formerly extinct animals’ on a ");
		string.append("private island. But I’ve always liked animals, the more challenging, the better, so I signed up.”");
		string.append("“And if you were wondering,” Macomb adds, “Neary and I already worked for Hammett. He got ");
		string.append("an internship straight out of college, and has been working for NextGen ever since, because they ");
		string.append("paid for all his degrees. And I’ve always been a simple man. I’d live on an island full of flesh ");
		string.append("eating demons for this kind of money.”<br><br>");
		string.append("“You already do,” murmurs Muldrew, making Alex smile.<br><br>");
		string.append("“Yeah, well, at least I got to build cages for these demons,” Macomb grins lazily. “There’s no ");
		string.append("danger as long as they stay in there.”");

		Story askHire=new Story("Ask Muldrew and Macomb how they got their jobs", string.toString());
		story.addEdge(askDetails, askHire, 1);
		story.addEdge(askMilitary, askHire, 1);
		
		string.delete(0, string.length());
		string.append("“I assume you do more than just zookeep?” you ask Muldrew, thinking of Macomb's multiple roles.<br><br>");
		string.append("“Right. I’m in charge of safety and security too.” It could just be your imagination, but Muldrew ");
		string.append("seems to stand a little bit straighter when he says this.<br><br>");
		string.append("“That must be pretty easy if there’s only three of you,” you say, glancing at Macomb.<br><br>");
		string.append("Muldrew shakes his head a little. “Hammett still needed contractors to build all this and make ");
		string.append("sure it’s up to regulation.” He gestures at the Center, but is also referring to the island’s other ");
		string.append("facilities. “Macomb’s in charge of finding them and handling them on-site, but I do all the vetting.”");
		string.append("<br><br>“And if you were wondering, I’m Chief Technology Officer, Chief Architect, Chief Engineer, ");
		string.append("and head of Human Resources,” Macomb says, managing to keep a straight face.<br><br>");
		string.append("“You forgot Chief Blowhard,” Muldrew deadpans, making Alex smile. Then he explains to you ");
		string.append("“Hammett gave him the jobs, he gave himself the titles.”<br><br>");
		string.append("“Hey, it’s going to look great on my resume,” Macomb says, mock-serious. “Well, assuming I ");
		string.append("decide to keep working after this. I guess I don’t really need the money with how well Hammett pays.”");
		string.append("<br><br>“Then what does Neary do?” you ask them.<br><br>");
		string.append("“Biologist, zoologist, geneticist, and janitor,” Macomb snorts. “He clones, modifies, and ");
		string.append("monitors dinosaurs, then mops the floors.”<br><br>");
		string.append("“We drew straws for cleaning duties,” Muldrew says flatly.");
		
		Story askDuties=new Story("Ask Muldrew and Macomb what their jobs are like", string.toString());
		story.addEdge(askDetails, askDuties, 2);
		story.addEdge(askMilitary, askDuties, 2);
		
		//talk to alex
		string.delete(0, string.length());
		string.append("You decide Alex’s claim is the bigger bombshell, and ask him slowly “Your grandfather brought ");
		string.append("back multiple species from extinction for your birthday present?”<br><br>");
		string.append("Alex nods and beams, apparently oblivious to how insane this sounds. “Uh huh. My grandpa’s the best, right?”");
		string.append("<br><br>Macomb smirks at you. “Told you. The man spared no expense.”<br><br>");
		string.append("Muldrew says to you, more seriously, “Hammett didn’t become a trillionaire through luck, kid. ");
		string.append("He always gets what he wants.” He pauses for a moment, then adds “And he wants to keep his ");
		string.append("only heir happy.” The clear fondness in his gaze as he looks at Alex makes it clear that he has ");
		string.append("that in common with Hammett.<br><br>");
		string.append("You glance at Alex, then ask Muldrew “Doesn’t Hammett have children?” You keep up with the ");
		string.append("news as much as anyone else, but don’t know much about Hammett’s personal life.<br><br>");
		string.append("Muldrew pauses for a moment, also glancing at Alex, then carefully says “He has a daughter, but ");
		string.append("they don’t get along. She has no interest in business.”<br><br>");
		string.append("Alex doesn’t seem to mind the topic, and blithely adds “I heard Mom say Grandpa’s impossible ");
		string.append("to please, and more annoying than Dad.”<br><br>");
		string.append("Macomb stifles a laugh, seeming to agree, and Muldrew sighs. “His parents are divorced,” he ");
		string.append("explains to you, his look indicating that it's time to change topics.");

		Story askAlex=new Story("Ask about Alex's relationship with Hammett", string.toString());
		story.addEdge(bombshell1, askAlex, 2);
		story.addEdge(bombshell2, askAlex, 2);
		story.addEdge(bombshell3, askAlex, 2);
		story.addEdge(bombshell4, askAlex, 2);
		story.addEdge(bombshell5, askAlex, 2);
		story.addEdge(askAlex, quit, 3);
		
		//can ask alex one of two questions
		string.delete(0, string.length());
		string.append("“To be clear, none of these guys are actually related to you, right?” you ask Alex, trying to be certain.");
		string.append("<br><br>Alex giggles. “No, I just call them uncle ‘cause we’re close.”<br><br>");
		string.append("Macomb smiles a little. “He’s been visiting the island ever since Neary cloned the first dinosaur ");
		string.append("a few years ago. He literally comes by every weekend.”<br><br>");
		string.append("Muldrew nods. “Since I’m in charge of security, Hammett put me in charge of Alex.” His voice ");
		string.append("is a hint softer when he speaks again. “You know, I used to hate kids; it’s why I never married. ");
		string.append("But after a few years with this one, I’m almost starting to regret that.” Alex beams at the compliment.");
		string.append("<br><br>Curious, you ask “Alex, what do you do here every week?” You can’t imagine it would take that ");
		string.append("many visits to see the whole park.<br><br>");
		string.append("Alex looks at you like your question was weird. “See the dinosaurs and hang out with my ");
		string.append("uncles,” he says as if it’s obvious.<br><br>");
		string.append("“He likes saying hi and bye to all the dinosaurs every time he comes over or leaves. He named ");
		string.append("every single one of them,” Muldrew says flatly. He sounds as if the mere memory of waiting for ");
		string.append("Alex to finish his goodbyes is exhausting.");
		
		Story askUncle=new Story("Ask about Alex's relationship with Macomb, Muldrew, and Neary", string.toString());
		story.addEdge(askAlex, askUncle, 1);
		
		string.delete(0, string.length());
		string.append("“I take it you like dinosaurs then?” you ask Alex.<br><br>");
		string.append("His face lights up at the mention, and he nods enthusiastically. “Uh huh, they’re the best! Do you ");
		string.append("want to know which one is my favourite?”<br><br>");
		string.append("“Let me guess, a T. rex?” you ask, knowing it’s the most popular dinosaur by far.<br><br>");
		string.append("Alex shakes his head. “Nope, I like Nessie the best.”<br><br>");
		string.append("Muldrew sighs softly and explains “One of the plesiosaurs. He named them all.” You belatedly ");
		string.append("realise Alex was referring to the dinosaurs on the island.<br><br>");
		string.append("“‘One of’, huh? How many dinosaurs are on this island anyway,” you ask.<br><br>");
		string.append("Alex looks happy to tell you. “Ooh, there’s Nessie, and Terry, and Allen, and–”<br><br>");
		string.append("“Alex, we’re the only ones who know what those names mean,” Muldrew interrupts him with a ");
		string.append("mix of exasperation and affection. Alex gives an embarrassed smile, and Muldrew answers you ");
		string.append("on his behalf. “Species, there’s plesiosaurs, pterosaurs, allosauruses, stegosauruses, and ");
		string.append("dilophosauruses. Individuals, there’s only two or three of each.”<br><br>");
		string.append("“Hammett said he likes ‘quality over quantity’,” Macomb says, then adds off-handedly “That, ");
		string.append("and it’s hard to manage more than a dozen dinosaurs with only three people, but he’s too ");
		string.append("paranoid to hire anyone else this late in the process.”");

		Story askDino=new Story("Ask Alex if he likes dinosaurs", string.toString());
		story.addEdge(askAlex, askDino, 2);
		
		//then can ask alex one of two final questions
		string.delete(0, string.length());
		string.append("“So, Alex, what’s Hammett like?” you ask him casually. Like everyone in the country, you know ");
		string.append("what Hammett looks like–a stern man whose many wrinkles only accentuate his unyielding gaze. ");
		string.append("But you don’t know much about what he’s like in person beyond rumours, due to his lack of ");
		string.append("social media or public appearances, and refusal to give any interviews.<br><br>");
		string.append("“Grandpa’s the best! He always tells me how special I am, and he spends more time with me ");
		string.append("than Dad does!” Alex chirps. If it wasn’t for the blue eyes they shared, you’d never guess Alex ");
		string.append("was Hammett’s grandson and the heir to the world's only trillion dollar company, given his happy-go-lucky attitude.");
		string.append("<br><br>Macomb chuckles. “Wait until you’re older, then let’s see if you still think that. The man’s ");
		string.append("ruthless when it comes to business; anything less than 110% of your effort, and you’re fired. And ");
		string.append("his deadlines are brutal…”<br><br>");
		string.append("Muldrew also gives his opinion, speaking neutrally. “Hammett’s a hard man with high ");
		string.append("expectations, but if you can meet them, he’s very generous with his rewards. I can’t say I have ");
		string.append("any complaints.”<br><br>");
		string.append("Macomb scoffs “Yeah, since you have the easiest job of us three. All you do is feed dinosaurs ");
		string.append("and act as Hammett’s messenger.”<br><br>");
		string.append("“Would you like to get locked in a cage with an allosaurus and stick a needle into it to gather ");
		string.append("blood samples?” Muldrew retorts without batting an eye.<br><br>");
		string.append("Macomb snorts and rolls his eyes but stays silent, knowing Muldrew has a point. “That’s what I thought,” ");
		string.append("Muldrew says, unable to hide the faint amusement in his voice.");
		
		Story askHammett=new Story("Ask Alex what he thinks of Hammett", string.toString());
		story.addEdge(askUncle, askHammett, 1);
		story.addEdge(askDino, askHammett, 1);
		
		string.delete(0, string.length());
		string.append("“I couldn’t help but notice Hammett’s dedication to scientific accuracy,” you comment. “All the ");
		string.append("animals in the commercial were actually from the Jurassic. If anyone was going to bring back ");
		string.append("dinosaurs, I didn’t expect T. rexes to be left out.”<br><br>");
		string.append("“That’s because of me,” Alex says proudly. “Grandpa was going to, until I told him all about the ");
		string.append("differences between the Jurassic and Cretaceous.”<br><br>");
		string.append("“Plus, you know, the fact that resurrecting a giant, flesh eating apex predator isn’t generally a ");
		string.append("good idea,” Macomb deadpans.<br><br>");
		string.append("“Alex, did Hammett choose the Jurassic period because of you?” you ask him, not even ");
		string.append("expecting a ten year old to know the word Jurassic.<br><br>");
		string.append("“Uh huh,” Alex nods. “The Triassic didn’t have as many cool dinosaurs, and I didn’t want ");
		string.append("the ones in the Cretaceous because they looked too scary.”<br><br>");
		string.append("Muldrew glances at you. “He’s been obsessed with dinosaurs since he saw a picture of one when ");
		string.append("he was three. Talked about them non-stop and learned everything he could. His parents tried to ");
		string.append("get him into something ‘normal’ like baseball, but Hammett decided it was easier to just recreate ");
		string.append("the Jurassic. The fact that he can make money off it as a park is just a bonus to him,” he says ");
		string.append("calmly. Now Hammett’s obsession with historical accuracy finally makes sense.");

		Story askAccurate=new Story("(Requires Doctor) Ask Alex about the accuracy of the park", string.toString());
		story.addEdge(askUncle, askAccurate, 2);
		story.addEdge(askDino, askAccurate, 2);
		
		//end act 1
		string.delete(0, string.length());
		string.append("Suddenly, all of the lights in the lobby go out, as well as the air conditioning. If it wasn’t for the ");
		string.append("sunlight streaming in through the windows, you’d be in total darkness. Alex jumps a little at the ");
		string.append("sudden darkness, and both Muldrew and Macomb look around, as if trying to determine the cause.<br><br>");
		string.append("“Macomb, what is this?” Muldrew snaps.<br><br>");
		string.append("You can’t see Macomb's expression, but you can hear the surprise in his voice. “Hey, I never ");
		string.append("make the same mistake twice. After the last blackout, I added backup generators. They should be ");
		string.append("kicking in any second now.”<br><br>");
		string.append("After several seconds pass, you’re all still standing in the dark. “Macomb…” Muldrew growls.<br><br>");
		string.append("“Huh, that’s weird,” Macomb says, almost nonchalant, as if deep in thought. “I don’t know what ");
		string.append("could have caused this. The system’s redundant. It’s weather-proof, animal-proof, impact-proof…”");

		Story end=new Story("Continue the tour", string.toString());
		story.addEdge(quit, end, 1);
		//can quit early at any time
		story.addEdge(askDetails, end, 3);
		story.addEdge(askMilitary, end, 3);
		story.addEdge(askUncle, end, 3);
		story.addEdge(askDino, end, 3);
		//conversation is over
		story.addEdge(askHire, end, 1);
		story.addEdge(askDuties, end, 1);
		story.addEdge(askHammett, end, 1);
		story.addEdge(askAccurate, end, 1);		
		
		return end;
	}
	
	private static Story[] MakeArgument(Story start) //after power goes out, player chooses who to leave with
	{
		StringBuilder string=new StringBuilder(600); //STUPID BUG MAKES CLICKING LINK TWICE NOT WORK FIX
		string.append("You remember the encounter with Neary earlier, and ask Macomb “Wasn’t Neary going back to ");
		string.append("the substation before?”<br><br>");
		string.append("“Oh yeah, he was going to put Sarah back–” Macomb suddenly stops as he remembers Neary’s complaints.<br><br>");
		string.append("Muldrew sounds like he’s speaking through gritted teeth, instantly figuring out what Neary was ");
		string.append("doing with Macomb’s guitar. “Macomb, Neary and I both warned you about tripping hazards a dozen times. ");
		string.append("If your carelessness caused an accident, I swear…” He seems unwilling to finish ");
		string.append("his sentence with Alex in earshot.<br><br>");
		string.append("Macomb raises his hands. “Whoa, let’s not jump to conclusions here. Anything could’ve caused ");
		string.append("this,” he says, but doesn’t even sound like he believes it.<br><br>");
		string.append("“It doesn’t matter what the cause is, the fact is that the power’s out,” Muldrew is clearly agitated.");
		string.append("<br><br>“What’s the problem? Can’t you just turn it back on?” you ask him.<br><br>");
		string.append("Muldrew turns to look at you, and even with the lack of light, you can make out the steeliness in ");
		string.append("his gaze. “Yes, but the problem is that all the island’s electricity comes from the substation. That ");
		string.append("includes the dinosaur enclosures. Without power, there’s nothing to keep them in there.”");
		
		Story remind=new Story("Bring up Neary", string.toString());
		story.addEdge(start, remind, 1);
		
		string.delete(0, string.length());
		string.append("“Alright, nobody panic!” Macomb shouts. Muldrew shoots him a glare, and Alex looks like he barely ");
		string.append("even understands what's happening.<br><br>");
		string.append("Muldrew takes Alex’s hand, still glaring at Macomb. “We need to get Alex to safety.”<br><br>");
		string.append("“Oh yeah, just Alex. The rest of us will be fine with the rampaging dinosaurs on the loose.” You ");
		string.append("can practically hear the sarcasm drip from Macomb’s voice.<br><br>");
		string.append("Muldrew’s voice sounds intense, and his gaze probably is too. “Hammett put me in charge of ");
		string.append("Alex’s safety. He’s coming by tonight, and when he does, he expects his only grandson to be ");
		string.append("safe.” Alex is finally starting to understand the gravity of the situation, and is getting nervous now.");
		string.append("<br><br>“He also expects the island to be working and the dinosaurs to be locked away! We have to get ");
		string.append("the power back on. We can’t just hide and hope Hammett will find us before the dinosaurs do!” ");
		string.append("Macomb sounds frustrated by the situation, but also worried.<br><br>");
		string.append("“We can’t wrangle a dozen dinosaurs by ourselves. We find a safe place and wait for Hammett to ");
		string.append("pick us up, then deal with the dinosaurs after.” Muldrew has a plan, and has his mind set on it.<br><br>");
		string.append("“Nowhere is safe as long as the power’s out!” exclaims Macomb. “We just restart the generator, ");
		string.append("then we can use the dinosaurs' tracking chips to evade them. And we can actually call Hammett to tell him ");
		string.append("where we are, which we can't do without phone signal, which we don't have without power!” ");
		string.append("Macomb also has a plan, and seems equally convinced that he’s right.<br><br>");
		string.append("Neither man seems willing to budge from his position, and Alex is too dazed to even speak right now.");
		
		Story cont=new Story("Continue", string.toString());
		story.addEdge(remind, cont, 1);
		
		string.delete(0, string.length());
		string.append("“Can’t we do both? Splitting up is a terrible idea,” you caution.<br><br>");
		string.append("“Absolutely not,” Muldrew rumbles. “I’m taking Alex as far away from them as I can, not closer.”<br><br>");
		string.append("“The substation is in the same direction as some of the enclosures,” Macomb mutters to you with ");
		string.append("a sigh. “Muldrew, you have to come with me. When Hammett gets here, he’s not going to know ");
		string.append("what happened. It could take hours for him to get a team in here to look for us.” Macomb clearly ");
		string.append("doesn’t want to split up any more than you do.<br><br>");
		string.append("“You cannot be stupid enough to go to the substation alone.” Muldrew sounds furious, equally ");
		string.append("opposed to splitting up.<br><br>");
		string.append("“I won’t be alone,” Macomb shoots back. “Neary’s there too. And you can’t seriously think you ");
		string.append("can evade dinosaurs by yourself. You should come with us.”<br><br>");
		string.append("“I’m not going to take any chances with Alex’s life.” Muldrew sounds frustrated, but it’s mixed ");
		string.append("with a hint of pleading. “And you shouldn’t take any chances with yours. We don’t know what ");
		string.append("caused the blackout, and you don’t know if it can even be fixed. And Neary’s smarter than both ");
		string.append("of us; I’m sure he’s already found somewhere to hide. Just come to the lodges with us.”<br><br>");
		string.append("This argument clearly isn’t going anywhere, and they probably shouldn’t be wasting any more time ");
		string.append("when dinosaurs are on the loose.");
		
		Story neutral=new Story("Try to compromise", string.toString());
		story.addEdge(cont, neutral, 3);
		
		string.delete(0, string.length());
		string.append("“I think Muldrew is right.” The sound of your voice actually makes Macomb and Muldrew jump, so ");
		string.append("caught up in their argument that they’d forgotten you were there. “We just need to hold out until ");
		string.append("Hammett shows, and there’s no telling how broken the generator is. It might not be fixable.”<br><br>");
		string.append("Muldrew nods at you in appreciation, and Macomb can’t hide the hurt in his voice. “Fine, but ");
		string.append("I’m still going to the substation.”<br><br>");
		string.append("“Macomb–” you start, but he cuts you off, not wanting to hear it.<br><br>");
		string.append("“I can get it to work; I built the thing. And I’m not going to run and hide while Neary is out there ");
		string.append("alone.” Macomb sounds even more determined now.<br><br>");
		string.append("“Good luck, mate,” Muldrew says softly. He knows Macomb well enough to know there’s no ");
		string.append("way to dissuade him. But as much as it pains him to split up like this, he also refuses to be ");
		string.append("dissuaded from his own plan.<br><br>");
		string.append("“Yeah, thanks,” Macomb mutters, then heads out of the Center, the sudden influx of sunlight ");
		string.append("from the doors opening forcing your eyes into squint. He pauses on the steps and says “When the ");
		string.append("power comes back on, meet me at the control tower.” Then he leaves, alone.<br><br>");
		string.append("Muldrew watches him go, then turns to you and solemnly says “If his plan works. In the ");
		string.append("meanwhile, we should get going too.” In the light, you can now clearly see Alex’s expression. ");
		string.append("He’s staring at the spot where Macomb left, wide-eyed and frozen.");
		
		Story sideMuldrew= new Story("Side with Muldrew", string.toString());
		story.addEdge(cont, sideMuldrew, 1);
		story.addEdge(neutral, sideMuldrew, 1);
		
		string.delete(0, string.length());
		string.append("“I think Macomb is right.” The sound of your voice makes Macomb and Muldrew jump, so ");
		string.append("caught up in their argument that they’d forgotten you were there. “We can’t just try to outrun the ");
		string.append("dinosaurs; the island was literally made for them. We should try to get some control over the situation.”");
		string.append("<br><br>Macomb sighs in relief, and Muldrew can’t hide the frustration in his voice. “Fine, but Alex and ");
		string.append("I won’t be going with you.”<br><br>");
		string.append("“Muldrew–” you start, but he cuts you off, not wanting to hear it.<br><br>");
		string.append("“I can’t risk losing him. Hammett would never forgive me. I would never forgive myself. I just ");
		string.append("can’t justify putting Alex in danger” Muldrew sounds even more determined now.<br><br>");
		string.append("“I’m sorry,” Macomb says softly. He knows Muldrew well enough to know there’s no way to ");
		string.append("dissuade him. But as much as it pains him to split up like this, he also refuses to be dissuaded ");
		string.append("from his own plan.<br><br>");
		string.append("“I know, mate,” Muldrew says, voice equally soft. Macomb motions for you to follow him out of ");
		string.append("the Center, the sudden influx of sunlight from the doors opening forcing your eyes into a squint. ");
		string.append("He pauses on the steps and says to Muldrew “When the power comes back on, meet us at the ");
		string.append("control tower.” Then he starts walking again. But you can’t help but glance back one last time.<br><br>");
		string.append("Muldrew solemnly watches you two go with nothing but a nod of confirmation. In the light, ");
		string.append("you can now clearly see Alex’s expression. He’s staring at you, wide-eyed and frozen.");

		Story sideMacomb=new Story("Side with Macomb", string.toString());
		story.addEdge(cont, sideMacomb, 2);
		story.addEdge(neutral, sideMacomb, 2);
		
		Story[] toret= {sideMuldrew, sideMacomb};
		 
		return toret;
	}
	
	private static Story[] MakeMuldrewPath(Story start) //leave with muldrew and fend off pterosaur
	{
		StringBuilder string=new StringBuilder(600); 
		string.append("You have to hurry to keep up with Muldrew's quick pace. You can see that it’s now late into the ");
		string.append("afternoon, having arrived on Isla Soleada a little bit before noon. Despite the tension in the air, ");
		string.append("the clearing looks as peaceful as it did when you first arrived. There are no signs of any ");
		string.append("dinosaurs…yet.<br><br>");
		string.append("Muldrew leads you and Alex to a path with the sign reading “Lodges: COMING SOON”, in the opposite direction ");
		string.append("of where Macomb is going. You start walking down it, quickly being surrounded by trees to ");
		string.append("the left and right.<br><br>");
		string.append("“This is going to be a long walk, kid,” Muldrew suddenly says. “Hammett made sure the lodges were ");
		string.append("isolated from the rest of the island, which is why we're going there.”<br><br>");
		string.append("“Why'd he do that?” you ask.<br><br>");
		string.append("“There was too much empty space left after we finished building the enclosures. He couldn’t ");
		string.append("resist the chance to squeeze more money out of people with extended trips, but wanted to mitigate the security ");
		string.append("risks.” Muldrew still sounds as calm as ever, but you can see the regret still lingering in his eyes.");
		
		Story begin=new Story("Continue", string.toString());
		story.addEdge(start, begin, 1);
		
		//can ask a question
		string.delete(0, string.length());
		string.append("“How much do you know about these dinosaurs? You’ve spent a lot of time with them, right?” ");
		string.append("you ask Muldrew, looking around vigilantly in case one shows up.<br><br>");
		string.append("Muldrew nods. “I know them well enough, but they’ve never been out in the wild before. We ");
		string.append("raised them in their enclosures for safety reasons, but half of them are young and not fully trained yet.”<br><br>");
		string.append("“They shouldn’t act too differently now that they’re free, right?” you ask Muldrew.<br><br>");
		string.append("“You ever heard of alpha and beta wolves? The way males in wolf packs fight until they have a ");
		string.append("dominance hierarchy? Turns out that’s not quite true. Only captive wolves do that. The packs in ");
		string.append("the wild are like families,” he says ominously, then falls into brooding silence.<br><br>");
		string.append("You can’t help but notice how worried Alex looks. He hasn’t said a word since you left the ");
		string.append("Center, and is clutching Muldrew’s hand like it’s a lifeline.");

		Story askDino=new Story("Ask Muldrew about his preparedness", string.toString());
		story.addEdge(begin, askDino, 1);
		
		string.delete(0, string.length());
		string.append("“Is there seriously not a single vehicle on the entire island?” you ask, after walking down the ");
		string.append("forest path for several uneventful minutes.<br><br>");
		string.append("“No,” Muldrew says. “The only vehicles allowed are Hammett’s private jets, and they aren’t ");
		string.append("allowed to park, only pick up and drop off passengers; there’s no way to move beyond foot. ");
		string.append("Both for historical accuracy and security reasons.”<br><br>");
		string.append("“So, does Hammett walk everywhere when he’s here?” you ask him, not remembering the man’s");
		string.append("exact age, but knowing he’s even older than Muldrew and not nearly as fit.<br><br>");
		string.append("“Yes,” Muldrew replies. “80 years old, but I’ve never heard him complain about it once.” He has ");
		string.append("a hint of respect in his voice when he says this, but once he finishes talking, lapses into brooding ");
		string.append("silence again.<br><br>");
		string.append("You can’t help but notice how worried Alex looks. He hasn’t said a word since you left the ");
		string.append("Center, and is clutching Muldrew’s hand like it’s a lifeline.");
		
		Story askCar=new Story("Ask Muldrew if there are any vehicles you can use", string.toString());
		story.addEdge(begin, askCar, 2);
		
		//can reassure muldrew or alex
		string.delete(0, string.length());
		string.append("“Are you…okay?” you ask Muldrew, unsure if he even wants to talk about it.<br><br>");
		string.append("“I’m fine, kid,” Muldrew says.<br><br>");
		string.append("“You don’t look fine,” Alex says softly, looking up at him.<br><br>");
		string.append("Muldrew’s face softens when he looks at Alex, and he sighs. “I’m just…concerned about your uncle, okay?”");
		string.append("<br><br>“Uncle Ivan said he was going to meet us at the control tower. He always keeps his promises,” ");
		string.append("Alex says insistently.<br><br>");
		string.append("“I don’t know him as well as you do, but I don’t think he would’ve gone off on his own if he ");
		string.append("didn’t think he could make it,” you interject. Alex nods firmly, and squeezes Muldrew’s hand.<br><br>");
		string.append("A faint smile appears on Muldrew’s face, and he sighs. “Yeah, I know. He has a high opinion of ");
		string.append("his skills, but it’s not undeserved. Only reason I never told him was I didn’t want his ");
		string.append("head to get any bigger.” His lips curl upward almost reluctantly, and at least for the moment, he seems to ");
		string.append("have been snapped out of his brooding.");

		Story askMuldrew=new Story("Check on Muldrew", string.toString());
		story.addEdge(askDino, askMuldrew, 1);
		story.addEdge(askCar, askMuldrew, 1);
		
		string.delete(0, string.length());
		string.append("“Hey Alex, are you okay? you ask him, noticing the worry written on his face.<br><br>");
		string.append("“I miss Uncle Ivan,” he says softly, clearly worried about Macomb going off alone.<br><br>");
		string.append("Muldrew’s shoulders sag slightly, but he maintains his calm tone, injecting a hint of firmness. ");
		string.append("“Alex, look at me.” Alex obeys, eyes quivering. “Macomb will be fine. He practically made this ");
		string.append("island. If anyone can get the power back on, it’s him.”<br><br>");
		string.append("“Why couldn’t we go with him?” Alex asks.<br><br>");
		string.append("Muldrew sighs. “You know your grandfather would never allow that. He’d be devastated if ");
		string.append("anything happened to you.”<br><br>");
		string.append("You decide to take a chance and also try to reassure Alex. “You trust your uncle, right?” He nods ");
		string.append("firmly. “And he said he was going to turn the power back on, right?” Alex nods again. “Then I'm sure ");
		string.append("that’s what’s going to happen. I’m sure Macomb is doing everything he can to not let his favourite nephew down.” ");
		string.append("Alex smiles a little, sniffling, and Muldrew looks at you, silent but openly showing gratitude in his eyes.");

		Story askAlex=new Story("Check on Alex", string.toString());
		story.addEdge(askDino, askAlex, 2);
		story.addEdge(askCar, askAlex, 2);	
		
		//skip convo triggers fight
		string.delete(0, string.length());
		string.append("The three of you are suddenly startled by a high pitched screech, and look up to see a pterosaur ");
		string.append("flying in the distance above the trees. It’s the size of a huge bird like an albatross, with wings just ");
		string.append("as big. Unlike an albatross, it’s featherless and has a long tail, not to mention a beak full of ");
		string.append("jagged teeth, not too dissimilar from a crocodile.<br><br>");
		string.append("“Terry?” Alex asks, squinting to make out the reptile’s details.<br><br>");
		string.append("“A Dearc sgiathanach,” Muldrew growls to you, instinctively moving to stand in front of Alex ");
		string.append("protectively. Despite the tension in his stance, Muldrew looks as calm as ever.<br><br>");
		string.append("“Can you get it to back off?” you ask Muldrew.<br><br>");
		string.append("“Usually, but I didn’t get to feed him before the power went out, and their instincts ");
		string.append("kick in when they’re hungry. He doesn’t recognise you, and probably thinks you’re food.”");
		string.append("<br><br>The pterosaur is rapidly approaching, and has its beady eyes locked on you.");
		string.append(" The only things around you are the twigs, dirt, and stones on the forest floor.");

		Story askNothing=new Story("Stay silent", string.toString());
		story.addEdge(begin, askNothing, 3);
		story.addEdge(askCar, askNothing, 3);
		story.addEdge(askDino,askNothing, 3);
		
		//also trigger fight when convo complete
		Story pteroAttack=new Story("Continue", askNothing);
		story.addEdge(askMuldrew, pteroAttack, 1);
		story.addEdge(askAlex, pteroAttack, 1);
		
		//try to survive; choices randomised for extra fun
		string.delete(0, string.length());
		string.append("The pterosaur is clearly a Dearc sgiathanach, so you quickly rack your brain for everything you ");
		string.append("know on them. It ate aquatic animals, using its jagged teeth to capture slippery prey. It ate squid ");
		string.append("that were as big as you, circling the oceans until it found a meal. Once it had something in sight, ");
		string.append("the Dearc divebombed–similar to a hawk–and <em>grabbed it out of the open water</em> with its beak.");

		Story hint=new Story("(Requires Doctor) Recall what you know about the pterosaur", string.toString());
		story.addEdge(pteroAttack, hint, RandInt(false));
		
		string.delete(0, string.length());
		string.append("“Stand back; I’m handling this.” You command Muldrew and Alex like they're your subordinates, as your old ");
		string.append("officer habits start to kick in.<br><br>");
		string.append("“What are you doing?!” Muldrew yells as you start searching the ground for stones to throw.<br><br>");
		string.append("You manage to find one that’s both round and heavy enough, and pick it up, responding “I’m defending us.”");
		string.append("<br><br>When the pterosaur gets close enough for you to see its eyes, it’s already diving at you, opening ");
		string.append("its mouth as if wanting to swallow your head whole. Muldrew and Alex stares at you, respectively with ");
		string.append("gritted teeth and silent worry, as you stare it down, waiting for the right moment.<br><br>");
		string.append("Taking a deep breath, you throw the stone at the pterosaur’s open mouth.");
		
		Story fight=new Story("(Requires Lawyer) Fight", string.toString());
		story.addEdge(pteroAttack, fight, RandInt(false));
		
		string.delete(0, string.length());
		string.append("Your aim is perfect, and the pterosaur staggers from the force of the throw as the rock enters its ");
		string.append("mouth and gets lodged in its throat. The impact throws it off course, and it crashes into the ");
		string.append("ground several feet away from you, choking on the rock.<br><br>");
		string.append("It gets up as quickly as it can, and after several guttural retches, manages to spit out the rock. It ");
		string.append("turns to look at you, looking almost angry, and attempts to take to the skies for another attack.<br><br>");
		string.append("But its crash landing seems to have broken one of its arm-wings, leaving it stranded on the ");
		string.append("ground. Rather than try to attack you head on—the opposite of what it was built for—the ");
		string.append("pterosaur hobbles away into the forest, trying to avoid the human that almost killed it.<br><br>");
		string.append("Muldrew curses from relief. “I can’t believe you just took ");
		string.append("down a dinosaur.” He’s clearly in awe. Alex is completely wide-eyed at the sight.<br><br>");
		string.append("“I qualified as an Expert rifleman during boot camp,” you explain, still watching the pterosaur ");
		string.append("disappear into the trees.<br><br>");
		string.append("Muldrew snorts in disbelief. “They teach you how to throw stones in boot camp nowadays?”<br><br>");
		string.append("“No, but they taught us how to aim. Close enough,” you reply, finally relaxing as the adrenaline ");
		string.append("starts wearing off.");
		
		Story win=new Story("Continue", string.toString()); 
		story.addEdge(fight, win, 1);
		
		string.delete(0, string.length());
		string.append("“We need to get to cover!” you shout, and start running into the trees.<br><br>");
		string.append("Muldrew lifts Alex into a cradle carry and follows you without hesitation, as does the pterosaur. It’s ");
		string.append("close enough for you to see its eyes, its mouth opened as if it wants to swallow your head whole.<br><br>");
		string.append("You and Muldrew split up but stay within sight of each other, and it’s clear the pterosaur is only ");
		string.append("interested in you, the unfamiliar–and therefore not off-limits–human.<br><br>");
		string.append("When you see the pterosaur divebomb towards you, you wait a moment, making it think you’re ");
		string.append("vulnerable. When it gets close, you quickly hide behind a tree, using it as an obstacle between you and the ");
		string.append("pterosaur. Muldrew does the same, both he and Alex watching you tensely, hoping you made the right call.");
		string.append("<br><br>Its path to you now blocked, the pterosaur awkwardly swerves and aborts its divebomb to avoid ");
		string.append("taking a bite out of the tree. Then it resumes its flight and circles around for another go.<br><br>");
		string.append("Just like before, you use a tree as cover, almost like a matador with a bull. And just like before, it ");
		string.append("works; the pterosaur is unable to get a clear line of attack to you. Unlike a bull, it learns quickly, ");
		string.append("and decides you aren’t worth the trouble. It gives you a look that could almost be described as ");
		string.append("angry, then flies off to another part of the island, looking for easier prey.");
		
		Story hide=new Story("Hide", string.toString());
		story.addEdge(pteroAttack, hide, RandInt(false));
		story.addEdge(hint, hide, RandInt(true));
		
		string.delete(0, string.length());
		string.append("You quickly lie down on the ground and go limp, shouting for Muldrew and Alex to do the same. ");
		string.append("“We need to play dead!”<br><br>");
		string.append("“No, you idiot!” Muldrew yells back. “That just makes you an easier target!” He's seen the pterosaur kill before, and knows better.");
		string.append("<br><br>The pterosaur begins to divebomb when it gets closer, opening its mouth in anticipation of an easy meal.");
		string.append("<br><br>You scramble to get up, trying to run, and Muldrew curses, making a split second decision. He ");
		string.append("lets go of Alex and runs over to you to grab you by the wrist, trying to help you get out of the way.");
		string.append("<br><br>Unfortunately for him, that just puts him in the pterosaur’s path. It wraps its mouth around his ");
		string.append("head, its teeth sinking into his skin with a spray of blood, splattering onto your face and turning ");
		string.append("your vision red. You can barely see it when the pterosaur starts swallowing chunks of Muldrew, ");
		string.append("but you can hear his screaming just fine, even louder than Alex’s. You can barely think, and try ");
		string.append("to get the pterosaur off of him, but it swipes at you with one of its wings, bigger than one of your ");
		string.append("own arms.<br><br>");
		string.append("You fall to the ground and can’t do anything but watch as its serrated teeth rip the flesh off Muldrew’s bones.");
		string.append("<br><br>It eats quickly, and soon all that’s left of Muldrew is his body, mangled beyond recognition and ");
		string.append("almost completely stained red from the pterosaur’s messy eating. Once the pterosaur has had its ");
		string.append("fill, it flies away, leaving you and Alex alone.");
		
		Story feign=new Story("Play dead", string.toString());
		story.addEdge(pteroAttack, feign, RandInt(false));
		story.addEdge(hint, feign, RandInt(true));
		
		string.delete(0, string.length());
		string.append("“Run!” you shout at Muldrew and Alex, and start reversing direction, trying to put as much ");
		string.append("distance between you and the pterosaur as possible.<br><br>");
		string.append("Muldrew picks up Alex in a cradle carry, and starts running without hesitation. Even while ");
		string.append("carrying Alex, he still manages to run faster than you, and you quickly fall behind.<br><br>");
		string.append("The pterosaur begins to divebomb when it gets closer, opening its mouth in anticipation of an easy meal.");
		string.append("<br><br>Unfortunately for you, that’s exactly what you become when the pterosaur wraps its mouth ");
		string.append("around your head, its teeth sinking into your skin with a spray of blood, turning your vision red. ");
		string.append("You can’t see it when the pterosaur starts swallowing chunks of you, or hear when Alex starts ");
		string.append("screaming, the sound drowning out Muldrew’s rapid string of curses. You can’t even think, as ");
		string.append("your world is consumed by the pain of serrated teeth ripping the flesh off your bones.<br><br>");
		string.append("Though each moment feels like an eternity of agony to you, your life ends in a matter of seconds. ");
		string.append("You don’t even get the chance to wonder what you did wrong. Perhaps if you did, you’d come to ");
		string.append("realise that <em>it was a bad idea to try and outrun an animal that evolved to chase prey faster than ");
		string.append("humans</em>. But it’s too late for regrets now, because…<br><br>");
		string.append("<strong>You are dead.</strong>");
		
		Story run=new Story("Run", string.toString());
		story.addEdge(pteroAttack, run, RandInt(false));
		story.addEdge(hint, run, RandInt(true));
		
		//same as pteroAttack
		story.addEdge(askNothing, run, RandInt(false));
		story.addEdge(askNothing, hint, RandInt(false));
		story.addEdge(askNothing, fight, RandInt(false)); 
		story.addEdge(askNothing, hide, RandInt(false));
		story.addEdge(askNothing, feign, RandInt(false));
		
		Story[] endings=new Story[] {win, hide, feign}; //either everyone is alive or a companion dies; story ends if player dies
		
		return endings;
	}
	
	private static Story[] MakeMacombPath(Story start) //leave with macomb and fend off pterosaur
	{
		StringBuilder string=new StringBuilder(600); 
		string.append("You have to hurry to keep up with Macomb’s quick pace. You can see that it’s now late into the ");
		string.append("afternoon, having arrived on Isla Soleada a little bit before noon. Despite the tension in the air, ");
		string.append("the clearing looks as peaceful as it did when you first arrived. There are no signs of any ");
		string.append("dinosaurs…yet.<br><br>");
		string.append("Macomb leads you to the path with the sign reading “Substation: EMPLOYEES ONLY”, the ");
		string.append("same path Neary took before. You start walking down it, quickly being surrounded by trees to ");
		string.append("the left and right.<br><br>");
		string.append("“This is going to be a long walk,” Macomb suddenly says. “Hammett insisted the substation ");
		string.append("should be isolated from the rest of the island.”<br><br>");
		string.append("“For security reasons?” you guess.<br><br>");
		string.append("“Yeah. I’m starting to regret listening to him now,” Macomb mutters. His mood is unsurprisingly ");
		string.append("quite poor now, though when he glances at you, you can still see the gratitude in his eyes.");

		Story begin=new Story("Continue", string.toString());
		story.addEdge(start, begin, 1);
		
		//can ask a question
		string.delete(0, string.length());
		string.append("“How much do you know about these dinosaurs? You’ve worked with them, right?” you ask, ");
		string.append("looking around vigilantly in case one shows up.<br><br>");
		string.append("Macomb sighs. “Actually, no. I thought they were cool, but I never really paid attention to any of ");
		string.append("their specifics. Honestly, I just took this job for the money and bragging rights, but Hammett’s ");
		string.append("non-disclosure agreement took away one of those.” He mutters “This is seriously the last thing I ");
		string.append("ever thought would happen.”<br><br>");
		string.append("“You took a job on an isolated island full of dinosaurs and you never thought you’d be in ");
		string.append("danger?” you ask him.<br><br>");
		string.append("Macomb snorts, aware of the irony. “Of course not, I’m the one who designed the security. My ");
		string.append("designs have never failed.” He still sounds sore over the blackout happening.");

		Story askDino=new Story("Ask Macomb about his preparedness", string.toString());
		story.addEdge(begin, askDino, 1);
		
		string.delete(0, string.length());
		string.append("“Is there seriously not a single vehicle on the entire island?” you ask, after walking down the ");
		string.append("forest path for several uneventful minutes.<br><br>");
		string.append("“Nope,” Macomb says. “We walk everywhere, just like in ye olde prehistoric days.”<br><br>");
		string.append("“Even Hammett?” you ask him, not remembering the man’s exact age, but knowing he’s older ");
		string.append("than you and Macomb combined.<br><br>");
		string.append("“Heh, yeah,” Macomb says, with faint amusement. “He’s like 80 years old, but I’ve never once ");
		string.append("heard him complain. About the walking or the heat. He’s like a machine.” Both you and Macomb ");
		string.append("have long since been sweating from the Jurassic level temperatures.<br><br>");
		string.append("Then he lapses into silence again, looking pensive. You can clearly tell he’s still bothered about the blackout.");

		Story askCar=new Story("Ask Macomb if there are any vehicles you can use", string.toString());
		story.addEdge(begin, askCar, 2);
		
		//reassure macomb
		string.delete(0, string.length());
		string.append("“The blackout wasn’t your fault,” you tell Macomb, trying to sound convincing. “We don’t know ");
		string.append("what caused it.”<br><br>");
		string.append("“It’s not just that,” he sighs. “I’m worried about Muldrew and Alex too, all on their own…”<br><br>");
		string.append("“Muldrew is literally Alex’s bodyguard, right? I’m sure he knows what he’s doing,” you say.<br><br>");
		string.append("Macomb doesn’t look convinced. “No offence to Muldrew, but he’s getting old. And even if he "); 
		string.append("was in his prime, literal dinosaurs are on the loose. One guy can only do so much, even if that guy is him.”");
		string.append("<br><br>Macomb pauses and sighs a little. He makes eye contact with you, trying to force a smile. ");
		string.append("“Thanks for looking out for me though.” He’s referring to both back in the Center, and right now.<br><br>");
		string.append("“You’re welcome,” you tell him, though his expression is leaving you unsure how much your words actually helped.");

		Story reassure=new Story("Reassure Macomb he didn't do anything wrong", string.toString());
		story.addEdge(askDino, reassure, 1);
		story.addEdge(askCar, reassure, 1);
		
		string.delete(0, string.length());
		string.append("“You know, I am a Jurassic period paleontologist,” you start. “I don’t have as much experience ");
		string.append("with live dinosaurs as you do, but I do know a lot about them. If anything happens, I’m sure I’ll ");
		string.append("be able to help out.”<br><br>");
		string.append("Macomb smiles weakly. “Yeah, about that…we never even got to the tour, did we? Now if we ");
		string.append("see a dinosaur, it’s probably going to try and eat us.”<br><br>");
		string.append("“At least I’ll get to see a dinosaur,” you say, recalling the reason you came here in the first place. ");
		string.append("As terrifying as it would be for the human part of you to be a dinosaur’s prey, the paleontologist ");
		string.append("part of you is looking forward to the chance to see a real live dinosaur, something every ");
		string.append("paleontologist has dreamed of.");

		Story reassureDoc=new Story("(Requires Doctor) Reassure Macomb that you can help him survive", string.toString());
		story.addEdge(askDino, reassureDoc, 2);
		story.addEdge(askCar, reassureDoc, 2);
		
		string.delete(0, string.length());
		string.append("“You know, I am an ex-Marine,” you start. “I admit I don’t know much about dinosaurs, but if the worst ");
		string.append("comes to worst, I can at least help you fight one off.”<br><br>");
		string.append("Macomb can’t hold back a laugh. “I’m sorry, but you’ve never seen a dinosaur like I have, or you ");
		string.append("wouldn’t have said that. You can’t fight those things; they’re like super-animals.” After he stops ");
		string.append("laughing, he says more genuinely “I appreciate the thought though.”<br><br>");
		string.append("“They can’t all be that tough,” you say, thinking of dinosaurs like velociraptors and pterodactyls.<br><br>");
		string.append("Macomb smiles, thinking of dinosaurs like allosauruses and stegosauruses. “Yeah, we’ll see how ");
		string.append("much of a tough guy you are when one shows up,” he teases.");
		
		Story reassureLaw=new Story("(Requires Lawyer) Reassure Macomb you can help him survive", string.toString());
		story.addEdge(askDino, reassureLaw, 3);
		story.addEdge(askCar, reassureLaw, 3);
		
		//skip convo triggers fight
		string.delete(0, string.length());
		string.append("You and Macomb are suddenly startled by a high pitched screech, and look up to see a pterosaur ");
		string.append("flying in the distance above the trees. It’s the size of a huge bird like an albatross, with wings just ");
		string.append("as big. Unlike an albatross, it’s featherless and has a long tail, not to mention a beak full of ");
		string.append("jagged teeth, not too dissimilar from a crocodile.<br><br>");
		string.append("Macomb curses, going wide-eyed. “I don’t know which one that is; I think Muldrew called it the ‘jark’ or ");
		string.append("something. What do we do?”<br><br>");
		string.append("The pterosaur is rapidly approaching, and has its beady eyes locked on you.");
		string.append(" The only things around you are the twigs, dirt, and stones on the forest floor.");

		Story askNothing=new Story("Stay silent", string.toString());
		story.addEdge(begin, askNothing, 3);
		story.addEdge(askCar, askNothing, 4);
		story.addEdge(askDino,askNothing, 4);
		
		//also trigger fight when convo complete
		Story pteroAttack=new Story("Continue", askNothing);
		story.addEdge(reassure, pteroAttack, 1);
		story.addEdge(reassureDoc, pteroAttack, 1);
		story.addEdge(reassureLaw, pteroAttack, 1); 
		
		//try to survive; choices randomised for extra fun
		string.delete(0, string.length());
		string.append("The pterosaur is clearly a Dearc sgiathanach, so you quickly rack your brain for everything you ");
		string.append("know on them. It ate aquatic animals, using its jagged teeth to capture slippery prey. It ate squid ");
		string.append("that were as big as you, circling the oceans until it found a meal. Once it had something in sight, ");
		string.append("the Dearc divebombed–similar to a hawk–and <em>grabbed it out of the open water</em> with its beak.");

		Story hint=new Story("(Requires Doctor) Recall what you know about the pterosaur", string.toString());
		story.addEdge(pteroAttack, hint, RandInt(false));
		
		string.delete(0, string.length());
		string.append("You quickly lie down on the ground and go limp, shouting for Macomb to do the same. “We need to play dead!”");
		string.append("<br><br>He nods nervously and mimics your position, but still keeps his eyes on the pterosaur, silently ");
		string.append("panicking. The pterosaur begins to divebomb when it gets closer, opening its mouth in anticipation of an easy meal.");
		string.append("<br><br>Unfortunately for you, that’s exactly what you become when the pterosaur wraps its mouth ");
		string.append("around your head, its teeth sinking into your skin with a spray of blood, turning your vision red. ");
		string.append("You can’t see it when the pterosaur starts swallowing chunks of you, or hear when Macomb ");
		string.append("starts screaming. You can’t even think, as your world is consumed by the pain of serrated teeth ");
		string.append("ripping the flesh off your bones.<br><br>");
		string.append("Though each moment feels like an eternity of agony to you, your life ends in a matter of seconds. ");
		string.append("You don’t even get the chance to wonder what you did wrong. Perhaps if you did, you’d figure out that ");
		string.append("<em>all you did was make yourself an easier target for an active predator</em>. But it’s ");
		string.append("too late for regrets now, because…<br><br><strong>You are dead.</strong>");

		Story feign=new Story("Play dead", string.toString());
		story.addEdge(pteroAttack, feign, RandInt(false));
		story.addEdge(hint, feign, RandInt(true));
		
		string.delete(0, string.length());
		string.append("“We need to get to cover!” you shout, and start running into the trees.<br><br>");
		string.append("Macomb hesitates for a moment, but follows you, as does the pterosaur. It’s close enough for you ");
		string.append("to see its eyes, its mouth opened as if it wants to swallow your head whole.<br><br>");
		string.append("You and Macomb split up but stay within sight of each other, and it’s clear the pterosaur is only ");
		string.append("interested in you, probably because it already knows Macomb is off-limits.<br><br>");
		string.append("When you see the pterosaur divebomb towards you, you wait a moment, making it think you’re ");
		string.append("vulnerable. When it gets close, you quickly hide behind a tree, using it as an obstacle between ");
		string.append("you and the pterosaur. Macomb does the same, watching tensely, hoping you made the right call.<br><br>");
		string.append("Its path to you now blocked, the pterosaur awkwardly swerves and aborts its divebomb to avoid ");
		string.append("taking a bite out of the tree. Then it resumes its flight and circles around for another go.<br><br>");
		string.append("Just like before, you use a tree as cover, almost like a matador with a bull. And just like before, it ");
		string.append("works; the pterosaur is unable to get a clear line of attack to you. Unlike a bull, it learns quickly, ");
		string.append("and decides you aren’t worth the trouble. It gives you a look that could almost be described as ");
		string.append("angry, then flies off to another part of the island, looking for easier prey.");

		Story hide=new Story("Hide", string.toString());
		story.addEdge(pteroAttack, hide, RandInt(false));
		story.addEdge(hint, hide, RandInt(true));
		
		string.delete(0, string.length());
		string.append("“Run!” you shout at Macomb, and start reversing direction, trying to put as much distance ");
		string.append("between you and the pterosaur as possible.<br><br>");
		string.append("The pterosaur begins to divebomb when it gets closer, opening its mouth in anticipation of an easy meal.");
		string.append("<br><br>The two of you run as fast as you can, but Macomb is slower than you are, and the pterosaur is ");
		string.append("faster than both of you.<br><br>");
		string.append("Hunger overtaking it, the pterosaur switches targets from you to him, and wraps its mouth ");
		string.append("around his head, its teeth sinking into his skin with a spray of blood. You halt in your tracks at ");
		string.append("the sound of his scream, and turn around to see the pterosaur start to swallow chunks of ");
		string.append("Macomb. You can barely think, and try to get the pterosaur off of him, but it swipes at you with ");
		string.append("one of its wings, bigger than one of your own arms.<br><br>");
		string.append("You fall to the ground and can’t do anything but watch as its serrated teeth rip the flesh off ");
		string.append("Macomb’s bones.<br><br>");
		string.append("It eats quickly, and soon all that’s left of Macomb is his body, mangled beyond recognition and ");
		string.append("almost completely stained red from the pterosaur’s messy eating. Once the pterosaur has had its ");
		string.append("fill, it flies away, leaving you alone.");
		
		Story run=new Story("Run", string.toString());
		story.addEdge(pteroAttack, run, RandInt(false));
		story.addEdge(hint, run, RandInt(true));
		
		string.delete(0, string.length());
		string.append("“Stand back; I’m handling this.” You command Macomb like he’s your subordinate, as your old ");
		string.append("officer habits start to kick in.<br><br>");
		string.append("“What are you doing?!” he shouts as you start searching the ground for stones to throw.<br><br>");
		string.append("You manage to find one that’s both round and heavy enough, and pick it up, responding “I’m defending us.”");
		string.append("<br><br>When the pterosaur gets close enough for you to see its eyes, it’s already diving at you, opening ");
		string.append("its mouth as if wanting to swallow your head whole. Macomb stares at you, wide-eyed and ");
		string.append("frozen with panic, as you stare it down, waiting for the right moment.<br><br>");
		string.append("Taking a deep breath, you throw the stone at the pterosaur’s open mouth.<br><br>");
		string.append("Your aim is perfect, and the pterosaur staggers from the force of the throw as the rock enters its ");
		string.append("mouth and gets lodged in its throat. The impact throws it off course, and it crashes into the ");
		string.append("ground several feet away from you, choking on the rock.<br><br>");
		string.append("It gets up as quickly as it can, and after several guttural retches, manages to spit out the rock. It ");
		string.append("turns to look at you, looking almost angry, and attempts to take to the skies for another attack.<br><br>");
		string.append("But its crash landing seems to have broken one of its arm-wings, leaving it stranded on the ");
		string.append("ground. Rather than try to attack you head on—the opposite of what it was built for—the ");
		string.append("pterosaur hobbles away into the forest, trying to avoid the human that almost killed it.<br><br>");
		string.append("Macomb curses again, but this time in amazement instead of terror. “I can’t believe you just took ");
		string.append("down a dinosaur.” He’s so awed that he momentarily forgets pterosaurs are reptiles, not dinosaurs.<br><br>");
		string.append("“I qualified as an Expert rifleman during boot camp,” you explain, watching the pterosaur ");
		string.append("disappear into the trees.<br><br>");
		string.append("Macomb laughs in disbelief. “They teach you how to throw stones in boot camp?”<br><br>");
		string.append("“No, but they taught us how to aim. Close enough,” you reply, finally relaxing as the adrenaline ");
		string.append("starts wearing off.");
		
		Story fight=new Story("(Requires Lawyer) Fight", string.toString());
		story.addEdge(pteroAttack, fight, RandInt(false));
		
		//same as pteroAttack
		story.addEdge(askNothing, run, RandInt(false));
		story.addEdge(askNothing, hint, RandInt(false));
		story.addEdge(askNothing, fight, RandInt(false)); 
		story.addEdge(askNothing, hide, RandInt(false));
		story.addEdge(askNothing, feign, RandInt(false));
		
		Story[] endings=new Story[] {fight, hide, run}; //either everyone is alive or a companion dies; story ends if player dies
		
		return endings;
	}
	
	private static Story[] MakeMuldrewPath2(Story start1, Story start2) //make it to hotel with muldrew and alex but dilo is there
	{
		StringBuilder string=new StringBuilder(600); 
		string.append("Alex hugs Muldrew tightly, and Muldrew hugs him back for a long moment before speaking to ");
		string.append("you. “I have to admit, kid, I’m impressed. Not many people could handle that like you did.”<br><br>");
		string.append("“Thanks,” you say, finding a tree to lean on while waiting for your heart to stop trying to leap out ");
		string.append("of your chest. “Honestly, I wasn’t sure if that would work either.”<br><br>");
		string.append("“I’m glad <a href=\"dearc.html\">Terry</a> is still alive,” Alex says softly. “He was my second favourite.”");
		string.append("<br><br>Muldrew speaks softly. “Alex, I told you this before. They’re wild animals, not your friends. ");
		string.append("They do what they’re made to do, nothing more.”<br><br>");
		string.append("Alex doesn’t respond, just holds onto Muldrew’s hand, looking dejected and almost betrayed by ");
		string.append("Terry’s attack on you.<br><br>");
		string.append("Muldrew sighs softly and says to you gently “We should get going. Get to shelter before ");
		string.append("anything else shows up.”<br><br>");
		string.append("“Okay,” you nod at him, and the three of you continue walking down the path to the lodges.");

		Story begin=new Story("Continue", string.toString());
		story.addEdge(start1, begin, 1);
		story.addEdge(start2, begin, 1);
		
		//ask a question		
		string.delete(0, string.length());
		string.append("While you walk, something suddenly occurs to you, so you ask “How did the dinosaurs break ");
		string.append("out, anyway? What kind of enclosures do you have?”<br><br>");
		string.append("Muldrew glances at you. “Right, you never finished your tour. The enclosure barriers are mostly ");
		string.append("made out of stone; Hammett wanted them to blend in with the island,” he murmurs, referencing ");
		string.append("Hammett’s fixation on historical accuracy.<br><br>");
		string.append("“But just stone was too risky, so he let us add electric fences with voltage meant for elephants, ");
		string.append("and a layer of glass for visitors’ safety,” Muldrew says, then sighs.<br><br>");
		string.append("“The problem is they get aggressive when hungry, and now it’s far past their feeding time. I bet ");
		string.append("they noticed the fences were down, found a weak spot, and forced their way out,” Muldrew says ");
		string.append("sourly. Alex nods sadly, also able to imagine that happening.<br><br>");
		string.append("“You ‘bet’, not know? You mean you just assumed they were free this whole time?” you ");
		string.append("perceptively point out.<br><br>");
		string.append("Muldrew grunts. “Better to assume the worst and hope for the best. Besides, I was right, wasn’t I?”");

		Story askDino=new Story("Ask how the dinosaurs escaped", string.toString());
		story.addEdge(begin, askDino, 1);
		
		string.delete(0, string.length());
		string.append("While you walk, something suddenly occurs to you, so you ask Muldrew “Wait, if you’re in charge of ");
		string.append("security, shouldn’t you be armed?” You look him over again but see no weapons, lethal or non-lethal.<br><br>");
		string.append("Muldrew shakes his head. “You think Hammett would want any dinosaurs getting hurt, let alone ");
		string.append("killed? Each one is worth billions of dollars. He would never risk it.”<br><br>");
		string.append("“He never considered how dangerous they are?” you ask, with some disbelief.<br><br>");
		string.append("Muldrew sighs a little. “That’s exactly why they were locked up. Only Hammett, Neary, and I ");
		string.append("have the access codes to free them, and the substation is secured too. Making this place ");
		string.append("sabotage-proof was one of Hammett’s main priorities; that’s why there’s only three of us. And ");
		string.append("Macomb did days of testing: the generator never should’ve gone out in the first place.” He ");
		string.append("sounds annoyed that this is even happening, and Alex looks at him sympathetically.<br><br>");
		string.append("“Why is everything tied to one generator anyway?” you ask.<br><br>");
		string.append("Muldrew snorts softly. “Convenience, mainly. It was easier for Macomb to design the park this ");
		string.append("way, plus it cost Hammett less. That’s why there’s no signal either. We’re on our own until ");
		string.append("Hammett arrives.”");
		
		Story askGuns=new Story("Ask if there are any weapons", string.toString());
		story.addEdge(begin, askGuns, 2);
		
		string.delete(0, string.length());
		string.append("You fall into silence as you keep walking, but thankfully don’t encounter any more dinosaurs. ");
		string.append("Eventually the trees around you start to part, and you, Muldrew, and Alex reach the end of the path.");
		string.append("<br><br>In front of you is an open area on the side of the island. The only thing in it is an incomplete ");
		string.append("building overlooking the ocean below; it’s made of wood with stone foundations like the ");
		string.append("Visitor’s Center, but its design is clearly meant to be a three storey hotel. Currently, it’s little more ");
		string.append("than a shell. You can see scaffolding still attached to the incomplete roof, and it doesn’t even have ");
		string.append("windows yet, allowing you to see that its interior is bare.<br><br>");
		string.append("As you walk closer, Muldrew tells you “Hammett wanted the island clear while you were here. ");
		string.append("Apparently thinks it’s more professional-looking this way.” Then he mutters “Lucky for the ");
		string.append("contractors too; at least they’re safe.”<br><br>");
		string.append("You’re almost at the doorless entrance when you hear a low growling sound behind you. You all ");
		string.append("slowly turn around and see a dinosaur emerging from the same path you came from.<br><br>");
		string.append("This one is much bigger than the pterosaur, around the size and length of a pickup truck. ");
		string.append("However, it’s a completely different species, earthbound and walking on two feet. It has two ");
		string.append("hard-looking crests on top of its snout, and two shortened arms like a T. rex. Its mouth is full of ");
		string.append("curved, sharp teeth, and its eyes are looking at you. It slowly begins to approach you, still growling.");
		string.append("<br><br>This time, there’s nothing around and nowhere to go except for the unfinished hotel at your back, ");
		string.append("as the dinosaur is blocking the path back into the forest.");

		Story encounter=new Story("Continue in silence", string.toString());
		story.addEdge(begin, encounter, 3);
		story.addEdge(askDino, encounter, 1);
		story.addEdge(askGuns, encounter, 1);
		
		//dino fight time
		string.delete(0, string.length());
		string.append("“I’ve got this, trust me,” you whisper to Muldrew and Alex.");
		string.append("“You’d better,” Muldrew mutters, looking highly doubtful and tense, but willing to trust you after ");
		string.append("you helped them escape the pterosaur. But he moves protectively in front of Alex anyway. Alex ");
		string.append("looks less doubtful and more curious about what you’re going to do.<br><br>");
		string.append("You clear your throat and stand a bit straighter, calling on both your officer training and lawyer ");
		string.append("skills for this. “STAND DOWN!” you order the dinosaur like it’s an unruly subordinate.<br><br>");
		string.append("It only growls softly and stares at you blankly. Both Muldrew and Alex are giving you similar ");
		string.append("blank stares.<br><br>");
		string.append("You place your hands on your hips, and speak as loudly as you can without actually yelling, ");
		string.append("mimicking your drill instructor. “United States Code of Law, Title 18, Section 113. Quote. ");
		string.append("Whoever, within the special maritime and territorial jurisdiction of the United States, is guilty of ");
		string.append("an assault shall be punished as follows: Assault with intent to commit murder…by a fine under ");
		string.append("this title, imprisonment for not more than 20 years, or both. End quote.”<br><br>");
		string.append("The dinosaur is still staring at you, but still hasn’t attacked yet. Muldrew and Alex are looking ");
		string.append("at you like you’ve completely lost your mind.");
		
		Story dominate=new Story("(Requires Lawyer) Try to assert your dominance", string.toString());
		story.addEdge(encounter, dominate, RandInt(false));
		
		string.delete(0, string.length());
		string.append("“Do you have any idea how stupid you are? Are you even literate? Twenty years of your life! ");
		string.append("What are you, four? Five? You can’t even begin to understand how much trouble you’re facing!” ");
		string.append("When the dinosaur growls at you again, you yell “You SHUT UP when I’m talking to you!”<br><br>");
		string.append("That actually works, and it stops growling, so you ignore the way your heart is hammering ");
		string.append("against your ribcage and keep lecturing it. “Attacking an old man and a child? Are you trying to ");
		string.append("get your sentence maxed?! Do you want to throw your entire life away because of one stupid ");
		string.append("decision? Twenty years! That’s two decades of no education and no income. Your kids will grow ");
		string.append("up without you; you’ll miss their whole childhood. And don’t get me started on how bad prison is.”");
		string.append("<br><br>You quickly take a deep breath and continue, while the dinosaur shifts in place uncertainly. ");
		string.append("“You think you’re so tough? Huh? Threatening people who can’t fight back? Wait until you go to ");
		string.append("prison, then we’ll see how tough you are! You won’t last one day with an attitude like that. If ");
		string.append("you’re anything other than a suicidal halfwit, you better plead guilty and hope to your creator ");
		string.append("you land probation, because you sure won’t make it in prison.”<br><br>");
		string.append("The dinosaur hisses at you, and looks like it’s had enough of your lecturing. It’s apparently decided ");
		string.append("you aren’t worth eating, and turns around, walking off into the forest in search of less aggressive prey.");
		
		Story lecture=new Story("Keep it up", string.toString());
		story.addEdge(dominate, lecture, 1);
		
		string.delete(0, string.length());
		string.append("You practically deflate from relief, and for the first time since you’ve met him, you hear Muldrew laugh. ");
		string.append("You turn around and find him literally wheezing from laughter; Alex is also giggling uncontrollably.");
		string.append("<br><br>You would ask what’s so funny, but considering that you just threatened a <a href=\"dilo.html\">dinosaur</a> ");
		string.append("with a prison sentence for trying to eat you, you can’t blame them. Their laughter is infectious, ");
		string.append("and you can't help but join in.<br><br>");
		string.append("It takes a few minutes for you all to calm down, and when you do, Muldrew has to wipe tears ");
		string.append("from his eyes. “I thought you were a prosecutor,” he says to you.<br><br>");
		string.append("“I am,” you respond with a smile. “But I know a few defence attorneys.”<br><br>");
		string.append("“I bet none of them will believe you when you tell them about this,” Muldrew says, struggling ");
		string.append("not to burst out laughing again.<br><br>");
		string.append("“I can’t tell them anyway,” you sigh. “Hammett made me sign a non-disclosure before I even got ");
		string.append("on the jet here.”<br><br>");
		string.append("Muldrew sighs too. “Of course he did. Talking about a dinosaur attack is bad publicity anyway.”<br><br>");
		string.append("After a pause, he seems to remember where he is, and says “Right. We should get into the hotel ");
		string.append("now that it’s clear.”<br><br>You nod, and follow Muldrew and Alex inside.");

		Story leave=new Story("Continue", string.toString());
		story.addEdge(lecture, leave, 1);
		
		string.delete(0, string.length());
		string.append("“Follow my lead,” you whisper to Muldrew.<br><br>");
		string.append("You take out your phone, which thankfully still has charge in it, and turn on the flashlight, ");
		string.append("shining it at the dinosaur.<br><br>");
		string.append("The dinosaur flinches and hisses at the sudden burst of light, and Muldrew quickly takes out his ");
		string.append("own phone, doing the same.<br><br>");
		string.append("You keep the flashlight on, and raise your hands above your head to appear bigger and thus more ");
		string.append("threatening. Muldrew mirrors your movements.<br><br>");
		string.append("“Go away!” you shout at the dinosaur. “We are not food!”<br><br>");
		string.append("“Piss off, Dilbert,” Muldrew yells. It snaps its head over at Muldrew when he calls its name, and ");
		string.append("he shines the light directly in its eyes, making it wince.<br><br>");
		string.append("Your strategy quickly produces results, as it only takes a few more shouts before the dinosaur ");
		string.append("decides you aren’t worth eating, and turns around, walking off into the forest in search of less ");
		string.append("aggressive prey.");

		Story scare=new Story("Try to scare it off", string.toString());
		story.addEdge(encounter, scare, RandInt(false));
		
		string.delete(0, string.length());
		string.append("Once it’s out of sight, you turn off your flashlight, and Muldrew does the same. “You’re good at ");
		string.append("this,” Alex remarks, with clear admiration over the fact that you’ve now warded off two different dinosaurs.");
		string.append("<br><br>“It works on most wild animals,” you explain. “Make yourself look bigger and talk to let them ");
		string.append("know you aren’t their usual easy prey. The flashlight was impulsive, but I didn’t know if the ");
		string.append("usual methods would be enough with a <a href=\"dilo.html\">dinosaur</a>.”<br><br>");
		string.append("“Of course, I should’ve thought of that,” Muldrew groans, sounding annoyed at himself. “I’m a ");
		string.append("zookeeper.” He mutters “I can barely think straight with all these dinosaurs running loose.”<br><br>");
		string.append("Alex giggles at Muldrew, then asks you “How do you know how to fight wild animals?”<br><br>");
		string.append("“I saw a public service announcement as a kid. ‘Grizzley the Bear says it’s up to you to stay safe ");
		string.append("when hiking,’” you answer, recalling the way the hat wearing bear would intermittently appear ");
		string.append("during commercial breaks to teach hikers how to stay safe.<br><br>");
		string.append("“Course you did, so did I. They were showing those when I was a kid,” Muldrew grumbles, still ");
		string.append("annoyed, but only half-serious. “Let’s just get in the hotel.”<br><br>");
		string.append("You nod, and follow Muldrew and Alex inside.");
		
		Story victory=new Story("Continue", string.toString());
		story.addEdge(scare, victory, 1);
		
		string.delete(0, string.length());
		string.append("“We have to play dead,” you whisper to the others.<br><br>");
		string.append("“That’s never going to work,” Muldrew hisses, eyeing the dinosaur warily.<br><br>");
		string.append("“If it thinks we aren’t a threat, it’ll leave us alone,” you insist.<br><br>");
		string.append("“It’s a carnivore! It eats meat, and it’s hungry! You think it cares if you’re still moving or not?” ");
		string.append("Muldrew says, raising his voice and sounding irritated that you don’t have basic animal knowledge.<br><br>");
		string.append("The dinosaur seems to have finished assessing you, and takes your quietness and stillness as a ");
		string.append("sign that you’re a suitable meal. It charges at you, surprisingly fast for its size.<br><br>");
		string.append("Muldrew curses and grabs Alex, and moves to the side, yelling “Dilbert, stop!” to no avail.<br><br>");
		string.append("You panic when you see the hundreds of pounds of muscle and flesh suddenly rush you, staring ");
		string.append("straight into its open mouth. You try to run, but the dinosaur is far faster than you.<br><br>");
		string.append("The dinosaur wraps its mouth around your body, and you can’t help but let out a scream when you feel its ");
		string.append("many sharp teeth sink into your skin. Its bite tightens, and you feel its teeth start to rip your body apart, ");
		string.append("slicing your flesh with ease.<br><br>");
		string.append("Muldrew and Alex are both staring at you in horror. Tears are streaming down Alex’s face. ");
		string.append("“RUN!” you shout to them, before the dinosaur rips out a huge chunk of the flesh from your ");
		string.append("back, and swallows it whole. You scream again, and it’s enough to jolt them into action.<br><br>");
		string.append("Muldrew grabs Alex and turns to go, giving you one last glance that’s a mixture of guilt at not ");
		string.append("being able to stop your death, and gratitude for your focus on their safety. You try to reassure ");
		string.append("them one last time, but the dinosaur’s next bite tears the skin and meat right off your chest, and ");
		string.append("you begin hemorrhaging blood. The pain is so intense you can barely think, but you’re at least ");
		string.append("glad that they were able to escape. The last thing you see is the dinosaur’s mouth engulfing your ");
		string.append("head, before your life ends with one last wet crunch.<br><br><strong>You died.</strong>");

		Story die=new Story("Try to play dead", string.toString());
		story.addEdge(encounter, die, RandInt(false));
		
		string.delete(0, string.length());
		string.append("“We need to hide inside,” you whisper to the others.<br><br>");
		string.append("“Your idea worked last time,” Muldrew mutters, and he nods, deferring to your dinosaur ");
		string.append("surviving expertise.<br><br>");
		string.append("Keeping your eyes on the dinosaur, you slowly back up until your back is to the hotel wall. ");
		string.append("Muldrew and Alex do the same, but the dinosaur takes a step forwards with every step you move back.");
		string.append("<br><br>You slip inside the hotel and so do Muldrew and Alex. “Where do we hide?” Muldrew whispers ");
		string.append("urgently, as the dinosaur keeps advancing closer.<br><br>");
		string.append("You look around and notice that none of the rooms even have doors installed yet. There’s not ");
		string.append("even any furniture for you to hide behind. Muldrew curses when the dinosaur follows, barely ");
		string.append("managing to squeeze through the double-door-sized entrance. He and you realise at the same ");
		string.append("time that you made a mistake. With nowhere to hide or run, all you’ve done is trap them inside the ");
		string.append("building with a hungry dinosaur.");
		
		Story hide=new Story("Try to hide in the hotel", string.toString());
		story.addEdge(encounter, hide, RandInt(false));
		
		string.delete(0, string.length());
		string.append("You all keep backing up slowly, and Muldrew’s eyes fill with resignation. “Keep Alex safe for ");
		string.append("me,” he tells you solemnly.<br><br>");
		string.append("“Uncle Roland?” Alex asks him with concern, able to tell something is wrong.<br><br>");
		string.append("Muldrew gently pushes Alex towards you, and you take Alex’s hand, understanding what ");
		string.append("Muldrew is doing and unable to stop it.<br><br>");
		string.append("“Love you, Alex,” Muldrew says. Then he turns to face the dinosaur and shouts “Dilbert, it’s ");
		string.append("feeding time!” Muldrew turns around and tries to lead the dinosaur upstairs, away from you and Alex. ");
		string.append("It immediately starts to chase him, running right past you two without a second glance.<br><br>");
		string.append("Muldrew is fast, but the dinosaur is much faster. He barely steps foot on the stairs before the ");
		string.append("dinosaur puts his body between its open jaws and clamps down in a spray of red. Muldrew and ");
		string.append("Alex scream almost simultaneously.<br><br>");
		string.append("“RUN–” Muldrew starts, before the dinosaur rips out and swallows a huge chunk of his back, his blood ");
		string.append("running into the gaps in the floorboards as he cries out in agony.<br><br>");
		string.append("Alex keeps screaming and tries to run over to Muldrew, but you forcibly lift him into a fireman’s ");
		string.append("carry, knowing Muldrew is right.<br><br>");
		string.append("Alex kicks you a few times as he struggles to get free, sobbing and screaming his uncle’s name ");
		string.append("while you turn your back on him and start running, understanding that Muldrew’s sacrifice will ");
		string.append("be pointless if you don’t.");

		Story run=new Story("Panic", string.toString());
		story.addEdge(hide, run, 1);
		
		string.delete(0, string.length());
		string.append("Muldrew’s screams echo after you as you head outside, mixing with Alex’s, but you ignore both ");
		string.append("and keep running like your life depends on it.<br><br>");
		string.append("You head down the path you came from, moving until you physically can’t anymore, but at least ");
		string.append("manage to make it to the clearing with the Visitor’s Center before collapsing.<br><br>");
		string.append("Alex gets off you, now completely silent, having stopped screaming some time after Muldrew’s ");
		string.append("screams abruptly stopped. When you look over at him, you can see that his tears have dried up, ");
		string.append("but he looks shattered. He sits down in the dirt without even looking at you, and you lie there for ");
		string.append("a moment, having to catch your breath before doing anything else.<br><br>");
		string.append("If there’s any good news, it’s that Muldrew’s plan worked. The dinosaur didn’t even try chasing ");
		string.append("after you, too occupied with gorging itself on its meal. You and Alex are safe, at least for now.");

		Story escape=new Story("Continue", string.toString());
		story.addEdge(run, escape, 1);
		
		string.delete(0, string.length());
		string.append("You easily identify the dinosaur based on the crests on its head. It’s obviously a dilophosaurus. ");
		string.append("They’re <em>carnivores–pursuit predators–and pretty good ones</em>. They likely preyed on ");
		string.append("smaller animals, and were lightweight (for a dinosaur), fast and agile (for a dinosaur), and had ");
		string.append("two types of teeth. The back teeth were serrated, for tearing through flesh–dead or alive–and the ");
		string.append("front teeth were curved, for digging into skin and holding prey in its jaws.");

		Story hint=new Story("(Requires Doctor) Try to identify the dinosaur", string.toString());
		story.addEdge(encounter, hint, RandInt(false));
		
		story.addEdge(hint, hide, RandInt(true));
		story.addEdge(hint, die, RandInt(true)); 
		story.addEdge(hint, scare, RandInt(true));
		
		Story[] endings=new Story[] { victory, leave, escape}; //first two no one dies; last one muldrew dies
		
		return endings;
	}
	
	private static Story[] MakeAlexPath(Story start) //make it to hotel with alex but dilo is there
	{
		StringBuilder string=new StringBuilder(600);
		string.append("You make your way over to Muldrew’s body, on the off-chance he’s alive.<br><br>");
		string.append("He isn’t. Even if he was breathing, all that’s left of his face are rags of bloody flesh hanging off ");
		string.append("his skull. His throat and chest have been torn open, and the dirt around his body is stained as red ");
		string.append("as your shirt and face. You’ve never seen so much gore in your life, but the smell is even worse. ");
		string.append("It’s blood mixed with sweat mixed with bile from Muldrew’s torn open stomach, and the stench, ");
		string.append("combined with the sight, is enough to make you vomit. You at least manage to direct it away ");
		string.append("from Muldrew’s body.<br><br>");
		string.append("You belatedly wipe the blood off your face and realise Alex is still off to the side, staring at the body ");
		string.append("and quietly sobbing. You need to decide what to do next.");

		Story begin=new Story("Continue", string.toString());
		story.addEdge(start, begin, 1);
		
		//player can choose level of emotional attachment before leaving		
		string.delete(0, string.length());
		string.append("You walk over to Alex, kneeling and looking him in his tear-filled eyes. “Alex, I’m sorry,” is all ");
		string.append("you can say.<br><br>");
		string.append("Alex finally tears his gaze away from Muldrew’s body and wraps his arms around you, not ");
		string.append("seeming to notice or care about the blood on your shirt. He puts his head on your shoulder and ");
		string.append("sobs wordlessly for what feels like hours, but is probably no more than a few minutes.<br><br>");
		string.append("You hug him back, somewhat awkward, but at least glad that he doesn’t seem to blame you for ");
		string.append("Muldrew’s death.<br><br>");
		string.append("When he’s finally finished, Alex sniffles and says shakily “We can’t leave him there.”<br><br>");
		string.append("Muldrew’s body is already starting to attract insects, and will likely attract much bigger ");
		string.append("scavengers if left in the open like that.");

		Story hug=new Story("Comfort Alex", string.toString());
		story.addEdge(begin, hug, 1);
		
		string.delete(0, string.length());
		string.append("You look at the flies gathering around Muldrew’s body and decide you need to do something, but ");
		string.append("you don’t have a shovel or anything even close to one.<br><br>");
		string.append("Alex notices your hesitance and quickly figures out what you’re thinking. He softly suggests ");
		string.append("“We can cover him up.”<br><br>");
		string.append("You look around and easily spot several sizable rocks, then get to work gathering them and ");
		string.append("placing them on top of Muldrew’s body. You don’t even want to try to move it, given that it ");
		string.append("almost looks like it’ll fall apart if you try.<br><br>");
		string.append("Alex silently joins in, and eventually the two of you have Muldrew’s body buried underneath a ");
		string.append("pile of rocks, which should keep scavengers away for now.<br><br>");
		string.append("Alex doesn’t say anything, and you don’t know Muldrew enough to even try and give him a ");
		string.append("eulogy, so there’s nothing left to do but move on.");

		Story bury=new Story("Bury Muldrew", string.toString());
		story.addEdge(begin, bury, 2);
		story.addEdge(hug, bury, 1);
		
		string.delete(0, string.length());
		string.append("“We should get moving,” you tell Alex. His eyes can’t help but well up with fresh tears at the ");
		string.append("thought of leaving Muldrew behind.<br><br>");
		string.append("“Muldrew would want you to stay safe, and we can only do that if we get to shelter,” you tell ");
		string.append("him. After a pause, you also add “And once we’re safe, your grandfather can send someone to get him.”");
		string.append("<br><br>The mention of seeing Hammett again is enough to give Alex some resolve, and he nods ");
		string.append("wordlessly, wiping his tears away before they can fall. He doesn’t say anything as he follows you ");
		string.append("down the path to the lodges. As you walk, the sight of Muldrew’s body gradually fades into the distance behind you.");

		Story leave=new Story("Move on", string.toString());
		story.addEdge(begin, leave, 3);
		story.addEdge(hug, leave, 2);
		story.addEdge(bury, leave, 1);
		
		string.delete(0, string.length());
		string.append("You fall into uncomfortable silence as you keep walking, but thankfully don’t encounter any more dinosaurs. ");
		string.append("Eventually the trees around you start to part, and you and Alex reach the end of the path.<br><br>");
		string.append("In front of you is an open area on the side of the island. The only thing in it is an incomplete ");
		string.append("building overlooking the ocean below; it’s made of wood with stone foundations like the ");
		string.append("Visitor’s Center, but its design is clearly meant to be a three storey hotel. Currently, it’s little ");
		string.append("more than a shell. You can see scaffolding still attached to the incomplete roof, and it doesn’t ");
		string.append("even have windows yet, allowing you to see that its interior is completely bare.<br><br>");
		string.append("You’re almost at the doorless entrance when you hear a low growling sound behind you. You both ");
		string.append("slowly turn around and see a dinosaur emerging from the same path you came from.<br><br>");
		string.append("This one is much bigger than the pterosaur, around the size and length of a pickup truck. ");
		string.append("However, it’s a completely different species, earth  bound and walking on two feet. It has two ");
		string.append("hard-looking crests on top of its snout, and two shortened arms like a T. rex. Its mouth is full of ");
		string.append("curved, sharp teeth, and its eyes are looking at you. It slowly begins to approach you, still growling.");
		string.append("<br><br>This time, there’s nothing around and nowhere to go except for the unfinished hotel at your back, ");
		string.append("as the dinosaur is blocking the path back into the forest.");
		
		Story encounter=new Story("Continue", string.toString());
		story.addEdge(leave, encounter, 1);
		
		//dino fight		
		string.delete(0, string.length());
		string.append("“I’ve got this, trust me,” you whisper to Alex.<br><br>");
		string.append("Alex looks doubtful but says nothing, not having any ideas himself.<br><br>");
		string.append("You clear your throat and stand a bit straighter, calling on both your officer training and lawyer ");
		string.append("skills for this. “STAND DOWN!” you order the dinosaur like it’s an unruly subordinate.<br><br>");
		string.append("It only growls softly and stares at you blankly. Alex gives you a similar blank stare.<br><br>");
		string.append("You place your hands on your hips, and speak as loudly as you can without actually yelling, ");
		string.append("trying to mimic your old drill instructor. “United States Code of Law, Title 18, Section 113. ");
		string.append("Quote. Whoever, within the special maritime and territorial jurisdiction of the United States, is ");
		string.append("guilty of an assault shall be punished as follows: Assault with intent to commit murder…by a ");
		string.append("fine under this title, imprisonment for not more than 20 years, or both. End quote.”<br><br>");
		string.append("The dinosaur is staring back at you, but still hasn’t attacked yet. Alex is looking ");
		string.append("at you like you’ve completely lost it.");
		
		Story dominate=new Story("(Requires Lawyer) Try to assert your dominance", string.toString());
		story.addEdge(encounter, dominate, RandInt(false));
		
		string.delete(0, string.length());
		string.append("“Do you have any idea how stupid you are? Are you even literate? Twenty years of your life! ");
		string.append("What are you, four? Five? You can’t even begin to understand how much trouble you’re in!” ");
		string.append("When the dinosaur growls at you again, you yell “You SHUT UP when I’m talking to you!”<br><br>");
		string.append("That actually works, and it stops growling, so you ignore the way your heart is hammering ");
		string.append("against your ribcage and keep lecturing it. “Attacking an old man and a child? Are you trying to ");
		string.append("get your sentence maxed?! Do you want to throw your entire life away because of one stupid ");
		string.append("decision? Twenty years! That’s two decades of no education and no income. Your kids will grow ");
		string.append("up without you; you’ll miss their whole childhood. And don’t get me started on how bad prison is.”<br><br>");
		string.append("You quickly take a deep breath and continue, while the dinosaur shifts in place uncertainly. ");
		string.append("“You think you’re so tough? Huh? Threatening people who can’t fight back? Wait until you go to ");
		string.append("prison, then we’ll see how tough you are! You won’t last one day with an attitude like that. If ");
		string.append("you’re anything other than a suicidal halfwit, you better plead guilty and hope to your creator ");
		string.append("you land probation, because you sure won’t make it in prison.”<br><br>");
		string.append("The dinosaur hisses at you, and looks like it’s had enough of your lecturing. It’s apparently ");
		string.append("decided you aren’t worth eating, and turns around, walking off into the forest in search of less ");
		string.append("aggressive prey.");

		Story lecture=new Story("Keep it up", string.toString());
		story.addEdge(dominate, lecture, 1);
		
		string.delete(0, string.length());
		string.append("You practically deflate from relief, and you hear Alex start to giggle uncontrollably.<br><br>");
		string.append("You would ask what’s so funny, but considering that you just threatened a <a href=\"dilo.html\">dinosaur</a> ");
		string.append("with a prison sentence for trying to eat you, you can’t blame him. His laughter is infectious, and you can’t ");
		string.append("help but join in.<br><br>");
		string.append("It takes a few minutes for you two to calm down, and when you do, Alex asks you ");
		string.append("“How'd you do that?”<br><br>");
		string.append("“I know a few defence attorneys,” you respond with a smile. “And I'm used to arguing with people for a living.”");
		string.append("<br><br>");
		string.append("“That's so cool,” Alex says. “I bet the attorneys will think so too!”<br><br>");
		string.append("“I can’t tell them,” you sigh. “Hammett made me sign a non-disclosure before I even got ");
		string.append("on the jet here.” Talking about a rampaging dinosaur would be bad publicity for him anyway.<br><br>");
		string.append("After a pause, you remember where you are, and say “Anyway, we should get into the hotel ");
		string.append("now that it’s clear.”<br><br>");
		string.append("Alex nods, and follows you inside.");

		Story safe=new Story("Continue", string.toString());
		story.addEdge(lecture, safe, 1);
		
		string.delete(0, string.length());
		string.append("“Follow my lead,” you whisper to Alex.<br><br>");
		string.append("You take out your phone, which thankfully still has charge in it, and turn on the flashlight, ");
		string.append("shining it at the dinosaur.<br><br>");
		string.append("The dinosaur flinches and hisses at the sudden burst of light, and Alex quickly takes out his ");
		string.append("own phone, doing the same.<br><br>");
		string.append("You keep the flashlight on, and raise your hands above your head to appear bigger and thus more ");
		string.append("threatening. Alex mirrors your movements.<br><br>");
		string.append("“Go away!” you shout at the dinosaur. “We are not food!”<br><br>");
		string.append("“Leave us alone, Dilbert,” Alex yells. It snaps its head over at Alex when he calls its name, and ");
		string.append("he shines the light directly in its eyes, making it wince.<br><br>");
		string.append("Your strategy quickly produces results, as it only takes a few more shouts before the dinosaur ");
		string.append("decides you aren’t worth eating, and turns around, walking off into the forest in search of less ");
		string.append("aggressive prey.");

		Story scare=new Story("Try to scare it off", string.toString());
		story.addEdge(encounter, scare, RandInt(false));
		
		string.delete(0, string.length());
		string.append("Once it’s out of sight, you turn off your flashlight, and Alex does the same. “How'd you know that ");
		string.append("would work?” Alex asks.<br><br>");
		string.append("“It works on most wild animals,” you explain. “Make yourself look bigger and talk to let them ");
		string.append("know you aren’t their usual easy prey. The flashlight was impulsive, but I didn’t know if the ");
		string.append("usual methods would be enough with a <a href=\"dilo.html\">dinosaur</a>.”<br><br>");
		string.append("Alex asks you “How do you know how to fight wild animals?”<br><br>");
		string.append("“I saw a public service announcement as a kid. ‘Grizzley the Bear says it’s up to you to stay safe ");
		string.append("when hiking,’” you answer, recalling the way the hat wearing bear would intermittently appear ");
		string.append("during commercial breaks to teach hikers how to stay safe.<br><br>");
		string.append("“Who's that?” Alex asks innocently, having grown up on streaming, totally unaccustomed to ads.<br><br>");
		string.append("“I can explain later,” you sigh. “Let’s just get in the hotel.”<br><br>");
		string.append("Alex nods and follows you inside.");

		Story survive=new Story("Continue", string.toString());
		story.addEdge(scare, survive, 1);
		
		string.delete(0, string.length());
		string.append("“We have to play dead,” you whisper to Alex.<br><br>");
		string.append("“Are you sure?” he whispers back, eyeing the dinosaur warily. He doesn't seem to think it's a good idea.");
		string.append("<br><br>“If it thinks we aren’t a threat, it’ll leave us alone,” you insist.<br><br>");
		string.append("“But it's a carnivore! It eats meat that's dead and alive,” Alex responds, having seen it feed before.");
		string.append("<br><br>The dinosaur seems to have finished assessing you, and takes your quietness and stillness as a ");
		string.append("sign that you’re a suitable meal. It charges at you, surprisingly fast for its size.<br><br>");
		string.append("Alex instinctively moves away from you, and yells “Dilbert, stop!” to no avail.<br><br>");
		string.append("You panic when you see the hundreds of pounds of muscle and flesh suddenly rush you, staring ");
		string.append("straight into its open mouth. You try to run, but the dinosaur is far faster than you.<br><br>");
		string.append("The dinosaur wraps its mouth around your body, and you can’t help but let out a scream when ");
		string.append("you feel its many sharp teeth sink into your skin. Its bite tightens, and you feel its teeth start to ");
		string.append("rip your body apart, slicing your flesh with ease.<br><br>");
		string.append("Alex is staring at you, wide-eyed and tears streaming down his face. “RUN!” you shout to him, ");
		string.append("before the dinosaur rips out a huge chunk of the flesh from your back, and swallows it whole. ");
		string.append("You scream again, and it’s enough to jolt Alex into action.<br><br>");
		string.append("Alex turns and runs out of the hotel, giving you one last glance that’s a mixture of terror and ");
		string.append("guilt. You try to reassure him one last time, but the dinosaur’s next bite tears the skin and meat ");
		string.append("off your chest, and you begin hemorrhaging blood. The pain is so intense you can barely think, ");
		string.append("but you're at least glad that Alex was able to escape. ");
		string.append("The last thing you see is the dinosaur’s mouth engulfing your head, before your life ends with ");
		string.append("one last wet crunch.<br><br><strong>You died.</strong>");
		
		Story die=new Story("Try to play dead", string.toString());
		story.addEdge(encounter, die, RandInt(false));
		
		string.delete(0, string.length());
		string.append("“We need to hide inside,” you whisper to Alex.<br><br>");
		string.append("“Okay,” Alex whispers, looking afraid, but not having any better ideas.<br><br>");
		string.append("Keeping your eyes on the dinosaur, you slowly back up until your back is to the hotel wall. Alex ");
		string.append("does the same, but the dinosaur takes a step forwards with every step you move back.<br><br>");
		string.append("You slip inside the hotel and so does Alex. “Where do we hide?” Alex whispers urgently, as the ");
		string.append("dinosaur keeps advancing closer.<br><br>");
		string.append("You look around and notice that none of the rooms even have doors installed yet. There’s not ");
		string.append("even any furniture for you to hide behind. Then the dinosaur follows, barely managing to ");
		string.append("squeeze through the double-door-sized entrance. You realise that you made a mistake. With ");
		string.append("nowhere to hide, all you’ve done is trap yourself inside the building with a hungry dinosaur.");

		Story hide=new Story("Try to hide in the hotel", string.toString());
		story.addEdge(encounter, hide, RandInt(false));
		
		string.delete(0, string.length());
		string.append("You look at Alex, and he looks back at you, trembling, but completely innocent and trusting. You ");
		string.append("know what you have to do.<br><br>");
		string.append("You quickly pick Alex up, too fast for him to even realise what you’re doing, and throw him as ");
		string.append("hard as you can, towards the stairs. He’s surprisingly light, and lands by the bottom of the stairs, ");
		string.append("his head hitting the floor with a thud. The dinosaur’s head snaps over to look at him.<br><br>");
		string.append("Alex sits up and tears immediately start flowing from his eyes. He stares at you, looking ");
		string.append("shocked, betrayed, and heartbroken, like a kicked puppy. He starts bawling, not even trying to ");
		string.append("stand, some part of him knowing that he’s going to die.<br><br>");
		string.append("Your plan worked, and the dinosaur seems attracted to Alex’s crying. It walks up to him without ");
		string.append("so much as glancing at you, looming over him ominously. Alex forces out a “WHY?”, his voice ");
		string.append("full of hopelessness, as if he knows the answer doesn’t even matter.<br><br>");
		string.append("You don’t even get a chance to respond. The dinosaur wraps its jaws around the entire top half of ");
		string.append("Alex’s body and slams its mouth shut, silencing his cries in an instant. ");

		Story killAlex=new Story("Sacrifice Alex", string.toString());
		story.addEdge(hide, killAlex, 2);
		
		string.delete(0, string.length());
		string.append("You look at Alex, and he looks back at you, trembling, but completely innocent and trusting. You ");
		string.append("know what you have to do.<br><br>");
		string.append("You say solemnly to Alex “Stay safe. If Macomb gets the power back on, go find him by the ");
		string.append("control tower. If not, then get to the Visitor’s Center and wait for your grandfather to get you.”<br><br>");
		string.append("Alex’s eyes start quivering. “Aren’t you coming with me?”<br><br>");
		string.append("“No. I’m sorry,” you tell Alex, before you turn to face the dinosaur.<br><br>");
		string.append("“Hey, you stupid animal!” you shout at it, waving your hands. “Come and get me!”<br><br>");
		string.append("“NO!” Alex screams, but it’s too late. You start running to the stairs to get it away from Alex, ");
		string.append("and your plan works perfectly. The dinosaur chases after you, faster than you even expected, and ");
		string.append("catches up to you before you can even put your foot on the first step.<br><br>");
		string.append("The dinosaur wraps its mouth around your body, and you can’t help but let out a scream when ");
		string.append("you feel its many sharp teeth sink into your skin. Its bite tightens, and you feel its teeth start to ");
		string.append("rip your body apart, slicing your flesh with ease.<br><br>");
		string.append("Alex is staring at you, wide-eyed and tears streaming down his face. “RUN!” you shout to him, ");
		string.append("before the dinosaur rips out a huge chunk of the flesh from your back, and swallows it whole. ");
		string.append("You scream again, and it’s enough to jolt Alex into action.<br><br>");
		string.append("Alex turns and runs, giving you one last glance that’s a mixture of terror and ");
		string.append("guilt. You try to reassure him one last time, but the dinosaur’s next bite tears the skin and meat ");
		string.append("off your chest, and you begin hemorrhaging blood. The pain is so intense you can’t even think, ");
		string.append("but you don’t regret your choice even for a second, glad that Alex was at least able to escape. ");
		string.append("The last thing you see is the dinosaur’s mouth engulfing your head, before your life ends with ");
		string.append("one last wet crunch.<br><br><strong>You died.</strong>");
		
		Story killSelf=new Story("Sacrifice yourself", string.toString());
		story.addEdge(hide, killSelf, 1);
		
		string.delete(0, string.length());
		string.append("While the dinosaur starts tearing Alex’s body apart with noisy crunches, you turn your back and ");
		string.append("start running. You leave the hotel and run back down the path, trying to put as much distance as ");
		string.append("possible between you and the creature.<br><br>");
		string.append("You keep running until you physically can’t anymore, but at least manage to make it to the clearing ");
		string.append("with the Visitor’s Center before you collapse. You lie on the ground, trying to catch your breath.");
		string.append("<br><br>Thankfully, your plan worked; the dinosaur didn’t even try to chase after you, too busy gorging ");
		string.append("itself on its meal, as small as it was. You are safe, at least for now.");
		
		Story shadow=new Story("Continue", string.toString());
		story.addEdge(killAlex, shadow, 1);
		
		string.delete(0, string.length());
		string.append("You easily identify the dinosaur based on the crests on its head. It’s obviously a dilophosaurus. ");
		string.append("They’re <em>carnivores–pursuit predators–and pretty good ones</em>. They likely preyed on ");
		string.append("smaller animals, and were lightweight (for a dinosaur), fast and agile (for a dinosaur), and had ");
		string.append("two types of teeth. The back teeth were serrated, for tearing through flesh, and the front teeth ");
		string.append("were curved, for digging into skin and holding prey in its jaws.");

		Story hint=new Story("(Requires Doctor) Try to identify the dinosaur", string.toString());
		story.addEdge(encounter, hint, RandInt(false));
		story.addEdge(hint, hide, RandInt(true));
		story.addEdge(hint, die, RandInt(true));
		story.addEdge(hint, scare, RandInt(true));
		
		Story[] endings=new Story[] {survive, safe, shadow}; //alex survives in first two; dies in last
		
		return endings;
	}
	
	private static Story[] MakeMacombPath2(Story start1, Story start2) //make it to substation with macomb but dilo is there
	{
		StringBuilder string=new StringBuilder(600); 
		string.append("Macomb slumps to the ground, a hand in his hair. “I don’t know what’s crazier, the fact that a ");
		string.append("<a href=\"dearc.html\">pterosaur</a> tried to eat us, or the fact that we survived it.”<br><br>");
		string.append("You find a tree to lean against while waiting for your heart to stop trying to leap out of your ");
		string.append("chest. “Actually, it looked like it was just after me,” you note.<br><br>");
		string.append("“Heh, probably because Muldrew trained the dinosaurs to leave the rest of us alone. Or tried to, ");
		string.append("anyway,” Macomb says.<br><br>"); 
		string.append("“What do you mean ‘tried to’?” you ask him.<br><br>");
		string.append("He looks at you, sounding more serious. “I mean dinosaurs are crazy. They’ve never seen ");
		string.append("humans before, so they barely even tolerate Muldrew. He doesn’t really train them ");
		string.append("so much as keep them from killing us. And when they get hungry, they can’t even do that. ");
		string.append("Their predator instincts take over or something.”<br><br>");
		string.append("“How often do you feed them?” you ask him.<br><br>");
		string.append("“Three times a day, but they eat a lot, so it’s more ‘how much’ do we feed them. Muldrew ");
		string.append("already gave them breakfast, but the power went out before their lunchtime. That’s probably why ");
		string.append("they’re going psycho on us. If this had only happened an hour or two later, they probably ");
		string.append("wouldn’t have even broken out,” Macomb sighs.<br><br>");
		string.append("After a pause, he says “Speaking of, we should probably fix the power before another one tries to ");
		string.append("eat us, huh?”<br><br>");
		string.append("The two of you slowly get up and resume your walk to the substation.");

		Story begin=new Story("Continue", string.toString());
		story.addEdge(start1, begin, 1);
		story.addEdge(start2, begin, 1);
		
		//ask a question
		string.delete(0, string.length());
		string.append("While you walk, something suddenly occurs to you, so you ask “How did the dinosaurs break ");
		string.append("out, anyway? What kind of enclosures do you have?”<br><br>");
		string.append("Macomb hesitates for a moment, then remembers you never finished (or really even started) your ");
		string.append("tour. “Right, uh, they’re made of stone mostly. But they have electric fences for safety and glass ");
		string.append("for viewing…except the fences aren’t working now. I guess they noticed and smashed their way out.”<br><br>");
		string.append("“You ‘guess’? Earlier you and Muldrew made it sound like you knew for a fact they were loose,” ");
		string.append("you say, referring to their argument in the Visitor’s Center.<br><br>");
		string.append("Macomb chuckles, a little sheepish. “Yeah, I know. It was an assumption, but I figured if anyone ");
		string.append("should make it, it’d be Muldrew, so I went along with it. Anyway, it doesn’t really matter how ");
		string.append("they’re free now, because they are.”");

		Story askDino=new Story("Ask how the dinosaurs escaped", string.toString());
		story.addEdge(begin, askDino, 1);
		
		string.delete(0, string.length());
		string.append("While you walk, something suddenly occurs to you, so you ask “Wait, are there any weapons on ");
		string.append("the island?”<br><br>");
		string.append("Macomb shakes his head and laughs a little bit. “You think Hammett would want any of his ");
		string.append("precious dinosaurs getting hurt, let alone killed? They’re each worth billions.”<br><br>");
		string.append("“He never considered how dangerous they are?” you ask, with some disbelief.<br><br>");
		string.append("Macomb sighs. “That’s exactly why they were locked up. As long as no one let them out, we ");
		string.append("were completely safe. Making this place immune to sabotage was one of Hammett’s main ");
		string.append("demands, and that’s what I did. I even tested the generator for days to make sure it wouldn’t ");
		string.append("accidentally give out.” He sounds almost nostalgic as he describes the work he did in the early ");
		string.append("days of the park.<br><br>");
		string.append("“Why is everything tied to one generator anyway?” you ask.<br><br>");
		string.append("Macomb grins. “Convenience. It was easier for me to design, and cheaper for Hammett. It was a ");
		string.append("win for both of us.” He sounds almost proud, which makes sense considering how difficult ");
		string.append("Hammett is to please. “The downside is we don’t even have any signal without power,” he says ");
		string.append("more seriously. “Not that it would matter, I guess, because it’d take a while for anyone to reach ");
		string.append("us out here. We’re on our own for now.”");
		
		Story askGuns=new Story("Ask if there are any weapons", string.toString());
		story.addEdge(begin, askGuns, 2);
		
		string.delete(0, string.length());
		string.append("You fall into silence as you keep walking, but thankfully don’t encounter any more dinosaurs. ");
		string.append("Eventually the trees around you start to part, and you and Macomb reach the end of the path.<br><br>");
		string.append("In front of you is the substation, deep in the forest and surrounded by trees. It actually appears to ");
		string.append("be made of metal instead of stone, and looks a lot like an above ground bunker, with a single ");
		string.append("entrance and no windows. The door has a keypad above the handle, but oddly there’s a shoe stuck in the ");
		string.append("doorway, keeping the door from closing. For some reason, there isn’t a single power transformer in sight.");
		string.append("<br><br>Macomb sees you staring and smiles. “Cool, huh? Since visitors aren’t going to see it, Hammett ");
		string.append("let me build it with metal, plus it’s more secure this way. The door's extra thick and closes on its ");
		string.append("own. The shoe is new, but we have a generator to restart.”<br><br>");
		string.append("He literally takes a single step before you suddenly hear a low growling sound behind you. You ");
		string.append("both slowly turn around and see a dinosaur emerging from the path you just came from.<br><br>");
		string.append("This one is much bigger than the pterosaur, around the size and length of a pickup truck. ");
		string.append("However, it’s a completely different species, earthbound and walking on two feet. It has two ");
		string.append("hard-looking crests on top of its snout, and two shortened arms like a T. rex. Its mouth is full of ");
		string.append("curved, sharp teeth, and its eyes are looking at you. It slowly begins to approach you, still growling.");
		string.append("<br><br>As before, there are sticks and stones in the dirt around you. The substation is behind you, no ");
		string.append("more than a few metres away.");
		
		Story encounter=new Story("Continue in silence", string.toString());
		story.addEdge(begin, encounter, 3);
		story.addEdge(askDino, encounter, 1);
		story.addEdge(askGuns, encounter, 1);
		
		//next dino fight
		string.delete(0, string.length());
		string.append("“I’ve got this, trust me,” you whisper to Macomb.<br><br>");
		string.append("“I hope so,” Macomb mutters, looking nervous, but willing to trust you, especially since you ");
		string.append("already dealt with the pterosaur.<br><br>");
		string.append("You clear your throat and stand a bit straighter, calling on both your officer training and lawyer ");
		string.append("skills for this. “STAND DOWN!” you order the dinosaur like it’s an unruly subordinate.<br><br>");
		string.append("It only growls softly and stares at you blankly. Macomb gives you a surprised look.<br><br>");
		string.append("You place your hands on your hips, and speak as loudly as you can without actually yelling, ");
		string.append("trying to mimic your old drill instructor. “United States Code of Law, Title 18, Section 113. ");
		string.append("Quote. Whoever, within the special maritime and territorial jurisdiction of the United States, is ");
		string.append("guilty of an assault shall be punished as follows: Assault with intent to commit murder…by a ");
		string.append("fine under this title, imprisonment for not more than 20 years, or both. End quote.”<br><br>");
		string.append("The dinosaur is staring back at you, but still hasn’t attacked yet. Macomb is looking at you like ");
		string.append("you’ve completely lost it.");

		Story dominate=new Story("(Requires Lawyer) Try to assert your dominance", string.toString());
		story.addEdge(encounter, dominate, RandInt(false));
		
		string.delete(0, string.length());
		string.append("“Do you have any idea how stupid you are? Are you even literate? Twenty years of your life! ");
		string.append("What are you, four? Five? You can’t even begin to understand how much trouble you’re in!” ");
		string.append("When the dinosaur growls at you again, you yell “You SHUT UP when I’m talking to you!”<br><br>");
		string.append("That actually works, and it stops growling, so you ignore the way your heart is hammering ");
		string.append("against your ribcage and keep lecturing it. “Attacking an old man and a child? Are you trying to ");
		string.append("get your sentence maxed?! Do you want to throw your entire life away because of one stupid ");
		string.append("decision? Twenty years! That’s two decades of no education and no income. Your kids will grow ");
		string.append("up without you; you’ll miss their whole childhood. And don’t get me started on how bad prison is.”");
		string.append("<br><br>You quickly take a deep breath and continue, while the dinosaur shifts in place uncertainly. “You ");
		string.append("think you’re so tough? Huh? Threatening people who can’t fight back? Wait until you go to ");
		string.append("prison, then we’ll see how tough you are! You won’t last one day with an attitude like that. If ");
		string.append("you’re anything other than a suicidal halfwit, you better plead guilty and hope to your creator ");
		string.append("you land probation, because you sure won’t make it in prison.”<br><br>");
		string.append("The dinosaur hisses at you, and looks like it’s had enough of your lecturing. It’s apparently ");
		string.append("decided you aren’t worth eating, and turns around, walking off into the forest in search of less ");
		string.append("aggressive prey.");
		
		Story lecture=new Story("Keep it up", string.toString());
		story.addEdge(dominate, lecture, 1);
		
		string.delete(0, string.length());
		string.append("You practically deflate from relief, and Macomb suddenly uncontrollably bursts into laughter, a ");
		string.append("mixture of relief and disbelief.<br><br>");
		string.append("You would ask what’s so funny, but considering that you just threatened a <a href=\"dilo.html\">dinosaur</a> ");
		string.append("with a prison sentence for trying to eat you, you can’t blame him. His laughter is infectious, ");
		string.append("and you can't help but join in.<br><br>");
		string.append("It takes a few minutes for you two to calm down, and when you do, Macomb has to wipe tears ");
		string.append("from his eyes. “I thought you were a prosecutor,” he says to you.<br><br>");
		string.append("“I am,” you respond with a smile. “But I know a few defence attorneys.”<br><br>");
		string.append("“I bet none of them will believe you when you tell them about this,” Macomb says grinning.<br><br>");
		string.append("“I can’t tell them anyway,” you sigh. “Hammett made me sign a non-disclosure before I even got ");
		string.append("on the jet here.”<br><br>");
		string.append("Macomb snorts. “What am I saying? Of course he did. Talking about a dinosaur attack is bad ");
		string.append("publicity anyway.”<br><br>");
		string.append("After a pause, he seems to remember where he is, and says “Oh, right. We should get into the ");
		string.append("substation now.”<br><br>");
		string.append("You nod, and follow Macomb to the door.");
		
		Story win=new Story("Continue", string.toString());
		story.addEdge(lecture, win, 1);
		
		string.delete(0, string.length());
		string.append("“Follow my lead,” you whisper to Macomb.<br><br>");
		string.append("You take out your phone, which thankfully still has charge in it, and turn on the flashlight, ");
		string.append("shining it at the dinosaur.<br><br>");
		string.append("The dinosaur flinches and hisses at the sudden burst of light, and Macomb quickly takes out his ");
		string.append("own phone, doing the same.<br><br>");
		string.append("You keep the flashlight on, and raise your hands above your head to appear bigger and thus more ");
		string.append("threatening. Macomb mirrors your movements.<br><br>");
		string.append("“Go away!” you shout at the dinosaur. “We are not food!”<br><br>");
		string.append("“I bet I taste like sweat anyway!” Macomb yells. It snaps its head over at Macomb when he ");
		string.append("suddenly speaks, and he shines the light directly in its eyes, making it wince.<br><br>");
		string.append("Your strategy quickly produces results, as it only takes a few more shouts before the dinosaur ");
		string.append("decides you aren’t worth eating, and turns around, walking off into the forest in search of less ");
		string.append("aggressive prey.");

		Story scare=new Story("Try to scare it off", string.toString());
		story.addEdge(encounter, scare, RandInt(false));
		
		string.delete(0, string.length());
		string.append("Once it’s out of sight, you turn off your flashlight, and Macomb does the same. “You’re good at ");
		string.append("this,” he remarks, with clear admiration over the fact that you’ve now warded off two different ");
		string.append("dinosaurs.<br><br>");
		string.append("“It works on most wild animals,” you explain. “Make yourself look bigger and talk to let them ");
		string.append("know you aren’t their usual easy prey. The flashlight was impulsive, but I didn’t know if the ");
		string.append("usual methods would be enough with a <a href=\"dilo.html\">dinosaur</a>.”<br><br>");
		string.append("Macomb raises his eyebrow and asks you “And how do you know how to fight wild animals?”<br><br>");
		string.append("“I saw a public service announcement as a kid. ‘Grizzley the Bear says it’s up to you to stay safe ");
		string.append("when hiking,’” you answer, recalling the way the hat wearing bear would intermittently appear ");
		string.append("during commercial breaks to teach hikers how to stay safe.<br><br>");
		string.append("“Oh, those things. They were showing those when I was a kid too,” Macomb says. “I totally ");
		string.append("forgot about them. Ironic, huh? Anyway, let’s just get to the substation.”<br><br>");
		string.append("You nod, and follow Macomb to the door.");

		Story escape=new Story("Continue", string.toString());
		story.addEdge(scare, escape, 1);
		
		string.delete(0, string.length());
		string.append("“We have to play dead,” you whisper to Macomb.<br><br>");
		string.append("“Are you sure that’s a good idea?” Macomb whispers, eyeing the dinosaur warily.<br><br>");
		string.append("“If it thinks we aren’t a threat, it’ll leave us alone,” you insist.<br><br>");
		string.append("“But it’s a carnivore! Won’t it just eat us anyway?” Macomb asks, sounding confused and conflicted.");
		string.append("<br><br>The dinosaur seems to have finished assessing you, and takes your quietness and stillness as a ");
		string.append("sign that you’re a suitable meal. It charges at you, surprisingly fast for its size.<br><br>");
		string.append("Macomb curses and instinctively moves away from you.<br><br>");
		string.append("You panic when you see the hundreds of pounds of muscle and flesh suddenly rush at you, ");
		string.append("staring straight into its open mouth. You try to run, but the dinosaur is far faster than you.<br><br>");
		string.append("The dinosaur wraps its mouth around your body, and you can’t help but let out a scream when ");
		string.append("you feel its many sharp teeth sink into your skin. Its bite tightens, and you feel its teeth start to ");
		string.append("rip your body apart, slicing your flesh with ease.<br><br>");
		string.append("Macomb is staring at you in horror. “RUN!” you shout to him, before the dinosaur rips out a ");
		string.append("huge chunk of the flesh from your back, and swallows it whole. You scream again, and it’s ");
		string.append("enough to jolt him into action.<br><br>");
		string.append("Macomb runs to the substation door, giving you one last glance that’s a mixture of guilt at not ");
		string.append("being able to stop your death, and gratitude for your focus on his safety. You try to reassure him ");
		string.append("one last time, but the dinosaur’s next bite tears the skin and meat right off your chest, and you ");
		string.append("begin hemorrhaging blood. The pain is so intense you can barely think, but you at least hear the ");
		string.append("substation door slamming shut, and know Macomb made it. The last thing you see is the ");
		string.append("dinosaur’s mouth engulfing your head, before your life ends with one last wet crunch.");
		string.append("<br><br><strong>You died.</strong>");
		
		Story die=new Story("Try to play dead", string.toString());
		story.addEdge(encounter, die, RandInt(false));
		
		string.delete(0, string.length());
		string.append("“We need to hide inside,” you whisper to Macomb.<br><br>");
		string.append("“Your idea worked last time,” he mutters, and nods, deferring to your dinosaur surviving expertise.");
		string.append("<br><br>Keeping your eyes on the dinosaur, you slowly back up until your back is to the substation. ");
		string.append("Macomb does the same, but the dinosaur takes a step forwards with every step you move back.<br><br>");
		string.append("Macomb reaches the cracked open door first, and tries to push it further open with his hands, but ");
		string.append("it barely budges. He curses and whispers “I forgot the door is weighted. Hold on.”<br><br>");
		string.append("Macomb turns around and grabs the handle with both of his hands, grunting a little as he pulls ");
		string.append("the door wide open enough for you to get in.<br><br>");
		string.append("Unfortunately for Macomb, the moment the dinosaur sees his turned back, it considers him ");
		string.append("viable prey and starts charging.<br><br>");
		string.append("Macomb’s eyes widen and he yells at you “Get in!”");

		Story hide=new Story("Try to hide in the substation", string.toString());
		story.addEdge(encounter, hide, RandInt(false));
		
		string.delete(0, string.length());
		string.append("You hurry inside the substation, and Macomb tries to follow you, but it’s too late. The dinosaur is ");
		string.append("far faster than him, and gets to him first. He doesn’t even get his foot in the doorway before the ");
		string.append("dinosaur puts his body between its open jaws and clamps down in a spray of red.<br><br>");
		string.append("Macomb screams and lets go of the door, its weight making it start to automatically swing shut ");
		string.append("again, but the shoe blocks it from fully closing.<br><br>");
		string.append("“GO–” Macomb starts, before the dinosaur rips out and swallows a huge chunk of his back, his ");
		string.append("blood dripping onto the dirt as he cries out in agony.<br><br>");
		string.append("As much as it feels like you’re abandoning Macomb, you remove the shoe so the door can fully ");
		string.append("close, knowing there’s nothing you can do to save him. The last thing you see through the crack ");
		string.append("of the closing door is the dinosaur putting Macomb’s head between its jaws, then ending his life ");
		string.append("with a wet crunch.<br><br>");
		string.append("You can still hear the muffled sounds of the dinosaur eating, but at least Macomb is no longer ");
		string.append("screaming. You listen to it tear off and swallow his flesh for what feels like an eternity, before the ");
		string.append("dinosaur is finally satisfied and stomps off into the forest.");
		
		Story survive=new Story("Move quickly", string.toString());
		story.addEdge(hide, survive, 1);
		
		string.delete(0, string.length());
		string.append("You easily identify the dinosaur based on the crests on its head. It’s obviously a dilophosaurus. ");
		string.append("They’re <em>carnivores–pursuit predators–and pretty good ones</em>. They likely preyed on ");
		string.append("smaller animals, and were lightweight (for a dinosaur), fast and agile (for a dinosaur), and had ");
		string.append("two types of teeth. The back teeth were serrated, for tearing through flesh, and the front teeth ");
		string.append("were curved, for digging into skin and holding prey in its jaws.");

		Story hint=new Story("(Requires Doctor) Try to identify the dinosaur", string.toString());
		story.addEdge(encounter, hint, RandInt(false));
		story.addEdge(hint, hide, RandInt(true));
		story.addEdge(hint, die, RandInt(true));
		story.addEdge(hint, scare, RandInt(true));
		
		Story[] endings=new Story[] {win, escape, survive}; //macomb dies in last one
		
		return endings;
	}
	
	private static Story[] MakeSoloPath(Story start) //make it to substation alone but dilo is there
	{
		StringBuilder string=new StringBuilder(600); 
		string.append("You make your way over to Macomb’s body, on the off-chance he’s alive.<br><br>");
		string.append("He isn’t. Even if he was breathing, all that’s left of his face are rags of bloody flesh hanging off ");
		string.append("his skull. His throat and chest have been torn open, and the dirt around his body is stained as red ");
		string.append("as your shirt and face. You’ve never in your life seen so much gore, but the smell is even worse. ");
		string.append("It’s blood mixed with sweat mixed with bile from Macomb’s torn open stomach, and the stench, ");
		string.append("combined with the sight, is enough to make you vomit. You at least manage to direct it away ");
		string.append("from Macomb’s body.<br><br>");
		string.append("You belatedly wipe the blood from your face and realise you need to decide what to do next.");

		Story begin=new Story("Continue", string.toString());
		story.addEdge(start, begin, 1);
		
		//bury body or not
		string.delete(0, string.length());
		string.append("You look at the flies starting to gather around Macomb's body and decide you need to do something, but ");
		string.append("you don’t have a shovel or anything even close to one.<br><br>");
		string.append("After a moment of hesitation, you look around and easily spot several sizable rocks, then get to work ");
		string.append("gathering them and placing them on top of Macomb's body. You don’t even want to try to move it, given ");
		string.append("that it almost looks like it’ll fall apart if you try.<br><br>");
		string.append("It takes a while, but eventually you manage to bury Macomb's body underneath a ");
		string.append("pile of rocks, which should keep scavengers away for now.<br><br>");
		string.append("As awkward as it feels to just leave, you don’t know Macomb enough to even try and give him a ");
		string.append("eulogy, so there’s nothing left to do but move on.");

		Story bury=new Story("Bury Macomb", string.toString());
		story.addEdge(begin, bury, 1);
		
		string.delete(0, string.length());
		string.append("You walk away, the silence weighing on you as a reminder of Macomb’s absence, but thankfully ");
		string.append("don’t encounter any more dinosaurs. Eventually the trees around you start to part, and you reach ");
		string.append("the end of the path.<br><br>");
		string.append("In front of you is the substation, deep in the forest and surrounded by trees. It actually appears to ");
		string.append("be made of metal instead of stone, and looks a lot like an above ground bunker, with a single ");
		string.append("entrance and no windows. The door has a keypad above the handle, but oddly there’s a shoe stuck in the ");
		string.append("doorway, keeping the door from closing. For some reason, there isn’t a single power transformer in sight.");
		string.append("<br><br>You literally take a single step forward before you suddenly hear a low growling sound behind ");
		string.append("you. You slowly turn around and see a dinosaur emerging from the path you just came from.<br><br>");
		string.append("This one is much bigger than the pterosaur, around the size and length of a pickup truck. ");
		string.append("However, it’s a completely different species, earthbound and walking on two feet. It has two ");
		string.append("hard-looking crests on top of its snout, and two shortened arms like a T. rex. Its mouth is full of ");
		string.append("curved, sharp teeth, and its eyes are looking at you. It slowly begins to approach you, still growling.");
		string.append("<br><br>As before, there are sticks and stones in the dirt around you. The substation is behind you, no ");
		string.append("more than a few metres away.");
		
		Story encounter=new Story("Move on", string.toString());
		story.addEdge(begin, encounter, 2);
		story.addEdge(bury, encounter, 1);
		
		//dino fight
		string.delete(0, string.length());
		string.append("You really don’t know what to do, and out of desperation, decide to take a risk.<br><br>");
		string.append("You clear your throat and stand a bit straighter, calling on both your officer training and lawyer ");
		string.append("skills for this. “STAND DOWN!” you order the dinosaur like it’s an unruly subordinate.<br><br>");
		string.append("It only growls softly and stares at you blankly.<br><br>");
		string.append("You place your hands on your hips, and speak as loudly as you can without actually yelling, ");
		string.append("trying to mimic your old drill instructor. “United States Code of Law, Title 18, Section 113. ");
		string.append("Quote. Whoever, within the special maritime and territorial jurisdiction of the United States, is ");
		string.append("guilty of an assault shall be punished as follows: Assault with intent to commit murder…by a ");
		string.append("fine under this title, imprisonment for not more than 20 years, or both. End quote.”<br><br>");
		string.append("The dinosaur is staring back at you, but still hasn’t attacked yet.");

		Story dominate=new Story("(Requires Lawyer) Try to assert your dominance", string.toString());
		story.addEdge(encounter, dominate, RandInt(false));
		
		string.delete(0, string.length());
		string.append("“Do you have any idea how stupid you are? Are you even literate? Twenty years of your life! ");
		string.append("What are you, four? Five? You can’t even begin to understand how much trouble you’re in!” ");
		string.append("When the dinosaur growls at you again, you yell “You SHUT UP when I’m talking to you!”<br><br>");
		string.append("That actually works, and it stops growling, so you ignore the way your heart is hammering ");
		string.append("against your ribcage and keep lecturing it. “Attacking an old man and a child? Are you trying to ");
		string.append("get your sentence maxed?! Do you want to throw your entire life away because of one stupid ");
		string.append("decision? Twenty years! That’s two decades of no education and no income. Your kids will grow ");
		string.append("up without you; you’ll miss their whole childhood. And don’t get me started on how bad prison is.”");
		string.append("<br><br>You quickly take a deep breath and continue, while the dinosaur shifts in place uncertainly. “You ");
		string.append("think you’re so tough? Huh? Threatening people who can’t fight back? Wait until you go to ");
		string.append("prison, then we’ll see how tough you are! You won’t last one day with an attitude like that. If ");
		string.append("you’re anything other than a suicidal halfwit, you better plead guilty and hope to your creator ");
		string.append("you land probation, because you sure won’t make it in prison.”<br><br>");
		string.append("The dinosaur hisses at you, and looks like it’s had enough of your lecturing. It’s apparently ");
		string.append("decided you aren’t worth eating, and turns around, walking off into the forest in search of less ");
		string.append("aggressive prey.");
		
		Story lecture=new Story("Keep it up", string.toString());
		story.addEdge(dominate, lecture, 1);
		
		string.delete(0, string.length());
		string.append("You practically deflate from relief, and can't help but uncontrollably burst into laughter, a ");
		string.append("mixture of relief and disbelief.<br><br>");
		string.append("You just threatened a <a href=\"dilo.html\">dinosaur</a> with a prison sentence for trying to eat you. And ");
		string.append("somehow it worked. You know that if Macomb was here, he would’ve found it just as funny as you do.<br><br>");
		string.append("It takes a few minutes for you to calm down, and when you do, you have to wipe tears from your eyes.");
		string.append("<br><br>Even though you’re a prosecutor, you know a few defence attorneys, and picked up a thing or ");
		string.append("two from them. Unfortunately, they’ll never be able to know just how much they helped you, ");
		string.append("because Hammett made you sign a non-disclosure agreement before he would let you visit. Word ");
		string.append("of a dinosaur attack would be bad publicity anyway.<br><br>");
		string.append("Once you’re done with your retrospection, you remember where you are, and start walking to the substation.");

		Story win=new Story("Continue", string.toString());
		story.addEdge(lecture, win, 1);
		
		string.delete(0, string.length());
		string.append("You decide to play dead, hoping it’ll prevent the dinosaur from seeing you as a threat, and will ");
		string.append("leave you alone.<br><br>");
		string.append("You get on the ground and go limp, hiding your face and trying to slow your fearful breathing as ");
		string.append("the dinosaur approaches.<br><br>");
		string.append("After a few seconds, it seems to have finished assessing you, and takes your quietness and ");
		string.append("stillness as a sign that you’re a suitable meal.<br><br>");
		string.append("The dinosaur wraps its mouth around your body, and you can’t help but let out a scream when ");
		string.append("you feel its many sharp teeth sink into your skin. Its bite tightens, and you feel its teeth start to ");
		string.append("rip your body apart, slicing your flesh with ease.<br><br>");
		string.append("The dinosaur rips out a huge chunk of the flesh from your back, and swallows it whole. You ");
		string.append("scream again, but there’s no one around to hear it.<br><br>");
		string.append("The dinosaur’s next bite tears the skin and meat right off your chest, and you begin ");
		string.append("hemorrhaging blood. The pain is so intense that you can barely think, unable to focus enough to ");
		string.append("even feel regret over the choices that landed you here. The last thing you see is the dinosaur’s mouth ");
		string.append("engulfing your head, before your life ends with one last wet crunch.<br><br>");
		string.append("<strong>You died.</strong>");
		
		Story die=new Story("Try to play dead", string.toString());
		story.addEdge(encounter, die, RandInt(false));
		
		string.delete(0, string.length());
		string.append("You get an idea and take out your phone, which thankfully still has charge in it. Then you turn on ");
		string.append("the flashlight, shining it at the dinosaur, which flinches and hisses at the sudden burst of light.<br><br>");
		string.append("You keep the flashlight on, and raise your hands above your head to appear bigger and thus more threatening.");
		string.append("<br><br>“Go away!” you shout at the dinosaur. “I am not food!”<br><br>");
		string.append("It takes a while, but your strategy eventually produces results, as after enough shouting, the ");
		string.append("dinosaur decides you aren’t worth eating, and turns around, walking off into the forest in search ");
		string.append("of less aggressive prey.<br><br>");
		string.append("Once it leaves, you turn off your flashlight and are suddenly very glad that you paid attention to ");
		string.append("those Grizzley the Bear public service announcements as a kid. He taught you that when faced ");
		string.append("with a wild animal, you should try to make yourself look bigger, and talk to let them know you ");
		string.append("aren’t their usual easy prey. The flashlight wasn’t part of the announcements, but you figured ");
		string.append("something extra wouldn’t hurt given that <a href=\"dilo.html\">dinosaurs</a> aren’t like most wild animals.");
		string.append("<br><br>With the dinosaur now gone, you can finally walk over to the substation’s door.");

		Story scare=new Story("Try to scare it off", string.toString());
		story.addEdge(encounter, scare, RandInt(false));
		
		string.delete(0, string.length());
		string.append("You decide to hide inside the substation, figuring that its metal walls will be the perfect ");
		string.append("protection from the dinosaur.<br><br>");
		string.append("Keeping your eyes on the dinosaur, you slowly back up until your back is to the substation, but ");
		string.append("the dinosaur takes a step forwards with every step you move back, still eyeing you cautiously.<br><br>");
		string.append("Once next to the door, you try to push it further open with your hands, but it barely budges. It’s ");
		string.append("surprisingly heavy. It seems the door is weighted so ");
		string.append("it automatically shuts itself after being opened, apparently to prevent access without the passcode.");
		string.append("<br><br>You have no choice but to turn around and grab the handle with both of your hands, grunting a ");
		string.append("little as you pull the door wide open enough for you to get in.<br><br>");
		string.append("Unfortunately for you, the moment the dinosaur sees your turned back, it considers you viable ");
		string.append("prey and starts charging.");

		Story hide=new Story("Try to hide inside the substation", string.toString());
		story.addEdge(encounter, hide, RandInt(false));
		
		string.delete(0, string.length());
		string.append("You try to get inside the substation, but it’s too late. The dinosaur is far faster, and gets to you ");
		string.append("just as you place your foot in the doorway.<br><br>");
		string.append("The dinosaur wraps its mouth around your body, and you can’t help but let out a scream when ");
		string.append("you feel its many sharp teeth sink into your skin. Its bite tightens, and you feel its teeth start to ");
		string.append("rip your body apart, slicing your flesh with ease.<br><br>");
		string.append("The dinosaur rips out a huge chunk of the flesh from your back, and swallows it whole. You ");
		string.append("scream again, but there’s no one around to hear it.<br><br>");
		string.append("The dinosaur’s next bite tears the skin and meat right off your chest, and you begin ");
		string.append("hemorrhaging blood. The pain is so intense that you can barely think, unable to focus enough to ");
		string.append("even feel regret over the choices that landed you here. The last thing you see is the dinosaur’s mouth ");
		string.append("engulfing your head, before your life ends with one last wet crunch.<br><br>");
		string.append("<strong>You died.</strong>");
		
		Story death=new Story("Move quickly", string.toString());
		story.addEdge(hide, death, 1);
		
		string.delete(0, string.length());
		string.append("You easily identify the dinosaur based on the crests on its head. It’s obviously a dilophosaurus. ");
		string.append("They’re <em>carnivores–pursuit predators–and pretty good ones</em>. They likely preyed on ");
		string.append("smaller animals, and were lightweight (for a dinosaur), fast and agile (for a dinosaur), and had ");
		string.append("two types of teeth. The back teeth were serrated, for tearing through flesh, and the front teeth ");
		string.append("were curved, for digging into skin and holding prey in its jaws.");

		Story hint=new Story("(Requires Doctor) Try to identify the dinosaur", string.toString());
		story.addEdge(encounter, hint, RandInt(false));
		story.addEdge(hint, hide, RandInt(true));
		story.addEdge(hint, die, RandInt(true));
		story.addEdge(hint, scare, RandInt(true));
		
		Story[] endings=new Story[] {win, scare};
		
		return endings;
	}
	
	private static Story MakeMuldrewPath3(Story start1, Story start2) //hotel shelter with muldrew and alex until power returns
	{
		StringBuilder string=new StringBuilder(600); 
		string.append("You, Muldrew, and Alex enter the hotel and head up to the second floor. It gives you a nice view ");
		string.append("of the ocean, and more importantly allows you to keep an eye on the forest and path, both of ");
		string.append("which are currently devoid of dinosaurs, meaning you’re safe.<br><br>");
		string.append("In addition to lacking windows or doors, the unfinished hotel doesn’t even have a single piece of ");
		string.append("furniture in it; after checking several rooms, you give up and sit on the floor. There are no lights ");
		string.append("either, but the afternoon sun provides sufficient illumination.<br><br>");
		string.append("Muldrew is leaning against the open window frame, acting as lookout, and Alex is on the floor ");
		string.append("right next to him, looking like he isn’t sure what to do now.");

		Story begin=new Story("Continue", string.toString());
		story.addEdge(start1, begin, 1);
		story.addEdge(start2, begin, 1);
		
		//make conversation or don't
		string.delete(0, string.length());
		string.append("“So, what's it like being around dinosaurs? I assume they aren’t usually this aggressive?” you ask, ");
		string.append("not wanting to bring up anything sensitive, but badly wanting to know more about the creatures ");
		string.append("you’ve devoted your life to studying.<br><br>");
		string.append("“Actually, they are,” Muldrew replies. He sounds reluctant to talk about this right now, but implicitly ");
		string.append("thinks you deserve it to know given that you’ve helped him survive two dinosaur attacks.<br><br>");
		string.append("“Uncle Roland says no one’s allowed to get close to them except him,” says Alex. “He always ");
		string.append("makes us stay behind the glass.”<br><br>");
		string.append("Muldrew spends the next few hours explaining what dinosaurs are like, comparing them to your ");
		string.append("expectations as a scientist. Paleontology is unsurprisingly very accurate, and dinosaurs act ");
		string.append("largely the way you expected them to. The only problem is that they aren’t very well adapted to ");
		string.append("modern environments and life.<br><br>");
		string.append("Neary studied each of the dinosaurs extensively during every stage of their life, and due to being ");
		string.append("a fast learner and brilliant biologist, was able to figure out how to inoculate them from modern ");
		string.append("germs with custom made vaccines.<br><br>");
		string.append("But their behaviour was another problem entirely. It took Muldrew a while to get them to stop ");
		string.append("seeing him as either food or a threat, and even though they can tolerate his presence now, they ");
		string.append("aren’t as tame around other humans. They’re similar to aggressive wild animals, even moreso ");
		string.append("when hungry, but he’s in the process of getting them to be more cautious around humans, with mixed results.");
		string.append("<br><br>You share with them how you first got into paleontology, and Alex asks you tons of questions ");
		string.append("that you’re happy to answer. Muldrew tells you about his experience with animal handling—both ");
		string.append("dogs and dinosaurs—and his brief time as a safari guide. And before you know it, it’s evening time.");
		
		Story talkDino=new Story("(Requires Doctor) Make conversation", string.toString());
		story.addEdge(begin, talkDino, 1);
		
		string.delete(0, string.length());
		string.append("You decide to tell a joke you heard once in boot camp. “You guys want to hear a joke?”<br><br>");
		string.append("Alex instantly perks up, wanting a laugh, and nods eagerly. Muldrew looks at you sceptically ");
		string.append("but says “Go ahead.”<br><br>");
		string.append("“Three guys are hiking through the woods when they find a lamp. One of them picks it up and ");
		string.append("out comes a genie. It says ‘since you were the first to free me, I’ll grant each of you three ");
		string.append("wishes,’” you start. Alex looks interested, but Muldrew seems to think he’s heard this one before.<br><br>");
		string.append("You continue. “The first guy says ‘I want to be a billionaire.’ The genie snaps its fingers and the ");
		string.append("guy gets a notification from his bank saying his balance is exactly one billion. The second guy ");
		string.append("sees this and considers it, then says ‘I want to be the richest man alive.’ The genie snaps and his ");
		string.append("phone is flooded with notifications saying his investments are now worth trillions. The third guy ");
		string.append("sees this and considers it, thinking even longer, then says ‘I want my left arm to rotate clockwise ");
		string.append("for the rest of my life.’ Another snap and his arm starts rotating.” Muldrew frowns slightly, ");
		string.append("clearly not expecting this direction. Alex looks confused, but you aren’t done yet.");

		Story setup=new Story("(Requires Lawyer) Make conversation", string.toString());
		story.addEdge(begin, setup, 2);
		
		string.delete(0, string.length());
		string.append("“For his second wish, the first guy says ‘I want the love of the most beautiful woman in the ");
		string.append("world.’ The genie snaps, and a jaw-droppingly beautiful woman appears, wrapping her arms ");
		string.append("around him. The second guy sees this and considers it, then says ‘I want to be able to get any ");
		string.append("woman I want.’ The genie snaps, and with a single look at her, the woman abandons the first guy ");
		string.append("to flirt with the second. The third guy sees this and thinks even longer, then says ‘I want my right ");
		string.append("arm to rotate counter-clockwise for the rest of my life.’ Another snap and now both of his arms ");
		string.append("are rotating, in opposite directions,” you say. Muldrew looks intrigued, and Alex is more confused.");
		string.append("<br><br>“The genie tells them to use their last wish wisely. The first guy thinks for a while, then says ‘");
		string.append("I want to be healthy and never get sick or injured again.’ The genie snaps, and his complexion ");
		string.append("instantly clears up, and a few pounds of excess fat disappear. The second guy sees this and thinks ");
		string.append("about it, then says ‘I want to be young and healthy forever.’ With another snap, he becomes ");
		string.append("immortal and in equally perfect health. The third guy thinks the longest, then smiles triumphantly ");
		string.append("and says ‘I want my head to nod back and forth until the day I die.’ The genie snaps, and he’s ");
		string.append("now nodding his head and flailing his arms around,” you tell them. Muldrew is fully invested ");
		string.append("now, and Alex looks like you’ve lost him.");
		
		Story buildup=new Story("Keep going", string.toString());
		story.addEdge(setup, buildup, 1); 
		
		string.delete(0, string.length());
		string.append("“The genie disappears and the guys go their separate ways. Ten years later they happen to meet ");
		string.append("again and talk about how things have been. The first guy says ‘My family and I are set for life ");
		string.append("and don’t need to work anymore, my wife is amazing in bed, and I haven’t even gotten a cold.’ ");
		string.append("The second guy says ‘I’ve used my money to end homelessness and fund a cure for cancer, I ");
		string.append("haven’t aged a day, and yes, she is amazing in bed.’ The third guy is still flailing his arms around ");
		string.append("and nodding his head, and says ‘Guys, I think messed up.’”<br><br>");
		string.append("Muldrew bursts out laughing, but Alex only gives a confused smile, not getting it. “Then why ");
		string.append("did he wish for that?” he asks.<br><br>");
		string.append("“That’s the whole point,” Muldrew says in between wheezes of laughter. “It’s not supposed to ");
		string.append("make sense. That’s why it’s funny.”<br><br>");
		string.append("You smile, pleased that at least Muldrew liked your joke. After he recovers from laughing, you two try ");
		string.append("to explain the joke to Alex, to no avail, as he simply had no expectations to subvert. <br><br>");
		string.append("The three of you spend the next several hours telling more jokes, and you share a few stories, ");
		string.append("both from your time as JAG Officer, and as a private prosecutor. Muldrew tells you about his ");
		string.append("time training dogs in the Army and life in the UK, and Alex listens eagerly to you both, asking ");
		string.append("lots of questions. Before you know it, it’s already evening.");

		Story punchline=new Story("Deliver the punchline", string.toString());
		story.addEdge(buildup, punchline, 1);
		
		string.delete(0, string.length());
		string.append("You decide not to break the silence, and Alex curls up into a ball on the floor, looking mentally and ");
		string.append("physically tired from the walking and action. He closes his eyes and soon falls asleep despite the hard floor.");
		string.append("<br><br>Muldrew alternates between watching over Alex and keeping an eye on the forest. You get bored ");
		string.append("of sitting and doing nothing, and stand up, both enjoying the peaceful view and also keeping an ");
		string.append("eye out in case any dinosaurs show.<br><br>");
		string.append("Thankfully, none do. Several hours pass with no sound other than Alex’s soft breathing, mixed ");
		string.append("with Muldrew’s and your own. He doesn’t break the silence, not wanting to wake up Alex yet, ");
		string.append("and neither do you. You don’t have much to do beyond reflect on the events of the day. Lost in ");
		string.append("thought, you barely even notice when it finally becomes evening.");

		Story sleep=new Story("Stay silent", string.toString());
		story.addEdge(begin, sleep, 3);
		
		//final dino encounter
		string.delete(0, string.length());
		string.append("Suddenly, your phone vibrates, and you pull it out to see half a dozen missed notifications ");
		string.append("popping up all at once. It seems you suddenly have signal again, which must mean the power is back.");
		string.append("<br><br>Muldrew notices this and checks his own phone, smiling a little and saying “He did it.” Then he ");
		string.append("addresses you and Alex “Let’s get to the substation. Hammett’s going to be here soon.”<br><br>");
		string.append("Alex gets up, looking as relieved as you feel. The three of you make your way downstairs and ");
		string.append("leave the hotel. Muldrew remains vigilant and tense, while Alex looks nervous but also relieved ");
		string.append("that this is about to be over.");
		
		Story leave=new Story("Continue", string.toString());
		story.addEdge(talkDino, leave, 1);
		story.addEdge(punchline, leave, 1);
		story.addEdge(sleep, leave, 1);
		
		string.delete(0, string.length());
		string.append("The silent walk back to the park’s entrance is tense but hopeful, but that silence is suddenly ");
		string.append("broken when Muldrew takes out his phone, saying “I just remembered. We can track the dinosaurs now.” ");
		string.append("Using the tracking chips was part of Macomb's rationale for wanting to restore the power, and in ");
		string.append("hindsight, his thinking was pretty smart.<br><br>");
		string.append("You all slow down and watch as he opens up an app whose icon is the Jurassic Preserve logo. It ");
		string.append("shows a map of the island on his phone, with a bunch of coloured dots on it. There are also a few ");
		string.append("buttons, apparently for viewing the dinosaur enclosures and remote feeding.<br><br>");
		string.append("Muldrew sees you looking and murmurs “Orange means dinosaur, green means human. It ");
		string.append("tracks us too.” Around a dozen are orange, but four of them are green, your presence not being ");
		string.append("counted since you don’t have Macomb's app.<br><br>");
		string.append("It appears that everyone is alive, and Neary and Macomb are at the control tower together. ");
		string.append("However, one of the orange dots is slowly approaching them. And even worse, a faster moving ");
		string.append("orange dot is approaching the three of you.<br><br>");
		string.append("You all look up at the same time, hearing the footsteps behind you, too heavy to be from any ");
		string.append("human. A huge dinosaur walks onto the path a few metres behind you.<br><br>");
		string.append("“Allen?” Alex says, sounding afraid. Muldrew curses in frustration. “Why’d it have to be the allosaurus?”");
		string.append("<br><br>This dinosaur is even larger than the last one, around the size of a school bus. This one doesn’t ");
		string.append("have any bone crests, but it has the same T. rex-like arms and bipedalism. Its teeth are serrated ");
		string.append("rather than curved. It’s clearly a different species. And unlike the previous dinosaur, this one ");
		string.append("barely even hesitates before charging at you, clearly wanting you as a meal.");

		Story encounter=new Story("Continue", string.toString());
		story.addEdge(leave, encounter, 1);
		
		string.delete(0, string.length());
		string.append("Muldrew unleashes a rapid string of curses, knowing better than probably any human alive how ");
		string.append("dangerous allosauruses can be. He instinctively picks Alex up, putting him into a cradle carry, ");
		string.append("and starts sprinting.<br><br>");
		string.append("You start running as fast as you can without looking back, and the dinosaur follows, easily ");
		string.append("keeping pace despite its large size. The forest’s previous quiet tranquility has been shattered, ");
		string.append("replaced by the dinosaur’s thundering stomps and your panicked breathing, punctuated by its hungry growls.");
		string.append("<br><br>Your heart is pounding so hard it’s almost audible, and you don’t even need to look back to tell ");
		string.append("that the dinosaur is dangerously close to you. It’s not within biting distance yet, but at this rate, ");
		string.append("you won’t be outrunning it either. You know you need to do something to gain the advantage, or ");
		string.append("the dinosaur will end up outlasting you.");
		
		Story run=new Story("Run", string.toString());
		story.addEdge(encounter, run, 1); 
		
		return run;
	}
	
	private static Story[] MakeAlexPath2(Story start1, Story start2, Story exit) //hotel shelter with alex until the power returns
	{
		//no dialogue lmao
		StringBuilder string=new StringBuilder(600); 
		string.append("You and Alex enter the hotel and head up to the second floor. It gives you a nice view of the ");
		string.append("ocean, and more importantly allows you to keep an eye on the forest and path, both of which are ");
		string.append("currently devoid of dinosaurs, meaning you’re safe.<br><br>");
		string.append("In addition to lacking windows or doors, the unfinished hotel doesn’t even have a single piece of ");
		string.append("furniture in it; after checking several rooms, you give up and go to the nearest one. There are no ");
		string.append("lights either, but the afternoon sun provides sufficient illumination.<br><br>");
		string.append("Alex curls up into a ball on the floor, looking mentally and ");
		string.append("physically tired from the walking and action. He closes his eyes and soon falls asleep despite the hard floor.");
		string.append("<br><br>You lean against the open window frame, both enjoying the peaceful view and also keeping an ");
		string.append("eye out in case any dinosaurs show.<br><br>");
		string.append("Thankfully, none do. Several hours pass with no sound other than Alex’s soft breathing, mixed ");
		string.append("with your own. You don’t have much to do beyond reflect on the events of the day. Lost in ");
		string.append("thought, you barely even notice when it finally becomes evening.");

		Story begin=new Story ("Continue", string.toString());
		story.addEdge(start1, begin, 1);
		story.addEdge(start2, begin, 1);
		
		string.delete(0, string.length());
		string.append("Suddenly, your phone vibrates, and you pull it out to see half a dozen missed notifications ");
		string.append("popping up all at once. It seems you suddenly have signal again, which must mean the power is back.");
		string.append("<br><br>You wake Alex up, telling him “Macomb did it. The power’s back.”<br><br>");
		string.append("He wakes up quickly, and processes your words just as fast, a mixture of feelings flashing across ");
		string.append("his face: relief, excitement, and lingering grief. He settles on a determined look and tells you ");
		string.append("firmly “We have to meet him at the control tower.”<br><br>");
		string.append("You are looking forward to leaving as much as he is, and nod, saying “Of course. Let’s go.”<br><br>");
		string.append("The two of you make your way downstairs and leave the hotel. Alex looks nervous but also ");
		string.append("relieved that this is about to be over.");
		
		Story leave=new Story("Continue", string.toString());
		story.addEdge(begin, leave, 1);
		
		//dino encounter
		string.delete(0, string.length());
		string.append("The silent walk back to the park’s entrance is tense but hopeful, but those hopes are quickly ");
		string.append("dashed when you hear footsteps behind you, too heavy to be from any human. You and Alex turn ");
		string.append("around to see what it is, and find a huge dinosaur walking onto the path a few metres behind you.<br><br>");
		string.append("“Allen?” Alex says, sounding afraid.<br><br>");
		string.append("It’s even larger than the last one, around the size of a school bus. This one doesn’t have any bone ");
		string.append("crests, but it has the same T. rex-like arms and bipedalism. Its teeth are serrated rather than ");
		string.append("curved. It’s clearly a different species. And unlike the previous dinosaur, this one barely even ");
		string.append("hesitates before charging at you, clearly wanting you as a meal.");

		Story encounter=new Story("Continue", string.toString());
		story.addEdge(exit, encounter, 1);
		story.addEdge(leave, encounter, 1);
		
		string.delete(0, string.length());
		string.append("Barely even thinking, you quickly grab Alex and hold him in a cradle carry, knowing there’s no ");
		string.append("way he’d be able to keep up if he ran on his own. Alex barely reacts, too busy staring at the dinosaur's ");
		string.append("mouth, which could probably fit his whole body inside of it, and teeth, which are almost as long as his fingers.");
		string.append("<br><br>You start running as fast as you can without looking back, and the dinosaur follows, easily ");
		string.append("keeping pace despite its large size. The forest’s previous quiet tranquility has been shattered, ");
		string.append("replaced by the dinosaur’s thundering stomps and your panicked breathing, punctuated by its hungry growls.");
		string.append("<br><br>Your heart is pounding so hard it’s almost audible, and you don’t even need to look back to tell ");
		string.append("that the dinosaur is dangerously close to you. It’s not within biting distance yet, but at this rate, ");
		string.append("you won’t be outrunning it either. You know you need to do something to gain the advantage, or ");
		string.append("the dinosaur will end up outlasting you.");
		
		Story save=new Story("Run with Alex", string.toString());
		story.addEdge(encounter, save, 1);
		
		string.delete(0, string.length());
		string.append("Barely even thinking, you quickly shove Alex towards the dinosaur. He doesn’t even see it ");
		string.append("coming until it’s too late, implicitly trusting you and being blindsided by your sudden betrayal. ");
		string.append("Alex falls into the dirt, and the dinosaur stops in its tracks to look at him.<br><br>");
		string.append("Tears well up in Alex’s eyes, and he turns to stare at you, looking shocked, betrayed, and ");
		string.append("heartbroken, like a kicked puppy. He doesn’t even have time to say anything before the dinosaur ");
		string.append("wraps its jaws around the entire top half of Alex’s body and slams its mouth shut, instantly ");
		string.append("killing him with a bloody spray.<br><br>");
		string.append("You immediately turn around and start running without looking back, hoping the dinosaur will be ");
		string.append("slowed down by Alex. You try to put as much distance between you and it as possible, running ");
		string.append("down the past. You can hear it greedily tearing apart Alex’s body, but the dinosaur is large and ");
		string.append("Alex is small. It finishes eating within a minute, not enough time for you to completely escape.<br><br>");
		string.append("Though you have a large head start, there’s only one direction for you to run. The dinosaur is ");
		string.append("smart enough to notice this, and hungry enough to keep pursuing you despite your advantage in ");
		string.append("You have a safe amount of space between you and it, but you’re not quite able to escape ");
		string.append("it just yet. You know you need to do something to gain the advantage so you can lose it.");
		
		Story kill=new Story("Leave Alex behind", string.toString());
		story.addEdge(encounter, kill, 2);
		
		Story[] endings=new Story[] {save, kill};
		
		return endings;
	}
	
	private static Story MakeNewAlexPath(Story start) //address muldrew death then merge with regular alex path
	{		
		//false choice because death needs to be addressed, but not much to be done or said without body
		StringBuilder prime=new StringBuilder(600);
		prime.append("<br><br>Alex doesn’t say anything to you, just stares at the ground blankly. You realise you’re both still ");
		prime.append("sitting out in the open, and decide you should at least get into the Center, in case another ");
		prime.append("dinosaur shows up.<br><br>");
		prime.append("“We should get to shelter,” you tell him, standing up.<br><br>");
		prime.append("Alex gets up too, but doesn’t say a word as he follows you inside the Center. It’s the exact same ");
		prime.append("as you left it, still dark with all the lights off. Alex sits on the floor and rests his back against the ");
		prime.append("wall, resting his head on his knees. He clearly doesn’t want to talk right now.<br><br>");
		prime.append("You decide to sit close to one of the windows, so you can keep an eye out for any dinosaurs. It ");
		prime.append("doesn’t hurt that the view isn’t bad, almost peaceful looking. Before long, you hear Alex softly ");
		prime.append("snoring, probably tired from all the walking and uncertainty. You don’t have much to do beyond ");
		prime.append("reflect on the events of the day. Lost in thought, you barely even notice when it finally becomes evening.");
		
		StringBuilder string=new StringBuilder(600); 
		string.append("“What Muldrew did…he was really brave,” you tell him gently. “I’m sure your grandfather will ");
		string.append("be proud of him...”");

		Story compliment=new Story("Commend Muldrew", string.toString()+prime.toString());
		story.addEdge(start, compliment, 2);
		
		string.delete(0, string.length());
		string.append("“I’m sorry, Alex,” you tell him gently. “I never meant for that to happen.”");

		Story apologise=new Story("Express regret", string.toString()+prime.toString());
		story.addEdge(start, apologise, 1);
		
		string.delete(0, string.length());
		string.append("You decide it’s better not to say anything about Muldrew’s death, especially given how Alex ");
		string.append("looks right now.");

		Story silence=new Story("Give Alex space", string.toString()+prime.toString());
		story.addEdge(start, silence, 3);
		
		string.delete(0, string.length());
		string.append("Suddenly, the Center’s lights all turn back on. Its air conditioning is back too, but by now you’re ");
		string.append("almost used to the heat.");
		string.append("<br><br>You wake Alex up, telling him “Macomb did it. The power’s back.”<br><br>");
		string.append("He wakes up quickly, and processes your words just as fast, a mixture of feelings flashing across ");
		string.append("his face: relief, excitement, and lingering grief. He settles on a determined look and tells you ");
		string.append("firmly “We have to meet him at the control tower.”<br><br>");
		string.append("You are looking forward to leaving as much as he is, and nod, saying “Of course. Let’s go.”<br><br>");
		string.append("The two of you leave the Visitor's Center and head to the path whose sign reads “Exit”. ");
		string.append("You aren't looking forward to showing up without Muldrew, but at least Alex is still alive. ");
		string.append("For his part, Alex looks nervous but also relieved that this is about to be over.<br><br>");
		string.append("The two of you start walking down the path, heading back to where it all began.");
		
		Story leave=new Story("Continue", string.toString());
		story.addEdge(apologise, leave, 1);
		story.addEdge(compliment, leave, 1);
		story.addEdge(silence, leave, 1);
		
		return leave;
	}
	
	private static Story MakeDeathPath(Story start) //center shelter alone until the power returns
	{
		//no dialogue because no one to talk to
		StringBuilder string=new StringBuilder(600);
		string.append("Once you’ve recovered, you realise you’re sitting out in the open, and decide you should at least ");
		string.append("shelter inside the Center, in case another dinosaur shows up. It’s not like there’s much else you ");
		string.append("can do until Hammett arrives, since the power is still out and you have no way of knowing whether ");
		string.append("Macomb is even alive or not.<br><br>");
		string.append("When you go inside, it’s the exact same as you left it, still dark with all the lights off. You decide ");
		string.append("to sit close to one of the windows, so you can keep an eye out for any dinosaurs. It doesn’t hurt ");
		string.append("that the view isn’t bad, almost peaceful looking, but you don’t have much to do beyond reflect on ");
		string.append("the events of the day. Lost in thought, you barely even notice when it finally becomes evening.");
		
		Story begin=new Story("Continue", string.toString());
		story.addEdge(start, begin, 1);
		
		string.delete(0, string.length());
		string.append("Suddenly, the Center’s lights all turn back on. Its air conditioning is back too, but by now you’re ");
		string.append("almost used to the heat. Macomb clearly succeeded in his task, and you remember that he said to ");
		string.append("meet him by the control tower, that camouflaged building you saw him come out of when you first met.");
		string.append("<br><br>You aren’t looking forward to showing up alone, especially given what you did to Alex, but ");
		string.append("Hammett is going to be there with his jet, so it’s your only way off the island. You leave the ");
		string.append("Center and look at the paths’ signs, eventually finding the one that reads “Exit”.<br><br>");
		string.append("You start walking down the path, heading back to where it all began.");

		Story leave=new Story("Continue", string.toString());
		story.addEdge(begin, leave, 1);
		
		//dino encounter
		string.delete(0, string.length());
		string.append("The silent walk back to the park’s entrance is tense but hopeful, but those hopes are quickly ");
		string.append("dashed when you hear footsteps behind you, too heavy to be from any human. You turn ");
		string.append("around to see what it is, and find a huge dinosaur walking onto the path a few metres behind you.<br><br>");
		string.append("It’s even larger than the last one, around the size of a school bus. This one doesn’t have any bone ");
		string.append("crests, but it has the same T. rex-like arms and bipedalism. Its teeth are serrated rather than ");
		string.append("curved. It’s clearly a different species. And unlike the previous dinosaur, this one barely even ");
		string.append("hesitates before charging at you, clearly wanting you as a meal.");

		Story encounter=new Story("Continue", string.toString());
		story.addEdge(leave, encounter, 1);
		
		string.delete(0, string.length());
		string.append("You start running as fast as you can without looking back, and the dinosaur follows, easily ");
		string.append("keeping pace despite its large size. The forest’s previous quiet tranquility has been shattered, ");
		string.append("replaced by the dinosaur’s thundering stomps and your panicked breathing, punctuated by its hungry growls.");
		string.append("<br><br>Your heart is pounding so hard it’s almost audible, and you don’t even need to look back to tell ");
		string.append("that the dinosaur is dangerously close to you. It’s not within biting distance yet, but at this rate, ");
		string.append("you won’t be outrunning it either. You know you need to do something to gain the advantage, or ");
		string.append("the dinosaur will end up outlasting you.");
		
		Story run=new Story("Run", string.toString());
		story.addEdge(encounter, run, 1);
		
		return run;
	}
	
	private static Story MakeMacombPath3(Story start1, Story start2) //restore power with macomb
	{
		StringBuilder string=new StringBuilder(600); 
		string.append("Once inside, you and Macomb take out your phones and activate the flashlights in ");
		string.append("order to see the inside of the substation, as the blackout has disabled its interior's lights.<br><br>");
		string.append("A few steps in front of you is an auxiliary cord, stretched dangerously high across the floor, leading to ");
		string.append("an amplifier placed next to a desk. You instantly notice the coffee spilled on the floor below the ");
		string.append("desk, with the shattered pieces of a mug near it. On the desk is a large computer, and there's a ");
		string.append("control box to the desk’s right, with a circuit breaker to its left. A dented electric guitar is lying ");
		string.append("on the ground, and there’s an equal sized dent in the control box, evidently the cause of the blackout.");
		string.append("<br><br>A closer look at the room reveals a large generator opposite the desk, taking up half of the room, ");
		string.append("and currently silent. There’s a wardrobe on the far side of the room, opposite the entrance, and ");
		string.append("there are a few posters of musicians you don’t recognise on the walls. The desk has a few framed ");
		string.append("pictures on it, of Macomb and the others. This is clearly a weird combination of Macomb’s ");
		string.append("workplace and living quarters, though the shoe is presumably Neary's.<br><br>");
		string.append("Suddenly, Macomb curses angrily, startling you. He slams his fist onto the wall, shouting “This ");
		string.append("was my fault! Neary and Muldrew told me it was a tripping hazard, and I didn’t listen!”<br><br>");
		string.append("However, you can see another possible explanation for this scene.");
		
		Story begin=new Story("Continue", string.toString());
		story.addEdge(start1, begin, 1);
		story.addEdge(start2, begin, 1);
		
		//can mention sabotage possibility or not
		StringBuilder prime=new StringBuilder(600);
		prime.append("<br><br>Macomb goes silent as he walks over to the wardrobe and retrieves the toolbox on top of it. Then ");
		prime.append("he heads over to the control box, quietly examining it for a moment. “I can definitely fix this. ");
		prime.append("The impact area’s pretty small, and I always keep spare parts in my toolbox,” he murmurs. He ");
		prime.append("stares longingly at his dented guitar for a moment. “Sarah’s going to be harder to fix, but I guess ");
		prime.append("that’s for later.” Macomb starts assessing the damage, taking out the tools and parts he thinks ");
		prime.append("he’s going to need.");
		
		string.delete(0, string.length());
		string.append("“It might not be,” you posit. “It might be sabotage.”<br><br>");
		string.append("Macomb looks at you incredulously, almost offended. “That’s not even possible.”<br><br>");
		string.append("You press on, bringing up things you see as questionable. “Well, Neary already knew the cord ");
		string.append("was there, so how could he suddenly have tripped over it? And he just disappeared after the ");
		string.append("blackout. He knew we were at the Visitor’s Center, so why didn’t he come to warn us? With how ");
		string.append("valuable his knowledge is, it would make sense for someone like Marzani to pay him to switch sides while ");
		string.append("we’re distracted by dinosaurs,” you say, referring to the Marzani Global Corporation, the closest thing ");
		string.append("NextGen has to a competitor. You've heard rumours of them trying to make their own dinosaurs, without success.");
		string.append("<br><br>Macomb defends Neary vehemently, looking outraged at your suggestion. “Absolutely not. ");
		string.append("Neary has known Muldrew and me for years. Even if we argue sometimes, he would never put either of us ");
		string.append("in danger like that.”<br><br>");
		string.append("He doesn’t even let you get your next words out, sharply cutting you off. “No offence, but you ");
		string.append("don’t know the first thing about us, or you wouldn’t have even considered that. So just drop it, ");
		string.append("okay?” He clearly doesn’t want to hear another word about this.");

		Story sabotage=new Story("Bring up the possibility of sabotage", string.toString()+prime.toString());
		story.addEdge(begin, sabotage, 1);
		
		string.delete(0, string.length());
		string.append("You decide it’s easier to avoid the topic altogether, and tell Macomb “Focus; that doesn’t matter ");
		string.append("right now. We’re here to restore the power, remember?”<br><br>");
		string.append("Macomb looks at you and sighs. The guilt fades from his eyes but doesn’t completely disappear ");
		string.append("as he nods his head. “Right.”");

		Story dont=new Story("Tell Macomb to focus", string.toString()+prime.toString());
		story.addEdge(begin, dont, 2);
		
		//can talk or not during task
		string.delete(0, string.length());
		string.append("“So, what’s it like being around dinosaurs? I assume they aren’t usually this aggressive?” you ");
		string.append("ask, not wanting to bring up anything sensitive, but badly wanting to know more about the ");
		string.append("creatures you’ve devoted your life to studying.<br><br>");
		string.append("“Actually, they are,” Macomb snorts. He doesn’t find the topic sensitive at all, despite just ");
		string.append("having two of them attack him. “Muldrew doesn’t even let anyone get near them. We just have to ");
		string.append("stay behind the glass and wait in suspense when he goes in their enclosures,” he deadpans.");
		string.append("<br><br>Macomb spends the next few hours explaining what dinosaurs are like while he works, doing his ");
		string.append("best to compare them to your expectations as a scientist. Even though he didn’t spend much time ");
		string.append("with them, from what he does tell you, paleontology is unsurprisingly very accurate, with ");
		string.append("dinosaurs largely acting the way you expected them to. The only problem is that they aren’t very ");
		string.append("well adapted to modern environments and life.<br><br>");
		string.append("Neary studied each of the dinosaurs extensively during every stage of their life, and due to being ");
		string.append("a quick learner and brilliant biologist, was able to figure out how to inoculate them from modern ");
		string.append("germs with custom made vaccines.<br><br>");
		string.append("But their behaviour was another problem entirely. It took Muldrew a while to get them to stop ");
		string.append("seeing him as either food or a threat, and even though they can tolerate his presence now, they ");
		string.append("aren’t as tame around other humans. They’re similar to aggressive wild animals, even moreso ");
		string.append("when hungry, but he’s in the process of getting them to be more cautious around humans, with mixed results.");
		string.append("<br><br>You share with him how you first got into paleontology and tell him the names of the dinosaurs ");
		string.append("he describes to you, as well as teach him what you can about their behaviour. Macomb tells you ");
		string.append("more about his life, having relatively humble beginnings. He came from a poor family, but he ");
		string.append("was naturally gifted and managed to score a full-ride scholarship. His innovative architecture ");
		string.append("designs, mixed with his talent for numbers and engineering, earned him Hammett’s attention, and ");
		string.append("he joined NextGen shortly after Neary did, enjoying the challenging work and high pay in equal ");
		string.append("amounts. The hours fly by and before you know it, Macomb has fully repaired the control box.");
		
		Story talk=new Story("(Requires Doctor) Make conversation", string.toString());
		story.addEdge(dont, talk, 1);
		
		string.delete(0, string.length());
		string.append("You decide to tell a joke you heard once in boot camp. “Hey, you want to hear a joke?”<br><br>");
		string.append("Macomb shrugs and says “Go ahead. I could use a good laugh.”<br><br>");
		string.append("“Three guys are hiking through the woods when they find a lamp. One of them picks it up and ");
		string.append("out comes a genie. It says ‘since you were the first to free me, I’ll grant each of you three ");
		string.append("wishes,’” you start. Macomb doesn’t react, but is clearly paying attention.<br><br>");
		string.append("You continue. “The first guy says ‘I want to be a billionaire.’ The genie snaps its fingers and the ");
		string.append("guy gets a notification from his bank saying his balance is exactly one billion. The second guy ");
		string.append("sees this and considers it, then says ‘I want to be the richest man alive.’ The genie snaps and his ");
		string.append("phone is flooded with notifications saying his investments are now worth trillions. The third guy ");
		string.append("sees this and considers it, thinking even longer, then says ‘I want my left arm to rotate clockwise ");
		string.append("for the rest of my life.’ Another snap and his arm starts rotating.” Macomb looks more curious at ");
		string.append("the unexpected direction.");

		Story setup=new Story("(Requires Lawyer) Make conversation", string.toString());
		story.addEdge(dont, setup, 2);
		
		string.delete(0, string.length());
		string.append("“For his second wish, the first guy says ‘I want the love of the most beautiful woman in the ");
		string.append("world.’ The genie snaps, and a jaw-droppingly beautiful woman appears, wrapping her arms ");
		string.append("around him. The second guy sees this and considers it, then says ‘I want to be able to get any ");
		string.append("woman I want.’ The genie snaps, and with a single look at her, the woman abandons the first guy ");
		string.append("to flirt with the second. The third guy sees this and thinks even longer, then says ‘I want my right ");
		string.append("arm to rotate counter-clockwise for the rest of my life.’ Another snap and now both of his arms ");
		string.append("are rotating, in opposite directions,” you say. Macomb stops working to listen to this, fully invested now.");
		string.append("<br><br>“The genie tells them to use their last wish wisely. The first guy thinks for a while, then says ‘");
		string.append("I want to be healthy and never get sick or injured again.’ The genie snaps, and his complexion ");
		string.append("instantly clears up, and a few pounds of excess fat disappear. The second guy sees this and thinks ");
		string.append("about it, then says ‘I want to be young and healthy forever.’ With another snap, he becomes ");
		string.append("immortal and in equally perfect health. The third guy thinks the longest, then smiles triumphantly ");
		string.append("and says ‘I want my head to nod back and forth until the day I die.’ The genie snaps, and he’s ");
		string.append("now nodding his head and flailing his arms around,” you say. Macomb is clearly trying to figure ");
		string.append("out what the punchline is.");

		Story buildup=new Story("Keep going", string.toString());
		story.addEdge(setup, buildup, 1);
		
		string.delete(0, string.length());
		string.append("“The genie disappears and the guys go their separate ways. Ten years later they happen to meet ");
		string.append("again and talk about how things have been. The first guy says ‘My family and I are set for life ");
		string.append("and don’t need to work anymore, my wife is amazing in bed, and I haven’t even gotten a cold.’ ");
		string.append("The second guy says ‘I’ve used my money to end homelessness and fund a cure for cancer, I ");
		string.append("haven’t aged a day, and yes, she is amazing in bed.’ The third guy is still flailing his arms around ");
		string.append("and nodding his head, and says ‘Guys, I think messed up.’”<br><br>");
		string.append("Macomb bursts out laughing, and you smile, pleased that he liked your joke. “Oh wow, that’s a ");
		string.append("good one. I’m totally going to steal that from you,” he grins.<br><br>");
		string.append("Once Macomb finishes laughing, he returns to working on the box. The two of you swap several ");
		string.append("more jokes, and you share a few stories, both from your time as JAG Officer, and as a private ");
		string.append("prosecutor. Macomb tells you more about his life, having relatively humble beginnings. He came ");
		string.append("from a poor family, but he was naturally gifted and managed to score a full-ride scholarship. His ");
		string.append("innovative architecture designs, mixed with his talent for numbers and engineering, earned him ");
		string.append("Hammett’s attention, and he joined NextGen shortly after Neary did, enjoying the challenging ");
		string.append("work and high pay in equal amounts. The hours fly by and before you know it, Macomb has fully ");
		string.append("repaired the control box.");
		
		Story punchline=new Story("Deliver the punchline", string.toString());
		story.addEdge(buildup, punchline, 1);
		
		string.delete(0, string.length());
		string.append("You don't say anything more, and neither does Macomb, who is well accustomed to working in silence.<br><br>");
		string.append("It doesn't take long before you get bored of standing in the dark, watching him fix machine parts, so ");
		string.append("you prop open the substation's door with the shoe, finding it too heavy to hold open yourself. You both ");
		string.append("enjoy the peaceful view of the forest, and keep an eye out just in case any more dinosaurs show.");
		string.append("<br><br>Thankfully, none do. Several hours pass with no sound beyond the clinks and clangs of Macomb's ");
		string.append("tools against the control box. You don’t have much to do beyond reflect on the events of the day. ");
		string.append("Lost in thought, you barely even notice when it finally becomes evening.");

		Story silent=new Story("Stay silent", string.toString());
		story.addEdge(sabotage, silent, 1); //punishment for annoying him lmao; also ensures choice is more meaningful
		story.addEdge(dont, silent, 3);
		
		//last dino encounter
		string.delete(0, string.length());
		string.append("“And…done,” Macomb says, and stands up. While he puts away his toolbox, you can see that ");
		string.append("the control box looks as good as new now.<br><br>“The power isn’t back,” you note.<br><br>");
		string.append("“Don’t worry. Now that the regulators are fixed, all I need to do now is restart the generator,” ");
		string.append("Macomb says, going over to the large machine. After a minute of pressing buttons and turning ");
		string.append("knobs, the generator rattles to life, humming loudly but steadily. A moment later, the substation’s ");
		string.append("lights turn on.<br><br>");
		string.append("Macomb cheers and grins proudly. “Look at that! Power’s back.”<br><br>");
		string.append("“Nice work,” you tell him, equally glad, and finally turn off the flashlight on your phone, which ");
		string.append("is about to run out of battery.<br><br>");
		string.append("When Macomb sees you doing that, he remembers to check his own phone. “Oops. This wasn’t ");
		string.append("part of the plan.” It seems his phone has also run out of battery.");

		Story fix=new Story("Continue", string.toString());
		story.addEdge(talk, fix, 1);
		story.addEdge(punchline, fix, 1);
		story.addEdge(silent, fix, 1);
		
		string.delete(0, string.length());
		string.append("“Well, looks we can’t track the dinosaurs now,” Macomb says, looking sheepish at his lack of foresight.");
		string.append("<br><br>“We made it this far without relying on tracking chips,” you note. “I’m sure we can get out of ");
		string.append("here without them.”<br><br>");
		string.append("“Yeah, good point,” he says, then walks to the door, pushing it open with a grunt. When you look ");
		string.append("outside, it’s already evening time, but the forest is at least clear of dinosaurs.<br><br>");
		string.append("Macomb looks at you intently. “We’re going to the control tower now. I need to get the runway ");
		string.append("ready for Hammett’s jet.”<br><br>");
		string.append("You nod, and add “Muldrew and Alex should be there too, right?”<br><br>");
		string.append("Macomb sighs. “I hope so. Let’s go.” He looks anxious to get to safety and put this all behind him.<br><br>");
		string.append("He leaves the substation, and you follow, letting the door swing shut behind you.");

		Story leave=new Story("Continue", string.toString());
		story.addEdge(fix, leave, 1);
		
		string.delete(0, string.length());
		string.append("The silent walk back to the park’s entrance is tense but hopeful, but those hopes are quickly ");
		string.append("dashed when you hear footsteps behind you, too heavy to be from any human. You and Macomb turn ");
		string.append("around to see what it is, and find a huge dinosaur walking onto the path a few metres behind you.");
		string.append("<br><br>“Yeah, that's not good,” Macomb deadpans.<br><br>");
		string.append("It’s even larger than the last one, around the size of a school bus. This one doesn’t have any bone ");
		string.append("crests, but it has the same T. rex-like arms and bipedalism. Its teeth are serrated rather than ");
		string.append("curved. It’s clearly a different species. And unlike the previous dinosaur, this one barely even ");
		string.append("hesitates before charging at you, clearly wanting you as a meal.");

		Story encounter=new Story("Continue", string.toString());
		story.addEdge(leave, encounter, 1);
		
		string.delete(0, string.length());
		string.append("Macomb curses when the dinosaur charges, and immediately starts running, looking like he’s ");
		string.append("panicking and hoping you’ll find some way to get them out of this.<br><br>");
		string.append("Unfortunately, there’s not much you can do against the two thousand pound dinosaur chasing ");
		string.append("you, other than run for your life.<br><br>");
		string.append("You start running as fast as you can without looking back, and the dinosaur follows, easily ");
		string.append("keeping pace despite its large size. The forest’s previous quiet tranquility has been shattered, ");
		string.append("replaced by the dinosaur’s thundering stomps and your panicked breathing, punctuated by its hungry growls.");
		string.append("<br><br>Your heart is pounding so hard it’s almost audible, and you don’t even need to look back to tell ");
		string.append("that the dinosaur is dangerously close to you. It’s not within biting distance yet, but at this rate, ");
		string.append("you won’t be outrunning it either. You know you need to do something to gain the advantage, or ");
		string.append("the dinosaur will end up outlasting you.");

		Story run=new Story("Run", string.toString());
		story.addEdge(encounter, run, 1);
		
		return run;
	}
	
	private static Story MakeNewSoloPath(Story start) //bury macomb or not, then merge with regular solo path
	{
		StringBuilder string=new StringBuilder(600); 
		string.append("You put the shoe back in the doorway and make your way over to Macomb’s body, on the off-chance he’s still ");
		string.append("alive.<br><br>");
		string.append("He isn’t. Most of what’s left of his body is little more than rags of bloody flesh hanging off his ");
		string.append("bones. His ribs have been broken, and his internal organs are either lying on the ground or in the ");
		string.append("dinosaur’s stomach. The dirt around his body is soaked with blood, and as bad as the gore is, the ");
		string.append("smell is even worse.<br><br>");
		string.append("It’s blood mixed with sweat mixed with bile from Macomb’s torn open stomach, and the stench, ");
		string.append("combined with the sight, is enough to make you vomit. You at least manage to direct it away ");
		string.append("from his body.");

		Story begin=new Story("Continue", string.toString());
		story.addEdge(start, begin, 1);
		
		string.delete(0, string.length());
		string.append("You look at the flies starting to gather around Macomb's body and decide you need to do something, but ");
		string.append("you don’t have a shovel or anything even close to one.<br><br>");
		string.append("After a moment of hesitation, you look around and easily spot several sizable rocks, then get to work ");
		string.append("gathering them and placing them on top of Macomb's body. You don’t even want to try to move it, given ");
		string.append("that it almost looks like it’ll fall apart if you try.<br><br>");
		string.append("It takes a while, but eventually you manage to bury Macomb's body underneath a ");
		string.append("pile of rocks, which should keep scavengers away for now.<br><br>");
		string.append("As awkward as it feels to just leave, you don’t know Macomb enough to even try and give him a ");
		string.append("eulogy, so there’s nothing left to do but head back inside.");
		
		Story bury=new Story("Bury his body", string.toString());
		story.addEdge(begin, bury, 1);
		
		string.delete(0, string.length());
		string.append("Once inside, you take out your phone and activate the flashlight in order to see the inside of the ");
		string.append("substation, as the blackout has disabled its interior’s lights.<br><br>");
		string.append("A few steps in front of you is an auxiliary cord, stretched dangerously high across the floor, ");
		string.append("leading to an amplifier placed next to a desk. You instantly notice the coffee spilled on the floor ");
		string.append("below the desk, with the shattered pieces of a mug near it. On the desk is a large computer, and ");
		string.append("there’s a control box to the desk’s right, with a circuit breaker to its left. A dented electric guitar ");
		string.append("is lying on the ground, and there’s an equal sized dent in the control box, evidently the cause of ");
		string.append("the blackout.<br><br>");
		string.append("A closer look at the room reveals a large generator opposite the desk, taking up half of the room, ");
		string.append("and currently silent. There’s a wardrobe on the far side of the room, opposite the entrance, and ");
		string.append("there are a few posters of musicians you don’t recognise on the walls. The desk has a few framed ");
		string.append("pictures on it, of Macomb and the others. This was clearly a weird combination of Macomb’s ");
		string.append("workplace and living quarters.<br><br>");
		string.append("It would appear that Neary tripped while on his way to return Macomb’s guitar, then ran away, ");
		string.append("presumably to find a less dark place to hide in while waiting for Hammett to arrive. But he was smart ");
		string.append("enough to leave his shoe by the door to keep it from locking, clearly meant to help Macomb.");
		
		Story enter=new Story("Enter the substation", string.toString());
		story.addEdge(begin, enter, 2);
		story.addEdge(bury, enter, 1);
		
		return enter;
	}
	
	private static Story MakeSoloPath2(Story start1, Story start2, Story inside) //fail to restore power and try to find others
	{
		//no dialogue because no one to talk to
		StringBuilder string=new StringBuilder(600); 
		string.delete(0, string.length());
		string.append("Once inside, you take out your phone and activate the flashlight in order to see the inside of the ");
		string.append("substation, as the blackout has disabled its interior’s lights.<br><br>");
		string.append("A few steps in front of you is an auxiliary cord, stretched dangerously high across the floor, ");
		string.append("leading to an amplifier placed next to a desk. You instantly notice the coffee spilled on the floor ");
		string.append("below the desk, with the shattered pieces of a mug near it. On the desk is a large computer, and ");
		string.append("there’s a control box to the desk’s right, with a circuit breaker to its left. A dented electric guitar ");
		string.append("is lying on the ground, and there’s an equal sized dent in the control box, evidently the cause of ");
		string.append("the blackout.<br><br>");
		string.append("A closer look at the room reveals a large generator opposite the desk, taking up half of the room, ");
		string.append("and currently silent. There’s a wardrobe on the far side of the room, opposite the entrance, and ");
		string.append("there are a few posters of musicians you don’t recognise on the walls. The desk has a few framed ");
		string.append("pictures on it, of Macomb and the others. This was clearly a weird combination of Macomb’s ");
		string.append("workplace and living quarters.<br><br>");
		string.append("It would appear that Neary tripped while on his way to return Macomb’s guitar, then ran away, ");
		string.append("presumably to find a less dark place to hide in while waiting for Hammett to arrive. But he was smart ");
		string.append("enough to leave his shoe by the door to keep it from locking, clearly meant to help Macomb.");
		
		Story begin=new Story("Continue", string.toString());
		story.addEdge(start1, begin, 1);
		story.addEdge(start2, begin, 1);
		
		string.delete(0, string.length());
		string.append("You walk closer to the control box, unplugging the cord so you can’t trip over it. You examine ");
		string.append("the damage to it, noticing that it’s relatively minor. Parts have been broken and several wires are ");
		string.append("frayed, but the good news is that the damage is localised to the area where the guitar hit the box.<br><br>");
		string.append("You look around and find a toolbox on top of the wardrobe. After retrieving it, you find a few ");
		string.append("replacement parts inside, along with a bunch of tools whose names you don’t even know. But the ");
		string.append("bad news is that without Macomb or even the Internet to guide you, you don’t even know where to start.");
		string.append("<br><br>You do your best to fix the damage, but quickly discover you have no idea what you’re doing. ");
		string.append("After a few more minutes of half-heartedly fumbling around with the box, you give up, knowing ");
		string.append("there’s nothing you can do to restore the power. Rather than waste any more of your precious ");
		string.append("phone battery, or stay in a dark room alone, you decide to go find Muldrew and Alex, hoping ");
		string.append("they’re still alive.");
		
		Story inspect=new Story("Try to restore the power", string.toString());
		story.addEdge(begin, inspect, 1);
		story.addEdge(inside, inspect, 1);
		
		//dino encounter
		string.delete(0, string.length());
		string.append("The walk to the clearing takes a while, and you're constantly on edge, looking out for dinosaurs, but ");
		string.append("there aren't any. You look through the paths' signs to try and remember where Muldrew said he was ");
		string.append("taking Alex. When you find one reading “Lodges: COMING SOON”, you recall that was the place, and start ");
		string.append("walking down it, the afternoon fading into evening.<br><br>");
		string.append("The walk is tense but hopeful, but those hopes are quickly dashed ");
		string.append("when you hear footsteps behind you, too heavy to be from any human. You turn around to see ");
		string.append("what it is, and find a huge dinosaur walking onto the path a few metres behind you.<br><br>");
		string.append("It’s even larger than the last one, around the size of a school bus. This one doesn’t have any bone ");
		string.append("crests, but it has the same T. rex-like arms and bipedalism. Its teeth are serrated rather than ");
		string.append("curved. It’s clearly a different species. And unlike the previous dinosaur, this one barely even ");
		string.append("hesitates before charging at you, clearly wanting you as a meal.");

		Story encounter=new Story("Continue", string.toString());
		story.addEdge(inspect, encounter, 1);
		
		string.delete(0, string.length());
		string.append("You start running as fast as you can without looking back, and the dinosaur follows, easily ");
		string.append("keeping pace despite its large size. The forest’s previous quiet tranquility has been shattered, ");
		string.append("replaced by the dinosaur’s thundering stomps and your panicked breathing, punctuated by its hungry growls.");
		string.append("<br><br>Your heart is pounding so hard it’s almost audible, and you don’t even need to look back to tell ");
		string.append("that the dinosaur is dangerously close to you. You decide to go to the control tower, hoping Muldrew will ");
		string.append("be able to help you handle this dinosaur. It’s not within biting distance yet, but at this rate, ");
		string.append("you won’t be outrunning it either. You know you need to do something to gain the advantage, or ");
		string.append("the dinosaur will end up outlasting you.");
		
		Story run=new Story("Run", string.toString());
		story.addEdge(encounter, run, 1);  
		
		return run;
	}
	
	private static Story MakeMuldrewEscape(Story start) //escape allosaurus with muldrew and alex
	{
		//run away; no choices because main story is over, just flavour to avoid being boring
		StringBuilder string=new StringBuilder(600); 
		string.append("The dinosaur is bigger, stronger, and tougher than you, but the one thing you have over it is your ");
		string.append("intelligence. You frantically scan the environment while running, and note all the trees on either ");
		string.append("side of the path you’re on. They’re growing close together enough that it would have trouble ");
		string.append("following you through the gaps, given its size.<br> <br>");
		string.append("“Follow me!” you yell at Muldrew, as you veer off the path and run into the forest, ");
		string.append("making sure to keep the path in sight so you don’t get lost. He quickly understands your plan ");
		string.append("and follows, racing ahead of you. The dinosaur attempts to follow, but can’t ");
		string.append("even squeeze its body more than halfway through the many tree trunks, which is an easy fit for a ");
		string.append("human like you. You’re finally able to slow down and catch your breath, and still keeping a large ");
		string.append("amount of distance, stand and watch it for a moment. Muldrew sees what you’re doing ");
		string.append("and also slows to a stop, even farther away from the dinosaur than you are.<br><br>");
		string.append("Even though the dinosaur can’t follow you into the forest, it doesn’t give up, backing up and ");
		string.append("standing on the path, as if knowing that you’re going to leave the cover of the trees eventually.<br><br>");
		string.append("“Good thinking,” Muldrew says to you, panting from exertion, but clearly impressed. ");
		string.append("He places Alex down, no longer needing to carry him, and comments “At least there’s enough trees ");
		string.append("to get us to the tower safely. We’ll have to run for it then, but we can hide inside the tower.”<br><br>");
		string.append("You’re fully aware that this doesn’t get rid of the dinosaur, but it allows your heart to stop ");
		string.append("pounding and keeps you from being eaten, which is the second best thing. You start walking ");
		string.append("through the forest, parallel to the path, and the dinosaur follows, stalking you. Despite the silent ");
		string.append("tension of the walk, you do at least get to observe a dinosaur up close for the first time, almost ");
		string.append("able to appreciate it now that it’s not trying to kill you.<br><br>");
		string.append("Before long, you near the end of the path, and can see the stone arches in sight. You brace yourself for ");
		string.append("one final burst of speed, hoping you can reach the control tower and shelter inside of it before the ");
		string.append("dinosaur can catch you.");
		
		Story hide=new Story("(Requires Doctor) Outwit the dinosaur", string.toString());
		story.addEdge(start, hide, 1);
		
		string.delete(0, string.length());
		string.append("The dinosaur is bigger, stronger, and tougher than you, but the one thing you have over it is your ");
		string.append("speed. You may no longer be a Marine, but it was hard to completely break the habit of ");
		string.append("exercising regularly, and you’re still in good shape.<br> <br>");
		string.append("Knowing that you might die if you don’t, you push yourself to run faster than you ever have in ");
		string.append("your life, barely even feeling tired due to the adrenaline. Despite his panting, ");
		string.append("probably more from carrying Alex than from exhaustion, Muldrew manages to keep up, ");
		string.append("and the two of you run almost side by side.<br><br>");
		string.append("The bad news is that the dinosaur doesn’t seem to be tiring at all. The good news is that you’re ");
		string.append("finally nearing the end of the path, and can see the stone arches in sight. You brace yourself for ");
		string.append("one final burst of speed, hoping you can reach the control tower and shelter inside of it before the ");
		string.append("dinosaur can catch you.");

		Story run=new Story("(Requires Lawyer) Outrun the dinosaur", string.toString());
		story.addEdge(start, run, 2);
		
		//reunite with macomb
		string.delete(0, string.length());
		string.append("You steel yourself, and run as fast as you can through those stone arches, not daring to look back ");
		string.append("as the dinosaur thunders after you, growling and letting out a roar that’s loud enough to almost ");
		string.append("make you flinch. Alex actually does flinch, but Muldrew just grits his teeth.<br> <br>");
		string.append("On the other side of the arches is Macomb, impressively still alive and looking uninjured, but in ");
		string.append("front of him is a dinosaur anyone would recognise on sight: a stegosaurus. It’s almost the exact ");
		string.append("same size as the creature chasing you, though it walks on all fours. It has two rows of bony ");
		string.append("spikes running down its back, and has four long spikes attached to its tail’s end. Unlike the other ");
		string.append("dinosaurs you’ve encountered, it doesn’t appear aggressive at all, munching on a cycad. ");
		string.append("However, its long body is completely blocking the control tower’s entrance.<br><br>");
		string.append("When Macomb sees you running over, he grins and shouts “You’re alive!” However, his grin ");
		string.append("dies the moment he sees what’s chasing you.<br><br>");
		string.append("“We have to hide!” you yell at him, but the one safe place around here is currently inaccessible, ");
		string.append("blocked off by the stegosaurus, whose back spikes go erect when it notices your approach. It ");
		string.append("stops eating and turns in your direction, looking like it’s preparing for a fight. You seem to be ");
		string.append("stuck in a difficult spot.");
		
		Story sprint=new Story("Run as fast as you can", string.toString());
		story.addEdge(hide, sprint, 1);
		story.addEdge(run, sprint, 1);
		
		string.delete(0, string.length());
		string.append("You already know that you’re being chased by an allosaurus, which is a carnivore and apex ");
		string.append("predator. The stegosaurus is a herbivore. Both are easily capable of killing a human, but under ");
		string.append("completely different conditions.<br> <br>");
		string.append("“Follow me!” you shout, and run towards the stegosaurus. You know that its erect spikes are a ");
		string.append("sign of threat display, but you're not the threat it’s focused on. It’s not looking at you, but the ");
		string.append("dinosaur behind you; you know the situation isn’t quite as bad as it seems.<br><br>");
		string.append("Muldrew obeys without hesitation, implicitly trusting you after all you’ve been through, and the ");
		string.append("two of you run towards the stegosaurus. However, you make sure to take the long way around ");
		string.append("and stay a good distance back, not wanting to risk provoking it, or getting within range of its tail.");
		string.append("<br><br>When Macomb sees the stegosaurus practically ignoring you and Muldrew, he follows, ");
		string.append("scrambling to get out of the allosaurus’s way and join you in safety. You and Muldrew catch your ");
		string.append("breath while Macomb silently panics. Muldrew finally sets Alex down, and the four of you end up hiding ");
		string.append("behind the control tower, instead of inside of it as planned.<br><br>");
		string.append("The allosaurus finally stops when it sees that the stegosaurus is now between you and it, and the ");
		string.append("two dinosaurs stare at each other tensely before the allosaurus suddenly attacks.");
		
		Story think=new Story("(Requires Doctor) Make a decision", string.toString());
		story.addEdge(sprint, think, 1);
		
		string.delete(0, string.length());
		string.append("“What do I do?!” you shout, not just to Macomb, but to Muldrew and even Alex. You don’t stop ");
		string.append("running, but are rapidly running out of ground and breath.<br> <br>");
		string.append("“Go to the stegosaurus!” Muldrew shouts back, voice strained from growing exhaustion, and ");
		string.append("leads the way, not bothering to explain. He takes a wide berth around the stegosaurus, ");
		string.append("presumably trying to avoid accidentally provoking or startling it, and you follow. Surprisingly, it ");
		string.append("doesn’t even seem to care, too busy staring down the other dinosaur.<br><br>");
		string.append("When Macomb sees the stegosaurus practically ignoring you and Muldrew, he follows, ");
		string.append("scrambling to get out of harm’s way and join you in safety. You and Muldrew nearly collapse from exhaustion");
		string.append(", your legs like jelly, while Macomb silently panics. Muldrew finally sets Alex down, and the four of ");
		string.append("you hide behind the control tower, instead of inside of it as planned.<br><br>");
		string.append("The dinosaur that’d been chasing you finally stops when it sees that the stegosaurus is now between ");
		string.append("you and it, and the two dinosaurs stare at each other tensely before the stegosaurus is suddenly attacked.");

		Story cry=new Story("(Requires Lawyer) Make a decision", string.toString());
		story.addEdge(sprint, cry, 2);
		
		string.delete(0, string.length());
		string.append("The dinosaur that’d been chasing you, an <a href=\"allo.html\">allosaurus</a>, charges at the stegosaurus, ");
		string.append("and the stegosaurus reacts in an instant. It turns to the side and lashes out with its thagomizer, its spiked ");
		string.append("tail, which moves almost in a blur. You’re glad you’ve taken cover behind the tower, because the ");
		string.append("swing is wide and the stegosaurus doesn’t seem to care about anything other than its fight for its ");
		string.append("life. The thagomizer’s spikes sink into the allosaurus’s side, and come out covered in blood.<br><br>");
		string.append("The allosaurus bellows but doesn’t stop, only staggering a little before thundering forth and ");
		string.append("extending its open mouth. The stegosaurus turns further to the side in a defensive move, ");
		string.append("exposing its side to protect its head, but it moves slowly, shuffling its feet. The allosaurus sinks ");
		string.append("its teeth into the upper part of the stegosaurus’s flank and tears out a bloody chunk, making the ");
		string.append("wound gush blood while the stegosaurus hisses in pain.<br> <br>");
		string.append("While the allosaurus is swallowing the meat, the stegosaurus lashes out with its blood soaked ");
		string.append("thagomizer again, this time hitting the allosaurus higher, in its neck. The allosaurus makes a ");
		string.append("gurgly roar, its throat already filling with blood. Both dinosaurs are badly injured, but neither ");
		string.append("stops attacking the other.<br><br>");
		string.append("The allosaurus takes another bite from the stegosaurus in the same spot, deepening its already ");
		string.append("large bite wound. Based on the massive amount of blood dripping from the wound, the ");
		string.append("allosaurus must have gotten several vital organs. The stegosaurus lets out a weak hiss but can’t ");
		string.append("even swing its thagomizer again before collapsing, dead.<br><br>");
		string.append("The allosaurus feasts on its corpse, hungrily taking messy bites out of its sides and swallowing ");
		string.append("the bloody chunks of meat before going for more. However, it’s still wounded, and its neck ");
		string.append("continues to hemorrhage blood while it eats. The allosaurus doesn’t seem to care, but its greedy ");
		string.append("feeding noticeably slows down. Its movements gradually become more sluggish, and eventually ");
		string.append("it falls over with a loud thud, dead from the blood loss.");
		
		Story fight=new Story("Continue", string.toString());
		story.addEdge(think, fight, 1);
		story.addEdge(cry, fight, 1);
		
		//talk with macomb about survival; another false choice to keep things interesting
		StringBuilder prime=new StringBuilder(600);
		prime.append("<br><br>Muldrew sighs. “Hammett’s going to be pissed. <a href=\"stego.html\">Two</a> species just went extinct again.”");
		prime.append("<br><br>Macomb laughs, almost hysterically. “Hammett? An allosaurus just tried to eat you and you’re ");
		prime.append("concerned about Hammett’s investment value going down?”<br> <br>");
		prime.append("Alex giggles, glad that Macomb is back and completely safe. Muldrew looks at Macomb and ");
		prime.append("smiles, saying “It’s good to see you again too, mate.” The two of them hug each other, and then ");
		prime.append("Alex, before Macomb turns to you.<br><br>");
		prime.append("“I’m glad you’re alive too. Thanks for keeping these two alive for me,” he smiles, eyes overflowing with ");
		prime.append("gratitude.<br><br>");
		prime.append("“Thanks,” you tell him. “I did my best.”<br><br>");
		prime.append("“Don’t be modest, mate. You practically single-handedly saved us back there,” Muldrew says to ");
		prime.append("you, clapping you on the back.<br><br>");
		prime.append("“‘Mate’?” you ask him, noting that’s the first time he’s called you that.<br><br>");
		prime.append("Muldrew smiles. “You earned it. You save my life, what else would I call you?”<br><br>");
		prime.append("Suddenly, the door to the control tower creaks open. All four of your heads snap over to see Neary emerging.");
				
		string.delete(0, string.length());
		string.append("“That was…amazing,” you say, staring in awe at the two dinosaur corpses. You are one of the ");
		string.append("only humans to ever witness two dinosaurs fight, a sight better than any nature documentary.");

		Story amaze=new Story("React with awe", string.toString()+prime.toString());
		story.addEdge(fight, amaze, 1);
		
		string.delete(0, string.length());
		string.append("“That was…terrifying,” you admit, staring at the two dinosaur corpses, one of which had been ");
		string.append("trying to eat you just minutes ago. Your heart is still pounding from the experience.");

		Story terror=new Story("React with shock", string.toString()+prime.toString());
		story.addEdge(fight, terror, 2);
		
		string.delete(0, string.length());
		string.append("You don’t say anything. You can’t really say anything in the aftermath of such an insane experience.");

		Story stoic=new Story("Don't react", string.toString()+prime.toString());
		story.addEdge(fight, stoic, 3);
		
		string.delete(0, string.length());
		string.append("Neary looks like he can’t believe what he’s seeing, staring at the four of you like he’s looking at a ghost.");
		string.append("<br> <br>“Uncle Donnie, you’re alive!” Alex exclaims. Muldrew looks relieved but not surprised, clearly ");
		string.append("expecting Neary to have made it, but Macomb is just gaping at him.<br><br>");
		string.append("“Neary, where were you?!” Macomb cries, sounding both bewildered and almost angry. “You ");
		string.append("couldn’t have warned us about the blackout?”<br><br>");
		string.append("“I-I, uh…” Neary can’t even respond, just sputters, now looking very guilty.<br><br>");
		string.append("“Wait, what’s this about the blackout?” Muldrew interrupts, looking at Macomb intently.<br><br>");
		string.append("“Neary caused it!” Macomb says, clearly agitated, but suddenly looks guilty too. “Well, I might ");
		string.append("have too, a little bit, maybe.”<br><br>");
		string.append("Muldrew is about to ask more about this when Alex points at something in the sky. “Look, Grandpa’s coming!”");
		string.append("<br><br>All of you turn to look and see a jet on the horizon, approaching the island.");

		Story neary=new Story("“Neary?!”", string.toString()); 
		story.addEdge(amaze, neary, 1);
		story.addEdge(terror, neary, 1);
		story.addEdge(stoic, neary, 1);
		
		return neary;
	}
	
	private static Story MakeMacombEscape(Story start) //escape allosaurus with macomb
	{
		//run away; no choices because main story is over, just flavour to avoid being boring
		StringBuilder string=new StringBuilder(600); 
		string.append("The dinosaur is bigger, stronger, and tougher than you, but the one thing you have over it is your ");
		string.append("intelligence. You frantically scan the environment while running, and note all the trees on either ");
		string.append("side of the path you’re on. They’re growing close together enough that it would have trouble ");
		string.append("following you through the gaps, given its size. <br><br>");
		string.append("“Follow me!” you yell at Macomb, as you veer off the path and run into the forest, ");
		string.append("making sure to keep the path in sight so you don’t get lost. He quickly understands your plan ");
		string.append("and follows, lagging behind you a little. The dinosaur attempts to follow, but can’t ");
		string.append("even squeeze its body more than halfway through the many tree trunks, which is an easy fit for a ");
		string.append("human like you. You’re finally able to slow down and catch your breath, and still keeping a large ");
		string.append("amount of distance, stand and watch it for a moment. Macomb sees what you’re doing ");
		string.append("and also slows to a stop, even farther away from the dinosaur than you are.<br><br>");
		string.append("Even though the dinosaur can’t follow you into the forest, it doesn’t give up, backing up and ");
		string.append("standing on the path, as if knowing that you’re going to leave the cover of the trees eventually.<br><br>");
		string.append("“Good thinking,” Macomb says to you, panting from exhaustion, but clearly impressed. ");
		string.append("He adds “At least there’s enough trees for us to get to the tower without being eaten. ");
		string.append("We’ll have to run for it, but we should be safe inside.”<br><br>");
		string.append("You’re fully aware that this doesn’t get rid of the dinosaur, but it allows your heart to stop ");
		string.append("pounding and keeps you from being eaten, which is the second best thing. You start walking ");
		string.append("through the forest, parallel to the path, and the dinosaur follows, stalking you. Despite the silent ");
		string.append("tension of the walk, you do at least get to observe a dinosaur up close for the first time, almost ");
		string.append("able to appreciate it now that it’s not trying to kill you.<br><br>");
		string.append("Before long, you near the end of the path, and can see the stone arches in sight. You brace yourself for ");
		string.append("one final burst of speed, hoping you can reach the control tower and shelter inside of it before the ");
		string.append("dinosaur can catch you.");

		Story hide=new Story("(Requires Doctor) Outwit the dinosaur", string.toString());
		story.addEdge(start, hide, 1);
		
		string.delete(0, string.length());
		string.append("The dinosaur is bigger, stronger, and tougher than you, but the one thing you have over it is your ");
		string.append("speed. You may no longer be a Marine, but it was hard to completely break the habit of ");
		string.append("exercising regularly, and you’re still in good shape. <br><br>");
		string.append("Knowing that you might die if you don’t, you push yourself to run faster than you ever have in ");
		string.append("your life, barely even feeling tired due to the adrenaline. ");
		string.append("Despite his exhausted panting, Macomb doesn’t ");
		string.append("want to get eaten either, and tries to keep up, still lagging behind you, but at least not within the ");
		string.append("dinosaur’s biting distance.<br><br>");
		string.append("The bad news is that the dinosaur doesn’t seem to be tiring at all. The good news is that you’re ");
		string.append("finally nearing the end of the path, and can see the stone arches in sight. You brace yourself for ");
		string.append("one final burst of speed, hoping you can reach the control tower and shelter inside of it before the ");
		string.append("dinosaur can catch you.");

		Story run=new Story("(Requires Lawyer) Outrun the dinosaur", string.toString());
		story.addEdge(start, run, 2);
		
		//reunite with muldrew and alex
		string.delete(0, string.length());
		string.append("You steel yourself, and run as fast as you can through those stone arches, not daring to look back ");
		string.append("as the dinosaur thunders after you, growling and letting out a roar that’s loud enough to almost ");
		string.append("make you flinch. Macomb actually does flinch, but the sound seems to motivate him to run faster. <br><br>");
		string.append("On the other side of the arches is Muldrew and Alex. Both are still alive and look uninjured, but ");
		string.append("in front of them is a dinosaur anyone would recognise on sight: a stegosaurus. It’s almost the ");
		string.append("exact same size as the creature chasing you, though it walks on all fours. It has two rows of bony ");
		string.append("spikes running down its back, and has four long spikes attached to its tail’s end. Unlike the other ");
		string.append("dinosaurs you’ve encountered, it doesn’t appear aggressive at all, munching on a cycad. The ");
		string.append("control tower is also in sight, looking more like a camouflaged shack than an actual tower. ");
		string.append("However, the stegosaurus’s long body is completely blocking the tower’s entrance.<br><br>");
		string.append("When Muldrew sees you running over, he smiles and shouts “You made it!”, and Alex looks ");
		string.append("equally relieved. However, their smiles fade the moment they see what’s chasing you.<br><br>");
		string.append("“We have to hide!” you yell at them, but the one safe place around here is currently inaccessible, ");
		string.append("blocked off by the stegosaurus, whose back spikes go erect when it notices your approach. It ");
		string.append("stops eating and turns in your direction, looking like it’s preparing for a fight. You seem to be ");
		string.append("stuck in a difficult spot.");
		
		Story sprint=new Story("Run as fast as you can", string.toString());
		story.addEdge(hide, sprint, 1);
		story.addEdge(run, sprint, 1);
		
		string.delete(0, string.length());
		string.append("You already know that you’re being chased by an allosaurus, which is a carnivore and apex ");
		string.append("predator. The stegosaurus is a herbivore. Both are easily capable of killing a human, but under ");
		string.append("completely different conditions. <br><br>");
		string.append("“Follow me!” you shout, and run towards the stegosaurus. You know that its erect spikes are a ");
		string.append("sign of threat display, but you're not the threat it’s focused on. It’s not looking at you, but the ");
		string.append("dinosaur behind you; you know the situation isn’t quite as bad as it seems.<br><br>");
		string.append("Macomb obeys without hesitation, explicitly trusting you after all you’ve been through, and the ");
		string.append("two of you run towards the stegosaurus. However, you make sure to take the long way around ");
		string.append("and stay a good distance back, not wanting to risk provoking it, or getting within range of its tail.");
		string.append("<br><br>Muldrew and Alex follow you two to safety, noticing that the stegosaurus is practically ignoring ");
		string.append("you. You and Macomb catch your breath while Muldrew looks tense and Alex looks worried. ");
		string.append("The four of you end up hiding behind the control tower, instead of inside of it as planned.<br><br>");
		string.append("The allosaurus finally stops when it sees that the stegosaurus is now between you and it, and the ");
		string.append("two dinosaurs stare at each other tensely before the allosaurus suddenly attacks.");

		Story think=new Story("(Requires Doctor) Make a decision", string.toString());
		story.addEdge(sprint, think, 1);
		
		string.delete(0, string.length());
		string.append("“What do I do?!” you shout, not just to Muldrew, but to Macomb and Alex. You don’t stop ");
		string.append("running, but are rapidly running out of ground and breath. <br><br>");
		string.append("“Go to the stegosaurus!” Muldrew shouts back, and leads the way with Alex, not bothering to ");
		string.append("explain. He takes a wide berth around the stegosaurus, presumably trying to avoid accidentally ");
		string.append("provoking or startling it, and you follow. Surprisingly, it doesn’t even seem to care, too busy ");
		string.append("staring down the other dinosaur.<br><br>");
		string.append("When Macomb sees the stegosaurus practically ignoring Muldrew and Alex, he follows, ");
		string.append("scrambling to get out of harm’s way and join you in safety. You and Macomb nearly collapse ");
		string.append("from exhaustion, your legs like jelly, while Muldrew looks tense and Alex looks worried. The ");
		string.append("four of you end up hiding behind the control tower, instead of inside of it as planned.<br><br>");
		string.append("The dinosaur that’d been chasing you finally stops when it sees that the stegosaurus is now between you ");
		string.append("and it, and the two dinosaurs stare at each other tensely before the stegosaurus is suddenly attacked.");

		Story cry=new Story("(Requires Lawyer) Make a decision", string.toString());
		story.addEdge(sprint, cry, 2);
		
		string.delete(0, string.length());
		string.append("The dinosaur that’d been chasing you, an <a href=\"allo.html\">allosaurus</a>, charges at the stegosaurus, and ");
		string.append("the stegosaurus reacts in an instant. It turns to the side and lashes out with its thagomizer, its spiked ");
		string.append("tail, which moves almost in a blur. You’re glad you’ve taken cover behind the tower, because the ");
		string.append("swing is wide and the stegosaurus doesn’t seem to care about anything other than its fight for its ");
		string.append("life. The thagomizer’s spikes sink into the allosaurus’s side, and come out covered in blood. <br><br>");
		string.append("The allosaurus bellows but doesn’t stop, only staggering a little before thundering forth and ");
		string.append("extending its open mouth. The stegosaurus turns further to the side in a defensive move, ");
		string.append("exposing its side to protect its head, but it moves slowly, shuffling its feet. The allosaurus sinks ");
		string.append("its teeth into the upper part of the stegosaurus’s flank and tears out a bloody chunk, making the ");
		string.append("wound gush blood while the stegosaurus hisses in pain.<br><br>");
		string.append("While the allosaurus is swallowing the meat, the stegosaurus lashes out with its blood soaked ");
		string.append("thagomizer again, this time hitting the allosaurus higher, in its neck. The allosaurus makes a ");
		string.append("gurgly roar, its throat already filling with blood. Both dinosaurs are badly injured, but neither ");
		string.append("stops attacking the other.<br><br>");
		string.append("The allosaurus takes another bite from the stegosaurus in the same spot, deepening its already ");
		string.append("large bite wound. Based on the massive amount of blood dripping from the wound, the ");
		string.append("allosaurus must have gotten several vital organs. The stegosaurus lets out a weak hiss but can’t ");
		string.append("even swing its thagomizer again before collapsing, dead.<br><br>");
		string.append("The allosaurus feasts on its corpse, hungrily taking messy bites out of its sides and swallowing ");
		string.append("the bloody chunks of meat before going for more. However, it’s still wounded, and its neck ");
		string.append("continues to hemorrhage blood while it eats. The allosaurus doesn’t seem to care, but its greedy ");
		string.append("feeding noticeably slows down. Its movements gradually become more sluggish, and eventually ");
		string.append("it falls over with a loud thud, dead from the blood loss.");
		
		Story fight=new Story("Continue", string.toString());
		story.addEdge(think, fight, 1);
		story.addEdge(cry, fight, 1);
		
		//talk with muldrew about survival; another false choice to keep things interesting
		StringBuilder prime=new StringBuilder(600);
		prime.append("<br><br>Muldrew sighs. “Hammett’s going to be pissed. <a href=\"stego.html\">Two</a> species just went extinct again.”");
		prime.append("<br><br>Macomb laughs, almost hysterically. “Hammett? An allosaurus just tried to eat us and you’re ");
		prime.append("concerned about Hammett’s investment value going down?” <br><br>");
		prime.append("Alex giggles, glad that Macomb is back and completely safe. Muldrew looks at Macomb and ");
		prime.append("smiles, saying “It’s good to see you again too, mate.” The two of them hug each other, and then ");
		prime.append("Alex, before Muldrew turns to you.<br><br>");
		prime.append("“I’m glad you’re alive too, mate. Thanks for keeping this idiot safe for me,” he smiles a little, and ");
		prime.append("claps you on the back.<br><br>");
		prime.append("“Thanks,” you tell him. “I did my best...wait, ‘mate’?” You note that's the first time he's called you that.");
		prime.append("<br><br>Muldrew smiles a little wider. “You earned it. You save my mate's life, what else would I call you?”");
		prime.append("<br><br>Suddenly, the door to the control tower creaks open. All four of your heads snap over to see Neary emerging.");

		string.delete(0, string.length());
		string.append("“That was…amazing,” you say, staring in awe at the two dinosaur corpses. You are one of the ");
		string.append("only humans to ever witness two dinosaurs fight, a sight better than any nature documentary.");

		Story amaze=new Story("React with awe", string.toString()+prime.toString());
		story.addEdge(fight, amaze, 1);

		string.delete(0, string.length());
		string.append("“That was…terrifying,” you admit, staring at the two dinosaur corpses, one of which had been ");
		string.append("trying to eat you just minutes ago. Your heart is still pounding from the experience.");

		Story terror=new Story("React with shock", string.toString()+prime.toString());
		story.addEdge(fight, terror, 2);

		string.delete(0, string.length());
		string.append("You don’t say anything. You can’t really say anything in the aftermath of such an insane experience.");

		Story stoic=new Story("Don't react", string.toString()+prime.toString());
		story.addEdge(fight, stoic, 3);

		string.delete(0, string.length());
		string.append("Neary looks like he can’t believe what he’s seeing, staring at the four of you like he’s looking at a ghost.");
		string.append(" <br><br>“Uncle Donnie, you’re alive!” Alex exclaims. Muldrew looks relieved but not surprised, clearly ");
		string.append("expecting Neary to have made it, but Macomb is just gaping at him.<br><br>");
		string.append("“Neary, where were you?!” Macomb cries, sounding both bewildered and almost angry. “You ");
		string.append("couldn’t have warned us about the blackout?”<br><br>");
		string.append("“I-I, uh…” Neary can’t even respond, just sputters, now looking very guilty.<br><br>");
		string.append("“Wait, what’s this about the blackout?” Muldrew interrupts, looking at Macomb intently.<br><br>");
		string.append("“Neary caused it!” Macomb says, clearly agitated, but suddenly looks guilty too. “Well, I might ");
		string.append("have too, a little bit, maybe…”<br><br>");
		string.append("Muldrew is about to ask more about this when Alex points at something in the sky. “Look, Grandpa’s coming!”");
		string.append("<br><br>All of you turn to look and see a jet on the horizon, approaching the island.");

		Story neary=new Story("“Neary?!”", string.toString()); 
		story.addEdge(amaze, neary, 1); 
		story.addEdge(terror, neary, 1);
		story.addEdge(stoic, neary, 1);
		
		return neary;
	}
	
	private static Story MakeAlexEscape(Story start) //escape allosaurus with alex
	{
		//run away; no choices because main story is over, just flavour to avoid being boring
		StringBuilder string=new StringBuilder(600);
		string.append("The dinosaur is bigger, stronger, and tougher than you, but the one thing you have over it is your ");
		string.append("intelligence. You frantically scan the environment while running, and note all the trees on either ");
		string.append("side of the path you’re on. They’re growing close together enough that it would have trouble ");
		string.append("following you through the gaps, given its size.  <br><br>");
		string.append("The dinosaur attempts to follow, but can’t ");
		string.append("even squeeze its body more than halfway through the many tree trunks, which is an easy fit for a ");
		string.append("human like you. You’re finally able to slow down and catch your breath, and still keeping a large ");
		string.append("amount of distance, stand and watch it for a moment.<br><br>");
		string.append("Even though the dinosaur can’t follow you into the forest, it doesn’t give up, backing up and ");
		string.append("standing on the path, as if knowing that you’re going to leave the cover of the trees eventually.<br><br>");
		string.append("Alex is silent as you place him down, no longer needing to carry him now. You tell him “I think there’s ");
		string.append("enough trees to get us to the tower safely. We’ll have to run for it then, but we can hide inside the tower.”");
		string.append(" He nods in response, still staring at the dinosaur wordlessly.<br><br>");
		string.append("You’re fully aware that this doesn’t get rid of the dinosaur, but it allows your heart to stop ");
		string.append("pounding and keeps you from being eaten, which is the second best thing. You start walking ");
		string.append("through the forest, parallel to the path, and the dinosaur follows, stalking you. Despite the silent ");
		string.append("tension of the walk, you do at least get to observe a dinosaur up close for the first time, almost ");
		string.append("able to appreciate it now that it’s not trying to kill you.<br><br>");
		string.append("Before long, you near the end of the path, and can see the stone arches in sight. You brace yourself for ");
		string.append("one final burst of speed, hoping you can reach the control tower and shelter inside of it before the ");
		string.append("dinosaur can catch you.");
		
		Story hide=new Story("(Requires Doctor) Outwit the dinosaur", string.toString());
		story.addEdge(start, hide, 1);
		
		string.delete(0, string.length());
		string.append("The dinosaur is bigger, stronger, and tougher than you, but the one thing you have over it is your ");
		string.append("speed. You may no longer be a Marine, but it was hard to completely break the habit of ");
		string.append("exercising regularly, and you’re still in good shape.  <br><br>");
		string.append("Knowing that you might die if you don’t, you push yourself to run faster than you ever have in ");
		string.append("your life, barely even feeling tired due to the adrenaline, not even feeling Alex’s weight in your ");
		string.append("arms, and too focused to notice the way he’s completely tensed up, as if afraid to move.<br><br>");
		string.append("The bad news is that the dinosaur doesn’t seem to be tiring at all. The good news is that you’re ");
		string.append("finally nearing the end of the path, and can see the stone arches in sight. You brace yourself for ");
		string.append("one final burst of speed, hoping you can reach the control tower and shelter inside of it before the ");
		string.append("dinosaur can catch you.");

		Story run=new Story("(Requires Lawyer) Outrun the dinosaur", string.toString());
		story.addEdge(start, run, 2);
		
		//reunite with macomb, without muldrew
		string.delete(0, string.length());
		string.append("You steel yourself, and run as fast as you can through those stone arches, not daring to look back ");
		string.append("as the dinosaur thunders after you, growling and letting out a roar that’s loud enough to almost ");
		string.append("make you flinch, though Alex actually does flinch.  <br><br>");
		string.append("On the other side of the arches is Macomb, impressively still alive and looking uninjured, but in ");
		string.append("front of him is a dinosaur anyone would recognise on sight: a stegosaurus. It’s almost the exact ");
		string.append("same size as the creature chasing you, though it walks on all fours. It has two rows of bony ");
		string.append("spikes running down its back, and has four long spikes attached to its tail’s end. Unlike the other ");
		string.append("dinosaurs you’ve encountered, it doesn’t appear aggressive at all, munching on a cycad. ");
		string.append("However, its long body is completely blocking the control tower’s entrance.<br><br>");
		string.append("When Macomb sees you running over, he grins and shouts “You’re alive!” However, his grin ");
		string.append("dies the moment he sees the dried blood on your shirt and notices Muldrew's absence.<br><br>");
		string.append("“We have to hide!” you yell at him, but the one safe place around here is currently inaccessible, ");
		string.append("blocked off by the stegosaurus, whose back spikes go erect when it notices your approach. It ");
		string.append("stops eating and turns in your direction, looking like it’s preparing for a fight. You seem to be ");
		string.append("stuck in a difficult spot.");
		
		Story sprint=new Story("Run as fast as you can", string.toString());
		story.addEdge(hide, sprint, 1);
		story.addEdge(run, sprint, 1);
		
		string.delete(0, string.length());
		string.append("You already know that you’re being chased by an allosaurus, which is a carnivore and apex ");
		string.append("predator. The stegosaurus is a herbivore. Both are easily capable of killing a human, but under ");
		string.append("completely different conditions.  <br><br>");
		string.append("“Follow me!” you shout, and run towards the stegosaurus. You know that its erect spikes are a ");
		string.append("sign of threat display, but you're not the threat it’s focused on. It’s not looking at you, but the ");
		string.append("dinosaur behind you; you know the situation isn’t quite as bad as it seems.<br><br>");
		string.append("You run towards the stegosaurus, making sure to take the long way around ");
		string.append("and stay a good distance back, not wanting to risk provoking it, or getting within range of its tail.");
		string.append("<br><br>When Macomb sees the stegosaurus practically ignoring you, he follows, ");
		string.append("scrambling to get out of the allosaurus’s way and join you in safety. You put Alex down and catch your ");
		string.append("breath while Macomb silently panics. The three of you end up hiding ");
		string.append("behind the control tower, instead of inside of it as planned.<br><br>");
		string.append("The allosaurus finally stops when it sees that the stegosaurus is now between you and it, and the ");
		string.append("two dinosaurs stare at each other tensely before the allosaurus suddenly attacks.");
		
		Story think=new Story("(Requires Doctor) Make a decision", string.toString());
		story.addEdge(sprint, think, 1);
		
		string.delete(0, string.length());
		string.append("“What do I do?!” you shout, not just to Macomb, but even to Alex. You don’t stop ");
		string.append("running, but are rapidly running out of ground and breath.  <br><br>");
		string.append("“Uh...behind the stegosaurus!” Macomb shouts back, uncertain of his plan, but certain that a herbivore ");
		string.append("will be less likely to eat you than a carnivore. He takes a wide berth around the stegosaurus, ");
		string.append("very concerned about it accidentally attacking him, and you follow. Surprisingly, it ");
		string.append("doesn’t even seem to care, too busy staring down the other dinosaur.<br><br>");
		string.append("You set Alex down and nearly collapse from exhaustion, your legs like jelly, while ");
		string.append("Macomb silently panics. The three of ");
		string.append("you end up hiding behind the control tower, instead of inside of it as planned.<br><br>");
		string.append("The dinosaur that’d been chasing you finally stops when it sees that the stegosaurus is now between ");
		string.append("you and it, and the two dinosaurs stare at each other tensely before the stegosaurus is suddenly attacked.");

		Story cry=new Story("(Requires Lawyer) Make a decision", string.toString());
		story.addEdge(sprint, cry, 2);
		
		string.delete(0, string.length());
		string.append("The dinosaur that’d been chasing you, an <a href=\"allo.html\">allosaurus</a>, charges at the stegosaurus, and ");
		string.append("the stegosaurus reacts in an instant. It turns to the side and lashes out with its thagomizer, its spiked ");
		string.append("tail, which moves almost in a blur. You’re glad you’ve taken cover behind the tower, because the ");
		string.append("swing is wide and the stegosaurus doesn’t seem to care about anything other than its fight for its ");
		string.append("life. The thagomizer’s spikes sink into the allosaurus’s side, and come out covered in blood.<br><br>");
		string.append("The allosaurus bellows but doesn’t stop, only staggering a little before thundering forth and ");
		string.append("extending its open mouth. The stegosaurus turns further to the side in a defensive move, ");
		string.append("exposing its side to protect its head, but it moves slowly, shuffling its feet. The allosaurus sinks ");
		string.append("its teeth into the upper part of the stegosaurus’s flank and tears out a bloody chunk, making the ");
		string.append("wound gush blood while the stegosaurus hisses in pain.  <br><br>");
		string.append("While the allosaurus is swallowing the meat, the stegosaurus lashes out with its blood soaked ");
		string.append("thagomizer again, this time hitting the allosaurus higher, in its neck. The allosaurus makes a ");
		string.append("gurgly roar, its throat already filling with blood. Both dinosaurs are badly injured, but neither ");
		string.append("stops attacking the other.<br><br>");
		string.append("The allosaurus takes another bite from the stegosaurus in the same spot, deepening its already ");
		string.append("large bite wound. Based on the massive amount of blood dripping from the wound, the ");
		string.append("allosaurus must have gotten several vital organs. The stegosaurus lets out a weak hiss but can’t ");
		string.append("even swing its thagomizer again before collapsing, dead.<br><br>");
		string.append("The allosaurus feasts on its corpse, hungrily taking messy bites out of its sides and swallowing ");
		string.append("the bloody chunks of meat before going for more. However, it’s still wounded, and its neck ");
		string.append("continues to hemorrhage blood while it eats. The allosaurus doesn’t seem to care, but its greedy ");
		string.append("feeding noticeably slows down. Its movements gradually become more sluggish, and eventually ");
		string.append("it falls over with a loud thud, dead from the blood loss.");
		
		Story fight=new Story("Continue", string.toString());
		story.addEdge(think, fight, 1);
		story.addEdge(cry, fight, 1);
		
		//macomb and alex mourn muldrew
		StringBuilder prime=new StringBuilder(600);
		prime.append("<br><br>Macomb looks you over closely, staring at the bloodstain on your shirt and noticing the look in ");
		prime.append("Alex’s eyes. “Muldrew didn’t make it,” he says softly. It isn’t a question.<br><br>");
		prime.append("Alex tears up and rushes over to hug Macomb, unable to speak. Macomb hugs him, fighting his own tears.");
		prime.append("<br><br>“I’m sorry,” you say, standing there awkwardly.  <br><br>");
		prime.append("Macomb looks at you, tears in his eyes, and says solemnly “I’m sure it’s not your fault.” ");
		prime.append("Actually, Muldrew’s death was arguably your fault, but bringing that up won’t help anything.<br><br>");
		prime.append("“Thanks for keeping Alex safe. That’s all Muldrew wanted,” says Macomb, his voice cracking a little.<br><br>");
		prime.append("“I did my best,” you say, then fall into awkward silence as Alex and Macomb hug each other while silently crying.");
		prime.append("<br><br>Suddenly, the quiet is broken by the sound of the door to the control tower creaking open. All ");
		prime.append("three of your heads snap over to see Neary emerging.");

		string.delete(0, string.length());
		string.append("“That was…amazing,” you say, staring in awe at the two dinosaur corpses. You are one of the ");
		string.append("only humans to ever witness two dinosaurs fight, a sight better than any nature documentary.");

		Story amaze=new Story("React with awe", string.toString()+prime.toString());
		story.addEdge(fight, amaze, 1);

		string.delete(0, string.length());
		string.append("“That was…terrifying,” you admit, staring at the two dinosaur corpses, one of which had been ");
		string.append("trying to eat you just minutes ago. Your heart is still pounding from the experience.");

		Story terror=new Story("React with shock", string.toString()+prime.toString());
		story.addEdge(fight, terror, 2);

		string.delete(0, string.length());
		string.append("You don’t say anything. You can’t really say anything in the aftermath of such an insane experience.");

		Story stoic=new Story("Don't react", string.toString()+prime.toString());
		story.addEdge(fight, stoic, 3);
		
		string.delete(0, string.length());
		string.append("Neary looks like he can’t believe what he’s seeing, staring at the three of you like he’s looking at ");
		string.append("a ghost. He quickly figures out what happened, given Muldrew’s absence and the looks on all your faces.");
		string.append("<br><br>“Uncle Donnie, you’re alive!” Alex exclaims, seeming relieved that he doesn’t have to mourn ");
		string.append("another loss. But Macomb is only gaping at Neary.  <br><br>");
		string.append("“Neary, where were you?!” Macomb suddenly cries, sounding both bewildered and almost angry. ");
		string.append("“You couldn’t have warned us about the blackout?”<br><br>");
		string.append("“I-I, uh…” Neary can’t even respond, just sputters, now looking very guilty.<br><br>");
		string.append("“What about the blackout?” Alex asks, looking at Macomb curiously.<br><br>");
		string.append("“Neary caused it!” Macomb says, clearly agitated, but suddenly looks guilty too. “Well, I might ");
		string.append("have too, a little bit, maybe…”<br><br>");
		string.append("Alex is about to ask more about this when he notices something in the sky, pointing at it. “Look, ");
		string.append("Grandpa’s coming!”<br><br>All of you turn to look and see a jet on the horizon, approaching the island.");

		Story neary=new Story("“Neary?!”", string.toString()); 
		story.addEdge(amaze, neary, 1); 
		story.addEdge(terror, neary, 1);
		story.addEdge(stoic, neary, 1);
		
		return neary;
	}
	
	private static Story MakePowerlessEscape(Story start) //escape allosaurus alone with macomb dead
	{
		//run away; no choices because main story is over, just flavour to avoid being boring
		StringBuilder string=new StringBuilder(600);
		string.append("The dinosaur is bigger, stronger, and tougher than you, but the one thing you have over it is your ");
		string.append("intelligence. You frantically scan the environment while running, and note all the trees on either ");
		string.append("side of the path you’re on. They’re growing close together enough that it would have trouble ");
		string.append("following you through the gaps, given its size.<br>  <br>");
		string.append("You veer off the path and run into the forest, making sure to keep the path in sight so you don’t ");
		string.append("get lost. The dinosaur attempts to follow, but can’t even squeeze its body more than halfway ");
		string.append("through the many tree trunks, which is an easy fit for a human like you. You’re finally able to ");
		string.append("slow down and catch your breath, and still keeping a large amount of distance, stand and watch it ");
		string.append("for a moment.<br><br>");
		string.append("Even though the dinosaur can’t follow you into the forest, it doesn’t give up, backing up and ");
		string.append("standing on the path, as if knowing that you’re going to leave the cover of the trees eventually. ");
		string.append("You know that you will, and will have to sprint to get to the control tower, but hopefully will be ");
		string.append("able to outrun the dinosaur and shelter inside it before it can catch you.<br><br>");
		string.append("You’re fully aware that this doesn’t get rid of the dinosaur, but it allows your heart to stop ");
		string.append("pounding and keeps you from being eaten, which is the second best thing. You start walking ");
		string.append("through the forest, parallel to the path, and the dinosaur follows, stalking you. Despite the silent ");
		string.append("tension of the walk, you do at least get to observe a dinosaur up close for the first time, almost ");
		string.append("able to appreciate it now that it’s not trying to kill you.<br><br>");
		string.append("Before long, you near the end of the path, and can see the stone arches in sight. You brace yourself for ");
		string.append("one final burst of speed, hoping you can reach the control tower and shelter inside of it before the ");
		string.append("dinosaur can catch you.");
		
		Story hide=new Story("(Requires Doctor) Outwit the dinosaur", string.toString());
		story.addEdge(start, hide, 1);
		
		string.delete(0, string.length());
		string.append("The dinosaur is bigger, stronger, and tougher than you, but the one thing you have over it is your ");
		string.append("speed. You may no longer be a Marine, but it was hard to completely break the habit of ");
		string.append("exercising regularly, and you’re still in good shape.<br>  <br>");
		string.append("Knowing that you might die if you don’t, you push yourself to run faster than you ever have in ");
		string.append("your life, barely even feeling tired due to the adrenaline. ");
		string.append("The bad news is that the dinosaur doesn’t seem to be tiring at all. The good news is that you’re ");
		string.append("finally nearing the end of the path, and can see the stone arches in sight. You brace yourself for ");
		string.append("one final burst of speed, hoping you can reach the control tower and shelter inside of it before the ");
		string.append("dinosaur can catch you.");

		Story run=new Story("(Requires Lawyer) Outrun the dinosaur", string.toString());
		story.addEdge(start, run, 2);
		
		//reunite with muldrew and alex, without macomb
		string.delete(0, string.length());
		string.append("You steel yourself, and run as fast as you can through those stone arches, not daring to look back ");
		string.append("as the dinosaur thunders after you, growling and letting out a roar that’s loud enough to almost ");
		string.append("make you flinch.<br>   <br>");
		string.append("On the other side of the arches is Muldrew and Alex. Both are still alive and look uninjured, but ");
		string.append("in front of them is a dinosaur anyone would recognise on sight: a stegosaurus. It’s almost the ");
		string.append("exact same size as the creature chasing you, though it walks on all fours. It has two rows of bony ");
		string.append("spikes running down its back, and has four long spikes attached to its tail’s end. Unlike the other ");
		string.append("dinosaurs you’ve encountered, it doesn’t appear aggressive at all, munching on a cycad. The ");
		string.append("control tower is also in sight, looking more like a camouflaged shack than an actual tower. ");
		string.append("However, the stegosaurus’s long body is completely blocking the tower’s entrance.<br><br>");
		string.append("When Muldrew sees you running over, he smiles and shouts “You made it!”, and Alex looks equally relieved. ");
		string.append("However, their smiles fade the moment they notice the dried blood on your shirt and register Macomb's absence.");
		string.append("<br><br>“We have to hide!” you yell at them, but the one safe place around here is currently inaccessible, ");
		string.append("blocked off by the stegosaurus, whose back spikes go erect when it notices your approach. It ");
		string.append("stops eating and turns in your direction, looking like it’s preparing for a fight. You seem to be ");
		string.append("stuck in a difficult spot.");
		
		Story sprint=new Story("Run as fast as you can", string.toString());
		story.addEdge(hide, sprint, 1);
		story.addEdge(run, sprint, 1);
		
		string.delete(0, string.length());
		string.append("You already know that you’re being chased by an allosaurus, which is a carnivore and apex ");
		string.append("predator. The stegosaurus is a herbivore. Both are easily capable of killing a human, but under ");
		string.append("completely different conditions.<br>  <br>");
		string.append("“Follow me!” you shout, and run towards the stegosaurus. You know that its erect spikes are a ");
		string.append("sign of threat display, but you're not the threat it’s focused on. It’s not looking at you, but the ");
		string.append("dinosaur behind you; you know the situation isn’t quite as bad as it seems.<br><br>");
		string.append("You run towards the stegosaurus, making sure to take the long way around ");
		string.append("and stay a good distance back, not wanting to risk provoking it, or getting within range of its tail.");
		string.append("<br><br>Muldrew and Alex follow you to safety, noticing that the stegosaurus is practically ignoring ");
		string.append("you. You catch your breath while Muldrew looks tense and Alex looks worried, both about the allosaurus and ");
		string.append("Macomb's absence. The three of you end up hiding behind the control tower, instead of inside of it as planned.");
		string.append("<br><br>The allosaurus finally stops when it sees that the stegosaurus is now between you and it, and the ");
		string.append("two dinosaurs stare at each other tensely before the allosaurus suddenly attacks.");

		Story think=new Story("(Requires Doctor) Make a decision", string.toString());
		story.addEdge(sprint, think, 1);
		
		string.delete(0, string.length());
		string.append("“What do I do?!” you shout, to both Muldrew and Alex. You don’t stop ");
		string.append("running, but are rapidly running out of ground and breath.<br>  <br>");
		string.append("“Go to the stegosaurus!” Muldrew shouts back, and leads the way with Alex, not bothering to ");
		string.append("explain. He takes a wide berth around the stegosaurus, presumably trying to avoid accidentally ");
		string.append("provoking or startling it, and you follow. Surprisingly, it doesn’t even seem to care, too busy ");
		string.append("staring down the other dinosaur.<br><br>");
		string.append("You follow them to safety, and nearly collapse from exhaustion, your legs like jelly, while ");
		string.append("Muldrew looks tense and Alex looks worried, both about the allosaurus and Macomb's absence. The ");
		string.append("three of you end up hiding behind the control tower, instead of inside of it as planned.<br><br>");
		string.append("The dinosaur that’d been chasing you finally stops when it sees that the stegosaurus is now between you ");
		string.append("and it, and the two dinosaurs stare at each other tensely before the stegosaurus is suddenly attacked.");

		Story cry=new Story("(Requires Lawyer) Make a decision", string.toString());
		story.addEdge(sprint, cry, 2);
		
		string.delete(0, string.length());
		string.append("The dinosaur that’d been chasing you, an <a href=\"allo.html\">allosaurus</a>, charges at the stegosaurus, and ");
		string.append("the stegosaurus reacts in an instant. It turns to the side and lashes out with its thagomizer, its spiked ");
		string.append("tail, which moves almost in a blur. You’re glad you’ve taken cover behind the tower, because the ");
		string.append("swing is wide and the stegosaurus doesn’t seem to care about anything other than its fight for its ");
		string.append("life. The thagomizer’s spikes sink into the allosaurus’s side, and come out covered in blood.<br>  <br>");
		string.append("The allosaurus bellows but doesn’t stop, only staggering a little before thundering forth and ");
		string.append("extending its open mouth. The stegosaurus turns further to the side in a defensive move, ");
		string.append("exposing its side to protect its head, but it moves slowly, shuffling its feet. The allosaurus sinks ");
		string.append("its teeth into the upper part of the stegosaurus’s flank and tears out a bloody chunk, making the ");
		string.append("wound gush blood while the stegosaurus hisses in pain.<br><br>");
		string.append("While the allosaurus is swallowing the meat, the stegosaurus lashes out with its blood soaked ");
		string.append("thagomizer again, this time hitting the allosaurus higher, in its neck. The allosaurus makes a ");
		string.append("gurgly roar, its throat already filling with blood. Both dinosaurs are badly injured, but neither ");
		string.append("stops attacking the other.<br><br>");
		string.append("The allosaurus takes another bite from the stegosaurus in the same spot, deepening its already ");
		string.append("large bite wound. Based on the massive amount of blood dripping from the wound, the ");
		string.append("allosaurus must have gotten several vital organs. The stegosaurus lets out a weak hiss but can’t ");
		string.append("even swing its thagomizer again before collapsing, dead.<br><br>");
		string.append("The allosaurus feasts on its corpse, hungrily taking messy bites out of its sides and swallowing ");
		string.append("the bloody chunks of meat before going for more. However, it’s still wounded, and its neck ");
		string.append("continues to hemorrhage blood while it eats. The allosaurus doesn’t seem to care, but its greedy ");
		string.append("feeding noticeably slows down. Its movements gradually become more sluggish, and eventually ");
		string.append("it falls over with a loud thud, dead from the blood loss.");
		
		Story fight=new Story("Continue", string.toString());
		story.addEdge(think, fight, 1);
		story.addEdge(cry, fight, 1);
		
		//macomb and alex mourn muldrew
		StringBuilder prime=new StringBuilder(600);
		prime.append("<br><br>Muldrew looks you over closely, staring at the bloodstain on your shirt and noticing Macomb’s ");
		prime.append("absence. “Macomb didn’t make it,” he says softly. It isn’t a question.<br>  <br>");
		prime.append("Alex tears up and rushes over to hug Muldrew, the impact of the words hitting him hard. ");
		prime.append("Muldrew hugs him back, fighting his own tears.<br><br>");
		prime.append("“I’m sorry,” you say, standing there awkwardly.<br><br>");
		prime.append("Muldrew looks at you, eyes watery, and says solemnly “I’m sure it’s not your fault.” Actually, ");
		prime.append("Macomb’s death was arguably your fault, but bringing that up won’t help anything.<br><br>");
		prime.append("“I knew something was wrong when the power didn’t come back,” says Muldrew, his voice ");
		prime.append("cracking a little. “But I went here anyway. I was hoping…” he trails off, unable to finish his ");
		prime.append("sentence, not needing to, the meaning clear.<br><br>");
		prime.append("You fall into uncomfortable silence, watching Muldrew and Alex hug each other while silently crying.");
		prime.append("<br><br>Suddenly, the quiet is broken by the sound of the door to the control tower creaking open. All ");
		prime.append("three of your heads snap over to see Neary emerging.");

		string.delete(0, string.length());
		string.append("“That was…amazing,” you say, staring in awe at the two dinosaur corpses. You are one of the ");
		string.append("only humans to ever witness two dinosaurs fight, a sight better than any nature documentary.");

		Story amaze=new Story("React with awe", string.toString()+prime.toString());
		story.addEdge(fight, amaze, 1);

		string.delete(0, string.length());
		string.append("“That was…terrifying,” you admit, staring at the two dinosaur corpses, one of which had been ");
		string.append("trying to eat you just minutes ago. Your heart is still pounding from the experience.");

		Story terror=new Story("React with shock", string.toString()+prime.toString());
		story.addEdge(fight, terror, 2);

		string.delete(0, string.length());
		string.append("You don’t say anything. You can’t really say anything in the aftermath of such an insane experience.");

		Story stoic=new Story("Don't react", string.toString()+prime.toString());
		story.addEdge(fight, stoic, 3);
		
		string.delete(0, string.length());
		string.append("Neary looks like he can’t believe what he’s seeing, staring at the three of you like he’s looking at ");
		string.append("a ghost. He quickly figures out what happened, given Macomb’s absence and the looks on all your faces.");
		string.append("<br>  <br>“Uncle Donnie, you’re alive!” Alex exclaims, seeming relieved that he doesn’t have to mourn ");
		string.append("another loss. Muldrew is also relieved, but not surprised, looking as if he expected Neary to have made it.");
		string.append("<br><br>“Neary, where were you?” Muldrew asks, voice still strained from grief. “Why didn’t you come ");
		string.append("find us when the power went out?”<br><br>");
		string.append("“I-I, uh…” Neary can’t even respond, just sputters, now looking very guilty.<br><br>");
		string.append("Alex is about to ask about this too when he notices something in the sky, pointing at it. “Look, ");
		string.append("Grandpa’s coming!”<br><br>");
		string.append("All of you turn to look and see a jet on the horizon, approaching the island.");

		Story neary=new Story("“Neary?!”", string.toString()); 
		story.addEdge(amaze, neary, 1); 
		story.addEdge(terror, neary, 1);
		story.addEdge(stoic, neary, 1);
		
		return neary;
	}
	
	private static Story MakeDeathEscape(Story start1, Story start2) //escape allosaurus alone with alex dead
	{
		//run away; no choices because main story is over, just flavour to avoid being boring
		StringBuilder string=new StringBuilder(600);
		string.append("The dinosaur is bigger, stronger, and tougher than you, but the one thing you have over it is your ");
		string.append("intelligence. You frantically scan the environment while running, and note all the trees on either ");
		string.append("side of the path you’re on. They’re growing close together enough that it would have trouble ");
		string.append("following you through the gaps, given its size.<br><br>");
		string.append("You veer off the path and run into the forest, making sure to keep the path in sight so you don’t ");
		string.append("get lost. The dinosaur attempts to follow, but can’t even squeeze its body more than halfway ");
		string.append("through the many tree trunks, which is an easy fit for a human like you. You’re finally able to ");
		string.append("slow down and catch your breath, and still keeping a large amount of distance, stand and watch it ");
		string.append("for a moment.<br><br>");
		string.append("Even though the dinosaur can’t follow you into the forest, it doesn’t give up, backing up and ");
		string.append("standing on the path, as if knowing that you’re going to leave the cover of the trees eventually. ");
		string.append("You know that you will, and will have to sprint to get to the control tower, but hopefully will be ");
		string.append("able to outrun the dinosaur and shelter inside it before it can catch you.<br><br>");
		string.append("You’re fully aware that this doesn’t get rid of the dinosaur, but it allows your heart to stop ");
		string.append("pounding and keeps you from being eaten, which is the second best thing. You start walking ");
		string.append("through the forest, parallel to the path, and the dinosaur follows, stalking you. Despite the silent ");
		string.append("tension of the walk, you do at least get to observe a dinosaur up close for the first time, almost ");
		string.append("able to appreciate it now that it’s not trying to kill you.<br><br>");
		string.append("Before long, you near the end of the path, and can see the stone arches in sight. You brace yourself for ");
		string.append("one final burst of speed, hoping you can reach the control tower and shelter inside of it before the ");
		string.append("dinosaur can catch you.");
		
		Story hide=new Story("(Requires Doctor) Outwit the dinosaur", string.toString());
		story.addEdge(start1, hide, 1);
		story.addEdge(start2, hide, 1);
		
		string.delete(0, string.length());
		string.append("The dinosaur is bigger, stronger, and tougher than you, but the one thing you have over it is your ");
		string.append("speed. You may no longer be a Marine, but it was hard to completely break the habit of ");
		string.append("exercising regularly, and you’re still in good shape.<br><br>");
		string.append("Knowing that you might die if you don’t, you push yourself to run faster than you ever have in ");
		string.append("your life, barely even feeling tired due to the adrenaline.");
		string.append("The bad news is that the dinosaur doesn’t seem to be tiring at all. The good news is that you’re ");
		string.append("finally nearing the end of the path, and can see the stone arches in sight. You brace yourself for ");
		string.append("one final burst of speed, hoping you can reach the control tower and shelter inside of it before the ");
		string.append("dinosaur can catch you.");

		Story run=new Story("(Requires Lawyer) Outrun the dinosaur", string.toString());
		story.addEdge(start1, run, 2);
		story.addEdge(start2, run, 2);
		
		//reunite with macomb alone
		string.delete(0, string.length());
		string.append("You steel yourself, and run as fast as you can through those stone arches, not daring to look back ");
		string.append("as the dinosaur thunders after you, growling and letting out a roar that’s loud enough to almost ");
		string.append("make you flinch.<br><br>");
		string.append("On the other side of the arches is Macomb, impressively still alive and looking uninjured, but in ");
		string.append("front of him is a dinosaur anyone would recognise on sight: a stegosaurus. It’s almost the exact ");
		string.append("same size as the creature chasing you, though it walks on all fours. It has two rows of bony ");
		string.append("spikes running down its back, and has four long spikes attached to its tail’s end. Unlike the other ");
		string.append("dinosaurs you’ve encountered, it doesn’t appear aggressive at all, munching on a cycad. ");
		string.append("However, its long body is completely blocking the control tower’s entrance.<br><br>");
		string.append("When Macomb sees you running over, he grins and shouts “You’re alive!” However, his grin ");
		string.append("dies the moment he sees you're alone.<br><br>");
		string.append("“We have to hide!” you yell at him, but the one safe place around here is currently inaccessible, ");
		string.append("blocked off by the stegosaurus, whose back spikes go erect when it notices your approach. It ");
		string.append("stops eating and turns in your direction, looking like it’s preparing for a fight. You seem to be ");
		string.append("stuck in a difficult spot.");
		
		Story sprint=new Story("Run as fast as you can", string.toString());
		story.addEdge(hide, sprint, 1);
		story.addEdge(run, sprint, 1);
		
		string.delete(0, string.length());
		string.append("You already know that you’re being chased by an allosaurus, which is a carnivore and apex ");
		string.append("predator. The stegosaurus is a herbivore. Both are easily capable of killing a human, but under ");
		string.append("completely different conditions.<br><br>");
		string.append("“Follow me!” you shout, and run towards the stegosaurus. You know that its erect spikes are a ");
		string.append("sign of threat display, but you're not the threat it’s focused on. It’s not looking at you, but the ");
		string.append("dinosaur behind you; you know the situation isn’t quite as bad as it seems.<br><br>");
		string.append("You run towards the stegosaurus, making sure to take the long way around ");
		string.append("and stay a good distance back, not wanting to risk provoking it, or getting within range of its tail.");
		string.append("<br><br>When Macomb sees the stegosaurus practically ignoring you, he follows, ");
		string.append("scrambling to get out of the allosaurus’s way and join you in safety. You catch your ");
		string.append("breath while Macomb silently panics. The two of you end up hiding ");
		string.append("behind the control tower, instead of inside of it as planned.<br><br>");
		string.append("The allosaurus finally stops when it sees that the stegosaurus is now between you and it, and the ");
		string.append("two dinosaurs stare at each other tensely before the allosaurus suddenly attacks.");
		
		Story think=new Story("(Requires Doctor) Make a decision", string.toString());
		story.addEdge(sprint, think, 1);
		
		string.delete(0, string.length());
		string.append("“What do I do?!” you shout to Macomb. You don’t stop ");
		string.append("running, but are rapidly running out of ground and breath.<br><br>");
		string.append("“Uh...behind the stegosaurus!” Macomb shouts back, uncertain of his plan, but certain that a herbivore ");
		string.append("will be less likely to eat you than a carnivore. He takes a wide berth around the stegosaurus, ");
		string.append("very concerned about it accidentally attacking him, and you follow. Surprisingly, it ");
		string.append("doesn’t even seem to care, too busy staring down the other dinosaur.<br><br>");
		string.append("You nearly collapse from exhaustion, your legs like jelly, while ");
		string.append("Macomb silently panics. The two of ");
		string.append("you end up hiding behind the control tower, instead of inside of it as planned.<br><br>");
		string.append("The dinosaur that’d been chasing you finally stops when it sees that the stegosaurus is now between ");
		string.append("you and it, and the two dinosaurs stare at each other tensely before the stegosaurus is suddenly attacked.");

		Story cry=new Story("(Requires Lawyer) Make a decision", string.toString());
		story.addEdge(sprint, cry, 2);
		
		string.delete(0, string.length());
		string.append("The dinosaur that’d been chasing you, an <a href=\"allo.html\">allosaurus</a>, charges at the stegosaurus, and ");
		string.append("the stegosaurus reacts in an instant. It turns to the side and lashes out with its thagomizer, its spiked ");
		string.append("tail, which moves almost in a blur. You’re glad you’ve taken cover behind the tower, because the ");
		string.append("swing is wide and the stegosaurus doesn’t seem to care about anything other than its fight for its ");
		string.append("life. The thagomizer’s spikes sink into the allosaurus’s side, and come out covered in blood.<br><br>");
		string.append("The allosaurus bellows but doesn’t stop, only staggering a little before thundering forth and ");
		string.append("extending its open mouth. The stegosaurus turns further to the side in a defensive move, ");
		string.append("exposing its side to protect its head, but it moves slowly, shuffling its feet. The allosaurus sinks ");
		string.append("its teeth into the upper part of the stegosaurus’s flank and tears out a bloody chunk, making the ");
		string.append("wound gush blood while the stegosaurus hisses in pain.<br><br>");
		string.append("While the allosaurus is swallowing the meat, the stegosaurus lashes out with its blood soaked ");
		string.append("thagomizer again, this time hitting the allosaurus higher, in its neck. The allosaurus makes a ");
		string.append("gurgly roar, its throat already filling with blood. Both dinosaurs are badly injured, but neither ");
		string.append("stops attacking the other.<br><br>");
		string.append("The allosaurus takes another bite from the stegosaurus in the same spot, deepening its already ");
		string.append("large bite wound. Based on the massive amount of blood dripping from the wound, the ");
		string.append("allosaurus must have gotten several vital organs. The stegosaurus lets out a weak hiss but can’t ");
		string.append("even swing its thagomizer again before collapsing, dead.<br><br>");
		string.append("The allosaurus feasts on its corpse, hungrily taking messy bites out of its sides and swallowing ");
		string.append("the bloody chunks of meat before going for more. However, it’s still wounded, and its neck ");
		string.append("continues to hemorrhage blood while it eats. The allosaurus doesn’t seem to care, but its greedy ");
		string.append("feeding noticeably slows down. Its movements gradually become more sluggish, and eventually ");
		string.append("it falls over with a loud thud, dead from the blood loss.");
		
		Story fight=new Story("Continue", string.toString());
		story.addEdge(think, fight, 1);
		story.addEdge(cry, fight, 1); 
		
		//macomb mourn alex and muldrew
		StringBuilder prime=new StringBuilder(600);
		prime.append("<br><br>Macomb looks at you again, having instantly noticed the fact that you’re completely alone. ");
		prime.append("“Neither of them made it?” he asks, his voice already strained at the thought.<br><br>");
		prime.append("“No, they didn’t,” you say, standing there awkwardly. You know it’s not a good idea to mention ");
		prime.append("that you killed Alex to ensure your own survival.<br><br>");
		prime.append("Macomb’s eyes immediately start to fill up with tears. “What…happened? Wait, never mind. I ");
		prime.append("don’t want to hear it right now. I can’t–” he’s too choked up with emotion to finish.<br><br>");
		prime.append("You fall into uncomfortable silence, watching as Macomb turns his back and quietly sobs.<br><br>");
		prime.append("Suddenly, the quiet is broken by the sound of the door to the control tower creaking open. Both ");
		prime.append("of your heads snap over to see Neary emerging.");

		string.delete(0, string.length());
		string.append("“That was…amazing,” you say, staring in awe at the two dinosaur corpses. You are one of the ");
		string.append("only humans to ever witness two dinosaurs fight, a sight better than any nature documentary.");

		Story amaze=new Story("React with awe", string.toString()+prime.toString());
		story.addEdge(fight, amaze, 1);

		string.delete(0, string.length());
		string.append("“That was…terrifying,” you admit, staring at the two dinosaur corpses, one of which had been ");
		string.append("trying to eat you just minutes ago. Your heart is still pounding from the experience.");

		Story terror=new Story("React with shock", string.toString()+prime.toString());
		story.addEdge(fight, terror, 2);

		string.delete(0, string.length());
		string.append("You don’t say anything. You can’t really say anything in the aftermath of such an insane experience.");

		Story stoic=new Story("Don't react", string.toString()+prime.toString());
		story.addEdge(fight, stoic, 3);
		
		string.delete(0, string.length());
		string.append("Neary looks like he can’t believe what he’s seeing, staring at the two of you like he’s looking at a ");
		string.append("ghost. He quickly figures out what happened, given the shattered look on Macomb’s face, and ");
		string.append("the absence of Muldrew and Alex.<br><br>");
		string.append("“Neary, where were you?!” Muldrew asks him, his voice a mixture of grief, bewilderment, and ");
		string.append("annoyance. “You couldn’t have warned us about the blackout?”<br><br>");
		string.append("“I-I, uh…” Neary can’t even respond, just sputters, now looking very guilty.<br><br>");
		string.append("“Wait, Neary knew about it?” you ask him.<br><br>");
		string.append("“Neary caused it!” Macomb says, clearly agitated, but suddenly looks guilty too. “Well, I might ");
		string.append("have too, a little bit, maybe…”<br><br>");
		string.append("Macomb trails off but suddenly notices something in the sky, pointing it out. “Hammett’s coming!”");
		string.append("<br><br>All of you turn to look and see a jet on the horizon, approaching the island.");

		Story neary=new Story("“Neary?!”", string.toString()); 
		story.addEdge(amaze, neary, 1); 
		story.addEdge(terror, neary, 1);
		story.addEdge(stoic, neary, 1);
		
		return neary;
	}
	
	private static void MakeGoodEnding(Story start1, Story start2) //everyone lives
	{
		//meet hammett
		StringBuilder string=new StringBuilder(600); 
		string.append("“I’ll get the landing pad ready,” Macomb says, focusing on doing his job. He opens the control ");
		string.append("tower’s door and heads inside, stepping over the stegosaurus corpse that’s still in front of it.<br><br>");
		string.append("Muldrew turns to Neary again, looking more serious than usual. “Neary, I need an explanation,” ");
		string.append("he says. Macomb didn’t close the control tower’s door, so he can still hear everything.<br><br>");
		string.append("Neary flushes and can’t even make eye contact, but does so. “I’m sorry, okay? I…I was going to ");
		string.append("put Macomb’s guitar back, and I tripped over his extension cord. I hit the generator, and the ");
		string.append("power went out.” He sounds reluctant to admit this, clearly embarrassed and guilty, but his ");
		string.append("explanation seems light on details. He almost seems like he’s hiding something, hoping that if he ");
		string.append("doesn’t bring it up, no one will know about it.<br><br>");
		string.append("“What were you doing in the control tower then?” Muldrew says, his eyes narrowed. He’s fully ");
		string.append("aware that Neary has been in the substation dozens of times and had never tripped until today, ");
		string.append("but doesn’t mention it yet. <br><br>");
		string.append("“Uh, I just knew it would be safe. I’m sorry I ran, I-I just panicked,” Neary says, looking ");
		string.append("genuinely remorseful. “I…I fell asleep, then I heard a thud outside, and found you guys…” He ");
		string.append("shifts around from discomfort. <br><br>");
		string.append("“Everyone, move out of the way! Hammett’s going to land,” Macomb interrupts, shouting from ");
		string.append("inside the tower.<br><br>");
		string.append("The landing strip starts to extend from the edge of the island, and you can hear Hammett’s jet ");
		string.append("now that it’s closer, loud enough to drown out conversation. You, Neary, Muldrew, and Alex all ");
		string.append("back up and watch as the jet approaches the island.");

		Story landG=new Story("Continue", string.toString());
		story.addEdge(start1, landG, 1);
		story.addEdge(start2, landG, 1);
		
		string.delete(0, string.length());
		string.append("The jet descends onto the landing pad, as elegantly as it did this morning, and slows to a stop. ");
		string.append("After a moment, the door opens and Hammett steps out. He looks exactly like the photos you’ve ");
		string.append("seen. His hair is white and his skin is wrinkled with age, yet his face seems to have gotten harder, ");
		string.append("rather than softer, with his age. He’s wearing his signature blue suit and walks slowly, not out of ");
		string.append("frailty, but out of confidence; he’s in no hurry because he knows everyone here answers to him. ");
		string.append("Hammett is flanked by a man who’s clearly his bodyguard: tall, muscular, and armed with what ");
		string.append("looks like two handguns and a shotgun. <br><br>");
		string.append("Once Hammett exits the jet, his piercing blue eyes scan the scene in front of him, noting the ");
		string.append("looks on all your faces. When he speaks, his voice is surprisingly strong, almost booming. “What ");
		string.append("is the meaning of this?” he thunders. <br><br>");
		string.append("“Sir, there was an…accident. The power went out and the dinosaurs escaped. We did our best to ");
		string.append("survive,” Muldrew immediately responds, almost standing at attention. Hammett’s gaze is cold ");
		string.append("with fury, which makes sense considering that the two dead dinosaurs in front of him ");
		string.append("probably each cost him billions of dollars to make.<br><br>");
		string.append("“You’re my head of security, Muldrew. I don’t pay you to have ‘accidents’. What happened?” ");
		string.append("Hammett almost looks like a predator waiting to pounce, trying to figure out who to blame for this.");

		Story meetG=new Story("Meet Hammett", string.toString());
		story.addEdge(landG, meetG, 1);
		
		string.delete(0, string.length());
		string.append("Muldrew glances at Neary, whose embarrassment has suddenly hardened into an almost defiant ");
		string.append("kind of bravery. “The accident happened because of me. I tripped in the substation and damaged ");
		string.append("the generator,” Neary says, still appearing guilty and not bothering to hide it from Hammett.<br><br>");
		string.append("Hammett’s eyes narrow dangerously, but before he can speak, Macomb interjects. “No, it was ");
		string.append("my fault,” he says, sounding guilty and looking worried for Neary. “I didn’t listen to Neary’s ");
		string.append("warnings. He tripped over my stuff.” <br><br>");
		string.append("Muldrew interrupts Macomb to tell Hammett “I take full responsibility, sir. You put me in charge ");
		string.append("of the island’s security, and I failed. I should’ve ensured the generator was better protected.” He ");
		string.append("looks like he believes what he’s saying. <br><br>");
		string.append("“Enough,” Hammett barks. “You will each write me a report about the incident by tomorrow. I ");
		string.append("will determine who’s at fault, and punish him accordingly.” The three of them nod ");
		string.append("simultaneously. Macomb looks relieved, Neary looks unusually reserved, and Muldrew looks ");
		string.append("like he’s already mentally getting started.<br><br>");
		string.append("Hammett turns to face you, his eyes free of anger but no less cold, though he at least manages to ");
		string.append("sound polite. “You have my sincere apologies for the day’s events. I assure you, this…chaos was ");
		string.append("in no way representative of the quality of Jurassic Preserve or NextGen as a company. We pride ");
		string.append("ourselves on our prudence as much as our innovation. The completed version will have much ");
		string.append("stronger safeguards.” ");

		Story punishG=new Story("Continue", string.toString());
		story.addEdge(meetG, punishG, 1);
		
		//tell hammett opinion of park
		string.delete(0, string.length());
		string.append("“I hope so, because this park has a lot of potential,” you tell Hammett. His eyes briefly flash with ");
		string.append("what looks like satisfaction. <br><br>");
		string.append("“You have a strong setup here. I’m not too experienced with animal rights or environmental law, ");
		string.append("but the dinosaurs seemed healthy enough, and the island’s ecosystem looks stable. And I assume you ");
		string.append("have approval from the Mayaqutan government?” you say, Mayaquta being the developing Latin ");
		string.append("American country that Hammett bought the island from.<br><br>");
		string.append("“Of course. The Jurassic Preserve’s construction was done through all the proper legal channels,” ");
		string.append("Hammett says coolly. “I simply requested they remain silent about its purpose, as I wish to ");
		string.append("preserve the surprise.” <br><br>");
		string.append("“If you have their full consent, then your legal case should be airtight once the safety issues are ");
		string.append("handled. Your only concerns should be protests and lawsuits, and I’m sure your lawyers can handle that,” ");
		string.append("you tell him. “As far as I’m concerned, this park is a good idea.”");

		Story lawLikeG=new Story("(Requires Lawyer) Approve of the park", string.toString());
		story.addEdge(punishG, lawLikeG, 3);
		
		string.delete(0, string.length());
		string.append("“I don’t think that matters, because this park is a disaster waiting to happen,” you tell Hammett. ");
		string.append("His eyes briefly flash with what looks like irritation.<br><br>");
		string.append("“I’m not too experienced with animal rights or environmental law, but I sincerely doubt you can ");
		string.append("privately own the sole existing members of multiple endangered—technically still functionally ");
		string.append("extinct—species,” you begin. <br><br>");
		string.append("“The Jurassic Preserve’s construction was done through all the proper legal channels,” Hammett ");
		string.append("says coolly. “The Mayaqutan government has full awareness of the project, and has given their ");
		string.append("full consent,” he explains, Mayaquta being the developing Latin American government he ");
		string.append("bought the island from. <br><br>");
		string.append("“Even so, the safety risks are absurd. I almost died today, more than once. If even one dinosaur ");
		string.append("got loose after the park opened, it could kill dozens of people. The risk is unacceptable and ");
		string.append("uninsurable. And that’s not even mentioning the lawsuits you’ll be drowning in from animal ");
		string.append("rights groups,” you tell him. “As far as I’m concerned, this park is a terrible idea.”");

		Story lawHateG=new Story("(Requires Lawyer) Disapprove of the park", string.toString());
		story.addEdge(punishG, lawHateG, 4);
		
		string.delete(0, string.length());
		string.append("“I hope so, because this park has a lot of potential,” you tell Hammett. His eyes briefly flash with ");
		string.append("what looks like satisfaction. <br><br>");
		string.append("“The dinosaurs all looked to be impressively well-adapted to the modern atmosphere. They all seem ");
		string.append("healthy and well-fed. It’s amazing that you managed to bring them back without any defects,” you begin.");
		string.append("<br><br>“All thanks to the efforts of our head scientist,” Hammett says coolly. “He’s a very quick learner, ");
		string.append("and managed to reconstruct the genetic code himself.” Neary stands up a little straighter when ");
		string.append("mentioned, looking proud. <br><br>");
		string.append("You nod at Neary, able to respect a good scientific accomplishment. “It’s not just that. The entire ");
		string.append("island, you managed to authentically recreate the Jurassic, even the temperature somehow. This ");
		string.append("is an amazing opportunity for science, to be able to study the life of this period,” you tell him. ");
		string.append("“As far as I’m concerned, this park is a great idea.”");

		Story docLikeG=new Story("(Requires Doctor) Approve of the park", string.toString());
		story.addEdge(punishG, docLikeG, 1);
		
		string.delete(0, string.length());
		string.append("“I don’t think that matters, because this park is a disaster waiting to happen,” you tell Hammett. ");
		string.append("His eyes briefly flash with what looks like irritation. <br><br>");
		string.append("“I don’t know how you managed to create dinosaurs without deformities, let alone keep them in ");
		string.append("perfect health, but I sincerely doubt they’ll stay that way for long. When they do, how are you ");
		string.append("going to treat them? Where will you find a veterinarian for dinosaurs?” you begin.<br><br>");
		string.append("“All thanks to the efforts of our head scientist,” Hammett says coolly. “He’s a very quick learner, ");
		string.append("and managed to reconstruct the genetic code himself. He also monitors the dinosaurs’ health, and if ");
		string.append("anything did happen, I have full faith in his ability to handle it.” Neary stands up a little ");
		string.append("straighter when mentioned, looking proud. <br><br>");
		string.append("You nod at Neary, able to respect a good scientific accomplishment. “It’s not just that. This is ");
		string.append("completely unsustainable. If you keep cloning dinosaurs, how long before an error occurs? If you ");
		string.append("let them breed, how are you going to manage all the dinosaurs? Stegosauruses alone can live up ");
		string.append("to 100 years!” you finish. “As far as I’m concerned, this park is a terrible idea.”");

		Story docHateG=new Story("(Requires Doctor) Disapprove of the park", string.toString());
		story.addEdge(punishG, docHateG, 2);
		
		//job offer from hammett
		StringBuilder accept=new StringBuilder(400);
		accept.append("Hammett looks as if he knew you’d say that, and allows himself a small satisfied smile. “I’m ");
		accept.append("glad to hear you be so enthusiastic about the Jurassic Preserve. I’ve been meaning to hire some ");
		accept.append("new employees in preparation for its grand opening, and I think you would be a good fit,” he ");
		accept.append("says coolly, prompting surprised looks from Macomb and Neary, hopeful excitement from Alex, ");
		accept.append("and curiosity from Muldrew. <br><br>");
		
		StringBuilder decline=new StringBuilder(400);
		decline.append("Hammett looks as if he knew you’d say that, and is utterly unfazed. “I’m glad to hear that you ");
		decline.append("speak your mind so freely. There are many people who wouldn’t so much as curse in my ");
		decline.append("presence. But you seem much less affected by my status. I could use someone like you in my ");
		decline.append("employ,” he says coolly, prompting surprised looks from Macomb and Neary, hopeful excitement ");
		decline.append("from Alex, and curiosity from Muldrew. <br><br>");
		
		StringBuilder doc=new StringBuilder(600);
		doc.append("“I’m a paleontologist, not a biologist. I don’t see what use you could have for me,” you say, ");
		doc.append("noting that most of what you just mentioned exclusively applies to biologists.<br><br>");
		doc.append("“Of course you are, Dr. Sattler. But your reputation transcends your field. Your support would be ");
		doc.append("of great value to the Jurassic Preserve, and your defence of it would be a great help. You may ");
		doc.append("think of it as consulting work,” Hammett explains. <br><br>");
		doc.append("Before you can respond, he keeps going. “You would be greatly compensated, of course. As my ");
		doc.append("other employees can attest, I’m a very generous employer. Your monthly pay would be in the ");
		doc.append("low seven digits,” Hammett says calmly. Macomb, Muldrew, and Neary all nod when you look ");
		doc.append("at them, confirming that this is a normal salary offer from their boss.<br><br>");
		doc.append("“You would be subject to a non-disclosure agreement about more…sensitive aspects of the ");
		doc.append("Jurassic Preserve’s operations, but you would receive unrestricted access to its dinosaurs and all ");
		doc.append("the data Neary has collected on them,” Hammet finishes, knowing this would be the dream for ");
		doc.append("any paleontologist. <br><br>");
		doc.append("Hammett looks at you calmly, almost as if he already knows your answer, and asks “What do ");
		doc.append("you say?” Despite their attempts to be subtle, the others are all clearly interested in your answer too.");
		
		StringBuilder law=new StringBuilder(600);
		law.append("“I’m a prosecutor, not a defence attorney. I don’t see what use you could have for me,” you say, ");
		law.append("given that you literally just told Hammett he’s likely to get sued, not need to sue someone. <br><br>");
		law.append("“Of course you are, Mr. Brandt. But your reputation transcends your field. Your support would ");
		law.append("be of great value to the Jurassic Preserve, and your mere presence would surely protect from a ");
		law.append("great number of lawsuits. You may think of it as consulting work,” Hammett explains.<br><br>");
		law.append("Before you can respond, he keeps going. “You would be greatly compensated, of course. As my ");
		law.append("other employees can attest, I’m a very generous employer. Your monthly pay would be in the ");
		law.append("low seven digits,” Hammett says calmly. Macomb, Muldrew, and Neary all nod when you look ");
		law.append("at them, confirming that this is a normal salary offer from their boss.<br><br>");
		law.append("“You would be subject to a non-disclosure agreement about more…sensitive aspects of the ");
		law.append("Jurassic Preserve’s operations, but you would have the opportunity to participate in some ");
		law.append("very…unique cases. Scott Marzani has been a thorn in my side for far too long, and will no ");
		law.append("doubt attempt to sue me soon, in a pitiful attempt to inhibit the Jurassic Preserve’s profitability,” ");
		law.append("Hammet says. Oddly, Neary looks like he’s almost startled to hear the name of Marzani Global ");
		law.append("Corporation’s CEO.<br><br>");
		law.append("“I have my own lawsuits planned to cripple his meddlesome company, and I could use your ");
		law.append("assistance with their execution,” Hammett finishes. You, like the rest of the country, are aware of ");
		law.append("the rumours of espionage and sabotage that constantly swirl around the rival companies. ");
		law.append("Marzani’s lawyers are as good as Hammett’s, among the best in the nation. You know that any ");
		law.append("case against them would be hard-fought, a challenge that would make full use of your skills, and ");
		law.append("any victory, especially if it takes Marzani down as Hammett claims, would make you a living ");
		law.append("legal legend. <br><br>");
		law.append("Hammett looks at you calmly, almost as if he already knows your answer, and asks “What do ");
		law.append("you say?” Despite their attempts to be subtle, the others are all clearly interested in your answer too.");
		
		Story docYesG=new Story("Continue", accept.toString()+doc.toString());
		story.addEdge(docLikeG, docYesG, 1);
		
		Story docNoG=new Story("Continue", decline.toString()+doc.toString());
		story.addEdge(docHateG, docNoG, 1);
		
		Story lawYesG=new Story("Continue", accept.toString()+law.toString());
		story.addEdge(lawLikeG, lawYesG, 1);
		
		Story lawNoG=new Story("Continue", decline.toString()+law.toString());
		story.addEdge(lawHateG, lawNoG, 1);
		
		//leave island
		StringBuilder prime=new StringBuilder(600);
		prime.append("<br><br>Hammett walks back into his jet, expecting you all to follow. He doesn’t seem to care about the ");
		prime.append("dinosaurs still on the loose, probably deciding to deal with it later. Alex runs over to walk by his ");
		prime.append("side, and the rest of you silently follow, getting into the back of the jet while Alex and Hammett ");
		prime.append("sit up front with Hammett's bodyguard. ");
		
		string.delete(0, string.length());
		string.append("“Alright, I’ll take the job,” you tell him, meeting his gaze in an attempt to get used to your new ");
		string.append("boss’s piercing stare. <br><br>");
		string.append("Muldrew isn’t surprised, but all three of the others look varying degrees of excited to have someone new ");
		string.append("on the island, especially since it’s someone they already know and like. Hammett is unfazed, calmly ");
		string.append("saying “Very well. We can discuss the details once we’re off the island.”");
		
		Story hiredG=new Story("Accept", string.toString()+prime.toString());
		story.addEdge(docYesG, hiredG, 1);
		story.addEdge(docNoG, hiredG, 1);
		story.addEdge(lawYesG, hiredG, 1);
		story.addEdge(lawNoG, hiredG, 1);
		
		string.delete(0, string.length());
		string.append("“I’m not interested in working for you, Hammett,” you tell him, meeting his gaze without ");
		string.append("difficulty, as you feel firmly about your decision. <br><br>");
		string.append("Muldrew isn’t surprised, but Macomb and Alex look disappointed, since they'd already started to like you. ");
		string.append("Neary looks like he actually respects your choice. Hammett is unfazed, calmly saying “Very well. My offer ");
		string.append("still stands, should you change your mind.”");
		
		Story firedG=new Story("Decline", string.toString()+prime.toString());
		story.addEdge(docYesG, firedG, 2);
		story.addEdge(docNoG, firedG, 2);
		story.addEdge(lawYesG, firedG, 2);
		story.addEdge(lawNoG, firedG, 2);
		
		//the end
		string.delete(0, string.length());
		string.append("Everyone seems more relaxed now that you’re leaving the dinosaurs behind, the atmosphere is ");
		string.append("surprisingly light. Hammett is giving Alex a hug while Macomb, Muldrew, and Neary talk.<br><br>");
		string.append("“Neary, I can’t believe you slept through most of that. What did your coffee have in it, horse ");
		string.append("tranquilisers?” Muldrew asks dryly. <br><br>");
		string.append("“Shut up,” Neary mumbles, still embarrassed. “The tower’s at the edge of the island, okay? And ");
		string.append("the dinosaurs didn’t come there until the end.” <br><br>");
		string.append("Macomb laughs. “That’s not even the craziest part. I’m still shocked that I survived that, not to ");
		string.append("mention got the power back on. Not to brag, but I’m kind of the hero here. None of you guys ");
		string.append("would have made it out without me,” he says, clearly bragging.<br><br>");
		string.append("“Hero? You think this is one of those dumb action movies you and Neary like to watch?” Muldrew asks him.");
		string.append("<br><br>“Hey, those movies are good. And yeah, why not? Someone should make a movie out of this. An ");
		string.append("isolated tropical island, rampaging dinosaurs, and a snarky but lovable hero with dashing good ");
		string.append("looks,” Macomb says, only half joking. <br><br>");
		string.append("“Who would want to watch a movie about us?” you interject, the idea sounding absurd. “We ");
		string.append("were running for our lives and completely terrified. That wouldn’t be a very captivating film.”<br><br>");
		string.append("“How did you do it anyway?” Neary asks you, genuinely curious. “Survive, that is.”<br><br>");
		string.append("Macomb answers for you. “<a href=\"extinction.html\">Life, uh, finds a way</a>. And we all made it, didn’t we? ");
		string.append("Each in our own way.” He sounds more serious when he says this, grateful that everyone made it.<br><br>");
		string.append("And he’s right. At least in part due to you, everyone made it. When you reflect on the choices ");
		string.append("you made, you consistently chose the right ones (though you can’t help but wonder if you ");
		string.append("could’ve gotten to know the three men a little better). But at the end of the day, you all made it, ");
		string.append("thanks to your actions. Whatever happens next, you know you’ll all be able to face it together.");
		string.append("<br><br><strong>The End</strong>");
		
		Story completeG=new Story("Continue", string.toString());
		story.addEdge(hiredG, completeG, 1);
		story.addEdge(firedG, completeG, 1); 
	}
	
	private static void MakePowerEnding(Story start) //same as powerless, but without plane crash
	{
		//meet hammett
		StringBuilder string=new StringBuilder(600); 
		string.append("“I’ll get the landing pad ready,” Macomb says, focusing on doing his job. He opens the control ");
		string.append("tower’s door and heads inside, stepping over the stegosaurus corpse that’s still in front of it.<br><br>");
		string.append("Alex turns to Neary, actually looking serious. “Uncle Donnie, did you cause this?” he says. ");
		string.append("Macomb didn’t close the control tower’s door, so he can still hear everything.<br><br>");
		string.append("Neary flushes and can’t even make eye contact, but does so. “Yes…I’m sorry, okay? I…I was ");
		string.append("going to put Macomb’s guitar back, and I tripped over his extension cord. I hit the generator, and ");
		string.append("the power went out.” He sounds reluctant to admit this, clearly embarrassed and guilty, but his ");
		string.append("explanation seems light on details. He almost seems like he’s hiding something, hoping that if he ");
		string.append("doesn’t bring it up, no one will know about it.  <br><br>");
		string.append("“Why were you in the control tower then?” you ask, making no attempt to hide your curiosity. ");
		string.append("You remember that Macomb said he’d argued with Neary about tripping hazards nine times, and ");
		string.append("don’t understand how Neary only caused an accident today, but don’t bring that up yet.<br><br>");
		string.append("“Uh, I just knew it would be safe. I’m sorry I ran, I-I just panicked,” Neary says, looking ");
		string.append("genuinely remorseful. “I…I fell asleep, then I heard a thud outside, and found you guys…” He ");
		string.append("shifts around from discomfort.<br><br>");
		string.append("“Everyone, move out of the way! Hammett’s going to land,” Macomb interrupts, shouting from ");
		string.append("inside the tower.<br><br>");
		string.append("The landing strip starts to extend from the edge of the island, and you can hear Hammett’s jet ");
		string.append("now that it’s closer, loud enough to drown out conversation. You, Neary, and Alex all back up ");
		string.append("and watch as the jet approaches the island.");

		Story land=new Story("Continue", string.toString());
		story.addEdge(start, land, 1);
		
		string.delete(0, string.length());
		string.append("The jet descends onto the landing pad, as elegantly as it did this morning, and slows to a stop. ");
		string.append("After a moment, the door opens and Hammett steps out. He looks exactly like the photos you’ve ");
		string.append("seen. His hair is white and his skin is wrinkled with age, yet his face seems to have gotten harder, ");
		string.append("rather than softer, with his age. He’s wearing his signature blue suit and walks slowly, not out of ");
		string.append("frailty, but out of confidence; he’s in no hurry because he knows everyone here answers to him. ");
		string.append("Hammett is flanked by a man who’s clearly his bodyguard: tall, muscular, and armed with what ");
		string.append("looks like two handguns and a shotgun.  <br><br>");
		string.append("Once Hammett exits the jet, his piercing blue eyes scan the scene in front of him, noting the ");
		string.append("looks on all your faces. When he speaks, his voice is surprisingly strong, almost booming. “What ");
		string.append("is the meaning of this?” he thunders.<br><br>");
		string.append("“There was an accident. The power went out and the dinosaurs escaped. We…tried to survive,” ");
		string.append("Macomb quickly responds, looking more than a bit awkward. Hammett’s gaze is cold with fury, ");
		string.append("which makes sense considering that the two dead dinosaurs in front of him probably each cost ");
		string.append("him billions of dollars to make.<br><br>");
		string.append("“You’re my top engineer, Macomb. I don’t pay you to have ‘accidents’. What happened?” ");
		string.append("Hammett almost looks like a predator waiting to pounce, trying to figure out who to blame for this.");
		
		Story meet=new Story("Meet Hammett", string.toString());
		story.addEdge(land, meet, 1);
		
		string.delete(0, string.length());
		string.append("Macomb almost glances at Neary, but stops himself. “The generator was damaged…it was my ");
		string.append("fault. I was careless.”  <br><br>");
		string.append("Hammett’s eyes narrow dangerously, but before he can speak, Neary interjects, his ");
		string.append("embarrassment suddenly hardened into an almost defiant kind of bravery. “The accident ");
		string.append("happened because of me. I tripped in the substation and damaged the generator,” he says, still ");
		string.append("appearing guilty and not bothering to hide it from Hammett.<br><br>");
		string.append("“Muldrew should have secured the generator properly,” Hammett says calmly, almost too calmly. ");
		string.append("“It was his responsibility.”<br><br>");
		string.append("Macomb hesitates to respond, seeming awkward, but Alex speaks for him, voice quivering with");
		string.append("a fresh wave of sadness. “Grandpa, Uncle Roland is…gone…”<br><br>");
		string.append("Hammett’s eyes and tone actually soften when he responds to Alex. “I’m aware,” he says, having ");
		string.append("already noticed the dried blood on you. Then he turns his gaze back to Macomb, cold again. “Muldrew ");
		string.append("gave you safety regulations to follow. You should have listened to him.” Macomb looks pained ");
		string.append("to hear it, Hammett’s words sounding harsher given Muldrew’s death.<br><br>");
		string.append("Hammett also addresses Neary, saying “And clumsiness is not an excuse for such a gross level of ");
		string.append("incompetence.” Neary’s fists ball up, but he remains uncharacteristically reserved. “The both of ");
		string.append("you will be appropriately punished at a later time,” Hammett finishes.<br><br>");
		string.append("Hammett turns to face you, his eyes free of anger but no less cold, though he at least manages to ");
		string.append("sound polite. “You have my sincere apologies for the day’s events. I assure you, this…chaos was ");
		string.append("in no way representative of the quality of Jurassic Preserve or NextGen as a company. We pride ");
		string.append("ourselves on our prudence as much as our innovation. The completed version will have much ");
		string.append("stronger safeguards.”");

		Story punish=new Story("Continue", string.toString());
		story.addEdge(meet, punish, 1);
		
		//tell hammett opinion of park
		string.delete(0, string.length());
		string.append("“I hope so, because this park has a lot of potential,” you tell Hammett. His eyes briefly flash with ");
		string.append("what looks like satisfaction.  <br><br>");
		string.append("“You have a strong setup here. I’m not too experienced with animal rights or environmental law, ");
		string.append("but the dinosaurs seemed healthy enough, and the island’s ecosystem looks stable. And I assume you ");
		string.append("have approval from the Mayaqutan government?” you say, Mayaquta being the developing Latin ");
		string.append("American country that Hammett bought the island from.<br><br>");
		string.append("“Of course. The Jurassic Preserve’s construction was done through all the proper legal channels,” ");
		string.append("Hammett says coolly. “I simply requested they remain silent about its purpose, as I wish to ");
		string.append("preserve the surprise.”<br><br>");
		string.append("“If you have their full consent, then your legal case should be airtight once the safety issues are ");
		string.append("handled. Your only concerns should be protests and lawsuits, and I’m sure your lawyers can handle that,” ");
		string.append("you tell him. “As far as I’m concerned, this park is a good idea.”");

		Story lawLike=new Story("(Requires Lawyer) Approve of the park", string.toString());
		story.addEdge(punish, lawLike, 3);
		
		string.delete(0, string.length());
		string.append("“I don’t think that matters, because this park is a disaster waiting to happen,” you tell Hammett. ");
		string.append("His eyes briefly flash with what looks like irritation.<br><br>");
		string.append("“I’m not too experienced with animal rights or environmental law, but I sincerely doubt you can ");
		string.append("privately own the sole existing members of multiple endangered—technically still functionally ");
		string.append("extinct—species,” you begin.  <br><br>");
		string.append("“The Jurassic Preserve’s construction was done through all the proper legal channels,” Hammett ");
		string.append("says coolly. “The Mayaqutan government has full awareness of the project, and has given their ");
		string.append("full consent,” he explains, Mayaquta being the developing Latin American government he ");
		string.append("bought the island from.<br><br>");
		string.append("“Even so, the safety risks are absurd. I almost died today, more than once. If even one dinosaur ");
		string.append("got loose after the park opened, it could kill dozens of people. The risk is unacceptable and ");
		string.append("uninsurable. And that’s not even mentioning the lawsuits you’ll be drowning in from animal ");
		string.append("rights groups,” you tell him. “As far as I’m concerned, this park is a terrible idea.”");

		Story lawHate=new Story("(Requires Lawyer) Disapprove of the park", string.toString());
		story.addEdge(punish, lawHate, 4);
		
		string.delete(0, string.length());
		string.append("“I hope so, because this park has a lot of potential,” you tell Hammett. His eyes briefly flash with ");
		string.append("what looks like satisfaction.  <br><br>");
		string.append("“The dinosaurs all looked to be impressively well-adapted to the modern atmosphere. They all seem ");
		string.append("healthy and well-fed. It’s amazing that you managed to bring them back without any defects,” you begin.");
		string.append("<br><br>“All thanks to the efforts of our head scientist,” Hammett says coolly. “He’s a very quick learner, ");
		string.append("and managed to reconstruct the genetic code himself.” Neary stands up a little straighter when ");
		string.append("mentioned, looking proud.<br><br>");
		string.append("You nod at Neary, able to respect a good scientific accomplishment. “It’s not just that. The entire ");
		string.append("island, you managed to authentically recreate the Jurassic, even the temperature somehow. This ");
		string.append("is an amazing opportunity for science, to be able to study the life of this period,” you tell him. ");
		string.append("“As far as I’m concerned, this park is a great idea.”");

		Story docLike=new Story("(Requires Doctor) Approve of the park", string.toString());
		story.addEdge(punish, docLike, 1);
		
		string.delete(0, string.length());
		string.append("“I don’t think that matters, because this park is a disaster waiting to happen,” you tell Hammett. ");
		string.append("His eyes briefly flash with what looks like irritation.  <br><br>");
		string.append("“I don’t know how you managed to create dinosaurs without deformities, let alone keep them in ");
		string.append("perfect health, but I sincerely doubt they’ll stay that way for long. When they do, how are you ");
		string.append("going to treat them? Where will you find a veterinarian for dinosaurs?” you begin.<br><br>");
		string.append("“All thanks to the efforts of our head scientist,” Hammett says coolly. “He’s a very quick learner, ");
		string.append("and managed to reconstruct the genetic code himself. He also monitors the dinosaurs’ health, and if ");
		string.append("anything did happen, I have full faith in his ability to handle it.” Neary stands up a little ");
		string.append("straighter when mentioned, looking proud.<br><br>");
		string.append("You nod at Neary, able to respect a good scientific accomplishment. “It’s not just that. This is ");
		string.append("completely unsustainable. If you keep cloning dinosaurs, how long before an error occurs? If you ");
		string.append("let them breed, how are you going to manage all the dinosaurs? Stegosauruses alone can live up ");
		string.append("to 100 years!” you finish. “As far as I’m concerned, this park is a terrible idea.”");

		Story docHate=new Story("(Requires Doctor) Disapprove of the park", string.toString());
		story.addEdge(punish, docHate, 2);
		
		//job offer from hammett
		StringBuilder accept=new StringBuilder(400);
		accept.append("Hammett looks as if he knew you’d say that, and allows himself a small satisfied smile. “I’m ");
		accept.append("glad to hear you be so enthusiastic about the Jurassic Preserve. I’ve been meaning to hire some ");
		accept.append("new employees in preparation for its grand opening, and I think you would be a good fit,” he ");
		accept.append("says coolly, prompting surprised looks from Macomb and Neary, and hopeful excitement from Alex.  <br><br>");

		StringBuilder decline=new StringBuilder(400);
		decline.append("Hammett looks as if he knew you’d say that, and is utterly unfazed. “I’m glad to hear that you ");
		decline.append("speak your mind so freely. There are many people who wouldn’t so much as curse in my ");
		decline.append("presence. But you seem much less affected by my status. I could use someone like you in my ");
		decline.append("employ,” he says coolly, prompting surprised looks from Macomb and Neary, and hopeful excitement ");
		decline.append("from Alex.  <br><br>");

		StringBuilder doc=new StringBuilder(600);
		doc.append("“I’m a paleontologist, not a biologist. I don’t see what use you could have for me,” you say, ");
		doc.append("noting that most of what you just mentioned exclusively applies to biologists.<br><br>");
		doc.append("“Of course you are, Dr. Sattler. But your reputation transcends your field. Your support would be ");
		doc.append("of great value to the Jurassic Preserve, and your defence of it would be a great help. You may ");
		doc.append("think of it as consulting work,” Hammett explains.<br><br>");
		doc.append("Before you can respond, he keeps going. “You would be greatly compensated, of course. As my ");
		doc.append("other employees can attest, I’m a very generous employer. Your monthly pay would be in the ");
		doc.append("low seven digits,” Hammett says calmly. Macomb and Neary nod when you look ");
		doc.append("at them, confirming that this is a normal salary offer from their boss.  <br><br>");
		doc.append("“You would be subject to a non-disclosure agreement about more…sensitive aspects of the ");
		doc.append("Jurassic Preserve’s operations, but you would receive unrestricted access to its dinosaurs and all ");
		doc.append("the data Neary has collected on them,” Hammet finishes, knowing this would be the dream for ");
		doc.append("any paleontologist.<br><br>");
		doc.append("Hammett looks at you calmly, almost as if he already knows your answer, and asks “What do ");
		doc.append("you say?” Despite their attempts to be subtle, the others are all clearly interested in your answer too.");

		StringBuilder law=new StringBuilder(600);
		law.append("“I’m a prosecutor, not a defence attorney. I don’t see what use you could have for me,” you say, ");
		law.append("given that you literally just told Hammett he’s likely to get sued, not need to sue someone.  <br><br>");
		law.append("“Of course you are, Mr. Brandt. But your reputation transcends your field. Your support would ");
		law.append("be of great value to the Jurassic Preserve, and your mere presence would surely protect from a ");
		law.append("great number of lawsuits. You may think of it as consulting work,” Hammett explains.<br><br>");
		law.append("Before you can respond, he keeps going. “You would be greatly compensated, of course. As my ");
		law.append("other employees can attest, I’m a very generous employer. Your monthly pay would be in the ");
		law.append("low seven digits,” Hammett says calmly. Macomb and Neary nod when you look ");
		law.append("at them, confirming that this is a normal salary offer from their boss.<br><br>");
		law.append("“You would be subject to a non-disclosure agreement about more…sensitive aspects of the ");
		law.append("Jurassic Preserve’s operations, but you would have the opportunity to participate in some ");
		law.append("very…unique cases. Scott Marzani has been a thorn in my side for far too long, and will no ");
		law.append("doubt attempt to sue me soon, in a pitiful attempt to inhibit the Jurassic Preserve’s profitability,” ");
		law.append("Hammet says. Oddly, Neary looks like he’s almost startled to hear the name of Marzani Global ");
		law.append("Corporation’s CEO.<br><br>");
		law.append("“I have my own lawsuits planned to cripple his meddlesome company, and I could use your ");
		law.append("assistance with their execution,” Hammett finishes. You, like the rest of the country, are aware of ");
		law.append("the rumours of espionage and sabotage that constantly swirl around the rival companies. ");
		law.append("Marzani’s lawyers are as good as Hammett’s, among the best in the nation. You know that any ");
		law.append("case against them would be hard-fought, a challenge that would make full use of your skills, and ");
		law.append("any victory, especially if it takes Marzani down as Hammett claims, would make you a living ");
		law.append("legal legend.<br><br>");
		law.append("Hammett looks at you calmly, almost as if he already knows your answer, and asks “What do ");
		law.append("you say?” Despite their attempts to be subtle, the others are all clearly interested in your answer too.");

		Story docYes=new Story("Continue", accept.toString()+doc.toString());
		story.addEdge(docLike, docYes, 1);

		Story docNo=new Story("Continue", decline.toString()+doc.toString());
		story.addEdge(docHate, docNo, 1);

		Story lawYes=new Story("Continue", accept.toString()+law.toString());
		story.addEdge(lawLike, lawYes, 1);

		Story lawNo=new Story("Continue", decline.toString()+law.toString());
		story.addEdge(lawHate, lawNo, 1);
		
		//leave island
		StringBuilder prime=new StringBuilder(600);
		prime.append("<br><br>Hammett walks back into his jet, expecting you all to follow. He doesn’t seem to care about the ");
		prime.append("dinosaurs still on the loose, probably deciding to deal with it later. Alex runs over to walk by his ");
		prime.append("side, and the rest of you silently follow, getting into the back of the jet while Alex and Hammett ");
		prime.append("sit up front with Hammett's bodyguard.");
		
		string.delete(0, string.length());
		string.append("“Alright, I’ll take the job,” you tell him, meeting his gaze in an attempt to get used to your new ");
		string.append("boss’s piercing stare.  <br><br>");
		string.append("All three of the others look varying degrees of excited to have someone new ");
		string.append("on the island, especially since it’s someone they already know and like. Hammett is unfazed, calmly ");
		string.append("saying “Very well. We can discuss the details once we’re off the island.”");
		
		Story hired=new Story("Accept", string.toString()+prime.toString());
		story.addEdge(docYes, hired, 1);
		story.addEdge(docNo, hired, 1);
		story.addEdge(lawYes, hired, 1);
		story.addEdge(lawNo, hired, 1);
		
		string.delete(0, string.length());
		string.append("“I’m not interested in working for you, Hammett,” you tell him, meeting his gaze without ");
		string.append("difficulty, as you feel firmly about your decision.  <br><br>");
		string.append("Both Macomb and Alex look disappointed since they'd already started to like you, but Neary looks like ");
		string.append("he actually respects your choice. Hammett is unfazed, calmly saying “Very well. My offer still ");
		string.append("stands, should you change your mind.”");
		
		Story fired=new Story("Decline", string.toString()+prime.toString());
		story.addEdge(docYes, fired, 2);
		story.addEdge(docNo, fired, 2);
		story.addEdge(lawYes, fired, 2);
		story.addEdge(lawNo, fired, 2);
		
		//the end
		string.delete(0, string.length());
		string.append("Everyone seems more relaxed now that you’re leaving the dinosaurs behind, but Muldrew's absence is ");
		string.append("almost an elephant in the room. Macomb and Neary are both quiet, Macomb looking down at the ocean, ");
		string.append("almost despondent, and Neary shifting around uncomfortably, probably worrying about the ");
		string.append("punishment Hammett promised to give him. However, Hammett doesn’t even seem affected, ");
		string.append("though he is giving Alex a hug. No one is talking or crying, everyone looking lost in their own world.  ");
		string.append("<br><br>As for you, you watch as the island slowly fades from view, reflecting on the choices you made. ");
		string.append("You wonder if you could have prevented Muldrew's death somehow, if you’d just made a different ");
		string.append("decision. You wonder if you could have gotten to know him better before he died, if you could have made ");
		string.append("more of your time on the island. But at the end of the day, you still managed to survive three ");
		string.append("rampaging dinosaurs and leave the Jurassic Preserve alive. Isn’t that all that really matters?");
		string.append("<br><br><strong>The End</strong>");

		Story complete=new Story("Continue", string.toString());
		story.addEdge(hired, complete, 1);
		story.addEdge(fired, complete, 1);
	}

	private static void MakePowerlessEnding(Story start) //hammett's jet crashes without macomb alive
	{
		//meet hammett
		StringBuilder string=new StringBuilder(600);
		string.append("Muldrew curses. “The power’s still out. We can’t roll out the landing pad.”<br><br>");
		string.append("Alex looks worried now. “How will Grandpa land then?” Neary’s eyes flicker with more guilt at Alex’s words.");
		string.append("<br><br>“We need to clear the area,” Muldrew says firmly, gesturing to the stegosaurus and allosaurus ");
		string.append("corpses on the ground. “There aren’t many trees here. Amber might be able to land even without ");
		string.append("the pad.” You deduce Amber is the pilot’s name, probably the same woman who flew you here this morning.");
		string.append("<br><br>You, Muldrew, and Neary all work on moving away as much of the corpses as possible. Alex ");
		string.append("joins too, but naturally can’t contribute much. The stegosaurus is much lighter due to being half ");
		string.append("eaten, but is already to the side. It’s the allosaurus that’s blocking the way. Both are massive even ");
		string.append("in death, and each one is made of thousands of pounds of still-fresh meat. You all do what you ");
		string.append("can to move the allosaurus, straining to pull on its tail, but it's heavy even in death and you can ");
		string.append("only move it a few inches at most.<br> <br>");
		string.append("You can hear Hammett’s jet as it flies closer, loud enough to drown out conversation. You, ");
		string.append("Muldrew, Alex, and Neary back up and take cover behind the control tower. You all watch as the ");
		string.append("jet approaches the island, unable to do anything more.<br><br>");
		string.append("It seems Amber already noticed the lack of a landing pad and has prepared herself for a crash ");
		string.append("landing. The jet lowers gradually and sets down, its wheels already sinking into the ground and ");
		string.append("spraying dirt everywhere. Its right wing narrow avoids slamming into the control tower, but the ");
		string.append("jet can’t avoid the allosaurus corpse.<br><br>");
		string.append("The jet manages to roll over the corpse with a big bump, caving in the allosaurus’s ribs, but ");
		string.append("Amber stays on course. The jet still hasn’t finished decelerating and keeps going, moving ");
		string.append("towards the stone arches that frame the park’s entrance. It crashes through them, the jet way too ");
		string.append("wide to fit through. The arches crack and fall on top of the jet, broken in half. The jet’s wings are ");
		string.append("damaged by the impact, forming gouges in them, though not deep enough to rupture the fuel tanks.");
		string.append("<br><br>The jet keeps rolling, the impact helping to slow it down slightly, and plows into the path’s trees, ");
		string.append("knocking many of them over. The wings finally break and fall off, but the repeated impacts are ");
		string.append("enough to kill the jet’s momentum, and it finally comes to a halt, leaving a trail of torn up dirt ");
		string.append("and flattened trees in its wake, but still intact.");

		Story land=new Story("Continue", string.toString());
		story.addEdge(start, land, 1);
		
		string.delete(0, string.length());
		string.append("You all look at each other, then Muldrew starts running over to the jet. You, Neary, and Alex ");
		string.append("follow, trying to keep up.<br> <br>");
		string.append("Jogging, you reach the jet in only a few minutes, and find the door already open, and Hammett ");
		string.append("steps out. He looks exactly like the photos you’ve seen. His hair is white and his skin is wrinkled ");
		string.append("with age, yet his face seems to have gotten harder, rather than softer, with his age. He’s wearing ");
		string.append("his signature blue suit and walks slowly, not out of frailty, but out of confidence; he’s in no hurry ");
		string.append("because he knows everyone here answers to him.<br><br>");
		string.append("Hammett is flanked by a man who’s clearly his bodyguard: tall, muscular, and armed with what ");
		string.append("looks like two handguns and a shotgun. A moment later, they’re followed by a woman dressed ");
		string.append("like a pilot, the same one from this morning. The three of them are all uninjured, and all seem ");
		string.append("completely unfazed by the plane crash.<br><br>");
		string.append("Once Hammett exits the jet, his piercing blue eyes scan the scene in front of him, noting the ");
		string.append("looks on all your faces. When he speaks, his voice is surprisingly strong, almost booming. “What ");
		string.append("is the meaning of this?” he thunders.<br><br>");
		string.append("“Sir, there was an…accident. The power went out and the dinosaurs escaped. We did our best to ");
		string.append("survive,” Muldrew immediately responds, almost standing at attention. Hammett’s gaze is cold ");
		string.append("with fury, which makes sense considering that the two dead dinosaurs his jet ran over ");
		string.append("probably each cost him billions of dollars to make.<br><br>");
		string.append("“You’re my head of security, Muldrew. I don’t pay you to have ‘accidents’. What happened?” ");
		string.append("Hammett almost looks like a predator waiting to pounce, trying to figure out who to blame for this.");

		Story meet=new Story("Meet Hammett", string.toString());
		story.addEdge(land, meet, 1);
		
		string.delete(0, string.length());
		string.append("Muldrew glances at you, since you were the only one who knows what happened at the ");
		string.append("substation. “There was…damage to the generator,” you tell Hammett. You didn’t expect to be ");
		string.append("put on the spot, and haven’t decided how much to reveal yet, whether to blame Macomb or Neary.");
		string.append("<br><br>Neary makes it a moot point, as his embarrassment has suddenly hardened into an almost defiant ");
		string.append("kind of bravery. “The accident happened because of me. I tripped in the substation and damaged ");
		string.append("the generator,” he says, still appearing guilty and not bothering to hide it from Hammett.<br><br>");
		string.append("Hammett’s eyes narrow dangerously, but before he can speak, Muldrew interrupts, saying “I take ");
		string.append("full responsibility, sir. You put me in charge of the island’s security, and I failed. I should’ve ");
		string.append("ensured the generator was better protected.” He looks like he believes what he’s saying.<br><br>");
		string.append("“Macomb should have secured the generator properly,” Hammett says calmly, almost too calmly. ");
		string.append("“It was his responsibility.”<br> <br>");
		string.append("Muldrew hesitates to respond, almost seeming awkward, but Alex speaks for him, voice ");
		string.append("quivering with a fresh wave of sadness. “Grandpa, Uncle Ivan is…gone…”<br><br>");
		string.append("Hammett’s eyes and tone actually soften when he responds to Alex. “I’m aware,” he says, having ");
		string.append("already noticed the dried blood on you. Then he turns his gaze back to Muldrew, cold again. ");
		string.append("“The generator was Macomb’s responsibility, but Macomb was yours.” Muldrew looks pained to ");
		string.append("hear it, Hammett’s words sounding harsher given Macomb’s death.<br><br>");
		string.append("Hammett also addresses Neary, saying “And clumsiness is not an excuse for such a gross level of ");
		string.append("incompetence.” Neary’s fists ball up, but he remains uncharacteristically reserved. “The both of ");
		string.append("you will be appropriately punished at a later time,” Hammett finishes.<br><br>");
		string.append("Hammett turns to face you, his eyes free of anger but no less cold, though he at least manages to ");
		string.append("sound polite. “You have my sincere apologies for the day’s events. I assure you, this…chaos was ");
		string.append("in no way representative of the quality of Jurassic Preserve or NextGen as a company. We pride ");
		string.append("ourselves on our prudence as much as our innovation. The completed version will have much ");
		string.append("stronger safeguards.”");

		Story punish=new Story("Continue", string.toString());
		story.addEdge(meet, punish, 1);
		
		//tell hammett opinion of park
		string.delete(0, string.length());
		string.append("“I hope so, because this park has a lot of potential,” you tell Hammett. His eyes briefly flash with ");
		string.append("what looks like satisfaction.<br> <br>");
		string.append("“You have a strong setup here. I’m not too experienced with animal rights or environmental law, ");
		string.append("but the dinosaurs seemed healthy enough, and the island’s ecosystem looks stable. And I assume you ");
		string.append("have approval from the Mayaqutan government?” you say, Mayaquta being the developing Latin ");
		string.append("American country that Hammett bought the island from.<br><br>");
		string.append("“Of course. The Jurassic Preserve’s construction was done through all the proper legal channels,” ");
		string.append("Hammett says coolly. “I simply requested they remain silent about its purpose, as I wish to ");
		string.append("preserve the surprise.”<br><br>");
		string.append("“If you have their full consent, then your legal case should be airtight once the safety issues are ");
		string.append("handled. Your only concerns should be protests and lawsuits, and I’m sure your lawyers can handle that,” ");
		string.append("you tell him. “As far as I’m concerned, this park is a good idea.”");

		Story lawLike=new Story("(Requires Lawyer) Approve of the park", string.toString());
		story.addEdge(punish, lawLike, 3);
		
		string.delete(0, string.length());
		string.append("“I don’t think that matters, because this park is a disaster waiting to happen,” you tell Hammett. ");
		string.append("His eyes briefly flash with what looks like irritation.<br><br>");
		string.append("“I’m not too experienced with animal rights or environmental law, but I sincerely doubt you can ");
		string.append("privately own the sole existing members of multiple endangered—technically still functionally ");
		string.append("extinct—species,” you begin.<br> <br>");
		string.append("“The Jurassic Preserve’s construction was done through all the proper legal channels,” Hammett ");
		string.append("says coolly. “The Mayaqutan government has full awareness of the project, and has given their ");
		string.append("full consent,” he explains, Mayaquta being the developing Latin American government he ");
		string.append("bought the island from.<br><br>");
		string.append("“Even so, the safety risks are absurd. I almost died today, more than once. If even one dinosaur ");
		string.append("got loose after the park opened, it could kill dozens of people. The risk is unacceptable and ");
		string.append("uninsurable. And that’s not even mentioning the lawsuits you’ll be drowning in from animal ");
		string.append("rights groups,” you tell him. “As far as I’m concerned, this park is a terrible idea.”");

		Story lawHate=new Story("(Requires Lawyer) Disapprove of the park", string.toString());
		story.addEdge(punish, lawHate, 4);
		
		string.delete(0, string.length());
		string.append("“I hope so, because this park has a lot of potential,” you tell Hammett. His eyes briefly flash with ");
		string.append("what looks like satisfaction.<br> <br>");
		string.append("“The dinosaurs all looked to be impressively well-adapted to the modern atmosphere. They all seem ");
		string.append("healthy and well-fed. It’s amazing that you managed to bring them back without any defects,” you begin.");
		string.append("<br><br>“All thanks to the efforts of our head scientist,” Hammett says coolly. “He’s a very quick learner, ");
		string.append("and managed to reconstruct the genetic code himself.” Neary stands up a little straighter when ");
		string.append("mentioned, looking proud.<br><br>");
		string.append("You nod at Neary, able to respect a good scientific accomplishment. “It’s not just that. The entire ");
		string.append("island, you managed to authentically recreate the Jurassic, even the temperature somehow. This ");
		string.append("is an amazing opportunity for science, to be able to study the life of this period,” you tell him. ");
		string.append("“As far as I’m concerned, this park is a great idea.”");

		Story docLike=new Story("(Requires Doctor) Approve of the park", string.toString());
		story.addEdge(punish, docLike, 1);
		
		string.delete(0, string.length());
		string.append("“I don’t think that matters, because this park is a disaster waiting to happen,” you tell Hammett. ");
		string.append("His eyes briefly flash with what looks like irritation.<br> <br>");
		string.append("“I don’t know how you managed to create dinosaurs without deformities, let alone keep them in ");
		string.append("perfect health, but I sincerely doubt they’ll stay that way for long. When they do, how are you ");
		string.append("going to treat them? Where will you find a veterinarian for dinosaurs?” you begin.<br><br>");
		string.append("“All thanks to the efforts of our head scientist,” Hammett says coolly. “He’s a very quick learner, ");
		string.append("and managed to reconstruct the genetic code himself. He also monitors the dinosaurs’ health, and if ");
		string.append("anything did happen, I have full faith in his ability to handle it.” Neary stands up a little ");
		string.append("straighter when mentioned, looking proud.<br><br>");
		string.append("You nod at Neary, able to respect a good scientific accomplishment. “It’s not just that. This is ");
		string.append("completely unsustainable. If you keep cloning dinosaurs, how long before an error occurs? If you ");
		string.append("let them breed, how are you going to manage all the dinosaurs? Stegosauruses alone can live up ");
		string.append("to 100 years!” you finish. “As far as I’m concerned, this park is a terrible idea.”");

		Story docHate=new Story("(Requires Doctor) Disapprove of the park", string.toString());
		story.addEdge(punish, docHate, 2);
		
		//job offer from hammett
		StringBuilder accept=new StringBuilder(400);
		accept.append("Hammett looks as if he knew you’d say that, and allows himself a small satisfied smile. “I’m ");
		accept.append("glad to hear you be so enthusiastic about the Jurassic Preserve. I’ve been meaning to hire some ");
		accept.append("new employees in preparation for its grand opening, and I think you would be a good fit,” he ");
		accept.append("says coolly, prompting a surprised look from Neary, hopeful excitement from Alex, ");
		accept.append("and curiosity from Muldrew.<br> <br>");
		
		StringBuilder decline=new StringBuilder(400);
		decline.append("Hammett looks as if he knew you’d say that, and is utterly unfazed. “I’m glad to hear that you ");
		decline.append("speak your mind so freely. There are many people who wouldn’t so much as curse in my ");
		decline.append("presence. But you seem much less affected by my status. I could use someone like you in my ");
		decline.append("employ,” he says coolly, prompting a surprised look from Neary, hopeful excitement ");
		decline.append("from Alex, and curiosity from Muldrew.<br> <br>");
		
		StringBuilder doc=new StringBuilder(600);
		doc.append("“I’m a paleontologist, not a biologist. I don’t see what use you could have for me,” you say, ");
		doc.append("noting that most of what you just mentioned exclusively applies to biologists.<br><br>");
		doc.append("“Of course you are, Dr. Sattler. But your reputation transcends your field. Your support would be ");
		doc.append("of great value to the Jurassic Preserve, and your defence of it would be a great help. You may ");
		doc.append("think of it as consulting work,” Hammett explains.<br> <br>");
		doc.append("Before you can respond, he keeps going. “You would be greatly compensated, of course. As my ");
		doc.append("other employees can attest, I’m a very generous employer. Your monthly pay would be in the ");
		doc.append("low seven digits,” Hammett says calmly. Muldrew and Neary nod when you look ");
		doc.append("at them, confirming that this is a normal salary offer from their boss.<br><br>");
		doc.append("“You would be subject to a non-disclosure agreement about more…sensitive aspects of the ");
		doc.append("Jurassic Preserve’s operations, but you would receive unrestricted access to its dinosaurs and all ");
		doc.append("the data Neary has collected on them,” Hammet finishes, knowing this would be the dream for ");
		doc.append("any paleontologist.<br><br>");
		doc.append("Hammett looks at you calmly, almost as if he already knows your answer, and asks “What do ");
		doc.append("you say?” Despite their attempts to be subtle, the others are all clearly interested in your answer too.");
		
		StringBuilder law=new StringBuilder(600);
		law.append("“I’m a prosecutor, not a defence attorney. I don’t see what use you could have for me,” you say, ");
		law.append("given that you literally just told Hammett he’s likely to get sued, not need to sue someone.<br><br>");
		law.append("“Of course you are, Mr. Brandt. But your reputation transcends your field. Your support would ");
		law.append("be of great value to the Jurassic Preserve, and your mere presence would surely protect from a ");
		law.append("great number of lawsuits. You may think of it as consulting work,” Hammett explains.<br><br>");
		law.append("Before you can respond, he keeps going. “You would be greatly compensated, of course. As my ");
		law.append("other employees can attest, I’m a very generous employer. Your monthly pay would be in the ");
		law.append("low seven digits,” Hammett says calmly. Muldrew and Neary nod when you look ");
		law.append("at them, confirming that this is a normal salary offer from their boss.<br><br>");
		law.append("“You would be subject to a non-disclosure agreement about more…sensitive aspects of the ");
		law.append("Jurassic Preserve’s operations, but you would have the opportunity to participate in some ");
		law.append("very…unique cases. Scott Marzani has been a thorn in my side for far too long, and will no ");
		law.append("doubt attempt to sue me soon, in a pitiful attempt to inhibit the Jurassic Preserve’s profitability,” ");
		law.append("Hammet says. Oddly, Neary looks like he’s almost startled to hear the name of Marzani Global ");
		law.append("Corporation’s CEO.<br> <br>");
		law.append("“I have my own lawsuits planned to cripple his meddlesome company, and I could use your ");
		law.append("assistance with their execution,” Hammett finishes. You, like the rest of the country, are aware of ");
		law.append("the rumours of espionage and sabotage that constantly swirl around the rival companies. ");
		law.append("Marzani’s lawyers are as good as Hammett’s, among the best in the nation. You know that any ");
		law.append("case against them would be hard-fought, a challenge that would make full use of your skills, and ");
		law.append("any victory, especially if it takes Marzani down as Hammett claims, would make you a living ");
		law.append("legal legend.<br><br>");
		law.append("Hammett looks at you calmly, almost as if he already knows your answer, and asks “What do ");
		law.append("you say?” Despite their attempts to be subtle, the others are all clearly interested in your answer too.");
		
		Story docYes=new Story("Continue", accept.toString()+doc.toString());
		story.addEdge(docLike, docYes, 1);
		
		Story docNo=new Story("Continue", decline.toString()+doc.toString());
		story.addEdge(docHate, docNo, 1);
		
		Story lawYes=new Story("Continue", accept.toString()+law.toString());
		story.addEdge(lawLike, lawYes, 1);
		
		Story lawNo=new Story("Continue", decline.toString()+law.toString());
		story.addEdge(lawHate, lawNo, 1);
		
		//leave the island
		string.delete(0, string.length());
		string.append("“Alright, I’ll take the job,” you tell him, meeting his gaze in an attempt to get used to your new ");
		string.append("boss’s piercing stare.<br> <br>");
		string.append("Muldrew isn’t surprised, but both Alex and Neary look varying degrees of excited to have someone new ");
		string.append("on the island, especially since it’s someone they already know and like. Hammett is unfazed, calmly ");
		string.append("saying “Very well. We can discuss the details once we’re off the island.”");
		
		StringBuilder prime=new StringBuilder(600);
		prime.append("<br><br>“We will make our way to the Visitor’s Center,” Hammett orders the group, clearly used to being ");
		prime.append("in charge. “My secretary knows our location, and will send a recovery team once I fail to return ");
		prime.append("within the next few hours. Kane and Muldrew will stay on lookout.” He seems surprisingly ");
		prime.append("indifferent to the fact that he’s still standing in front of the wreck that used to be his personal jet, ");
		prime.append("but unsurprisingly prepared for this contingency.<br><br>");
		prime.append("Kane, the bodyguard’s name, hands Muldrew one of his pistols, which he handles with ease, ");
		prime.append("clearly used to wielding a gun. The seven of you start walking, Muldrew leading the way, and ");
		prime.append("Hammett walking in the middle with Alex, while Kane heads the rear.<br><br>");
		prime.append("The island is rather quiet with two of its biggest dinosaurs gone, and you don’t even see another ");
		prime.append("one, making it to the Center without incident. It’s almost boring, no one willing to speak and ");
		prime.append("break the odd tension that forms whenever Hammett is around. You get the feeling that Hammett ");
		prime.append("isn’t the type of man who likes small talk.<br> <br>");
		prime.append("It isn’t after sunset that the recovery team arrives, and you see a private jet, identical to the one ");
		prime.append("Amber crashed, fly over the island. This one doesn’t even try to land, just flies over, likely ");
		prime.append("noticing the crash scene near the landing pad.<br><br>");
		prime.append("The jet disappears into the distance, but shortly after midnight, a group of men show up at the ");
		prime.append("Center, all as muscular, stoic, and heavily armed as Kane. Hammett exchanges a few words with ");
		prime.append("them, then you all follow them onto a nearby boat, clearly how they got here.");
		
		Story hired=new Story("Accept", string.toString()+prime.toString());
		story.addEdge(docYes, hired, 1);
		story.addEdge(docNo, hired, 1);
		story.addEdge(lawYes, hired, 1);
		story.addEdge(lawNo, hired, 1);
		
		string.delete(0, string.length());
		string.append("“I’m not interested in working for you, Hammett,” you tell him, meeting his gaze without ");
		string.append("difficulty, as you feel firmly about your decision.<br> <br>");
		string.append("Muldrew isn’t surprised, but Alex looks disappointed, since he'd already started to like you. Neary ");
		string.append("actually looks like he respects your choice. Hammett is unfazed, calmly saying “Very well. My offer still ");
		string.append("stands, should you change your mind.”");
		
		Story fired=new Story("Decline", string.toString()+prime.toString());
		story.addEdge(docYes, fired, 2);
		story.addEdge(docNo, fired, 2);
		story.addEdge(lawYes, fired, 2);
		story.addEdge(lawNo, fired, 2);
		
		//the end
		string.delete(0, string.length());
		string.append("Everyone seems more relaxed now that you’re leaving the dinosaurs behind, but Macomb’s absence is ");
		string.append("almost an elephant in the room. Muldrew and Neary are both quiet, Muldrew looking down at the ocean, ");
		string.append("almost despondent, and Neary shifting around uncomfortably, probably worrying about the ");
		string.append("punishment Hammett promised to give him. However, Hammett doesn’t even seem affected, ");
		string.append("though he is giving Alex a hug. No one is talking or crying, everyone looking lost in their own world.");
		string.append("<br> <br>As for you, you watch as the island slowly fades from view, reflecting on the choices you made. ");
		string.append("You wonder if you could have prevented Macomb’s death somehow, if you’d just made a different ");
		string.append("decision. You wonder if you could have gotten to know him better before he died, if you could have made ");
		string.append("more of your time on the island. But at the end of the day, you still managed to survive three ");
		string.append("rampaging dinosaurs and leave the Jurassic Preserve alive. Isn’t that all that really matters?");
		string.append("<br><br><strong>The End</strong>");

		Story complete=new Story("Continue", string.toString());
		story.addEdge(hired, complete, 1);
		story.addEdge(fired, complete, 1);
	}
	
	private static void MakeBadEnding(Story start) //hammett gets a little peeved that alex is dead
	{
		//meet hammett
		StringBuilder string=new StringBuilder(600); 
		string.append("“I’ll get the landing pad ready,” Macomb says, focusing on doing his job. He opens the control ");
		string.append("tower’s door and heads inside, stepping over the stegosaurus corpse that’s still in front of it.<br><br>");
		string.append("You turn to Neary again, and ask “Neary, did you cause this?”, the news a surprise to you. ");
		string.append("Macomb didn’t close the control tower’s door, so he can still hear everything.<br><br>");
		string.append("Neary flushes and can’t even make eye contact, but does so. “Yes…I’m sorry, okay? I…I was ");
		string.append("going to put Macomb’s guitar back, and I tripped over his extension cord. I hit the generator, and ");
		string.append("the power went out.” He sounds reluctant to admit this, clearly embarrassed and guilty, but his ");
		string.append("explanation seems light on details. He almost seems like he’s hiding something, hoping that if he ");
		string.append("doesn’t bring it up, no one will know about it.<br><br>");
		string.append("“Why were you in the control tower then?” you ask, making no attempt to hide your curiosity. ");
		string.append("You remember that Macomb said he’d argued with Neary about tripping hazards nine times, and ");
		string.append("don’t understand how Neary only caused an accident today, but don’t bring that up yet.<br><br>");
		string.append("“Uh, I just knew it would be safe. I’m sorry I ran, I-I just panicked,” Neary says, looking ");
		string.append("genuinely remorseful. “I…I fell asleep, then I heard a thud outside, and found you guys…” He ");
		string.append("shifts around from discomfort.<br><br>");
		string.append("“Guys, move out of the way! Hammett’s going to land,” Macomb interrupts, shouting from ");
		string.append("inside the tower.<br><br>");
		string.append("The landing strip starts to extend from the edge of the island, and you can hear Hammett’s jet ");
		string.append("now that it’s closer, loud enough to drown out conversation. You and Neary back up and watch ");
		string.append("as the jet approaches the island.");
		
		Story land=new Story("Continue", string.toString());
		story.addEdge(start, land, 1);
		
		string.delete(0, string.length());
		string.append("The jet descends onto the landing pad, as elegantly as it did this morning, and slows to a stop. ");
		string.append("After a moment, the door opens and Hammett steps out. He looks exactly like the photos you’ve ");
		string.append("seen. His hair is white and his skin is wrinkled with age, yet his face seems to have gotten harder, ");
		string.append("rather than softer, with his age. He’s wearing his signature blue suit and walks slowly, not out of ");
		string.append("frailty, but out of confidence; he’s in no hurry because he knows everyone here answers to him. ");
		string.append("Hammett is flanked by a man who’s clearly his bodyguard: tall, muscular, and armed with what ");
		string.append("looks like two handguns and a shotgun.<br><br>");
		string.append("Once Hammett exits the jet, his piercing blue eyes scan the scene in front of him, noting the ");
		string.append("looks on all your faces. When he speaks, his voice is surprisingly strong, almost booming. “What ");
		string.append("is the meaning of this?” he thunders.<br><br>");
		string.append("“There was an accident. The power went out and the dinosaurs escaped. We…tried to survive,” ");
		string.append("Macomb quickly responds, looking more than a bit awkward. Hammett’s gaze is cold with fury, ");
		string.append("which makes sense considering that the two dead dinosaurs in front of him probably each cost ");
		string.append("him billions of dollars to make.<br><br>");
		string.append("“You’re my top engineer, Macomb. I don’t pay you to have ‘accidents’. What happened?” ");
		string.append("Hammett almost looks like a predator waiting to pounce, trying to figure out who to blame for this.");
		
		Story meet=new Story("Meet Hammett", string.toString());
		story.addEdge(land, meet, 1);
		
		string.delete(0, string.length());
		string.append("Macomb almost glances at Neary, but stops himself. “The generator was damaged…it was my ");
		string.append("fault. I was careless.”<br><br>");
		string.append("Hammett’s eyes narrow dangerously, but before he can speak, Neary interjects, his ");
		string.append("embarrassment suddenly hardened into an almost defiant kind of bravery. “The accident ");
		string.append("happened because of me. I tripped in the substation and damaged the generator,” he says, still ");
		string.append("appearing guilty and not bothering to hide it from Hammett.<br><br>");
		string.append("Hammett’s expression has steadily been growing angrier with every word that comes out of their ");
		string.append("mouths. “Where is my grandson?” he practically growls.<br><br>");
		string.append("Macomb glances at you, and Hammett’s gaze follows, his piercing eyes boring into yours.");

		Story guilt=new Story("Continue", string.toString());
		story.addEdge(meet, guilt, 1);
		
		//tell hammett alex is dead
		StringBuilder prime=new StringBuilder(600);
		prime.append("<br><br>Hammett doesn’t raise his voice. In fact, he somehow sounds eerily calm, as if you’d just told ");
		prime.append("him about the weather. “I converted an empty island into a nature preserve. I recovered the only ");
		prime.append("intact samples of dinosaur DNA on the planet. I brought half a dozen species back from ");
		prime.append("extinction. It cost me tens of billions of dollars. It took me 7 years. Do you know why I paid such ");
		prime.append("a high price?” He’s looking directly into your eyes, addressing you.<br><br>");
		prime.append("“For Alex,” you say, the answer obvious but his point uncertain.");
		
		string.delete(0, string.length());
		string.append("“It was either his life or mine,” you tell Hammett, trying to remain firm under his gaze.<br><br>");
		string.append("Macomb and Neary’s eyes widen at the same time, both shocked speechless by this revelation. ");
		string.append("Hammett’s eyes are practically blazing with rage, and even his bodyguard, silent and stoic up to ");
		string.append("this point, visibly cringes at your words.<br><br>“Then you made the wrong choice,” Hammett says.");

		Story truth=new Story("Tell Hammett you killed Alex", string.toString()+prime.toString());
		story.addEdge(guilt, truth, 1);
		
		string.delete(0, string.length());
		string.append("“He didn’t make it. I'm sorry,” you tell Hammett, trying to remain firm under his gaze. ");
		string.append("Technically you aren’t even lying, except, perhaps, about being sorry.<br><br>");
		string.append("Macomb and Neary both look sad at the reminder. Hammett’s eyes flicker with grief for a ");
		string.append("moment before returning to their usual coldness.<br><br>");
		string.append("“He was under your care,” Hammett says, not really asking a question. You nod in response anyway.");

		Story lie=new Story("Lie", string.toString()+prime.toString());
		story.addEdge(guilt, lie, 2);
		
		//still dying no matter what lmao
		string.delete(0, string.length());
		string.append("“For my grandson, my heir, my legacy. All of this was for him. You wouldn’t be here if not for ");
		string.append("him, none of you,” he says, now addressing all of you. He’s still bizarrely calm, but his voice is ");
		string.append("firmer now, harder. “But all of you failed him.”<br><br>");
		string.append("“The Jurassic Preserve only existed for Alexander. Without him, all of this is meaningless,” ");
		string.append("Hammett continues, gesturing to the dinosaur corpses. “Now, the Jurassic Preserve no longer has ");
		string.append("a reason to exist. And neither do you. You are hereby terminated from my employ.”<br><br>");
		string.append("Before any of you can respond, Hammett gestures to his bodyguard. Lightning fast, the man ");
		string.append("draws one of his pistols and fires at Macomb with a loud bang. A red patch blossoms on the left side of ");
		string.append("his chest, and he barely has time to look down at it before he collapses to the ground.<br><br>");
		string.append("Neary screams and tries to run, but the bodyguard shoots him too, taking less than a second to ");
		string.append("switch targets. Neary’s size makes him an easy target, and with a loud bang, a patch of red starts ");
		string.append("to spread across his lower back. He lets out a shriek of pain and falls over.<br><br>");
		string.append("When you look back at Hammett, he’s already holding his bodyguard’s other pistol, and pointing ");
		string.append("it right at you. “I–” you start but are interrupted by a loud bang and a sudden white-hot pain. You ");
		string.append("look down and see that you’ve been shot in your chest.<br><br>");
		string.append("You have difficulty breathing, as if the oxygen you inhale disappears the moment it enters your ");
		string.append("body. You feel lightheaded, and can’t muster the energy to stand, collapsing to your knees.<br><br>");
		string.append("You hear another bang and look over, seeing that the bodyguard is standing over Neary with his ");
		string.append("gun extended. You can see blood splattered on the man’s shirt, and fragments of Neary’s brain ");
		string.append("and skull on the ground.<br><br>");
		string.append("You suddenly feel blood in your throat, and struggle to breathe, almost choking as blood pools ");
		string.append("up in your lungs. Hammett calmly, slowly walks towards you, and presses the barrel of his pistol ");
		string.append("against your forehead. He dispassionately stares you in the eyes, not saying a word, but a single ");
		string.append("tear forces its way out and rolls down his cheek. He pulls the trigger, and your world goes black.");
		string.append("<br><br><strong>The End</strong>");
		
		Story end=new Story("Continue", string.toString());
		story.addEdge(lie, end, 1);
		story.addEdge(truth, end, 1);
	}
}
