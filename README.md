# AisCoverage #

## Introduction ##

AisCoverage is a tool for calculating how well AIS receivers (sources) cover a geographical area. This information is useful for determining the range of a source, if a source is malfunctioning, if a source is redundant etc. Furthermore it can be used to see how different parameters (like the weather) affect the coverage. The tool runs as a background process analyzing a stream of AIS messages either from live data sources or a file. The connection capabilities are provided by the [AisLib](https://github.com/dma-ais/AisLib), which is a middleware Java component used to retrieve AIS messages from various sources (live sources or files).
[Read more](https://github.com/dma-ais/AisCoverage/wiki/AisCoverage)


## Prerequisites ##

* Java 1.7
* Maven

## Building ##

    ./mvnw clean install 

## Developing in Eclipse ##

M2 Eclipse plugin or 
    
    ./mvnw eclipse:eclipse
    
## Running

    target/ais-coverage-[version]-dist/ais-coverage-[version]/coverage.sh

## Rest API ##

    /coverage/rest/*
    
## Storing Coverage Data ##
Only MongoDB is supported at the moment. Mongo v3.4.2 or above needs to be installed and authentication is not yet supported.

Data (at the highest detailed level) is persisted by a background thread at regular (configurable) intervals. The default is 60 minutes.

## Handling incoming AIS packets

To avoid saturating the system with incoming packets, an overflow mechanism is implemented both at the `AisBus` and `CoverageHandler`
levels. The `AisBus` drops packets if lower layers of the application cannot handle more incoming packets. The `CoverageHandler` consumes the
packets provided by the bus through its consumers, but since it requires time to process every single packet, another buffer level is introduced
to let the bus provide as much packets as possible and give handling threads a chance to process messages with dropping as few as possible.

The size of the `AisBus` can be configured with the `<busQueueSize>` configuration element, while the `CoverageHandler` can be configured with the `<receivedPacketsBufferSize>` element:

```xml
<aisCoverageConfiguration>
    <aisbus>
        <busQueueSize>10000</busQueueSize>
    </aisbus>
    
    <receivedPacketsBufferSize>10000</receivedPacketsBufferSize>
</aisCoverageConfiguration>
```

Past these limits, the system will start overflowing and dropping packets.

## Distribution ##

A distributable zip file is found [here](http://fuka.dk/snapshots/AisCoverage-0.2.zip). <br>
Be aware: As it contains executable files, your browser may post a warning when you download the file. <br><br>
When you have downloaded the zip file, extract it to your desired location, and open the folder. <br>
Modify the configuration file (configuration.xml)to suit your needs (For guidance, Look at the 4 configuration samples (also included)). If you will be using several standart configurations you can edit the -file part in the bat file to point at the configuration file you wish you use for the given test<br>
A Sample data file is included, and the used configuration file is set up to use this sample data. <br>
So you only need need to modify the configuration file, to run your own data.

Run the coverage.bat file (windows) or the coverage.sh file (linux) to start the coverage-analysis. While the service is running, you can view the progress/result by opening your browser at the address given in the configuration file (sample-default-address = localhost:8090/coverage/)
<br><br>
When running tests from files, please use the memory only option. <br>
When running tests over longer periods using mongodb you might experience some issues. A solution to this, is being worked on.

The current release makes it possible to see the coverage within a limited timespan, down to a single hour, and with 1 hour intervals. <br>
The data is currently only persisted in memory, so might make an out of memory error, if run over longer amounts of time. <br>

A sample of how satellite data coverage will be handled is possible, by pressing ctrl, and dragging the mouse over the area of interest.
<br>

Examples of configuration files can be found here:<br>
[Read from a file](https://github.com/dma-ais/AisCoverage/blob/master/src/main/resources/coverage-fromfile-sample.xml)<br>
[Read from a live stream using a TCP connection](https://github.com/dma-ais/AisCoverage/blob/master/src/main/resources/coverage-fromtcp-sample.xml)<br>
[Keep coverage results in memory only](https://github.com/dma-ais/AisCoverage/blob/master/src/main/resources/coverage-memoryonly-sample.xml)<br>
[Store coverage results using an instance of MongoDB](https://github.com/dma-ais/AisCoverage/blob/master/src/main/resources/coverage-mongodb-sample.xml) (Remember to install MongoDB)<br>

## License ##

This library is provided under the LGPL, version 3.
