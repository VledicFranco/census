# Create root persistent disk.
gcutil adddisk census-control-disk --size_gb=10 --zone=us-central1-a --source_image=debian-7

# Create needed networks and firewalls.
gcutil addnetwork census-control
gcutil addfirewall census-control-default --network=census-control --allowed="tcp:9595"
gcutil addfirewall census-control-ssh --network=census-control --allowed="tcp:22"
gcutil addfirewall census-control-allow-internal --network=census-control --allowed_ip_sources=10.0.0.0/8 --allowed="tcp:1-65535,udp:1-65535,icmp"

# Create gce instance.
gcutil addinstance census-control --disk=census-control-disk,boot --service_account_scope=compute-rw --network=census-control --zone=us-central1-a --machine_type=n1-highcpu-2

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
