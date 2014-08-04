Google Compute Engine Setup
---------------------------

To setup the project on Google Compute Engine the following must be done:

0. Setup `gcutil`
1. Create the Census Framework network.
2. Install Census Control on a booteable disk.
3. Install Census Engine on a booteable disk.
4. Create a disk snapshot of the Census Engine disk.
5. Add the Census Engine startup script to Google Cloud Storage.
6. Check the configuration in the Census Control `instance` package.
7. Start the Census Control service.

### 0 Setup gcutil

Check [this](https://developers.google.com/compute/docs/gcutil/) link to install and setup `gcutil`.

_Note: it is recommended to set the default project so that the next commands doesn't need the `--project=<project_id>` flag._

```
gcloud auth login
gcloud config set project <project_id>
```

### 1 Create the Census Framework network

Create the network.
```
gcutil addnetwork census-framework
```
Open port 9595 for external communication with Census Control.
```
gcutil addfirewall census-framework-default --network=census-framework --allowed="tcp:9595"
```
Open port 22 for ssh connections to Census Control.
```
gcutil addfirewall census-framework-ssh --network=census-framework --allowed="tcp:22"
```
Allow communication between Census instances inside the GCE virtual network.
```
gcutil addfirewall census-framework-allow-internal --network=census-framework --allowed_ip_sources=10.0.0.0/8 --allowed="tcp:1-65535,udp:1-65535,icmp"
```

### 2 Install Census Control on a booteable disk

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
git clone https://github.com/FrancoAra/census-control.git
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

### 3 Install Census Engine on a booteable disk

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
git clone https://github.com/FrancoAra/census-engine.git
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

### 4 Create a disk snapshot of the Census Engine disk

```
gcutil addsnapshot census-engine-snapshot --source_disk=census-engine-disk
```

### 5 Add the Census Engine startup script to Google Cloud Storage

[Upload](https://developers.google.com/storage/docs/json_api/v1/how-tos/upload) a startup script to Google Cloud Storage for the Census Engine future instances.

Here is a possible script (stored in: `gs://census-framework/engine-startup.sh`):
```
#!/bin/sh

cd /usr/share/census-engine
/usr/share/play-2.1.5/play "start 9000"
```

_Note: You will need to change the startup script url in the Census Control instances configuration._

### 6 Check the configuration in the Census Control instance package

In the census-control instance, inside the Census Control project `/usr/share/census-control/app/instances/conf.scala` change the desired configuration.

### 7 Start the Census Control service

```
gcutil ssh census-control

sudo -s
cd /usr/share/census-control
../play-2.1.5/play "start 9595"
```

_Note: You can use a program like [screen](http://www.gnu.org/software/screen/) to demonize the service, or create the census-control instance with a startup script like you did with the census-engine instances._
