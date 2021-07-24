package us.jcedeno.jlimit;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor(staticName = "create")
@Data
public class PlayerMask {
    long timePlayedToday;
    long totalTimePlayed;
    long cannotPlayUntil;

    public void addTime(long time) {
        timePlayedToday += time;
        totalTimePlayed += time;
    }

    public void addSecond() {
        addTime(1);
    }

    public void resetTime() {
        timePlayedToday = 0L;
    }

    public static PlayerMask init() {
        return new PlayerMask(0L, 0L, -1L);
    }
}
