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
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

public class Win10RxTxTest {

    public static void main ( String[] args )
    {
		Win10RxTx rxtx = new Win10RxTx();
		
		try {
			rxtx.connect("COM3");
			
			Thread readData = new Thread(new Runnable() {
				@Override
				public void run() {
					String data;
					while(true) {
						if (Thread.interrupted()) break;
						data = rxtx.read();
						if (data != null) {
							System.out.print(data);
							
							//the real-time when data arrived
							System.out.println(
									(new SimpleDateFormat(" {HH:mm:ss}"))
										.format(new Date()));
						} else {
							try {
								Thread.sleep(100);
							} catch (InterruptedException e) {
								break;
							}
						}
					}
				}				
			});
			readData.start();
			
			Thread userInput = new Thread(new Runnable() {
				@Override
				public void run() {
	                int c = 0;
	                byte[] buf = new byte[1204];
	                try {
						while ( ( c = System.in.read(buf)) > -1 )
						{
							if (Thread.interrupted()) break;
						    if (c > 0) {
						    	if (c==5 && buf[0]=='-' 
						    			&& buf[1]=='-' && buf[2]=='-') {
						    		break;
						    	}
						    	if (c==5 && buf[0]=='+' 
						    			&& buf[1]=='+' && buf[2]=='+') {
						    		c = 3;
						    	}
						    	rxtx.write(Arrays.copyOf(buf, c));
						    }		              
							               
						}
					} catch (IOException e) {
						e.printStackTrace();
					}                					
				}				
			});
			userInput.start();
			
			userInput.join();
			readData.interrupt();
			readData.join();
						
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			rxtx.close();			
			System.out.println("Finished!");
		}
    }
}
