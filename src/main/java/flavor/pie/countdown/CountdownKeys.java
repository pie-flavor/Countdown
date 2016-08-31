package flavor.pie.countdown;

import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.key.KeyFactory;
import org.spongepowered.api.data.value.mutable.Value;

public class CountdownKeys {
    public final static Key<Value<Long>> TICKS_LEFT;
    public final static Key<Value<Long>> WAIT_TICKS_LEFT;
    static {
        TICKS_LEFT = KeyFactory.makeSingleKey(Long.class, Value.class, DataQuery.of("TicksLeft"));
        WAIT_TICKS_LEFT = KeyFactory.makeSingleKey(Long.class, Value.class, DataQuery.of("WaitTicksLeft"));
    }
}
