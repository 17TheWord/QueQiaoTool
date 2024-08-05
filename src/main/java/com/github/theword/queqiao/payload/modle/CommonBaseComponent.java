package com.github.theword.queqiao.payload.modle;

import lombok.Data;

@Data
public class CommonBaseComponent {
    private String text;
    private String color;
    private String font;
    private boolean bold;
    private boolean italic;
    private boolean underlined;
    private boolean strikethrough;
    private boolean obfuscated;
    private String insertion;

    @Override
    public String toString() {
        return this.text;
    }
}
