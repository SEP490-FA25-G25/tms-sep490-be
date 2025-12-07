package org.fyp.tmssep490be.dtos.enrollment;

public enum RecommendationType {

    OK, // Capacity đủ, enroll hết

    PARTIAL_SUGGESTED, // Vượt capacity, suggest enroll một phần

    OVERRIDE_AVAILABLE, // Vượt capacity nhưng <= 20%, có thể override

    BLOCKED // Vượt quá nhiều, không nên enroll
}
