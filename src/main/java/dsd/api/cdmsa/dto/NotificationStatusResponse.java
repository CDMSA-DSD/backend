package dsd.api.cdmsa.dto;

public record NotificationStatusResponse(
    boolean read,
    long notisNotRead) {}
