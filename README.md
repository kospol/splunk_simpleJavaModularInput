# Splunk Java Modular Input example and tutorial
Kostas Polychronis

### What is a modular input
Modular inputs are a very powerful tool that helps the process of putting data into a Splunk instance. It's used when the traditional input data solutions (monitoring files, listening for TCP or UDP data etc) are not viable or when there's need for more sophisticated processing of the data. For more imformation read [this](http://docs.splunk.com/Documentation/Splunk/latest/AdvancedDev/ModInputsIntro)

### What are we going to do in this example
The sole purpose of this project is to teach you how to develop a modular input with the Java Splunk SDK and create a simple app for that modular input.
More specifically we're going to create a modular input that will create random numbers and send them to Splunk. Through the modular input configuration we will set the preferences of the modular input such as the min and max number and the frequency.

### Why Java?
It’s true that it’s probably a little bit less complicated to create and/or maintain a modular input in python rather than in Java. Also, most of the times the servers that Splunk is running on don’t have a JVM installed. On the other hand, Java does offer a broad collection of libraries and a lot of developers are comfortable with the language. I believe that in most of the cases code reusability is the key for someone to create a modular input in Java. Don’t forget that Splunk offers SDKs for the following languages Python, JavaScript, PHP, Ruby and C#. If Java is not your thing, great! Pick one of the others and start sending data ;) More information about the rest of the SDKs can be found [here](http://dev.splunk.com/sdks)

### What is needed
* [A Splunk instance](https://www.splunk.com/en_us/download/splunk-enterprise.html)
* [The Splunk Java SDK](http://dev.splunk.com/java)
* Your favorite java editor (I used Netbeans for this example)
* 15 minutes (that’s the most difficult part)

### Project Structure
This repository is consisted of two main folders

* JavaModularInput -contains our java application and the Splunk SDK
	* src
		* com/splunk/modularinput -contains the java files for the modular input from the SDK
		* javamodularinput -contains our java program
		* MANIFEST.MF -the manifest for the jar file we will create

* myinut -contains the Splunk app of the modular input
	* bin/myinput.sh -the Shim for running our java program
	* default
		* app.conf -contains configuration for our Splunk app
		* indexes.conf -contains configuration for the new index the modular input will use
		* inputs.conf -contains configuration about how we are storing the data the modular input creates
	* jars
		* myinput.jar -our compiled java program
		* myinput.vmopts -contains the vm parameters
	* README
		* inputs.conf.spec -contains the specs for the inputs

### Project Build
#### Part 1: The Java program

1. In order to build the jar file for the modular input make sure you are inside the src directory.
```sh
$ cd JavaModularInput/src
```

2. Compile your app to create the .class files
```sh
$ javac javamodularinput/JavaModularInput.java
```
Since the JavaModularInput.java uses the SDKs files, make sure you run the javac command at the src folder, NOT inside the kavamodularinput folder.

3. Create the jar file
```sh
$ jar cmf MANIFEST.MF myinput.jar *
```
This will create the myinput.jar file that contains all the necessary files to run your java program

4. Verify the jar file works
```sh
$ java -jar myinput.jar --scheme
```

It should print the scheme specified in the java program.

5. Move the jar file to the splunk app folder
```sh
$ mv myinput.jar ../../myinput/jars/
```

That’s it!

#### Part 2: The modular input app
Splunk can not directly run java programs. To overcome that we will use a shim helper.
That means that we are going to place our jar file containing our java program inside the jars folder and Splunk will call the shim helper found inside the bin directory to run our jar file.

The shim helpers can be found inside the SDK directory under ```splunk-sdk-java-1.5.0/launchers/```. In this particular example we will run the modular input on a MacOS machine, so I copied ```shim-darwin.sh``` inside the ```bin``` folder and renamed it to ```myinput.sh```. Also I edited the file and fixed the path of the jar folder.

Normally you would want to support all the available environments. The folder structure should be like this:

* darwin_x86_64/bin/
    * shim-darwin.sh
* linux_x86/bin/
    * shim-linux.sh
* linux_x86_64/bin/
    * shim-linux.sh
* windows_x86/bin/
    * shim-windows_x86.exe
* windows_x86_64/bin/
    * shim-windows_x86_64.exe
    
Then rename all the shim executables to the name of the modular input. In this case it would be:
* darwin_x86_64/bin/
    * myinput.sh
* linux_x86/bin/
    * myinput.sh
* linux_x86_64/bin/
    * myinput.sh
* windows_x86/bin/
    * myinput.exe
* windows_x86_64/bin/
    * myinput.exe

> More information on supporting all the platforms can be found [here](http://dev.splunk.com/view/java-sdk/SP-CAAAER2) under the ```Set up the shims``` section.

Let’s analyze the app’s ```default``` directory. This directory is used by Splunk to create the modular input app.

### app.conf
```sh
[install]
is_configured = 1
[ui]
is_visible = 0
label = Java RandomNumbers ModInput
[launcher]
author = Kostas Polychronis
description = A simple modular input in java 
version = 1.0
```
The most important line here is the ```is_visible```. Since our modular input doesn’t have an accompanying app, we don’t want it to be visible with the other installed apps.

### indexes.conf
```sh
[myinput]
coldPath = $SPLUNK_DB/myinput/colddb
homePath = $SPLUNK_DB/myinput/db
thawedPath = $SPLUNK_DB/myinput/thaweddb
```

At indexes.conf we create a new index that we will use later to index the data our modular input is creating. We specify the name inside brackets, in this case our index will be named ```myinput``` and we specify the paths for the data to be saved.

NOTE: If we don’t specify the new index inside indexes.conf we will have to create it manually inside Splunk. If we send data to an unexisting index, Splunk will complain and not save the data.

> More info on the ```indexes.conf``` can be found [here](http://docs.splunk.com/Documentation/Splunk/latest/Admin/Indexesconf)

### inputs.conf
```sh
[myinput]
index = myinput
sourcetype = randomnums
```
Inside inputs.conf we specify the index and the sourcetype for out data. Notice how we used the ```myinput``` index we created in the indexes.conf file. If we don’t specify the sourcetype, Splunk will assign one automatically.

> More info on the ```inputs.conf``` can be found [here](http://docs.splunk.com/Documentation/Splunk/6.2.8/Data/Editinputs.conf)

# Analyzing the Java program
In order to understand how the Java program works you have to understand how Splunk is using the modular inputs. A modular input is a process that Splunk starts when it’s started. The difference with scripted inputs is that modular inputs run constantly where scripted inputs are started by Splunk on an interval, do something and output the data that Splunk indexes.

## The Java program is consisted of 4 main key parts:
### The getScheme method
The getScheme method is responsible to return the Scheme for the modular input. In this specific example we specify 3 arguments, the minimum and maximum range of the to be produced and the frequency. The Scheme class will generate the XML to be parsed by Splunk for the modular input using the ```--scheme``` argument. When adding a new modular input we will be asked to provide these values. Splunk will then pass these arguments to our modular input and provide the APIs to read these values. Another example of these input values could be the username and the password of a twitter account. A modular input could read these values and fetch all the tweets of that user.

>To verify and check the XML Scheme, run the following command:
```sh
$ java -jar myinput.jar --scheme
```
### The validateInput method
ValidateInput is an optional step used to validate that the values entered are correct. In this specific example we want to make sure that the minimum and maximum numbers make valid and that the frequency is at least 1 second.

### The streamEvents method
You can use a modular input multiple times. For example you can have two random number generators with different min and max values and/or different frequency. Another example is to use the Twitter modular input to fetch data from multiple accounts. But remember, the modular input is ```one``` process running constantly. This is the reason why inside the streamEvents there is a for loop. During start (that happens once, when Splunk starts or when you start the modular input), it loops through the different setups and provides the values for each one of them. Then we have to create a new thread that uses the information provided to produce an output.

### The Generator
Thing of the Generator as a worker. Provided the values we got from Splunk, we have to do something with them, produce an output -in this case a random number- create an event and send it to Splunk. You have to make sure to keep a state of the Thread’s lifecycle and manage it. In this case we want to create a new number every x seconds, so we make the thread sleep for x seconds and run it again. In the case of reading tweets, you may want to wait until a new tweet is found and save it.

>All the output from the EventWritter.synchronizedLog can be found at ```$SPLUNK_HOME/Splunk/var/log/splunk/splunkd.log```

# How to install it
Installing the example is trivial. Just copy and paste the ```myinput``` folder to the folder ```$SPLUNK_HOME/Splunk/etc/apps```.
Then in order for Splunk to refresh the apps list you have to restart Splunk. If all went ok, you should see the created index ```myinput``` under ```Settings -> Data -> Indexes```.

In order to create a new configuration of the myinput modular input, go to ```Settings -> Data -> Data Inputs``` find the modular input under the ```Local Inputs``` section and click ```Add new```.

There you will be asked to input the configuration for the modular input. Then you will be able to see the random generated numbers by going to search and searching for ```index="myinput"``` or ```source="myinput://main"```.

# Notes
This example is not perfect nor ideal. It is based on the official documentation for the (Splunk Java SDK](dev.splunk.com/view/java-sdk/SP-CAAAER2)

License
----

MIT

