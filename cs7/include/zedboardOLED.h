/*
 *
 *
 * 	*/

#ifndef ZEDBOARDOLED_H
#define ZEDBOARDOLED_H

#define DELAY 10000


static int int_seq [64];


/**
 * Implementation of Xilinx's output function for usage in Linux driver.
 * @param address Memory address to be written to
 * @param value   Value to be written to the memory address
 */
void Xil_Out32(unsigned *address, unsigned value);

/*  driver functions for ZedboardOLED IP core */
/*****************************************************************************/
/**
*
* prints a character on the OLED at the page and the position specified by the second
* and third arguments,example print_char('A',0,0);
*
* @param	char char_seq , the character to be printed.
*
* @param	unsigned int page(0-3) , the OLED is divided into 4 pages numbers, 0 is the upper
*			3 is the lower.
* @param	unsigned int position(0-15) , each page can hold 16 characters
* 			0 is the leftmost , 15 is the rightmost
*
* @return	int , 1 on success , 0 on failure.

******************************************************************************/
int oled_print_char(char char_seq, unsigned int page,
                    unsigned int position, void *oled_base_addr);

/*****************************************************************************/
/**
*
* prints a string of characters on the OLED at the page specified by the second
* argument, maximum string per page =16,example: print_char("Texas A&M Qatar,0);
*
* @param	char *start , the string message to be printed , maximum 16 letters.
*
* @param	unsigned int page(0-3) , the OLED is divided into 4 pages numbers, 0 is the upper
*			3 is the lower.
*
* @return	int , 1 on success , 0 on failure.
*
******************************************************************************/
int oled_print_message(char *start, unsigned int page, void *oled_base_addr);

/*****************************************************************************/
/**
*
* clears the screen, example: clean();
*
*
* @param	none.
*
* @return	none.
******************************************************************************/
void oled_clear(void *oled_base_addr);


#endif // ZEDBOARDOLED_H
