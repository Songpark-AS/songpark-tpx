/*
 * network.c
 *
 *  Created on: Mar 13, 2021
 *      Author: thanx
 */


#include "include/network.h"
#include "include/ui_control.h"

#define debug(x...) printf(x);printf("\n");
#define info(x...) printf(x);printf("\n");
#define warn(x...) printf(x);printf("\n");
#define err(x...) printf(x);printf("\n");




/*
Function : extractIpAddress
Arguments :
1) sourceString - String pointer that contains ip address
2) ipAddress - Target variable short type array pointer that will store ip address octets
*/
void extractIpAddress(unsigned char *sourceString,unsigned char *ipAddress)
{
    unsigned short len=0;
    unsigned char  oct[4]={0},cnt=0,cnt1=0,i,buf[5];

    len=strlen(sourceString);
    for(i=0;i<len;i++)
    {
        if(sourceString[i]!='.'){
            buf[cnt++] =sourceString[i];
        }
        if(sourceString[i]=='.' || i==len-1){
            buf[cnt]='\0';
            cnt=0;
            oct[cnt1++]=atoi(buf);
        }
    }
    ipAddress[0]=oct[0];
    ipAddress[1]=oct[1];
    ipAddress[2]=oct[2];
    ipAddress[3]=oct[3];
}

char * print_ip(unsigned int ip, unsigned char * bytes)
{
//    unsigned char bytes[4];
    bytes[0] = ip & 0xFF;
    bytes[1] = (ip >> 8) & 0xFF;
    bytes[2] = (ip >> 16) & 0xFF;
    bytes[3] = (ip >> 24) & 0xFF;
    bytes[4] = 0;
    printf("%d.%d.%d.%d\n", bytes[3], bytes[2], bytes[1], bytes[0]);
    sprintf(bytes, "%d.%d.%d.%d\0", bytes[3], bytes[2], bytes[1], bytes[0]);
    return bytes;
}


char * print_mac(unsigned long ip, unsigned char * bytes)
{

    bytes[0] = ip & 0xFF;
    bytes[1] = (ip >> 8) & 0xFF;
    bytes[2] = (ip >> 16) & 0xFF;
    bytes[3] = (ip >> 24) & 0xFF;
    bytes[4] = (ip >> 32) & 0xFF;
    bytes[5] = (ip >> 40) & 0xFF;
    bytes[6] = 0;
    printf("%d.%d.%d.%d\n", bytes[5], bytes[4], bytes[3], bytes[2], bytes[1], bytes[0]);
    return bytes;
}


/*
Function : extractIpAddress
Arguments :
1) sourceString - String pointer that contains ip address
2) macAddress - Target variable short type array pointer that will store ip address octets
*/
void extractMacAddress(unsigned char *sourceString,unsigned char *macAddress)
{
    unsigned short len=0;
    unsigned char  oct[6]={0},cnt=0,cnt1=0,i,buf[5];

    len=strlen(sourceString);
    for(i=0;i<len;i++)
    {
        if(sourceString[i]!='.'){
            buf[cnt++] =sourceString[i];
        }
        if(sourceString[i]=='.' || i==len-1){
            buf[cnt]='\0';
            cnt=0;
            //oct[cnt1++]=atoi(buf);
            oct[cnt1++]=(unsigned char)strtol(buf, NULL, 16);
        }
    }
    macAddress[0]=oct[0];
    macAddress[1]=oct[1];
    macAddress[2]=oct[2];
    macAddress[3]=oct[3];
    macAddress[4]=oct[4];
    macAddress[5]=oct[5];
}




