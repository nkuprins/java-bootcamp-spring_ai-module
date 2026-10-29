package com.accenture.springai_bootcamp_demo.voyage.service;

import com.accenture.springai_bootcamp_demo.voyage.entity.LogEntry;
import java.util.List;

record ShipChange(List<LogEntry> entries, List<MemoryDraft> memories) {
}
