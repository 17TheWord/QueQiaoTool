package com.github.theword.queqiao.tool.response;

public class RconCommandError {
    private String command;
    private String error;

    public RconCommandError() {
    }

    public RconCommandError(String command, String error) {
        this.command = command;
        this.error = error;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
