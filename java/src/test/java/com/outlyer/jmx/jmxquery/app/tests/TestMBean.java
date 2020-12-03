package com.outlyer.jmx.jmxquery.app.tests;

public interface TestMBean { 
	
    public void sayHello(); 
    public int add(int x, int y); 
    public String add(String x, String y); 
    public Double add(Double x, int y); 
    
    public String value(); 
    
    public int valueInt(); 

    public void setVal(final int val); 
    public int getVal();
   
    public String valueString(String str);
	String[] getNames();
} 