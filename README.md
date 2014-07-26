Census v0.2.0
=============

IMPORTANT: THIS DOCUMENTATION IS NOT COMPLETE AND STILL IN DEVELOPMENT, PLEASE WAIT WHILE A BETTER ONE IS FINISHED :)

Census is a distributed production environment built to exploit the potential of [Signal Collect](http://uzh.github.io/signal-collect/).

Features
--------

  1. Census has a library of implemented graph algorithms to be computed.
  2. Census uses [Neo4j](http://www.neo4j.org/) to facilitate the data interaction.
  3. You can use Census with static servers of your own or install it in Google Compute Engine to have computing instances on demand that Census can manage.

Google Compute Engine Setup
---------------------------

To setup the project on Google Compute Engine the following must be done:

1. Setup `gcutil`
2. Create the Census Framework network.
3. Install Census Control on a booteable disk.
4. Install Census Engine on a booteable disk.
5. Create a disk snapshot of the Census Engine disk.
6. Add the Census Engine startup script to Google Cloud Storage.
7. Check the configuration in the Census Control `instance` package.
8. Start the Census Control service.

### 1 Setup gcutil

Check [this](https://developers.google.com/compute/docs/gcutil/) link to install and setup `gcutil`.

_Note: it is recommended to set the default project so that the next commands doesn't need the `--project=<project_id>` flag._

```
gcloud auth login
gcloud config set project <project_id>
```

### 2 Create the Census Framework network

Create the network.
```
gcutil addnetwork census-framework
```
Open port 9595 for external communication with Census Control.
```
gcutil addfirewall census-framework-default --network=census-framework --allowed="tcp:9000"
```
Open port 22 for ssh connections to Census Control.
```
gcutil addfirewall census-framework-ssh --network=census-framework --allowed="tcp:22"
```
Allow communication between Census instances inside the GCE virtual network.
```
gcutil addfirewall census-framework-allow-internal --network=census-framework --allowed_ip_sources=10.0.0.0/8 --allowed="tcp:1-65535,udp:1-65535,icmp"
```

### 3 Install Census Control on a booteable disk

Create the Census Control bootable disk with Debian 7 and 10gb of space. 
```
gcutil adddisk census-control-disk --size_gb=10 --zone=us-central1-a --source_image=debian-7
```
Create a temporal Census Control instance to install the service.
```
gcutil addinstance census-control --disk=census-control-disk,boot  --network=census-framework --zone=us-central1-a --machine_type=n1-highcpu-2
```
Install the service.
```
# Connect to the instance.
gcutil ssh census-control

# Install necessary software.
sudo -s
apt-get update
apt-get install default-jdk
apt-get install git 
apt-get install unzip

cd /usr/share
# Clone the project.
git clone https://github.com/FrancoAra/census.git
# Install Play Framework 2.1.5
wget http://downloads.typesafe.com/play/2.1.5/play-2.1.5.zip
unzip play-2.1.5.zip

exit
exit
```
Delete the instance if you want.
```
gcutil deleteinstance census-control
```

### 4 Install Census Engine on a booteable disk

Create the Census Engine bootable disk with Debian 7 and 10gb of space. 
```
gcutil adddisk census-engine-disk --size_gb=10 --zone=us-central1-a --source_image=debian-7
```
Create a temporal Census Engine instance to install the service.
```
gcutil addinstance census-engine --disk=census-engine-disk,boot  --network=census-framework --zone=us-central1-a --machine_type=n1-highcpu-2
```
Install the service.
```
# Connect to the instance.
gcutil ssh census-engine

# Install necessary software.
sudo -s
apt-get update
apt-get install default-jdk
apt-get install git 
apt-get install unzip

cd /usr/share
# Clone the project or download the precompiled version here 
# so that Census Engine instances do not need to compile the 
# code when created.
git clone https://github.com/FrancoAra/census.git
# Install Play Framework 2.1.5
wget http://downloads.typesafe.com/play/2.1.5/play-2.1.5.zip
unzip play-2.1.5.zip

exit
exit
```
Delete the instance (you wont need this instance anymore).
```
gcutil deleteinstance census-engine
```

### 5 Create a disk snapshot of the Census Engine disk

```
gcutil addsnapshot census-engine-snapshot --source_disk=census-engine-disk
```

### 6 Add the Census Engine startup script to Google Cloud Storage

[Upload](https://developers.google.com/storage/docs/json_api/v1/how-tos/upload) a startup script to Google Cloud Storage for the Census Engine future instances.

Here is a possible script (stored in: `gs://census-framework/engine-startup.sh`):
```
#!/bin/sh

cd /usr/share/census
/usr/share/play-2.1.5/play "start 9000"
```

_Note: You will need to change the startup script url in the Census Control instances configuration._

### 7 Check the configuration in the Census Control instance package

In the census-control instance, inside the Census Control project `/usr/share/census/app/instances/conf.scala` change the desired configuration.

### 8 Start the Census Control service

```
gcutil addinstance census-control --disk=census-control-disk,boot  --network=census-framework --zone=us-central1-a --machine_type=n1-highcpu-2 --service_account_scope=compute-rw
gcutil ssh census-control

sudo -s
cd /usr/share/census
../play-2.1.5/play "start 9000"
```

_Note: You can use a program like [screen](http://www.gnu.org/software/screen/) to demonize the service, or create the census-control instance with a startup script like you did with the census-engine instances._
