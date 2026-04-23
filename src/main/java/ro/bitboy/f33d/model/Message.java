package ro.bitboy.f33d.model;

import java.time.Instant;

public record Message(String id, String sender, String text, Instant timestamp) {}
