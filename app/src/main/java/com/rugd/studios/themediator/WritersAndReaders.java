package com.rugd.studios.themediator;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;


public class WritersAndReaders 
{
	private static String path = Environment.getExternalStorageDirectory().getPath();

	//Clients will use this - only have one Item instance to keep track of - theirs
	public static void saveItem(Item item,String filename)
	{
        //make directory if it doesn't exist
        File f = new File(path + "/The_Mediator/");
        if(!f.isDirectory())
            f.mkdir();

		try 
		{
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(new File(path + "/The_Mediator/" + filename)));
			oos.writeObject(item);
			oos.flush();
			oos.close();
			Log.d("W&R","Saved item to disk: " + path + "/" + filename);
		} 
		catch (IOException e) 
		{
			System.err.println("Could not save backup: " + e.getMessage());
		}
	}
	
	//Server will use this - has many Item instances to keep track of
	public static void saveItems(ArrayList<Item> items,String filename)
	{
        //make directory if it doesn't exist
        File f = new File(path + "/The_Mediator/");
        if(!f.isDirectory())
            f.mkdir();

		try 
		{
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(new File(path + "/The_Mediator/"+filename)));
			oos.writeObject(items);
			oos.flush();
			oos.close();
			System.out.println("Saved items to disk: "  + path + "/" + filename);
		} 
		catch (IOException e) 
		{
			System.err.println("Could not save backup: " + e.getMessage());
		}
	}
	
	//Both server & clients
	public static void saveMessages(ArrayList<Message> messages,String filename)
	{
        //make directory if it doesn't exist
        File f = new File(path + "/The_Mediator/");
        if(!f.isDirectory())
            f.mkdir();

		//Write to disk
		try 
		{
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(new File(path + "/The_Mediator/"+filename)));
			oos.writeObject(messages);
			oos.flush();
			oos.close();
			System.out.println("Saved messages to disk: " + path + "/" + filename);
		} 
		catch (IOException e) 
		{
			System.err.println("Could not save backup: " + e.getMessage());
		}
	}
	
	//Clients will use this - only have one Item instance to keep track of - theirs
	@SuppressWarnings("unchecked")
	public static Item loadItem(String filename)
	{
        //make directory if it doesn't exist
        File f = new File(path + "/The_Mediator/");
        if(!f.isDirectory())
            f.mkdir();

		try
		{
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(new File(path + "/The_Mediator/"+filename)));
			Item item = (Item)ois.readObject();
			ois.close();
			System.out.println("Loaded item from disk: " + item);
			return item;
		} 
		catch (FileNotFoundException e2) 
		{
			System.err.println("No locally saved " + filename + " - creating a new one: " + e2.getMessage());
		}
		catch (IOException e2) 
		{
			System.err.println("IO Error: " + e2.getMessage());
		} 
		catch (ClassNotFoundException e1) 
		{
			System.err.println("This copy of the program is missing some files: " + e1.getMessage());
		}
		return null;
	}
	
	//Server will use this - has many Item instances to keep track of
	public static ArrayList<Item> loadItems(String filename)
	{
        //make directory if it doesn't exist
        File f = new File(path + "/The_Mediator/");
        if(!f.isDirectory())
            f.mkdir();

		try
		{
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(new File(path + "/The_Mediator/" + filename)));
			ArrayList<Item> items = (ArrayList<Item>)ois.readObject();
			ois.close();
			System.out.println("Loaded items from disk: " + items.size());
			return items;
		} 
		catch (FileNotFoundException e2) 
		{
			System.err.println("No locally saved " + filename + " - creating a new one: " + e2.getMessage());
		}
		catch (IOException e2) 
		{
			System.err.println("IO Error: " + e2.getMessage());
		} 
		catch (ClassNotFoundException e1) 
		{
			System.err.println("This copy of the program is missing some files: " + e1.getMessage());
		}
		return null;
	}
	
	//Both server & clients
	@SuppressWarnings("unchecked")
	public static ArrayList<Message> loadMessages(String filename)
	{
        //make directory if it doesn't exist
        File f = new File(path + "/The_Mediator/");
        if(!f.isDirectory())
            f.mkdir();

		try
		{
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(new File(path + "/The_Mediator/" + filename)));
			ArrayList<Message> messages = (ArrayList<Message>)ois.readObject();
			ois.close();
			System.out.println("Loaded messages from disk: " + messages.size());
			return messages;
		} 
		catch (FileNotFoundException e2) 
		{
			System.err.println("No locally saved "+filename+" - creating a new one: " + e2.getMessage());
		}
		catch (IOException e2) 
		{
			System.err.println("IO Error: " + e2.getMessage());
		} 
		catch (ClassNotFoundException e1) 
		{
			System.err.println("This copy of the program is missing some files: " + e1.getMessage());
		}
		return null;
	}
}
