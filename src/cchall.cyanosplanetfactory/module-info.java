module cchall.cyanosplanetfactory {
	exports hall.collin.christopher.cpf;
	requires java.desktop;
	requires java.logging;
	requires javafx.graphics;
	requires javafx.fxml;
	requires javafx.controls;
	requires javafx.base;
	requires name.cchall.globeviewer;
	requires com.grack.nanojson;
	requires gpl.cchall.worldfactory;
	
	opens hall.collin.christopher.cpf to javafx.fxml;
}