int readNlSock(int sockFd, char *bufPtr, int seqNum, int pId)
{
    struct nlmsghdr *nlHdr;
    int readLen = 0, msgLen = 0;

 do {
    /* Recieve response from the kernel */
        if ((readLen = recv(sockFd, bufPtr, BUFSIZE - msgLen, 0)) < 0) {
            perror("SOCK READ: ");
            return -1;
        }

        nlHdr = (struct nlmsghdr *) bufPtr;

    /* Check if the header is valid */
        if ((NLMSG_OK(nlHdr, readLen) == 0)
            || (nlHdr->nlmsg_type == NLMSG_ERROR)) {
            perror("Error in recieved packet");
            return -1;
        }

    /* Check if the its the last message */
        if (nlHdr->nlmsg_type == NLMSG_DONE) {
            break;
        } else {
    /* Else move the pointer to buffer appropriately */
            bufPtr += readLen;
            msgLen += readLen;
        }

    /* Check if its a multi part message */
        if ((nlHdr->nlmsg_flags & NLM_F_MULTI) == 0) {
           /* return if its not */
            break;
        }
    } while ((nlHdr->nlmsg_seq != seqNum) || (nlHdr->nlmsg_pid != pId));
    return msgLen;
}
/* For printing the routes. */
void printRoute(struct route_info *rtInfo)
{
    char tempBuf[512];

/* Print Destination address */
    if (rtInfo->dstAddr.s_addr != 0)
        strcpy(tempBuf,  inet_ntoa(rtInfo->dstAddr));
    else
        sprintf(tempBuf, "*.*.*.*\t");
    fprintf(stdout, "%s\t", tempBuf);

/* Print Gateway address */
    if (rtInfo->gateWay.s_addr != 0)
        strcpy(tempBuf, (char *) inet_ntoa(rtInfo->gateWay));
    else
        sprintf(tempBuf, "*.*.*.*\t");
    fprintf(stdout, "%s\t", tempBuf);

    /* Print Interface Name*/
    fprintf(stdout, "%s\t", rtInfo->ifName);

    /* Print Source address */
    if (rtInfo->srcAddr.s_addr != 0)
        strcpy(tempBuf, inet_ntoa(rtInfo->srcAddr));
    else
        sprintf(tempBuf, "*.*.*.*\t");
    fprintf(stdout, "%s\n", tempBuf);
}

void printGateway(char *gateway)
{
    printf("%s\n", gateway);
}
/* For parsing the route info returned */
void parseRoutes(char *gateway, struct nlmsghdr *nlHdr, struct route_info *rtInfo)
{
    struct rtmsg *rtMsg;
    struct rtattr *rtAttr;
    int rtLen;

    rtMsg = (struct rtmsg *) NLMSG_DATA(nlHdr);

/* If the route is not for AF_INET or does not belong to main routing table
then return. */
    if ((rtMsg->rtm_family != AF_INET) || (rtMsg->rtm_table != RT_TABLE_MAIN))
        return;

/* get the rtattr field */
    rtAttr = (struct rtattr *) RTM_RTA(rtMsg);
    rtLen = RTM_PAYLOAD(nlHdr);
    for (; RTA_OK(rtAttr, rtLen); rtAttr = RTA_NEXT(rtAttr, rtLen)) {
        switch (rtAttr->rta_type) {
        case RTA_OIF:
            if_indextoname(*(int *) RTA_DATA(rtAttr), rtInfo->ifName);
            break;
        case RTA_GATEWAY:
            rtInfo->gateWay.s_addr= *(u_int *) RTA_DATA(rtAttr);
            break;
        case RTA_PREFSRC:
            rtInfo->srcAddr.s_addr= *(u_int *) RTA_DATA(rtAttr);
            break;
        case RTA_DST:
            rtInfo->dstAddr .s_addr= *(u_int *) RTA_DATA(rtAttr);
            break;
        }
    }
    //printf("%s\n", inet_ntoa(rtInfo->dstAddr));

    if (rtInfo->dstAddr.s_addr == 0)
        sprintf(gateway, (char *) inet_ntoa(rtInfo->gateWay));
    //printRoute(rtInfo);

    return;
}


