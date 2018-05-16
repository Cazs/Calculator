package com.rugd.studios.themediator;

import java.io.Serializable;


public class Item implements Serializable
{
	private int qty			= 0;
	private String id			= "";
	private int recd		= -1;//-1=not sent,0=sent,1=delivered,2=ready,3=cancelled
	private String details	="";
	
	public Item(int qty, String id,String details)
	{
		this.qty = qty;
		this.id = id;
		this.details = details;
	}
	
	public String getDetails()
	{
		return this.details;
	}
	
	public int getQuantity()
	{
		return qty;
	}
	
	public String getID()
	{
		return id;
	}
	
	public int getRecStat()
	{
		return recd;
	}
	
	public void setDetails(String details)
	{
		this.details = details;
	}
	
	public void setQuantity(int qty)
	{
		this.qty = qty;
	}
	
	public void setID(String id)
	{
		this.id = id;
	}
	
	public void setRecStat(int recd)
	{
		this.recd = recd;
	}
	
	@Override
	public String toString()
	{
		return "<ID: " + id + "\nQuantity: " + qty + "\nDetails: " + details + "\nReceival Status: " + recd + ">";
	}
}
