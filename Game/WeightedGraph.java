package Game;

import java.util.*;

public class WeightedGraph<Story> 
{
	/*Serves as a digital implementation of a CYOA book.
	* Each vertex is a page, and each vertex's adjacencies are the 1 or more pages the player can choose to go to next.
	* The graph's weighted edges are just the option numbers for the player to give as input.
	* Adjacencies are held in a map so each choice is associated with a number; makes loading the next part of the story easy.
	* Edges are one directional; no going back once a choice has been made.
	*/
	private HashMap<Story, HashMap<Integer, Story>> graph= new HashMap<Story, HashMap<Integer, Story>>();
	//linkedhash for now so choices are in order when printing; will switch back to regular hash when done
	
	@Override
	public String toString() 
	{
		String content="";
		for (Story s: graph.keySet()) 
		{
			content+="{"+s.toString()+": "+graph.get(s).toString()+"},\n";
		}
		return content;
	}
	
	//Searches for a given vertex and returns true if the vertex is in the graph, false otherwise
	public boolean containsVertex(Story vertex) 
	{
		return graph.containsKey(vertex);
	}
	
	//Searches for a given edge and returns true if the edge is in the graph, false otherwise
	public boolean containsEdge(Story from, Story to, int weight)
	{
		if (weight<0||this.containsVertex(from)==false||this.containsVertex(to)==false)
		{
			return false;
		}
		else
		{
			HashMap<Integer, Story> neighbours=graph.get(from);
			if (neighbours.get(weight)==to)
			{
				return true;
			}
			else
			{
				return false;
			}
		}
	}

	//Add a vertex to the graph. If the vertex is already in the graph, throw an IllegalArgumentException
	public void addVertex(Story vertex) 
	{
		if (graph.containsKey(vertex))  
		{
			throw new IllegalArgumentException("Vertex "+vertex+" is already in the graph.");
		} 
		else
		{
			graph.put(vertex, new HashMap<Integer, Story>()); //has no adjacencies until edges added, so leave map empty
			//also avoids nullexceptions by making empty map instead of putting null
		}
	}
	
	//Adds a weighted edge from one vertex of the graph to another
	public void addEdge(Story from, Story to, Integer weight) 
	{
		if (weight<0||this.containsVertex(from)==false)
		{
			throw new IllegalArgumentException("Edges must be positive and the starting vertex must be present.");
		} 
		else if (this.containsEdge(from, to, weight))
		{
			throw new IllegalArgumentException("The edge "+weight+" from "+from+" to "+to+" is already in the graph.");
		}
		else
		{
			if (graph.containsKey(to)==false) //no longer need to call addVertex manually when adding new vertices to graph
			{
				this.addVertex(to);
			}
			HashMap<Integer, Story> neighbours=graph.get(from); //map holds vertex's adjacencies 
			neighbours.put(weight, to); //add edge connecting them
			
		}
	}
	
	//Returns the choices for a given vertex
	public HashMap<Integer, Story> getChoices(Story start) 
	{
		if (!(this.containsVertex(start)))
		{
			throw new IllegalArgumentException("Vertex "+start+" is not in the graph.");
		} 
		else
		{
			return graph.get(start); //should never be empty or null; should always be at least 1 value in map
		}
	}
}