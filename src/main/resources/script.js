
const container = document.querySelector(".container") ;
const menu = document.querySelector(".menu") ;
const netView = document.getElementById( "cloud" ) ;

var width = netView.parentElement.clientWidth ;
var height = netView.parentElement.clientHeight ;


function toggleMenu() {
	const collapsibles = document.querySelectorAll( ".collapsible" ) ;
		for( var i=0 ; i<collapsibles.length ; i++ ) {
    	collapsibles[i].classList.toggle("collapsed");
	}
}


/*
	Handle the init
	check browser compatibility
	setup drop zone + handlers
*/
function init() {

	const xhr = new XMLHttpRequest();
	if( !xhr.upload ) {
		alert( "This browser is not supported." ) ;
		return ;
	}

	const sliders = document.querySelectorAll( "input[type='range']" ) ;
	for( var i=0 ; i<sliders.length ; i++ ) {
		sliders[i].addEventListener("change", drawView, false ) ;
	}
		
	const dropSite = document.querySelector( ".drop-site" ) ;
	
	// file drop
	dropSite.addEventListener("dragover", FileDragHover, false);
	dropSite.addEventListener("dragleave", FileDragHover, false);
	dropSite.addEventListener("drop", FileDropped, false);

	function FileDragHover(e) {
		e.stopPropagation() ;
		e.preventDefault() ;
		var currentClassName = e.target.className || "" ;
		currentClassName = currentClassName.replace( /[\s]*drop-hover[\s]*/, " " ) ;
		if( e.type === "dragover" ) currentClassName += " drop-hover" ;
		e.target.className = currentClassName ;
	}

	
	function FileDropped(e) {
		FileDragHover(e);
	
		var files = e.dataTransfer.files;
		if( files.length > 0 ) {
			const fileReader = new FileReader();
			fileReader.addEventListener("loadend", function(evt){
					protoText = evt.target.result ;
					protoUrl = null ;
					Redraw( protoText, null ); 
				}
				, false);
			fileReader.readAsBinaryString( files[0] );			
		} else {		// not a file - possibly a URL
			protoUrl = e.dataTransfer.getData( 'text/plain' ) ;
			protoText = null ;
			Redraw( null, protoUrl ); 
		}
	}	
}


// The global representing the network
var netobj = null ;
var protoText = null ;
var protoUrl = null ;
/*
`	Handle the screen redraw - this implies a round trip
	to the server to parse the prototext file (or url)
*/
function Redraw() {
	const xhr = new XMLHttpRequest();

	var stageParam = null ;
	const stages = document.querySelector( ".stages" ) ;
	const stageCheckboxes = stages.querySelectorAll( "input[type='checkbox']" ) ;
	const checkedStages = [] ;
	for( let i=0 ; i<stageCheckboxes.length ; i++ ) {
		let stageCheckbox = stageCheckboxes[i] ;
		if( stageCheckbox.checked ) {
			checkedStages.push( stageCheckbox.value ) ;
		}
	}
	const url = checkedStages.length>0 ? 
				"upload?stage="+checkedStages.join('&stage=') :
				"upload"
	xhr.open("POST", url );
	xhr.setRequestHeader( "accept", "text/html" ) ;
	xhr.overrideMimeType('application/javascript; charset=utf-8');
	xhr.onreadystatechange = function() {
		if(xhr.readyState === XMLHttpRequest.DONE && xhr.status === 200) {
			netobj = JSON.parse( xhr.responseText ) ;
			defineStages() ;
			drawView() ;
	    }
	} ;
	
	if( protoText ) {
		xhr.setRequestHeader("Content-Type", "application/x-prototext");
		xhr.send( protoText ) ;
	} 
	if( protoUrl ) {
		xhr.setRequestHeader("Content-Type", "text/uri-list");
		xhr.send( protoUrl ) ;
	}	
}

/*
	create some checkboxes to enable/disable visibility
	of nodes in the network
*/
function defineStages() {
	const dat = Object.keys( netobj.stages ) ;

	const stageDiv = d3.select(".stages") ;

	const labels = stageDiv.selectAll( ".stages label" ) 
		.data( dat )
		.text( function(d) { return d ; } )
		;

	labels.append( 'input' )
		.on( 'change', Redraw )
		.property( "checked", function(d) { 
			return netobj.stages[d] ; 
		})
		.attr( 'type', 'checkbox' )
		.attr( 'name', 'stage' )
		.attr( 'value', function(d) { return d ; } )
		; 
	
	labels.exit()
		.remove()
		;

	labels.enter()
		.append( 'label' )
		.text( function(d) { return d ; } ) 
		.append( 'input' )
		.on( 'change', Redraw )
		.property( "checked", function(d) { 
			return netobj.stages[d] ; 
		})
		.attr( 'type', 'checkbox' )
		.attr( 'name', 'stage' )
		.attr( 'value', function(d) { return d ; } )
		;
}

