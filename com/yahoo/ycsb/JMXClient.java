package com.yahoo.ycsb;

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

//import net.sourceforge.jpcap.capture.PacketCapture;


import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.rmi.registry.LocateRegistry;
import java.lang.Thread;


//import net.sourceforge.jpcap.capture.PacketCapture;
import org.apache.cassandra.concurrent.JMXEnabledThreadPoolExecutorMBean;
import org.apache.cassandra.db.compaction.CompactionManagerMBean;
import org.apache.cassandra.service.StorageProxyMBean;
import org.apache.cassandra.service.StorageServiceMBean;


//import com.datastax.driver.core.ConsistencyLevel;
import java.io.RandomAccessFile;
import java.lang.InterruptedException;


//import com.lsu.hector.HectorClient;
//import com.lsu.ml.Learner;
public class JMXClient {

	public static String attr1 = "recentReadLatencyMicros";
	/*public static String attr2 = "recentWriteLatencyMicros";
	public static String attr3 = "totalReadLatencyMicros";
	public static String attr4 = "totalWriteLatencyMicros";
	public static String attr5 = "readOperations";
	public static String attr6 = "writeOperations";
	public static String attr7 = "readRepairRepairedBackground";
	public static String attr8 = "getReadRepairAttempted";
	public static String attr9 = "readRpcTimeout";
	public static String attr10 = "writeRpcTimeout";
	public static String attr11 = "rpcTimeout";
	public static String attr12 = "totalBytesCompacted";*/
	public static String attr14 = "load";
	public static String attr13 = "streamThroughputMbPerSec";
	public static String attr15 = "avgDelta";
	public static String attr16 = "retransmission";
	public static String attr17 = "packetCount";
	public static String attr18 = "threadCount";
	public static String attr19 = "readProportion";
	public static String attr20 = "opType";
	
	
	
