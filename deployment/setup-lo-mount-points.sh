#!/bin/bash

ACTION=$1
IMAGE=$2

if [[ $ACTION = "mount" ]]
then
    echo "Mounting image $IMAGE"
    losetup /dev/loop44 $IMAGE

    partprobe /dev/loop44

    mount /dev/loop44p1 boot
    mount /dev/loop44p2 root
    mount /dev/loop44p3 songpark
    mount /dev/loop44p4 extra

else

    echo "Unmounting boot, root, songpark and extra"

    umount /dev/loop44p1
    umount /dev/loop44p2
    umount /dev/loop44p3
    umount /dev/loop44p4

    dmsetup remove /dev/loop44
    
fi
