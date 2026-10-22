package com.group2.rms.chatbot.service;

/*
 * Các ý định chatbot nhận ra được. CHECK_STATUS và RECOMMEND chỉ dành cho ứng viên đã đăng nhập.
 */
public enum ChatbotIntent {
    WELCOME,
    FAQ,
    SEARCH_JOB,
    JOB_DETAIL,
    CHECK_STATUS,
    RECOMMEND,
    CONTACT_HR,
    UNKNOWN,
    DISABLED
}