	public static double parseNet(){
		String str = null;
		File file = new File("/proc/net/tcp");
		FileReader reader;
		 double delta = 0;
		try {
			reader = new FileReader(file);
			BufferedReader buffReader = new BufferedReader(reader);
		    String s;
		   
		    int i = 0;
		    //while((s = buffReader.readLine()) != null && !"".equalsIgnoreCase(s) && (s).contains("\t") && (s).split("\t")!=null && (s).split("\t").length>0  && (s).split("\t").length>1){
		    while((s = buffReader.readLine()) != null){
		    	//if(i>0 && s.contains("        "))
		    	if(i>0 && s.contains("        ") && s.split("        ")[1].contains(" ") && s.split("        ")[1].split(" ").length>7)
		    		delta = Double.parseDouble(s.split("        ")[1].split(" ")[7]);
		    		
		    	i++;
		    }
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    
		return delta;
	}
	
	
	/**
	 * @param args
	 */
	public static String cassandraJMX(String opType, double latency){
		BufferedWriter br = null,br_syn = null,br_log=null;
		//int res = 0;
		JMXServiceURL url=null;
		JMXConnector jmxc = null;
		String arffStr = "";
		MBeanServerConnection mbsc = null;
		try {
			
			/*String filename_syn= "/tmp/JmxSyn.log";
			File file_syn = new File(filename_syn);
			//while(!file.exists())
			//{
		
			System.out.println("***last line in file:"+tail(file_syn));
			while(file_syn.exists() && !tail(file_syn).equalsIgnoreCase("stop"))			{	
					Thread.sleep(1000000);
			}
			br_syn = new BufferedWriter(new FileWriter(filename_syn));
		    	br_syn.append("start\n");*/
			System.out.println("********34343 before ervice jmx url optype:---"+latency);//load
			url = new JMXServiceURL("service:jmx:rmi://172.31.32.82:8081/jndi/rmi://172.31.32.82:8081/jmxrmi");
			
			System.out.println("********before service jmx mp url:---");//load

			jmxc = JMXConnectorFactory.connect(url, null);
			System.out.println("********after ervice jmx url optype:---"+opType);//load
			arffStr = arffStr + opType  +",";
			arffStr = arffStr + String.valueOf(latency)  +",";
			mbsc = 
					jmxc.getMBeanServerConnection();
			ObjectName mbeanName = new ObjectName("org.apache.cassandra.db:type=StorageProxy");
			mbeanName = new ObjectName("org.apache.cassandra.db:type=StorageService");
			StorageServiceMBean mbeanStorageService = JMX.newMBeanProxy(mbsc, mbeanName, 
					StorageServiceMBean.class);
			
			arffStr = arffStr + //Double.toString(mbeanStorageService.getLoad()) +","+
					//arffStr = arffStr + Double.toString(mbeanStorageService.getLoad()); 
					mbeanStorageService.getStreamThroughputMbPerSec();	
			
			final OperatingSystemMXBean myOsBean=  
		            ManagementFactory.getOperatingSystemMXBean();
			double load = myOsBean.getSystemLoadAverage();
			//System.out.println("mxbean cpu load:"+load);
			arffStr = arffStr + "," + load + ",";
			String strLog = "";
			String filename= "/tmp/172.31.19.158.log";
			//br = new BufferedWriter(new FileWriter(filename));
			File f_log= new File(filename);
        OutputStreamWriter writer = null;
    
                if(!f_log.exists())
                        f_log.createNewFile(); //the true will append the new data

                writer = new OutputStreamWriter(
                        new FileOutputStream(f_log, true), "UTF-8");
                br = new BufferedWriter(writer);

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
			
			
			Process proc = Runtime.getRuntime().exec("/usr/bin/java -jar /home/user/Downloads/Analysis.jar /tmp/ /tmp/");
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
		    //System.out.println("** delta:"+delta+" totkey:"+totKey+" avgdelta:"+avgDelta);
		    arffStr = arffStr + avgDelta+ ",";
		    //String consistencyLevel = CassandraCQLClient.getProperties().getProperty(READ_CONSISTENCY_LEVEL_PROPERTY, READ_CONSISTENCY_LEVEL_PROPERTY_DEFAULT);
		    
		  
			arffStr = arffStr + Double.toString(parseNet());
			
			System.out.printf("**oct 10 2015 in jmxclient end avgDelta:",avgDelta);
			//JMXLogger.generateLog(arffStr,avgDelta);
		   	//br_syn.append("stop\n");
			//br_syn.close();
			
			String strfileLog= "/home/user/Downloads/arffLog.log";
			//br = new BufferedWriter(new FileWriter(filename));
			File file_log= new File(strfileLog);
			
        OutputStreamWriter writerLog = null;
    
                if(!file_log.exists())
                	file_log.createNewFile(); //the true will append the new data

                writerLog = new OutputStreamWriter(
                        new FileOutputStream(file_log, true), "UTF-8");
                br_log = new BufferedWriter(writerLog);
                br_log.write(arffStr+"\n");
                br_log.close();
                System.out.printf("**after write jmxclient end arff:",arffStr); 
			//}
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace(); 	
		} catch (MalformedObjectNameException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
	
	}
		 /*catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();}*/
	
		return arffStr;
}	
	

	
public static String tail( File file ) {
    RandomAccessFile fileHandler = null;
    try {
        fileHandler = new RandomAccessFile( file, "r" );
        long fileLength = fileHandler.length() - 1;
        StringBuilder sb = new StringBuilder();

        for(long filePointer = fileLength; filePointer != -1; filePointer--){
            fileHandler.seek( filePointer );
            int readByte = fileHandler.readByte();

            if( readByte == 0xA ) {
                if( filePointer == fileLength ) {
                    continue;
                }
                break;

            } else if( readByte == 0xD ) {
                if( filePointer == fileLength - 1 ) {
                    continue;
                }
                break;
            }

            sb.append( ( char ) readByte );
        }

        String lastLine = sb.reverse().toString();
        return lastLine;
    } catch( java.io.FileNotFoundException e ) {
        e.printStackTrace();
        return null;
    } catch( java.io.IOException e ) {
        e.printStackTrace();
        return null;
    } finally {
        if (fileHandler != null )
            try {
                fileHandler.close();
            } catch (IOException e) {
                /* ignore */
            }
    }
}
    
	public static void writeConsistencyLevel(String str){
			
	        File f = new File("/tmp/consistencyLevel.txt");
	        OutputStreamWriter writer = null;
	        BufferedWriter fbw = null;
	        try {
	        	//System.out.println("**********ground truth f const exists:"+f.exists());
	        	if(f.exists())
	        		f.delete();
	        	//if(!f.exists())
	            f.createNewFile(); //the true will append the new data
	            //System.out.println("**********ground truth f const exists:"+f.exists());    
	            writer = new OutputStreamWriter(
	                     new FileOutputStream(f, false), "UTF-8");
	            fbw = new BufferedWriter(writer);
	            fbw.append(str);
	            fbw.flush();
	            
	        } catch (Exception e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
	        }
	        finally{
	        	try {
	        		fbw.flush();
					fbw.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
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