int getGateway(char *gateway)
{
    struct nlmsghdr *nlMsg;
    struct rtmsg *rtMsg;
    struct route_info *rtInfo;
    char msgBuf[BUFSIZE];

    int sock, len, msgSeq = 0;

/* Create Socket */
    if ((sock = socket(PF_NETLINK, SOCK_DGRAM, NETLINK_ROUTE)) < 0)
        perror("Socket Creation: ");

    memset(msgBuf, 0, BUFSIZE);

/* point the header and the msg structure pointers into the buffer */
    nlMsg = (struct nlmsghdr *) msgBuf;
    rtMsg = (struct rtmsg *) NLMSG_DATA(nlMsg);

/* Fill in the nlmsg header*/
    nlMsg->nlmsg_len = NLMSG_LENGTH(sizeof(struct rtmsg));  // Length of message.
    nlMsg->nlmsg_type = RTM_GETROUTE;   // Get the routes from kernel routing table .

    nlMsg->nlmsg_flags = NLM_F_DUMP | NLM_F_REQUEST;    // The message is a request for dump.
    nlMsg->nlmsg_seq = msgSeq++;    // Sequence of the message packet.
    nlMsg->nlmsg_pid = getpid();    // PID of process sending the request.

/* Send the request */
    if (send(sock, nlMsg, nlMsg->nlmsg_len, 0) < 0) {
        printf("Write To Socket Failed...\n");
        return -1;
    }

/* Read the response */
    if ((len = readNlSock(sock, msgBuf, msgSeq, getpid())) < 0) {
        printf("Read From Socket Failed...\n");
    return -1;
    }
/* Parse and print the response */
    rtInfo = (struct route_info *) malloc(sizeof(struct route_info));
//fprintf(stdout, "Destination\tGateway\tInterface\tSource\n");
    for (; NLMSG_OK(nlMsg, len); nlMsg = NLMSG_NEXT(nlMsg, len)) {
        memset(rtInfo, 0, sizeof(struct route_info));
        parseRoutes(gateway, nlMsg, rtInfo);
    }
    free(rtInfo);
    close(sock);

    printGateway(gateway);
    return 0;
}

int getLocalIP(char * ip)
{
 int fd;
 struct ifreq ifr;

 fd = socket(AF_INET, SOCK_DGRAM, 0);

 /* I want to get an IPv4 IP address */
 ifr.ifr_addr.sa_family = AF_INET;

 /* I want IP address attached to "eth1" */
 strncpy(ifr.ifr_name, "eth1", IFNAMSIZ-1);

 ioctl(fd, SIOCGIFADDR, &ifr);

 close(fd);

 /* display result */
 sprintf(ip, "%s\0", inet_ntoa(((struct sockaddr_in *)&ifr.ifr_addr)->sin_addr));
 printf("LOCAL_IP::%s\n", ip);

 return 0;
}


int getNetMask(char * ip)
{
 int fd;
 struct ifreq ifr;

 fd = socket(AF_INET, SOCK_DGRAM, 0);

 /* I want to get an IPv4 IP address */
 ifr.ifr_addr.sa_family = AF_INET;

 /* I want IP address attached to "eth1" */
 strncpy(ifr.ifr_name, "eth1", IFNAMSIZ-1);

 ioctl(fd, SIOCGIFNETMASK, &ifr);

 close(fd);

 /* display result */
 sprintf(ip, "%s\0", inet_ntoa(((struct sockaddr_in *)&ifr.ifr_addr)->sin_addr));
  printf("Netmask::%s\n", ip);

 return 0;
}


int getMacAddress(char * mac_address)
{
    struct ifreq ifr;
    struct ifconf ifc;
    char buf[1024];
    int success = 0;

    int sock = socket(AF_INET, SOCK_DGRAM, IPPROTO_IP);
    if (sock == -1) { /* handle error*/ };

    ifc.ifc_len = sizeof(buf);
    ifc.ifc_buf = buf;
    if (ioctl(sock, SIOCGIFCONF, &ifc) == -1) {
    	/* handle error */
    	return -1;
    }

    struct ifreq* it = ifc.ifc_req;
    const struct ifreq* const end = it + (ifc.ifc_len / sizeof(struct ifreq));

    for (; it != end; ++it) {
        strcpy(ifr.ifr_name, it->ifr_name);
        if (ioctl(sock, SIOCGIFFLAGS, &ifr) == 0) {
            if (! (ifr.ifr_flags & IFF_LOOPBACK)) { // don't count loopback
                if (ioctl(sock, SIOCGIFHWADDR, &ifr) == 0) {
                    success = 1;
                    break;
                }
            }
        }
        else { /* handle error */ }
    }

//    unsigned char mac_address[6];

    if (success) {
    	memcpy(mac_address, ifr.ifr_hwaddr.sa_data, 6);
    	return 0;
    }else{
    	info("Failed to get local MAC address exiting...");
    	exit(-1);
    	return -1;
    }
}




