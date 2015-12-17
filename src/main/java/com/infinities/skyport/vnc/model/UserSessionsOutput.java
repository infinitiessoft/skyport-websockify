package com.infinities.skyport.vnc.model;

public class UserSessionsOutput {

	private Long id;

	private StringBuilder output;


	public UserSessionsOutput(Long id, StringBuilder output) {
		this.id = id;
		this.output = output;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public StringBuilder getOutput() {
		return output;
	}

	public void setOutput(StringBuilder output) {
		this.output = output;
	}
}
