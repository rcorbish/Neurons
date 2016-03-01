package local.rac.custom;

import java.util.Random;

import org.apache.log4j.Level;

public class Test {
	static Random rng = new Random( 200 );

	public static void main(String[] args) {
		final int INPUT_COUNT = 2 ;
		final int OUTPUT_COUNT = 1 ;
		int clock = 0 ;
		try {
			Brain brain = new Brain( 0.15, Brain.FULL, INPUT_COUNT, OUTPUT_COUNT, new int[] { 2, 4, 2 } ) ;

			/*RestServer server = */new WebServer( brain ) ;
			double inputs[] = new double[INPUT_COUNT] ;
			double outputs[][] = new double[50][OUTPUT_COUNT] ;
			
			for( ; ; ) {
				if( clock == outputs.length ) {
					clock=0 ;					
				}
				outputs[clock][0] = 0.0 ;
				for( int i=0 ; i<INPUT_COUNT ; i++ ) {
					inputs[i] = rng.nextDouble() * 40 ;
					outputs[clock][0] += inputs[i] ;
				}
				int clk = clock - 35 ;
				if( clk<0 ) clk += outputs.length ;
				System.out.println( "Error:" + brain.train( inputs, outputs[clk] ) ) ;
				clock++ ;
		
				Thread.sleep(50);
			}
		} catch( Throwable t ) {  
			t.printStackTrace();
		}
	}


	static {
		org.apache.log4j.LogManager.getLogger("org.eclipse.jetty").setLevel(Level.WARN);
		final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("org.eclipse.jetty");
		if ((logger instanceof ch.qos.logback.classic.Logger)) {
			ch.qos.logback.classic.Logger logbackLogger = (ch.qos.logback.classic.Logger) logger;
			logbackLogger.setLevel(ch.qos.logback.classic.Level.WARN);
		}
	}
}
