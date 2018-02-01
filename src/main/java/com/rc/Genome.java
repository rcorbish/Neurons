package com.rc ;

import java.util.BitSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This holds some numbers. Each number is 0..1, with a granularity
 * of 1/BITS_PER_NUMBER
 * 
 */
public class Genome  {

	final static Logger log = LoggerFactory.getLogger( Genome.class ) ;

	// Numbers have a range of 0..1 in 1/64 ( 2^6 ) 
	final public static int BITS_PER_NUMBER = 6 ; 
	final public static double NUMBER_GRANULARITY = 1 << BITS_PER_NUMBER ;

	final private BitSet data ;

	/**
	 * Create a genome that can hold a fixed amount of data
	 * items. All items are cleared.
	 */
	public Genome( int capacity ) {
		this.data = new BitSet( capacity * BITS_PER_NUMBER ) ;
	}

	/**
	 * Get a copy of one of the values from the genome
	 * @param index the zero based index in the genome
	 * @return The stored value - range is 0.0 .. 1.0
	 */
	public double getValue( int index ) {
		int rc = 0 ;

		int start = index * BITS_PER_NUMBER ;
		int mask = 1 ;
		for( int i=0 ; i<BITS_PER_NUMBER ; i++, mask <<= 1 ) {
			rc += data.get(start+i) ? mask : 0  ;
		}
		return ((double)rc) / NUMBER_GRANULARITY  ;
	}


	/**
	 * Set one of the values in the genome to a given value
	 * @param value The value to store - range is 0.0 .. 1.0
	 * @param index the zero based index in the genome
	 */
	public void  setValue( double value, int index ) {
		int bits = (int)Math.floor( NUMBER_GRANULARITY * value ) ;
		
		int mask = 1 ;
		int start = index * BITS_PER_NUMBER ;
		for( int i=0 ; i<BITS_PER_NUMBER ; i++, mask <<= 1 ) {
			if( 0 != (mask & bits) ) {
				data.set( start + i ) ;
			} else {
				data.clear( start + i ) ;
			}
		}
	}

	public double accuracy() {
		return (1.0/BITS_PER_NUMBER) ;
	}
	
	public int capacity() {
		return data.size() / BITS_PER_NUMBER ;
	}
}


