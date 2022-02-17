#!/bin/bash
output_dir=$1

# stop TPX and BP
echo "stopping services"
systemctl stop sp-tpx sp-bridgeprogram

apt-get update

# perform the upgrade
echo "upgrading"
apt-get install -y --only-upgrade teleporter-fw
status=$?
if $(exit $status); then
    # set upgrading flag that'll be picked up by TPX on next boot
    echo "Setting upgrading_flag"
    echo "upgrading" > "$output_dir"/upgrading_flag

    echo "rebooting"
    reboot
fi
