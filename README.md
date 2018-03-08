[![Github All Releases](https://img.shields.io/github/downloads/drivenbyentropy/aptasuite/total.svg)]()
[![HitCount](http://hits.dwyl.io/drivenbyentropy/aptasuite.svg)](http://hits.dwyl.io/drivenbyentropy/aptasuite)
[![contributions welcome](https://img.shields.io/badge/contributions-welcome-brightgreen.svg?style=flat)](https://github.com/drivenbyentropy/aptasuite/issues)

# AptaSUITE
A full-featured aptamer bioinformatics software collection for the comprehensive analysis of HT-SELEX experiments providing both, command line and graphical user interfaces.

AptaSUITE is a platform independent implementation of multiple algorithms designed for the identification of aptamer candidate sequences and the analysis of the SELEX process per se.

AptaSUITE is designed to be scalable with both data size and CPU count while minimizing the memory footprint by providing fast, off-heap data structures and storage solutions.

In its core, AptaSUITE consists of a collection of APIs and corresponding implementations facilitating storage, retrieval, and manipulation of aptamer data (such as sequences, aptamer counts in individual selection cycles, structure information and more). On top of these core data structures, a number of previously published algorithms have been implemented. Currently, these are [AptaPLEX](https://www.ncbi.nlm.nih.gov/pubmed/27080809), [AptaSIM](https://www.ncbi.nlm.nih.gov/pubmed/25870409), [AptaMUT](https://www.ncbi.nlm.nih.gov/pubmed/25870409), [AptaCLUSTER](https://www.ncbi.nlm.nih.gov/pubmed/25558474), and [AptaTRACE](https://www.ncbi.nlm.nih.gov/pubmed/27467247).

If you have any issues or recommendations, please feel free to open a [ticket](https://github.com/drivenbyentropy/aptasuite/issues).

## Installation
Download the latest precompiled version from the [release page](https://github.com/drivenbyentropy/aptasuite/releases) or [build the project from source](https://github.com/drivenbyentropy/aptasuite/wiki/Compiling-from-source).

## Usage
To open the GUI, either double-click on the executable jar file called ``aptasuite-x.y.z.jar`` or call the jar file from command line without parameters (``x.y.z`` corresponds to the version you downloaded):
```
$ java -jar aptasuite-x.y.z.jar
```
Then, follow the instructions on the screen to get started.

To work with the command line interface, create a [configuration file](https://github.com/drivenbyentropy/aptasuite/wiki/The-configuration-file) and call the desired routines of aptasuite. For instance, to import a particular dataset, cluster it, perform sequence-structure identification, and to export the demultiplexed pools to file, you would call
```
$ java -jar path/to/aptasuite-x.y.z.jar -parse -cluster -predict structure -trace -export cycles
```
For a full list of commands, run
```
$ java -jar path/to/aptasuite-x.y.z.jar -help 
```

Please see the [Wiki](https://github.com/drivenbyentropy/aptasuite/wiki) for a detailed manual.

## Screenshots
![image](https://drivenbyentropy.github.io/images/screnshot2.png)  |  ![image](https://drivenbyentropy.github.io/images/screnshot4.png)
:-------------------------:|:-------------------------:
![image](https://drivenbyentropy.github.io/images/screnshot1.png)  |  ![image](https://drivenbyentropy.github.io/images/screnshot3.png)

