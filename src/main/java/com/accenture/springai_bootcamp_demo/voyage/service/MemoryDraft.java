package com.accenture.springai_bootcamp_demo.voyage.service;

import com.accenture.springai_bootcamp_demo.voyage.entity.MemoryKind;

record MemoryDraft(Long crewMemberId, int turn, MemoryKind kind, String content) {
}