/*
 * Converts struct sockaddr with an IPv4 address to network byte order uin32_t.
 * Returns 0 on success.
 */
int int_ip4(struct sockaddr *addr, uint32_t *ip)
{
    if (addr->sa_family == AF_INET) {
        struct sockaddr_in *i = (struct sockaddr_in *) addr;
        *ip = i->sin_addr.s_addr;
        return 0;
    } else {
        err("Not AF_INET");
        return 1;
    }
}

/*
 * Formats sockaddr containing IPv4 address as human readable string.
 * Returns 0 on success.
 */
int format_ip4(struct sockaddr *addr, char *out)
{
    if (addr->sa_family == AF_INET) {
        struct sockaddr_in *i = (struct sockaddr_in *) addr;
        const char *ip = inet_ntoa(i->sin_addr);
        if (!ip) {
            return -2;
        } else {
            strcpy(out, ip);
            return 0;
        }
    } else {
        return -1;
    }
}

/*
 * Writes interface IPv4 address as network byte order to ip.
 * Returns 0 on success.
 */
int get_if_ip4(int fd, const char *ifname, uint32_t *ip) {
    int err = -1;
    struct ifreq ifr;
    memset(&ifr, 0, sizeof(struct ifreq));
    if (strlen(ifname) > (IFNAMSIZ - 1)) {
        err("Too long interface name");
        goto out;
    }

    strcpy(ifr.ifr_name, ifname);
    if (ioctl(fd, SIOCGIFADDR, &ifr) == -1) {
        perror("SIOCGIFADDR");
        goto out;
    }

    if (int_ip4(&ifr.ifr_addr, ip)) {
        goto out;
    }
    err = 0;
out:
    return err;
}

/*
 * Sends an ARP who-has request to dst_ip
 * on interface ifindex, using source mac src_mac and source ip src_ip.
 */
int send_arp(int fd, int ifindex, const unsigned char *src_mac, uint32_t src_ip, uint32_t dst_ip)
{
    int err = -1;
    unsigned char buffer[BUF_SIZE];
    memset(buffer, 0, sizeof(buffer));

    struct sockaddr_ll socket_address;
    socket_address.sll_family = AF_PACKET;
    socket_address.sll_protocol = htons(ETH_P_ARP);
    socket_address.sll_ifindex = ifindex;
    socket_address.sll_hatype = htons(ARPHRD_ETHER);
    socket_address.sll_pkttype = (PACKET_BROADCAST);
    socket_address.sll_halen = MAC_LENGTH;
    socket_address.sll_addr[6] = 0x00;
    socket_address.sll_addr[7] = 0x00;

    struct ethhdr *send_req = (struct ethhdr *) buffer;
    struct arp_header *arp_req = (struct arp_header *) (buffer + ETH2_HEADER_LEN);
    int index;
    ssize_t ret, length = 0;

    //Broadcast
    memset(send_req->h_dest, 0xff, MAC_LENGTH);

    //Target MAC zero
    memset(arp_req->target_mac, 0x00, MAC_LENGTH);

    //Set source mac to our MAC address
    memcpy(send_req->h_source, src_mac, MAC_LENGTH);
    memcpy(arp_req->sender_mac, src_mac, MAC_LENGTH);
    memcpy(socket_address.sll_addr, src_mac, MAC_LENGTH);

    /* Setting protocol of the packet */
    send_req->h_proto = htons(ETH_P_ARP);

    /* Creating ARP request */
    arp_req->hardware_type = htons(HW_TYPE);
    arp_req->protocol_type = htons(ETH_P_IP);
    arp_req->hardware_len = MAC_LENGTH;
    arp_req->protocol_len = IPV4_LENGTH;
    arp_req->opcode = htons(ARP_REQUEST);

    debug("Copy IP address to arp_req");
    memcpy(arp_req->sender_ip, &src_ip, sizeof(uint32_t));
    memcpy(arp_req->target_ip, &dst_ip, sizeof(uint32_t));

    ret = sendto(fd, buffer, 42, 0, (struct sockaddr *) &socket_address, sizeof(socket_address));
    if (ret == -1) {
        perror("sendto():");
        goto out;
    }
    err = 0;
out:
    return err;
}

