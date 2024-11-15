package com.example.atlasinstructionsskeleton;

public class AtlasSlide extends Slide {
    // Any additional properties specific to AtlasSlide can be added here

    public AtlasSlide(String title, String instruction) {
        // Since AtlasSlide doesn't need an image resource ID, we pass 0 or any default value
        super(title, instruction, 0);
    }
}
