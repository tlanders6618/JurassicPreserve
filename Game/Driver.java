package Game;

import java.awt.*;
import javax.swing.*;

public class Driver 
{
	
	public static void main(String[] args) //program is run from here
	{
		javax.swing.SwingUtilities.invokeLater(new Runnable() 
		{
			@Override
			public void run() 
			{
				createAndShowGUI();
			}
		}
		);
	}
	
	public static void createAndShowGUI() 
	{
		try
		{
			UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
		}
		catch (Exception e)
		{
			System.out.println("Error: Nimbus theme unavailable. Reverting to default theme.");
		}
		JFrame frame = new JFrame("Choice of Your Own Adventure: Jurassic Preserve"); //the gui is a jpanel inside of a jframe
		frame.setContentPane(new MyGUI()); //contains and handles all the game related stuff
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setMinimumSize(new Dimension(600,300)); //width then height
		frame.pack();
		frame.setVisible(true);
	}
}
