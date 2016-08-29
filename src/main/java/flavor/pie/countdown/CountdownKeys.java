package flavor.pie.countdown;

import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.key.KeyFactory;
import org.spongepowered.api.data.value.mutable.Value;

public class CountdownKeys {
    public static Key<Value<Long>> TICKS_LEFT = KeyFactory.makeSingleKey(Long.class, Value.class, DataQuery.of("TicksLeft"));
    public static Key<Value<Long>> WAIT_TICKS_LEFT = KeyFactory.makeSingleKey(Long.class, Value.class, DataQuery.of("WaitTicksLeft"));
}