/*
	redraw the view - we need a json representation of
	the network.
*/ 
function drawView() {
	function dragstarted(d) {
		if (!d3.event.active) simulation.alphaTarget(0.3).restart();
		d.fx = d.x;
		d.fy = d.y;
	}

	function dragged(d) {
		d.fx = d3.event.x;
		d.fy = d3.event.y;
	}

	function dragended(d) {
		if (!d3.event.active) simulation.alphaTarget(0);
		d.fx = null;
		d.fy = null;
	}


	while (netView.hasChildNodes()) {
    	netView.removeChild(netView.lastChild);
	}


	const svg = d3.select("svg") ;
  	const svgg = svg.append("g").attr("class", "network") ;

	const color = d3.scaleOrdinal(d3.schemeCategory20);

	const charge = document.getElementById("charge").value ;
	const tension = document.getElementById("tension").value ;
	const radius = document.getElementById("radius").value ;

  	const links = svgg
	    .selectAll("line")
    	.data(netobj.links)
    	.enter().append("line") 
    ;

  	const nodes = svgg
    	.selectAll("circle.node")
    	.data(netobj.nodes.filter( e => e.type!=='blob' ))
    	.enter().append("circle")
			.attr( 'class', 'node' )
      		.attr("r", function(d) { return (radius||50)/2 } ) 
      		.attr("fill", function(d) { return color(d.type); })
      		.call(d3.drag()
          		.on("start", dragstarted)
          		.on("drag", dragged)
          		.on("end", dragended)) 
  		;

  	const blobs = svg
    	.selectAll("circle.blob")
    	.data(netobj.nodes.filter( e => e.type==='blob' ) )
    	.enter().append("circle")
      		.attr( 'class', 'blob' )
      		.attr("r", function(d) { return (radius||50)/3 } ) 
      		.attr("fill", function(d) { return '#777'; })
      		.call(d3.drag()
          		.on("start", dragstarted)
          		.on("drag", dragged)
          		.on("end", dragended)) 
  		;

  	const texts = svgg
    	.selectAll("text")
    	.data(netobj.nodes.filter( e => e.type!=='blob' ))
    	.enter().append("text")
	    	.attr( 'text-anchor', 'middle' ) 
    		.style( 'font-size', ((radius||50)/5.5) + 'px' ) 
    		.text( function(d) { return d.name } )
  		;

      
  	function tick() {
    	nodes
    	.attr("cx", function(d) { return d.x; })
        .attr("cy", function(d) { return d.y; })
    	;
    	blobs
    	.attr("cx", function(d) { return d.x; })
        .attr("cy", function(d) { return d.y; })
    	;
    	texts
        .attr("x", function(d) { return d.x; })
        .attr("y", function(d) { return d.y; })
    	;
    
    	links
        .attr("x1", function(d) { return d.source.x; })
        .attr("y1", function(d) { return d.source.y; })
        .attr("x2", function(d) { return d.target.x; })
        .attr("y2", function(d) { return d.target.y; })
    	;
	}


	const simulation = d3.forceSimulation()
		.nodes( netobj.nodes ) 	
		.force( "link", d3.forceLink()
				.id( function(n) { return n.name ; } )
				.strength( function(l) { return tension||1 ; }) 
				.distance( function(l) { return 10 ; } )
				.links( netobj.links ) 
			)
		.force( "charge", d3.forceManyBody()
				.strength( -(charge||50) ) 
			)
		.force( "center", d3.forceCenter(width/2, height/2) ) 			
		.force( "collide", d3.forceCollide( radius||20 ) ) 
		.force( "y", d3.forceY( function(d) { return 0 ; } ).strength( 0.3 ) ) 
		.force( "x", d3.forceX( function(d) { return d.xPreferred || 0 ; } ).strength( function(d) { return d.xPreferred ? 0.1 : 0.0 ; } ) )			
		.on("tick", tick ) 
		.on("end", tick ) 
		;
}

window.onload = init() ;
