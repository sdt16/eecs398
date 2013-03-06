package edu.cwru.eecs398.obdreader.elm327;

public enum Elm327Commands {
	
	GET_NUMBER_OF_CODES("01 01\r"),
	GET_CODES("03\r");
	
	private String command;
	
	private Elm327Commands(String command) {
		this.command = command;
	}
	
	public String getCommand() {
		return command;
	}

}
