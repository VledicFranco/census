# Create root persistent disk.
gcutil adddisk census-control-disk --size_gb=10 --zone=us-central1-a --source_image=debian-7

# Create gce instance.
gcutil addinstance census-control --disk=census-control-disk,boot --service_account_scope=compute-rw --zone=us-central1-a --machine_type=n1-highcpu-2

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
