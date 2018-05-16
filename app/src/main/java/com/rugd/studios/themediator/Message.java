package com.rugd.studios.themediator;

import java.io.Serializable;


public class Message implements Serializable
{
	private int id		= 0;
	private String type		= "";
	private String msg		= "";
	private String destID	= "";
	private int priority	= NORMAL;

	public static final int NORMAL = 0;
	public static final int MEDIUM = 0;
	public static final int HIGH = 0;
	public static final int EXTREME = 0;
	
	public Message(int id,String msg,String type,String destID,int priority)
	{
		this.id = id;
		this.msg = msg;
		this.type = type;
		this.destID = destID;
		this.priority = priority;
	}

	public int getPriority(){return priority;}

	public void setPriority(int priority){this.priority=priority;}

	public int getId() 
	{
		return id;
	}

	public void setId(int id) 
	{
		this.id = id;
	}

	public String getType() 
	{
		return type;
	}

	public void setType(String type) 
	{
		this.type = type;
	}

	public String getMsg() 
	{
		return msg;
	}

	public void setMsg(String msg) 
	{
		this.msg = msg;
	}

	public String getDestID() 
	{
		return destID;
	}

	public void setDestID(String destID) 
	{
		this.destID = destID;
	}
}