/*
 * Gets interface information by name:
 * IPv4
 * MAC
 * ifindex
 */
int get_if_info(const char *ifname, uint32_t *ip, char *mac, int *ifindex)
{
    debug("get_if_info for %s", ifname);
    int err = -1;
    struct ifreq ifr;
    int sd = socket(AF_PACKET, SOCK_RAW, htons(ETH_P_ARP));
    if (sd <= 0) {
        perror("socket()");
        goto out;
    }
    if (strlen(ifname) > (IFNAMSIZ - 1)) {
        printf("Too long interface name, MAX=%i\n", IFNAMSIZ - 1);
        goto out;
    }

    strcpy(ifr.ifr_name, ifname);

    //Get interface index using name
    if (ioctl(sd, SIOCGIFINDEX, &ifr) == -1) {
        perror("SIOCGIFINDEX");
        goto out;
    }
    *ifindex = ifr.ifr_ifindex;
    printf("interface index is %d\n", *ifindex);

    //Get MAC address of the interface
    if (ioctl(sd, SIOCGIFHWADDR, &ifr) == -1) {
        perror("SIOCGIFINDEX");
        goto out;
    }

    //Copy mac address to output
    memcpy(mac, ifr.ifr_hwaddr.sa_data, MAC_LENGTH);

    if (get_if_ip4(sd, ifname, ip)) {
        goto out;
    }
    debug("get_if_info OK");

    err = 0;
out:
    if (sd > 0) {
        debug("Clean up temporary socket");
        close(sd);
    }
    return err;
}

/*
 * Creates a raw socket that listens for ARP traffic on specific ifindex.
 * Writes out the socket's FD.
 * Return 0 on success.
 */
int bind_arp(int ifindex, int *fd)
{
    debug("bind_arp: ifindex=%i", ifindex);
    int ret = -1;

    // Submit request for a raw socket descriptor.
    *fd = socket(AF_PACKET, SOCK_RAW, htons(ETH_P_ARP));
    if (*fd < 1) {
        perror("socket()");
        goto out;
    }

    debug("Binding to ifindex %i", ifindex);
    struct sockaddr_ll sll;
    memset(&sll, 0, sizeof(struct sockaddr_ll));
    sll.sll_family = AF_PACKET;
    sll.sll_ifindex = ifindex;
    if (bind(*fd, (struct sockaddr*) &sll, sizeof(struct sockaddr_ll)) < 0) {
        perror("bind");
        goto out;
    }

    ret = 0;
out:
    if (ret && *fd > 0) {
        debug("Cleanup socket");
        close(*fd);
    }
    return ret;
}

/*
 * Reads a single ARP reply from fd.
 * Return 0 on success.
 */
