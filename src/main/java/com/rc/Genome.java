package com.rc ;

import java.io.Serializable;
import java.util.BitSet;
import java.util.Random;
import java.util.StringJoiner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This holds some numbers. Each number is 0..1, with a granularity
 * of 1/BITS_PER_NUMBER
 * 
 */
public class Genome  implements Serializable {	

	private static final long serialVersionUID = 1L;

	final public static Logger log = LoggerFactory.getLogger( Genome.class ) ;
	final private static Random rng = new Random( 28 ) ;

	// Numbers have a range of 0..1 in  1/( 2^BITS_PER_NUMBER ) 
	final public static int BITS_PER_NUMBER = 10 ; 
	final public static double NUMBER_GRANULARITY = 1 << BITS_PER_NUMBER ;

	final private BitSet data ;
	private int capacity ;

	/**
	 * Create a genome that can hold a some data
	 * items. All items are cleared.
	 */
	public Genome() {
		this.data = new BitSet() ;
		capacity = 0 ;
	} 


	/**
	 * Create a genome that is a child of two parents.
	 * Characteristics are taken from each parent. After
	 * creation a mutation on the child is performed.
	 * 
	 * @param p1 the mommy gene
	 * @param p2 the daddy gene
	 * @param mutationRate the absolute chance of a mutation occurring (post birth)
	 */
	public Genome( Genome p1, Genome p2, double mutationRate ) {
		this.data = new BitSet() ;
		int len = Math.max( p1.data.length(), p2.data.length() ) ;

		int run ;
		for( int i=0 ; i<len ; i+=run ) {
			run = rng.nextInt( BITS_PER_NUMBER ) ;
			if( i+run >= len ) {
				run = len - run - 1 ;
			}
			BitSet b = rng.nextBoolean() ? p1.data : p2.data ;
			
			for( int j=0 ; j<run ; j++ ) {
				data.set( i+j, b.get(i+j) ) ;
			} 
		}

		for( int i=0 ; i<len ; i++ ) {
			if( rng.nextDouble() < mutationRate ) {
				data.set(i, !data.get(i) ) ;
			}
		}
	}

	/**
	 * Get a copy of one of the values from the genome
	 * @param index the zero based index in the genome
	 * @return The stored value - range is 0 .. 2^BITS_PER_NUMBER
	 */
	public int getInt( int index ) {
		int rc = 0 ;

		int start = index * BITS_PER_NUMBER ;
		int mask = 1 ;
		for( int i=0 ; i<BITS_PER_NUMBER ; i++, mask <<= 1 ) {
			rc += data.get(start+i) ? mask : 0  ;
		}
		return rc  ;
	}


	/**
	 * Get a copy of one of the values from the genome
	 * converting to a double
	 * @param index the zero based index in the genome
	 * @return The stored value - range is 0.0 .. 1.0
	 */
	public double getDouble( int index ) {
		int rc = getInt( index ) ;
		return ((double)rc) / NUMBER_GRANULARITY  ;
	}


	/**
	 * Set one of the values in the genome to a given value
	 * @param value The value to store - range is 0.0 .. 1.0
	 * @param index the zero based index in the genome
	 */
	public void  set( int value, int index ) {		
		int mask = 1 ;
		int start = index * BITS_PER_NUMBER ;
		for( int i=0 ; i<BITS_PER_NUMBER ; i++, mask <<= 1 ) {
			if( 0 != (mask & value) ) {
				data.set( start + i ) ;
			} else {
				data.clear( start + i ) ;
			}
		}
		if( index+1 > capacity ) {
			capacity = index+1 ;
		}
	}


	/**
	 * Set one of the values in the genome to a given value
	 * @param value The value to store - range is 0.0 .. 1.0
	 * @param index the zero based index in the genome
	 */
	public void  set( double value, int index ) {
		int bits = (int)Math.floor( NUMBER_GRANULARITY * value ) ;
		set( bits, index ) ;
	}

	/**
	 * Append one genome to the end of this
	 * @param tail the genome to glue onto the end of the receiver
	 */
	public void append( Genome tail ) {
		int start = capacity * BITS_PER_NUMBER ;
		for( int i = tail.data.nextSetBit(0); i >= 0; i = tail.data.nextSetBit(i+1) ) {
			data.set( i + start ) ;
		} 
		capacity += tail.capacity ;
	}

	/**
	 * Copy a subsection of the genome into a new Genmeo
	 * @param start the index of the first number ( not bit index )
	 * @param length the number of numbers to copy
	 */
	public Genome subSequence( int start, int length ) {
		Genome rc = new Genome() ;
		for( int i=0 ; i<length ; i++ ) {
			rc.set( getInt( start + i ), i ) ;
		} 
		return rc ;
	}


	public double accuracy() {
		return (1.0/BITS_PER_NUMBER) ;
	}
	
	public String toString() {

		StringJoiner rc = new StringJoiner( ", " ) ;

		int n = Math.min( capacity,  10 ) ;
		for( int i=0 ; i<n ; i++ ) {
			rc.add( String.valueOf( getInt(i) ) ) ;
		}
		return rc.toString() ;
	}
}


