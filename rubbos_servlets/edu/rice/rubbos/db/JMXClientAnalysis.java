package edu.rice.rubbos.db;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import java.rmi.registry.LocateRegistry;
import java.lang.Thread;

import org.apache.cassandra.concurrent.JMXEnabledThreadPoolExecutorMBean;
import org.apache.cassandra.db.compaction.CompactionManagerMBean;
import org.apache.cassandra.service.StorageProxyMBean;
import org.apache.cassandra.service.StorageServiceMBean;

import java.io.RandomAccessFile;
import java.lang.InterruptedException;
public class JMXClientAnalysis {

	/**
	 * @param args
	 */
	
	
	public static void cassandraJMXAnalysis(String opType, String key,long en, long st){
		BufferedWriter br = null,br_syn = null;
		int res = 0;
		JMXServiceURL url=null;
		JMXConnector jmxc = null;
		String arffStr = "";
		MBeanServerConnection mbsc = null;
		try {
			System.out.println("**** 10oct2014 entering cassandraJMXAnalysis method");
			String strLog = "";
			String filename= "/tmp/172.31.32.82.log";
			//br = new BufferedWriter(new FileWriter(filename));
			File f_log= new File(filename);
			OutputStreamWriter writer = null;
			if(!f_log.exists())
                        f_log.createNewFile(); //the true will append the new data

                writer = new OutputStreamWriter(
                        new FileOutputStream(f_log, true), "UTF-8");
                br = new BufferedWriter(writer);
	            strLog = strLog + key + "\n" + res + "\n";
			    strLog = strLog + st + "\n";
			    strLog = strLog + en + "\n";
			    int op =0;
			    if(opType=="read")
			    	op = 0;
			    else if(opType=="insert")
			    	op = 1;
			    else if(opType=="update")
			    	op = 2;
			    else if(opType=="scan")
			    	op = 3;
			    else
			    	op = 4;
			    strLog = strLog + op + "\n"; 
			    System.out.println("****op vALUE:---"+op);//load
			    synchronized(br){
			    br.append(strLog);
			    }
			    br.close();
				Process proc = Runtime.getRuntime().exec("sudo /usr/bin/java -jar /home/ubuntu/YCSB/Analysis.jar /tmp/ /tmp/");
				//StringTokenizer st = new StringTokenizer(in, "=;"); 
				File file = new File("/tmp/plot_gamma_perkey.txt");
				while(!file.exists())
				{
					
				}
				FileReader reader = new FileReader(file);
			    BufferedReader buffReader = new BufferedReader(reader);
			    String s;
			    double delta = 0,totKey = 0,avgDelta = 0;
			    while((s = buffReader.readLine()) != null && !"".equalsIgnoreCase(s) && (s).contains("\t") && (s).split("\t")!=null && (s).split("\t").length>0  && (s).split("\t").length>1){
			    	if(Double.parseDouble(s.split("\t")[1])>0)
			    		delta = delta + Double.parseDouble(s.split("\t")[1]);
			    	//System.out.println("***buffreader 2nd string:"+s);
			    	//System.out.println("***buffreader 2nd string split:"+s.split("\t").length);
			    }
			    file = new File("/tmp/plot_gamma_perkey_proportion.txt");
			    while(!file.exists())
				{
					
				}
				reader = new FileReader(file);
			    buffReader = new BufferedReader(reader);
			    if((s = buffReader.readLine()) != null && !"".equalsIgnoreCase(s) && (s).contains("\t") && (s).split("\t")!=null && (s).split("\t").length>0  && (s).split("\t").length>1){
			    	totKey = totKey + Double.parseDouble(s.split("\t")[0]);
			    	//System.out.println("***buffreader 2nd string:"+s);
			    	//System.out.println("***buffreader 2nd string split:"+s.split("\t").length);
			    }
			    
			    if(totKey!=0)
			    	avgDelta = delta / totKey;
			    System.out.println("**oct 10 2015 in jmxclient end avgDelta:"+avgDelta);
		    
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace(); 	
		} 
}	
	
	
	
	public static void main(String[] args) {
		//System.out.println(System.getProperty("java.library.path"));
		//int x = getMessageCount();
		//System.out.println("getmessagecount x:="+x);
			//getParameters();
		//dumpPackets();
		//System.out.println("getPacketCount:="+getPacketCount());;
		//double i = getAppThreadCount();
		//System.out.println("parseNet:="+parseNet());

	}

}