int read_arp(int fd, ETH_PARAM_ID id)
{
	unsigned char ip[128]={0};
	  unsigned char ipAddress[4];
	  unsigned char macAddress[6];
    debug("read_arp");
    int ret = -1;
    unsigned char buffer[BUF_SIZE];
    ssize_t length = recvfrom(fd, buffer, BUF_SIZE, 0, NULL, NULL);
    int index;
    if (length == -1) {
        perror("recvfrom()");
        goto out;
    }
    struct ethhdr *rcv_resp = (struct ethhdr *) buffer;
    struct arp_header *arp_resp = (struct arp_header *) (buffer + ETH2_HEADER_LEN);
    if (ntohs(rcv_resp->h_proto) != PROTO_ARP) {
        debug("Not an ARP packet");
        goto out;
    }
    if (ntohs(arp_resp->opcode) != ARP_REPLY) {
        debug("Not an ARP reply");
        goto out;
    }
    debug("received ARP len=%ld", length);
    struct in_addr sender_a;
    memset(&sender_a, 0, sizeof(struct in_addr));
    memcpy(&sender_a.s_addr, arp_resp->sender_ip, sizeof(uint32_t));
    debug("Sender IP: %s", inet_ntoa(sender_a));

    debug("Sender MAC: %02X:%02X:%02X:%02X:%02X:%02X",
          arp_resp->sender_mac[0],
          arp_resp->sender_mac[1],
          arp_resp->sender_mac[2],
          arp_resp->sender_mac[3],
          arp_resp->sender_mac[4],
          arp_resp->sender_mac[5]);



	printf("\nARP response Address: %03d. %03d. %03d. %03d\n",arp_resp->sender_ip[0],arp_resp->sender_ip[1],arp_resp->sender_ip[2],arp_resp->sender_ip[3]);
	write_eth_param(full_udp_stack_ip_base_0, id, (arp_resp->sender_ip[0]<<24)|(arp_resp->sender_ip[1]<<16)|(arp_resp->sender_ip[2]<<8)|(arp_resp->sender_ip[3]));


	printf("\nARP response Mac Address: %02x. %02x. %02x. %02x. %02x. %02x\n",arp_resp->sender_mac[0],arp_resp->sender_mac[1],arp_resp->sender_mac[2],arp_resp->sender_mac[3],arp_resp->sender_mac[4],arp_resp->sender_mac[5]);
	//write_eth_mac(full_udp_stack_ip_base_0, MAC_ID, ((arp_resp->sender_mac[0]<<40)|(arp_resp->sender_mac[1]<<32)|arp_resp->sender_mac[2]<<24)|(arp_resp->sender_mac[3]<<16)|(arp_resp->sender_mac[4]<<8)|(arp_resp->sender_mac[5]));
	write_eth_mac(full_udp_stack_ip_base_0, id, &arp_resp->sender_mac[0]);



    ret = 0;

out:
    return ret;
}

/*
 *
 * Sample code that sends an ARP who-has request on
 * interface <ifname> to IPv4 address <ip>.
 * Returns 0 on success.
 */
int run_arping(const char *ifname, const char *ip, ETH_PARAM_ID id) {
    int ret = -1;
    info("Running ARP for Destination IP:%s on IFNAME:%d ...", ip, ifname);
    uint32_t dst = inet_addr(ip);
    if (dst == 0 || dst == 0xffffffff) {
        printf("Invalid source IP\n");
        return 1;
    }

    int src;
    int ifindex;
    char mac[MAC_LENGTH];
    if (get_if_info(ifname, &src, mac, &ifindex)) {
        err("get_if_info failed, interface %s not found or no IP set?", ifname);
        goto out;
    }
    int arp_fd;
    if (bind_arp(ifindex, &arp_fd)) {
        err("Failed to bind_arp()");
        goto out;
    }

    if (send_arp(arp_fd, ifindex, mac, src, dst)) {
        err("Failed to send_arp");
        goto out;
    }
    info("Enter arp data read wait loop....");
    while(1) {
        int r = read_arp(arp_fd, id);
        if (r == 0) {
            info("Got reply, break out");
            break;
        }
    }

    ret = 0;
out:
    if (arp_fd) {
        close(arp_fd);
        arp_fd = 0;
    }
    return ret;
}

//int do_arp_cmdline(int argc, const char **argv) {
//    int ret = -1;
//    if (argc != 3) {
//        printf("Usage: %s <INTERFACE> <DEST_IP>\n", argv[0]);
//        return 1;
//    }
//    const char *ifname = argv[1];
//    const char *ip = argv[2];
//    return test_arping(ifname, ip);
//}






