package com.outlyer.jmx.jmxquery.app.tests;

public class Test implements TestMBean {

    private String names[] = new String[3];

    public void setNames(String[] names) {
        this.names = names;
    }

    public String[] getNames() {
        names[0] = "Test1";
        names[2] = "Test3";
        return names;
    }

    public void sayHello() {
        System.out.println("Hello");

    }

    public int add(int x, int y) {
        return x + y;
    }

    public String value() {
        System.out.println("Hello");
        return "Hello";
    }

    public int valueInt() {
        return 5;
    }

    public String valueString(String str) {
        return str;
    }

    public void setInt(Integer i) {
        System.out.println(i);
    }

    public String add(String x, String y) {

        return x + y;
    }

    public Double add(Double x, int y) {
        return x + y;
    }

}
