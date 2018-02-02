package com.rc;

import java.util.ArrayList;
import java.util.List;

public class EdgeList {

	private final List<Edge> edges ;

	
	public EdgeList() {
		edges = new ArrayList<>() ;
	}

	public EdgeList( Genome genome ) {
		this() ;
		for( int i=0 ; i<genome.capacity() ; i+=2 ) {
			double w = genome.getDouble( i ) ;
			int s = genome.getInt( i + 1 ) ;
			edges.add(  new Edge( s, w ) ) ;			
		}
	}
	
	public EdgeList( List<Edge> edges ) {
		this() ;
		this.edges.addAll( edges ) ;
	}
	
	public Genome toGenome() {
		Genome rc = new Genome( edges.size() * 2 ) ;
		for( int i=0 ; i<edges.size() ; i++ ) {
			rc.set( edges.get(i).weight, i*2 + 0 ) ;
			rc.set( edges.get(i).source, i*2 + 1 ) ;
		}
		return rc ;
	}
	
	public void set( int index, Edge e ) {
		edges.set( index, e ) ;
	}
	
	public void add( Edge e ) {
		edges.add( e ) ;
	}
	
	public Edge get( int index  ) {
		return edges.get( index ) ;
	}
	
	public void clear() {
		edges.clear();
	}
	
	public int size() {
		return edges.size() ;
	}
}
