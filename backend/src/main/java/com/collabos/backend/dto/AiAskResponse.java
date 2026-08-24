package com.collabos.backend.dto;

import java.util.List;

public record AiAskResponse(boolean configured, String answer, List<String> sources) {
}
