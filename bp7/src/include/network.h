/*
 * network.h
 *
 *  Created on: Mar 16, 2021
 *      Author: thanx
 */

#ifndef SRC_INCLUDE_NETWORK_H_
#define SRC_INCLUDE_NETWORK_H_

#include <netinet/in.h>
#include <net/if.h>
#include <stdio.h>

#include <string.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/socket.h>
#include <sys/ioctl.h>
#include <linux/netlink.h>
#include <linux/rtnetlink.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <arpa/inet.h>


#include <asm/types.h>
#include <linux/if_packet.h>
#include <linux/if_ether.h>
#include <linux/if_arp.h>
#include <arpa/inet.h>  //htons etc

#include "reg_io.h"

#define BUFSIZE 8192
//char gateway[255];

#define PROTO_ARP 0x0806
#define ETH2_HEADER_LEN 14
#define HW_TYPE 1
#define MAC_LENGTH 6
#define IPV4_LENGTH 4
#define ARP_REQUEST 0x01
#define ARP_REPLY 0x02
#define BUF_SIZE 60


struct route_info {
    struct in_addr dstAddr;
    struct in_addr srcAddr;
    struct in_addr gateWay;
    char ifName[IF_NAMESIZE];
};

struct arp_header {
    unsigned short hardware_type;
    unsigned short protocol_type;
    unsigned char hardware_len;
    unsigned char protocol_len;
    unsigned short opcode;
    unsigned char sender_mac[MAC_LENGTH];
    unsigned char sender_ip[IPV4_LENGTH];
    unsigned char target_mac[MAC_LENGTH];
    unsigned char target_ip[IPV4_LENGTH];
};


void extractIpAddress(unsigned char *sourceString,unsigned char *ipAddress);
void extractMacAddress(unsigned char *sourceString,unsigned char *macAddress);
char * print_mac(unsigned long ip, unsigned char * bytes);
char * print_ip(unsigned int ip, unsigned char * bytes);

int run_arping(const char *ifname, const char *ip, ETH_PARAM_ID id);
int getMacAddress(char * mac_address);
int getNetMask(char * ip);
int getLocalIP(char * ip);
int getGateway(char *gateway);
#endif /* SRC_INCLUDE_NETWORK_H_ */
