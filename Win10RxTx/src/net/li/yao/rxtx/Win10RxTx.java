/*
	Copyright 2017 Brian Yao Li
	
	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at
	
		http://www.apache.org/licenses/LICENSE-2.0
	
	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.
 */
package net.li.yao.rxtx;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;

public class Win10RxTx {
	//RxTx comm protocol:
	//StandardCharsets.UTF_8 strings with CR as the delimiter
	//NUL, LF and CR will be removed from the rx'ed string
	private static final int NUL = (byte)'\0';
	private static final int LF = (byte)'\n';
	private static final int CR = (byte)'\r';
	
	//Declare the followings as static so that there is only one single
	//copy of them in the memory no matter how many objects are created
	//and how many connections are initiated
	private static List<String> resultList = Collections
			.synchronizedList(new ArrayList<>());
	private static InputStream in = null;
	private static OutputStream out = null;
	private static SerialPort serialPort = null;
	private static int numberOfCopy = 0;
	
	//Declare these so that we can keep track of them and make all 
	//worker threads to be removed when the COM port is closed
	private Thread readThread = null;
	private Thread writeThread = null;
	
	/**
	 * Constructor
	 */
	public Win10RxTx() {
		super();
		numberOfCopy++;
	}

	/**
	 * Connect to a given COM port
	 * @param portName
	 * @throws Exception
	 */
	public void connect ( String portName ) throws Exception
	{
		CommPortIdentifier portIdentifier = CommPortIdentifier
				.getPortIdentifier(portName);
		if ( portIdentifier.isCurrentlyOwned() ) {
			//the port may have already open and usable
			return;
		} else {
			CommPort commPort = portIdentifier.open(this.getClass().getName(),
					2000);
			if ( commPort instanceof SerialPort ) {
				serialPort = (SerialPort) commPort;
				serialPort.setSerialPortParams(9600,	//57600,
						SerialPort.DATABITS_8,
						SerialPort.STOPBITS_1,
						SerialPort.PARITY_NONE);
	
				//wasted me days to find out this -- we must have the following 
				//two lines to make rx work!!!
				serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_RTSCTS_IN);
				serialPort.setRTS(true);
	
				in = serialPort.getInputStream();
				out = serialPort.getOutputStream();
	
				//let the worker thread to read the port continuously
				readThread = new Thread(new SerialReader(in));
				readThread.start();
			} else {
				throw new Exception("Error: Only serial ports are "
						+ "handled by this example.");
			}
		}     
	}
	
	/**
	 * Close a connection and release all resources if it is the last one
	 * @return the number of instances still in running
	 */
	public int close() {
		if (--numberOfCopy > 0) return numberOfCopy;
		
		if (readThread != null) {
			readThread.interrupt();
			try {
				readThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			readThread = null;
		}
		if (writeThread != null) {
			writeThread.interrupt();
			try {
				writeThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			writeThread = null;
		}
		if (in != null) {
			try {
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			in = null;
		}
		if (out != null) {
			try {
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			out = null;
		}
		if (serialPort != null) {
			serialPort.close();
			serialPort = null;
		}	
		return 0;
	}
	
	/**
	 * Close all connections
	 */
	public void closeAll() {
		//force to last copy left and close
		numberOfCopy = 1;
		close();
	}

	/**
	 * Read the oldest string received and remove it from the list
	 * @return string of data or null otherwise
	 */
	public String read() {
		synchronized (resultList) {
			if (resultList.size() > 0) {
				String data = resultList.get(0);
				resultList.remove(0); 
				return data;
			}
		}
		return null;
	}
	
	/**
	 * Write data in byte[]
	 * @param data
	 */
	public void write(byte[] data) {
		//wait for 1sec if the previous writing has not finished
		//and then force it to stop
		if (writeThread != null) {
			try {
				writeThread.join(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			writeThread.interrupt();
		}
		//let the working thread to write to COM port
		writeThread = new Thread(new SerialWriter(out, data));
		writeThread.start();
	}
	
	/**
	 * Write data in string
	 * @param data
	 */
	public void write(String data) {
		write(data.getBytes(StandardCharsets.UTF_8));
	}
	
	/**
	 * Helper method to add a string to the list, thread-safe
	 * @param data
	 */
	private static void addToList(String data) {
		synchronized (resultList) {
			resultList.add(data);
		}
	}
	
	/**
	 * @author liy
	 * Worker thread to read the port continuously and 
	 * add the result to the list. Can be interrupted externally
	 */
	private static class SerialReader implements Runnable {
		InputStream in;
	    
		public SerialReader ( InputStream in ) {
			this.in = in;
		}
	    
		@Override
		public void run () {
			byte[] buffer = new byte[1024];
			int len = -1;
			byte[] data = new  byte[512];
			int dataIndex = 0;
			try {
				while ( true ) {
					//read the input stream and decide if break and continue
					len = this.in.read(buffer);
					if (len < 0 || Thread.interrupted()) {
						break;
					} else if (len==0) {
						Thread.sleep(100);
						continue;
					} 
					
					//meaningful data read, so parse the data
					for (int i = 0; i < len; i++) {
						byte currentByte = buffer[i];
						//ignore NUL and LF
						if ((currentByte != NUL) && (currentByte != LF)) {
							//CR is the delimiter
							if (currentByte==CR) {
								if (dataIndex != 0) {
									//received one complete string, save it
									addToList(new String(data, 0, dataIndex,
											StandardCharsets.UTF_8));
									dataIndex = 0;	//ready for the next string
								}
							} else {
								data[dataIndex++] = currentByte;
							}
						}
					}
					
				}
			} catch ( IOException e ) {
			    e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}            
		}
	}
	
	/**
	 * @author liy
	 * Worker thread to write data to the port until finished 
	 * Can be interrupted externally
	 */
	private static class SerialWriter implements Runnable {
		OutputStream out;
		byte[] data;
		
		public SerialWriter ( OutputStream out, byte[] data ) {
			this.out = out;
			this.data = data;
		}
		
		@Override
		public void run () {
			try {                
				for (int i = 0; i < data.length; i++) {
					if (Thread.interrupted()) break;
					this.out.write(data[i]);
				}
				this.out.flush();
			} catch ( IOException e ) {
				e.printStackTrace();
			}            
		}
	}

}
