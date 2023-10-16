package uk.co.hopperelec.mc.randomevents.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;
import uk.co.hopperelec.mc.randomevents.utils.TimeInSeconds;

public class RandomEventsGameConfig {
    public TimeInSeconds countdownLength;
    public long lootSeed;
    @NotNull public RandomEventScope shareScope;
    @NotNull public String weightPresetName;
    public boolean enableSpecialItems;

    @JsonCreator
    public RandomEventsGameConfig(
            @JsonProperty("countdown_length") TimeInSeconds countdownLength,
            @JsonProperty("loot_seed") long lootSeed,
            @JsonProperty("share_scope") @NotNull RandomEventScope shareScope,
            @JsonProperty("weight_preset") @NotNull String weightPresetName,
            @JsonProperty("enable_special_items") boolean enableSpecialItems
    ) {
        this.countdownLength = countdownLength;
        this.lootSeed = lootSeed;
        this.shareScope = shareScope;
        this.weightPresetName = weightPresetName;
        this.enableSpecialItems = enableSpecialItems;
    }

    public RandomEventsGameConfig(@NotNull RandomEventsGameConfig o) {
        this(o.countdownLength, o.lootSeed, o.shareScope, o.weightPresetName, o.enableSpecialItems);
    }
}