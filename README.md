# Java-RxTx-in-Windows-10-USB-COM-ports
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

----------------
This is a working example of Java RxTx in Windows 10 with USB virtual COM ports.

Java is platform-dependent. It can work in Windows, MacOS, Linux, and even embedded systems like Raspberry Pi. This feature of Java, however, makes it difficult to work with UART (Universal Asynchronous Receiver and Transmitter), which is hardware dependent. The RxTx library was developed to tackle this problem and has been proven very useful (http://rxtx.qbang.org/wiki/index.php/Main_Page).

We are working on a project in which the Raspberry Pi would communicate with a ZigBee coordinator via UART. To start with, we tried the RxTx in a Windows 10 computer. The RxTx for x64 compiled binaries can be downloaded from Mfizz RxTx page (http://fizzed.com/oss/rxtx-for-java). The starting point is one of the code examples, "Two way communication with the serial port".

Unfortunately, the example didn't succeed in our first go. After an intensive investigation and some experiments, the problem was found and solved. Based on the example, we have further developed the program so that it is more robust and ready to build in other project. We'd like to share this with the public community.

A few words on UART. The traditional COM port in a computer is a UART. It is now obsolete and replaced by USB. By using the USB, an embedded peripheral can not only communicate with the host computer but also obtain the power supply from the host. The generic USB would provide a virtual COM port in the host which still uses the UART protocol.
