#define F_CPU 1000000UL

#include <avr/io.h>
#include <util/delay.h>

/*
 * This demonstrate how to use the avr_mcu_section.h file
 * The macro adds a section to the ELF file with useful
 * information for the simulator
 */
#include "avr_mcu_section.h"
AVR_MCU(F_CPU, "atmega328p");
AVR_MCU_SIMAVR_CONSOLE(&GPIOR0);


const struct avr_mmcu_vcd_trace_t _mytrace[]  _MMCU_ = {
    { AVR_MCU_VCD_SYMBOL("DDRB"), .what = (void*)&DDRB, },
    { AVR_MCU_VCD_SYMBOL("PORTB"), .what = (void*)&PORTB, },
};

int
main (void)
{
    DDRB |= _BV(DDB0);
    const char *s = "Hello World\r";
    for (const char *t = s; *t; t++)
      GPIOR0 = *t; 
    while(1) 
    {
        PORTB ^= _BV(PB0);
        _delay_ms(100);
    }
}
