package com.github.theword.queqiao.tool.payload.modle.component;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
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

    public CommonBaseComponent(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return getText();
    }
}
