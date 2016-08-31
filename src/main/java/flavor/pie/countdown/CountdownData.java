package flavor.pie.countdown;

import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.data.manipulator.DataManipulatorBuilder;
import org.spongepowered.api.data.manipulator.immutable.common.AbstractImmutableData;
import org.spongepowered.api.data.manipulator.mutable.common.AbstractData;
import org.spongepowered.api.data.merge.MergeFunction;
import org.spongepowered.api.data.persistence.AbstractDataBuilder;
import org.spongepowered.api.data.persistence.InvalidDataException;

import java.util.Optional;

public class CountdownData extends AbstractData<CountdownData, CountdownData.Immutable> {
    Countdown plugin;
    CountdownData(long ticks, Countdown plugin) {
        this.ticks = ticks;
        waitTicks = -1;
    }
    CountdownData(long ticks, long waitTicks, Countdown plugin) {
        this.waitTicks = ticks;
        this.ticks = ticks;
    }
    long ticks;
    long waitTicks;
    @Override
    protected void registerGettersAndSetters() {
        registerFieldGetter(CountdownKeys.TICKS_LEFT, this::getTicks);
        registerFieldSetter(CountdownKeys.TICKS_LEFT, this::setTicks);
        registerFieldGetter(CountdownKeys.WAIT_TICKS_LEFT, this::getWaitTicks);
        registerFieldSetter(CountdownKeys.WAIT_TICKS_LEFT, this::setWaitTicks);
    }

    public long getWaitTicks() {
        return waitTicks;
    }

    public void setWaitTicks(long waitTicks) {
        this.waitTicks = waitTicks;
    }

    public long getTicks() {
        return ticks;
    }

    public void setTicks(long ticks) {
        this.ticks = ticks;
    }

    @Override
    public Optional<CountdownData> fill(DataHolder dataHolder, MergeFunction overlap) {
        Optional<CountdownData> data_ = dataHolder.get(CountdownData.class);
        if (data_.isPresent()) {
            CountdownData data = data_.get();
            CountdownData finalData = overlap.merge(this, data);
            ticks = finalData.ticks;
            waitTicks = finalData.waitTicks;
        }
        return Optional.of(this);
    }

    public Optional<CountdownData> from(DataView container) {
        long ticks = container.getLong(CountdownKeys.TICKS_LEFT.getQuery()).orElse(plugin.builder.defaultTicks);
        long waitTicks = container.getLong(CountdownKeys.WAIT_TICKS_LEFT.getQuery()).orElse(this.waitTicks);
        this.ticks = ticks;
        this.waitTicks = waitTicks;
        return Optional.of(this);
    }

    @Override
    public Optional<CountdownData> from(DataContainer container) {
        return from((DataView) container);
    }

    @Override
    public CountdownData copy() {
        return new CountdownData(ticks, waitTicks, plugin);
    }

    @Override
    public Immutable asImmutable() {
        return new Immutable(ticks, waitTicks, plugin);
    }

    @Override
    public int compareTo(CountdownData o) {
        return Long.compare(ticks, o.ticks);
    }

    @Override
    public int getContentVersion() {
        return 1;
    }

    @Override
    public DataContainer toContainer() {
        return super.toContainer().set(CountdownKeys.WAIT_TICKS_LEFT.getQuery(), waitTicks).set(CountdownKeys.TICKS_LEFT.getQuery(), ticks);
    }

    public static class Immutable extends AbstractImmutableData<Immutable, CountdownData> {
        Countdown plugin;
        long ticks;
        long waitTicks;
        Immutable(long ticks, Countdown plugin) {
            this.ticks = ticks;
            waitTicks = plugin.builder.defaultWaitTicks;
        }
        Immutable(long ticks, long waitTicks, Countdown plugin) {
            this.waitTicks = waitTicks;
            this.ticks = ticks;
        }
        @Override
        protected void registerGetters() {
            registerFieldGetter(CountdownKeys.TICKS_LEFT, this::getTicks);
            registerFieldGetter(CountdownKeys.WAIT_TICKS_LEFT, this::getWaitTicks);
        }

        public long getWaitTicks() {
            return waitTicks;
        }

        public long getTicks() {
            return ticks;
        }

        @Override
        public CountdownData asMutable() {
            return new CountdownData(ticks, waitTicks, plugin);
        }

        @Override
        public int compareTo(Immutable o) {
            return Long.compare(this.ticks, o.ticks);
        }

        @Override
        public int getContentVersion() {
            return 1;
        }

        @Override
        public DataContainer toContainer() {
            return super.toContainer().set(CountdownKeys.WAIT_TICKS_LEFT.getQuery(), waitTicks).set(CountdownKeys.TICKS_LEFT.getQuery(), ticks);
        }
    }

    public static class Builder extends AbstractDataBuilder<CountdownData> implements DataManipulatorBuilder<CountdownData, Immutable> {
        long defaultTicks;
        long defaultWaitTicks;
        Countdown plugin;
        Builder(long defaultTicks, long defaultWaitTicks, Countdown plugin) {
            super(CountdownData.class, 1);
            this.defaultTicks = defaultTicks;
            this.defaultWaitTicks = defaultWaitTicks;
            this.plugin = plugin;
        }

        @Override
        public CountdownData create() {
            return new CountdownData(defaultTicks, defaultWaitTicks, plugin);
        }

        @Override
        public Optional<CountdownData> createFrom(DataHolder dataHolder) {
            return create().fill(dataHolder);
        }

        @Override
        protected Optional<CountdownData> buildContent(DataView container) throws InvalidDataException {
            return create().from(container);
        }
    }

}
