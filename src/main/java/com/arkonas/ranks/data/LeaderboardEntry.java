package com.arkonas.ranks.data;

import java.util.UUID;

public record LeaderboardEntry(UUID uuid, String name, int count) {
}
