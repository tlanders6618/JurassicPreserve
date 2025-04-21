package Game;

import java.util.HashMap;

public class Story 
{
	/* Story is the implementation for a CYOA book page. 
	 * Pages are made independently from each other, connected by the graph maintained in the GUI.
	 */
	private String title; //name for the instructions that lead to the page, e.g. "try to outrun the dinosaur"
	private String text; //text for each page
	private String choiceName; //if the player makes an important choice by turning to this page
	private int id;
	private static int counter=1; //needed for equality comparisons
	
	public Story(String tit, String tex)
	{
		this.title=tit;
		this.text=tex;
		this.choiceName=null;
		id=counter++;
	}
	
	public Story(String tit, String tex, String cname)
	{
		this.title=tit;
		this.text=tex;
		this.choiceName=cname;
		id=counter++;
	}
	
	public Story (String tit, Story other) //facilitates making false choices; identical text but different choice names
	{
		this.title=tit;
		this.text=other.text;
		this.choiceName=other.choiceName;
		id=counter++;
	}
	
	public String getTitle()
	{
		return this.title;
	}
	
	public String getText()
	{
		return this.text;
	}
	
	public String getChoice ()
	{
		return this.choiceName;
	}
	
	@Override
	public int hashCode()
	{
		return title.hashCode()+this.id;
	}
	
	@Override
	public boolean equals (Object other)
	{
		if (this==other) 
		{
			return true;
		}
		else if (!(other instanceof Story)) 
		{
			return false;
		}
		
		Story dummy=(Story) other;
		
		//can no longer use text comparison since later part of story has lots of duplicate text between many paths
		return (this.id==dummy.id); 
	}
	
	@Override
	public String toString()
	{
		return this.title;
		
	}
}
