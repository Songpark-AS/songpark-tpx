CROSS_COMPILE?=arm-linux-gnueabihf-
APP_NAME?=connect
CC = $(CROSS_COMPILE)gcc
CCSIZE = $(CROSS_COMPILE)size
INSTALL_DIR = /media/ZED_ROOT/
IDIR = src/include
CFLAGS = -I$(IDIR) --std=gnu99 -O0
LIBS = -lrt -lpthread
OBJS = src/network.o src/adau1761_controller.o src/zedboardOLED.o src/reg_io.o src/udpclient.o src/stream_control.o src/volume_control.o src/filter_control.o src/ui_control.o src/app_main.o

all : $(APP_NAME) 

$(APP_NAME) : $(OBJS)
	$(CC) $(CFLAGS) $(OBJS) $(LIBS) -o $(APP_NAME)
	$(CCSIZE) $(APP_NAME)
%.o: %.c
	$(CC) $(CFLAGS) -c -o $@ $<

clean:
	rm -rf src/*.o $(APP_NAME)

install: $(APP_NAME)
	
	cp $(APP_NAME) $(INSTALL_DIR)
	
mount : 
	udevil mount /dev/mmcblk0p1 $(INSTALL_DIR)/usr/bin
	
umount : 
	udevil umount /dev/mmcblk0p1
