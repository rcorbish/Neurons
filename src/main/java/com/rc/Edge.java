package com.rc ;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Edge {
	public int source ;
	public double weight ;
	public Edge( int sourceIndex, double weight ) {
		this.source = sourceIndex ;
		this.weight = weight ;
	}
}

