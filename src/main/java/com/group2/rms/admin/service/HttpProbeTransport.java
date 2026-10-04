package com.group2.rms.admin.service;

import java.io.IOException;
import java.net.URI;

/** Sends one safe GET to a fixed monitoring target and returns its HTTP status. */
public interface HttpProbeTransport {
    int get(URI uri, String sessionId) throws IOException, InterruptedException;
}
