package org.fyp.tmssep490be.services;

public interface PolicyService {

    /**
     * Lấy giá trị INTEGER của policy (GLOBAL scope).
     * Nếu không tìm thấy hoặc lỗi parse thì trả về defaultValue.
     */
    int getGlobalInt(String policyKey, int defaultValue);

    /**
     * Lấy giá trị BOOLEAN của policy (GLOBAL scope).
     * Nếu không tìm thấy hoặc lỗi parse thì trả về defaultValue.
     */
    boolean getGlobalBoolean(String policyKey, boolean defaultValue);
}



