package com.rc ;

public class Edge {
	static public double MAX_WEIGHT = 0.85 ;
	static public double MIN_WEIGHT = 0.00 ;
	
	static public int GENOME_SIZE = 4 ;
	
	private final int source ;
	private final int target ;
	private double weight ;
	
	public Edge( int sourceIndex, int targetIndex, double weight ) {
		this.target = targetIndex ;
		this.source = sourceIndex ;
		this.weight = weight ;
	}
	
	public Edge( Genome genome ) {
		this.source = genome.getInt( 0 ) ;
		this.target = genome.getInt( 1 ) ;

		int w1 = genome.getInt( 2 ) ;
		double w2 = genome.getDouble( 3 ) ;

		this.weight = (w1 + w2) / 500.0 - 1.0 ;
	}
	
	public Genome toGenome() {
		Genome rc = new Genome() ;
		rc.set( source, 0 ) ;
		rc.set( target, 1 ) ;

		double w = (weight + 1.0 ) * 500.0 ;
		int w1 = (int) w ;
		double w2 = (w - w1) ;

		rc.set( w1, 2 ) ;
		rc.set( w2, 3 ) ;
		return rc ;
	}
	
	public int source() {
		return source ;
	}
	public int target() {
		return target ;
	}
	public double weight() {
		return weight ;
	}
	public String id() {
		return source + "-" + target ;
	}
	
	public void addWeight( double addition ) {
		double factor = 1.0 ; // addition > 0 ? (0.5 - weight ) : ( weight + 0.15 ) ; 
		weight += factor * addition ;
		if( weight > MAX_WEIGHT ) weight = MAX_WEIGHT ;
		if( weight < MIN_WEIGHT ) weight = MIN_WEIGHT ;
	}
}

