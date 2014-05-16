# Create root persistent disks.
gcutil adddisk census-<control | engine | neo4j>-disk --size_gb=10 --zone=us-central1-a --source_image=debian-7

# Create needed networks and firewalls for Census Control.
gcutil addnetwork census-framework
gcutil addfirewall census-framework-default --network=census-framework --allowed="tcp:9595"
gcutil addfirewall census-framework-ssh --network=census-framework --allowed="tcp:22"
gcutil addfirewall census-framework-allow-internal --network=census-framework --allowed_ip_sources=10.0.0.0/8 --allowed="tcp:1-65535,udp:1-65535,icmp"

# Create gce instance for Census Control.
gcutil addinstance census-control --disk=census-control-disk,boot --service_account_scope=compute-rw --network=census-framework --zone=us-central1-a --machine_type=n1-highcpu-2

# Create testing gce instance for Census Engine.
gcutil addinstance census-engine-test --disk=census-engine-disk,boot --service_account_scope=storage-ro --network=census-framework --zone=us-central1-a --machine_type=n1-highcpu-2 --metadata=startup-script-url:gs://census-framework/engine-startup.sh

# Create neo4j instance.
gcutil addinstance census-neo4j --disk=census-neo4j-disk,boot --service_account_scope=storage-ro --network=census-framework --zone=us-central1-a --machine_type=n1-standard-1

# Get logs from instance.
gcutil getserialportoutput <instance>

# Script for census-control instance instalation.
sudo -s
apt-get update
apt-get install default-jdk
apt-get install git
apt-get install unzip
useradd -m census-control
passwd census-conrol (root)
login census-control
cd
git clone https://github.com/FrancoAra/census-control.git
wget http://downloads.typesafe.com/play/2.1.5/play-2.1.5.zip
unzip play-2.1.5.zip
export PATH=$PATH:$HOME/play-2.1.5
echo "export PATH=$PATH:$HOME/play-2.1.5" >> $HOME/.bashrc
cd census-control
screen -dmS census-control play "start 9595"
exit
exit
exit
