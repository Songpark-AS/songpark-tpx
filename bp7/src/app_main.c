/*
 * =====================================================================================
 *
 *       Filename:  app_main.c
 *
 *    Description:  application initialization functions
 *
 *         Author:  Thanx
 *   Organization:  Cantavi
 *
 * =====================================================================================
 */

#include <stdlib.h>
#include <unistd.h>
#include <stdio.h>
#include "include/ui_control.h"
#include "include/adau1761_controller.h"

FILE *tty1;
FILE *tty2;


int main (int argc, char *argv[]) {
	
	tty2 = freopen("/dev/pts/2", "w", stdout);

	if (tty2 == NULL) {
		perror("Unable to open terminal for writting");
		exit(1);
	}

	tty1 = freopen("/dev/pts/2", "r", stdin);

	if (tty1 == NULL) {
		perror("Unable to open terminal for reading");
		exit(1);
	}

	// char s[256];
	// while(1){
	// 	memset(s, 0, sizeof(s));
	// 	while(*s == 0){
	// 		//fgets(s, sizeof(s), stdin);
    //          scanf("%s",s);
	// 	}
	// fprintf (stderr, s);
	// printf("Echo:: %s", s);
	// }

	int ret = 0;
	printf ("Initializing Cantavi Streamer!!!\n");

	if (argc < 2){
		printf ("Error Initializing Cantavi Streamer arguments too few!!!\n");
		return EXIT_FAILURE;
	}
	printf("Got %d arguments.....\n", argc);
	ret = ui_init (argc, argv);

	if (ret != 0) {
		fprintf (stderr, "ui_init failed\n");
		return EXIT_FAILURE;
	}




	printf ("Runnin Cantavi Streamer UI\n");
	ret = ui_run ();

	if (ret != 0) {
		fprintf (stderr, "ui_run failed\n");
		return EXIT_FAILURE;
	}

	ui_exit();

	return EXIT_SUCCESS;

}
