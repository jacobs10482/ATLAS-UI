package com.example.atlasinstructionsskeleton;

public class Slide {
    String title;
    String instruction;
    int imageResId;
    boolean showInteractive;

    public Slide(String title, String instruction, int imageResId, boolean showInteractive) {
        this.title = title;
        this.instruction = instruction;
        this.imageResId = imageResId;
        this.showInteractive = showInteractive;
    }
}
