var stdOut;

function videoBios(ax, bx) {
	var ah = (ax & 0xFF00) >> 8, al = ax & 0xFF;
	switch (ah) {
		case 0x0e:
			if (stdOut == null) {
				stdOut = document.getElementById("stdOut");
			}
			
			if (al == 0x0a) 
				stdOut.innerHTML += "<br>";
			else 
				stdOut.innerHTML += String.fromCharCode(al);
			break;
		default: throw new Error();
	}
}
